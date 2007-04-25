/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.tcconfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This class writes a {@link StandardTerracottaAppServerConfig} to the filesystem.
 */
public final class TerracottaServerConfigGenerator {

  private static final String                     NAME = "tc-config.xml";
  private final File                              configFile;
  private final StandardTerracottaAppServerConfig config;

  public TerracottaServerConfigGenerator(File location, StandardTerracottaAppServerConfig config)
      throws FileNotFoundException, IOException {
    this.configFile = new File(location + File.separator + NAME);
    this.config = config;

    config.build();
    
    FileWriter out = new FileWriter(configFile);
    out.write(config.toString());
    out.flush();
    out.close();
  }

  public StandardTerracottaAppServerConfig getConfig() {
    return config;
  }

  public String configPath() {
    return configFile.getPath();
  }

  public File configFile() {
    return configFile;
  }
}
