package framework;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.http.HttpResponse;
import java.sql.Date;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Array;


import com.google.gson.Gson;
import com.google.gson.JsonElement;

import framework.CustomException;
import framework.CustomException.RequestException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

public class Util {

    public static ArrayList<Class<?>> scanClasses(String source1, String source2, ServletContext servletContext, Class<?> cla)
    throws MalformedURLException, ClassNotFoundException {
        ArrayList<Class<?>> classes = new ArrayList<>();

        // Scanner la première source (Controllers)
        classes.addAll(scanSingleSource(source1, servletContext, cla));

        // Scanner la deuxième source (Models)
        if (!source1.equals(source2)) {
            classes.addAll(scanSingleSource(source2, servletContext, cla));
        }

        return classes;
    }

    private static ArrayList<Class<?>> scanSingleSource(String source, ServletContext servletContext, Class<?> cla)
        throws MalformedURLException, ClassNotFoundException {
        ArrayList<Class<?>> classes = new ArrayList<>();

        String classPath = "/" + servletContext.getResource(source).getPath().substring(1).replace("%20", " ");
        System.out.println("class path: " + classPath);
        File[] packages = new File(classPath).listFiles();

        if (packages != null) {
            for (File pkg : packages) {
                if (pkg.isFile() && pkg.getName().endsWith(".class")) {
                    String className = source.substring("WEB-INF/classes".length()).replace('/', '.');

                    if (className.startsWith(".")) {
                        className = className.substring(1);
                    }
                    if (className.length() > 0 && !className.endsWith(".")) {
                        className += ".";
                    }

                    className += pkg.getName().substring(0, pkg.getName().length() - ".class".length()).replace('/', '.');
                    System.out.println("class processed: " + className);

                    Class<?> clazz = Class.forName(className);
                    System.out.println("class: " + clazz);
                    System.out.println(Arrays.toString(clazz.getAnnotations()));
                    if (clazz.isAnnotationPresent((Class<? extends java.lang.annotation.Annotation>) cla)) {
                        classes.add(clazz);
                        System.out.println("class added: " + clazz);
                    }
                    System.out.println();
                } else if (pkg.isDirectory()) {
                    System.out.println("directory found: " + pkg);
                    classes.addAll(scanSingleSource(source + "/" + pkg.getName(), servletContext, cla));
                }
            }
        } else {
            System.out.println("packages null for source: " + source);
        }
        return classes;
    }

    public static HashMap<String, Mapping> getUrlMapping(ArrayList<Class<?>> controllers)
    throws CustomException.BuildException {

        HashMap<String, Mapping> urlMapping = new HashMap<>();
        boolean classAnnotedAuth = false;
        String profil = "";

        for (Class<?> clazz : controllers) {

            if (clazz.isAnnotationPresent(framework.Annotation.Auth.class)) {
                classAnnotedAuth = true;
                profil = clazz.getAnnotation(framework.Annotation.Auth.class).value();
                System.out.println(clazz.getName() + " is annoted Auth");
            }

            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(framework.Annotation.Url.class)) {
                    String url = method.getAnnotation(framework.Annotation.Url.class).value();
                    VerbeAction verbeAction = new VerbeAction();
                    verbeAction.setMethode(method.getName());
                    verbeAction.setVerbe(method.isAnnotationPresent(framework.Annotation.Post.class) ? "POST" : "GET");

                    if (!urlMapping.containsKey(url)) {
                        Mapping mapping = new Mapping();

                        if (classAnnotedAuth) {
                            mapping.setNeedAuth(true);
                            mapping.setProfil(profil);
                            System.out.println(method.getName() + " is annoted Auth");
                        }

                        if (method.isAnnotationPresent(framework.Annotation.Auth.class)) {
                            if (classAnnotedAuth) {
                                throw new CustomException.BuildException(clazz.getName() + " is already annoted @Auth, remove @Auth on method");
                            }
                            profil = method.getAnnotation(framework.Annotation.Auth.class).value();
                            mapping.setNeedAuth(true);
                            mapping.setProfil(profil);
                            System.out.println(method.getName() + " is annoted Auth");
                        }

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
            classAnnotedAuth = false;
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
                
                MethodParamResult paramResult = getMethodParams(method, req, res);
                
                // Check for validation errors
                if (!paramResult.getErrorMap().isEmpty()) {
                    // Retrieve the previous ModelView from session if it exists
                    ModelView previousModelView = (ModelView) req.getSession().getAttribute("page_precedent");
                    
                    if (previousModelView != null) {    
                        previousModelView.mergeValidationErrors(paramResult.getErrorMap());
                        previousModelView.mergeValidationValues(paramResult.getValueMap());
                        return previousModelView;
                    }
                }
                
                Object[] methodParams = paramResult.getMethodParams();
                System.out.println("methode params="+methodParams);
                Object obj = method.invoke(object, methodParams);

                // If the method returns a ModelView, store it in the session
                if (obj instanceof ModelView modelView) {
                    req.getSession().setAttribute("page_precedent", modelView);
                }

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
        System.out.println(modelView.getUrl());
        for (Map.Entry<String, Object> entry : modelView.getData().entrySet()) {
            request.setAttribute(entry.getKey(), entry.getValue());
            System.out.println(entry.getKey() + "_" + entry.getValue());
        }

        for (Map.Entry<String, String> errorEntry : modelView.getValidationErrors().entrySet()) {
            request.setAttribute(errorEntry.getKey(), errorEntry.getValue());
            System.out.println(errorEntry.getKey() + " : " + errorEntry.getValue());
        }

        for (Map.Entry<String, Object> valueEntry : modelView.getValidationValues().entrySet()) {
            request.setAttribute(valueEntry.getKey(), valueEntry.getValue());
            System.out.println( valueEntry.getKey() + " : " + valueEntry.getValue());
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
    public static StatusCode checkAuthProfil(Mapping mapping,HttpServletRequest req,String hote_name)throws CustomException.RequestException{
        String hote = "hote";
        if(hote_name != null && hote_name != ""){
            hote = hote_name;
        }
        
        if (mapping.isNeedAuth()) {
            if(!mapping.getProfil().equals(req.getSession().getAttribute(hote))){
                return new StatusCode(401,"Unauthorise",false,"");
            }
        }
        return null;
    }
    public static ResponsePage processUrl(HashMap<String, Mapping> urlMapping, PrintWriter out, HttpServletRequest req, HttpServletResponse res, ArrayList<Class<?>> controleurs,String hote_name)throws CustomException.BuildException ,
    CustomException.RequestException , Exception{
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
                    valeur.getClassName();

                    if(checkAuthProfil(valeur,req,hote_name)!=null)
                        return  new ResponsePage(checkAuthProfil(valeur,req,hote_name),html);

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
                            trouve = true;
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
        } catch (CustomException.RequestException e) {
            e.printStackTrace();
            throw e; 
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
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

    public static MethodParamResult getMethodParams(Method method, HttpServletRequest request, HttpServletResponse response) throws Exception {
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
            
            Class<?> paramType = parameters[i].getType();
            
            if (!isSimpleType(paramType)) {
                try {
                    Object paramObject = createAndPopulateObject(paramType, paramName, request);
                    methodParams[i] = paramObject;
                    
                } catch (Exception e) {
                    e.printStackTrace();
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
    
            if (parameters[i].isAnnotationPresent(framework.Annotation.Valid.class)) {
                List<ResponseValidation> errors = Contraintes.validateObject(methodParams[i]);
                for (ResponseValidation responseValidation : errors) {
                    if (!responseValidation.getErrors().isEmpty()) {
                        errorMap.put("error_" + responseValidation.getInputName(), 
                                     String.join(", ", responseValidation.getErrors()));
                        valueMap.put("value_" + responseValidation.getInputName(), 
                                     responseValidation.getValue());
                    }
                }
            } else {
                List<ResponseValidation> errors = Contraintes.valider(methodParams[i], 
                                                                      parameters[i].getAnnotations(), 
                                                                      paramName);
                if (!errors.get(0).getErrors().isEmpty()) {
                    errorMap.put("error_" + paramName, String.join(", ", errors.get(0).getErrors()));
                    valueMap.put("value_" + paramName, methodParams[i]);
                }
            }
        }
        
        return new MethodParamResult(methodParams, errorMap, valueMap);
    }
    private static boolean isSpecialType(Class<?> type) {
        return type == LocalDateTime.class 
            || type == LocalDate.class 
            || type == LocalTime.class;
    }

    private static Object createSpecialTypeInstance(Class<?> type, String value) {
        if (value == null || value == "")  
            return null;
        
        if (type == LocalDateTime.class) {
            return LocalDateTime.parse(value);
        } else if (type == LocalDate.class) {
            return LocalDate.parse(value);
        } else if (type == LocalTime.class) {
            return LocalTime.parse(value);
        }   
        return null;
    }
    
    public static Object createAndPopulateObject(Class<?> paramType, String paramName, 
    HttpServletRequest request) throws Exception {
    
        // Gérer les tableaux et les listes
        if (paramType.isArray()) {
            System.out.println("Détection d'un tableau : " + paramType.getComponentType().getName());

            Class<?> componentType = paramType.getComponentType();
            List<Object> tempList = new ArrayList<>();

            int i = 0;
            while (true) {
                String indexedParam = paramName + "[" + i + "]";
                boolean foundData = false;
                Object newObject = createAndPopulateObject(componentType, indexedParam, request);

                if (newObject != null) {
                    tempList.add(newObject);
                    foundData = true;
                }

                if (!foundData) break;
                i++;
            }

            return tempList.toArray((Object[]) Array.newInstance(componentType, tempList.size()));
        }    

        // Gérer les types spéciaux (ex: Date, LocalDateTime)
        if (isSpecialType(paramType)) {
            String paramValue = request.getParameter(paramName);
            return createSpecialTypeInstance(paramType, paramValue);
        }

        // Gérer les objets complexes
        Object paramObject;
        try {
            paramObject = paramType.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            if (paramType.isInterface()) {
                return null;
            }
            throw e;
        }

        // Itérer sur les champs de l'objet
        Field[] fields = paramType.getDeclaredFields();
        boolean hasData = false;

        for (Field field : fields) {
            String fieldName = field.getName();
            String fullParamName = paramName + "." + fieldName;
            field.setAccessible(true);

            if (isSimpleType(field.getType())) {
                String fieldValue = request.getParameter(fullParamName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    Object typedValue = convertToType(fieldValue, field.getType());
                    field.set(paramObject, typedValue);
                    hasData = true;
                }
            } else {
                Object nestedObject = createAndPopulateObject(field.getType(), fullParamName, request);
                if (nestedObject != null) {
                    field.set(paramObject, nestedObject);
                    hasData = true;
                }
            }
        }

        return hasData ? paramObject : null;
    }

// Méthode auxiliaire pour obtenir le type générique d'une liste
    private static Class<?> getGenericType(Class<?> listClass) {
        Type genericSuperclass = listClass.getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) genericSuperclass).getActualTypeArguments()[0];
        }
        return Object.class;
    }
    
    private static boolean isSimpleType(Class<?> type) {
        return type.isPrimitive() || 
               type.equals(String.class) || 
               type.equals(Session.class) || 
               type.equals(FileUpload.class) ||
               type.equals(Integer.class) ||
               type.equals(Long.class) ||
               type.equals(Double.class) ||
               type.equals(Float.class) ||
               type.equals(Date.class) ||
               type.equals(Boolean.class);
    }

    private static Object convertToType(String paramValue, Class<?> type) throws Exception {
        if (paramValue == null || type == null) {
            return null;
        }

        if (type == Integer.class || type == int.class) {
            return Integer.parseInt(paramValue);
        } else if (type == Long.class || type == long.class) {
            return Long.parseLong(paramValue);
        } else if (type == Double.class || type == double.class) {
            return Double.parseDouble(paramValue);
        } else if (type == Float.class || type == float.class) {
            return Float.parseFloat(paramValue);
        } else if (type == Boolean.class || type == boolean.class) {
            return Boolean.parseBoolean(paramValue);
        } else if (type == Date.class) {
            return Date.valueOf(paramValue);
        } else {
            return paramValue;
        }
    }

    public static String capitalize(String inputString) {
        char firstLetter = inputString.charAt(0);
        char capitalizeFirstLetter = Character.toUpperCase(firstLetter);
        return capitalizeFirstLetter + inputString.substring(1);
    }

    public static FileUpload handleFileUpload(HttpServletRequest request, String inputFileParam) throws IOException, ServletException {
        Part filePart = request.getPart(inputFileParam); 
        if(filePart==null)
            return null;
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