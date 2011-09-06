/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.CopyUtils;

import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tctest.runner.TransparentAppConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class ServerCrashingTestBase extends TransparentTestBase {

  private final int  nodeCount;
  private int        port;
  private int        adminPort;
  private int        groupPort;
  private File       configFile;
  private final List jvmArgs;

  protected ServerCrashingTestBase(int nodeCount, String jvmArgsArray[]) {
    this.nodeCount = nodeCount;
    this.jvmArgs = new ArrayList();
    for (int i = 0; jvmArgsArray != null && i < jvmArgsArray.length; i++) {
      jvmArgs.add(jvmArgsArray[i]);
    }
  }

  protected ServerCrashingTestBase(int nodeCount) {
    this(nodeCount, null);
  }

  @Override
  public void setUp() throws Exception {

    // for some test cases to enable l1reconnect
    if (enableL1Reconnect()) {
      setJvmArgsL1Reconnect((ArrayList) jvmArgs);
    }

    if (enableL2Reconnect()) {
      setJvmArgsL2Reconnect((ArrayList) jvmArgs);
    }

    // XXX: ERR! HACK! Will collide eventually
    PortChooser pc = new PortChooser();
    port = pc.chooseRandomPort();
    adminPort = pc.chooseRandomPort();
    groupPort = pc.chooseRandomPort();
    configFile = getTempFile("config-file.xml");
    writeConfigFile();

    setupConfigLogDataStatisticsPaths(configFactory());

    setUpControlledServer(configFactory(), configHelper(), port, adminPort, groupPort, configFile.getAbsolutePath(),
                          jvmArgs);

    getTransparentAppConfig().setClientCount(nodeCount);
    initializeTestRunner();
    TransparentAppConfig cfg = getTransparentAppConfig();
    cfg.setAttribute(ServerCrashingAppBase.CONFIG_FILE, configFile.getAbsolutePath());
    cfg.setAttribute(ServerCrashingAppBase.PORT_NUMBER, String.valueOf(port));
    cfg.setAttribute(ServerCrashingAppBase.HOST_NAME, "localhost");
    cfg.setAttribute(ServerCrashingAppBase.ADMIN_PORT, String.valueOf(adminPort));

    doSetUp(this);
  }

  private synchronized void writeConfigFile() {
    try {
      TerracottaConfigBuilder cb = new TerracottaConfigBuilder();

      cb.getServers().getL2s()[0].setDSOPort(port);
      cb.getServers().getL2s()[0].setJMXPort(adminPort);
      cb.getServers().getL2s()[0].setL2GroupPort(groupPort);

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

  @Override
  public int getAdminPort() {
    return adminPort;
  }

  public File getConfigFile() {
    return configFile;
  }
}
