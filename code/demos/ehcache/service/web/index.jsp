<!--

All content copyright (c) 2003-2009 Terracotta, Inc.,
except as may otherwise be noted in a separate copyright notice.
All rights reserved.

-->
<html>
<body>

<% org.terracotta.Service cache = new org.terracotta.Service(request.getRequestURL().toString()); %>

<h2>Account Balance</h2>
<% String accountNumber = "1234-00"; %>
<%
    /* in a real app, this wouldn't be done in JSP... */
    int delta = 0;
    if (request.getParameter("add") != null) {
        delta = 1;
    }
    if (request.getParameter("subtract") != null) {
        delta = -1;
    }
    if (delta != 0) {
        cache.updateAccountBalanceFor(accountNumber, delta);
    }

    org.terracotta.Service.AccountView account = cache.getAccountViewFor(accountNumber);
%>
<p><strong>Account Number:</strong> <%=account.getAccountIdentifier()%>
</p>

<p><strong>Account Balance:</strong> <%=account.getAccountBalance()%>
</p>

<p><strong>Time cached:</strong> <%=account.getTimeCached()%>
</p>

<p><strong>Creation context:</strong> <%=account.getCreationContext()%>
</p>

<p><strong><a href="?add">Add a dollar</a></strong></p>

<p><strong><a href="?subtract">Subtract a dollar</a></strong></p>

<p><strong><a href="http://localhost:9081<%=request.getRequestURI()%>">Go to Jetty 1 (9081)</a></strong></p>

<p><strong><a href="http://localhost:9082<%=request.getRequestURI()%>">Go to Jetty 2 (9082)</a></strong></p>

<h2>Application/Request Context</h2>
<table border="0">
    <tr>
        <td><strong>Server Info:</strong></td>
        <td><%=application.getServerInfo()%>
        </td>
    </tr>
    <tr>
        <td><strong>Request URI:</strong></td>
        <td><%=request.getRequestURI()%>
        </td>
    </tr>
    <%
        for (java.util.Enumeration e = request.getAttributeNames(); e.hasMoreElements();) {
            String name = (String)e.nextElement();
            Object value = request.getAttribute(name);
    %>
    <tr>
        <td><strong><%=name%>
        </strong></td>
        <td><%=value%>
        </td>
    </tr>
    <%
        }
    %>
</table>
</body>
</html>