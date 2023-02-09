package com.xl.multirequestbody.controller;

import com.xl.multirequestbody.annotation.MultiRequestBody;
import lombok.Data;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 测试类
 *
 * @author XL
 */
@RestController
@RequestMapping("/test")
public class DemoController {

    @Data
    public static class Dog {

        private String name;

        private String color;
    }

    @Data
    public static class User {

        private String name;

        private Integer age;
    }

    @RequestMapping("/testStr")
    public String multiRequestBodyDemo1(@MultiRequestBody String test1, @MultiRequestBody String test2) {
        System.out.println(test1 + "-->" + test2);
        return "";
    }

    @RequestMapping("/testChar")
    public String multiRequestBodyDemo1(@MultiRequestBody char id) {
        System.out.println(id);
        return "";
    }

    @RequestMapping("/demo")
    public String multiRequestBodyDemo1(@MultiRequestBody Dog dog, @MultiRequestBody User user) {
        System.out.println(dog.toString() + user.toString());
        return dog + ";" + user;
    }

    @RequestMapping("/demo2")
    public String multiRequestBodyDemo2(@MultiRequestBody("dog") Dog dog, @MultiRequestBody User user) {
        System.out.println(dog.toString() + user.toString());
        return dog + ";" + user;
    }

    @RequestMapping("/demo3")
    public String multiRequestBodyDemo3(@MultiRequestBody("dog") Dog dog, @MultiRequestBody("user") User user) {
        System.out.println(dog.toString() + user.toString());
        return dog + ";" + user;
    }

    @RequestMapping("/demo4")
    public String multiRequestBodyDemo4(@MultiRequestBody("dog") Dog dog, @MultiRequestBody Integer age) {
        System.out.println(dog.toString() + age.toString());
        return dog + ";age属性为：" + age;
    }

    @RequestMapping("/demo5")
    public String multiRequestBodyDemo5(@MultiRequestBody("color") String color, @MultiRequestBody("age") Integer age) {
        return "color=" + color + "; age=" + age;
    }

    @RequestMapping("/demo6")
    public String multiRequestBodyDemo6(@MultiRequestBody("dog") Dog dog, @MultiRequestBody Integer age) {
        System.out.println(dog.toString() + age.toString());
        return dog + ";age属性为：" + age;
    }

    @RequestMapping("/demo7")
    public String multiRequestBodyDemo7(@MultiRequestBody(required = false) Dog dog, @MultiRequestBody("age") Integer age) {
        return "dog=" + dog + "; age=" + age;
    }

    @RequestMapping("/demo10")
    public String multiRequestBodyDemo10(@MultiRequestBody(parseAllFields = false, required = false) Dog dog) {
        return dog.toString();
    }

    @RequestMapping("/demo99")
    public String multiRequestBodyDemo99(@MultiRequestBody(parseAllFields = false, required = false) Character demo) {
        return demo.toString();
    }

    @RequestMapping("/testList")
    public String multiRequestBodyDemo1(@MultiRequestBody List test, @MultiRequestBody String str) {
        return test.toString() + str;
    }
}