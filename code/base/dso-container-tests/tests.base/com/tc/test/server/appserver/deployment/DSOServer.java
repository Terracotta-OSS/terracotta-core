/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.deployment;

import com.tc.config.schema.test.L2ConfigBuilder;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.objectserver.control.ExtraProcessServerControl;
import com.tc.test.server.util.TcConfigBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class DSOServer extends AbstractStoppable {

  private static final String       SERVER_TEST_CONFIG = "server-config.xml";

  private ExtraProcessServerControl serverProc         = null;
  private final boolean             withPersistentStore;

  private int                       serverPort         = 9510;
  private int                       adminPort          = 9520;

  private final File                workingDir;
  private TcConfigBuilder           configBuilder;

  public DSOServer(boolean withPersistentStore, File workingDir) {
    this.withPersistentStore = withPersistentStore;
    this.workingDir = workingDir;
  }

  public DSOServer(boolean withPersistentStore, File workingDir, TcConfigBuilder configBuilder) {
    this.withPersistentStore = withPersistentStore;
    this.workingDir = workingDir;
    this.configBuilder = configBuilder;
    this.serverPort = configBuilder.getDsoPort();
    this.adminPort = configBuilder.getJmxPort();
  }

  protected void doStart() throws Exception {
    File configFile = writeConfig();
    serverProc = new ExtraProcessServerControl("localhost", serverPort, adminPort, configFile.getAbsolutePath(), false);
    serverProc.writeOutputTo(new File(workingDir, "dso-server.log"));
    serverProc.start();
  }

  protected void doStop() throws Exception {
    logger.debug("Stopping...");
    serverProc.shutdown();
    logger.debug("...stopped");
  }

  private File writeConfig() throws IOException {
    File configFile = new File(workingDir, SERVER_TEST_CONFIG);
    if (configBuilder != null) {
      configBuilder.saveToFile(configFile);
    } else {
      TerracottaConfigBuilder builder = TerracottaConfigBuilder.newMinimalInstance();
      builder.getSystem().setConfigurationModel("development");

      L2ConfigBuilder l2 = builder.getServers().getL2s()[0];
      l2.setDSOPort(serverPort);
      l2.setJMXPort(adminPort);
      l2.setData(workingDir + File.separator + "data");
      l2.setLogs(workingDir + File.separator + "logs");
      if (withPersistentStore) {
        l2.setPersistenceMode("permanent-store"); // XXX make this one configurable
      }

      String configAsString = builder.toString();

      FileOutputStream fileOutputStream = new FileOutputStream(configFile);
      PrintWriter out = new PrintWriter((fileOutputStream));
      out.println(configAsString);
      out.flush();
      out.close();
    }
    return configFile;
  }

  public String toString() {
    return "DSO server; serverport:" + serverPort + "; adminPort:" + adminPort;
  }

  public int getServerPort() {
    return serverPort;
  }

  public int getAdminPort() {
    return adminPort;
  }
}
