/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.async.api.EventContext;
import com.tc.net.NodeID;
import com.tc.util.Assert;

public class NullTCGroupMemberDiscovery implements TCGroupMemberDiscovery {

  public Node getLocalNode() {
    Assert.fail();
    return null;
  }

  public void setupNodes(Node local, Node[] nodes) {
    return;
  }

  public void setTCGroupManager(TCGroupManagerImpl manager) {
    return;
  }

  public void start() {
    return;
  }

  public void stop(long timeout) {
    return;
  }

  public void discoveryHandler(EventContext context) {
    Assert.fail();
  }

  public void nodeJoined(NodeID nodeID) {
    return;
  }

  public void nodeLeft(NodeID nodeID) {
    return;
  }

  public void nodeZapped(NodeID nodeID) {
    return;
  }

  public boolean isValidClusterNode(NodeID nodeID) {
    return true;
  }

  public void addNode(Node node) {
    throw new UnsupportedOperationException();
  }

  public void removeNode(Node node) {
    throw new UnsupportedOperationException();
  }

  public boolean isServerConnected(String nodeName) {
    throw new UnsupportedOperationException();
  }
}
