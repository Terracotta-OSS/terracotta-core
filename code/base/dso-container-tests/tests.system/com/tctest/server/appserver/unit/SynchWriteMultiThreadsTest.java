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
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 */
public class SynchWriteMultiThreadsTest extends AbstractAppServerTestCase {

  private static final int INTENSITY      = 1000;
  private static final int NUM_OF_DRIVERS = 25;

  public SynchWriteMultiThreadsTest() {
    // this.disableAllUntil("2007-03-08");
  }

  private static class Driver extends Thread {
    private HttpClient                 client;
    private int                        port0, port1;
    private SynchWriteMultiThreadsTest parent;

    public Driver(SynchWriteMultiThreadsTest parent, int port0, int port1) {
      client = HttpUtil.createHttpClient();
      this.parent = parent;
      this.port0 = port0;
      this.port1 = port1;
    }

    public void run() {
      try {
        URL url0 = parent.createUrl(port0, "server=0&command=ping");
        assertEquals("Send ping", "OK", HttpUtil.getResponseBody(url0, client));
        URL url1 = parent.createUrl(port1, "server=1&command=ping");
        assertEquals("Receive pong", "OK", HttpUtil.getResponseBody(url1, client));
        generateTransactionRequests();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    private void generateTransactionRequests() throws Exception {
      for (int i = 0; i < INTENSITY; i++) {
        URL url0 = parent.createUrl(port0, "server=0&command=insert&data=" + i);
        assertEquals("Send data", "OK", HttpUtil.getResponseBody(url0, client));
      }
    }

    public void validate() throws Exception {
      URL url1 = parent.createUrl(port1, "server=1&command=query&data=0");
      assertEquals("Query 0", "0", HttpUtil.getResponseBody(url1, client));
      url1 = parent.createUrl(port1, "server=1&command=query&data=" + (INTENSITY - 1));
      assertEquals("Query last attr", "" + (INTENSITY - 1), HttpUtil.getResponseBody(url1, client));
    }
  }

  public URL createUrl(int port, String query) throws MalformedURLException {
    return super.createUrl(port, SynchWriteMultiThreadsTest.DsoPingPongServlet.class, query);
  }

  public final void testSessions() throws Exception {

    this.setSynchronousWrite(true);
    this.startDsoServer();

    int port0 = startAppServer(true).serverPort();
    int port1 = startAppServer(true).serverPort();

    // start all drivers
    Driver[] drivers = new Driver[NUM_OF_DRIVERS];
    for (int i = 0; i < NUM_OF_DRIVERS; i++) {
      drivers[i] = new Driver(this, port0, port1);
      drivers[i].start();
    }

    // wait for all of them to finish
    for (int i = 0; i < NUM_OF_DRIVERS; i++) {
      drivers[i].join();
    }

    // send kill signal to server0
    killServer0(port0);

    // validate data on server1
    for (int i = 0; i < NUM_OF_DRIVERS; i++) {
      drivers[i].validate();
    }
  }

  private void killServer0(int port0) {
    try {
      URL url0 = createUrl(port0, "server=0&command=kill");
      assertEquals("OK", HttpUtil.getResponseBody(url0, HttpUtil.createHttpClient()));
    } catch (Throwable e) {
      // expected
      System.err.println("Caught exception from kill server0");
    }
  }

  public static final class DsoPingPongServlet extends HttpServlet {
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
      HttpSession session = request.getSession(true);
      response.setContentType("text/html");
      PrintWriter out = response.getWriter();

      String server = request.getParameter("server");
      String command = request.getParameter("command");
      String data = request.getParameter("data");

      String log = "##sessionId = " + session.getId() + "##command = " + command + "##data = " + data;
      //System.err.println(log);
      //System.err.flush();

      switch (Integer.parseInt(server)) {
        case 0:
          handleServer0(session, out, command, data);
          break;
        case 1:
          handleServer1(session, out, command, data);
          break;
        default:
          out.print("unknown value for server param: " + server);
      }
      out.flush();
    }

    private void handleServer0(HttpSession session, PrintWriter out, String command, String data) {
      if (command.equals("ping")) {
        session.setAttribute("ping", "pong");
        out.println("OK");
      } else if (command.equals("insert")) {
        session.setAttribute("data" + data, data + "");
        out.println("OK");
      } else if (command.equals("kill")) {
        out.println("OK");
        Runtime.getRuntime().halt(1);
      }
    }

    private void handleServer1(HttpSession session, PrintWriter out, String command, String data) {
      if (command.equals("ping")) {
        String ping = (String) session.getAttribute("ping");
        if (ping == null) {
          out.println("ping is null");
        } else out.println("OK");
      } else if (command.equals("query")) {
        String queried_data = (String) session.getAttribute("data" + data);
        if (queried_data == null) {
          out.println("data" + data + " is null");
        } else out.println(queried_data);
      }
    }
  }
}
