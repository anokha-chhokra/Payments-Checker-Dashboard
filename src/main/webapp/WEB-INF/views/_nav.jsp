<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<nav>
    <a href="<c:url value='/dashboard'/>" class="${param.active == 'dashboard' ? 'active' : ''}">Dashboard</a>
    <a href="<c:url value='/discrepancies'/>" class="${param.active == 'discrepancies' ? 'active' : ''}">Discrepancies</a>
    <a href="<c:url value='/upload'/>" class="${param.active == 'upload' ? 'active' : ''}">Upload data</a>
    <form method="post" action="<c:url value='/logout'/>">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <button type="submit">Log out</button>
    </form>
</nav>
