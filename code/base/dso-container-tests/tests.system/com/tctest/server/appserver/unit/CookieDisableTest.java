/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.AppServerInfo;
import com.tc.test.server.appserver.deployment.AbstractOneServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.GenericServer;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.webapp.servlets.ShutdownNormallyServlet;

import java.util.Arrays;

import junit.framework.Test;

/**
 * This test makes sure we honor cookie disable mechanism in Tomcat and appservers that use Tomcat
 * 
 * @author hhuynh
 */
public class CookieDisableTest extends AbstractOneServerDeploymentTest {
  private static final String CONTEXT = "CookieDisableTest";

  public CookieDisableTest() {
    //disableAllUntil("2008-12-15");
  }

  public boolean shouldDisable() {
    boolean tomcat = appServerInfo().getId() == AppServerInfo.TOMCAT;
    return super.shouldDisable() && !tomcat;
  }
  
  public static Test suite() {
    return new CookieDisableTestSetup();
  }

  public void testSession() throws Exception {
    WebConversation conversation = new WebConversation();

    WebResponse response1 = request(server0, "cmd=insert", conversation);
    assertEquals("OK", response1.getText().trim());

    response1 = request(server0, "cmd=query", conversation);
    assertNotEquals("OK", response1.getText().trim());
    
    System.out.println("Cookie names: " + Arrays.asList(conversation.getCookieNames()));
    assertEquals(0, conversation.getCookieNames().length);
  } 

  private WebResponse request(WebApplicationServer server, String params, WebConversation con) throws Exception {
    return server.ping("/" + CONTEXT + "/ShutdownNormallyServlet?" + params, con);
  }

  /** ****** test setup ********* */
  private static class CookieDisableTestSetup extends OneServerTestSetup {

    public CookieDisableTestSetup() {
      super(CookieDisableTest.class, CONTEXT);
      GenericServer.setDsoEnabled(true);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet("ShutdownNormallyServlet", "/ShutdownNormallyServlet/*", ShutdownNormallyServlet.class, null,
                         false);
      builder.addResourceFullpath("/com/tctest/server/appserver/unit/cookiedisabletest", "context.xml",
                                  "META-INF/context.xml");
    }

    protected void configureTcConfig(TcConfigBuilder clientConfig) {
      clientConfig.addWebApplication(CONTEXT);
    }
  }

}
