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
 
@WebServlet("/doCreateUser")
public class doCreateUser extends BaseHttpServlet{
     
    static Logger log = Logger.getLogger(doLogin.class);
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
        before(request, response);
        String user=request.getParameter("username");
        String password=request.getParameter("password");
        String name =request.getParameter("name");
        String accounttype =request.getParameter("accounttype");
        String phone =request.getParameter("phone");
        String mail =request.getParameter("mail");
        String address =request.getParameter("address");
        PrintWriter out =response.getWriter();
        String sql = "insert into usr values('" + user + "', '" + password + "', '" + name + "', " + accounttype + ", '" + mail + "', '"
                     + phone + "', '" + address + "', sysdate)";
        try{
                log.info("executing..." + sql);
                _stmt.executeQuery(sql);
        }catch(Exception e){
            out.print(e);
        }
        request.getRequestDispatcher("registerSuccess.jsp").forward(request, response);
        log.info("Finished doCreateUser...");
        //after(request, response);
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
        this.doGet(request, response);
    }
}
