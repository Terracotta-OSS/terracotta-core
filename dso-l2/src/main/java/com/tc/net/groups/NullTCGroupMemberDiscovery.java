/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.groups;

import com.tc.async.api.EventContext;
import com.tc.net.NodeID;
import com.tc.util.Assert;

public class NullTCGroupMemberDiscovery implements TCGroupMemberDiscovery {

  @Override
  public Node getLocalNode() {
    Assert.fail();
    return null;
  }

  @Override
  public void setupNodes(Node local, Node[] nodes) {
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
  public void stop(long timeout) {
    return;
  }

  @Override
  public void discoveryHandler(EventContext context) {
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
  public void addNode(Node node) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeNode(Node node) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isServerConnected(String nodeName) {
    throw new UnsupportedOperationException();
  }
}
