/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import org.apache.commons.httpclient.HttpClient;

import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;
import com.tc.test.server.util.HttpUtil;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.util.Date;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RequestForwardTest extends AbstractAppServerTestCase {

  public RequestForwardTest() {
    // disableAllUntil("2007-01-01");
  }

  public static final class ForwarderServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
      final String action = req.getParameter("action");
      final String target = req.getParameter("target");
      String reply = "OK";
      RequestDispatcher requestDispatcher = req.getRequestDispatcher(target);
      System.err.println("### ForwarderServlet.doGet is here...");
      if ("s-f-s".equals(action)) {
        req.getSession();
        requestDispatcher.forward(req, resp);
        req.getSession();
      } else if ("n-f-s".equals(action)) {
        requestDispatcher.forward(req, resp);
        req.getSession();
      } else if ("s-f-n".equals(action)) {
        req.getSession();
        System.err.println("### ForwarderServlet: calling forward ...");
        requestDispatcher.forward(req, resp);
        System.err.println("### ForwarderServlet: returned from forward forward ...");
      } else {
        reply = "INVALID REQUEST";
        resp.getWriter().print(reply);
        resp.flushBuffer();
      }
    }
  }

  public static final class ForwardeeServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      final String action = req.getParameter("action");
      System.err.println("### ForwardEEServlet.doGet is here...");
      String reply = "FORWARD OK";
      if (action.endsWith("s")) {
        req.getSession();
        reply= "FORWARD GOT SESSION";
      } else if (action.endsWith("n")) {
        reply= "FORWARD DID NOT GET SESSION";
      } else {
        reply = "INVALID REQUEST";
      }
      resp.getWriter().print(reply);
      resp.flushBuffer();
    }
  }

  private int port;

  public void testSessionForwardSession() throws Exception {
    startDsoServer();

    port = startAppServer(true).serverPort();

    HttpClient client = HttpUtil.createHttpClient();

    // first, sanity check
    URL url = new URL(createUrl(port, RequestForwardTest.ForwarderServlet.class) + "?action=0");
    checkResponse("INVALID REQUEST", url, client);

    url = new URL(createUrl(port, RequestForwardTest.ForwarderServlet.class)
                  + "?action=s-f-s&target=RequestForwardTest-ForwardeeServlet");
    checkResponse("FORWARD GOT SESSION", url, client);
  }

  public void testForwardSession() throws Exception {
    startDsoServer();

    port = startAppServer(true).serverPort();

    HttpClient client = HttpUtil.createHttpClient();

    // first, sanity check
    URL url = new URL(createUrl(port, RequestForwardTest.ForwarderServlet.class) + "?action=0");
    checkResponse("INVALID REQUEST", url, client);

    url = new URL(createUrl(port, RequestForwardTest.ForwarderServlet.class) + "?action=n-f-s&target=RequestForwardTest-ForwardeeServlet");
    checkResponse("FORWARD GOT SESSION", url, client);
  }
  
  public void testSessionForward() throws Exception {
    startDsoServer();

    port = startAppServer(true).serverPort();

    HttpClient client = HttpUtil.createHttpClient();

    // first, sanity check
    URL url = new URL(createUrl(port, RequestForwardTest.ForwarderServlet.class) + "?action=0");
    checkResponse("INVALID REQUEST", url, client);

    url = new URL(createUrl(port, RequestForwardTest.ForwarderServlet.class) + "?action=s-f-n&target=RequestForwardTest-ForwardeeServlet");
    checkResponse("FORWARD DID NOT GET SESSION", url, client);
  }

  private void checkResponse(String expectedResponse, URL url, HttpClient client) throws ConnectException, IOException {
    System.err.println("=== Send Request [" + (new Date()) + "]: url=[" + url + "]");
    final String actualResponse = HttpUtil.getResponseBody(url, client);
    System.err.println("=== Got Response [" + (new Date()) + "]: url=[" + url + "], response=[" + actualResponse + "]");
    assertTimeDirection();
    assertEquals(expectedResponse, actualResponse);
  }
}
