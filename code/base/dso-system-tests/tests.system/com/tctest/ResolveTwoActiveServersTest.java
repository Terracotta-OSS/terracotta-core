/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.schema.builder.LockConfigBuilder;
import com.tc.config.schema.builder.RootConfigBuilder;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.config.schema.test.ApplicationConfigBuilder;
import com.tc.config.schema.test.DSOApplicationConfigBuilderImpl;
import com.tc.config.schema.test.HaConfigBuilder;
import com.tc.config.schema.test.InstrumentedClassConfigBuilderImpl;
import com.tc.config.schema.test.L2ConfigBuilder;
import com.tc.config.schema.test.L2SConfigBuilder;
import com.tc.config.schema.test.LockConfigBuilderImpl;
import com.tc.config.schema.test.RootConfigBuilderImpl;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.util.Assert;
import com.tc.util.PortChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

public class ResolveTwoActiveServersTest extends TransparentTestBase {
  private static final int NODES           = 2;

  // This work only for 2 servers right now
  private static final int SERVERS         = 2;
  private final int[]      dsoPorts        = new int[SERVERS];
  private final int[]      jmxPorts        = new int[SERVERS];
  private final int[]      l2GroupPorts    = new int[SERVERS];
  private final int[]      proxyPorts      = new int[SERVERS];
  private final String[]   serverNames     = new String[SERVERS];
  private String           persistenceMode = L2ConfigBuilder.PERSISTENCE_MODE_TEMPORARY_SWAP_ONLY;
  private File[]           configFiles;

  public ResolveTwoActiveServersTest() {
    // TODO: fix and enable
    // With tribes, the proxies get by-passed. When we come up with a work around for this, modify this test accordingly
    // and enable it.

    this.disableAllUntil("3001-01-01");
  }

  protected Class getApplicationClass() {
    return ResolveTwoActiveServersTestApp.class;
  }

  public void setUp() throws Exception {
    assertEquals(SERVERS, 2);

    setPorts();

    // to be used by external process servers
    configFiles = new File[SERVERS];
    for (int i = 0; i < SERVERS; i++) {
      configFiles[i] = getTempFile("config-file-" + i + ".xml");
      writeConfigFile(configFiles[i], i);
    }

    TestTVSConfigurationSetupManagerFactory factory = new TestTVSConfigurationSetupManagerFactory(
                                                                                                  TestTVSConfigurationSetupManagerFactory.MODE_DISTRIBUTED_CONFIG,
                                                                                                  null,
                                                                                                  new FatalIllegalConfigurationChangeHandler());

    // to be used by in-process clients
    setConfigFactory(factory);

    L1TVSConfigurationSetupManager manager = factory.createL1TVSConfigurationSetupManager();
    setUpForMultipleExternalProcesses(factory, new StandardDSOClientConfigHelperImpl(manager), dsoPorts, jmxPorts,
                                      l2GroupPorts, proxyPorts, serverNames, configFiles);

    getTransparentAppConfig().setClientCount(NODES);
    initializeTestRunner();

    // TransparentAppConfig cfg = getTransparentAppConfig();
    // cfg.setAttribute(ServerCrashingAppBase.CONFIG_FILE, configFile.getAbsolutePath());
    // cfg.setAttribute(ServerCrashingAppBase.PORT_NUMBER, String.valueOf(port));
    // cfg.setAttribute(ServerCrashingAppBase.HOST_NAME, "localhost");
  }

  private void setConfigFactory(TestTVSConfigurationSetupManagerFactory factory) {
    for (int i = 0; i < SERVERS; i++) {
      factory.addServerToL1Config(null, dsoPorts[i], jmxPorts[i]);
    }
  }

  private void writeConfigFile(File configFile, int index) {
    try {
      int theOther = (index + 1) % 2;

      L2ConfigBuilder[] l2s = new L2ConfigBuilder[SERVERS];
      for (int i = 0; i < SERVERS; i++) {
        L2ConfigBuilder l2 = new L2ConfigBuilder();
        l2.setData(getTempFile("data-" + i).getAbsolutePath());
        l2.setLogs(getTempFile("logs-" + i).getAbsolutePath());
        serverNames[i] = "server-" + i;
        l2.setName(serverNames[i]);
        l2.setDSOPort(dsoPorts[i]);
        l2.setJMXPort(jmxPorts[i]);
        l2.setPersistenceMode(persistenceMode);
        l2s[i] = l2;
      }
      l2s[index].setL2GroupPort(l2GroupPorts[index]);
      l2s[theOther].setL2GroupPort(proxyPorts[theOther]);

      HaConfigBuilder ha = new HaConfigBuilder();
      ha.setMode(HaConfigBuilder.HA_MODE_NETWORKED_ACTIVE_PASSIVE);

      L2SConfigBuilder l2sConfigbuilder = new L2SConfigBuilder();
      l2sConfigbuilder.setL2s(l2s);
      l2sConfigbuilder.setHa(ha);

      DSOApplicationConfigBuilderImpl appConfigBuilder = new DSOApplicationConfigBuilderImpl();

      InstrumentedClassConfigBuilder[] instrClasses = new InstrumentedClassConfigBuilder[] { new InstrumentedClassConfigBuilderImpl(
                                                                                                                                    getApplicationClass()) };
      appConfigBuilder.setInstrumentedClasses(instrClasses);

      LockConfigBuilder[] locks = new LockConfigBuilder[] { new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK,
                                                                                      getApplicationClass(),
                                                                                      LockConfigBuilder.LEVEL_WRITE) };
      appConfigBuilder.setLocks(locks);

      RootConfigBuilder[] roots = new RootConfigBuilder[] {
          new RootConfigBuilderImpl(getApplicationClass(), "barrier"),
          new RootConfigBuilderImpl(getApplicationClass(), "myArrayTestRoot") };
      appConfigBuilder.setRoots(roots);

      ApplicationConfigBuilder app = ApplicationConfigBuilder.newMinimalInstance();
      app.setDSO(appConfigBuilder);

      TerracottaConfigBuilder cb = new TerracottaConfigBuilder();
      cb.setServers(l2sConfigbuilder);
      cb.setApplication(app);

      FileOutputStream fileOutputStream = new FileOutputStream(configFile);
      PrintWriter out = new PrintWriter((fileOutputStream));
      out.println(cb.toString());
      out.flush();
      out.close();
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    }
  }

  private void setPorts() {
    PortChooser pc = new PortChooser();

    for (int i = 0; i < SERVERS; i++) {
      int dsoPort = pc.chooseRandomPort();
      while (!isUnusedPort(dsoPort)) {
        dsoPort = pc.chooseRandomPort();
      }
      dsoPorts[i] = dsoPort;

      int jmxPort = pc.chooseRandomPort();
      while (!isUnusedPort(jmxPort)) {
        jmxPort = pc.chooseRandomPort();
      }
      jmxPorts[i] = jmxPort;

      int l2GroupPort = pc.chooseRandomPort();
      while (!isUnusedPort(l2GroupPort)) {
        l2GroupPort = pc.chooseRandomPort();
      }
      l2GroupPorts[i] = l2GroupPort;

      int proxyPort = pc.chooseRandomPort();
      while (!isUnusedPort(proxyPort)) {
        proxyPort = pc.chooseRandomPort();
      }
      proxyPorts[i] = proxyPort;
    }
  }

  // private void setPorts() {
  // int base = 9500;
  //
  // for (int i = 0; i < SERVERS; i++) {
  // dsoPorts[i] = base - (i * 1000) + 10;
  // jmxPorts[i] = base - (i * 1000) + 20;
  // l2GroupPorts[i] = base - (i * 1000) + 30;
  // proxyPorts[i] = base - (i * 1000) + 40;
  // }
  // }

  private boolean isUnusedPort(int port) {
    for (int i = 0; i < dsoPorts.length; i++) {
      if (dsoPorts[i] == port) { return false; }
    }
    for (int i = 0; i < jmxPorts.length; i++) {
      if (jmxPorts[i] == port) { return false; }
    }
    for (int i = 0; i < l2GroupPorts.length; i++) {
      if (l2GroupPorts[i] == port) { return false; }
    }
    for (int i = 0; i < proxyPorts.length; i++) {
      if (proxyPorts[i] == port) { return false; }
    }
    return true;
  }
}
