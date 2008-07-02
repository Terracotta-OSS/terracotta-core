/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import org.apache.commons.lang.ClassUtils;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.deployment.AbstractOneServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.webapp.servlets.ResponseIsCommittedServlet;

import javax.servlet.http.HttpServletResponse;

import junit.framework.Test;

public class ResponseIsCommittedTest extends AbstractOneServerDeploymentTest {

  private static final String CONTEXT = "ResponseIsCommitted";
  private static final String MAPPING = "Servlet";

  public static Test suite() {
    return new ResponseIsCommittedTestTestSetup();
  }

  public void test() throws Exception {
    WebResponse response;

    response = request("/" + CONTEXT + "/" + MAPPING, "cmd=sendRedirect");
    assertEquals(HttpServletResponse.SC_MOVED_TEMPORARILY, response.getResponseCode());

    response = request("/" + CONTEXT + "/" + MAPPING, "cmd=check-sendRedirect");
    assertEquals("true", response.getText().trim());
  }

  private WebResponse request(String request, String query) throws Exception {
    WebConversation wc = new WebConversation();
    wc.getClientProperties().setAutoRedirect(false);
    String url = "http://localhost:" + server0.getPort() + request + "?" + query;
    wc.setExceptionsThrownOnErrorStatus(false);
    return wc.getResponse(url);
  }

  private static class ResponseIsCommittedTestTestSetup extends OneServerTestSetup {

    public ResponseIsCommittedTestTestSetup() {
      super(ResponseIsCommittedTest.class, CONTEXT);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet(ClassUtils.getShortClassName(ResponseIsCommittedServlet.class), "/" + MAPPING + "/*",
                         ResponseIsCommittedServlet.class, null, false);
    }

    protected void configureTcConfig(TcConfigBuilder tcConfigBuilder) {
      tcConfigBuilder.addWebApplication(CONTEXT);
    }

  }

}
