<%@ page language="java" import="java.io.*,java.util.*, com.sn.reporter.WeChatReporter" pageEncoding="UTF-8"%>
    <%
               String path = request.getContextPath();
    %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<link rel="stylesheet" type="text/css" href="css/common.css" />
<link rel="stylesheet" type="text/css" href="css/register.css" />
<title>用户注册</title>
</head>
<body>
<div id="header" >
<div>用户注册</div>
</div>
<div id="content">
<div id="CreateUserForm">
<h3 align="left"><font color=green>用户注册</font></h3>
<form name="tijiao" method="post" onsubmit="return check()" action="doCreateUser">
        <div class="item">用户名:</div>
            <div class="kong">
                <input id="text1" type="text" name="username" placeholder="请输入用户名" onblur="check()"><span id="div1" class="tian" style="margin-top: 4px">*<span>
            </div>
        <div class="item">密码:</div>
            <div class="kong">
                <input id="text2" type="password" name="password" onblur="check()">
                <span id="div2" class="tian" style="margin-top: -25px">*<span>
            </div>
        <div class="item">确认密码:</div>
            <div class="kong">
                <input id="text3" type="password" name="confirm_password" onblur="check()">
                <span id="div3" class="tian">*<span>
            </div>
        <div class="item">真实姓名:</div>
            <div class="kong">
                <input id="i2" type="text" name="name">
            </div>
        <div class="item">账户类型:</div>
        <div id="accounttype">
            <input type="radio" name="accounttype" value="0" />管理员
            <input type="radio" name="accounttype" value="1" checked="1"/>推广员
            <input type="radio" name="accounttype" value="2" />品牌商
            <input type="radio" name="accounttype" value="3" />切货商
            <input type="radio" name="accounttype" value="4" />代理商
        </div>
        <div class="item">电话号码:</div>
            <div class="kong">
                <input id="i5" type="text" name="phone">
            </div>
        <div class="item">邮箱地址:</div>
            <div class="kong">
                <input id="text4" type="text" name="mail" onblur="check()">
                <span id="div4" class="tian">*<span>
            </div>
        <div class="item">地址:</div>
        <div class="kong">
            <textarea name="address" style="width: 280px;height: 40px;"></textarea>
        </div>
<div class="can">
    <input id="i111" type="submit" name="002" value="注  册">
    <input id="i222" type="reset" name="004" value="重置">
</div>
</form>
</div>
</div>
<div id="footer">
<div class="telephone"> 客服电话:13916638409</div>
<div class="mail">邮件: yl_nxj@163.com</div>
<div class="address">地址:上海市白银路14444号</div>
<div class="note">沪备ICP第123879号</div>
</div>
</body>
</html>