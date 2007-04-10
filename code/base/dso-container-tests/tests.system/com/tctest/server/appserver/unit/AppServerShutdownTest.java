/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import org.apache.commons.httpclient.HttpClient;

import com.tc.process.LinkedJavaProcessPollingAgent;
import com.tc.test.ProcessInfo;
import com.tc.test.server.Server;
import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;
import com.tc.test.server.util.HttpUtil;
import com.tc.util.runtime.Os;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Iterator;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Test to make sure the app server shutdown normally with DSO
 */
public class AppServerShutdownTest extends AbstractAppServerTestCase {

  private static final int TIME_WAIT_FOR_SHUTDOWN = 3 * 60 * 1000;

  public AppServerShutdownTest() {
    // this.disableAllUntil("2007-04-08");
  }

  public final void testShutdown() throws Exception {

    startDsoServer();

    HttpClient client = HttpUtil.createHttpClient();
    int port1 = startAppServer(true).serverPort();
    int port2 = startAppServer(true).serverPort();

    URL url1 = createUrl(port1, ShutdownNormallyServlet.class, "cmd=insert");
    assertEquals("cmd=insert", "OK", HttpUtil.getResponseBody(url1, client));

    URL url2 = createUrl(port2, ShutdownNormallyServlet.class, "cmd=query");
    assertEquals("cmd=query", "OK", HttpUtil.getResponseBody(url2, client));

    int count_before = ProcessInfo.ps_grep_java().split("\\n").length;

    System.out.println("Shut down app server normally...");
    for (Iterator iter = appservers.iterator(); iter.hasNext();) {
      Server server = (Server) iter.next();
      server.stop();
    }
    System.out.println("Shutting down completed.");

    // There could be 2 kinds of failures:
    // 1. Cargo didn't shutdown the appserver normally
    // 2. DSO didn't allow the appserver to shutdown -- We want to catch this
    System.out.println("Grepping for java processes...");
    assertFalse("Cargo processes still linger", checkProcesses(count_before));

    System.out.println("Polling heartbeat threads...");
    assertFalse("Linked child processes are still alive", checkAlive());

  }
  
  /**
   * return true if app server processes are found
   */
  private boolean checkProcesses(int count_before) throws Exception {
    boolean found = false;
    String processes_after;
    long start = System.currentTimeMillis();
    do {
      Thread.sleep(1000);      
      processes_after = ProcessInfo.ps_grep_java();
      int count_after = processes_after.split("\\n").length;
      
      if (Os.isLinux()) {
        found = count_after + 2 != count_before;
      } else {
        found = processes_after.indexOf("CargoLinkedChildProcess") > 0;
      }
      
    }while (found && System.currentTimeMillis() - start < TIME_WAIT_FOR_SHUTDOWN);
    
    if (found) {
      System.out.println(processes_after);
    }
    
    return found;
  }
  
  /**
   * check server status by pinging its linked-child-process
   * return true if any app server is still alive
   */
  private boolean checkAlive() throws Exception {    
    long start = System.currentTimeMillis();
    boolean foundAlive = false;
    do {
      Thread.sleep(1000);
      foundAlive = LinkedJavaProcessPollingAgent.isAnyAppServerAlive();
    } while (foundAlive && System.currentTimeMillis() - start < TIME_WAIT_FOR_SHUTDOWN);
    
    return foundAlive;
  }

  public static final class ShutdownNormallyServlet extends HttpServlet {
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
      HttpSession session = request.getSession(true);
      response.setContentType("text/html");
      PrintWriter out = response.getWriter();

      String cmdParam = request.getParameter("cmd");
      if ("insert".equals(cmdParam)) {
        session.setAttribute("hung", "daman");
        out.println("OK");
      } else if ("query".equals(cmdParam)) {
        String data = (String) session.getAttribute("hung");
        if (data != null && data.equals("daman")) {
          out.println("OK");
        } else {
          out.println("ERROR: " + data);
        }
      } else {
        out.println("unknown cmd");
      }

    }
  }
}
