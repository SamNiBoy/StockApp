<%@ page language="java" import="java.io.*,java.util.*, com.sn.reporter.WeChatReporter" pageEncoding="UTF-8"%>
    <%
               String path = request.getContextPath();
    %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="easy" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">

<link rel="stylesheet" type="text/css" href="css/common.css" />
<title>用户查询结果</title>
</head>
<body>
<div id="header" >
<div class="welcome">${username}, 您的角色是${role}</div>

<div><a class="logout" href="/doLogout" >退出</a></div>
</div>
<div id="content" border="solid">
<easy:table/>
<div class="centerTable">
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