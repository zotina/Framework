package framework.sprint0;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.*;
import javax.servlet.http.*;

public class FrontController extends HttpServlet {
    ArrayList<Class<?>> controllers;

    public void init() throws ServletException {
        super.init();
        String controllerPackage = getServletConfig().getInitParameter("packageController");
        if (controllerPackage == null || controllerPackage.length() == 0) {
            controllerPackage = "WEB-INF/classes";
        } else {
            controllerPackage = "WEB-INF/classes/" + controllerPackage.replace('.', '/');
        }
        try {
            controllers = Util.scanClasses(controllerPackage, getServletContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void processRequest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        out.println("<HTML>");
        out.println("<HEAD><TITLE>Hello Hello</TITLE></HEAD>");
        out.println("<BODY>");
        out.println("<BIG>Bonjour tout le monde</BIG>");
        out.println("<BIG>you're here:</BIG>");
        out.println(req.getRequestURI());
        out.println("</BODY></HTML>");
        out.println(controllers);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }
}