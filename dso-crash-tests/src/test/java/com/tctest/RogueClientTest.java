/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.IOUtils;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedVariable;

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
import com.tctest.runner.TransparentAppConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CyclicBarrier;

public class RogueClientTest extends TransparentTestBase {

  private static final int NODE_COUNT = 1;
  private int              port;
  private File             configFile;
  private int              adminPort;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
    TransparentAppConfig cfg = t.getTransparentAppConfig();
    cfg.setAttribute(RogueClientTestApp.CONFIG_FILE, configFile.getAbsolutePath());
    cfg.setAttribute(RogueClientTestApp.PORT_NUMBER, String.valueOf(port));
    cfg.setAttribute(RogueClientTestApp.HOST_NAME, "localhost");
    cfg.setAttribute(RogueClientTestApp.JMX_PORT, String.valueOf(adminPort));
  }

  @Override
  protected Class getApplicationClass() {
    return RogueClientTestApp.class;
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

    String testClassName = RogueClientTestApp.class.getName();
    InstrumentedClassConfigBuilder instrumented1 = new InstrumentedClassConfigBuilderImpl();
    instrumented1.setClassExpression(testClassName + "*");

    InstrumentedClassConfigBuilder instrumented2 = new InstrumentedClassConfigBuilderImpl();
    instrumented2.setClassExpression(RogueClientTestApp.L1Client.class.getName() + "*");

    InstrumentedClassConfigBuilder instrumented3 = new InstrumentedClassConfigBuilderImpl();
    instrumented3.setClassExpression(SynchronizedInt.class.getName() + "*");

    InstrumentedClassConfigBuilder instrumented4 = new InstrumentedClassConfigBuilderImpl();
    instrumented4.setClassExpression(SynchronizedVariable.class.getName() + "*");

    InstrumentedClassConfigBuilder instrumented5 = new InstrumentedClassConfigBuilderImpl();
    instrumented5.setClassExpression(CyclicBarrier.class.getName() + "*");

    out.getApplication()
        .getDSO()
        .setInstrumentedClasses(new InstrumentedClassConfigBuilder[] { instrumented1, instrumented2, instrumented3,
                                    instrumented4, instrumented5 });

    LockConfigBuilder lock1 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock1.setMethodExpression("* " + testClassName + "*.*(..)");
    lock1.setLockLevel(LockConfigBuilder.LEVEL_WRITE);

    LockConfigBuilder lock2 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock2.setMethodExpression("* " + RogueClientTestApp.L1Client.class.getName() + "*.*(..)");
    lock2.setLockLevel(LockConfigBuilder.LEVEL_WRITE);

    LockConfigBuilder lock3 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock3.setMethodExpression("* " + SynchronizedVariable.class.getName() + "*.*(..)");
    lock3.setLockLevel(LockConfigBuilder.LEVEL_WRITE);

    LockConfigBuilder lock4 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock4.setMethodExpression("* " + SynchronizedInt.class.getName() + "*.*(..)");
    lock4.setLockLevel(LockConfigBuilder.LEVEL_WRITE);

    LockConfigBuilder lock5 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock5.setMethodExpression("* " + CyclicBarrier.class.getName() + "*.*(..)");
    lock5.setLockLevel(LockConfigBuilder.LEVEL_WRITE);

    out.getApplication().getDSO().setLocks(new LockConfigBuilder[] { lock1, lock2, lock3, lock4, lock5 });

    RootConfigBuilder barrier = new RootConfigBuilderImpl(RogueClientTestApp.class, "barrier", "barrier");
    RootConfigBuilder finished = new RootConfigBuilderImpl(RogueClientTestApp.class, "finished", "finished");
    RootConfigBuilder l1ClientBarrier = new RootConfigBuilderImpl(RogueClientTestApp.L1Client.class, "barrier",
                                                                  "barrier");
    RootConfigBuilder l1Clientfinished = new RootConfigBuilderImpl(RogueClientTestApp.L1Client.class, "finished",
                                                                   "finished");
    RootConfigBuilder lbq = new RootConfigBuilderImpl(RogueClientTestApp.class, "lbqueue", "lbqueue");
    RootConfigBuilder nodeId = new RootConfigBuilderImpl(RogueClientTestApp.class, "nodeId", "nodeId");
    out.getApplication().getDSO()
        .setRoots(new RootConfigBuilder[] { barrier, finished, l1ClientBarrier, l1Clientfinished, lbq, nodeId });

    return out;
  }

}
