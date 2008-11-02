/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.deployment.AbstractOneServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.util.TcConfigBuilder;

import javax.servlet.http.HttpServletResponse;

import junit.framework.Test;

public class ErrorPageTest extends AbstractOneServerDeploymentTest {

  private static final String CONTEXT = "ErrorPageTest";

  public static Test suite() {
    return new ErrorPageTestTestSetup();
  }

  public void test() throws Exception {

    WebConversation wc = new WebConversation();
    wc.getClientProperties().setAutoRedirect(false);
    String url = "http://localhost:" + server0.getPort() + "/" + CONTEXT + "/DOES_NOT_EXIST";
    wc.setExceptionsThrownOnErrorStatus(false);

    WebResponse response = wc.getResponse(url);

    assertEquals("*** This is the ErrorPageTest error page ***", response.getText().trim());
  }

  private static class ErrorPageTestTestSetup extends OneServerTestSetup {

    public ErrorPageTestTestSetup() {
      super(ErrorPageTest.class, CONTEXT);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addErrorPage(HttpServletResponse.SC_NOT_FOUND, "/error.html");
      builder.addResource("/com/tctest/server/appserver/unit/errorpagetest", "error.html", "");
    }

    protected void configureTcConfig(TcConfigBuilder tcConfigBuilder) {
      tcConfigBuilder.addWebApplication(CONTEXT);
    }

  }

}
