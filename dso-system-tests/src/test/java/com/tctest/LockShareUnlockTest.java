/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.filters.StringInputStream;

import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.schema.builder.LockConfigBuilder;
import com.tc.config.schema.builder.RootConfigBuilder;
import com.tc.config.test.schema.InstrumentedClassConfigBuilderImpl;
import com.tc.config.test.schema.LockConfigBuilderImpl;
import com.tc.config.test.schema.RootConfigBuilderImpl;
import com.tc.config.test.schema.TerracottaConfigBuilder;
import com.tc.util.PortChooser;
import com.tctest.runner.TransparentAppConfig;

import java.io.File;
import java.io.FileOutputStream;

import junit.framework.Assert;

public class LockShareUnlockTest extends TransparentTestBase {

  private static final int NODE_COUNT = 1;

  private int              port;
  private File             configFile;
  private int              jmxPort;
  private int              groupPort;

  public LockShareUnlockTest() {
    timebombTestForRewrite();
  }

  @Override
  protected Class getApplicationClass() {
    return LockShareUnlockTestApp.class;
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
    TransparentAppConfig cfg = t.getTransparentAppConfig();
    cfg.setAttribute(LockShareUnlockTestApp.CONFIG_FILE, configFile.getAbsolutePath());
    cfg.setAttribute(LockShareUnlockTestApp.PORT_NUMBER, String.valueOf(port));
    cfg.setAttribute(LockShareUnlockTestApp.HOST_NAME, "localhost");
    cfg.setAttribute(LockShareUnlockTestApp.JMX_PORT, String.valueOf(jmxPort));
  }

  @Override
  public void setUp() throws Exception {
    PortChooser pc = new PortChooser();
    port = pc.chooseRandomPort();
    jmxPort = pc.chooseRandomPort();
    groupPort = pc.chooseRandomPort();
    configFile = getTempFile("tc-config.xml");
    writeConfigFile();

    setUpControlledServer(configFactory(), configHelper(), port, jmxPort, groupPort, configFile.getAbsolutePath(), null);
    doSetUp(this);
  }

  private synchronized void writeConfigFile() {
    try {
      TerracottaConfigBuilder builder = createConfig();
      FileOutputStream out = new FileOutputStream(configFile);
      IOUtils.copy(new StringInputStream(builder.toString()), out);
      out.close();
    } catch (Exception e) {
      Assert.fail("Can't create config file " + e);
    }
  }

  private TerracottaConfigBuilder createConfig() {
    TerracottaConfigBuilder tcConfigBuilder = new TerracottaConfigBuilder();

    tcConfigBuilder.getServers().getL2s()[0].setDSOPort(port);
    tcConfigBuilder.getServers().getL2s()[0].setJMXPort(jmxPort);
    tcConfigBuilder.getServers().getL2s()[0].setL2GroupPort(groupPort);

    tcConfigBuilder.getClient().setFaultCount(1);

    String testClassAppName = LockShareUnlockTestApp.class.getName();

    InstrumentedClassConfigBuilder instrumented = new InstrumentedClassConfigBuilderImpl();
    instrumented.setClassExpression(testClassAppName + "*");

    tcConfigBuilder.getApplication().getDSO()
        .setInstrumentedClasses(new InstrumentedClassConfigBuilder[] { instrumented });

    Class<?> klass = LockShareUnlockTestApp.class;
    RootConfigBuilder[] roots = { new RootConfigBuilderImpl(klass, "root", "root") };
    tcConfigBuilder.getApplication().getDSO().setRoots(roots);

    LockConfigBuilder lock1 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock1.setMethodExpression("* " + klass.getName() + ".addToRoot(..)");
    lock1.setLockLevel(LockConfigBuilder.LEVEL_WRITE);

    LockConfigBuilder lock2 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock2.setMethodExpression("* " + klass.getName() + ".testUnbalancedWriteWrite(..)");
    lock2.setLockLevel(LockConfigBuilder.LEVEL_WRITE);

    LockConfigBuilder lock3 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock3.setMethodExpression("* " + klass.getName() + ".testUnbalancedReadWrite(..)");
    lock3.setLockLevel(LockConfigBuilder.LEVEL_READ);

    tcConfigBuilder.getApplication().getDSO().setLocks(new LockConfigBuilder[] { lock1, lock2, lock3 });

    return tcConfigBuilder;
  }
}
