/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.tc.test.server.appserver.AppServerFactory;
import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.webapp.servlets.SessionIDIntegrityTestServlet;

import java.util.regex.Pattern;

import junit.framework.Test;

/**
 * Test to make sure session id is preserved with Terracotta
 */
public class SessionIDIntegrityTest extends AbstractTwoServerDeploymentTest {
  private static final String CONTEXT = "SessionIDIntegrityTest";
  private static final String MAPPING = "SessionIDIntegrityTestServlet";

  public static Test suite() {
    return new SessionIDIntegrityTestSetup();
  }

  public final void testSessionId() throws Exception {
    WebConversation wc = new WebConversation();

    assertEquals("OK", request(server0, "cmd=insert", wc));

    String server0_session_id = wc.getCookieValue("JSESSIONID");
    System.out.println("Server0 session id: " + server0_session_id);
    assertSessionIdIntegrity(server0_session_id, "server_0");

    assertEquals("OK", request(server1, "cmd=query", wc));

    String server1_session_id = wc.getCookieValue("JSESSIONID");
    System.out.println("Server1 session id: " + server1_session_id);
    assertSessionIdIntegrity(server1_session_id, "server_1");
  }

  private void assertSessionIdIntegrity(String sessionId, String extra_id) {
    int appId = AppServerFactory.getCurrentAppServerId();

    switch (appId) {
      case AppServerFactory.TOMCAT:
      case AppServerFactory.WASCE:
      case AppServerFactory.JBOSS:
        assertTrue(sessionId.endsWith("." + extra_id));
        break;
      case AppServerFactory.WEBLOGIC:
        assertTrue(Pattern.matches("\\S+!-?\\d+", sessionId));
        break;
      case AppServerFactory.WEBSPHERE:
        assertTrue(Pattern.matches("0000\\S+:\\S+", sessionId));
        break;
      default:
        throw new RuntimeException("Appserver id [" + appId + "] is missing in this test");
    }
  }

  private String request(WebApplicationServer server, String params, WebConversation wc) throws Exception {
    return server.ping("/" + CONTEXT + "/" + MAPPING + "?" + params, wc).getText().trim();
  }

  private static class SessionIDIntegrityTestSetup extends TwoServerTestSetup {
    public SessionIDIntegrityTestSetup() {
      super(SessionIDIntegrityTest.class, CONTEXT);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet(MAPPING, "/" + MAPPING + "/*", SessionIDIntegrityTestServlet.class, null, false);
    }

    protected void configureTcConfig(TcConfigBuilder clientConfig) {
      clientConfig.addWebApplication(CONTEXT);
    }
  }
}
