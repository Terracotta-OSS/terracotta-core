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
package com.tc.net.groups;

import com.tc.net.NodeID;
import com.tc.util.Assert;

import java.util.Set;

public class NullTCGroupMemberDiscovery implements TCGroupMemberDiscovery {

  @Override
  public Node getLocalNode() {
    Assert.fail();
    return null;
  }

  @Override
  public void setupNodes(Node local, Set<Node> nodes) {
    return;
  }

  public void setTCGroupManager(TCGroupManagerImpl manager) {
    return;
  }

  @Override
  public void start() {
    return;
  }

  @Override
  public void stop() {
    return;
  }
  
  @Override
  public void discoveryHandler(DiscoveryStateMachine context) {
    Assert.fail();
  }

  @Override
  public void nodeJoined(NodeID nodeID) {
    return;
  }

  @Override
  public void nodeLeft(NodeID nodeID) {
    return;
  }

  public void nodeZapped(NodeID nodeID) {
    return;
  }

  @Override
  public boolean isValidClusterNode(NodeID nodeID) {
    return true;
  }

  @Override
  public boolean isServerConnected(String nodeName) {
    throw new UnsupportedOperationException();
  }
}
