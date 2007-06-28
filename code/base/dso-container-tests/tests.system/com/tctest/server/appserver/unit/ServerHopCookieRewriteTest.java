/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import org.apache.commons.httpclient.HttpClient;

import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;
import com.tc.test.server.util.HttpUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Properties;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public final class ServerHopCookieRewriteTest extends AbstractAppServerTestCase {

  private static final String DLM = "!";

  public void testSessions() throws Exception {
    startDsoServer();

    HttpClient client = HttpUtil.createHttpClient();
    String[] args = new String[] { "-Dcom.tc.session.delimiter=" + DLM };

    int port0 = startAppServer(true, new Properties(), args).serverPort();
    int port1 = startAppServer(true, new Properties(), args).serverPort();

    URL url0 = new URL(createUrl(port0, ServerHopCookieRewriteTest.DsoPingPongServlet.class) + "?server=0");
    URL url1 = new URL(createUrl(port1, ServerHopCookieRewriteTest.DsoPingPongServlet.class) + "?server=1");
    URL url2 = new URL(createUrl(port0, ServerHopCookieRewriteTest.DsoPingPongServlet.class) + "?server=2");
    assertEquals("OK", HttpUtil.getResponseBody(url0, client));
    assertEquals("OK", HttpUtil.getResponseBody(url1, client));
    assertEquals("OK", HttpUtil.getResponseBody(url2, client));
  }

  public static final class DsoPingPongServlet extends HttpServlet {

    private static final String ATTR_NAME = "test-attribute";
    private static final String SESS_ID   = "session-id";

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
      HttpSession session = request.getSession(true);
      response.setContentType("text/html");
      PrintWriter out = response.getWriter();

      String serverParam = request.getParameter("server");
      if ("0".equals(serverParam)) {
        hit0(request, session, out);
      } else if ("1".equals(serverParam)) {
        hit1(request, session, out);
      } else if ("2".equals(serverParam)) {
        hit2(request, session, out);
      } else {
        out.print("unknown value for server param: " + serverParam);
      }
    }

    private void hit1(HttpServletRequest req, HttpSession session, PrintWriter out) {
      if (session.isNew()) {
        out.print("session is new for server 1; sessionId=" + session.getId());
      } else {
        String value = (String) session.getAttribute(ATTR_NAME);
        if (value == null) {
          out.print("attribute is null");
        } else {
          if (!value.equals("0")) {
            out.print("unexpected value: " + value);
          }
          final String oldKey = getKey((String) session.getAttribute(SESS_ID));
          final String newKey = getKey(session.getId());
          final String oldServerId = getServerId((String) session.getAttribute(SESS_ID));
          final String newServerId = getServerId(session.getId());
          if (!session.getId().equals(req.getRequestedSessionId()) && newKey.equals(oldKey)
              && !newServerId.equals(oldServerId)) {
            out.print("OK");
          } else {
            out.print("oldKey=" + oldKey + ",newKey=" + newKey + ",oldServerId=" + oldServerId + ",newServerId="
                      + newServerId);
          }
        }
      }
    }

    private String getServerId(String s) {
      if (s == null || s.trim().length() == 0 || s.indexOf(DLM) == -1) return null;
      return s.substring(s.indexOf(DLM) + 1);
    }

    private String getKey(String s) {
      if (s == null || s.trim().length() == 0 || s.indexOf(DLM) == -1) return null;
      return s.substring(0, s.indexOf(DLM));
    }

    private void hit2(HttpServletRequest req, HttpSession session, PrintWriter out) {
      if (session.isNew()) {
        out.print("session is  new for server 2; sessionId=" + session.getId());
        return;
      }

      String value = (String) session.getAttribute(ATTR_NAME);
      if (value == null) {
        out.print("missing attribute");
      }
      value = (String) session.getAttribute(SESS_ID);
      if (!session.getId().equals(req.getRequestedSessionId()) && session.getId().equals(value)) {
        out.print("OK");
      } else {
        out.print("expected=" + session.getId() + ",got=" + value);
      }
    }

    private void hit0(HttpServletRequest req, HttpSession session, PrintWriter out) {
      if (!session.isNew()) {
        out.print("session is not new for server 0; sessionId=" + session.getId());
        return;
      }

      String value = (String) session.getAttribute(ATTR_NAME);
      if (!session.getId().equals(req.getRequestedSessionId()) && value == null) {
        out.print("OK");
        session.setAttribute(ATTR_NAME, "0");
        session.setAttribute(SESS_ID, session.getId());
      } else {
        out.print("attribute already exists: " + value);
      }
    }
  }
}
