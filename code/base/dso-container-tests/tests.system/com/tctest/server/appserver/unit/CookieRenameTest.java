/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.StandardAppServerParameters;
import com.tc.test.server.appserver.deployment.AbstractOneServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.webapp.servlets.ShutdownNormallyServlet;

import java.util.Arrays;
import java.util.List;

import junit.framework.Test;

/**
 * Test for CDV-544
 * 
 * @author hhuynh
 */
public class CookieRenameTest extends AbstractOneServerDeploymentTest {
  private static final String CONTEXT = "CookieRenameTest";
  private static final String SERVLET = "ShutdownNormallyServlet";

  public CookieRenameTest() {
    disableAllUntil("2008-06-01");
  }

  public static Test suite() {
    return new CookieRenameTestSetup();
  }

  private WebResponse request(WebApplicationServer server, String params, WebConversation con) throws Exception {
    return server.ping("/" + CONTEXT + "/" + SERVLET + "?" + params, con);
  }

  public void testCookieRename() throws Exception {
    WebConversation wc = new WebConversation();
    WebResponse wr = request(server0, "cmd=insert", wc);
    List names = Arrays.asList(wc.getCookieNames());
    System.out.println("Cookie names: " + names);
    assertTrue(names.contains("MY_SESSION_ID"));
    wr = request(server0, "cmd=query", wc);
    assertEquals("OK", wr.getText().trim());
  }

  private static class CookieRenameTestSetup extends OneServerTestSetup {

    public CookieRenameTestSetup() {
      super(CookieRenameTest.class, CONTEXT);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet(SERVLET, "/" + SERVLET + "/*", ShutdownNormallyServlet.class, null, false);
    }

    protected void configureTcConfig(TcConfigBuilder tcConfigBuilder) {
      tcConfigBuilder.addWebApplication(CONTEXT);
    }

    protected void configureServerParamers(StandardAppServerParameters params) {
      params.appendJvmArgs("-Dcom.tc.session.cookie.name=MY_SESSION_ID");
    }

  }
}
