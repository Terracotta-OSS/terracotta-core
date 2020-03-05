/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.testing.config;


import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * This class is essentially a struct containing the data which describes how the servers in the stripe are to be
 * configured, where they are sourced, and how they should be run.
 * It exists to give context to the parameters in CommonIdioms.
 */
public class StripeConfiguration {
  private final Path kitOriginDir;
  private final Path stripeInstallationDir;
  private final int serverHeapInM;
  private final String stripeName;
  private final Path tcConfig;
  private final String logConfigExtension;
  private final boolean consistentStart;
  private final String uri;
  private final ClusterInfo clusterInfo;
  private final List<String> serverNames;
  private final List<Integer> serverDebugPorts;
  private final Properties serverProperties = new Properties();
  private final List<Integer> serverPorts;
  private final List<Integer> serverGroupPorts;
  private final Set<Path> extraJarPaths = new HashSet<>();
  private final List<InetSocketAddress> serverAddresses;

  public StripeConfiguration(Path kitOriginDir, Path stripeInstallationDir, List<Integer> serverDebugPorts, List<Integer> serverPorts,
                             List<Integer> serverGroupPorts, List<String> serverNames, String stripeName, Set<Path> extraJarPaths,
                             int serverHeapInM, String logConfigExt, Properties serverProperties, boolean consistentStart,
                             Path tcConfig) {
    this.kitOriginDir = kitOriginDir;
    this.stripeInstallationDir = stripeInstallationDir;
    this.serverDebugPorts = serverDebugPorts;
    this.serverPorts = serverPorts;
    this.serverGroupPorts = serverGroupPorts;
    this.serverNames = serverNames;
    this.stripeName = stripeName;
    this.tcConfig = tcConfig;
    this.extraJarPaths.addAll(extraJarPaths);
    this.serverHeapInM = serverHeapInM;
    this.logConfigExtension = logConfigExt;
    this.consistentStart = consistentStart;
    this.clusterInfo = buildClusterInfo(serverNames, serverPorts, serverGroupPorts);
    this.uri = buildUri(this.serverPorts);
    this.serverProperties.putAll(serverProperties);
    this.serverAddresses = buildServerAddresses(this.serverPorts);
  }

  public Path getKitOriginDir() {
    return kitOriginDir;
  }

  public int getServerHeapInM() {
    return serverHeapInM;
  }

  public List<Integer> getServerPorts() {
    return Collections.unmodifiableList(serverPorts);
  }

  public List<Integer> getServerGroupPorts() {
    return Collections.unmodifiableList(serverGroupPorts);
  }

  public List<Integer> getServerDebugPorts() {
    return Collections.unmodifiableList(serverDebugPorts);
  }

  public String getStripeName() {
    return stripeName;
  }

  public String getLogConfigExtension() {
    return logConfigExtension;
  }

  public boolean isConsistentStart() {
    return consistentStart;
  }

  public String getUri() {
    return uri;
  }

  public List<InetSocketAddress> getServerAddresses() {
    return Collections.unmodifiableList(serverAddresses);
  }

  public ClusterInfo getClusterInfo() {
    return clusterInfo;
  }

  public Properties getServerProperties() {
    return serverProperties;
  }

  public Set<Path> getExtraJarPaths() {
    return Collections.unmodifiableSet(extraJarPaths);
  }

  public List<String> getServerNames() {
    return Collections.unmodifiableList(serverNames);
  }

  public Path getStripeInstallationDir() {
    return stripeInstallationDir;
  }

  public Path getTcConfig() {
    return tcConfig;
  }

  private String buildUri(List<Integer> serverPorts) {
    StringBuilder connectUri = new StringBuilder("terracotta://");
    boolean needsComma = false;
    for (int i = 0; i < serverNames.size(); ++i) {
      if (needsComma) {
        connectUri.append(",");
      }
      connectUri.append("localhost:").append(serverPorts.get(i));
      needsComma = true;
    }
    return connectUri.toString();
  }

  private List<InetSocketAddress> buildServerAddresses(List<Integer> serverPorts) {
    List<InetSocketAddress> addresses = new ArrayList<>();
    for (int i = 0; i < serverNames.size(); ++i) {
      addresses.add(InetSocketAddress.createUnresolved("localhost", serverPorts.get(i)));
    }
    return addresses;
  }

  private ClusterInfo buildClusterInfo(List<String> serverNames, List<Integer> serverPorts, List<Integer> groupPorts) {
    Map<String, ServerInfo> servers = new HashMap<>();

    for (int i = 0; i < serverNames.size(); i++) {
      String serverName = serverNames.get(i);
      servers.put(serverName, new ServerInfo(serverName, serverPorts.get(i), groupPorts.get(i)));
    }
    return new ClusterInfo(servers);
  }
}
