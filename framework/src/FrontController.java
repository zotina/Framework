package framework;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import jakarta.servlet.*;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.*;


@MultipartConfig
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
            System.out.println("n of controller: " + controllers.size());
            urlMapping = Util.getUrlMapping(controllers);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void processRequest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        ResponsePage responsePage =Util.processUrl(urlMapping, out, req, res, controllers); 
        if (responsePage == null) {
            return;
        }
        StatusCode statusCode = responsePage.getStatusCode();
        out.println("Http "+statusCode.getStatus()+":"+statusCode.getName());
        try {
            Util.processStatus(statusCode);
            out.println(responsePage.getHtml());
        } catch (CustomException.BuildException e) {
            e.printStackTrace();
        } catch (CustomException.RequestException e) {
            out.println(e.getMessage());
        } catch (Exception e) {
            out.println(e.getMessage());
        }
        req.getSession().setAttribute("page",req.getRequestURI());
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }
}