/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.tc.test.AppServerInfo;
import com.tc.test.server.appserver.deployment.AbstractOneServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.webapp.servlets.SessionIDFromURLServlet;

import junit.framework.Test;

public class SessionIDFromURLTest extends AbstractOneServerDeploymentTest {
  private static final String CONTEXT = "SessionIDFromURL";
  private static final String SERVLET = "SessionIDFromURLServlet";

  public static Test suite() {
    return new SessionIDFromURLTestSetup();
  }

  public SessionIDFromURLTest() {
    if (appServerInfo().getId() == AppServerInfo.WEBSPHERE) {
      disableAllUntil("2008-12-14");
    }
  }

  public void testURLSessionId() throws Exception {
    String encodedURL = server0.ping("/" + CONTEXT + "/" + SERVLET + "?cmd=new").getText().trim();

    encodedURL = encodedURL.concat("?cmd=query");
    String response = new WebConversation().getResponse(encodedURL).getText().trim();
    assertEquals("OK", response);
  }

  private static class SessionIDFromURLTestSetup extends OneServerTestSetup {

    public SessionIDFromURLTestSetup() {
      super(SessionIDFromURLTest.class, CONTEXT);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet("SessionIDFromURLServlet", "/" + SERVLET + "/*", SessionIDFromURLServlet.class, null, false);
    }

    protected void configureTcConfig(TcConfigBuilder tcConfigBuilder) {
      tcConfigBuilder.addWebApplication(CONTEXT);
    }
  }
}
