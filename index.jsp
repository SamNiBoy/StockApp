<%@ page language="java" import="java.io.*,java.util.*, com.sn.reporter.WeChatReporter" pageEncoding="UTF-8"%>
    <%
               String path = request.getContextPath();
    %>
<%
out.println("samni, welcome");
BufferedReader br = new BufferedReader(new InputStreamReader((ServletInputStream)request.getInputStream()));
String line = null;
StringBuilder sb = new StringBuilder();
while((line = br.readLine())!=null){
   sb.append(line);
}
String xmlS = sb.toString();
System.out.println(xmlS);
WeChatReporter wcr = new WeChatReporter();
System.out.println("after wechat create");
wcr.setWcMsg(xmlS);
String msg = wcr.getResponse();
out.print(msg);
%>
