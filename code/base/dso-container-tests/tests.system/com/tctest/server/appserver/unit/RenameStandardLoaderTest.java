/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.deployment.AbstractOneServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.webapp.servlets.StandardLoaderServlet;

import junit.framework.Test;

public class RenameStandardLoaderTest extends AbstractOneServerDeploymentTest {

  private static final String CONTEXT = "simple";

  public static Test suite() {
    return new RenameStandardLoaderSetup();
  }

  public void testSession() throws Exception {
    System.out.println("DSO port: " + getServerManager().getServerTcConfig().getDsoPort());
    System.out.println("tc-config file: " + server0.getTcConfigFile());
    
    WebConversation conversation = new WebConversation();

    WebResponse response1 = request(server0, "cmd=insert", conversation);
    assertEquals("OK", response1.getText().trim());
  }

  private WebResponse request(WebApplicationServer server, String params, WebConversation con) throws Exception {
    return server.ping("/" + CONTEXT + "/" + CONTEXT + "?" + params, con);
  }

  /** ****** test setup ********* */
  private static class RenameStandardLoaderSetup extends OneServerTestSetup {

    public RenameStandardLoaderSetup() {
      super(RenameStandardLoaderTest.class, CONTEXT);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet("StandardLoaderServlet", "/" + CONTEXT + "/*", StandardLoaderServlet.class, null, false);
    }

    protected void configureTcConfig(TcConfigBuilder tcConfigBuilder) {
      String rootName = "sharedMap";
      String fieldName = StandardLoaderServlet.class.getName() + ".sharedMap";
      tcConfigBuilder.addRoot(fieldName, rootName);

      String methodExpression = "* " + StandardLoaderServlet.class.getName() + ".*(..)";
      tcConfigBuilder.addAutoLock(methodExpression, "write");

      tcConfigBuilder.addInstrumentedClass(StandardLoaderServlet.class.getName() + "$Inner", false);
    }

  }

}
