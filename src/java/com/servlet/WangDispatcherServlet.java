package com.servlet;

import com.annotation.Controller;
import com.annotation.RequestMapping;
import com.annotation.ResponseBody;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * @program: test-mvc
 * @description: 自实现的DispatcherServlet
 * @author: Mr.Wang
 * @create: 2020-06-01 18:21
 **/
public class WangDispatcherServlet extends HttpServlet {

    private static String  COMPENT_SCAN_ELEMENT_PACKAGE_NAME= "package";

    private static String COMPENT_SCAN_ELEMENT_NAME = "compentScan";

    private static String XML_PATH_LOCAL= "xmlPathLocal";

    private  static String prefix = "";
    private  static String suffix = "";

    private static String projectPath = WangDispatcherServlet.class.getResource("/").getPath();

    private  static Map<String, Method> methodMap = new HashMap<>();

    /**
     * init主要做得事情:
     * 加载配置文件 web.xml 加载spring mvc.xml
     扫描整个项目 根据配置文件给定的目录来扫描
     扫描所有加了@Controller注解的类
     当扫描到加了@Controller注解的类之后遍历里面所有的方法
     拿到方法对象之后 解析方法上面是否加了@RequestMapping注解
     定义一个Map集合  吧@RequstMapping的Value 与方法对象绑定起来
     Map<String,Method>
     * @param config
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        //得到springmvc文件名字
        String initParameter = config.getInitParameter(XML_PATH_LOCAL);
        File file = new File(projectPath + "\\" + initParameter);
        //解析springmvc的xml文件
        Document document = parse(file);
        Element rootElement = document.getRootElement();
        Element view = rootElement.element("view");
        prefix = view.attribute("prefix").getValue();
        suffix = view.attribute("suffix").getValue();
        //得到扫描的包
        Element compentScan = rootElement.element(COMPENT_SCAN_ELEMENT_NAME);
        //value：com
        String aPackage = compentScan.attribute(COMPENT_SCAN_ELEMENT_PACKAGE_NAME).getValue();
        //扫描包下的类文件注册到一个map中
        scanProjectByPath(projectPath+"//"+aPackage);
    }

    //递归扫描包下的类
    private void scanProjectByPath(String path) {
        File file = new File(path);
        //递归解析
        scanFile(file);
    }

    private void scanFile(File file) {
        //如果文件是一个文件夹
        if (file.isDirectory()){
            for (File listFile : file.listFiles()) {
                scanFile(listFile);
            }
        }else{
            //如果不是文件夹
            //D://project//com//controller//TestController.class
            //com.controller.TestController
            String filePath = file.getPath();
            String suffix =filePath.substring(filePath.lastIndexOf("."));
            if (suffix.equals(".class")){
                String classPath  =  filePath.replace(new File(projectPath).getPath()+"\\","");
                classPath = classPath.replaceAll("\\\\",".");
                String className = classPath.substring(0,classPath.lastIndexOf("."));
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(Controller.class)){
                        RequestMapping classRequestMapping = clazz.getAnnotation(RequestMapping.class);
                        String classRequestMappingUrl="";
                        if (classRequestMapping!=null){
                            classRequestMappingUrl=classRequestMapping.value();
                        }
                        for (Method method : clazz.getDeclaredMethods()) {
                            if (!method.isSynthetic()){
                                RequestMapping annotation = method.getAnnotation(RequestMapping.class);
                                if (annotation!=null){
                                    String methodRequsetMappingUrl  = "";
                                    methodRequsetMappingUrl  = annotation.value();
                                    System.out.println("类:"+clazz.getName()+"的"+method.getName()+"方法被映射到了"+classRequestMappingUrl+methodRequsetMappingUrl+"上面");
                                    methodMap.put(classRequestMappingUrl+methodRequsetMappingUrl,method);
                                }
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Document parse(File file){
        SAXReader saxReader = new SAXReader();
        try {
            return saxReader.read(file);
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return  null;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    /**
     * 执行的时候做的事情:
     * 拿到请求URI去map里面get
     * 给参数赋值并调用方法
     * 拿到方法返回值做视图跳转和消息返回
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String requestURI = req.getRequestURI();
        Method method = methodMap.get(requestURI);
        if (method!=null){
            //jdk8以前拿不到参数名字
            Parameter[] parameters = method.getParameters();
            Object[] objects = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                String name = parameter.getName();
                Class type = parameter.getType();
                if (type.equals(String.class)){
                    objects[i]=req.getParameter(name);
                }else if (type.equals(HttpServletRequest.class)){
                    objects[i] =req;
                }else if(type.equals(HttpServletResponse.class)){
                    objects[i]=resp;
                }else{
                    //传入的参数是一个实体类
                    try {
                        Object o = type.newInstance();
                        for (Field field : type.getDeclaredFields()) {
                            field.setAccessible(true);
                            String fieldName = field.getName();
                            field.set(o,req.getParameter(fieldName));
                        }
                        objects[i]=o;
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }

                }
            }

            try {
                Object o=null;
                o = method.getDeclaringClass().newInstance();
                Object invoke = method.invoke(o, objects);
                if (!method.getReturnType().equals(Void.class)){
                    ResponseBody responseBody = method.getAnnotation(ResponseBody.class);
                    if (responseBody!=null){
                        //提供接口来做
                        resp.getWriter().write(String.valueOf(invoke));
                    }else{
                        req.getRequestDispatcher(prefix+String.valueOf(invoke)+suffix).forward(req,resp);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            resp.setStatus(400);
        }
    }
}
