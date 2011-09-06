<!--

  All content copyright (c) 2003-2006 Terracotta, Inc.,
  except as may otherwise be noted in a separate copyright notice.
  All rights reserved.

-->

<%@ page contentType="text/html; charset=utf-8" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN">
<!-- - - - - - - - - - - - - - - -->
<!--  Department Task List Demo  -->
<!--  Terracotta, Inc.           -->
<!-- - - - - - - - - - - - - - - -->
<html:html>
<head>
<html:base />
<title>Department Task List  &bull;  Sharable To Do List</title>
<style>
   body        { font-size: 85%; font-family: trebuchet ms, sans-serif;
                 margin: 0px; }
   h1          { font-family: impact, american typewriter, sans-serif;
                 letter-spacing: 0.1em; margin: 0px }
   h2          { font-size: 110%; font-weight: normal; margin: 0px }
   h3          { text-align: center; border-bottom: 2px dotted gray;
                 margin: 0px 0px 10px 0px }
   div.main    { width: 500px; background-color: LightSteelBlue;
                 border: 2px MidnightBlue dotted; padding: 40px;
                 margin: 50px auto }
   div.box     { float: left; margin-top: 30px }
   div.sidebar { float: right; background-color: gainsboro;
                 border: 1px midnightblue solid; padding: 20px }
   div.footer  { text-align: center; font-size: 75%; margin-top: 40px }
   a.footer    { color: saddlebrown; text-decoration: none }
   br.all      { clear: both }
   .button_add { text-align: right; margin-top: 4px }
   .warning    { color: red; margin-top: 30px }
   </style>
<link rel="shortcut icon" type="image/ico"
   href="http://www.terracotta.org/favicon.ico">
</head>
<body>


<!-- - - - - - - - - - - - - - - - - - -->
<!--  Session Clustering Information   -->
<!--  Terracotta Sessions              -->
<!-- - - - - - - - - - - - - - - - - - -->
<script>
dsoEnabled = <%= Boolean.getBoolean("tc.active") %>;
cartSize = <bean:write name="displayUserListForm" property="numTasks"/>;
server1 = "9081";
server2 = "9082";
currentServer = location.href.indexOf(server1) == -1 ? server2 : server1;
otherServer = currentServer == server1 ? server2 : server1;
serverColor = currentServer == server1 ? "goldenrod" : "darkseagreen";
msgNoDsoEmptyCart = 'This web application is currently running in regular non-clustered mode, and the task list is empty.&nbsp;  Use the "add" button to put an item on the task list.<br><br>If you just jumped over from the other server, the task list is empty because the session data is not being shared between servers.&nbsp;  Try running with Terracotta Sessions enabled.';
msgNoDsoFullCart =  'This web application is currently running in regular non-clustered mode, and you have added an item to the task list.&nbsp;  Now click the link on the left to jump over to the other server.&nbsp;  Since the session data is not being shared across servers, the task list will be lost when you jump to the other server.';
msgDsoEmptyCart =   'This web application is currently running clustered on Terracotta Sessions.&nbsp;  The task list is empty.&nbsp;  Use the "add" button to put an item on the task list.&nbsp;  Terracotta Sessions makes the session data transparently available to the other server, so the task list and its contents will be maintained if you jump to another server.';
msgDsoFullCart  =   'This web application is currently running clustered on Terracotta Sessions, and you have added an item to the task list.<br><br>You can click the link on the left to jump over to the other server.&nbsp;  Since the session data is being shared across servers, the task list will be preserved.<br><br>If you just jumped over from the other server, the task list has maintained its contents because Terracotta Sessions replicated the session data.&nbsp;  To view the session data, connect to the cluster using the Developer Console, then click the <bold>Sessions</bold> button to open the <bold>Sessions</bold> panels under the <bold>My application</bold> node.';
function getMsg() {
   if (!dsoEnabled && cartSize==0)
      return msgNoDsoEmptyCart;
   else if (!dsoEnabled && cartSize>0)
      return msgNoDsoFullCart;
   else if (dsoEnabled && cartSize==0)
      return msgDsoEmptyCart;
   else
      return msgDsoFullCart;
   }
rowStart =  "<tr><td style='text-align: right'><nobr>";
rowMiddle = "</nobr></td><td><nobr><b>";
rowEnd =    "</b></nobr></td></tr>";
document.writeln(
   '<div style="background: ' + serverColor + '; font-size: 90%; ' +
      'font-family: sans-serif; border: 3px solid gray; ' +
      'padding: 15px; margin: 0px 80px">' +
   '<table style="font-size: 100%"><tr style="vertical-align: top"><td>' +
   '<table style="font-size: 100%">' +
   rowStart + "Current Server:" + rowMiddle + currentServer + rowEnd +
   rowStart + 'Terracotta Sessions:' + rowMiddle +
      (dsoEnabled ? 'On' : 'Off') + rowEnd +
   rowStart + 'Items in Cart:' + rowMiddle + cartSize + rowEnd +
   rowStart + 'Go to:' + rowMiddle + '<a href="http://' + location.hostname +
      ':' + otherServer + '/DepartmentTaskList">Server ' + otherServer +
      '</a>' + rowEnd +
   '</table></td><td style="padding-left: 15px">' +
   '<b>Session Clustering Information</b><br><br>' + getMsg() +
   '</td></tr></table></div>');
</script>

<div class=main>
<h1>Department Task List</h1>
<h2>Sharable To Do List</h2>
<div class=sidebar>
   <html:form action="deleteFromList">
      <h3>To Do List</h3>
      <logic:iterate id="listItem" name="displayUserListForm" property="userList">
         <html:multibox property="itemsForDelete"><bean:write name="listItem" />
            </html:multibox>
         <bean:write name="listItem" /><br>
         </logic:iterate>
      <br>
      <html:submit value="Remove Selected Tasks" />
      </html:form>
   </div>
<div class=box>
   <html:form action="addToList">
      Enter a new task to be added:<br>
      <html:text property="newListItem" size="30" maxlength="30" /><br>
      <div class="button_add"><html:submit value="Add Task" /></div>
      </html:form>
   </div>
<br class=all>
<div class=warning><bean:write name="displayUserListForm" property="errorMsg" /></div>
</div>


<div class=footer>
   Powered by<br>
   <a class=footer href="http://www.terracotta.org/">TERRACOTTA</a>
   </div>
</body>
</html:html>
