<%@page import="java.sql.*"%>
<%@page import="javax.sql.*"%>
<%@page import="com.sn.db.DBManager"%>
<%@page import="javax.naming.*"%>
<%request.setCharacterEncoding("utf-8"); %>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>判断登录</title>
</head>
<body>
    <%
        Context ctx = null;
        Connection con = null;
        Statement stmt = null;
        ResultSet rs =null;
        String name = request.getParameter("name").trim();
        String password = request.getParameter("password").trim();
        try{
                con = DBManager.getConnection();
                stmt = con.createStatement();
                rs=stmt.executeQuery("select * from user where name='"+name+"'");
                if(rs.next()){
                    rs=stmt.executeQuery("select * from user where name='"+name+"' and password='"+password+"'");
                    if(rs.next()){
                        out.print(name+"登录成功");
                    }else{
                        out.print("密码输入错误！！！<br>"+"重新<a href=\"login.jsp\">登录</a>");
                    }
                }else{
                    out.print("<font color=red>"+name+"</font>用户不存在！！！<br>"+"请点击<a href=\"registered.jsp\">注册</a>");
                }
        }catch(Exception e){
            out.print(e);
        }finally{
            if(rs!=null)
                rs.close();
            if(stmt!=null)
                stmt.close();
            if(con!=null)
                con.close();
        }
    %>
</body>
</html>