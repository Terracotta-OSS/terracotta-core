/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.webapp.servletfilters.SimpleFilter;
import com.tctest.webapp.servlets.ShutdownNormallyServlet;

import junit.framework.Test;

/**
 * Test session filter
 */
public final class SimpleSessionFilterTest extends AbstractTwoServerDeploymentTest {
  private static final String CONTEXT = "SimpleSessionFilterTest";
  private static final String SERVLET = "ShutdownNormallyServlet";
  
  public static Test suite() {
    return new SimpleSessionFilterTestSetup();
  }
  
  public void testFilter() throws Exception {
    WebConversation conversation = new WebConversation();

    WebResponse response1 = request(server0, "cmd=insert", conversation);
    assertEquals("OK", response1.getText().trim());

    WebResponse response2 = request(server1, "cmd=query", conversation);
    assertEquals("OK", response2.getText().trim());
  }

  private WebResponse request(WebApplicationServer server, String params, WebConversation con) throws Exception {
    return server.ping("/" + CONTEXT + "/" + SERVLET + "?" + params, con);
  }
  
  private static class SimpleSessionFilterTestSetup extends TwoServerTestSetup {
    
    public SimpleSessionFilterTestSetup() {
      super(SimpleSessionFilterTest.class, CONTEXT);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet("ShutdownNormallyServlet", "/" + SERVLET + "/*", ShutdownNormallyServlet.class, null, false);
      builder.addFilter("SimpleFilter", "/*", SimpleFilter.class, null);
    }
   
    protected void configureTcConfig(TcConfigBuilder clientConfig) {
      clientConfig.addWebApplication(CONTEXT);
      clientConfig.addInstrumentedClass(SimpleFilter.class.getName());
    }
  }
}
