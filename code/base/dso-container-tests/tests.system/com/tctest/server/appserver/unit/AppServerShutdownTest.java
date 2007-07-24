/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.tc.process.HeartBeatService;
import com.tc.test.ProcessInfo;
import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tc.util.runtime.Os;
import com.tctest.webapp.servlets.ShutdownNormallyServlet;

import junit.framework.Test;

/**
 * Test to make sure the app server shutdown normally with DSO
 */
public class AppServerShutdownTest extends AbstractTwoServerDeploymentTest {
  private static final String CONTEXT                = "AppServerShutdownTest";
  private static final String SERVLET                = "ShutdownNormallyServlet";

  private static final int    TIME_WAIT_FOR_SHUTDOWN = 3 * 60 * 1000;

  public static Test suite() {
    return new AppServerShutdownTestSetup();
  }

  public final void testShutdown() throws Exception {
    WebConversation wc = new WebConversation();

    assertEquals("OK", request(server0, "cmd=insert", wc));
    assertEquals("OK", request(server1, "cmd=query", wc));

    System.out.println("Shut down app server normally...");
    getServerManager().stopAllWebServers();
    System.out.println("Shutting down completed.");

    if (!Os.isLinux()) { // can't get full command line args in linux
      System.out.println("Grepping for java processes...");
      assertFalse("Cargo processes still linger", checkProcesses());
    }

    System.out.println("Polling heartbeat threads...");
    assertFalse("Linked child processes are still alive", checkAlive());

  }

  private String request(WebApplicationServer server, String params, WebConversation con) throws Exception {
    return server.ping("/" + CONTEXT + "/" + SERVLET + "?" + params, con).getText().trim();
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

  private static class AppServerShutdownTestSetup extends TwoServerTestSetup {

    public AppServerShutdownTestSetup() {
      super(AppServerShutdownTest.class, CONTEXT);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet(SERVLET, "/" + SERVLET + "/*", ShutdownNormallyServlet.class, null, false);
    }

    protected void configureTcConfig(TcConfigBuilder clientConfig) {
      clientConfig.addWebApplication(CONTEXT, true);
    }
  }
}
