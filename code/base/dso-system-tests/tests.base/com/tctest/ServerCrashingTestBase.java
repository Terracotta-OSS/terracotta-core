/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.CopyUtils;

import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tctest.runner.TransparentAppConfig;

import java.io.File;
import java.io.FileOutputStream;

public abstract class ServerCrashingTestBase extends TransparentTestBase {

  private final int nodeCount;
  private int       port;
  private int       adminPort;
  private File      configFile;

  protected ServerCrashingTestBase(int nodeCount) {
    this.nodeCount = nodeCount;
  }

  public void setUp() throws Exception {
    // XXX: ERR! HACK! Will collide eventually
    PortChooser pc = new PortChooser();
    port = pc.chooseRandomPort();
    adminPort = pc.chooseRandomPort();
    configFile = getTempFile("config-file.xml");
    writeConfigFile();

    TestTVSConfigurationSetupManagerFactory factory = new TestTVSConfigurationSetupManagerFactory(
                                                                                                  TestTVSConfigurationSetupManagerFactory.MODE_DISTRIBUTED_CONFIG,
                                                                                                  null,
                                                                                                  new FatalIllegalConfigurationChangeHandler());

    factory.addServerToL1Config(null, port, adminPort);
    factory.addServerToL2Config(null, port, adminPort);
    L1TVSConfigurationSetupManager manager = factory.createL1TVSConfigurationSetupManager();
    setUpControlledServer(factory, new StandardDSOClientConfigHelper(manager), port, adminPort, configFile
        .getAbsolutePath());

    getTransparentAppConfig().setClientCount(nodeCount);
    initializeTestRunner();
    TransparentAppConfig cfg = getTransparentAppConfig();
    cfg.setAttribute(ServerCrashingAppBase.CONFIG_FILE, configFile.getAbsolutePath());
    cfg.setAttribute(ServerCrashingAppBase.PORT_NUMBER, String.valueOf(port));
    cfg.setAttribute(ServerCrashingAppBase.HOST_NAME, "localhost");

    doSetUp(this);
  }

  private synchronized void writeConfigFile() {
    try {
      TerracottaConfigBuilder cb = new TerracottaConfigBuilder();

      cb.getServers().getL2s()[0].setDSOPort(port);
      cb.getServers().getL2s()[0].setJMXPort(adminPort);

      createConfig(cb);

      FileOutputStream out = new FileOutputStream(configFile);
      CopyUtils.copy(cb.toString(), out);
      out.close();
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    }
  }

  protected abstract void createConfig(TerracottaConfigBuilder cb);

  public int getPort() {
    return port;
  }

  public int getAdminPort() {
    return adminPort;
  }

  public File getConfigFile() {
    return configFile;
  }
}
