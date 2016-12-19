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
package org.terracotta.testing.master;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author vmad
 */
public class ClusterInfo {
  private static final String SERVER_INFO_DELIM = ";";
  private final Map<String, ServerInfo> servers;

  public ClusterInfo(Map<String, ServerInfo> servers) {
    this.servers = servers;
  }

  public ServerInfo getServerInfo(String name) {
    return servers.get(name);
  }

  public Collection<ServerInfo> getServersInfo() {
    return Collections.unmodifiableCollection(servers.values());
  }

  public String encode() {
    StringBuffer stringBuffer = new StringBuffer();

    for(ServerInfo serverInfo : servers.values()) {
      stringBuffer.append(serverInfo.encode());
      stringBuffer.append(SERVER_INFO_DELIM);
    }

    return stringBuffer.toString();
  }

  public static ClusterInfo decode(String from) {
    String[] serverInfoTokens = from.split(SERVER_INFO_DELIM);
    Map<String, ServerInfo> servers = new HashMap<>();

    for(int i = 0; i < serverInfoTokens.length; i++) {
      ServerInfo serverInfo = ServerInfo.decode(serverInfoTokens[i]);
      servers.put(serverInfo.getName(), serverInfo);
    }

    return new ClusterInfo(servers);
  }
}
