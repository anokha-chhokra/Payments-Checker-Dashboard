<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <title>Upload data - Reconciler</title>
    <style>
        body { font-family: Arial, sans-serif; background: #f4f5f7; margin: 0; }
        nav { background: #111827; padding: 14px 24px; display: flex; gap: 20px; align-items: center; }
        nav a { color: #d1d5db; text-decoration: none; font-size: 14px; }
        nav a.active, nav a:hover { color: #fff; }
        nav form { margin-left: auto; }
        nav button { background: none; border: none; color: #d1d5db; cursor: pointer; font-size: 14px; }
        .content { max-width: 600px; margin: 40px auto; background: #fff; padding: 28px; border-radius: 8px; box-shadow: 0 1px 4px rgba(0,0,0,0.1); }
        h1 { font-size: 20px; }
        label { display: block; margin-top: 16px; font-size: 14px; font-weight: bold; }
        input[type=file] { margin-top: 6px; }
        button.submit { margin-top: 24px; padding: 10px 20px; background: #2563eb; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
        .msg { padding: 10px; border-radius: 4px; margin-bottom: 16px; font-size: 14px; }
        .error { background: #fee2e2; color: #991b1b; }
        .success { background: #dcfce7; color: #166534; }
        p.hint { font-size: 13px; color: #6b7280; }
    </style>
</head>
<body>
<jsp:include page="_nav.jsp"><jsp:param name="active" value="upload"/></jsp:include>

<div class="content">
    <h1>Upload orders.csv and payments.csv</h1>
    <p class="hint">Uploading replaces your previously uploaded orders and payments, then reconciliation runs automatically.</p>

    <c:if test="${not empty error}"><div class="msg error">${error}</div></c:if>
    <c:if test="${not empty message}"><div class="msg success">${message}</div></c:if>

    <form method="post" action="<c:url value='/upload'/>" enctype="multipart/form-data">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <label>orders.csv
            <input type="file" name="ordersFile" accept=".csv" required/>
        </label>
        <label>payments.csv
            <input type="file" name="paymentsFile" accept=".csv" required/>
        </label>
        <button class="submit" type="submit">Upload &amp; reconcile</button>
    </form>
</div>
</body>
</html>
