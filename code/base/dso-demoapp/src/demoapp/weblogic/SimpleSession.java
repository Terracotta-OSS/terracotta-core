package demoapp.weblogic;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class SimpleSession extends HttpServlet {

  private final List list = new ArrayList();

  private void dumpSession(HttpSession session, PrintWriter out) {

    out.println("<table>");
    Enumeration keys = session.getAttributeNames();
    while (keys.hasMoreElements()) {
      String key = (String) keys.nextElement();
      out.print("<tr>");
      out.print("<td>");
      out.print(key);
      out.print("</td>");
      out.print("<td>");
      out.print(session.getAttribute(key));
      out.print("</td>");
      out.print("</tr>");
    }

    out.println("</table>");
  }

  public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
    // Get the session object
    HttpSession session = req.getSession(true);

    // Local variables
    PrintWriter out = res.getWriter();
    Integer ival;

    // set content type and other response header fields first
    res.setContentType("text/html");

    // Retrieve the count value from the session
    ival = (Integer) session.getAttribute("sessiontest.counter");
    if (ival == null) ival = new Integer(1);
    else ival = new Integer(ival.intValue() + 1);

    synchronized (list) {
      list.add(new MyInteger(ival.intValue()));
    }

    out.println("list has " + list.size() + " items in it <p>");

    session.setAttribute("sessiontest.counter", ival);

    try {
      // then write the data of the response
      out.println("You have hit this page <b>" + ival + "</b> times.<p>");

      // out.println("ClassLoader: " + getClass().getClassLoader() + "<p>\n");

      // when the user clicks on the link in the next line, the SimpleSession is called again,
      // but now URL rewriting is turned on
      out.println("Click <a href=\"" + res.encodeURL("SimpleSession") + "\">here</a>");
      out.println(" to ensure that session tracking is working even " + "if cookies aren't supported.<br>");
      out.println("Note that by default URL rewriting is not enabled " + "because of its expensive overhead");
      out.println("<p>");
      out.println("<h4>Request and Session Data:</h4>");
      out.println("Session ID in Request: " + req.getRequestedSessionId());
      out.println("<br>Session ID in Request from Cookie: " + req.isRequestedSessionIdFromCookie());
      out.println("<br>Session ID in Request from URL: " + req.isRequestedSessionIdFromURL());
      out.println("<br>Valid Session ID: " + req.isRequestedSessionIdValid());
      out.println("<h4>Session Data:</h4>");
      out.println("New Session: " + session.isNew());
      out.println("<br>Session ID: " + session.getId());
      out.println("<br>Creation Time: " + session.getCreationTime());
      out.println("<br>Last Accessed Time: " + session.getLastAccessedTime());

      out.println("<h4>Dump Session</h4>");
      dumpSession(session, out);
    } catch (Exception ex) {
      out.println("<p><b>!! Example Failed !!<br><br> The following exception occur:</b><br><br>");
      ex.printStackTrace(new PrintWriter(out));
      ex.printStackTrace();
    }
  }

}