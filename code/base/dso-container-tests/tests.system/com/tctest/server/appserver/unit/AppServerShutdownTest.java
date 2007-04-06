/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import org.apache.commons.httpclient.HttpClient;

import com.tc.test.ProcessInfo;
import com.tc.test.server.Server;
import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;
import com.tc.test.server.util.HttpUtil;

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
 *
 */
public class AppServerShutdownTest extends AbstractAppServerTestCase {
  
  public AppServerShutdownTest() {
    //this.disableAllUntil("2007-04-08");
  }

  public final void testShutdown() throws Exception {
    
    startDsoServer();

    HttpClient client = HttpUtil.createHttpClient();
    int port = startAppServer(true).serverPort();
    
    URL url = createUrl(port, ShutdownNormallyServlet.class);
    assertEquals("OK", HttpUtil.getResponseBody(url, client));
    
    System.out.println("Shut down app server normally...");
    for (Iterator iter = appservers.iterator(); iter.hasNext();) {
      Server server = (Server) iter.next();
      server.stop();
    }
    System.out.println("Shutting down completed.");
     
    // There could be 2 kinds of failures: 
    //   1. Cargo didn't shutdown the appserver normally
    //   2. DSO didn't allow the appserver to shutdown -- We want to catch this    
    Thread.sleep(5*1000);
    System.out.println("Checking to see if any app server is still alive...");
    String processes = ProcessInfo.ps_grep_java();
    System.out.println("Java processes found: " + processes);
    assertFalse("App server didn't shutdown", processes.indexOf("CargoLinkedChildProcess") > 0);    
  }

  public static final class ShutdownNormallyServlet extends HttpServlet {
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
      HttpSession session = request.getSession(true);
      session.setAttribute("hung", "daman");
      response.setContentType("text/html");
      PrintWriter out = response.getWriter();
      out.println("OK");
    }    
  }
}
