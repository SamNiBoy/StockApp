package com.sn;

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
import java.io.PrintWriter;
 
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
 
public class BaseHttpServlet extends HttpServlet{
    protected Connection _con = null;
    protected Statement _stmt = null;
    protected void before(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
        request.setCharacterEncoding("utf-8");
        response.setHeader("Content-type", "text/html;charset=UTF-8");
        response.setCharacterEncoding("utf-8");
        try {
            /*if (_con == null) {
                    _con = DBManager.getConnection();
                    _stmt = _con.createStatement();
            }*/
           
        }
        catch(Exception e) {
            throw new ServletException("Can not get db connection from BaseHttpServlet!");
        }
        
    }
    protected void after(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
        try {
            //_con.commit();
            /*if(_stmt!=null)
                _stmt.close();
            if(_con!=null)
                _con.close();*/
        }
        catch(Exception e) {
            throw new ServletException("Can not close db connection from BaseHttpServlet!");
        }
    }
}
