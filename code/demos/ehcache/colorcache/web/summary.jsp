<!--

All content copyright (c) 2003-2009 Terracotta, Inc.,
except as may otherwise be noted in a separate copyright notice.
All rights reserved.

-->
<script>
server1 = "9081";
server2 = "9082";
currentServer = location.href.indexOf(server1) == -1 ? server2 : server1;
otherServer = currentServer == server1 ? server2 : server1;
serverColor = currentServer == server1 ? "goldenrod" : "darkseagreen";
summaryMsg = "ColorCache demonstrates sharing a distributed cache between two nodes. Click on the link on the left to switch between nodes. Enter the name of a color e.g. <i>red</i> to request it's RGB code from the backing data store. The first request is slow, as the data isn't yet cached. Try requesting the same color from the other node. You will see that subsequent requests for that color (from either node) are served rapidly from the cache. The cache is also configured to evict unused color elements after a short time. With the Terracotta Developer Console, you can monitor the cache activity and dynamically change it's eviction configuration; see the <i><b>Ehcache</b></i> tab under <i><b>My application</b></i>.";
function getMsg() {
  return summaryMsg;
}
rowStart = "<tr><td style='text-align: right'><nobr>";
rowMiddle = "</nobr></td><td><nobr><b>";
rowEnd = "</b></nobr></td></tr>";
document.writeln(
   '<hr style="margin-top: 10px">' +
   '<div style="background: ' + serverColor + '; font-size: 85%; ' +
      'font-family: sans-serif; border: 3px solid gray; ' +
      'padding: 15px; margin: 50px 80px">' +
   '<table style="font-size: 100%"><tr style="vertical-align: top"><td>' +
   '<table style="font-size: 100%">' +
   rowStart + 'Current server:' + rowMiddle + currentServer + rowEnd +
   rowStart + 'Cache size:' + rowMiddle + '<%=request.getParameter("cacheSize")%>' + rowEnd +
   rowStart + 'Time to live:' + rowMiddle + '<%=request.getParameter("ttl")%>' + rowEnd +
   rowStart + 'Time to invalidation:' + rowMiddle + '<%=request.getParameter("tti")%>' + rowEnd +
   rowStart + 'Go to:' + rowMiddle + '<a href="http://' + location.hostname +
      ':' + otherServer + '<%=request.getContextPath()%>?colorName=<%=request.getParameter("colorName")%>"><b>Server ' +
      otherServer + '</b></a>' + rowEnd +
   '</table></td><td style="padding-left: 15px">' +
   getMsg() + '</td></tr></table></div>');
</script>
