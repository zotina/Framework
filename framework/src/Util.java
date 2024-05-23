package framework.sprint0;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;

import jakarta.servlet.*;

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
                if (method.isAnnotationPresent(framework.sprint0.Annotation.Get.class)) {
                    urlMapping.put(method.getAnnotation(framework.sprint0.Annotation.Get.class).value(),
                            new Mapping(clazz.getSimpleName(), method.getName()));
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
}
