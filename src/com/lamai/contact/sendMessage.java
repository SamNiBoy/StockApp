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
        log.info("LaMai Initial log4j.properties...");
        PropertyConfigurator.configure("/usr/share/tomcat/webapps/LaMai/WEB-INF/conf/log4j.properties");
    }
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
        before(request, response);
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
        log.info("mail sent");
        } catch (AddressException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    
        String sql = "insert into msg select case when max(id) is null then 1 else max(id)+1 end,'" + user + "','"+mail+"','" +phone+"','" + message+"', now() from msg";
        try{
                log.info("executing..." + sql);
                _stmt.executeUpdate(sql);
                log.info("insert msg table success..." + sql);
        }catch(Exception e){
            log.info("insert msg table failed:" + sql +"\n"+e.getMessage());
        }
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
