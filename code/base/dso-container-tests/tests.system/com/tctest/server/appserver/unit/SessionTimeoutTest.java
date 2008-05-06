/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.AppServerInfo;
import com.tc.test.server.appserver.deployment.AbstractDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.ServerTestSetup;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.webapp.servlets.SessionConfigServlet;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.Test;

/**
 * Test session timeout settings from web.xml, tc.properties
 * 
 * @author hhuynh
 */
public class SessionTimeoutTest extends AbstractDeploymentTest {
  private static final String  CONTEXT            = "SessionConfigTest";
  private static final String  MAPPING            = "app";

  private DeploymentBuilder    builder;
  private TcConfigBuilder      tcConfigBuilder;
  private WebApplicationServer server;
  private Map                  extraServerJvmArgs = new HashMap();

  public SessionTimeoutTest() {
    //
  }

  public static Test suite() {
    return new ServerTestSetup(SessionTimeoutTest.class);
  }

  // test session-timeout field in web.xml
  public void testSessionTimeOutNegative() throws Exception {
    initWithSessionTimeout(-1);
    WebConversation wc = new WebConversation();
    WebResponse response = request(server, "testcase=testSessionTimeOutNegative&hit=0", wc);
    long actual = Long.parseLong(response.getText().replaceAll("[^\\d+-]", ""));
    System.out.println("actual: " + actual);
    assertTrue(actual < 0);

    // even though negative number should dictate the session never timeouts
    // we only test it for 1 minute to save time.
    Thread.sleep(60 * 1000);
    response = request(server, "testcase=testSessionTimeOutNegative&hit=1", wc);
    assertEquals("OK", response.getText().trim());
  }

  // test session-timeout field in web.xml
  public void testSessionTimeOutArbitrary() throws Exception {
    int someBigValue = Integer.MAX_VALUE / 60;
    initWithSessionTimeout(someBigValue);
    WebConversation wc = new WebConversation();
    WebResponse response = request(server, "testcase=testSessionTimeOutArbitrary", wc);
    int actual = Integer.parseInt(response.getText().replaceAll("[^\\d+]", ""));
    assertEquals(someBigValue * 60, actual);
  }

  public void testSessionTimeOutFromTCProperties() throws Exception {
    if (appServerInfo().getId() == AppServerInfo.JETTY) return;
    extraServerJvmArgs.put("com.tc.session.maxidle.seconds", String.valueOf(Integer.MAX_VALUE));
    init();
    WebConversation wc = new WebConversation();
    WebResponse response = request(server, "testcase=testSessionTimeOutArbitrary", wc);
    int actual = Integer.parseInt(response.getText().replaceAll("[^\\d+]", ""));
    assertEquals(Integer.MAX_VALUE, actual);
  }

  public void testResetTimeoutToLowerValue() throws Exception {
    // CDV-634
    if (true) return;

    init();
    int timeoutValue = 30;
    WebConversation wc = new WebConversation();

    System.out.println("Setting timeout value to: " + timeoutValue);
    WebResponse response = request(server, "testcase=testResetTimeoutToLowerValue&hit=0&timeout=" + timeoutValue, wc);
    assertEquals("OK", response.getText().trim());

    System.out.println("About to sleep for: " + (timeoutValue / 2));
    Thread.sleep(1000 * (timeoutValue / 2));

    System.out.println("Setting timeout value to: " + (timeoutValue / 2));
    response = request(server, "testcase=testResetTimeoutToLowerValue&hit=1&value=ABC&timeout=" + (timeoutValue / 2),
                       wc);
    assertEquals("OK", response.getText().trim());

    System.out.println("Retrieving previous stored value in session...");
    response = request(server, "testcase=testResetTimeoutToLowerValue&hit=2&value=ABC", wc);
    assertEquals("OK", response.getText().trim());
  }

  private void init() throws Exception {
    createTestDeployment();
    createAndStartAppServer();
  }
  
  private void initWithSessionTimeout(int timeOutInMinutes) throws Exception {
    createTestDeployment();
    builder.addSessionConfig("session-timeout", String.valueOf(timeOutInMinutes));
    createAndStartAppServer();
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
    server.addWarDeployment(builder.makeDeployment(), CONTEXT);
    if (extraServerJvmArgs.size() > 0) {
      for (Iterator it = extraServerJvmArgs.entrySet().iterator(); it.hasNext();) {
        Map.Entry e = (Map.Entry) it.next();
        server.getServerParameters().appendSysProp((String) e.getKey(), (String) e.getValue());
      }
    }
    server.start();
  }

}
