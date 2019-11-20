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
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.sn.BaseHttpServlet;
import com.sn.db.DBManager;
import com.sn.model.TableWrapper;
import com.sn.model.User;

import java.io.PrintWriter;
 
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
 
@WebServlet("/doQueryUser")
public class doQueryUser extends BaseHttpServlet{
     
    static Logger log = Logger.getLogger(doQueryUser.class);
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
        before(request, response);
        String user=request.getParameter("username");
        String name =request.getParameter("name");
        String accounttype =request.getParameter("accounttype");
        String phone =request.getParameter("phone");
        String mail =request.getParameter("mail");
        String address =request.getParameter("address");
        
        String whereCls = " where 1= 1 ";
        
        if (user != null && !user.equals(""))
        {
             whereCls += " and username like '%" + user + "%'";
        }
        if (name != null && !name.equals(""))
        {
             whereCls += " and name like '%" + name + "%'";
        }
        if (accounttype != null && !accounttype.equals(""))
        {
             whereCls += " and account_type =" + accounttype;
        }
        if (phone != null && !phone.equals(""))
        {
             whereCls += " and phone like '%" + phone + "%'";
        }
        if (mail != null && !mail.equals(""))
        {
             whereCls += " and mail like '%" + mail + "%'";
        }
        if (address != null && !address.equals(""))
        {
             whereCls += " and address like '%" + address + "%'";
        }
        
        String sql = "select * from usr " + whereCls + " order by username ";
        ResultSet rs = null;
        try{
                log.info("executing..." + sql);
                rs = _stmt.executeQuery(sql);
                
                List<Object> users = new ArrayList<Object>();
                while(rs.next()) {
                    log.info("create user:" + rs.getString("username"));
                    User u = new User(rs.getString("username"),
                            rs.getString("name"),
                            rs.getInt("account_type"),
                            rs.getString("phone"),
                            rs.getString("mail"),
                            rs.getString("address")
                            );
                   users.add(u);
                }
               log.info("total loaded:" + users.size() + " uers.");
               TableWrapper tw = new TableWrapper();
               tw.setColumns("用户名,姓名,账户,电话,邮件,地址");
               tw.setData(users);
               log.info("TW.columns:" + tw.columns);
               request.setAttribute("TW", tw);
        }catch(Exception e){
            e.printStackTrace();
        }
        request.getRequestDispatcher("listUsers.jsp").forward(request, response);
        log.info("Finished doQueryUser...");
        //after(request, response);
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
        this.doGet(request, response);
    }
}
