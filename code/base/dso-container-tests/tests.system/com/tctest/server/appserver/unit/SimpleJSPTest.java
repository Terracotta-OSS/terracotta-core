/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.AppServerInfo;
import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;

import junit.framework.Test;

public class SimpleJSPTest extends AbstractTwoServerDeploymentTest {
  private static final String CONTEXT  = "simplejsptest";
  private static final String INDEXJSP = "index.jsp";

  public SimpleJSPTest() {
    //
  }

  public static Test suite() {
    return new SimpleJSPTestSetup();
  }

  public void testSession() throws Exception {
    WebConversation conversation = new WebConversation();

    WebResponse response1 = request(server0, "cmd=insert", conversation);
    assertEquals("OK", response1.getText().trim());

    WebResponse response2 = request(server1, "cmd=query", conversation);
    assertEquals("OK", response2.getText().trim());
  }

  private WebResponse request(WebApplicationServer server, String params, WebConversation con) throws Exception {
    return server.ping("/" + CONTEXT + "/" + INDEXJSP + "?" + params, con);
  }

  /** ****** test setup ********* */
  private static class SimpleJSPTestSetup extends TwoServerTestSetup {

    public SimpleJSPTestSetup() {
      super(SimpleJSPTest.class, CONTEXT);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addResource("/com/tctest/server/appserver/unit/simplejsptest", "index.jsp", "");
    }

    protected void configureTcConfig(TcConfigBuilder clientConfig) {
      clientConfig.addWebApplication(CONTEXT);
      if (appServerInfo().getId() == AppServerInfo.TOMCAT) {
        clientConfig.addInstrumentedClass("org.apache.jsp.index_jsp");
      }
    }

  }

}
