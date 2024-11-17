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
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
                
                System.out.println("mbola nitohy fa tsy nijanona getValue");
                Object[] methodParams = getMethodParams(method, req,res);
                System.out.println("methode params="+methodParams);
                if(methodParams==null){
                    System.out.println("getValu ko null");
                    return null;
                }
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

    public static StatusCode processRequest(HttpServletRequest req, Mapping mapping){
        boolean verbFound = false;
    
        for (VerbeAction verbeAction : mapping.getVerbeActions()) {
            if (verbeAction.getVerbe().equalsIgnoreCase(req.getMethod())) {
                verbFound = true;
                return null;
            }
        }
        
        if (!verbFound) {
            return new StatusCode(400,"Bad Request",false,"can not access request method");  
        }
        
        return null;
    }
    
    public static ResponsePage processUrl(HashMap<String, Mapping> urlMapping, PrintWriter out, HttpServletRequest req, HttpServletResponse res, ArrayList<Class<?>> controleurs) {
        Object urlValue = null;
        boolean trouve = false;
        String html = "";
        String url = Util.removeRootSegment(req.getRequestURI());
        
        try {
            html += Util.header(url, controleurs);
    
            for (Map.Entry<String, Mapping> entree : urlMapping.entrySet()) {
                String cle = entree.getKey();
                Mapping valeur = entree.getValue();
    
                if (cle.equals(url)) {
                    VerbeAction matchingVerbe = null;
                    for (VerbeAction verbeAction : valeur.getVerbeActions()) {
                        if (verbeAction.getVerbe().equalsIgnoreCase(req.getMethod())) {
                            matchingVerbe = verbeAction;
                            break;
                        }
                    }
    
                    if (matchingVerbe != null) {
                        StatusCode processR = processRequest(req, valeur);
                        if (processR != null) {
                            return new ResponsePage(processR, html);
                        }
                        try {
                            urlValue = Util.getValueMethod(matchingVerbe.getMethode(), req, res, valeur.getClassName(), url);
                            
                            if (urlValue == null) {
                                String redirectPage = (String)req.getSession().getAttribute("page");
                                if (redirectPage != null) {
                                    HttpSession session = req.getSession();
                                    Map<String, String> errorMap = (Map<String, String>) session.getAttribute("validationErrors");
                                    Map<String, Object> valueMap = (Map<String, Object>) session.getAttribute("validationValues");
                                    
                                    if (errorMap != null) {
                                        for (Map.Entry<String, String> entry : errorMap.entrySet()) {
                                            req.setAttribute(entry.getKey(), entry.getValue());
                                            System.err.println("error="+entry.getKey()+"_value="+entry.getValue());
                                        }
                                        session.removeAttribute("validationErrors");
                                    }
                                    
                                    if (valueMap != null) {
                                        for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
                                            req.setAttribute(entry.getKey(), entry.getValue());
                                            System.err.println("valueReturn="+entry.getKey()+"_valueReturnvalue="+entry.getValue());
                                        }
                                        session.removeAttribute("validationValues");
                                    }
                                    
                                    res.sendRedirect(redirectPage);
                                    return null;
                                }
                            } else {
                                trouve = true;
                                
                                html += "<BIG><p>URLMAPPING:</BIG>" + valeur.getClassName() + "_" + matchingVerbe.getMethode() + "</p>";
                                html += "</br>";
                                html += "<BIG><p>MethodeValue:</BIG>";
                                html += urlValue;
    
                                if (urlValue instanceof String s) {
                                    html += s;
                                } else if (urlValue instanceof ModelView m) {
                                    Util.sendModelView(m, req, res);
                                } else if (urlValue instanceof JsonElement j) {
                                    html += j;
                                } else {
                                    Class<?> cls = Class.forName(valeur.getClassName());
                                    return new ResponsePage(new StatusCode(500, "internal server error", false,
                                        "Impossible d'obtenir la valeur pour le type " 
                                        + urlValue.getClass() 
                                        + " dans la méthode " + matchingVerbe.getMethode() 
                                        + "\n à " + valeur.getClassName() + "." 
                                        + matchingVerbe.getMethode() + "(" + cls.getSimpleName() + ".java)"), html);
                                }
                            }
                        } catch (Exception e) {
                            return new ResponsePage(new StatusCode(500, "internal server error", false, e.getMessage()), html);
                        }
                    }
                    break;
                }
            }
            
            if (!Util.isRoot(url) && !trouve) {
                return new ResponsePage(new StatusCode(404, "url not found", false, "could not find " + req.getRequestURI()), html);
            }
            
            return new ResponsePage(new StatusCode(200, true), html);
        } catch (Exception e) {
            return new ResponsePage(new StatusCode(500, "internal server error", false, e.getMessage()), html);
        }
    }
    public static void processStatus(StatusCode statusCode) throws CustomException.BuildException,CustomException.RequestException{
        if (!statusCode.isSuccess() ) {
            if (statusCode.getStatus() == 500 ){
                throw new CustomException.BuildException(statusCode.getMessage());
            }
            else{
                throw new CustomException.RequestException(statusCode.getMessage());
            }
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

    public static Object[] getMethodParams(Method method, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] methodParams = new Object[parameters.length];
        Map<String, String> errorMap = new HashMap<>();
        Map<String, Object> valueMap = new HashMap<>();
        
        for (int i = 0; i < parameters.length; i++) {
            String paramName = "";
            if (parameters[i].isAnnotationPresent(framework.Annotation.Param.class)) {
                paramName = parameters[i].getAnnotation(framework.Annotation.Param.class).value();
            } else {
                throw new Exception("ETU2597");
            }
            
            Class paramType = parameters[i].getType();
            
            // Traitement des objets complexes (non primitifs)
            if (!paramType.isPrimitive() && !paramType.equals(String.class) && 
                !paramType.equals(Session.class) && !paramType.equals(FileUpload.class)) {
                
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
                    
                } catch (InstantiationException | IllegalAccessException | 
                        InvocationTargetException | NoSuchMethodException e) {
                    throw new IllegalArgumentException(
                        "Error creating parameter object: " + paramName, e
                    );
                }
            }
            else if (paramType.equals(Session.class)) {
                methodParams[i] = new Session(request.getSession());
            }
            else if(paramType.equals(FileUpload.class)) {
                methodParams[i] = handleFileUpload(request, paramName);
            }
            else {
                String paramValue = request.getParameter(paramName);
                if (paramValue == null) {
                    throw new IllegalArgumentException("Missing parameter " + paramName);
                }
                methodParams[i] = convertToType(paramValue, paramType);
            }
            
            List<String> errors = Contraintes.valider(methodParams[i], parameters[i]);
            if (!errors.isEmpty()) {    
                errorMap.put("error_" + paramName, String.join(", ", errors));
                valueMap.put("value_" + paramName, methodParams[i]);
            }
        }
    
        if (!errorMap.isEmpty()) {
            HttpSession session = request.getSession();
            session.setAttribute("validationErrors", errorMap);
            session.setAttribute("validationValues", valueMap);
            return null;
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

    public static FileUpload handleFileUpload(HttpServletRequest request, String inputFileParam) throws IOException, ServletException {
        Part filePart = request.getPart(inputFileParam); 
        String fileName = extractFileName(filePart);
        byte[] fileContent = filePart.getInputStream().readAllBytes();

        String uploadDir = request.getServletContext().getRealPath("") + "uploads/" + fileName;
        System.out.println("upload = "+uploadDir);

        File uploadFolder = new File(uploadDir);
        if (!uploadFolder.exists()) {
            uploadFolder.mkdirs();
        }

        String uploadPath = uploadDir + File.separator + fileName;
        System.out.println("upload = "+uploadPath);

        filePart.write(uploadPath);

        return new FileUpload(fileName, uploadPath, fileContent);
    }

    private static String extractFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        String[] items = contentDisposition.split(";");
        for (String item : items) {
            if (item.trim().startsWith("filename")) {
                return item.substring(item.indexOf("=") + 2, item.length() - 1);
            }
        }
        return "";
    }
}