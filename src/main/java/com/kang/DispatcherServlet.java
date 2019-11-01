package com.kang;

import com.kang.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DispatcherServlet extends HttpServlet {
    List<String> classNames = new ArrayList<String>();
    HashMap<String, Object> beans = new HashMap<String, Object>();
    HashMap<String, Object> handlerMaps = new HashMap<String, Object>();

    public void init(ServletConfig config) {
        String packageName = this.getClass().getPackage().getName();
        doScan(packageName);//扫包,默认扫描DispatcherServlet所在目录

        doInstance();//实例化对象

        doAutowired();//依赖注入

        urlHanding();//地址映射

    }

    void urlHanding() {
        for (Map.Entry<String, Object> m : beans.entrySet()) {
            Object object = m.getValue();
            Class<?> clazz = object.getClass();
            if (clazz.isAnnotationPresent(MyController.class)) {
                MyRequestMapping mr = clazz.getAnnotation(MyRequestMapping.class);
                String path1 = mr.value();//得到/user路径

                Method[] methods = clazz.getMethods();
                for (Method mt : methods) {
                    if (mt.isAnnotationPresent(MyRequestMapping.class)) {
                        MyRequestMapping q = mt.getAnnotation(MyRequestMapping.class);
                        String path2 = q.value();//得到/hello
                        handlerMaps.put(path1 + path2+"/", mt);//把路径和对应的方法保存起来，然后在doGet中实现
                    } else {
                        continue;
                    }
                }
            }
        }

    }


    void doAutowired() {
        for (Map.Entry<String, Object> m : beans.entrySet()) {
            Class<?> clazz = m.getValue().getClass();
            if (clazz.isAnnotationPresent(MyController.class)) {
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    if (field.isAnnotationPresent(MyAutowired.class)) {
                        MyAutowired ma = field.getAnnotation(MyAutowired.class);
                        String key = ma.value();
                        Object object = beans.get(key);
                        field.setAccessible(true);
                        try {
                            field.set(m.getValue(), object);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    } else {
                        continue;
                    }
                }
            } else {
                continue;
            }
        }
    }


    void doInstance() {
        for (String className : classNames) {
            String cn = className.replace(".class", "");
            try {
                Class<?> clazz = Class.forName(cn);

                if (clazz.isAnnotationPresent(MyController.class)) {
                    Object instance = clazz.newInstance();
                    MyController mc = clazz.getAnnotation(MyController.class);
                    String key = mc.value();
                    beans.put(key, instance);
                } else if (clazz.isAnnotationPresent(MyService.class)) {
                    Object instance = clazz.newInstance();
                    MyService ms = clazz.getAnnotation(MyService.class);
                    String key = ms.value();
                    beans.put(key, instance);
                } else {
                    continue;
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }


    void doScan(String packageName) {
        String pn=packageName;
        URL url = this.getClass().getClassLoader().getResource(packageName.replace(".", "/"));
        String fileStr = url.getFile();
        File file = new File(fileStr);
        String[] filesStr = file.list();
        for (String path : filesStr) {
            File filePath = new File(fileStr + path);
            if (filePath.isDirectory()) {
                doScan((packageName + "." + path));
            } else {
                classNames.add(packageName + "." + filePath.getName());
            }
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) {
        this.doPost(req, res);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) {
        String uri = req.getRequestURI();
        Method method = (Method) handlerMaps.get(uri);
        Object object = null;
        Here:
        //获取拥有该方法的对象，该对象拥有注解有@MyRequestMapping,且为url
        for (Map.Entry<String, Object> map : beans.entrySet()) {
            Class<?> clazz = map.getValue().getClass();
            if (clazz.isAnnotationPresent(MyController.class)) {
                if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                    if (clazz.getAnnotation(MyRequestMapping.class).value().equals("/" + uri.split("/")[1])) {
                        Method[] methods = clazz.getMethods();
                        for (Method med : methods) {
                            if (med.isAnnotationPresent(MyRequestMapping.class)) {
                                MyRequestMapping ma = med.getAnnotation(MyRequestMapping.class);
                                if (ma.value().equals("/" + uri.split("/")[2])) {
                                    object = map.getValue();
                                    break Here;
                                }
                            } else {
                                continue;
                            }
                        }
                    }
                }
            }

        }
        //首先要获取参数列表getArgs

        try {
            method.invoke(object, getArgs(req,res,method));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }


    }
    Object[] getArgs(HttpServletRequest req, HttpServletResponse res,Method method){
        Class<?>[] paramClazzs=method.getParameterTypes();
        Object[] args=new Object[paramClazzs.length];
        int i=0;
        int index=0;
        for(Class<?> paramclazz:paramClazzs){
            if(ServletRequest.class.isAssignableFrom(paramclazz)){
                args[i++]=req;
            }
            if(ServletResponse.class.isAssignableFrom(paramclazz)){
                args[i++]=res;
            }
            Annotation[] paramAns=method.getParameterAnnotations()[index];//第0和1个的时候长度为0，因为参数前面没有注解，第3和4个参数前面有注解，因为每个参数的注解可以有多个，所以二维数组
            if(paramAns.length>0){
                for(Annotation paramAn:paramAns){
                    if(MyRequestParam.class.isAssignableFrom(paramAn.getClass())){
                        MyRequestParam rp=(MyRequestParam) paramAn;
                        args[i++]=req.getParameter(rp.value());
                    }
                }
            }
            index++;
        }
        return args;
    }
}
