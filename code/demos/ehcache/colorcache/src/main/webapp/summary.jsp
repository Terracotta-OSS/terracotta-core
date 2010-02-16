<!--

All content copyright (c) 2003-2009 Terracotta, Inc.,
except as may otherwise be noted in a separate copyright notice.
All rights reserved.

-->
<%
  String server1 = "9081";
  String server2 = "9082";
  String currentServer = request.getRequestURL().indexOf(server1) == -1 ? server2 : server1;
  String otherServer = currentServer == server1 ? server2 : server1;
  String serverColor = currentServer == server1 ? "goldenrod" : "darkseagreen";
  String href = "http://localhost:"+otherServer+request.getContextPath()+"?colorName="+request.getParameter("colorName");
%>
<hr style="margin-top: 10px">
<div style="background: <%=serverColor%>; font-size: 85%; font-family: sans-serif; border: 3px solid gray; padding: 15px; margin: 50px 80px">
<table style="font-size: 100%">
  <tr style="vertical-align: top">
    <td>
      <table style="font-size: 100%">
        <tr>
          <td style="text-align: right"><nobr>Current server:</nobr></td><td><nobr><b><%=currentServer%></b></nobr></td>
        </tr>
        <tr>
          <td style="text-align: right"><nobr>Cache size:</nobr></td><td><nobr><b><%=request.getParameter("cacheSize")%></b></nobr></td>
        </tr>
        <tr>
          <td style="text-align: right"><nobr>Time to live:</nobr></td><td><nobr><b><%=request.getParameter("ttl")%></b></nobr></td>
        </tr>
        <tr>
          <td style="text-align: right"><nobr>Time to idle:</nobr></td><td><nobr><b><%=request.getParameter("tti")%></b></nobr></td>
        </tr>
        <tr>
          <td style="text-align: right"><nobr>Go to:</nobr></td><td><nobr><b><a href="<%=href%>"<b>Server <%=otherServer%></b></a></b></nobr></td>
        </tr>
      </table>
    </td>
    <td style="padding-left: 15px">
<p>ColorCache demonstrates sharing a distributed cache between two nodes. Click on the link on the left to switch between nodes. Enter the name of a color e.g. <i>red</i> to request it's RGB code from the backing data store. The first request is slow, as the data isn't yet cached. Try requesting the same color from the other node. You will see that subsequent requests for that color (from either node) are served rapidly from the cache. The cache is also configured to evict unused color elements after a short time.<p>With the Terracotta Developer Console, you can monitor the cache activity and dynamically change it's eviction configuration; see the <i><b>Ehcache</b></i> tab under <i><b>My application</b></i>.
    </td>
  </tr>
</table>
</div>
