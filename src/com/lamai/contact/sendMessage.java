package com.lamai.contact;

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
import com.sn.mail.reporter.MailSenderFactory;
import com.sn.mail.reporter.SimpleMailSender;
import com.sn.mail.reporter.StockObserverable;

import java.io.PrintWriter;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
 
@WebServlet("/sendMessage")
public class sendMessage extends BaseHttpServlet{
     
    static Logger log = Logger.getLogger(sendMessage.class);
    static {
        PropertyConfigurator.configure("/usr/share/tomcat/webapp/LaMai/WEB-INF/conf/log4j.properties");
    }
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
        //before(request, response);
        String user=request.getParameter("name");
        String mail=request.getParameter("mail");
        String phone =request.getParameter("phone");
        String message =request.getParameter("message");
        ResultSet rs = null; 
        PrintWriter out =response.getWriter();

        // 发送邮件
        SimpleMailSender sms = MailSenderFactory.getSender();
        List<String> recipients = new ArrayList<String>();
        recipients.add("sam.ni@lamaisys.com");
        // recipients.add("samniboy@gmail.com");
        String subject = "LaMai Vistor Message";
        String content = "Message from LaMai:<br/>" +
                         "<b>Name:</b> " + user + "<br/>" +
                         "<b>Mail:</b> " + mail + "<br/>" +
                         "<b>Phone:</b>" + phone + "<br/>" +
                         "<b>Message:</b><br/>" + message;
        log.info("got mail:" + subject + "\n" + content);
        log.info("From:" + user + "\nmail:" + mail + "\nphone:" + phone);
        try {
            for (String recipient : recipients) {
                sms.send(recipient, subject, content);
            }
        } catch (AddressException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    
        /*String sql = "select * from usr where username='"+user+"'";
        try{
                log.info("executing..." + sql);
                rs=_stmt.executeQuery(sql);
                if(rs.next()){
                    sql = "select * from usr where username='"+user+"' and password='"+password+"'";
                    log.info("executing..." + sql);
                    rs=_stmt.executeQuery(sql);
                    if(rs.next()){
                        //out.print(user+"登录成功");
                        int account_type = rs.getInt("account_type");
                        request.setAttribute("username", user);
                        request.setAttribute("account_type", account_type);
                        if (account_type == 0) {
                            request.setAttribute("role", "管理员");
                        }
                        else if (account_type == 1)
                        {
                            request.setAttribute("role", "推广员");
                        }
                        else {
                            request.setAttribute("role", "品牌商");
                        }
                        request.getRequestDispatcher("main.jsp").forward(request, response);
                    }else{
                        out.print("密码输入错误！！！<br>"+"重新<a href=\"login.jsp\">登录</a>");
                    }
                }else{
                    out.print("<font color=red>"+user+"</font>用户不存在！！！<br>"+"请点击<a href=\"registered.jsp\">注册</a>");
                }
        }catch(Exception e){
            out.print(e);
        }*/
        //out.print("<font color=red>"+user+"</font>mail:" + mail + ", phone:" + phone + "<br>message:"+message+", <a href=\"contact.html>Return</a>");
        request.getRequestDispatcher("contact.html").forward(request, response);
        log.info("Finished sendMessage...");
        //after(request, response);
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
        this.doGet(request, response);
    }
}
