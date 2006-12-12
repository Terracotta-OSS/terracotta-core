<!--

  All content copyright (c) 2003-2006 Terracotta, Inc.,
  except as may otherwise be noted in a separate copyright notice.
  All rights reserved.

-->

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN">
<!-- - - - - - - - - - - - - - -->
<!--  JMX Sample Application   -->
<!--  Terracotta, Inc.         -->
<!-- - - - - - - - - - - - - - -->
<html>
<head>
  <title>Terracotta for Spring  &bull;  JMX - <%= request.getHeader("host") %></title>
  <style>
    body  { width: 620px; font-size: 85%; font-family: trebuchet ms, sans-serif; margin: 20px 40px; }
    input { font-family: monospace; border:1px solid gray; }
  </style>
  <script type='text/javascript'>
    function startDemo() 
    {
       document.getElementById("errmsg0").style.display = 'none';
    }
  </script>
</head>
  <body onload="startDemo()">
    <h2>JMX Sample Application <%= request.getHeader("host") %> / <a href="<c:url value="/index.html"/>">Refresh</a></h2>

    <div id="errmsg0" align="center" style="color:red; background-color:yellow; border:2px solid brown; font-size:12pt; padding:5px; display:;">
       You need to have JavaScript enabled.
    </div>
    <p/>

    The JMX sample application shows how to use Terracotta for Spring to cluster Spring
    beans and AOP Advices and how to expose clustered data through JMX.
    <p/>

    The application has a <code>Counter</code> bean that is incremented when you click Increment.
    <p/>
      
    The <code>Counter</code> bean is advised using the Spring AOP interceptor <code>CounterHistoryAdvice</code>, 
    which calculates the number of clicks within each 30-second interval and retains this data for 24 hours. 
    <p/>
      
  	Click <a href="<c:url value="/index.html"/>">here</a> to refresh the current page have an updated view of the 
  	<code>Clustered Counter</code> value and captured invocation history.
    <p/>
      
    Click <a href="http://<%= request.getServerName() %>:<%= request.getServerPort() == 8081 ? 8082 : 8081 %>/jmx">here</a> 
    to switch to and increment the <code>Clustered Counter</code> value from the other Tomcat server node.
    <p/>

    Data captured in <code>Counter</code> and <code>CounterHistoryAdvice</code> beans is exported using Spring 
    JMX framework and can be accessed from the <code>JConsole</code> tool included in the JDK 1.5 (either using local 
    connection or remotely using URL <code>service:jmx:rmi:///jndi/rmi://localhost:8091/jmxrmi</code>) or any 
    other JMX-compliant tool.
    <p/>

    As with other Terracotta sample applications, you can easily cluster the web application data without writing any code. 
    <p/>
      
    When running under Terracotta for Spring you can configure classes that should be included into instrumentation, 
    as in this example, <code>Counter</code> and <code>CounterHistoryAdvice</code> beans is configured to be distributed.
    <p/>

    <table border="1" cellpadding="10" cellspacing="0">
      <tr>
        <th>&nbsp;</th>
        <th>Local</th>
        <th>Clustered</th>
      </tr>

      <tr>
        <th valign="top">Counter</th>
        <td valign="top">
        	<c:if test="${empty localCounter}">UNKNOWN</c:if>
        	<c:if test="${! empty localCounter}">
        	  <c:out value="${localCounter}"/> / <a href="<c:url value="/incrementLocal.html"/>">Increment</a>
        	</c:if>
        </td>

        <td valign="top">
        	<c:if test="${empty clusteredCounter}">UNKNOWN</c:if>
        	<c:if test="${! empty clusteredCounter}">
        	  <c:out value="${clusteredCounter}"/> / <a href="<c:url value="/incrementClustered.html"/>">Increment</a>
        	</c:if>
        </td>
      </tr>
    
      <tr>
        <th valign="top">Invocations</th>
        <td valign="top">
        	<c:if test="${empty localHistory}">No history<br/></c:if>
        	<c:if test="${! empty localHistory}">
        	  <c:forEach items="${localHistory}" var="data">
        	    <c:out value="${data}"/><br/>
        	  </c:forEach>
        	</c:if>
        </td>

        <td valign="top">
        	<c:if test="${empty clusteredHistory}">No history<br/></c:if>
        	<c:if test="${! empty clusteredHistory}">
        	  <c:forEach items="${clusteredHistory}" var="data">
        	    <c:out value="${data}"/><br/>
        	  </c:forEach>
        	</c:if>
        </td>
      </tr>
    </table>
  </body>
</html>
