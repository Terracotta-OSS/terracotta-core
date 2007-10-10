/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.AppServerFactory;
import com.tc.test.server.appserver.deployment.AbstractOneServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.webapp.servlets.SessionConfigServlet;

import java.util.Date;

import junit.framework.Test;

/**
 * Test CookieEnabled=false
 * @author hhuynh
 */
public class SessionConfigTest extends AbstractOneServerDeploymentTest {
  private static final String CONTEXT = "SessionConfigTest";
  private static final String MAPPING = "app";

  public SessionConfigTest() {
    if (AppServerFactory.getCurrentAppServerId() != AppServerFactory.WEBLOGIC) {
      disableAllUntil(new Date(Long.MAX_VALUE));
    }
  }

  public static Test suite() {
    return new SessionConfigTestSetup();
  }

  public void testCookieDisabled() throws Exception {
    WebConversation conversation = new WebConversation();

    WebResponse response1 = request(server0, "testcase=CookiesEnabled&hit=0", conversation);
    assertEquals("OK", response1.getText().trim());

    response1 = request(server0, "testcase=CookiesEnabled&hit=1", conversation);
    assertEquals("OK", response1.getText().trim());

  }

  private WebResponse request(WebApplicationServer server, String params, WebConversation con) throws Exception {
    return server.ping("/" + CONTEXT + "/" + MAPPING + "?" + params, con);
  }

  private static class SessionConfigTestSetup extends OneServerTestSetup {

    public SessionConfigTestSetup() {
      super(SessionConfigTest.class, CONTEXT);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet("SessionConfigServlet", "/" + MAPPING + "/*", SessionConfigServlet.class, null, false);
      // add container specific descriptor

      switch (AppServerFactory.getCurrentAppServerId()) {
        case AppServerFactory.WEBLOGIC:
          if (AppServerFactory.getCurrentAppServerMajorVersion().equals("8")) {
            builder.addResourceFullpath("/com/tctest/server/appserver/unit/sessionconfigtest", "weblogic81a.xml",
                                        "WEB-INF/weblogic.xml");
          }
          if (AppServerFactory.getCurrentAppServerMajorVersion().equals("9")) {
            builder.addResourceFullpath("/com/tctest/server/appserver/unit/sessionconfigtest", "weblogic92a.xml",
                                        "WEB-INF/weblogic.xml");
          }
          break;
        case AppServerFactory.TOMCAT:
          builder.addResourceFullpath("/com/tctest/server/appserver/unit/sessionconfigtest", "contexta.xml",
                                      "META-INF/context.xml");
          break;
        default:
          break;
      }
    }

    protected void configureTcConfig(TcConfigBuilder clientConfig) {
      clientConfig.addWebApplication(CONTEXT);
    }
  }

}
