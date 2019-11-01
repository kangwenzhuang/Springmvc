package com.kang.service.serviceImpl;

import com.kang.annotation.MyService;
import com.kang.service.UserService;


@MyService("userService")
public class UserServiceImpl implements UserService {
    public String result(String name, String age) {
        return "name:" + name + '\n' + "age:" + age;
    }
}
