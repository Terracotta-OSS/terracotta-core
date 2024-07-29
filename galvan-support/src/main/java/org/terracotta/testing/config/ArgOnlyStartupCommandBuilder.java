/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.terracotta.testing.config;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.Properties;
import static org.terracotta.testing.demos.TestHelpers.isWindows;

public class ArgOnlyStartupCommandBuilder implements StartupCommandBuilder {
  private Path tcConfig;
  private Path kitDir;
  private Path serverWorkingDir;
  private String logConfigExt;
  private String serverName;
  private boolean consistentStartup;
  private String[] builtCommand;

  public StartupCommandBuilder tcConfig(Path tcConfig) {
    this.tcConfig = tcConfig;
    return this;
  }

  public StartupCommandBuilder serverWorkingDir(Path serverWorkingDir) {
    this.serverWorkingDir = serverWorkingDir;
    return this;
  }

  public StartupCommandBuilder serverName(String serverName) {
    this.serverName = serverName;
    return this;
  }

  public StartupCommandBuilder stripeName(String stripeName) {
    return this;
  }

  public StartupCommandBuilder consistentStartup(boolean consistentStartup) {
    this.consistentStartup = consistentStartup;
    return this;
  }

  public StartupCommandBuilder kitDir(Path kitDir) {
    this.kitDir = kitDir;
    return this;
  }

  public StartupCommandBuilder logConfigExtension(String logConfigExt) {
    this.logConfigExt = logConfigExt;
    return this;
  }

  protected void installServer() throws IOException {
    // Create a copy of the server for this installation.
    Files.createDirectories(serverWorkingDir);

    //Copy a custom logback configuration
    Files.copy(this.getClass().getResourceAsStream("/tc-logback.xml"), serverWorkingDir.resolve("logback-test.xml"), REPLACE_EXISTING);
    Properties props = new Properties();
    props.setProperty("serverWorkingDir", serverWorkingDir.toAbsolutePath().toString());
    props.store(new FileWriter(serverWorkingDir.resolve("logbackVars.properties").toFile()), "logging variables");
    Files.copy(this.getClass().getResourceAsStream("/tc-logback.xml"), serverWorkingDir.resolve("logback-test.xml"), REPLACE_EXISTING);

    if (logConfigExt != null) {
      InputStream logExt = this.getClass().getResourceAsStream("/" + logConfigExt);
      if (logExt != null) {
        Files.copy(logExt, serverWorkingDir.resolve("logback-ext-test.xml"), REPLACE_EXISTING);
      }
    }
  }

  /**
   * Returns a normalized absolute path to the shell/bat script, and quotes the windows path to avoid issues with special path chars.
   * @param scriptPath path to the script from the base kit
   * @return string representation of processed path
   */
  protected String getAbsolutePath(Path scriptPath) {
    Path basePath =  getServerWorkingDir().resolve(getKitDir()).resolve(scriptPath).toAbsolutePath().normalize();
    return isWindows() ? "\"" + basePath + ".bat\"" : basePath + ".sh";
  }

  @Override
  public String[] build() {
    if (builtCommand == null) {
      try {
        installServer();
        builtCommand = new String[]{"-f", serverWorkingDir.resolve(tcConfig).toString(), "-n", serverName};
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return builtCommand;
  }

  public Path getTcConfig() {
    return tcConfig;
  }

  public Path getKitDir() {
    return kitDir;
  }

  public Path getServerWorkingDir() {
    return serverWorkingDir;
  }

  public String getLogConfigExt() {
    return logConfigExt;
  }

  public String getServerName() {
    return serverName;
  }

  public boolean isConsistentStartup() {
    return consistentStartup;
  }
}
