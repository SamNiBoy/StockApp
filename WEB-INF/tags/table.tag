<%@ tag import="java.util.Date" import="java.text.DateFormat" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%

DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);
Date now = new Date(System.currentTimeMillis());
out.println(df.format(now));
%>
<meta charset="UTF-8">

<table>
<tr class="tableTitle" style="background:#aeaeff">
   <c:forTokens items="${requestScope.TW.columns}" var="item" delims="," varStatus="status">
      <td><c:out value="${item}"/></td>
  </c:forTokens>
</tr>
<c:forEach items="${requestScope.TW.data}" var="user" varStatus="status">
<c:if test="${status.count %2 == 0}">
  <tr class="tableRow" style="background:#eeeeff">
</c:if>
<c:if test="${status.count %2 != 0}">
  <tr class="tableRow" style="background:#dedeff">
</c:if>
    <td>${user.getUsername()}</td>
    <td>${user.name}</td>
    <td>${user.accounttype}</td>
    <td>${user.phone}</td>
    <td>${user.mail}</td>
    <td>${user.address}</td>
</tr>
</c:forEach>
</table>

Page Size:${requestScope.TW.page_size}
Value is:${"abc"}