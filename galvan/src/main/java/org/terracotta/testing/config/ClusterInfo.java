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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ClusterInfo {
  private static final String SERVER_INFO_DELIM = ";";
  private final List<ServerInfo> servers;

  public ClusterInfo(List<ServerInfo> servers) {
    this.servers = servers;
  }

  public ServerInfo getServerInfo(String name) {
    return servers.stream().filter(serverInfo -> serverInfo.getName().equals(name)).findFirst().orElseThrow(RuntimeException::new);
  }

  public List<ServerInfo> getServersInfo() {
    return Collections.unmodifiableList(servers);
  }

  public String encode() {
    return servers.stream().map(serverInfo -> serverInfo.encode() + SERVER_INFO_DELIM).collect(Collectors.joining());
  }

  public static ClusterInfo decode(String from) {
    String[] serverInfoTokens = from.split(SERVER_INFO_DELIM);
    List<ServerInfo> servers = Arrays.stream(serverInfoTokens).map(ServerInfo::decode).collect(Collectors.toList());
    return new ClusterInfo(servers);
  }
}
