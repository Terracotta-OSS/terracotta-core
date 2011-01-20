<%
final String contextPath=request.getContextPath();
%>
<html>
<!--
  Copyright 2004 The Apache Software Foundation

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

<jsp:useBean id="cart" scope="session" class="demo.cart.DummyCart" />

<jsp:setProperty name="cart" property="*" />
<%
  cart.processRequest(request);

  // session attribute must be set on every request for
  // data to be updated in the cluster
  session.setAttribute("cart", cart);
%>

<head><title>carts</title></head>
<body bgcolor="white">

<FONT size = 5 COLOR="#CC0000">
<br> You have the following items in your cart:
<ol>
<% 
   String[] items = cart.getItems();
   for (int i=0; i<items.length; i++) {
%>
   <li> <% out.print(demo.cart.HTMLFilter.filter(items[i])); %> 
<%
   }
%>
</ol>

</FONT>

<hr>

<font size = 5 color="#CC0000">

<form type=POST action="<%=contextPath%>">
<BR>
Please enter item to add or remove:
<br>
Add Item:

<SELECT NAME="item">
<OPTION>Beavis & Butt-head Video collection
<OPTION>X-files movie
<OPTION>Twin peaks tapes
<OPTION>NIN CD
<OPTION>JSP Book
<OPTION>Concert tickets
<OPTION>Love life
<OPTION>Switch blade
<OPTION>Rex, Rugs & Rock n' Roll
</SELECT>


<br> <br>
<INPUT TYPE=submit name="submit" value="add">
<INPUT TYPE=submit name="submit" value="remove">

</form>
       
</FONT>



<!-- - - - - - - - - - - - - -->
<!--  Start: Cluster Demo    -->
<!--  Terracotta, Inc.       -->
<!-- - - - - - - - - - - - - -->
<script>
dsoEnabled = <%= Boolean.getBoolean("tc.active") %>;
cartSize = <%= cart.getItems().length %>;
server1 = "9081";
server2 = "9082";
currentServer = location.href.indexOf(server1) == -1 ? server2 : server1;
otherServer = currentServer == server1 ? server2 : server1;
serverColor = currentServer == server1 ? "goldenrod" : "darkseagreen";
msgNoDsoEmptyCart = 'This web application is currently running in regular non-clustered mode, and your cart is empty.&nbsp;  Use the "add" button to put an item in your cart.<br><br>If you just jumped over from the other server, your cart is empty because the session data is not being shared between servers.&nbsp;  Try running with Terracotta Sessions enabled.';
msgNoDsoFullCart =  'This web application is currently running in regular non-clustered mode, and you have added an item to your cart.&nbsp;  Now click the link on the left to jump over to the other server.&nbsp;  Since the session data is not being shared across servers, your cart will be lost when you jump to the other server.';
msgDsoEmptyCart =   'This web application is currently running clustered on Terracotta Sessions.&nbsp;  Your cart is empty.&nbsp;  Use the "add" button to put an item in your cart.&nbsp;  Terracotta Sessions makes the session data transparently available to the other server, so your cart and its contents will be maintained if you jump to another server.';
msgDsoFullCart  =   'This web application is currently running clustered on Terracotta Sessions, and you have added an item to your cart.<br><br>You can click the link on the left to jump over to the other server.&nbsp;  Since the session data is being shared across servers, your cart will be preserved.<br><br>If you just jumped over from the other server, your cart has maintained its contents because Terracotta Sessions replicated the session data. To view the session data, connect to the cluster using the Developer Console, then click the <bold>Sessions</bold> button to open the <bold>Sessions</bold> panels under the <bold>My application</bold> node.';
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
   '<hr style="margin-top: 10px">' +
   '<div style="background: ' + serverColor + '; font-size: 85%; ' +
      'font-family: sans-serif; border: 3px solid gray; ' +
      'padding: 15px; margin: 50px 80px">' +
   '<table style="font-size: 100%"><tr style="vertical-align: top"><td>' +
   '<table style="font-size: 100%">' +
   rowStart + "Current Server:" + rowMiddle + currentServer + rowEnd +
   rowStart + 'Terracotta Sessions:' + rowMiddle +
      (dsoEnabled ? 'On' : 'Off') + rowEnd +
   rowStart + 'Items in Cart:' + rowMiddle + cartSize + rowEnd +
   rowStart + 'Go to:' + rowMiddle + '<a href="http://' + location.hostname +
      ':' + otherServer + '<%=contextPath%>"><b>Server ' + otherServer + '</b></a>' + rowEnd +
   '</table></td><td style="padding-left: 15px">' +
   getMsg() + '</td></tr></table></div>');
</script>
<!-- - - - - - - - - - - - -->
<!--  End: Cluster Demo    -->
<!-- - - - - - - - - - - - -->


</body>
</html>
