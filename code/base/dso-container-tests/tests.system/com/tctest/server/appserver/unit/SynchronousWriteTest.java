/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.webapp.servlets.SynchronousWriteTestServlet;

import junit.framework.Test;

/**
 * Test session with synchronous-write
 */
public class SynchronousWriteTest extends AbstractTwoServerDeploymentTest {
  private static final String CONTEXT   = "SynchronousWriteTest";
  private static final String SERVLET   = "SynchronousWriteTestServlet";

  private static final int    INTENSITY = 100;

  public static Test suite() {
    return new SynchronousWriteTestSetup();
  }

  public void testSynchWrite() throws Exception {
    WebConversation wc = new WebConversation();
    createTransactions(server0, wc);
    assertEquals("99", request(server1, "server=1&data=99", wc));
    System.out.println("created final request to server 1");
    
  }

  private void createTransactions(WebApplicationServer server, WebConversation wc) throws Exception {
    for (int i = 0; i < INTENSITY; i++) {
      assertEquals("OK", request(server, "server=0&data=" + i, wc));
      System.out.println("created " + i + "-th request to server 0");
    }
  }

  private String request(WebApplicationServer server, String params, WebConversation con) throws Exception {
    return server.ping("/" + CONTEXT + "/" + SERVLET + "?" + params, con).getText().trim();
  }

  private static class SynchronousWriteTestSetup extends TwoServerTestSetup {

    public SynchronousWriteTestSetup() {
      super(SynchronousWriteTest.class, CONTEXT);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet(SERVLET, "/" + SERVLET + "/*", SynchronousWriteTestServlet.class, null, false);
    }

    protected void configureTcConfig(TcConfigBuilder clientConfig) {
      clientConfig.addWebApplication(CONTEXT, true);
    }
  }
}
