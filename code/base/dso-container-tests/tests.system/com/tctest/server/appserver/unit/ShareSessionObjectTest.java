/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import org.apache.commons.httpclient.HttpClient;

import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;
import com.tc.test.server.util.HttpUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class ShareSessionObjectTest extends AbstractAppServerTestCase {
  public final void testSessions() throws Exception {

    startDsoServer();

    HttpClient client = HttpUtil.createHttpClient();

    int port0 = startAppServer(true).serverPort();
    int port1 = startAppServer(true).serverPort();

    URL url0 = new URL(createUrl(port0, TestServlet.class) + "?cmd=set");
    URL url1 = new URL(createUrl(port1, TestServlet.class) + "?cmd=read");

    assertEquals("OK", HttpUtil.getResponseBody(url0, client));
    assertEquals("OK", HttpUtil.getResponseBody(url1, client));
  }

  public static final class TestServlet extends HttpServlet {

    private static final String ATTR_NAME = "session reference";

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
      PrintWriter out = response.getWriter();

      try {
        doGet0(request, response);
        out.write("OK");
      } catch (Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        out.write(sw.toString());
      }
    }

    private void doGet0(HttpServletRequest request, HttpServletResponse response) {
      HttpSession session = request.getSession(true);
      response.setContentType("text/html");

      String cmd = request.getParameter("cmd");

      Object o = session.getAttribute(ATTR_NAME);
      if ("set".equals(cmd)) {
        if (o != null) { throw new AssertionError("attribute is not null: " + o); }
        session.setAttribute(ATTR_NAME, session);
      } else if ("read".equals(cmd)) {
        if (o != session) { throw new AssertionError("different session objects, " + session + " != " + o); }
      } else {
        throw new AssertionError("unknown cmd: " + cmd);
      }

    }
  }

}
