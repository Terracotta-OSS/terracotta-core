/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.restart.system;

import org.apache.commons.io.FileUtils;

import com.tc.config.schema.test.L2ConfigBuilder;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.util.PortChooser;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.TransparentAppConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

public class ClientAbscondAfterServerCrashTest extends TransparentTestBase {

  private int  dsoPort;
  private int  adminPort;
  private File configDir;
  private File configFile;
  private File client1Workspace;
  private File client2Workspace;

  @Override
  protected Class getApplicationClass() {
    return ClientAbscondAfterServerCrashTestApp.class;
  }

  @Override
  protected void setUp() throws Exception {
    // choose the ports
    PortChooser portChooser = new PortChooser();
    dsoPort = portChooser.chooseRandomPort();
    adminPort = portChooser.chooseRandomPort();
    int groupPort = portChooser.chooseRandomPort();
    configDir = getTempDirectory();
    configFile = new File(configDir, "abscondingClient-tc-config.xml");

    // write the config file
    try {
      TerracottaConfigBuilder builder = TerracottaConfigBuilder.newMinimalInstance();
      builder.getSystem().setConfigurationModel("production");

      L2ConfigBuilder l2 = builder.getServers().getL2s()[0];
      l2.setDSOPort(dsoPort);
      l2.setJMXPort(adminPort);
      l2.setL2GroupPort(groupPort);
      l2.setData(configDir + File.separator + "data");
      l2.setLogs(configDir + File.separator + "logs");
      l2.setReconnectWindowForPrevConnectedClients(15);
      l2.setPersistenceMode(L2ConfigBuilder.PERSISTENCE_MODE_PERMANENT_STORE);
      String configAsString = builder.toString();
      FileOutputStream fileOutputStream = new FileOutputStream(configFile);
      PrintWriter out = new PrintWriter((fileOutputStream));
      out.println(configAsString);
      out.flush();
      out.close();
    } catch (Exception e) {
      throw new AssertionError(e);
    }

    // External L2. We need to crash the server and reboot from app.
    setUpControlledServer(configFactory(), configHelper(), dsoPort, adminPort, groupPort, configFile.getAbsolutePath());
    doSetUp(this);
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(1).setIntensity(1);
    t.initializeTestRunner();
    client1Workspace = getTempFile("client-1");
    client2Workspace = getTempFile("client-2");
    FileUtils.forceMkdir(client1Workspace);
    FileUtils.forceMkdir(client2Workspace);
    TransparentAppConfig txAppConfig = t.getTransparentAppConfig();

    txAppConfig.setAttribute(ClientAbscondAfterServerCrashTestApp.HOST_NAME, "localhost");
    txAppConfig.setAttribute(ClientAbscondAfterServerCrashTestApp.DSO_PORT, String.valueOf(dsoPort));
    txAppConfig.setAttribute(ClientAbscondAfterServerCrashTestApp.ADMIN_PORT, String.valueOf(adminPort));
    txAppConfig.setAttribute(ClientAbscondAfterServerCrashTestApp.CONFIG_FILE, configFile.getAbsolutePath());
    txAppConfig.setAttribute(ClientAbscondAfterServerCrashTestApp.CLIENT1_SPACE, client1Workspace.getAbsolutePath());
    txAppConfig.setAttribute(ClientAbscondAfterServerCrashTestApp.CLIENT2_SPACE, client2Workspace.getAbsolutePath());
  }
}
