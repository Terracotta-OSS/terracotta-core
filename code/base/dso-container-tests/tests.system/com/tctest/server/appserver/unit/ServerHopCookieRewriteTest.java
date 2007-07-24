/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.AppServerFactory;
import com.tc.test.server.appserver.deployment.AbstractDeploymentTest;
import com.tc.test.server.appserver.deployment.Deployment;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.ServerTestSetup;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.webapp.servlets.ServerHopCookieRewriteTestServlet;

import junit.framework.Test;

public final class ServerHopCookieRewriteTest extends AbstractDeploymentTest {
  private static final String CONTEXT = "CookieRewrite";
  private static final String MAPPING = "ServerHopCookieRewriteTestServlet";
  private Deployment deployment;

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
    int appId = AppServerFactory.getCurrentAppServerId();
    if (appId != AppServerFactory.WEBSPHERE) {
      server.getServerParameters().appendSysProp("com.tc.session.delimiter", ServerHopCookieRewriteTestServlet.DLM);
    }
    return server;
  }
  
  // public void testSessions() throws Exception {
  // int appId = AppServerFactory.getCurrentAppServerId();
  //
  // final String[] args;
  //
  // if (AppServerFactory.WEBSPHERE == appId) {
  // args = new String[] {};
  // } else {
  // args = new String[] { "-Dcom.tc.session.delimiter=" + ServerHopCookieRewriteTestServlet.DLM };
  // }
  //
  // int port0 = startAppServer(true, new Properties(), args).serverPort();
  // int port1 = startAppServer(true, new Properties(), args).serverPort();
  //
  // URL url0 = new URL(createUrl(port0, ServerHopCookieRewriteTestServlet.class) + "?server=0");
  // URL url1 = new URL(createUrl(port1, ServerHopCookieRewriteTestServlet.class) + "?server=1");
  // URL url2 = new URL(createUrl(port0, ServerHopCookieRewriteTestServlet.class) + "?server=2");
  // URL url3 = new URL(createUrl(port0, ServerHopCookieRewriteTestServlet.class) + "?server=3");
  // assertEquals("OK", HttpUtil.getResponseBody(url0, client));
  // assertEquals("OK", HttpUtil.getResponseBody(url1, client));
  // assertEquals("OK", HttpUtil.getResponseBody(url2, client));
  // assertEquals("OK", HttpUtil.getResponseBody(url3, client));
  // }

  private Deployment makeDeployment() throws Exception {
    DeploymentBuilder builder = makeDeploymentBuilder(CONTEXT + ".war");
    builder.addServlet(MAPPING, "/" + MAPPING + "/*", ServerHopCookieRewriteTestServlet.class, null, false);
    return builder.makeDeployment();
  }
}
