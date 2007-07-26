/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.FileUtils;

import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.io.File;

public abstract class ServerCrashingAppBase extends AbstractErrorCatchingTransparentApp {

  public static final String CONFIG_FILE = "config-file";
  public static final String PORT_NUMBER = "port-number";
  public static final String HOST_NAME   = "host-name";

  private ApplicationConfig  config;

  public ServerCrashingAppBase(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
    this.config = config;
  }

  // used by external clients
  public ServerCrashingAppBase() {
    super();
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

  public String getConfigFilePath() {
    return config.getAttribute(CONFIG_FILE);
  }

  public String getConfigFileDirectoryPath() {
    String configFile = getConfigFilePath();
    int i = configFile.lastIndexOf(File.separatorChar);
    return configFile.substring(0, i);
  }

  protected ExtraL1ProcessControl spawnNewClient(String clientId, Class clientClass) throws Exception {
    final String hostName = getHostName();
    final int port = getPort();
    final File configFile = new File(getConfigFilePath());
    File workingDir = new File(configFile.getParentFile(), "client-" + clientId);
    FileUtils.forceMkdir(workingDir);

    ExtraL1ProcessControl client = new ExtraL1ProcessControl(hostName, port, clientClass, configFile.getAbsolutePath(),
                                                             new String[0], workingDir);
    client.start();
    client.mergeSTDERR();
    client.mergeSTDOUT();
    client.waitFor();
    System.err.println("\n### Started New Client");
    return client;
  }

}
