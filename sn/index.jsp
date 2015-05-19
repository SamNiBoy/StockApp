<%@ page language="java" import="java.io.*,java.util.*, com.sn.reporter.WeChatReporter" pageEncoding="UTF-8"%>
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
%>

<%
WeChatReporter wcr = new WeChatReporter();
wcr.setWcMsg(xmlS);
out.print(wcr.getResponse());
System.out.println(wcr.getResponse());
%>

