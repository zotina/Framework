package framework.sprint0;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

public class FrontController extends HttpServlet {
    private ArrayList<Class<?>> controllers;
    private HashMap<String, Mapping> urlMapping;

    public void init() throws ServletException {
        super.init();
        String controllerPackage = getServletConfig().getInitParameter("packageController");
        if (controllerPackage == null || controllerPackage.length() == 0) {
            controllerPackage = "WEB-INF/classes";
        } else {
            controllerPackage = "WEB-INF/classes/" + controllerPackage.replace('.', '/');
        }
        try {
            controllers = Util.scanClasses(controllerPackage, getServletContext(), Annotation.Controller.class);
            urlMapping = Util.getUrlMapping(controllers);
        } catch (

        Exception e) {
            e.printStackTrace();
        }
    }

    public void processRequest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        boolean test = false;
        String requestURI = Util.removeRootSegment(req.getRequestURI());
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        out.println("<HTML>");
        out.println("<HEAD><TITLE>Hello Hello</TITLE></HEAD>");
        out.println("<BODY>");
        out.println("</br>");
        out.println("<BIG>URL:</BIG>");
        out.println(requestURI);
        out.println("</br>");
        out.println("<BIG>CONTROLLER:</BIG>" + controllers);
        out.println("</br>");
        for (Map.Entry<String, Mapping> entry : urlMapping.entrySet()) {
            String key = entry.getKey();
            Mapping value = entry.getValue();

            if (key.equals(requestURI)) {
                out.println("<BIG><p>URLMAPPING:</BIG>" + value.getClassName() + "_"
                        + value.getMethodeName() + "</p>");
                out.println("</br>");
                out.println("<BIG><p>MethodeValue:</BIG>"
                        + (String) Util.getValueMethod(value.getMethodeName(), value.getClassName()) + "</p>");
                test = true;
                break;
            }

        }
        out.println(!Util.isRoot(req.getRequestURI()) && !test ? "<BIG style=\"color: red;\" >URL NOT FOUND<BIG>" : "");
        out.println("</BODY></HTML>");
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }
}