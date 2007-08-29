/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.deployment.AbstractOneServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.webapp.servlets.RequestForwardTestForwardeeServlet;
import com.tctest.webapp.servlets.RequestForwardTestForwarderServlet;

import junit.framework.Test;

public class RequestForwardTest extends AbstractOneServerDeploymentTest {
  private static final String CONTEXT  = "RequestForward";
  private static final String MAPPING1 = "RequestForwardTestForwarderServlet";
  private static final String MAPPING2 = "RequestForwardTestForwardeeServlet";

  public static Test suite() {
    return new RequestForwardTestSetup();
  }
  
  private WebResponse request(WebApplicationServer server, String mapping, String params, WebConversation wc)
      throws Exception {
    return server.ping("/" + CONTEXT + "/" + mapping + "?" + params, wc);
  }

  public void testSessionForwardSession() throws Exception {
    WebConversation conversation = new WebConversation();
    WebResponse response = request(server0, MAPPING1, "action=0", conversation);
    assertEquals("INVALID REQUEST", response.getText().trim());

    response = request(server0, MAPPING1, "action=s-f-s&target=" + MAPPING2, conversation);
    assertEquals("FORWARD GOT SESSION", response.getText().trim());
  }

   public void testForwardSession() throws Exception {
     WebConversation conversation = new WebConversation();
     WebResponse response = request(server0, MAPPING1, "action=0", conversation);
     assertEquals("INVALID REQUEST", response.getText().trim());

     response = request(server0, MAPPING1, "action=n-f-s&target=" + MAPPING2, conversation);
     assertEquals("FORWARD GOT SESSION", response.getText().trim());
   }
  
   public void testSessionForward() throws Exception {
     WebConversation conversation = new WebConversation();
     WebResponse response = request(server0, MAPPING1, "action=0", conversation);
     assertEquals("INVALID REQUEST", response.getText().trim());

     response = request(server0, MAPPING1, "action=s-f-n&target=" + MAPPING2, conversation);
     assertEquals("FORWARD DID NOT GET SESSION", response.getText().trim());
   }

  private static class RequestForwardTestSetup extends OneServerTestSetup {

    public RequestForwardTestSetup() {
      super(RequestForwardTest.class, CONTEXT);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet("RequestForwardTestForwarderServlet", "/" + MAPPING1 + "/*",
                         RequestForwardTestForwarderServlet.class, null, false);
      builder.addServlet("RequestForwardTestForwardeeServlet", "/" + MAPPING2 + "/*",
                         RequestForwardTestForwardeeServlet.class, null, false);
    }

    protected void configureTcConfig(TcConfigBuilder tcConfigBuilder) {
      tcConfigBuilder.addWebApplication(CONTEXT);
    }

  }
}
