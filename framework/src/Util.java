package framework;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.MalformedURLException;
import java.net.URLDecoder;
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

    public static HashMap<String, Mapping> getUrlMapping(ArrayList<Class<?>> controllers)
            throws CustomException.BuildException, CustomException.RequestException {
        HashMap<String, Mapping> urlMapping = new HashMap<>();
        for (Class<?> clazz : controllers) {
            Method[] methods = clazz.getDeclaredMethods();

            for (Method method : methods) {
                if (method.isAnnotationPresent(framework.Annotation.Get.class)) {
                    if (!classNameExists(urlMapping, method.getName())) {
                        urlMapping.put(method.getAnnotation(framework.Annotation.Get.class).value(),
                                new Mapping(clazz.getName(), method.getName()));
                    } else {
                        throw new CustomException.BuildException(
                                "duplicate function " +
                                        method.getName()
                                        + "\n        at " + clazz.getName() + "." + method.getName() + "("
                                        + clazz.getSimpleName()
                                        + ".java)");
                    }
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
        return url.trim().length() == 1;
    }

    public static Object getValueMethod(String methodName, HttpServletRequest req,
            HttpServletResponse res, String className, String url) {
        try {
            return invokeFunction(methodName, req, res, className, url);
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

    public static void processUrl(HashMap<String, Mapping> map, PrintWriter out, HttpServletRequest req,
            HttpServletResponse res, HashMap<String, Mapping> urlMapping, ArrayList<Class<?>> controllers)
            throws ServletException, IOException, CustomException.BuildException, CustomException.RequestException,
            Exception {
        Object urlValue;
        boolean test = false;
        String html = "";
        String url = Util.removeRootSegment(req.getRequestURI());
        html += Util.header(url, controllers);

        for (Map.Entry<String, Mapping> entry : urlMapping.entrySet()) {
            String key = entry.getKey();
            Mapping value = entry.getValue();

            if (key.equals(url)) {
                urlValue = Util.getValueMethod(value.getMethodeName(), req, res, value.getClassName(), url);
                html += "<BIG><p>URLMAPPING:</BIG>" + value.getClassName() + "_"
                        + value.getMethodeName() + "</p>";
                html += "</br>";
                html += "<BIG><p>MethodeValue:</BIG>";
                html += urlValue;

                if (urlValue instanceof String s) {
                    html += s;
                    test = true;
                } else if (urlValue instanceof ModelView m) {
                    Util.sendModelView(m, req, res);
                    test = true;
                } else {
                    html = "";
                    Class<?> cls = Class.forName(value.getClassName());
                    throw new CustomException.BuildException(
                            "can't getValue for type " + (urlValue == null ? "void" : urlValue.getClass())
                                    + " in method "
                                    + value.getMethodeName()
                                    + "\n        at " + value.getClassName() + "." + value.getMethodeName() + "("
                                    + cls.getSimpleName() + ".java)");
                }
                out.println("</p>");
                test = true;
                break;
            }

        }
        if (!Util.isRoot(url) && !test)
            throw new CustomException.RequestException("ERROR 404 URL: " + req.getRequestURI() + " NOT FOUND");
        else
            out.println(html);
    }

    public static String header(String requestURI, ArrayList<Class<?>> controllers) {
        String html = "<HTML>" +
                "<HEAD><TITLE>Hello Hello</TITLE></HEAD>" +
                "<BODY>" +
                "</br>" +
                "<BIG>URL:</BIG>" +
                requestURI +
                "</br>" +
                "<BIG>CONTROLLER:</BIG>" + controllers +
                "</br>";
        return html;
    }

    public static boolean classNameExists(HashMap<String, Mapping> hsmap, String methodName) {
        for (Mapping mapping : hsmap.values()) {
            if (mapping.getMethodeName().equals(methodName)) {
                return true;
            }
        }
        return false;
    }

    public static Object invokeFunction(String methodName, HttpServletRequest req,
            HttpServletResponse res, String className, String url)
            throws Exception {
        Class<?> cls = Class.forName(className);
        Method[] methods = cls.getMethods();
        boolean test = false;
        String paramName = "";
        String paramValue = "";
        Object[] methodArgs = null;
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                Parameter[] parameters = method.getParameters();
                methodArgs = new Object[parameters.length];

                for (int i = 0; i < parameters.length; i++) {
                    Parameter parameter = parameters[i];
                    if (url.contains("?")) {
                        Map<String, String> form = parseQueryString(url);
                        for (Map.Entry<String, String> entry : form.entrySet()) {
                            System.out.println(entry.getKey() + " : " + entry.getValue());
                            if (entry.getKey().equals(parameter.getName())) {
                                paramValue = entry.getValue();
                                methodArgs[i] = convertToType(paramValue, parameter.getType());
                                test = true;
                            }
                        }
                    } else {
                        if (parameter.isAnnotationPresent(framework.Annotation.Param.class)) {
                            framework.Annotation.Param annotation = parameter
                                    .getAnnotation(framework.Annotation.Param.class);
                            paramName = annotation.value();
                            paramValue = req.getParameter(paramName);
                            methodArgs[i] = convertToType(paramValue, parameter.getType());
                            test = true;
                        }
                    }
                }
                if (test) {
                    return method.invoke(cls.newInstance(), methodArgs);
                }
            }
        }
        Method methode = cls.getMethod(methodName);
        Object obj = cls.newInstance();
        return methode.invoke(obj);
    }

    private static Object convertToType(String paramValue, Class<?> type) {
        if (type == String.class) {
            return paramValue;
        } else if (type == Integer.class || type == int.class) {
            return Integer.parseInt(paramValue);
        } else if (type == Double.class || type == double.class) {
            return Double.parseDouble(paramValue);
        }
        return null;
    }

    public static Map<String, String> parseQueryString(String urlString) {
        Map<String, String> params = new HashMap<>();

        int questionMarkIndex = urlString.indexOf('?');
        if (questionMarkIndex != -1) {
            String queryString = urlString.substring(questionMarkIndex + 1);

            String[] pairs = queryString.split("&");

            for (String pair : pairs) {
                int equalsIndex = pair.indexOf('=');
                if (equalsIndex != -1) {
                    try {
                        String key = URLDecoder.decode(pair.substring(0, equalsIndex), "UTF-8");
                        String value = URLDecoder.decode(pair.substring(equalsIndex + 1), "UTF-8");

                        params.put(key.trim(), value.trim());
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return params;
    }
}