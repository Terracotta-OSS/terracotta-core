/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.dsoserver;

import com.tc.test.server.tcconfig.TerracottaServerConfigGenerator;

import java.io.File;

/**
 * This object is created by the client and passed to the DSO server at startup.
 */
public final class StandardDsoServerParameters implements DsoServerParameters {

  private final File config;
  private final int  dsoPort;
  private final int  jmxPort;
  private final File outputFile;
  private final File workingDir;

  public StandardDsoServerParameters(TerracottaServerConfigGenerator config, File workingDir, File outputFile,
                                     int dsoPort, int jmxPort) {
    this.workingDir = workingDir;
    this.outputFile = outputFile;
    this.dsoPort = dsoPort;
    this.jmxPort = jmxPort;
    this.config = config.configFile();
  }

  public File configFile() {
    return config;
  }

  public int dsoPort() {
    return dsoPort;
  }

  public int jmxPort() {
    return jmxPort;
  }

  public String jvmArgs() {
    return "";
  }

  public String classpath() {
    return "";
  }

  public File outputFile() {
    return outputFile;
  }

  public File workingDir() {
    return workingDir;
  }
}
