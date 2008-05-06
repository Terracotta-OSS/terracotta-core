/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.webapp.servlets.SessionObjectIdentityTestServlet;

import junit.framework.Test;

public class SessionObjectIdentityTest extends AbstractTwoServerDeploymentTest {
  private static final String CONTEXT = "SessionObjectIdentityTest";
  private static final String SERVLET = "SessionObjectIdentityTestServlet";

  public static Test suite() {
    return new SessionObjectIdentityTestSetup();
  }

  public final void testSessions() throws Exception {
    WebConversation wc = new WebConversation();
    String url = "/" + CONTEXT + "/" + SERVLET + "?cmd=";
    WebResponse response = server0.ping(url + "create", wc);
    assertEquals("OK", response.getText().trim());
    response = server0.ping(url + "checkIdentity", wc);
    assertEquals("OK", response.getText().trim());
    response = server0.ping(url + "shareSession", wc);
    assertEquals("OK", response.getText().trim());
    response = server1.ping(url + "checkShared", wc);
  }

  private static class SessionObjectIdentityTestSetup extends TwoServerTestSetup {
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
