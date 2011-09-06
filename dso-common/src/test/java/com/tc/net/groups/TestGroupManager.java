/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.async.api.Sink;
import com.tc.config.NodesStore;
import com.tc.exception.TCRuntimeException;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.text.PrettyPrinter;

import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

public class TestGroupManager implements GroupManager {

  public NodeID              localNodeID         = new ServerID("Test-Server", new byte[] { 5, 6, 6 });
  public LinkedBlockingQueue broadcastedMessages = new LinkedBlockingQueue();
  public LinkedBlockingQueue sentMessages        = new LinkedBlockingQueue();

  public NodeID getLocalNodeID() {
    return localNodeID;
  }

  public void closeMember(ServerID next) {
    // NOP
  }

  public NodeID join(Node thisNode, NodesStore nodeStore) {
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
    try {
      broadcastedMessages.put(msg);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  public void sendAll(GroupMessage msg, Set nodeIDs) {
    try {
      broadcastedMessages.put(msg);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  public GroupResponse sendAllAndWaitForResponse(GroupMessage msg) {
    try {
      broadcastedMessages.put(msg);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
    return null;
  }

  public GroupResponse sendAllAndWaitForResponse(GroupMessage msg, Set nodeIDs) {
    try {
      broadcastedMessages.put(msg);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
    return null;
  }

  public void sendTo(NodeID node, GroupMessage msg) {
    try {
      sentMessages.put(new Object[] { node, msg });
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
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

  public boolean isNodeConnected(NodeID sid) {
    return true;
  }

  public boolean isServerConnected(String nodeName) {
    throw new UnsupportedOperationException();
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    throw new UnsupportedOperationException();
  }
}
