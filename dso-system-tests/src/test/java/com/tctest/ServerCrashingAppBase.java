/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.FileUtils;

import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ServerCrashingAppBase extends AbstractErrorCatchingTransparentApp {

  public static final String      CONFIG_FILE = "config-file";
  public static final String      PORT_NUMBER = "port-number";
  public static final String      HOST_NAME   = "host-name";
  public static final String      ADMIN_PORT  = "admin-port";

  private final ApplicationConfig config;

  public ServerCrashingAppBase(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
    this.config = config;
  }

  public ApplicationConfig getConfig() {
    return config;
  }

  public String getHostName() {
    return config.getAttribute(HOST_NAME);
  }

  public int getPort() {
    return Integer.parseInt(config.getAttribute(PORT_NUMBER));
  }

  public int getAdminPort() {
    return Integer.parseInt(config.getAttribute(ADMIN_PORT));
  }

  public String getConfigFilePath() {
    return config.getAttribute(CONFIG_FILE);
  }

  public String getConfigFileDirectoryPath() {
    String configFile = getConfigFilePath();
    int i = configFile.lastIndexOf(File.separatorChar);
    return configFile.substring(0, i);
  }

  protected ExtraL1ProcessControl spawnNewClientAndWaitForCompletion(String clientID, Class clientClass) throws Exception {
    final String hostName = getHostName();
    final int port = getPort();
    final File configFile = new File(getConfigFilePath());
    File workingDir = new File(configFile.getParentFile(), "client-" + clientID);
    FileUtils.forceMkdir(workingDir);

    List jvmArgs = new ArrayList();
    addTestTcPropertiesFile(jvmArgs);
    ExtraL1ProcessControl client = new ExtraL1ProcessControl(hostName, port, clientClass, configFile.getAbsolutePath(),
                                                             Collections.EMPTY_LIST, workingDir, jvmArgs);
    client.start();
    client.mergeSTDERR();
    client.mergeSTDOUT();
    System.err.println("\n### Started New Client");
    client.waitFor();
    System.err.println("\n### Wait completed for New Client");
    return client;
  }

  protected final void addTestTcPropertiesFile(List jvmArgs) {
    URL url = getClass().getResource("/com/tc/properties/tests.properties");
    if (url == null) { return; }
    String pathToTestTcProperties = url.getPath();
    if (pathToTestTcProperties == null || pathToTestTcProperties.equals("")) { return; }
    // System.err.println("\n##### -Dcom.tc.properties=" + pathToTestTcProperties);
    jvmArgs.add("-Dcom.tc.properties=" + pathToTestTcProperties);
  }

}
