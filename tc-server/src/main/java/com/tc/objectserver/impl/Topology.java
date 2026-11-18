/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2026
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
package com.tc.objectserver.impl;

import com.tc.net.groups.Node;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Topology {
  private final Set<Node> servers = new HashSet<>();
  private final Node self;

  public Topology( Node self, Set<Node> servers) {
    this.self = self;
    this.servers.addAll(servers);
  }

  public String getLocal() {
    return self.getServerNodeName();
  }

  public Set<String> getServers() {
    return servers.stream().map(Node::getServerNodeName).collect(Collectors.toSet());
  }

  public Set<String> getPeers() {
    return servers.stream().filter(n->!n.equals(self)).map(Node::getServerNodeName).collect(Collectors.toSet());
  }
}
