<!--

  All content copyright (c) 2003-2006 Terracotta, Inc.,
  except as may otherwise be noted in a separate copyright notice.
  All rights reserved

-->

<%@ page contentType="text/html" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>

<%
    String node = System.getProperty("tc.node.name", "unknown"); 
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN">
<!-- - - - - - - - - - - - - - - -->
<!--  Events Sample Application  -->
<!--  Terracotta, Inc.           -->
<!-- - - - - - - - - - - - - - - -->
<html>
<head>
    <title>Terracotta for Spring &bull; Web Flow (Server <%= node %>)</title>
    <link rel="shortcut icon" type="image/ico" href="http://www.terracotta.org/favicon.ico">
    <style>
        body  { width: 620px; font-size: 85%; font-family: trebuchet ms, sans-serif; margin: 20px 40px; }
        input { font-family: monospace; border:1px solid gray; }
        samp  { font-family: monospace; }
    </style>
    <script type='text/javascript' language="JavaScript">
      // IE workaround
      function checkEnter(e) 
      {
    	  var ua   = window.navigator.userAgent;
    	  var msie = ua.indexOf( "MSIE " );
    	  if (msie == -1)
    	    return true;
    	  
        var keycode;
        e       = event;
        keycode = e.keyCode;
        if (keycode == 13) 
        {
          document.getElementById("_eventId_submit").click();
          return false;
        }
        return true 
      }
      
      function startDemo() 
      {
        document.getElementById("errmsg0").style.display = 'none';
        document.getElementById("field0").focus();
      }
    </script>
</head>
<body onload="startDemo()">
  <h2>Spring Web Flow: (you are on Server <%= node %>)</h2>

  <div id="errmsg0" align="center" style="color:red; background-color:yellow; border:2px solid brown; font-size:12pt; padding:5px; display:;">
     You need to have JavaScript enabled.
  </div>
  <p/>

  The Web Flow sample application shows how Spring Web Flow with continuation support can be
  clustered with Terracotta for Spring to enable failover of the conversation state.
  <p/>

  At any point you may stop/start/restart any or all of the web server nodes (the title
  above will tell you which one served your current request) and observe that Terracotta for Spring
  maintains your conversation state properly, regardless of which server node served the
  request.
  <p/>

  If you stop both server nodes you must restart at least one of them for the
  application to work; but for demonstration purposes you can stop both nodes to see that
  the Terracotta Server maintains the state even when no server nodes are running, then
  allows them to resume with the current conversation state as they are restarted.
  <p/>

  You can stop the server nodes individually by issuing the appropriate <code>stop-tomcat</code>
  command from the <code>spring/samples/webflow</code> directory. For example, <code>./stop-tomcat1.sh</code>
  (or <code>stop-tomcat1.bat</code> in Windows) will stop the Tomcat server instance from node1.
  <p/>
    
  <c:choose>
    <c:when test="${webflow.state == null or webflow.state == 'stateA'}">
      <h3>Running the application</h3>
      The application will ask you four general questions in order; you can use the web browser
      "Back" and "Forward" buttons to navigate through the conversation and go back and change your
      answers. 
      <p/>
    </c:when>
    <c:otherwise>
      <c:choose>
        <c:when test="${webflow.state != 'complete'}">
          <h3><c:out value="${webflow.a}"/>, please continue by answering the next question</h3>
          You may use your browser "Back" and "Forward" buttons to go back and change some of
          your answers, while maintaining your conversation state across multiple server nodes.
          <p/>
        </c:when>
        <c:otherwise>
          <h3>Thank you for completing the sample survey</h3>
          You may restart the survey by clicking the 'Restart' button.
          <p/>
        </c:otherwise>
      </c:choose>
    </c:otherwise>
  </c:choose>

  <form action="webflow.htm" name="myform" id="myform" method="post">
    <c:if test="${webflow.state != 'complete'}">
      <input type="hidden" name="_flowExecutionKey" value='<c:out value="${flowExecutionKey}"/>'/>
    </c:if>
    
    <table border="1" cellpadding="2" cellspacing="0" style="border-collapse:collapse">
      <c:choose>
        <c:when test="${webflow.state != null and webflow.state != 'stateA'}">
          <th>Survey question</th>
          <th colspan="2">Your answer</th>
        </c:when>
      </c:choose>
      <c:choose>
        <c:when test="${webflow.state == null or webflow.state == 'stateA'}">
          <tr>
            <td bgcolor="#eaeaea">What is your name?</td>
            <td bgcolor="#eaeaea" valign="middle">
              <input type="text" name="value" value="<c:out value='${webflow.a}'/>" id="field0" onKeyPress="return checkEnter(event)" style="width:300px"/>
            </td>
            <td bgcolor="#eaeaea" valign="middle">
              <input type="submit" id="_eventId_submit" name="_eventId_submit" value="Continue"/>
            </td>
          </tr>
        </c:when>
        <c:otherwise>
          <tr>
            <td>Your name</td>
            <td colspan="2" style="width:300px"><c:out value="${webflow.a}"/><c:if test="${webflow.a == null}">&nbsp;</c:if></td>
          </tr>
        </c:otherwise>
      </c:choose>
      <c:choose>
        <c:when test="${webflow.state == 'stateB'}">
          <tr>
            <td bgcolor="#eaeaea">What is your favorite color?</td>
            <td bgcolor="#eaeaea">
              <input type="text" name="value" value="<c:out value='${webflow.b}'/>" id="field0" onKeyPress="return checkEnter(event)" style="width:300px"/>
            </td>
            <td bgcolor="#eaeaea" valign="middle">
              <input type="submit" id="_eventId_submit" name="_eventId_submit" value="Continue"/>
            </td>
          </tr>
        </c:when>
        <c:when test="${webflow.state != null and webflow.state != 'stateA' and webflow.state != 'stateB'}">
          <tr>
            <td>Your favorite color</td>
            <td colspan="2" style="width:300px"><c:out value="${webflow.b}"/><c:if test="${webflow.b==null}">&nbsp;</c:if></td>
          </tr>
        </c:when>
      </c:choose>
      <c:choose>
        <c:when test="${webflow.state == 'stateC'}">
          <tr>
            <td bgcolor="#eaeaea">What is your favorite food?</td>
            <td bgcolor="#eaeaea">
              <input type="text" name="value" value="<c:out value='${webflow.c}'/>" id="field0" onKeyPress="return checkEnter(event)" style="width:300px"/>
            </td>
            <td bgcolor="#eaeaea" valign="middle">
              <input type="submit" id="_eventId_submit" name="_eventId_submit" value="Continue"/>
            </td>
          </tr>
        </c:when>
        <c:when test="${webflow.state != null and webflow.state != 'stateA' and webflow.state != 'stateB' and webflow.state != 'stateC'}">
          <tr>
            <td>Your favorite food</td>
            <td colspan="2" style="width:300px"><c:out value="${webflow.c}"/><c:if test="${webflow.c == null}">&nbsp;</c:if></td>
          </tr>
        </c:when>
      </c:choose>
      <c:choose>
        <c:when test="${webflow.state == 'stateD'}">
          <tr>
            <td bgcolor="#eaeaea">What is your favorite kind of music?</td>
            <td bgcolor="#eaeaea">
              <input type="text" name="value" value="<c:out value='${webflow.d}'/>" id="field0" onKeyPress="return checkEnter(event)" style="width:300px"/>
            </td>
            <td bgcolor="#eaeaea" valign="middle">
              <input type="submit" id="_eventId_submit" name="_eventId_submit" value="Continue"/>
            </td>
          </tr>
        </c:when>
        <c:when test="${webflow.state != null and webflow.state != 'stateA' and webflow.state != 'stateB' and webflow.state != 'stateC' and webflow.state != 'stateD'}">
          <tr>
            <td>Your favorite kind of music</td>
            <td colspan="2" style="width:300px"><c:out value="${webflow.d}"/><c:if test="${webflow.d == null}">&nbsp;</c:if></td>
          </tr>
        </c:when>
      </c:choose>
    </table>
  </form>
  <p/>
  <a href="webflow.htm?_flowId=webflow">Restart</a>
  <p/>
</body>
</html>
