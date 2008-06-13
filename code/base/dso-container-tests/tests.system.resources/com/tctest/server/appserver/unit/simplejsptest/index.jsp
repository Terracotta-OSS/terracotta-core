<%
    String cmdParam = request.getParameter("cmd");
    if ("getClass".equals(cmdParam)) {
      out.println(getClass());
    } else if ("insert".equals(cmdParam)) {
      session.setAttribute("hung", "daman");
      out.println("OK");
    } else if ("query".equals(cmdParam)) {
      String data = (String) session.getAttribute("hung");
      if ("daman".equals(data)) {
        out.println("OK");
      } else {
        out.println("ERROR: " + data);
      }
    } else {
      out.println("unknown cmd");
    }
%>