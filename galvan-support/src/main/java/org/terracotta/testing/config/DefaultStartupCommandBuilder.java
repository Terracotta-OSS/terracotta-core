/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.terracotta.testing.api.ConfigBuilder;

public class DefaultStartupCommandBuilder implements StartupCommandBuilder, Cloneable {
  private StripeConfiguration stripeConfig;
  private Path stripeWorkingDir;
  private Path serverWorkingDir;
  private String logConfigExt;
  private String serverName;
  private String stripeName;
  private String[] builtCommand;

  private final ConfigBuilder builder;

  public DefaultStartupCommandBuilder(ConfigBuilder builder) {
      this.builder = builder;
  }

  public DefaultStartupCommandBuilder copy() {
    try {
      return (DefaultStartupCommandBuilder)this.clone();
    } catch (CloneNotSupportedException c) {
      throw new RuntimeException(c);
    }
  }


  @Override
  public StartupCommandBuilder stripeConfiguration(StripeConfiguration config) {
    this.stripeConfig = config;
    return this;
  }

  @Override
  public StartupCommandBuilder stripeWorkingDir(Path stripeWorkingDir) {
    this.stripeWorkingDir = stripeWorkingDir;
    return this;
  }
  @Override
  public StartupCommandBuilder serverWorkingDir(Path serverWorkingDir) {
    this.serverWorkingDir = serverWorkingDir;
    return this;
  }

  @Override
  public StartupCommandBuilder serverName(String serverName) {
    this.serverName = serverName;
    return this;
  }

  @Override
  public StartupCommandBuilder stripeName(String stripeName) {
    this.stripeName = stripeName;
    return this;
  }

  @Override
  public StartupCommandBuilder logConfigExtension(String logConfigExt) {
    this.logConfigExt = logConfigExt;
    return this;
  }

  protected Path installServer() throws IOException {
    // Create a copy of the server for this installation.
    Files.createDirectories(stripeWorkingDir);
    Files.createDirectories(serverWorkingDir);
    // build the tc-config file
    builder.withStripeConfiguration(stripeConfig);
    Path tcConfig = builder.createConfig(stripeWorkingDir);
    //Copy a custom logback configuration
    Files.copy(this.getClass().getResourceAsStream("/tc-logback.xml"), serverWorkingDir.resolve("logback-test.xml"), REPLACE_EXISTING);
    Properties props = new Properties();
    props.setProperty("serverWorkingDir", serverWorkingDir.toAbsolutePath().toString());
    props.store(new FileWriter(serverWorkingDir.resolve("logbackVars.properties").toFile()), "logging variables");
    if (logConfigExt != null) {
      InputStream logExt = this.getClass().getResourceAsStream("/" + logConfigExt);
      if (logExt != null) {
        Files.copy(logExt, serverWorkingDir.resolve("logback-ext-test.xml"), REPLACE_EXISTING);
      }
    }
    return tcConfig;
  }

  @Override
  public String[] build() {
    if (builtCommand == null) {
      try {
        Path config = installServer();
        List<String> args = new ArrayList<>();

        args.add("-f");
        args.add(config.toString());
        args.add("-n");
        args.add(serverName);
        builtCommand = args.toArray(String[]::new);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return builtCommand;
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

  public String getStripeName() {
    return stripeName;
  }

  public boolean isConsistentStartup() {
    return stripeConfig.isConsistent();
  }
}
