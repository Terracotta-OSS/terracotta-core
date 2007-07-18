<!--

  All content copyright (c) 2003-2006 Terracotta, Inc.,
  except as may otherwise be noted in a separate copyright notice.
  All rights reserved.

-->

<%@ page isErrorPage="true" contentType="text/html" import="java.io.PrintWriter" %>
<%
    String node = System.getProperty("tc.node-name", "unknown"); 
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN">
<!-- - - - - - - - - - - - - - - -->
<!--  Events Sample Application  -->
<!--  Terracotta, Inc.           -->
<!-- - - - - - - - - - - - - - - -->
<html>
<head>
    <title>Terracotta for Spring &bull; Web Flow (Tomcat server node <%= node %>)</title>
    <link rel="shortcut icon" type="image/ico" href="http://www.terracotta.org/favicon.ico">
    <style>
        body  { width: 620px; font-size: 85%; font-family: trebuchet ms, sans-serif; margin: 20px 40px; }
        input { font-family: monospace; border:1px solid gray; }
    </style>
</head>
<body>
  <h2>Spring Web Flow: (you are on Tomcat server node <%= node %>)</h2>
  Since you've completed the survey, the Spring Webflow conversation is effectively over.
  <p/>
  The "Back" and "Forward" button actions of your browser will no longer yield to previous
  states of the survey pages.
  <p/>
  We've modified the <code>web.xml</code> file to handle a specific exception 
  (<code>org.springframework.webflow.execution.NoSuchFlowExecutionException</code>)
  to handle this Spring Webflow state and redirect you to this page.
  <p/>
  For a typical application, an end of conversation would redirect the user to some other
  logical page that is outside of the conversation flow (perhaps back to the site's home page, 
  or if it's a shopping site, then to the list of product offerings)
  <p/>
  For demonstration purposes we will just redirect you at the beginning of the survey and 
  <a href="webflow.htm?_flowId=webflow">start</a> a new conversation.
</body>
</html>
