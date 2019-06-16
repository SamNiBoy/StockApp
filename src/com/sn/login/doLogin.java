package com.sn.login;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.log4j.Logger;
import com.sn.db.DBManager;
import java.io.PrintWriter;
 
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
 
@WebServlet("/doLogin")
public class doLogin extends HttpServlet{
    private static final String DEFAULT_USER="rzm";
     
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String user=request.getParameter("name");
        String password=request.getParameter("password");
         
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer=response.getWriter();
        writer.append("<!DOCTYPE html>")
        .append("<html><head></head><body>")
        .append("用户是"+user + ", 密码是2:" + password)
        .append("</body></html>");
        response.sendRedirect("/HPCApp/loginSuccess.jsp?name=nxj&password=123");
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        this.doGet(request, response);
    }
}
