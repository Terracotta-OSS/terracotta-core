/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.restart.system;

import org.apache.commons.io.IOUtils;

import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.schema.builder.RootConfigBuilder;
import com.tc.config.schema.test.InstrumentedClassConfigBuilderImpl;
import com.tc.config.schema.test.RootConfigBuilderImpl;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tctest.RemoveObjectsTestApp;
import com.tctest.TestConfigurator;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.TransparentAppConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;

public class RemoveObjectsTest extends TransparentTestBase implements TestConfigurator {

  private int  clientCount = 2;
  private int  port;
  private File configFile;
  private int  adminPort;

  protected Class getApplicationClass() {
    return RemoveObjectsTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(clientCount).setIntensity(1);
    t.initializeTestRunner();
    TransparentAppConfig cfg = t.getTransparentAppConfig();
    cfg.setAttribute(RemoveObjectsTestApp.CONFIG_FILE, configFile.getAbsolutePath());
    cfg.setAttribute(RemoveObjectsTestApp.PORT_NUMBER, String.valueOf(port));
    cfg.setAttribute(RemoveObjectsTestApp.HOST_NAME, "localhost");
    cfg.setAttribute(RemoveObjectsTestApp.JMX_PORT, String.valueOf(adminPort));
  }

  public void setUp() throws Exception {
    PortChooser pc = new PortChooser();
    port = pc.chooseRandomPort();
    adminPort = pc.chooseRandomPort();
    configFile = getTempFile("config-file.xml");
    writeConfigFile();
    setUpControlledServer(configFactory(), configHelper(), port, adminPort, configFile.getAbsolutePath());
    doSetUp(this);
  }

  private synchronized void writeConfigFile() {
    try {
      TerracottaConfigBuilder builder = createConfig(port, adminPort);
      FileOutputStream out = new FileOutputStream(configFile);
      StringReader reader = new StringReader(builder.toString());
      IOUtils.copy(reader, out);
      out.close();
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    }
  }

  private TerracottaConfigBuilder createConfig(int port1, int adminPort1) {
    TerracottaConfigBuilder out = new TerracottaConfigBuilder();

    out.getServers().getL2s()[0].setDSOPort(port1);
    out.getServers().getL2s()[0].setJMXPort(adminPort1);

    // roots
    RootConfigBuilder[] roots = new RootConfigBuilder[] {
        new RootConfigBuilderImpl(getApplicationClass(), "sharedRoot"),
        new RootConfigBuilderImpl(getApplicationClass(), "myLock"),
        new RootConfigBuilderImpl(getApplicationClass(), "rwLock"),
        new RootConfigBuilderImpl(getApplicationClass(), "barrier") };
    out.getApplication().getDSO().setRoots(roots);

    // inst classes
    InstrumentedClassConfigBuilder instrumented1 = new InstrumentedClassConfigBuilderImpl();
    instrumented1.setClassExpression(getApplicationClass().getName());
    out.getApplication().getDSO().setInstrumentedClasses(new InstrumentedClassConfigBuilder[] { instrumented1 });

    out.getServers().getL2s()[0].setGCEnabled(true);
    out.getServers().getL2s()[0].setGCVerbose(true);
    out.getServers().getL2s()[0].setGCInterval(10);
    return out;
  }

}
