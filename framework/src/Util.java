package framework.sprint0;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;

import javax.servlet.*;

public class Util {

    public static ArrayList<Class<?>> scanClasses(String source, ServletContext servletContext)
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
                    if (clazz.isAnnotationPresent(Controller.class)) {
                        classes.add(clazz);
                    }
                }

                else if (pkg.isDirectory()) {
                    classes.addAll(scanClasses(source + "/" + pkg.getName(), servletContext));
                }
            }
        }
        return classes;
    }
}
