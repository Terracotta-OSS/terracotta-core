/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

  @Override
  public NodeID getLocalNodeID() {
    return localNodeID;
  }

  @Override
  public void closeMember(ServerID next) {
    // NOP
  }

  @Override
  public NodeID join(Node thisNode, NodesStore nodeStore) {
    return localNodeID;
  }

  @Override
  public void registerForGroupEvents(GroupEventsListener listener) {
    // NOP
  }

  @Override
  public void registerForMessages(Class msgClass, GroupMessageListener listener) {
    // NOP
  }

  @Override
  public void routeMessages(Class msgClass, Sink sink) {
    // NOP
  }

  @Override
  public void sendAll(GroupMessage msg) {
    try {
      broadcastedMessages.put(msg);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  @Override
  public void sendAll(GroupMessage msg, Set nodeIDs) {
    try {
      broadcastedMessages.put(msg);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  @Override
  public GroupResponse sendAllAndWaitForResponse(GroupMessage msg) {
    try {
      broadcastedMessages.put(msg);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
    return null;
  }

  @Override
  public GroupResponse sendAllAndWaitForResponse(GroupMessage msg, Set nodeIDs) {
    try {
      broadcastedMessages.put(msg);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
    return null;
  }

  @Override
  public void sendTo(NodeID node, GroupMessage msg) {
    try {
      sentMessages.put(new Object[] { node, msg });
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  @Override
  public GroupMessage sendToAndWaitForResponse(NodeID nodeID, GroupMessage msg) {
    return null;
  }

  @Override
  public void setZapNodeRequestProcessor(ZapNodeRequestProcessor processor) {
    // NOP
  }

  @Override
  public void zapNode(NodeID nodeID, int type, String reason) {
    // NOP
  }

  @Override
  public boolean isNodeConnected(NodeID sid) {
    return true;
  }

  @Override
  public boolean isServerConnected(String nodeName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    throw new UnsupportedOperationException();
  }
}
