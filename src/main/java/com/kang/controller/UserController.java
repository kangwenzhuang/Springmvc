package com.kang.controller;


import com.kang.annotation.MyAutowired;
import com.kang.annotation.MyController;
import com.kang.annotation.MyRequestMapping;
import com.kang.annotation.MyRequestParam;
import com.kang.service.UserService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@MyRequestMapping("/user")
@MyController("userController")
public class UserController {
    @MyAutowired("userService")
    private UserService userService;

    @MyRequestMapping("/hello")
    public void result(HttpServletRequest request, HttpServletResponse response, @MyRequestParam("name") String name, @MyRequestParam("age") String age) {
        String result= userService.result(name, age);
        try {
            PrintWriter pw=response.getWriter();
            pw.write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
