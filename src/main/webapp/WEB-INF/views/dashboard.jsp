<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <title>Dashboard - Reconciler</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4"></script>
    <style>
        body { font-family: Arial, sans-serif; background: #f4f5f7; margin: 0; }
        nav { background: #111827; padding: 14px 24px; display: flex; gap: 20px; align-items: center; }
        nav a { color: #d1d5db; text-decoration: none; font-size: 14px; }
        nav a.active, nav a:hover { color: #fff; }
        nav form { margin-left: auto; }
        nav button { background: none; border: none; color: #d1d5db; cursor: pointer; font-size: 14px; }
        .content { max-width: 900px; margin: 30px auto; padding: 0 16px; }
        h1 { font-size: 20px; }
        .cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; margin: 20px 0; }
        .card { background: #fff; padding: 18px; border-radius: 8px; box-shadow: 0 1px 4px rgba(0,0,0,0.1); }
        .card .label { font-size: 13px; color: #6b7280; }
        .card .value { font-size: 22px; font-weight: bold; margin-top: 6px; }
        .risk .value { color: #b91c1c; }
        .panel { background: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 1px 4px rgba(0,0,0,0.1); margin-bottom: 20px; }
        canvas { max-height: 300px; }
        .empty { color: #6b7280; }
        a.link { color: #2563eb; text-decoration: none; font-size: 14px; }
    </style>
</head>
<body>
<jsp:include page="_nav.jsp"><jsp:param name="active" value="dashboard"/></jsp:include>

<div class="content">
    <h1>Reconciliation dashboard</h1>

    <c:choose>
        <c:when test="${totalOrders == 0}">
            <p class="empty">No data yet. <a class="link" href="<c:url value='/upload'/>">Upload orders.csv and payments.csv</a> to get started.</p>
        </c:when>
        <c:otherwise>
            <div class="cards">
                <div class="card"><div class="label">Total orders</div><div class="value">${totalOrders}</div></div>
                <div class="card"><div class="label">Total payments</div><div class="value">${totalPayments}</div></div>
                <div class="card"><div class="label">Value reconciled</div><div class="value">$<fmt:formatNumber value="${totalReconciled}" minFractionDigits="2" maxFractionDigits="2"/></div></div>
                <div class="card"><div class="label">Value in dispute</div><div class="value">$<fmt:formatNumber value="${totalInDispute}" minFractionDigits="2" maxFractionDigits="2"/></div></div>
                <div class="card risk"><div class="label">Money at risk</div><div class="value">$<fmt:formatNumber value="${moneyAtRisk}" minFractionDigits="2" maxFractionDigits="2"/></div></div>
                <div class="card"><div class="label">Discrepancies found</div><div class="value">${totalDiscrepancies}</div></div>
            </div>

            <div class="panel">
                <h2 style="font-size:16px;margin-top:0;">Discrepancies by type</h2>
                <canvas id="breakdownChart"></canvas>
            </div>

            <p><a class="link" href="<c:url value='/discrepancies'/>">See every discrepancy &rarr;</a></p>
        </c:otherwise>
    </c:choose>
</div>

<script>
    const labels = ${breakdownLabelsJson};
    const values = ${breakdownValuesJson};
    const ctx = document.getElementById('breakdownChart');
    if (ctx) {
        new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{ label: 'Count', data: values, backgroundColor: '#2563eb' }]
            },
            options: {
                plugins: { legend: { display: false } },
                scales: { y: { beginAtZero: true, ticks: { precision: 0 } } }
            }
        });
    }
</script>
</body>
</html>
