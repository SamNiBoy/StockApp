<%@ page language="java" import="java.io.*,java.util.*, com.sn.reporter.WeChatReporter" pageEncoding="UTF-8"%>
    <%
               String path = request.getContextPath();
    %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<link rel="stylesheet" type="text/css" href="css/common.css" />
<title>主页</title>
</head>
<body class="body">
<div class="header" >
<div>${username}, 您的角色是${role}</div>
<div><a class="logout" href="/doLogout" >退出</a></div>
</div>
<%
int myrole = Integer.valueOf(request.getAttribute("account_type").toString());
if (myrole == 0)
{
     out.print("<div class=\"menugroup\">");
     out.print("<div class=\"menuitem\"><a class=\"menuitem\" href=\"/doRegister\" >注册账户</a></div>");
     out.print("<div class=\"menuitem\"><a class=\"menuitem\" href=\"/doRegister\" >查询库存</a></div>");
     out.print("<div class=\"menuitem\"><a class=\"menuitem\" href=\"/doRegister\" >审批上传</a></div>");
     out.print("<div class=\"menuitem\"><a class=\"menuitem\" href=\"/doRegister\" >审批订单</a></div>");
     out.print("</div>");
}
else if (myrole == 1)
{
    out.print("<div class=\"menugroup\">");
    out.print("<div class=\"menuitem\"><a class=\"menuitem\" href=\"/doRegister\" >查询库存</a></div>");
    out.print("<div class=\"menuitem\"><a class=\"menuitem\" href=\"/doRegister\" >预定库存</a></div>");
    out.print("<div class=\"menuitem\"><a class=\"menuitem\" href=\"/doRegister\" >取消预定库存</a></div>");
    out.print("</div>");
}
else if (myrole == 2)
{
    out.print("<div class=\"menugroup\">");
    out.print("<div class=\"menuitem\"><a class=\"menuitem\" href=\"/doRegister\" >查询库存</a></div>");
    out.print("<div class=\"menuitem\"><a class=\"menuitem\" href=\"/doRegister\" >上传库存</a></div>");
    out.print("<div class=\"menuitem\"><a class=\"menuitem\" href=\"/doRegister\" >取消上传库存</a></div>");
    out.print("<div class=\"menuitem\"><a class=\"menuitem\" href=\"/doRegister\" >修改上传库存</a></div>");
    out.print("</div>");
}
%>
</div>
<div class="footer">
<div class="telephone"> 客服电话:13916638409</div>
<div class="mail">邮件: yl_nxj@163.com</div>
<div class="address">地址:上海市白银路14444号</div>
<div class="note">沪备ICP第123879号</div>
</div>
</body>
</html>