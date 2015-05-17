<%@ page language="java" import="java.io.*,java.util.*" pageEncoding="UTF-8"%>
    <%
               String path = request.getContextPath();
    %>
<%
//out.println("samni, welcome");
BufferedReader br = new BufferedReader(new InputStreamReader((ServletInputStream)request.getInputStream()));
String line = null;
StringBuilder sb = new StringBuilder();
while((line = br.readLine())!=null){
   sb.append(line);
}
String xmlS = sb.toString();
/*
<xml>
<ToUserName>
<![CDATA[gh_a9586d5aa590]]>
</ToUserName>

<FromUserName>
<![CDATA[osCWfs-ZVQZfrjRK0ml-eEpzeop0]]>
</FromUserName>

<CreateTime>
1431827441
</CreateTime>

<MsgType>
<![CDATA[text]]>
</MsgType>

<Content>
<![CDATA[Standard ]]>
</Content>

<MsgId>
6149652032815793242
</MsgId>

</xml>
*/
System.out.println(xmlS);

if(xmlS !=null && !xmlS.equals("")){

int fromuser_s = xmlS.indexOf("<FromUserName><![CDATA[");
int fromuser_e = xmlS.indexOf("]]></FromUserName>");
String fromuser = xmlS.substring(fromuser_s + 23, fromuser_e);

int touser_s = xmlS.indexOf("<ToUserName><![CDATA[");
int touser_e = xmlS.indexOf("]]></ToUserName>");
String touser = xmlS.substring(touser_s + 21, touser_e);

int content_s = xmlS.indexOf("<Content><![CDATA[");
int content_e = xmlS.indexOf("]]></Content>");

String content = xmlS.substring(content_s + 18, content_e);

out.print("<xml>");
out.print("    <ToUserName><![CDATA["+fromuser+"]]></ToUserName>");
out.print("    <FromUserName><![CDATA["+touser+"]]></FromUserName>");
out.print("    <CreateTime>"+new Date().getTime()+"</CreateTime>");
out.print("    <MsgType><![CDATA[text]]></MsgType>");
out.print("    <Content><![CDATA[Welcome" + fromuser + " to " + touser + " your msg is " + content + "!]]></Content>");
out.print("    <FuncFlag>0</FuncFlag>");
out.print("</xml>"); 
}
%>
