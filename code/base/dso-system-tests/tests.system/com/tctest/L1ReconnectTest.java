/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.IOUtils;

import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.schema.builder.LockConfigBuilder;
import com.tc.config.schema.builder.RootConfigBuilder;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.config.schema.test.InstrumentedClassConfigBuilderImpl;
import com.tc.config.schema.test.LockConfigBuilderImpl;
import com.tc.config.schema.test.RootConfigBuilderImpl;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tctest.runner.AbstractTransparentApp;
import com.tctest.runner.TransparentAppConfig;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

public class L1ReconnectTest extends TransparentTestBase {

  public static final int NODE_COUNT = 1;
  private int             port;
  private File            configFile;
  private int             adminPort;

  public L1ReconnectTest() {
    this.disableAllUntil("2007-12-25");
    /*
     * The proxy test in this test is not proper, the proxy is started too late.
     * And observered a L1 run without going through proxy. No trivial to change.
     * Sine we have proxy test framework already. This test is good to be deleted.
     */
  }
  
  protected boolean enableL1Reconnect() {
    return true;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
    TransparentAppConfig cfg = t.getTransparentAppConfig();
    cfg.setAttribute(L1ReconnectTestApp.CONFIG_FILE, configFile.getAbsolutePath());
    cfg.setAttribute(L1ReconnectTestApp.PORT_NUMBER, String.valueOf(port));
    cfg.setAttribute(L1ReconnectTestApp.HOST_NAME, "localhost");
  }

  protected Class getApplicationClass() {
    return L1ReconnectTestApp.class;
  }

  public void setUp() throws Exception {
    
    ArrayList jvmArgs = new ArrayList();
    if (enableL1Reconnect()) {
      setJvmArgsL1Reconnect(jvmArgs);
    }

    PortChooser pc = new PortChooser();
    port = pc.chooseRandomPort();
    adminPort = pc.chooseRandomPort();
    configFile = getTempFile("tc-config.xml");
    writeConfigFile();

    TestTVSConfigurationSetupManagerFactory factory = new TestTVSConfigurationSetupManagerFactory(
                                                                                                  TestTVSConfigurationSetupManagerFactory.MODE_DISTRIBUTED_CONFIG,
                                                                                                  null,
                                                                                                  new FatalIllegalConfigurationChangeHandler());

    factory.addServerToL1Config(null, port, adminPort);
    L1TVSConfigurationSetupManager manager = factory.createL1TVSConfigurationSetupManager();
    setUpControlledServer(factory, new StandardDSOClientConfigHelper(manager), port, adminPort, configFile
        .getAbsolutePath(), jvmArgs);

    doSetUp(this);
  }

  private synchronized void writeConfigFile() {
    PrintWriter out = null;
    try {
      TerracottaConfigBuilder builder = createConfig();
      out = new PrintWriter(new FileWriter(configFile));
      out.println(builder.toString());
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    } finally {
      IOUtils.closeQuietly(out);
    }
  }

  private TerracottaConfigBuilder createConfig() {
    Class testAppClass = getApplicationClass();

    TerracottaConfigBuilder cb = new TerracottaConfigBuilder();
    cb.getServers().getL2s()[0].setDSOPort(port);
    cb.getServers().getL2s()[0].setJMXPort(adminPort);

    // locks
    LockConfigBuilder[] locks = new LockConfigBuilder[] {
        new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK, L1ReconnectTestApp.L1Client.class,
                                  LockConfigBuilder.LEVEL_WRITE),
        new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK, testAppClass, LockConfigBuilder.LEVEL_WRITE) };

    cb.getApplication().getDSO().setLocks(locks);

    // include classes
    InstrumentedClassConfigBuilder[] instrClasses = new InstrumentedClassConfigBuilder[] {
        new InstrumentedClassConfigBuilderImpl(AbstractTransparentApp.class),
        new InstrumentedClassConfigBuilderImpl(L1ReconnectTestApp.L1Client.class),
        new InstrumentedClassConfigBuilderImpl(testAppClass) };

    cb.getApplication().getDSO().setInstrumentedClasses(instrClasses);

    // roots
    RootConfigBuilder[] roots = new RootConfigBuilder[] { new RootConfigBuilderImpl(testAppClass, "sum", "sum"),
        new RootConfigBuilderImpl(L1ReconnectTestApp.L1Client.class, "sum", "sum") };
    cb.getApplication().getDSO().setRoots(roots);

    return cb;
  }
}
