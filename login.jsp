<%@ page language="java" import="java.io.*,java.util.*, com.sn.reporter.WeChatReporter" pageEncoding="UTF-8"%>
    <%
               String path = request.getContextPath();
    %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<link rel="stylesheet" type="text/css" href="css/common.css" />
<title>用户登录</title>
</head>
<body class="body">
<div class="header" >
<div>您好${username}, 欢迎您使用汇品苍系统</div>
</div>
<div class="content">
<form action="doLogin" method="post">
<div class="loginForm">
<h2 align="center"><font color=red>汇品苍用户登录</font></h2>
    <div class="loginInputs">
    <div class="username">
          <input class="textField" name="username" placeholder="用户名" value=""/>
    </div>
    <div class="password">
          <input type="password" class="textField" name="password" placeholder="密码" value=""/>
    </div>
    </div>
    <input class="login" type="submit" value="登录" name="login">
    <input class="reset" type="reset" value="重置" name="reset">
</div>
</form>
</div>
<div class="footer">
<div class="telephone"> 客服电话:13916638409</div>
<div class="mail">邮件: yl_nxj@163.com</div>
<div class="address">地址:上海市白银路14444号</div>
<div class="note">沪备ICP第123879号</div>
</div>
</body>
</html>