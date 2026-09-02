<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <title>Discrepancies - Reconciler</title>
    <style>
        body { font-family: Arial, sans-serif; background: #f4f5f7; margin: 0; }
        nav { background: #111827; padding: 14px 24px; display: flex; gap: 20px; align-items: center; }
        nav a { color: #d1d5db; text-decoration: none; font-size: 14px; }
        nav a.active, nav a:hover { color: #fff; }
        nav form { margin-left: auto; }
        nav button { background: none; border: none; color: #d1d5db; cursor: pointer; font-size: 14px; }
        .content { max-width: 1100px; margin: 30px auto; padding: 0 16px; }
        h1 { font-size: 20px; }
        .filters { display: flex; gap: 12px; margin-bottom: 16px; }
        .filters select, .filters input[type=text] { padding: 8px; }
        .filters button { padding: 8px 14px; background: #2563eb; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
        table { width: 100%; border-collapse: collapse; background: #fff; box-shadow: 0 1px 4px rgba(0,0,0,0.1); }
        th, td { text-align: left; padding: 10px; border-bottom: 1px solid #e5e7eb; font-size: 13px; vertical-align: top; }
        th { background: #f9fafb; }
        .badge { display: inline-block; padding: 2px 8px; border-radius: 12px; font-size: 12px; background: #e5e7eb; }
        .explain-btn { padding: 5px 10px; font-size: 12px; background: #111827; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
        .explain-box { margin-top: 8px; font-size: 12px; max-width: 320px; }
        .explain-box .loading { color: #6b7280; }
        .explain-box .error { color: #b91c1c; }
        .empty { color: #6b7280; }
    </style>
</head>
<body>
<jsp:include page="_nav.jsp"><jsp:param name="active" value="discrepancies"/></jsp:include>

<div class="content">
    <h1>Discrepancies</h1>

    <form class="filters" method="get" action="<c:url value='/discrepancies'/>">
        <select name="type">
            <option value="">All types</option>
            <c:forEach var="t" items="${types}">
                <option value="${t}" ${t == selectedType ? 'selected' : ''}>${t}</option>
            </c:forEach>
        </select>
        <input type="text" name="search" placeholder="Search order, payment ref, description" value="${search}"/>
        <button type="submit">Filter</button>
    </form>

    <c:choose>
        <c:when test="${empty discrepancies}">
            <p class="empty">No discrepancies match this filter.</p>
        </c:when>
        <c:otherwise>
            <table>
                <thead>
                <tr>
                    <th>Type</th>
                    <th>Order</th>
                    <th>Payment ref</th>
                    <th>Description</th>
                    <th>Amount at risk</th>
                    <th></th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="d" items="${discrepancies}">
                    <tr>
                        <td><span class="badge">${d.type}</span></td>
                        <td>${d.orderId}</td>
                        <td>${d.paymentRef}</td>
                        <td>${d.description}</td>
                        <td>$${d.amountAtRisk}</td>
                        <td>
                            <button class="explain-btn" onclick="explainRow(${d.id}, this)">Explain</button>
                            <div class="explain-box" id="explain-${d.id}"></div>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</div>

<script>
    async function explainRow(id, button) {
        const box = document.getElementById('explain-' + id);
        button.disabled = true;
        box.innerHTML = '<span class="loading">Asking the LLM...</span>';
        try {
            const res = await fetch('/discrepancies/' + id + '/explain', { method: 'POST' });
            const data = await res.json();
            if (!res.ok || data.error) {
                box.innerHTML = '<span class="error">' + (data.error || 'Something went wrong.') + '</span>';
            } else {
                box.innerHTML = '<strong>What happened:</strong> ' + escapeHtml(data.explanation)
                    + '<br/><strong>Do this:</strong> ' + escapeHtml(data.recommendedAction);
            }
        } catch (e) {
            box.innerHTML = '<span class="error">Network error while contacting the server.</span>';
        } finally {
            button.disabled = false;
        }
    }

    function escapeHtml(str) {
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }
</script>
</body>
</html>
