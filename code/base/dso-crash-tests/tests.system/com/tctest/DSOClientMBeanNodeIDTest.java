/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.IOUtils;

import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.schema.builder.LockConfigBuilder;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.config.schema.test.InstrumentedClassConfigBuilderImpl;
import com.tc.config.schema.test.L2ConfigBuilder;
import com.tc.config.schema.test.LockConfigBuilderImpl;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tctest.runner.TransparentAppConfig;

import java.io.File;
import java.io.FileOutputStream;

public class DSOClientMBeanNodeIDTest extends TransparentTestBase {

  private static final int NODE_COUNT = 1;
  private int              port;
  private File             configFile;
  private int              adminPort;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
    TransparentAppConfig cfg = t.getTransparentAppConfig();
    cfg.setAttribute(DSOClientMBeanNodeIDTestApp.CONFIG_FILE, configFile.getAbsolutePath());
    cfg.setAttribute(DSOClientMBeanNodeIDTestApp.PORT_NUMBER, String.valueOf(port));
    cfg.setAttribute(DSOClientMBeanNodeIDTestApp.HOST_NAME, "localhost");
    cfg.setAttribute(DSOClientMBeanNodeIDTestApp.JMX_PORT, String.valueOf(adminPort));
  }

  protected Class getApplicationClass() {
    return DSOClientMBeanNodeIDTestApp.class;
  }

  public void setUp() throws Exception {
    PortChooser pc = new PortChooser();
    port = pc.chooseRandomPort();
    adminPort = pc.chooseRandomPort();
    configFile = getTempFile("tc-config.xml");
    writeConfigFile();
    TestTVSConfigurationSetupManagerFactory factory = new TestTVSConfigurationSetupManagerFactory(
                                                                                                  TestTVSConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                                                                  null,
                                                                                                  new FatalIllegalConfigurationChangeHandler());

    factory.addServerToL1Config(null, port, adminPort);
    L1TVSConfigurationSetupManager manager = factory.createL1TVSConfigurationSetupManager();
    setUpControlledServer(factory, new StandardDSOClientConfigHelperImpl(manager), port, adminPort, configFile
        .getAbsolutePath());
    doSetUp(this);
  }

  private synchronized void writeConfigFile() {
    try {
      TerracottaConfigBuilder builder = createConfig(port, adminPort);
      FileOutputStream out = new FileOutputStream(configFile);
      IOUtils.write(builder.toString(), out);
      out.close();
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    }
  }

  public static TerracottaConfigBuilder createConfig(int port, int adminPort) {
    TerracottaConfigBuilder tcConfigBuilder = new TerracottaConfigBuilder();

    tcConfigBuilder.getServers().getL2s()[0].setDSOPort(port);
    tcConfigBuilder.getServers().getL2s()[0].setJMXPort(adminPort);
    tcConfigBuilder.getServers().getL2s()[0].setPersistenceMode(L2ConfigBuilder.PERSISTENCE_MODE_PERMANENT_STORE);

    String testClassName = DSOClientMBeanNodeIDTest.class.getName();
    String testClassAppName = DSOClientMBeanNodeIDTestApp.class.getName();

    InstrumentedClassConfigBuilder instrumented1 = new InstrumentedClassConfigBuilderImpl();
    instrumented1.setClassExpression(testClassName + "*");

    InstrumentedClassConfigBuilder instrumented2 = new InstrumentedClassConfigBuilderImpl();
    instrumented2.setClassExpression(testClassAppName + "*");

    tcConfigBuilder.getApplication().getDSO().setInstrumentedClasses(
                                                                     new InstrumentedClassConfigBuilder[] {
                                                                         instrumented1, instrumented2 });

    LockConfigBuilder lock = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock.setMethodExpression("* " + testClassAppName + "*.*(..)");
    lock.setLockLevel(LockConfigBuilder.LEVEL_WRITE);
    tcConfigBuilder.getApplication().getDSO().setLocks(new LockConfigBuilder[] { lock });

    return tcConfigBuilder;
  }

}