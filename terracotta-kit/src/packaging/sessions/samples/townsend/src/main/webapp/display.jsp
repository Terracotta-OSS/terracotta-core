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
<!-- - - - - - - - - - - - - - - - - - -->
<!--  Townsend Camera and Supply Demo  -->
<!--  Terracotta, Inc                  -->
<!-- - - - - - - - - - - - - - - - - - -->
<html:html>
<head>
<html:base />
<title>Townsend Camera and Supply  &bull;  Online Product Catalog</title>
<style>
   body       { color: navy; background-color: navy; margin: 0px;
                font-size: 85%; font-family: trebuchet ms, sans-serif; }
   a:hover    { color: dodgerblue }
   img        { border-style: none }
   h1         { font-family: impact, american typewriter, sans-serif;
                letter-spacing: 0.05em; margin: 0px }
   h2         { color: gray; font-size: 90%; margin: 0px 0px 30px 0px }
   div.main   { width: 460px; background-color: whitesmoke;
                border: 2px blue solid; padding: 40px;
                margin: 10px 10px 50px 100px }
   div.footer { text-align: center; font-size: 75% }
   a.footer   { color: dimgray; text-decoration: none }
   legend     { font-weight: bold; padding: 0px 7px }
   fieldset.camera { text-align: center }
   fieldset.recent { text-align: right }
   form            { display: inline }
   input.camera    { border: 1px solid gray; margin:  10px }
   tr.second_row   { vertical-align: top; height: 160px }
   td.details_box  { width: 100% }
   img.details     { float: right; border: 2px solid black; margin-left: 10px }
   br.all          { clear: both }
   </style>
<link rel="shortcut icon" type="image/ico" href="favicon.ico">
</head>
<body>


﻿<!-- - - - - - - - - - - - - - - - - - -->
<!--  Session Clustering Information   -->
<!--  Terracotta Sessions              -->
﻿<!-- - - - - - - - - - - - - - - - - - -->
<script>
dsoEnabled = <%= Boolean.getBoolean("tc.active") %>;
cartSize = <bean:write name="displayUserListForm" property="listLength"/>;
server1 = '9081';
server2 = '9082';
currentServer = location.href.indexOf(server1) == -1 ? server2 : server1;
otherServer = currentServer == server1 ? server2 : server1;
serverColor = currentServer == server1 ? 'goldenrod' : 'darkseagreen';
msgNoDsoEmptyCart = 'This web application is currently running in regular non-clustered mode, and you have not yet viewed any cameras.&nbsp;  Click on a camera to add it to the list of recent products.<br><br>If you just jumped over from the other server, the recent products list is empty because the session data is not being shared between servers.&nbsp;  Try running with Terracotta Sessions enabled.';
msgNoDsoFullCart =  'This web application is currently running in regular non-clustered mode, and you have viewed a camera.<br><br>Now click the above link to jump over to the other server.&nbsp;  Since the session data is not being shared across servers, the recent products list will be lost when you jump to the other server.';
msgDsoEmptyCart =   'This web application is currently running clustered on Terracotta Sessions.&nbsp;  You have not yet viewed any cameras.&nbsp;  Click on a camera to add it to the list of recent products.<br><br>Terracotta Sessions makes the session data transparently available to the other server, so the recent products list will be maintained if you jump to another server.';
msgDsoFullCart  =   'This web application is currently running clustered on Terracotta Sessions, and you have viewed a camera.<br><br>Click the link above to go to the other server. Since the session data is being shared between servers, the recent-products list is preserved. <br><br>If you just jumped over from the other server, the recent-products list has been maintained because Terracotta Sessions replicated the session data.&nbsp;  To view the session data, connect to the cluster using the Developer Console, then click the <bold>Sessions</bold> button to open the <bold>Sessions</bold> panels under the <bold>My application</bold> node.';
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
rowStart =  '<tr style="text-align: left"><td style="text-align: right">';
rowMiddle = '</td><td><b>';
rowEnd =    '</b></td></tr>';
document.writeln(
   '<div style="float: right; width: 200px; text-align: center; ' +
      'color: maroon; background: ' + serverColor + '; font-size: 90%; ' +
      'font-family: sans-serif; border: 5px solid dimgray; ' +
      'border-right: none; padding: 15px; margin: 20px 0px">' + 
      '<img style="background-color: white; border: 1px solid Chocolate; ' +
      'padding: 5px 15px" src="terracotta.png"><br>' +
   '<span style="font-weight: bold">' +
      'Session Clustering Information</span>' +
   '<table align=center style="float: center; font-size: 100%; ' +
      'padding: 10px 0px">' +
   rowStart + 'Current Server:' + rowMiddle + currentServer + rowEnd +
   rowStart + 'Terracotta Sessions:' + rowMiddle +
      (dsoEnabled ? 'On' : 'Off') + rowEnd +
   rowStart + 'Items in list:' + rowMiddle + cartSize + rowEnd +
   rowStart + 'Go to:' + rowMiddle + '<a href="http://' + location.hostname +
      ':' + otherServer + '/Townsend">Server ' + otherServer + rowEnd +
   '</table><div style="text-align: left"><b>Explanation</b><br>' +
   getMsg() + '</div></div>');
</script>

<jsp:useBean id="prodCatalog" class="demo.townsend.service.ProductCatalog" scope="request"/>
<div class=main>
<h1>Townsend Camera and Supply</h1>
<h2>Worldwide Online Source for Digtal Cameras</h2>

<!--  Product Catalog  -->
<fieldset class=camera>
   <legend>Product Catalog</legend>
   <%
   java.util.List prodDB =  prodCatalog.getCatalog();
   for (java.util.Iterator iter = prodDB.iterator(); iter.hasNext(); ) { 
	demo.townsend.service.Product prod = (demo.townsend.service.Product)iter.next();%>
      <html:form action="addToList">
      <input type=hidden name='id' value='<%=prod.getId()%>'>
      <input type='image' src='c<%=prod.getId()%>.gif'
         title='<%=prod.getName()%>' class=camera>
      </html:form>
      <% } %>
   </fieldset>
<br><br>
<table><tr class=second_row><td class=details_box>

<!--  Product Details  -->
<fieldset>
   <legend>Details</legend>
   <%
   org.apache.struts.action.DynaActionForm form =
      (org.apache.struts.action.DynaActionForm)
         session.getAttribute("displayUserListForm");
   demo.townsend.service.Product curProd = (demo.townsend.service.Product)form.get("currentProduct");
   if (curProd == null)
      out.println("<i>Select a camera from above to see more information.</i>");
   else { %>
      <img src='c<%=curProd.getId()%>.gif' class=details>
      <%=curProd.getName()%><br>
      <small><%=curProd.getDetails()%></small><br>
      In Stock: <%=curProd.getQuantity()%><br class=all>
      <% }
   %>
   </fieldset>
</td><td>&nbsp;</td><td>

<!--  Recent Products (last 5)  -->
<fieldset class=recent>
   <legend>Recent&nbsp;Products</legend>
   <%
   java.util.ArrayList recentList =
      (java.util.ArrayList)form.get("recentList");
   if (recentList.size() == 0)
      out.println("<i>Empty</i>");
   for (java.util.Iterator iter = recentList.iterator(); iter.hasNext(); ) { 
	demo.townsend.service.Product prod = (demo.townsend.service.Product)iter.next(); %>
      <nobr><%=prod.getName()%></nobr><br>
      <% }
   %>
   </fieldset>
</td></tr></table>

<div class=footer>
   Powered by Terracotta<br><a class=footer
      href="http://www.terracotta.org/">www.terracotta.org</a>
   </div>
</div>
</body>
</html:html>
