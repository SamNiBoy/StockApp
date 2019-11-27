<%@ page language="java" import="java.util.*" pageEncoding="utf-8"%>
<%@ page import="java.sql.*"%>
<%@ page import="javax.sql.*"%>
<%@ page import="javax.naming.*"%>
<html>
    <head>
        <title>My JSP 'index.jsp' starting page</title>
    </head>
    <body>
        
        <%
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/lamai");
            Connection conn = ds.getConnection();
            out.println(conn);
            conn.close();
        %>
    
    </body>
</html>