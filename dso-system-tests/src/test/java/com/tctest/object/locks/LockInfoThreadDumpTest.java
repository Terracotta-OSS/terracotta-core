package com.tctest.object.locks;

import org.apache.commons.io.IOUtils;

import EDU.oswego.cs.dl.util.concurrent.CountDown;

import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.schema.builder.LockConfigBuilder;
import com.tc.config.schema.builder.RootConfigBuilder;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.config.test.schema.InstrumentedClassConfigBuilderImpl;
import com.tc.config.test.schema.L2ConfigBuilder;
import com.tc.config.test.schema.LockConfigBuilderImpl;
import com.tc.config.test.schema.RootConfigBuilderImpl;
import com.tc.config.test.schema.TerracottaConfigBuilder;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

import java.io.File;
import java.io.FileOutputStream;

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

public class LockInfoThreadDumpTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;
  private int              port;
  private int              adminPort;
  private File             configFile;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
    t.getTransparentAppConfig().setAttribute(LockInfoThreadDumpTestApp.JMXPORT, String.valueOf(adminPort));
  }

  @Override
  protected Class getApplicationClass() {
    return LockInfoThreadDumpTestApp.class;
  }

  @Override
  public void setUp() throws Exception {
    PortChooser pc = new PortChooser();
    port = pc.chooseRandomPort();
    adminPort = pc.chooseRandomPort();
    int groupPort = pc.chooseRandomPort();
    configFile = getTempFile("tc-config.xml");
    writeConfigFile();
    TestConfigurationSetupManagerFactory factory = new TestConfigurationSetupManagerFactory(
                                                                                            TestConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                                                            null,
                                                                                            new FatalIllegalConfigurationChangeHandler());

    L1ConfigurationSetupManager manager = factory.getL1TVSConfigurationSetupManager();
    setUpControlledServer(factory, new StandardDSOClientConfigHelperImpl(manager), port, adminPort, groupPort,
                          configFile.getAbsolutePath());
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
    TerracottaConfigBuilder out = new TerracottaConfigBuilder();
    out.getServers().getL2s()[0].setDSOPort(port);
    out.getServers().getL2s()[0].setJMXPort(adminPort);
    out.getServers().getL2s()[0].setPersistenceMode(L2ConfigBuilder.PERSISTENCE_MODE_PERMANENT_STORE);

    TerracottaConfigBuilder cb = out;
    // locks
    LockConfigBuilder[] locks = new LockConfigBuilder[] {
        new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK, Object.class, LockConfigBuilder.LEVEL_WRITE),
        new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK, LockInfoThreadDumpTestApp.class,
                                  LockConfigBuilder.LEVEL_WRITE) };

    cb.getApplication().getDSO().setLocks(locks);

    // include instr classes
    InstrumentedClassConfigBuilder[] instrClasses = new InstrumentedClassConfigBuilder[] {
        new InstrumentedClassConfigBuilderImpl(Object.class),
        new InstrumentedClassConfigBuilderImpl(LockInfoThreadDumpTestApp.class),
        new InstrumentedClassConfigBuilderImpl(LockInfoThreadDumpTestApp.LockInfoDeadLockAction.class),
        new InstrumentedClassConfigBuilderImpl(CountDown.class),
        new InstrumentedClassConfigBuilderImpl(java.util.concurrent.CyclicBarrier.class) };

    cb.getApplication().getDSO().setInstrumentedClasses(instrClasses);

    // roots
    RootConfigBuilder l1 = new RootConfigBuilderImpl(LockInfoThreadDumpTestApp.class, "lock1", "lock1");
    RootConfigBuilder l2 = new RootConfigBuilderImpl(LockInfoThreadDumpTestApp.class, "lock2", "lock2");
    RootConfigBuilder l3 = new RootConfigBuilderImpl(LockInfoThreadDumpTestApp.class, "lock3", "lock3");
    RootConfigBuilder l4 = new RootConfigBuilderImpl(LockInfoThreadDumpTestApp.class, "count", "count");
    RootConfigBuilder l5 = new RootConfigBuilderImpl(LockInfoThreadDumpTestApp.class, "barrier", "barrier");

    cb.getApplication().getDSO().setRoots(new RootConfigBuilder[] { l1, l2, l3, l4, l5 });

    // methods
    LockConfigBuilder lock1 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock1.setMethodExpression("* " + LockInfoThreadDumpTestApp.class.getName() + "*.*(..)");
    lock1.setLockLevel(LockConfigBuilder.LEVEL_WRITE);

    LockConfigBuilder lock3 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock3.setMethodExpression("* " + CountDown.class.getName() + "*.*(..)");
    lock3.setLockLevel(LockConfigBuilder.LEVEL_WRITE);

    LockConfigBuilder lock2 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock2.setMethodExpression("* " + LockInfoThreadDumpTestApp.LockInfoDeadLockAction.class.getName() + "*.*(..)");
    lock2.setLockLevel(LockConfigBuilder.LEVEL_WRITE);

    cb.getApplication().getDSO().setLocks(new LockConfigBuilder[] { lock1, lock2, lock3 });

    return out;
  }
}