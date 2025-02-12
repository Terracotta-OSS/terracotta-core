/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * This class is essentially a struct containing the data which describes how the servers in the stripe are to be
 * configured, where they are sourced, and how they should be run.
 * It exists to give context to the parameters in CommonIdioms.
 */
public class StripeConfiguration {
  private final String stripeName;
  private final String logConfigExtension;
  private final String uri;
  private final ClusterInfo clusterInfo;
  private final List<String> serverNames;
  private final List<Integer> serverDebugPorts;
  private final Properties serverProperties = new Properties();
  private final Properties tcProperties = new Properties();
  private final List<Integer> serverPorts;
  private final List<Integer> serverGroupPorts;
  private final List<InetSocketAddress> serverAddresses;
  private final int reconnect;
  private final int voters;
  private final boolean consistent;

  public StripeConfiguration(List<Integer> serverDebugPorts, List<Integer> serverPorts,
                             List<Integer> serverGroupPorts, List<String> serverNames, String stripeName,
                             String logConfigExt, Properties serverProperties, Properties tcProperties,
                             int reconnect, int voters, boolean consistent
  ) {
    this.serverDebugPorts = serverDebugPorts;
    this.serverPorts = serverPorts;
    this.serverGroupPorts = serverGroupPorts;
    this.serverNames = serverNames;
    this.stripeName = stripeName;
    this.logConfigExtension = logConfigExt;
    this.clusterInfo = buildClusterInfo(serverNames, serverPorts, serverGroupPorts);
    this.uri = buildUri(this.serverPorts);
    this.serverProperties.putAll(serverProperties);
    this.tcProperties.putAll(tcProperties);
    this.reconnect = reconnect;
    this.voters = voters;
    this.consistent = consistent;
    this.serverAddresses = buildServerAddresses(this.serverPorts);
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
  
  public Properties getTcProperties() {
    return tcProperties;
  }

  public int getReconnectWindow() {
    return reconnect;
  }

  public int getVoters() {
    return voters;
  }

  public boolean isConsistent() {
    return consistent;
  }
  
  public List<String> getServerNames() {
    return Collections.unmodifiableList(serverNames);
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
    List<ServerInfo> servers = new ArrayList<>();

    for (int i = 0; i < serverNames.size(); i++) {
      servers.add(new ServerInfo(serverNames.get(i), serverPorts.get(i), groupPorts.get(i)));
    }
    return new ClusterInfo(servers);
  }
}
