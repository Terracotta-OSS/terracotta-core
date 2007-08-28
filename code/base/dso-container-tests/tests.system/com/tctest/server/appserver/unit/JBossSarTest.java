/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.tc.test.server.appserver.AppServerFactory;
import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.util.TcConfigBuilder;
import com.tc.util.JarBuilder;
import com.tctest.service.DirectoryMonitor;

import java.io.File;
import java.util.Date;

import junit.framework.Test;

public class JBossSarTest extends AbstractTwoServerDeploymentTest {
  private static final String CONTEXT = "jbossSar";

  public JBossSarTest() {
    if (AppServerFactory.getCurrentAppServerId() != AppServerFactory.JBOSS) {
      disableAllUntil(new Date(Long.MAX_VALUE));
    }
  }

  public static Test suite() {
    return new JBossSarTestSetup();
  }

  public void testSar() throws Exception {
    File sarFile = getTempFile("directory-monitor.sar");
    JarBuilder sar = new JarBuilder(sarFile);
    sar.putDirEntry("com/tctest/service");
    sar.putEntry("com/tctest/service/DirectoryMonitor.class",
                 readResource("/com/tctest/service/DirectoryMonitor.class"));
    sar.putEntry("com/tctest/service/DirectoryMonitorMBean.class",
                 readResource("/com/tctest/service/DirectoryMonitorMBean.class"));
    sar.putEntry("com/tctest/service/DirectoryMonitor$ScannerThread.class",
                 readResource("/com/tctest/service/DirectoryMonitor$ScannerThread.class"));
    sar.putEntry("META-INF/jboss-service.xml", readResource("/jboss-sar/jboss-service.xml"));
    sar.finish();

    File deploy = new File(server0.getWorkingDirectory(), "deploy");
    FileUtils.copyFileToDirectory(sarFile, deploy);
    deploy = new File(server1.getWorkingDirectory(), "deploy");
    FileUtils.copyFileToDirectory(sarFile, deploy);
    Thread.sleep(5000);
  }

  private byte[] readResource(String name) throws Exception {
    return IOUtils.toByteArray(getClass().getResourceAsStream(name));
  }

  private static class JBossSarTestSetup extends TwoServerTestSetup {

    public JBossSarTestSetup() {
      super(JBossSarTest.class, CONTEXT);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addResource("/jboss-sar", "jboss-service.xml", "META-INF");
      builder.addResource("/com/tctest/service", "DirectoryMonitor.class", "com/tctest/service");
      builder.addResource("/com/tctest/service", "DirectoryMonitorMBean.class", "com/tctest/service");

    }

    protected void configureTcConfig(TcConfigBuilder clientConfig) {
      clientConfig.addWebApplication(CONTEXT);
      clientConfig.addInstrumentedClass(DirectoryMonitor.class.getName());
      clientConfig.addRoot(DirectoryMonitor.class.getName() + ".list", "list");
      clientConfig.addAutoLock("* " + DirectoryMonitor.class.getName() + ".*(..)", "write");
    }
  }

}
