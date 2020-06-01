package com.controller;

import com.annotation.Controller;
import com.annotation.RequestMapping;
import com.annotation.ResponseBody;
import com.entity.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @program: test-mvc
 * @description:
 * @author: Mr.Wang
 * @create: 2020-06-01 18:25
 **/
@Controller
@RequestMapping("/test")
public class TestController {

    @RequestMapping("/test.do")
    @ResponseBody
    public String test(String name, HttpServletRequest request, HttpServletResponse response,User user){
        System.out.println(name);
        System.out.println(request);
        System.out.println(response);
        System.out.println(user);
        return "index";
    }

    @RequestMapping("/test1.do")
    public String test1(){
        return "index";
    }
}
