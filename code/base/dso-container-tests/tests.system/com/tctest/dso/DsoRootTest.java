/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.dso;

import com.meterware.httpunit.WebConversation;
import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.webapp.servlets.RootCounterServlet;

import java.util.Random;

import junit.framework.Test;

public class DsoRootTest extends AbstractTwoServerDeploymentTest {

  private static final int    TOTAL_REQUEST_COUNT = 100;
  private static final String CONTEXT             = "DsoRootTest";
  private static final String MAPPING             = "count";

  public static Test suite() {
    return new DsoRootTestSetup();
  }

  protected boolean isSessionTest() {
    return false;
  }

  private int getCount(WebApplicationServer server, WebConversation con) throws Exception {
    return Integer.parseInt(server.ping("/" + CONTEXT + "/" + MAPPING, con).getText().trim());
  }

  public void testRoot() throws Exception {
    int nodeCount = 2;
    WebConversation conversation = new WebConversation();
    WebApplicationServer[] servers = new WebApplicationServer[] { server0, server1 };

    Random random = new Random();
    for (int i = 0, currentRequestCount = 0; i < TOTAL_REQUEST_COUNT && currentRequestCount < TOTAL_REQUEST_COUNT; i++) {
      int remainingRequests = TOTAL_REQUEST_COUNT - currentRequestCount;
      for (int j = 0; j < random.nextInt(remainingRequests + 1); j++) {
        int newVal = getCount(servers[i % nodeCount], conversation);
        currentRequestCount++;
        assertEquals(currentRequestCount, newVal);
      }
    }
  }

  private static class DsoRootTestSetup extends TwoServerTestSetup {

    public DsoRootTestSetup() {
      super(DsoRootTest.class, CONTEXT);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet("RootCounterServlet", "/" + MAPPING + "/*", RootCounterServlet.class, null, false);
    }

    protected void configureTcConfig(TcConfigBuilder tcConfigBuilder) {
      String rootName = "counterObject";
      String fieldName = RootCounterServlet.class.getName() + ".counterObject";
      tcConfigBuilder.addRoot(fieldName, rootName);

      String methodExpression = "* " + RootCounterServlet.class.getName() + "$Counter.*(..)";
      tcConfigBuilder.addAutoLock(methodExpression, "write");
      
      tcConfigBuilder.addInstrumentedClass(RootCounterServlet.class.getName() + "$Counter", false);
    }

  }
}
