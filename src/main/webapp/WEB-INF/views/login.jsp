<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <title>Log in - Reconciler</title>
    <style>
        body { font-family: Arial, sans-serif; background: #f4f5f7; display: flex; justify-content: center; padding-top: 80px; }
        .box { background: #fff; padding: 32px; border-radius: 8px; box-shadow: 0 1px 4px rgba(0,0,0,0.15); width: 320px; }
        h1 { font-size: 20px; margin-bottom: 20px; }
        label { display: block; margin-top: 12px; font-size: 14px; color: #333; }
        input[type=email], input[type=password] { width: 100%; padding: 8px; margin-top: 4px; box-sizing: border-box; }
        button { margin-top: 20px; width: 100%; padding: 10px; background: #2563eb; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
        .msg { padding: 8px; border-radius: 4px; margin-bottom: 12px; font-size: 14px; }
        .error { background: #fee2e2; color: #991b1b; }
        .success { background: #dcfce7; color: #166534; }
        a { color: #2563eb; }
    </style>
</head>
<body>
<div class="box">
    <h1>Reconciliation Dashboard</h1>

    <c:if test="${not empty created}">
        <div class="msg success">Account created. You can log in now.</div>
    </c:if>
    <c:if test="${param.error != null}">
        <div class="msg error">Invalid email or password.</div>
    </c:if>
    <c:if test="${param.logout != null}">
        <div class="msg success">Logged out.</div>
    </c:if>

    <form method="post" action="<c:url value='/login'/>">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <label>Email
            <input type="email" name="username" required autofocus/>
        </label>
        <label>Password
            <input type="password" name="password" required/>
        </label>
        <button type="submit">Log in</button>
    </form>
    <p style="margin-top:16px;font-size:14px;">No account? <a href="<c:url value='/signup'/>">Sign up</a></p>
</div>
</body>
</html>
