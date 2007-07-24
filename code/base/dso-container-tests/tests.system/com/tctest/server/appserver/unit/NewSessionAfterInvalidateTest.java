/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.webapp.servlets.NewSessionAfterInvalidateTestServlet;

import junit.framework.Test;

public class NewSessionAfterInvalidateTest extends AbstractTwoServerDeploymentTest {

  private static final String CONTEXT = "NewSession";
  private static final String MAPPING = "new";

  public static Test suite() {
    return new NewSessionTestSetup();
  }

  public final void testSessions() throws Exception {
    WebConversation conversation = new WebConversation();

    WebResponse response1 = request(server0, "step=1", conversation);
    assertEquals("OK", response1.getText().trim());

    WebResponse response2 = request(server1, "step=2", conversation);
    assertEquals("OK", response2.getText().trim());
  }

  private WebResponse request(WebApplicationServer server, String params, WebConversation con) throws Exception {
    return server.ping("/" + CONTEXT + "/" + MAPPING + "?" + params, con);
  }

  private static class NewSessionTestSetup extends TwoServerTestSetup {

    public NewSessionTestSetup() {
      super(NewSessionAfterInvalidateTest.class, CONTEXT);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet("NewSessionAfterInvalidateTestServlet", "/" + MAPPING + "/*",
                         NewSessionAfterInvalidateTestServlet.class, null, false);
    }

    protected void configureTcConfig(TcConfigBuilder tcConfigBuilder) {
      tcConfigBuilder.addWebApplication(CONTEXT);
    }

  }
}
