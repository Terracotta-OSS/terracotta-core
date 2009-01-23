/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.async.api.Sink;
import com.tc.net.NodeID;
import com.tc.net.ServerID;

import java.util.Set;

public class TestGroupManager implements GroupManager {

  public NodeID localNodeID = new ServerID("Test-Server", new byte[] { 5, 6, 6 });

  public NodeID getLocalNodeID() {
    return localNodeID;
  }

  public NodeID join(Node thisNode, Node[] allNodes) {
    return localNodeID;
  }

  public void registerForGroupEvents(GroupEventsListener listener) {
    // NOP
  }

  public void registerForMessages(Class msgClass, GroupMessageListener listener) {
    // NOP
  }

  public void routeMessages(Class msgClass, Sink sink) {
    // NOP
  }

  public void sendAll(GroupMessage msg) {
    // NOP
  }

  public void sendAll(GroupMessage msg, Set nodeIDs) {
    // NOP
  }

  public GroupResponse sendAllAndWaitForResponse(GroupMessage msg) {
    return null;
  }

  public GroupResponse sendAllAndWaitForResponse(GroupMessage msg, Set nodeIDs) {
    return null;
  }

  public void sendTo(NodeID node, GroupMessage msg) {
    // NOP
  }

  public GroupMessage sendToAndWaitForResponse(NodeID nodeID, GroupMessage msg) {
    return null;
  }

  public void setZapNodeRequestProcessor(ZapNodeRequestProcessor processor) {
    // NOP
  }

  public void zapNode(NodeID nodeID, int type, String reason) {
    // NOP
  }

}
