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
import java.io.UncheckedIOException;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.Properties;
import static org.terracotta.testing.config.ConfigConstants.DEFAULT_CLIENT_RECONNECT_WINDOW;
import static org.terracotta.testing.config.ConfigConstants.DEFAULT_VOTER_COUNT;
import static org.terracotta.testing.demos.TestHelpers.isWindows;

public class DefaultStartupCommandBuilder implements StartupCommandBuilder, Cloneable {
  private StripeConfiguration stripeConfig;
  private Path stripeWorkingDir;
  private Path serverWorkingDir;
  private String logConfigExt;
  private String serverName;
  private String stripeName;
  private boolean consistentStartup;
  private String[] builtCommand;
  private String namespaceFragment = "";
  private String serviceFragment = "";
  private int clientReconnectWindowTime = DEFAULT_CLIENT_RECONNECT_WINDOW;
  private int failoverPriorityVoterCount = DEFAULT_VOTER_COUNT;
  
  private final Properties tcProperties = new Properties();
  private Properties systemProperties = new Properties();

  public DefaultStartupCommandBuilder() {
  }
  
  public DefaultStartupCommandBuilder copy() {
    try {
      return (DefaultStartupCommandBuilder)this.clone();
    } catch (CloneNotSupportedException c) {
      throw new RuntimeException(c);
    }
  }
  
  public StartupCommandBuilder withFailoverPriorityVoterCount(final int failoverPriorityVoterCount) {
    this.failoverPriorityVoterCount = failoverPriorityVoterCount;
    return this;
  }
  
  public StartupCommandBuilder withNamespaceFragment(final String namespaceFragment) {
    if (namespaceFragment == null) {
      throw new NullPointerException("Namespace fragment must be non-null");
    }
    this.namespaceFragment = namespaceFragment;
    return this;
  }
  
  public StartupCommandBuilder withServiceFragment(final String serviceFragment) {
    if (serviceFragment == null) {
      throw new NullPointerException("Service fragment must be non-null");
    }
    this.serviceFragment = serviceFragment;
    return this;
  }
  
  public StartupCommandBuilder withConsistentStartup(boolean consistent) {
    this.consistentStartup = consistent;
    return this;
  }
  
  public StartupCommandBuilder withClientReconnectWindowTime(final int clientReconnectWindowTime) {
    this.clientReconnectWindowTime = clientReconnectWindowTime;
    return this;
  }
  
  public StartupCommandBuilder withTcProperties(Properties tcProperties) {
    this.tcProperties.putAll(tcProperties);
    return this;
  }
  
  public StartupCommandBuilder withTcProperty(String key, String value) {
    this.tcProperties.putAll(tcProperties);
    return this;
  }
  
  public StartupCommandBuilder stripeConfiguration(StripeConfiguration config) {
    this.stripeConfig = config;
    return this;
  }
  
  public StartupCommandBuilder stripeWorkingDir(Path stripeWorkingDir) {
    this.stripeWorkingDir = stripeWorkingDir;
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
    this.stripeName = stripeName;
    return this;
  }

  public StartupCommandBuilder consistentStartup(boolean consistentStartup) {
    this.consistentStartup = consistentStartup;
    return this;
  }

  @Override
  public StartupCommandBuilder logConfigExtension(String logConfigExt) {
    this.logConfigExt = logConfigExt;
    return this;
  }

  private Path installServer() throws IOException {
    // Create a copy of the server for this installation.
    Files.createDirectories(stripeWorkingDir);
    Files.createDirectories(serverWorkingDir);
    Path tcConfig = createTcConfig(stripeWorkingDir);
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

  /**
   * Returns a normalized absolute path to the shell/bat script, and quotes the windows path to avoid issues with special path chars.
   * @param scriptPath path to the script from the base kit
   * @return string representation of processed path
   */
  protected String getAbsolutePath(Path scriptPath) {
    Path basePath =  getServerWorkingDir().resolve(scriptPath).toAbsolutePath().normalize();
    return isWindows() ? "\"" + basePath + ".bat\"" : basePath + ".sh";
  }

  @Override
  public String[] build() {
    if (builtCommand == null) {
      try {
        Path config = installServer();
        builtCommand = new String[]{"-f", config.toString(), "-n", serverName, "JAVA_OPTS=-Dlogback.configurationFile=logback-test.xml"};
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return builtCommand;
  }
  
  private Path createTcConfig(Path stripeInstallationDir) {
    Path config = stripeInstallationDir.resolve("tc-config.xml");
    if (!Files.exists(config)) {
      TcConfigBuilder configBuilder = new TcConfigBuilder(stripeInstallationDir, stripeConfig.getServerNames(), stripeConfig.getServerPorts(), stripeConfig.getServerGroupPorts(), tcProperties,
          namespaceFragment, serviceFragment, clientReconnectWindowTime, failoverPriorityVoterCount);

      String tcConfig = configBuilder.build();
      try {
        Path tcConfigPath = Files.createFile(config);
        Files.write(tcConfigPath, tcConfig.getBytes(UTF_8));
        return tcConfigPath;
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    return config;
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
