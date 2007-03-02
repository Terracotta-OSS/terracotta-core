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

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Simplest test case for DSO. This class should be used as a model for building container based tests. A feature which
 * was omitted in this test is the overloaded startAppServer() method which also takes a properties file. These
 * properties will then be available to the innerclass servlet below as system properties. View the superclass
 * description for more information about general usage.
 */
public class SimpleDsoSessionsTest extends AbstractAppServerTestCase {

  public final void testSessions() throws Exception {

    startDsoServer();

    HttpClient client = HttpUtil.createHttpClient();

    int port0 = startAppServer(true).serverPort();
    int port1 = startAppServer(true).serverPort();

    URL url0 = new URL(createUrl(port0, SimpleDsoSessionsTest.DsoPingPongServlet.class) + "?server=0");
    URL url1 = new URL(createUrl(port1, SimpleDsoSessionsTest.DsoPingPongServlet.class) + "?server=1");

    assertEquals("OK", HttpUtil.getResponseBody(url0, client));
    assertEquals("OK", HttpUtil.getResponseBody(url1, client));
  }

  public final void testSynchronousWriteSessions() throws Exception {

    setSynchronousWrite(true);
    startDsoServer();

    HttpClient client = HttpUtil.createHttpClient();

    int port0 = startAppServer(true).serverPort();
    int port1 = startAppServer(true).serverPort();

    URL url0 = new URL(createUrl(port0, SimpleDsoSessionsTest.DsoPingPongServlet.class) + "?server=0");
    URL url1 = new URL(createUrl(port1, SimpleDsoSessionsTest.DsoPingPongServlet.class) + "?server=1");

    assertEquals("OK", HttpUtil.getResponseBody(url0, client));
    assertEquals("OK", HttpUtil.getResponseBody(url1, client));
  }

  public static final class DsoPingPongServlet extends HttpServlet {

    private static final String ATTR_NAME = "test-attribute";

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
      HttpSession session = request.getSession(true);
      response.setContentType("text/html");
      PrintWriter out = response.getWriter();

      String serverParam = request.getParameter("server");
      if ("0".equals(serverParam)) {
        hit0(session, out);
      } else if ("1".equals(serverParam)) {
        hit1(session, out);
      } else {
        out.print("unknown value for server param: " + serverParam);
      }
    }

    private void hit1(HttpSession session, PrintWriter out) {
      System.err.println("### hit1: sessionId = " + session.getId());
      if (session.isNew()) {
        out.print("session is new for server 1; sessionId=" + session.getId());
      } else {
        String value = (String) session.getAttribute(ATTR_NAME);
        if (value == null) {
          out.print("attribute is null");
        } else {
          if (value.equals("0")) {
            out.print("OK");
          } else {
            out.print("unexpected value: " + value);
          }
        }
      }
    }

    private void hit0(HttpSession session, PrintWriter out) {
      if (!session.isNew()) {
        out.print("session is not new for server 0; sessionId=" + session.getId());
        return;
      }

      System.err.println("### hit0: sessionId = " + session.getId());

      String value = (String) session.getAttribute(ATTR_NAME);
      if (value == null) {
        out.print("OK");
        session.setAttribute(ATTR_NAME, "0");
      } else {
        out.print("attribute already exists: " + value);
      }
    }
  }
}
