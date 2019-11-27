<%@ page language="java"
	import="java.io.*,java.util.*,java.sql.*,javax.naming.*,javax.sql.*,javax.servlet.http.*,com.sn.db.DBManager,java.text.DecimalFormat,org.apache.log4j.Logger,org.apache.log4j.PropertyConfigurator"
	pageEncoding="UTF-8"
%>
<html>
<body>
<%
Class.forName("org.gjt.mm.mysql.Driver").newInstance();
String url ="jdbc:mysql://localhost/lamai?user=root&password=mysql,16&useUnicode=true&characterEncoding=utf-8";

/* See web.xml for the datasource definition*/
//Context ctx=new InitialContext(); 
//DataSource ds=(DataSource)ctx.lookup("java:comp/env/jdbc/lamai");
//Connection conn=ds.getConnection();
Connection conn= DriverManager.getConnection(url);
Statement stmt=conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_UPDATABLE);

String sql="select * from msg";
ResultSet rs=stmt.executeQuery(sql);

while(rs.next()){
%>
name:<%=rs.getString("name")%><br>
mail:<%=rs.getString("mail")%><br>
phone:<%=rs.getString("phone")%><br>
message:<%=rs.getString("message")%>
<%
}
%>

<%out.print("Congratulations!!! JSP connect MYSQL IS OK!!");%>

<%
rs.close();
stmt.close();
conn.close();
%> 
</body>
</html>