/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.test.server.appserver.deployment.AbstractOneServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.externall1.StandardLoaderApp;
import com.tctest.webapp.servlets.StandardLoaderServlet;

import java.util.Collections;

import junit.framework.Test;

public class RenameStandardLoaderTest extends AbstractOneServerDeploymentTest {

  private static final String CONTEXT = "simple";
  
  public RenameStandardLoaderTest() {
    this.disableAllUntil("2008-12-10");
  }

  public static Test suite() {
    return new RenameStandardLoaderSetup();
  }

  public void testSession() throws Exception {
    System.out.println("DSO port: " + getServerManager().getServerTcConfig().getDsoPort());
    System.out.println("tc-config file: " + server0.getTcConfigFile());

    WebConversation conversation = new WebConversation();

    WebResponse response1 = request(server0, "cmd=insert", conversation);
    assertEquals("OK", response1.getText().trim());

    int exitCode = spawnExtraL1();
    assertEquals(0, exitCode);

  }

  private int spawnExtraL1() throws Exception {

    ExtraL1ProcessControl client = new ExtraL1ProcessControl(getServerManager().getServerTcConfig().getDsoHost(),
                                                             getServerManager().getServerTcConfig().getDsoPort(),
                                                             StandardLoaderApp.class, server0.getTcConfigFile()
                                                                 .getAbsolutePath(), new String[] {}, server0
                                                                 .getWorkingDirectory(), Collections.EMPTY_LIST);
    client.start();
    return client.waitFor();
    
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
      
      
      fieldName = StandardLoaderApp.class.getName() + ".sharedMap";
      tcConfigBuilder.addRoot(fieldName, rootName);
    }

  }

}
