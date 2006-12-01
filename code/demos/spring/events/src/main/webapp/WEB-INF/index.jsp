<!--

  All content copyright (c) 2003-2006 Terracotta, Inc.,
  except as may otherwise be noted in a separate copyright notice.
  All rights reserved

-->

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN">
<!-- - - - - - - - - - - - - - - -->
<!--  Events Sample Application  -->
<!--  Terracotta, Inc.           -->
<!-- - - - - - - - - - - - - - - -->
<html>
  <head>
  <title>Terracotta for Spring  &bull;  Events - <%= request.getHeader("host") %></title>
  <style>
     body  { width: 620px; font-size: 85%; font-family: trebuchet ms, sans-serif; margin: 20px 40px; }
     input { font-family: monospace; border:1px solid gray; }
  </style>
   
  <script type='text/javascript'>
    function validateForm()
    {
      return document.getElementById("message").value.length > 0;
    }
    
    function startDemo() 
    {
       document.getElementById("errmsg0").style.display = 'none';
       document.getElementById("message").focus();
    }
  </script>
  </head>
  <body onload="startDemo()">
    <h2>Events (you are on currently on Tomcat server node <%= request.getServerPort() == 8081 ? "1" : "2" %>)</h2>

    <div id="errmsg0" align="center" style="color:red; background-color:yellow; border:2px solid brown; font-size:12pt; padding:5px; display:;">
       You need to have JavaScript enabled.
    </div>
    <p/>

  	The Events sample application shows how Terracotta for Spring propagates
  	<code>ApplicationContext</code> events to all nodes in a cluster.
    <p/>

  	Send a Message (see message history below):
  	<form onsubmit="return validateForm()" method="post" action="<c:url value="/message.html"/>">
  		<input type="hidden" name="sender" value="Tomcat server node <%= request.getServerPort() == 8081 ? "1" : "2" %>"/>
  		<input type="text" size="40" name="message" id="message"/>
  		<input type="submit" name="submit" value="Submit"/ -->
  	</form>
    <p/>

  	When you type in a message above, the message is turned into an instance of a Spring
  	<code>ApplicationEvent</code>, which is then published to the local <code>ApplicationContext</code>
  	via the <code>ApplicationContext.publishEvent(ApplicationEvent)</code> method.
    <p/>

  	Terracotta for Spring clusters the <code>ApplicationContext</code> and the events that are
  	published to it so they are available on all nodes in the cluster.  
  	<p/>
  	
  	Once you start to create a message history by using the form below, you can click 
  	<a href="http://<%= request.getServerName() %>:<%= request.getServerPort() == 8081 ? 8082 : 8081 %>/events/index.html">here</a>
  	to switch to the other Tomcat server node to verify that the events were published there, too.
  	<p/>
  	  
  	Click <a href="<c:url value="/index.html"/>">here</a> to refresh this page.
    <p/>

  	When running under Terracotta for Spring, you can configure event types <em>(such as
  	<code>demo.events.MessageEvent</code> in this sample application)</em> to be asynchronously
  	propagated across all cluster nodes.
    <p/>

    <h3>Message History</h3>
  	<c:if test="${empty events}">
  		No messages have been sent yet on any Tomcat server nodes.
  	</c:if>
  	<c:if test="${! empty events}">
      <table border="1" cellpadding="2" cellspacing="0" width="100%" style="border-collapse:collapse">
        <tr>
          <td width="50%"><b>Message</b></td>
          <td width="50%"><b>Sent By</b></td>
        </tr>
  			<c:forEach items="${events}" var="event">
    			<tr style="font-family:monospace">
    			  <td><c:out value="${event.message}"/></td>
    			  <td><c:out value="${event.source}"/> on <c:out value="${event.date}"/></td>
    			</tr>
  			</c:forEach>
  		</table>
  	</c:if>
    <p/>
  </body>
</html>
