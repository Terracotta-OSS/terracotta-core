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
package org.terracotta.testing.rules;

import java.io.IOException;
import java.nio.file.Files;
import org.terracotta.testing.config.StartupCommandBuilder;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;
import static java.util.stream.Collectors.toList;
import java.util.stream.IntStream;

import static org.terracotta.testing.config.ConfigConstants.DEFAULT_SERVER_HEAP_MB;
import org.terracotta.testing.config.DefaultStartupCommandBuilder;
import org.terracotta.testing.master.ServerDeploymentBuilder;
import org.terracotta.testing.support.PortTool;
import org.terracotta.utilities.test.net.PortManager;

public class BasicExternalClusterBuilder {
  private final int stripeSize;

  private Path clusterDirectory;
  private final Properties systemProperties = new Properties();

  private String logConfigExt = "logback-ext.xml";
  private int serverHeapSize = DEFAULT_SERVER_HEAP_MB;
  private boolean inline = true;
  private final DefaultStartupCommandBuilder cmdBuilder;
  private final ServerDeploymentBuilder serverBuilder = new ServerDeploymentBuilder();
  private Supplier<StartupCommandBuilder> startupBuilder;
    
  private BasicExternalClusterBuilder(final int stripeSize) {
    this.stripeSize = stripeSize;
    
    cmdBuilder = new DefaultStartupCommandBuilder();
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

  public BasicExternalClusterBuilder withNamespaceFragment(final String namespaceFragment) {
    this.cmdBuilder.withNamespaceFragment(namespaceFragment);
    return this;
  }

  public BasicExternalClusterBuilder withServiceFragment(final String serviceFragment) {
    this.cmdBuilder.withServiceFragment(serviceFragment);
    return this;
  }

  public BasicExternalClusterBuilder withClientReconnectWindowTime(final int clientReconnectWindowTime) {
    this.cmdBuilder.withClientReconnectWindowTime(clientReconnectWindowTime);
    return this;
  }
  
  public BasicExternalClusterBuilder withFailoverPriorityVoterCount(final int failoverPriorityVoterCount) {
    this.cmdBuilder.withFailoverPriorityVoterCount(failoverPriorityVoterCount);
    return this;
  }
  
  public BasicExternalClusterBuilder withTcProperties(Properties tcProperties) {
    this.cmdBuilder.withTcProperties(tcProperties);
    return this;
  }

  public BasicExternalClusterBuilder withTcProperty(String key, String value) {
    this.cmdBuilder.withTcProperty(key, value);
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
    return this;
  }

  public BasicExternalClusterBuilder logConfigExtensionResourceName(String logConfigExt) {
    this.logConfigExt = logConfigExt;
    return this;
  }

  public BasicExternalClusterBuilder startupBuilder(Supplier<StartupCommandBuilder> startupBuilder) {
    this.startupBuilder = startupBuilder;
    return this;
  }

  public BasicExternalClusterBuilder inline(boolean yes) {
    this.inline = yes;
    return this;
  }

  public BasicExternalClusterBuilder withConsistentStartup(boolean consistent) {
    this.cmdBuilder.withConsistentStartup(consistent);
    return this;
  }
  
  public BasicExternalClusterBuilder withServerPlugin(Path api, Path impl) {
    this.serverBuilder.addPlugin(api, impl);
    return this;
  }

  public Cluster build() {
    if (clusterDirectory == null) {
      try {
        clusterDirectory = Files.createTempDirectory("serverWorking");
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }

    if (inline) {
      return new BasicInlineCluster(clusterDirectory, stripeSize, this.serverBuilder.deploy(), systemProperties,
        logConfigExt, serverHeapSize, Optional.ofNullable(startupBuilder).orElse(cmdBuilder::copy));
    } else {
      return new BasicExternalCluster(clusterDirectory, stripeSize, this.serverBuilder.deploy(), systemProperties,
        logConfigExt, serverHeapSize, Optional.ofNullable(startupBuilder).orElse(cmdBuilder::copy));
    }
  }
}
