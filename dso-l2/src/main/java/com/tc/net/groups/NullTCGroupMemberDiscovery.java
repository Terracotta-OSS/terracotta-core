/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
