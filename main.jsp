<%@ page language="java" import="java.io.*,java.util.*, com.sn.reporter.WeChatReporter" pageEncoding="UTF-8"%>
    <%
               String path = request.getContextPath();
    %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<link rel="stylesheet" type="text/css" href="css/common.css" />
<title>主页</title>
</head>
<body>
<div id="header" >
<div class="welcome">${username}, 您的角色是${role}</div>

<c:out value="${username}" />

<div><a class="logout" href="/doLogout" >退出</a></div>
</div>
<div id="content">
<c:if test="${requestScope.account_type==0}">
     <div class="menugroup">
     <div class="menuitem"><a class="menuitem" href="doRegister" >注册账户</a></div>
     <div class="menuitem"><a class="menuitem" href="queryUser.jsp" >查询用户</a></div>
     <div class="menuitem"><a class="menuitem" href="doRegister" >查询库存</a></div>
     <div class="menuitem"><a class="menuitem" href="doRegister" >审批上传</a></div>
     <div class="menuitem"><a class="menuitem" href="doRegister" >审批订单</a></div>
     </div>
</c:if>
<c:if test="${requestScope.account_type==1}">
    <div class="menugroup">
    <div class="menuitem"><a class="menuitem" href="doRegister" >查询库存</a></div>
    <div class="menuitem"><a class="menuitem" href="doRegister" >预定库存</a></div>
    <div class="menuitem"><a class="menuitem" href="doRegister" >取消预定库存</a></div>
    </div>
</c:if>
<c:if test="${requestScope.account_type==2}">
    <div class="menugroup">
    <div class="menuitem"><a class="menuitem" href="doRegister" >查询库存</a></div>
    <div class="menuitem"><a class="menuitem" href="doRegister" >上传库存</a></div>
    <div class="menuitem"><a class="menuitem" href="doRegister" >取消上传库存</a></div>
    <div class="menuitem"><a class="menuitem" href="doRegister" >修改上传库存</a></div>
    </div>
</c:if>
</div>
<div id="footer">
<div class="telephone"> 客服电话:13916638409</div>
<div class="mail">邮件: yl_nxj@163.com</div>
<div class="address">地址:上海市白银路14444号</div>
<div class="note">沪备ICP第123879号</div>
</div>
</body>
</html>