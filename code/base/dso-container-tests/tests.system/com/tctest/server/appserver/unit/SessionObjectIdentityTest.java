/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.deployment.AbstractOneServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.webapp.servlets.SessionObjectIdentityTestServlet;

import junit.framework.Test;

public class SessionObjectIdentityTest extends AbstractOneServerDeploymentTest {
  private static final String CONTEXT = "SessionObjectIdentityTest";
  private static final String SERVLET = "SessionObjectIdentityTestServlet";

  public static Test suite() {
    return new SessionObjectIdentityTestSetup();
  }

  public final void testSessions() throws Exception {
    WebConversation wc = new WebConversation();
    String url = "/" + CONTEXT + "/" + SERVLET;
    WebResponse response = server1.ping(url, wc);
    assertEquals("OK", response.getText().trim());
    response = server1.ping(url, wc);
    assertEquals("OK", response.getText().trim());
  }

  private static class SessionObjectIdentityTestSetup extends OneServerTestSetup {
    public SessionObjectIdentityTestSetup() {
      super(SessionObjectIdentityTest.class, CONTEXT);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet(SERVLET, "/" + SERVLET + "/*", SessionObjectIdentityTestServlet.class, null, false);
    }

    protected void configureTcConfig(TcConfigBuilder clientConfig) {
      clientConfig.addWebApplication(CONTEXT);
    }
  }
}