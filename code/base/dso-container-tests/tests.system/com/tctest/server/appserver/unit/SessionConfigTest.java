/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.AppServerFactory;
import com.tc.test.server.appserver.deployment.AbstractDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.GenericServer;
import com.tc.test.server.appserver.deployment.ServerTestSetup;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.appserver.was6x.Was6xAppServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tc.util.io.TCFileUtils;
import com.tctest.webapp.servlets.SessionConfigServlet;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;

/**
 * Test session-descriptor setting http://edocs.bea.com/wls/docs81/webapp/weblogic_xml.html#1038173
 * http://edocs.bea.com/wls/docs90/webapp/weblogic_xml.html#1071982
 * 
 * @author hhuynh
 */
public class SessionConfigTest extends AbstractDeploymentTest {
  private static final String  RESOURCE_ROOT = "/com/tctest/server/appserver/unit/sessionconfigtest";
  private static final String  CONTEXT       = "SessionConfigTest";
  private static final String  MAPPING       = "app";

  private DeploymentBuilder    builder;
  private TcConfigBuilder      tcConfigBuilder;
  private WebApplicationServer server;
  private Map                  descriptors   = new HashMap();
  private boolean              weblogicOrWebsphere;

  public SessionConfigTest() {
    boolean weblogicOrWebsphere = AppServerFactory.getCurrentAppServerId() == AppServerFactory.WEBLOGIC
                                  || AppServerFactory.getCurrentAppServerId() == AppServerFactory.WEBSPHERE;
  }

  public static Test suite() {
    return new ServerTestSetup(SessionConfigTest.class);
  }

  // CookieEnabled = false
  public void testCookieDisabled() throws Exception {
    if (!weblogicOrWebsphere) return;
    descriptors.put("wl81", "weblogic81a.xml");
    descriptors.put("wl92", "weblogic92a.xml");
    descriptors.put("was61", "websphere61a.xml");
    init();

    WebConversation conversation = new WebConversation();

    WebResponse response = request(server, "testcase=testCookieDisabled&hit=0", conversation);
    assertEquals("OK", response.getText().trim());

    response = request(server, "testcase=testCookieDisabled&hit=1", conversation);
    assertEquals("OK", response.getText().trim());
  }

  // CookieEnabled = false, URLRewritingEnabled = false
  public void testUrlRewritingDisabled() throws Exception {
    if (!weblogicOrWebsphere) return;
    descriptors.put("wl81", "weblogic81b.xml");
    descriptors.put("wl92", "weblogic92b.xml");
    descriptors.put("was61", "websphere61b.xml");
    init();

    WebResponse response = request(server, "testcase=testUrlRewritingDisabled", new WebConversation());
    System.out.println("Response: " + response.getText());
    assertEquals("OK", response.getText().trim());
  }

  // TrackingEnabled = false -- only applicable to Weblogic
  public void testTrackingDisabled() throws Exception {
    if (!weblogicOrWebsphere) return;
    if (AppServerFactory.getCurrentAppServerId() != AppServerFactory.WEBLOGIC) { return; }
    descriptors.put("wl81", "weblogic81c.xml");
    descriptors.put("wl92", "weblogic92c.xml");
    init();

    WebConversation wc = new WebConversation();
    WebResponse response = request(server, "testcase=testTrackingDisabled&hit=0", wc);
    System.out.println("Response: " + response.getText());
    assertEquals("OK", response.getText().trim());

    response = request(server, "testcase=testTrackingDisabled&hit=1", wc);
    System.out.println("Response: " + response.getText());
    assertEquals("OK", response.getText().trim());
  }

  // IDLength = 69 -- only applicable to Weblogic
  public void testIdLength() throws Exception {
    if (AppServerFactory.getCurrentAppServerId() != AppServerFactory.WEBLOGIC) { return; }
    descriptors.put("wl81", "weblogic81d.xml");
    descriptors.put("wl92", "weblogic92d.xml");
    init();

    WebConversation wc = new WebConversation();
    WebResponse response = request(server, "", wc);
    assertEquals("OK", response.getText().trim());
    int idLength = wc.getCookieValue("JSESSIONID").indexOf("!");
    System.out.println(wc.getCookieValue("JSESSIONID") + ", length = " + idLength);
    assertEquals(69, idLength);
  }

  public void testSessionTimeOutNegative() throws Exception {
    String response = sessionTimeOutTestCase(-1);
    assertTrue(response.startsWith("-1"));
  }
  
  public void testSessionTimeOutArbitrary() throws Exception {
    String response = sessionTimeOutTestCase(69);
    assertTrue(response.startsWith("4140"));
  }
  

  private String sessionTimeOutTestCase(int timeOutInMinutes) throws Exception {
    createTestDeployment();
    builder.addSessionConfig("session-timeout", String.valueOf(timeOutInMinutes));
    createAndStartAppServer();

    WebConversation wc = new WebConversation();
    WebResponse response = request(server, "testcase=testSessionTimeOut", wc);
    System.out.println("Response: " + response.getText());
    return response.getText();
  }

  private WebResponse request(WebApplicationServer appserver, String params, WebConversation con) throws Exception {
    return appserver.ping("/" + CONTEXT + "/" + MAPPING + "?" + params, con);
  }

  private DeploymentBuilder createTestDeployment() {
    tcConfigBuilder = new TcConfigBuilder();
    tcConfigBuilder.addWebApplication(CONTEXT);

    // prepare test war
    builder = makeDeploymentBuilder(CONTEXT + ".war");
    builder.addServlet("SessionConfigServlet", "/" + MAPPING + "/*", SessionConfigServlet.class, null, false);

    return builder;
  }

  private void createAndStartAppServer() throws Exception {
    server = makeWebApplicationServer(tcConfigBuilder);
    addSessionDescriptor();
    server.addWarDeployment(builder.makeDeployment(), CONTEXT);
    server.start();
  }

  private void init() throws Exception {
    createTestDeployment();
    createAndStartAppServer();
  }

  private void addSessionDescriptor() throws Exception {
    if (descriptors.size() == 0) return;

    switch (AppServerFactory.getCurrentAppServerId()) {
      case AppServerFactory.WEBLOGIC:
        if (AppServerFactory.getCurrentAppServerMajorVersion().equals("8")) {
          builder.addResourceFullpath(RESOURCE_ROOT, (String) descriptors.get("wl81"), "WEB-INF/weblogic.xml");
        }
        if (AppServerFactory.getCurrentAppServerMajorVersion().equals("9")) {
          builder.addResourceFullpath(RESOURCE_ROOT, (String) descriptors.get("wl92"), "WEB-INF/weblogic.xml");
        }
        break;
      case AppServerFactory.WEBSPHERE:
        Was6xAppServer wasServer = (Was6xAppServer) ((GenericServer) server).getAppServer();
        wasServer.setExtraScript(TCFileUtils.getResourceFile(RESOURCE_ROOT + "/" + (String) descriptors.get("was61")));
        break;
      default:
        break;
    }
  }

}
