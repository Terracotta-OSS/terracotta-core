/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import org.apache.commons.httpclient.HttpClient;

import com.tc.process.HeartBeatService;
import com.tc.test.ProcessInfo;
import com.tc.test.server.Server;
import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;
import com.tc.test.server.util.HttpUtil;
import com.tc.util.runtime.Os;
import com.tctest.webapp.servlets.ShutdownNormallyServlet;

import java.net.URL;
import java.util.Iterator;


/**
 * Test to make sure the app server shutdown normally with DSO
 */
public class AppServerShutdownTest extends AbstractAppServerTestCase {

  private static final int TIME_WAIT_FOR_SHUTDOWN = 3 * 60 * 1000;

  public AppServerShutdownTest() {
    // this.disableAllUntil("2007-04-08");
    registerServlet(ShutdownNormallyServlet.class);
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

    System.out.println("Shut down app server normally...");
    for (Iterator iter = appservers.iterator(); iter.hasNext();) {
      Server server = (Server) iter.next();
      server.stop();
    }
    System.out.println("Shutting down completed.");

    // There could be 2 kinds of failures:
    // 1. Cargo didn't shutdown the appserver normally
    // 2. DSO didn't allow the appserver to shutdown -- We want to catch this

    if (!Os.isLinux()) { // can't get full command line args in linux
      System.out.println("Grepping for java processes...");
      assertFalse("Cargo processes still linger", checkProcesses());
    }

    System.out.println("Polling heartbeat threads...");
    assertFalse("Linked child processes are still alive", checkAlive());

  }

  /**
   * return true if app server processes are found
   */
  private boolean checkProcesses() throws Exception {
    boolean found = false;
    String processes_after;
    long start = System.currentTimeMillis();
    do {
      Thread.sleep(1000);
      processes_after = ProcessInfo.ps_grep_java();
      found = processes_after.indexOf("CargoLinkedChildProcess") > 0;
    } while (found && System.currentTimeMillis() - start < TIME_WAIT_FOR_SHUTDOWN);

    if (found) {
      System.out.println(processes_after);
    }

    return found;
  }

  /**
   * check server status by pinging its linked-child-process return true if any app server is still alive
   */
  private boolean checkAlive() throws Exception {
    long start = System.currentTimeMillis();
    boolean foundAlive = false;
    do {
      Thread.sleep(1000);
      foundAlive = HeartBeatService.anyAppServerAlive();
    } while (foundAlive && System.currentTimeMillis() - start < TIME_WAIT_FOR_SHUTDOWN);

    return foundAlive;
  }
}
