/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.filters.StringInputStream;

import com.tc.config.schema.test.L2ConfigBuilder;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tctest.runner.TransparentAppConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class ServerDBBackupTest extends TransparentTestBase {
  private static final int NODE_COUNT        = 2;
  private String           serverDbBackupDir = null;
  private int              port;
  private int              jmxPort;
  private File             configFile;

  @Override
  protected Class getApplicationClass() {
    return ServerDBBackupTestApp.class;
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
    TransparentAppConfig cfg = t.getTransparentAppConfig();
    cfg.setAttribute(ServerDBBackupTestApp.SERVER_DB_BACKUP, serverDbBackupDir);
    cfg.setAttribute(ServerDBBackupTestApp.JMX_PORT, String.valueOf(jmxPort));
  }

  public void setUp() throws Exception {
    PortChooser pc = new PortChooser();
    port = pc.chooseRandomPort();
    jmxPort = pc.chooseRandomPort();
    configFile = getTempFile("tc-config.xml");

    serverDbBackupDir = getTempDirectory().getAbsolutePath();

    writeConfigFile();

    configFactory().addServerToL1Config(null, port, jmxPort);

    ArrayList jvmArgs = new ArrayList();
    setJvmArgsL1Reconnect(jvmArgs);
    setUpControlledServer(configFactory(), configHelper(), port, jmxPort, configFile.getAbsolutePath(), jvmArgs);
    doSetUp(this);
  }

  private synchronized void writeConfigFile() {
    try {
      TerracottaConfigBuilder builder = createConfig(port, jmxPort, serverDbBackupDir);
      FileOutputStream out = new FileOutputStream(configFile);
      IOUtils.copy(new StringInputStream(builder.toString()), out);
      out.close();
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    }
  }

  public static TerracottaConfigBuilder createConfig(int port, int adminPort, String serverDbBackup) {
    TerracottaConfigBuilder out = new TerracottaConfigBuilder();

    out.getServers().getL2s()[0].setDSOPort(port);
    out.getServers().getL2s()[0].setJMXPort(adminPort);
    out.getServers().getL2s()[0].setPersistenceMode(L2ConfigBuilder.PERSISTENCE_MODE_PERMANENT_STORE);
    out.getServers().getL2s()[0].setServerDbBackup(serverDbBackup);

    return out;
  }
}