<!--

All content copyright (c) 2003-2009 Terracotta, Inc.,
except as may otherwise be noted in a separate copyright notice.
All rights reserved.

-->
<html>
<head>
<title>ColorCache</title>
<style>
 table.swatch {margin-left:20px}
 fieldset.gallery {float:right; margin-right: 10px}
 td.swatch {width:120px; height:100px; border:1px solid black; text-align:center; padding: 1px 1px 1px 1px; background-clip: padding-box}
 td.cached-swatch {width:16px; height:16px; cursor:pointer; background-clip: border-box}
 p.status {margin-left:16px; font-size: 1.2em; font-family: Verdana, Geneva, sans-serif}
</style>
<script type="text/javascript">
  function setStatus(status) {
    elem = document.getElementById("status");
    elem.innerHTML = status;
  }
  function retrieveColor() {
    elem = document.getElementById("colorNameField");
    setStatus("Retrieving color '" + elem.value + "'...");
    document.getElementById("form").submit();
  }
  function retrieveNamedColor(color) {
    elem = document.getElementById("colorNameField");
    elem.value = color;
    setStatus("Retrieving color '" + elem.value + "'...");
    document.getElementById("form").submit();
  }
</script>
</head>

<body>
<h2>ColorCache</h2>
<%!
  org.terracotta.ColorCache cache = new org.terracotta.ColorCache();

  private static String prettyColor(java.awt.Color color) {
    return new StringBuilder()
      .append(color.getRed())
      .append(",")
      .append(color.getGreen())
      .append(",")
      .append(color.getBlue()).toString();
  }
%>
<%
  long now = System.currentTimeMillis();
  String cellText = "";
  String colorName = request.getParameter("colorName");
  java.awt.Color color = null;
  if(colorName != null) {
    color = cache.getColor(colorName);
    if(color == null) {
      cellText = colorName + ": Not Found";
    }
  } else {
    colorName = "";
    cellText = "No Color Selected";
  }
%>
<form id="form" onsubmit="retrieveColor()">
<p>Enter Color Name:
  <input id="colorNameField" type="text" size="30" value="<%=colorName%>" name="colorName"/>
  <input type="submit" value="Retrieve Color"/>
<fieldset class="gallery">
<legend>Cached Colors</legend>
<table>
  <tr>
<%
    String[] colorNames = cache.getColorNames();
    for(String name : colorNames) {
%>
      <td class="cached-swatch"
          style="background-color:<%=name%>"
          onclick="retrieveNamedColor('<%=name%>')"
          onmouseover="this.style.border='1px'"
          onmouseout="this.style.border='0px'"/>
<%      
    }
%>
  </tr>
</table>
</fieldset>
</form>
<table class="swatch">
  <tr>
    <td class="swatch"
<% if(colorName != null && colorName.length() > 0) %>
        style="background-color:<%=colorName%>"
><%=cellText%></td>
    <td>
<p id="status" class="status">
<%
  long elapsed = System.currentTimeMillis() - now;
  if(color != null) {
%>
Color '<%=colorName%>' [<%=prettyColor(color)%>] retrieved in <%=elapsed%> milliseconds.
<%
  }
%>
</p>
    </td>
  </tr>
</table>
<jsp:include page="summary.jsp">
  <jsp:param name="colorName" value="<%=colorName%>"/>
  <jsp:param name="cacheSize" value="<%=cache.getSize()%>"/>
  <jsp:param name="ttl" value="<%=cache.getTTL()%>"/>
  <jsp:param name="tti" value="<%=cache.getTTI()%>"/>
</jsp:include>
</body>
</html>