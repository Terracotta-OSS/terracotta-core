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
package org.terracotta.testing.rules;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import org.terracotta.testing.config.StartupCommandBuilder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;
import org.terracotta.testing.api.ConfigBuilder;

import static org.terracotta.testing.config.ConfigConstants.DEFAULT_SERVER_HEAP_MB;
import org.terracotta.testing.config.DefaultLegacyConfigBuilder;
import org.terracotta.testing.config.DefaultStartupCommandBuilder;
import org.terracotta.testing.master.ServerDeploymentBuilder;
import org.terracotta.testing.api.LegacyConfigBuilder;
import org.terracotta.testing.config.ConfigConstants;

public class BasicExternalClusterBuilder {
  private final int stripeSize;

  private Path clusterDirectory;
  private final Properties systemProperties = new Properties();
  private final Properties tcProperties = new Properties();
  private int reconnectWindow = ConfigConstants.DEFAULT_CLIENT_RECONNECT_WINDOW;
  private int voters = ConfigConstants.DEFAULT_VOTER_COUNT;
  private boolean consistent = false;

  private String logConfigExt = "logback-ext.xml";
  private int serverHeapSize = DEFAULT_SERVER_HEAP_MB;
  private int customHeapSize = DEFAULT_SERVER_HEAP_MB;
  private ConfigBuilder configBuilder;
  private ServerDeploymentBuilder serverBuilder = new ServerDeploymentBuilder();
  private Supplier<StartupCommandBuilder> startupBuilder;
  private OutputStream parentStream;



  private BasicExternalClusterBuilder(final int stripeSize) {
    this.stripeSize = stripeSize;
  }

  public static BasicExternalClusterBuilder newCluster() {
    return new BasicExternalClusterBuilder(1);
  }

  public static BasicExternalClusterBuilder newCluster(int stripeSize) {
    if (stripeSize < 1) {
      throw new IllegalArgumentException("Must be at least one server in the cluster");
    }
    return new BasicExternalClusterBuilder(stripeSize);
  }

  public BasicExternalClusterBuilder in(Path clusterDirectory) {
    if (clusterDirectory == null) {
      throw new NullPointerException("Cluster directory must be non-null");
    }
    this.clusterDirectory = clusterDirectory;
    return this;
  }

  public BasicExternalClusterBuilder server(Path server) {
    this.serverBuilder.installPath(server);
    return this;
  }

  public BasicExternalClusterBuilder configBuilder(ConfigBuilder config) {
    this.configBuilder = config;
    return this;
  }

  private LegacyConfigBuilder legacy() {
    if (this.configBuilder == null) {
      this.configBuilder = new DefaultLegacyConfigBuilder();
    }
    return (LegacyConfigBuilder)this.configBuilder;
  }

  public BasicExternalClusterBuilder withNamespaceFragment(final String namespaceFragment) {
    this.legacy().withNamespaceFragment(namespaceFragment);
    return this;
  }

  public BasicExternalClusterBuilder withServiceFragment(final String serviceFragment) {
    this.legacy().withServiceFragment(serviceFragment);
    return this;
  }

  public BasicExternalClusterBuilder withClientReconnectWindowTime(final int clientReconnectWindowTime) {
    this.reconnectWindow = clientReconnectWindowTime;
    return this;
  }

  public BasicExternalClusterBuilder withFailoverPriorityVoterCount(final int failoverPriorityVoterCount) {
    this.voters = failoverPriorityVoterCount;
    return this;
  }

  public BasicExternalClusterBuilder withTcProperties(Properties tcProperties) {
    this.tcProperties.putAll(tcProperties);
    return this;
  }

  public BasicExternalClusterBuilder withTcProperty(String key, String value) {
    this.tcProperties.setProperty(key, value);
    return this;
  }

  public BasicExternalClusterBuilder withSystemProperties(Properties props) {
    this.systemProperties.putAll(props);
    return this;
  }

  public BasicExternalClusterBuilder withSystemProperty(String key, String value) {
    this.systemProperties.put(key, value);
    return this;
  }

  public BasicExternalClusterBuilder withServerHeap(int heapSize) {
    this.serverHeapSize = heapSize;
    this.customHeapSize = heapSize;
    return this;
  }

  public BasicExternalClusterBuilder withOutputStream(OutputStream output) {
    this.parentStream = output;
    return this;
  }

  public BasicExternalClusterBuilder logConfigExtensionResourceName(String logConfigExt) {
    this.logConfigExt = logConfigExt;
    return this;
  }

  public BasicExternalClusterBuilder deploymentBuilder(ServerDeploymentBuilder deploy) {
    this.serverBuilder = deploy;
    return this;
  }

  public BasicExternalClusterBuilder startupBuilder(Supplier<StartupCommandBuilder> startupBuilder) {
    this.startupBuilder = startupBuilder;
    return this;
  }

  public BasicExternalClusterBuilder inline(boolean yes) {
    if (yes) {
      this.serverHeapSize = -1;
      tcProperties.put("tc.messages.packup.enabled", false);
    } else {
      this.serverHeapSize = customHeapSize;
      tcProperties.put("tc.messages.packup.enabled", true);
    }
    return this;
  }

  public BasicExternalClusterBuilder withConsistentStartup(boolean consistent) {
    this.consistent = consistent;
    return this;
  }

  public BasicExternalClusterBuilder withServerPlugin(Path api, Path impl) {
    this.serverBuilder.addPlugin(api, impl);
    return this;
  }

  public Cluster build() {
    if (clusterDirectory == null) {
      String dir = System.getProperty("galvan.dir");
      try {
        if (dir != null) {
          clusterDirectory = Files.createDirectories(Paths.get(dir));
        } else {
          clusterDirectory = Files.createTempDirectory("serverWorking");
        }
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }

    String debugPortString = System.getProperty("serverDebugPortStart");
    int serverDebugStartPort = debugPortString != null ? Integer.parseInt(debugPortString) : 0;

    return new BasicExternalCluster(clusterDirectory, stripeSize, this.serverBuilder.deploy(), serverHeapSize, systemProperties, tcProperties,
            this.reconnectWindow, this.voters, this.consistent, serverDebugStartPort,
        logConfigExt, parentStream,
            Optional.ofNullable(startupBuilder).orElse(()-> new DefaultStartupCommandBuilder(Optional.ofNullable(configBuilder).orElse(new DefaultLegacyConfigBuilder()))));

  }
}
