package com.sn.login;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.log4j.Logger;

import com.sn.BaseHttpServlet;
import com.sn.db.DBManager;
import java.io.PrintWriter;
 
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
 
@WebServlet("/doRegister")
public class doRegister extends BaseHttpServlet{
     
    static Logger log = Logger.getLogger(doLogin.class);
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
            request.getRequestDispatcher("register.jsp").forward(request, response);
            log.info("Finished doRegister...");
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
        this.doGet(request, response);
    }
}
