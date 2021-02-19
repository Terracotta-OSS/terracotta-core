/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.testing.rules;

import org.terracotta.testing.config.StartupCommandBuilder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;
import org.terracotta.testing.config.ArgOnlyStartupCommandBuilder;

import static org.terracotta.testing.config.ConfigConstants.DEFAULT_CLIENT_RECONNECT_WINDOW;
import static org.terracotta.testing.config.ConfigConstants.DEFAULT_SERVER_HEAP_MB;
import static org.terracotta.testing.config.ConfigConstants.DEFAULT_VOTER_COUNT;
import org.terracotta.testing.config.DefaultStartupCommandBuilder;

public class BasicExternalClusterBuilder {
  private final int stripeSize;

  private Path clusterDirectory = Paths.get("target").resolve("galvan");
  private Set<Path> serverJars = Collections.emptySet();
  private String namespaceFragment = "";
  private String serviceFragment = "";
  private int clientReconnectWindowTime = DEFAULT_CLIENT_RECONNECT_WINDOW;
  private int failoverPriorityVoterCount = DEFAULT_VOTER_COUNT;
  private boolean consistentStart = false;
  private Properties tcProperties = new Properties();
  private Properties systemProperties = new Properties();
  private String logConfigExt = "logback-ext.xml";
  private int serverHeapSize = DEFAULT_SERVER_HEAP_MB;
  private boolean inline = true;
  private Supplier<StartupCommandBuilder> startupBuilder;

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

  public BasicExternalClusterBuilder withServerJars(Set<Path> serverJars) {
    if (serverJars == null) {
      throw new NullPointerException("Server JARs list must be non-null");
    }
    this.serverJars = serverJars;
    return this;
  }

  public BasicExternalClusterBuilder withNamespaceFragment(final String namespaceFragment) {
    if (namespaceFragment == null) {
      throw new NullPointerException("Namespace fragment must be non-null");
    }
    this.namespaceFragment = namespaceFragment;
    return this;
  }

  public BasicExternalClusterBuilder withServiceFragment(final String serviceFragment) {
    if (serviceFragment == null) {
      throw new NullPointerException("Service fragment must be non-null");
    }
    this.serviceFragment = serviceFragment;
    return this;
  }

  public BasicExternalClusterBuilder withClientReconnectWindowTime(final int clientReconnectWindowTime) {
    this.clientReconnectWindowTime = clientReconnectWindowTime;
    return this;
  }

  /**
   * Zero or any positive value will tune the cluster for consistency and set the respective voter count as provided.
   * A value of -1 will tune the cluster for availability. This is the default.
   */
  public BasicExternalClusterBuilder withFailoverPriorityVoterCount(final int failoverPriorityVoterCount) {
    this.failoverPriorityVoterCount = failoverPriorityVoterCount;
    return this;
  }

  public BasicExternalClusterBuilder withTcProperties(Properties tcProperties) {
    this.tcProperties.putAll(tcProperties);
    return this;
  }

  public BasicExternalClusterBuilder withTcProperty(String key, String value) {
    this.tcProperties.put(key, value);
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
    this.consistentStart = consistent;
    return this;
  }

  public Cluster build() {
    if (inline) {
      return new BasicInlineCluster(clusterDirectory, stripeSize, serverJars, namespaceFragment, serviceFragment,
        clientReconnectWindowTime, failoverPriorityVoterCount, consistentStart, tcProperties, systemProperties,
        logConfigExt, serverHeapSize, Optional.ofNullable(startupBuilder).orElse(ArgOnlyStartupCommandBuilder::new));
    } else {
      return new BasicExternalCluster(clusterDirectory, stripeSize, serverJars, namespaceFragment, serviceFragment,
        clientReconnectWindowTime, failoverPriorityVoterCount, consistentStart, tcProperties, systemProperties,
        logConfigExt, serverHeapSize, Optional.ofNullable(startupBuilder).orElse(DefaultStartupCommandBuilder::new));
    }
  }
}
