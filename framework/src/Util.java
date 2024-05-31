package framework;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

public class Util {

    public static ArrayList<Class<?>> scanClasses(String source, ServletContext servletContext, Class<?> cla)
            throws MalformedURLException, ClassNotFoundException {
        ArrayList<Class<?>> classes = new ArrayList<>();

        String classPath = servletContext.getResource(source).getPath();
        File[] packages = new File(classPath).listFiles();

        if (packages != null) {
            for (File pkg : packages) {
                if (pkg.isFile() && pkg.getName().endsWith(".class")) {
                    String className = source.substring("WEB-INF.classes".length()).replace('/', '.');

                    if (className.startsWith(".")) {
                        className = className.substring(1);
                    }
                    if (className.length() > 0 && !className.endsWith(".")) {
                        className += ".";
                    }

                    className += pkg.getName().substring(0, pkg.getName().length() - ".class".length()).replace('/',
                            '.');

                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent((Class<? extends Annotation>) cla)) {
                        classes.add(clazz);
                    }
                }

                else if (pkg.isDirectory()) {
                    classes.addAll(scanClasses(source + "/" + pkg.getName(), servletContext, cla));
                }
            }
        }
        return classes;
    }

    public static HashMap<String, Mapping> getUrlMapping(ArrayList<Class<?>> controllers) {
        HashMap<String, Mapping> urlMapping = new HashMap<>();
        for (Class<?> clazz : controllers) {
            Method[] methods = clazz.getDeclaredMethods();

            for (Method method : methods) {
                if (method.isAnnotationPresent(framework.Annotation.Get.class)) {
                    urlMapping.put(method.getAnnotation(framework.Annotation.Get.class).value(),
                            new Mapping(clazz.getName(), method.getName()));
                }
            }
        }
        return urlMapping;
    }

    public static String removeRootSegment(String url) {
        int firstSlashIndex = url.indexOf('/');
        if (firstSlashIndex != -1) {
            int secondSlashIndex = url.indexOf('/', firstSlashIndex + 1);
            if (secondSlashIndex != -1) {
                return url.substring(secondSlashIndex);
            } else {
                return "/";
            }
        }
        return url;
    }

    public static boolean isRoot(String url) {
        int firstSlashIndex = url.indexOf('/');
        return firstSlashIndex != -1 ? url.indexOf('/', firstSlashIndex + 1) == -1 : false;
    }

    public static Object getValueMethod(String methodName, String className) {
        try {
            Class<?> cls = Class.forName(className);
            Method method = cls.getMethod(methodName);
            Object obj = cls.newInstance();
            return method.invoke(obj);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void sendModelView(ModelView modelView, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        for (Map.Entry<String, Object> entry : modelView.getData().entrySet()) {
            request.setAttribute(entry.getKey(), entry.getValue());
            System.out.println(entry.getKey() + "_" + entry.getValue());
        }
        RequestDispatcher dispatch = request.getRequestDispatcher(modelView.getUrl());
        dispatch.forward(request, response);
    }

}
