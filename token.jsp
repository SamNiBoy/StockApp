<%@ page language="java"
	import="java.io.*,java.util.*,java.sql.*,com.sn.db.DBManager,java.net.URL,java.text.DecimalFormat,org.apache.log4j.Logger,org.apache.log4j.PropertyConfigurator"
	pageEncoding="UTF-8"%>
<html>
<title>Datas</title>
<body>
<%--my token is
AppID:wx23bd4a8c2f6afbef
AppSecret:d67ad4ccd309506bdad0313098dfbe42
 --%>
<%
String APPID="wx23bd4a8c2f6afbef";
String APPSECRET="d67ad4ccd309506bdad0313098dfbe42";
String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + APPID + "&secret=" + APPSECRET;
URL ul = new URL(url);
InputStream is = ul.openStream();
InputStreamReader isr = new InputStreamReader(is);
BufferedReader br = new BufferedReader(isr);
String str = "", txt ="";
while((str = br.readLine()) != null)
{
    txt += str;
    System.out.println(str);
}
System.out.println(txt);
out.print(txt);
%>
</body>
</html>