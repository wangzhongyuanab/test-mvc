package com.entity;

import javax.jnlp.IntegrationService;

/**
 * @program: test-mvc
 * @description:
 * @author: Mr.Wang
 * @create: 2020-06-01 18:25
 **/
public class User {
    public String name;
    public String age;
    public String sex;

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", age='" + age + '\'' +
                ", sex='" + sex + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }
}
