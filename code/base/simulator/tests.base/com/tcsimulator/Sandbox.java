/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator;

import java.io.File;

public final class Sandbox {
  public static final int     TEST_SERVER                 = 0;
  public static final int     CONTROL_SERVER              = 1;
  private static final String TEST_SERVER_SANDBOX_NAME    = "server";
  private static final String CONTROL_SERVER_SANDBOX_NAME = "control";
  private static final String TEST_CONFIG_FILENAME        = "tc-config.xml";
  private static final String CONTROL_CONFIG_FILENAME     = "control-config.xml";
  private static final String TC_DISTRIBUTION             = "terracotta";

  final File                  testHome;
  final File                  serverHome;
  final File                  configFile;
  final int                   serverType;

  public Sandbox(File testHome, int serverType) {
    this.testHome = testHome;
    this.testHome.mkdir();
    this.serverType = serverType;
    if (this.serverType == TEST_SERVER) {
      this.serverHome = new File(this.testHome, TEST_SERVER_SANDBOX_NAME);
      this.configFile = new File(this.testHome, TEST_CONFIG_FILENAME);
    } else if (serverType == CONTROL_SERVER) {
      this.serverHome = new File(this.testHome, CONTROL_SERVER_SANDBOX_NAME);
      this.configFile = new File(this.testHome, CONTROL_CONFIG_FILENAME);
    } else {
      throw new AssertionError("Can't resolve server type.  Must be either test- or control-server.");
    }
    this.serverHome.mkdir();
  }

  public File getConfigFile() {
    return this.configFile;
  }

  public File getServerHome() {
    return this.serverHome;
  }

  public File getTestHome() {
    return this.testHome;
  }

  public String getDistributionName() {
    return TC_DISTRIBUTION;
  }
}