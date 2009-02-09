/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.deployment.AbstractOneServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.util.TcConfigBuilder;
import com.tctest.webapp.servlets.ErrorRedirectedServlet;

import javax.servlet.http.HttpServletResponse;

import junit.framework.Test;

public class ErrorPageRedirectedTest extends AbstractOneServerDeploymentTest {

  private static final String CONTEXT = "ErrorPageRedirectedTest";

  public static Test suite() {
    return new ErrorPageRedirectedTestSetup();
  }

  public void test() throws Exception {

    WebConversation wc = new WebConversation();
    wc.getClientProperties().setAutoRedirect(false);
    String url = "http://localhost:" + this.server0.getPort() + "/" + CONTEXT + "/DOES_NOT_EXIST";
    wc.setExceptionsThrownOnErrorStatus(false);

    WebResponse response = wc.getResponse(url);
    assertEquals(HttpServletResponse.SC_MOVED_TEMPORARILY, response.getResponseCode());
  }

  private static class ErrorPageRedirectedTestSetup extends OneServerTestSetup {

    public ErrorPageRedirectedTestSetup() {
      super(ErrorPageRedirectedTest.class, CONTEXT);
    }

    @Override
    protected void configureWar(final DeploymentBuilder builder) {
      builder.addServlet("errorServlet", "/errorServlet/*", ErrorRedirectedServlet.class, null, false);
      builder.addErrorPage(HttpServletResponse.SC_NOT_FOUND, "/errorServlet/");
    }

    @Override
    protected void configureTcConfig(final TcConfigBuilder tcConfigBuilder) {
      tcConfigBuilder.addWebApplication(CONTEXT);
    }

  }

}
