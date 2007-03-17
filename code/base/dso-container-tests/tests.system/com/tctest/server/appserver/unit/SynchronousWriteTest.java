/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import org.apache.commons.httpclient.HttpClient;

import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;
import com.tc.test.server.util.HttpUtil;

import java.io.File;
import java.io.FileWriter;
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
public class SynchronousWriteTest extends AbstractAppServerTestCase {

  private static final int INTENSITY = 100;

  public final void testSessions() throws Exception {
    setSynchronousWrite(true);
    startDsoServer();

    HttpClient client = HttpUtil.createHttpClient();

    int port0 = startAppServer(true).serverPort();
    int port1 = startAppServer(true).serverPort();

    createTransactions(client, port0, SynchronousWriteTest.DsoPingPongServlet.class, "server=0");
    URL url1 = new URL(createUrl(port1, SynchronousWriteTest.DsoPingPongServlet.class) + "?server=1&data=" + (INTENSITY-1));

    assertEquals("99", HttpUtil.getResponseBody(url1, client));
  }

  private void createTransactions(HttpClient client, int port, Class klass, String params) throws Exception {
    File dataFile = new File(this.getTempDirectory(), "synchwrite.txt");
    PrintWriter out = new PrintWriter(new FileWriter(dataFile));

    for (int i = 0; i < INTENSITY; i++) {
      out.println("data" + i + "=" + i);
      URL url0 = new URL(createUrl(port, klass) + "?" + params + "&data=" + i);
      assertEquals("OK", HttpUtil.getResponseBody(url0, client));
    }

    out.close();
  }

  public static final class DsoPingPongServlet extends HttpServlet {
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
      HttpSession session = request.getSession(true);
      response.setContentType("text/html");
      PrintWriter out = response.getWriter();

      String serverParam = request.getParameter("server");
      String dataParam = request.getParameter("data");

      switch (Integer.parseInt(serverParam)) {
        case 0:
          hit0(session, out, dataParam);
          break;
        case 1:
          hit1(session, out, "data"+dataParam);
          break;
        default:
          out.print("unknown value for server param: " + serverParam);
      }
    }

    private void hit1(HttpSession session, PrintWriter out, String attrName) {
      System.err.println("### hit1: sessionId = " + session.getId());
      String value = (String) session.getAttribute(attrName);
      System.err.println(attrName + "=" + value);
      if (value == null) {
        out.print(attrName + " is null");
      } else {
        out.print(value);
      }
    }

    private void hit0(HttpSession session, PrintWriter out, String dataParam) {
      System.err.println("### hit0: sessionId = " + session.getId());
      System.err.println("setAttribute: " + "data" + dataParam);
      session.setAttribute("data" + dataParam, dataParam);
      out.print("OK");
    }
  }
}
