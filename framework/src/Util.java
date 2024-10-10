package framework;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

public class Util {

    public static ArrayList<Class<?>> scanClasses(String source, ServletContext servletContext, Class<?> cla)
            throws MalformedURLException, ClassNotFoundException {
        ArrayList<Class<?>> classes = new ArrayList<>();

        String classPath = servletContext.getResource(source).getPath().substring(1).replace("%20", " ");
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
                    System.out.println("class processed: " + className);
                    
                    Class<?> clazz = Class.forName(className);
                    System.out.println("class: " + clazz);
                    System.out.println(Arrays.toString(clazz.getAnnotations()));
                    if (clazz.isAnnotationPresent((Class<? extends Annotation>) cla)) {
                        classes.add(clazz);
                        System.out.println("class added: " + clazz);
                    }
                    System.out.println();
                }
                
                else if (pkg.isDirectory()) {
                    System.out.println("directory found: " + pkg);
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
                if (method.isAnnotationPresent(framework.Annotation.Url.class)) {
                    String url = method.getAnnotation(framework.Annotation.Url.class).value();
                    VerbeAction verbeAction = new VerbeAction();
                    verbeAction.setMethode(method.getName());
                    verbeAction.setVerbe(method.isAnnotationPresent(framework.Annotation.Post.class) ? "POST" : "GET");

                    if (!urlMapping.containsKey(url)) {
                        Mapping mapping = new Mapping();
                        mapping.setClassName(clazz.getName());
                        mapping.setVerbeActions(new ArrayList<>());
                        mapping.getVerbeActions().add(verbeAction);
                        urlMapping.put(url, mapping);
                    } else {
                        Mapping existingMapping = urlMapping.get(url);
                        existingMapping.getVerbeActions().add(verbeAction);
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
            HttpServletResponse res, String className, String url) throws Exception {
        Class<?> clazz = Class.forName(className);
        Object object = clazz.newInstance();
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (method.getName().equalsIgnoreCase(methodName)) {
                method.setAccessible(true);
                Object[] methodParams = getMethodParams(method, req);
                Object obj =method.invoke(object, methodParams);
                if(method.isAnnotationPresent(framework.Annotation.RestApi.class)){
                    if(obj instanceof ModelView m ){
                        return new Gson().toJson(m.getData());
                    }
                    return new Gson().toJson(obj);
                }
                return obj;
            }
        }
        return null;
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

    public static void processRequest(HttpServletRequest req, Mapping mapping) throws CustomException.RequestException {
        boolean verbFound = false;
    
        for (VerbeAction verbeAction : mapping.getVerbeActions()) {
            if (verbeAction.getVerbe().equalsIgnoreCase(req.getMethod())) {
                verbFound = true;
                break;
            }
        }
    
        if (!verbFound) {
            throw new CustomException.RequestException("HTTP 400 Bad Request:");   
        }
    }
    

    public static void processUrl(HashMap<String, Mapping> urlMapping, PrintWriter out, HttpServletRequest req, HttpServletResponse res, ArrayList<Class<?>> contrôleurs) 
        throws ServletException, IOException, CustomException.BuildException, CustomException.RequestException, Exception {
        Object urlValue = null; // Initialisation de urlValue à null
        boolean trouvé = false;
        String html = "";
        String url = Util.removeRootSegment(req.getRequestURI());
        html += Util.header(url, contrôleurs);

        for (Map.Entry<String, Mapping> entrée : urlMapping.entrySet()) {
            String cle = entrée.getKey();
            Mapping valeur = entrée.getValue();

            if (cle.equals(url)) {
                for (VerbeAction verbeAction : valeur.getVerbeActions()) {
                    processRequest(req, valeur);

                    try {
                        urlValue = Util.getValueMethod(verbeAction.getMethode(), req, res, valeur.getClassName(), url);
                    } catch (Exception e) {
                        throw new CustomException.RequestException(e.getMessage()+"process url ");
                    }

                    html += "<BIG><p>URLMAPPING:</BIG>" + valeur.getClassName() + "_" + verbeAction.getMethode() + "</p>";
                    html += "</br>";
                    html += "<BIG><p>MethodeValue:</BIG>";
                    html += urlValue;

                    if (urlValue instanceof String s) {
                        html += s;
                    } else if (urlValue instanceof ModelView m) {
                        Util.sendModelView(m, req, res);
                    } else if (urlValue instanceof JsonElement j) {
                        out.println(j);
                    } else {
                        html = "";
                        Class<?> cls = Class.forName(valeur.getClassName());
                        throw new CustomException.BuildException(
                                "Impossible d'obtenir la valeur pour le type " 
                                + (urlValue == null ? "void" : urlValue.getClass()) 
                                + " dans la méthode " + verbeAction.getMethode() 
                                + "\n à " + valeur.getClassName() + "." 
                                + verbeAction.getMethode() + "(" + cls.getSimpleName() + ".java)"
                        );
                    }
                    out.println("</p>");
                    trouvé = true;
                    break;
                }
                break;
            }
        }

       
        if (!Util.isRoot(url) && !trouvé) {
            throw new CustomException.RequestException("ERREUR 404 URL: " + req.getRequestURI() + " NON TROUVÉE");
        } else {
            out.println(html);
        }
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

    protected static Object[] getMethodParams(Method method, HttpServletRequest request)
            throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] methodParams = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            String paramName = "";
            if (parameters[i].isAnnotationPresent(framework.Annotation.Param.class)) {
                paramName = parameters[i].getAnnotation(framework.Annotation.Param.class).value();
            } else {
                throw new Exception("ETU2597");
            }

            Class<?> paramType = parameters[i].getType();

            if (!paramType.isPrimitive() && !paramType.equals(String.class) && !paramType.equals(Session.class)) {
                try {
                    Object paramObject = paramType.getDeclaredConstructor().newInstance();
                    Field[] fields = paramType.getDeclaredFields();

                    for (Field field : fields) {
                        String fieldName = field.getName();
                        String fieldValue = request.getParameter(paramName + "." + fieldName);
                        if (fieldValue != null) {
                            field.setAccessible(true);
                            Object typedValue = convertToType(fieldValue, field.getType());
                            field.set(paramObject, typedValue);
                        }
                    }
                    methodParams[i] = paramObject;
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                        | NoSuchMethodException e) {
                    throw new IllegalArgumentException("Error creating parameter object: " + paramName, e);
                }
            } else if (paramType.equals(Session.class)) {
                methodParams[i] = new Session(request.getSession());
            } else {
                String paramValue = request.getParameter(paramName);
                if (paramValue == null) {
                    throw new IllegalArgumentException("Missing parameter " + paramName);
                }
                methodParams[i] = convertToType(paramValue, paramType);
            }
        }
        return methodParams;
    }

    private static Object convertToType(String paramValue, Class<?> type) throws Exception {
        if (paramValue == null || type == null) {
            return null;
        }

        if (type == String.class) {
            return paramValue;
        } else if (type == Integer.class || type == int.class) {
            return Integer.parseInt(paramValue);
        } else if (type == Double.class || type == double.class) {
            return Double.parseDouble(paramValue);
        }

        return null;
    }

    public static String capitalize(String inputString) {
        char firstLetter = inputString.charAt(0);
        char capitalizeFirstLetter = Character.toUpperCase(firstLetter);
        return capitalizeFirstLetter + inputString.substring(1);
    }
}