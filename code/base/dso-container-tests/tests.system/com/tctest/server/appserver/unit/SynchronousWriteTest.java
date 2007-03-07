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
 * A container test will initiate transactions by adding data into session and same data into an external file. Right
 * after it's done with the transactionk, it kills the appserver. Synchronous-write lock will assure that the data was
 * sent actually made into DSO server and a 2nd client can verify the data integrity by comparing the data on server to
 * the data in the file.
 */
public class SynchronousWriteTest extends AbstractAppServerTestCase {

  private static final int INTENSITY = 1000;

  public SynchronousWriteTest() {
    this.disableAllUntil("2007-03-08");
  }

  public final void testSessions() throws Exception {
    setSynchronousWrite(true);
    startDsoServer();

    HttpClient client = HttpUtil.createHttpClient();

    // hit server0 with transactions
    int port0 = startAppServer(true).serverPort();
    int port1 = startAppServer(true).serverPort();

    URL url0 = new URL(createUrl(port0, SynchronousWriteTest.DsoPingPongServlet.class) + "?server=0&data=ping");
    assertEquals("OK", HttpUtil.getResponseBody(url0, client));
    URL url1 = new URL(createUrl(port1, SynchronousWriteTest.DsoPingPongServlet.class) + "?server=1&data=ping");
    assertEquals("OK", HttpUtil.getResponseBody(url1, client));


    try {
      url0 = new URL(createUrl(port0, SynchronousWriteTest.DsoPingPongServlet.class) + "?server=0&data=" + INTENSITY);
      assertEquals("OK", HttpUtil.getResponseBody(url0, client));
    } catch (Throwable e) {
      // expected
      System.err.println("Caught exception from killing appserver");
    }

    url1 = new URL(createUrl(port1, SynchronousWriteTest.DsoPingPongServlet.class) + "?server=1&data=0");
    assertEquals("0", HttpUtil.getResponseBody(url1, client));

    url1 = new URL(createUrl(port1, SynchronousWriteTest.DsoPingPongServlet.class) + "?server=1&data="
                       + (INTENSITY - 1));
    assertEquals("" + (INTENSITY - 1), HttpUtil.getResponseBody(url1, client));
  }

  public static final class DsoPingPongServlet extends HttpServlet {
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
      HttpSession session = request.getSession(true);
      System.err.println("#### sessionId = " + session.getId());
      PrintWriter out = response.getWriter();

      String serverParam = request.getParameter("server");
      String dataParam = request.getParameter("data");

      switch (Integer.parseInt(serverParam)) {
        case 0:
          handleServer0(session, out, dataParam);
          break;
        case 1:
          handleServer1(session, out, dataParam);
          break;
        default:
          out.print("unknown value for server param: " + serverParam);
      }
    }

    private void handleServer0(HttpSession session, PrintWriter out, String dataParam) {
      if (dataParam.equals("ping")) {
        session.setAttribute("ping", "pong");
        out.print("OK");
      } else {
        System.err.println("INTENSITY=" + dataParam);
        int intensity = Integer.parseInt(dataParam);
        for (int i = 0; i < intensity; i++) {
          session.setAttribute("data" + i, "" + i);
          System.err.println("data" + i + "=" + i);
        }
        System.err.flush();
        if (session.getAttribute("data0") != null) { // just a sanity check
          out.print("OK");
        } else {
          out.print("NOT-OK");
        }

        Runtime.getRuntime().halt(1);
      }

    }

    private void handleServer1(HttpSession session, PrintWriter out, String dataParam) {
      if (dataParam.equals("ping")) {
        String pong = (String) session.getAttribute("ping");
        if (pong == null) {
          out.println("ping is null");
        } else {
          System.err.println("ping="+pong);
          out.println("OK");
        }
      } else {
        String value = (String) session.getAttribute("data"+dataParam);
        System.err.println("data" + dataParam + "=" + value);
        if (value == null) {
          out.print("data" + dataParam + " is null");
        } else {
          out.print(value);
        }
      }
    }
  }
}
