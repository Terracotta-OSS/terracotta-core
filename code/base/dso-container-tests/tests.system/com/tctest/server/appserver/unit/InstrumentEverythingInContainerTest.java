/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.deployment.AbstractDeploymentTest;
import com.tc.test.server.appserver.deployment.Deployment;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.ServerTestSetup;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tc.util.runtime.Vm;
import com.tctest.webapp.servlets.OkServlet;

import junit.framework.Test;

public class InstrumentEverythingInContainerTest extends AbstractDeploymentTest {
  private static final String CONTEXT = "OkServlet";
  private Deployment          deployment;

  public static Test suite() {
    return new ServerTestSetup(InstrumentEverythingInContainerTest.class);
  }

  protected boolean isSessionTest() {
    return false;
  }

  public void setUp() throws Exception {
    super.setUp();
    if (deployment == null) deployment = makeDeployment();
  }

  public void testInstrumentEverything() throws Exception {
    TcConfigBuilder tcConfigBuilder = new TcConfigBuilder();
    tcConfigBuilder.addInstrumentedClass("*..*");
    // These bytes are obfuscated and get verify errors when instrumented by DSO
    tcConfigBuilder.addExclude("com.sun.crypto.provider..*");    

    WebApplicationServer server = makeWebApplicationServer(tcConfigBuilder);
    server.addWarDeployment(deployment, CONTEXT);
    if (!Vm.isIBM()) {
      // InstrumentEverythingInContainerTest under glassfish needs this
      server.getServerParameters().appendJvmArgs("-XX:MaxPermSize=128m");
    }
    server.start();

    WebConversation conversation = new WebConversation();
    WebResponse response = server.ping("/OkServlet/ok", conversation);
    assertEquals("OK", response.getText().trim());
  }

  private Deployment makeDeployment() throws Exception {
    DeploymentBuilder builder = makeDeploymentBuilder(CONTEXT + ".war");
    builder.addServlet("OkServlet", "/ok/*", OkServlet.class, null, false);
    return builder.makeDeployment();
  }
}
