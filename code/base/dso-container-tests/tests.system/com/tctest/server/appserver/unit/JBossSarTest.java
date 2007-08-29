/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.LogFactory;
import org.jboss.logging.JBossJDKLogManager;
import org.jboss.mx.util.JBossNotificationBroadcasterSupport;
import org.jboss.system.ServiceMBeanSupport;
import org.jboss.xb.QNameBuilder;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.AppServerFactory;
import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.JARBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tc.util.runtime.Vm;
import com.tctest.service.DirectoryMonitor;
import com.tctest.webapp.servlets.JBossSarServlet;

import java.io.File;
import java.util.Date;

import junit.framework.Test;

public class JBossSarTest extends AbstractTwoServerDeploymentTest {
  private static final String CONTEXT          = "jbossSar";
  private static final String SERVLET          = "JBossSarServlet";
  protected boolean           parentDelegation = false;

  public JBossSarTest() {
    if (AppServerFactory.getCurrentAppServerId() != AppServerFactory.JBOSS || Vm.isJDK14()) {
      disableAllUntil(new Date(Long.MAX_VALUE));
    }
  }

  public static Test suite() {
    return new JBossSarTestSetup(JBossSarTest.class);
  }

  protected void setUp() throws Exception {
    super.setUp();
    File sarFile = buildSar();

    System.out.println("Deploying SAR...");
    File deploy = new File(server0.getWorkingDirectory(), "deploy");
    FileUtils.copyFileToDirectory(sarFile, deploy);
    deploy = new File(server1.getWorkingDirectory(), "deploy");
    FileUtils.copyFileToDirectory(sarFile, deploy);

    // give jboss server time to pick up the hot deploy sar
    Thread.sleep(10 * 1000);
  }

  public void testSar() throws Exception {    
    doTest();
  }

  protected void doTest() throws Exception {
    System.out.println("Hitting jboss servers...");
    WebResponse resp = request(server0, "", new WebConversation());
    System.out.println("server0 response: " + resp.getText());
    assertTrue(resp.getText().startsWith("OK"));
    assertContains("YMCA", resp.getText());
    
    resp = request(server1, "", new WebConversation());
    System.out.println("server1 response: " + resp.getText());
    assertTrue(resp.getText().startsWith("OK"));
    assertContains("YMCA", resp.getText());
  }

  private File buildSar() throws Exception {
    File sarFile = getTempFile("directory-monitor.sar");
    JARBuilder sar = new JARBuilder(sarFile.getName(), sarFile.getParentFile());

    sar.addResource("/com/tctest/service", "DirectoryMonitor.class", "com/tctest/service");
    sar.addResource("/com/tctest/service", "DirectoryMonitorMBean.class", "com/tctest/service");
    sar.addResource("/com/tctest/service", "DirectoryMonitor$ScannerThread.class", "com/tctest/service");
    sar.addDirectoryOrJARContainingClass(LogFactory.class);

    if (parentDelegation) {
      sar.addResourceFullpath("/jboss-sar", "jboss-service-true.xml", "META-INF/jboss-service.xml");
    } else {
      sar.addResourceFullpath("/jboss-sar", "jboss-service-false.xml", "META-INF/jboss-service.xml");
    }

    sar.finish();
    return sarFile;
  }

  private WebResponse request(WebApplicationServer server, String params, WebConversation con) throws Exception {
    return server.ping("/" + CONTEXT + "/" + SERVLET + "?" + params, con);
  }

  static class JBossSarTestSetup extends TwoServerTestSetup {

    public JBossSarTestSetup(Class testClass) {
      super(testClass, CONTEXT);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addDirectoryOrJARContainingClass(DirectoryMonitor.class);
      builder.addDirectoryOrJARContainingClass(ServiceMBeanSupport.class);
      builder.addDirectoryOrJARContainingClass(JBossNotificationBroadcasterSupport.class);
      builder.addDirectoryOrJARContainingClass(JBossJDKLogManager.class);
      builder.addDirectoryOrJARContainingClass(QNameBuilder.class);

      builder.addServlet(SERVLET, "/" + SERVLET + "/*", JBossSarServlet.class, null, true);
    }

    protected void configureTcConfig(TcConfigBuilder clientConfig) {
      clientConfig.addWebApplication(CONTEXT);
      clientConfig.addInstrumentedClass(DirectoryMonitor.class.getName());

      clientConfig.addRoot(DirectoryMonitor.class.getName() + ".list", "list");
      clientConfig.addRoot(JBossSarServlet.class.getName() + ".list", "list");

      clientConfig.addAutoLock("* " + DirectoryMonitor.class.getName() + ".*(..)", "write");
      clientConfig.addAutoLock("* " + JBossSarServlet.class.getName() + ".*(..)", "write");
    }
  }

}
