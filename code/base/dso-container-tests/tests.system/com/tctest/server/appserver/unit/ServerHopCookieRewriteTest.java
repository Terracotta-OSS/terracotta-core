/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.AppServerInfo;
import com.tc.test.server.appserver.deployment.AbstractDeploymentTest;
import com.tc.test.server.appserver.deployment.Deployment;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.ServerTestSetup;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.webapp.servlets.ServerHopCookieRewriteTestServlet;

import java.util.Map;

import junit.framework.Test;

public final class ServerHopCookieRewriteTest extends AbstractDeploymentTest {
  private static final String CONTEXT = "CookieRewrite";
  private static final String MAPPING = "ServerHopCookieRewriteTestServlet";

  private Deployment          deployment;

  public static Test suite() {
    return new ServerTestSetup(ServerHopCookieRewriteTest.class);
  }

  public void setUp() throws Exception {
    super.setUp();
    if (deployment == null) deployment = makeDeployment();
  }

  public void testCookieRewrite() throws Exception {
    TcConfigBuilder tcConfigBuilder = new TcConfigBuilder();
    tcConfigBuilder.addWebApplication(CONTEXT);

    WebApplicationServer server0 = createServer(tcConfigBuilder);
    server0.start();

    WebApplicationServer server1 = createServer(tcConfigBuilder);
    server1.start();

    WebConversation conversation = new WebConversation();
    WebResponse response = request(server0, "server=0", conversation);
    assertEquals("OK", response.getText().trim());

    response = request(server1, "server=1", conversation);
    assertEquals("OK", response.getText().trim());

    response = request(server0, "server=2", conversation);
    assertEquals("OK", response.getText().trim());

    response = request(server0, "server=3", conversation);
    assertEquals("OK", response.getText().trim());
  }

  private WebResponse request(WebApplicationServer server, String params, WebConversation con) throws Exception {
    return server.ping("/" + CONTEXT + "/" + MAPPING + "?" + params, con);
  }

  private WebApplicationServer createServer(TcConfigBuilder configBuilder) throws Exception {
    WebApplicationServer server = makeWebApplicationServer(configBuilder);
    server.addWarDeployment(deployment, CONTEXT);
    int appId = appServerInfo().getId();
    if (appId != AppServerInfo.WEBSPHERE) {
      server.getServerParameters().appendSysProp("com.tc.session.delimiter",
                                                 ServerHopCookieRewriteTestServlet.DEFAULT_DLM);
    }
    return server;
  }

  private Deployment makeDeployment() throws Exception {
    DeploymentBuilder builder = makeDeploymentBuilder(CONTEXT + ".war");
    Map initParams = null;
    builder.addServlet(MAPPING, "/" + MAPPING + "/*", ServerHopCookieRewriteTestServlet.class, initParams, false);
    return builder.makeDeployment();
  }
}
