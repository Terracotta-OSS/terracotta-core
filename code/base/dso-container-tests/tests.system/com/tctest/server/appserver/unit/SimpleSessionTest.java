/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.webapp.servlets.SimpleDsoSessionsTestServlet;

import junit.framework.Test;

public class SimpleSessionTest  extends AbstractTwoServerDeploymentTest {
  private static final String CONFIG_FILE_FOR_TEST = "/tc-config-files/simplesession-tc-config.xml";
  private static final String CONTEXT = "simple";
  private static final String MAPPING = "doit";
  
  public static Test suite() {
    return new SimpleSessionTestSetup();
  }
  
  public void testSession() throws Exception {
    WebConversation conversation = new WebConversation();

    WebResponse response1 = request(server1, "server=0", conversation);
    assertEquals("OK", response1.getText().trim());

    WebResponse response2 = request(server2, "server=1", conversation);
    assertEquals("OK", response2.getText().trim());
  }
  
  private WebResponse request(WebApplicationServer server, String params, WebConversation con) throws Exception {
    return server.ping("/" + CONTEXT + "/" + MAPPING + "?" + params, con);
  }
  
  /******** test setup **********/
  private static class SimpleSessionTestSetup extends TwoServerTestSetup {
    
    public SimpleSessionTestSetup() {
      super(SimpleSessionTest.class, CONFIG_FILE_FOR_TEST, CONTEXT);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet("SimpleDsoSessionsTestServlet", "/" + MAPPING + "/*", SimpleDsoSessionsTestServlet.class, null, false);      
    }
    
    protected void configureTcConfig(TcConfigBuilder tcConfigBuilder) {
      tcConfigBuilder.addWebApplication("test");
    }
    
  }

}
