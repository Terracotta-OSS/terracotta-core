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

import com.tc.async.api.Sink;
import com.tc.config.GroupConfiguration;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.core.TCConnectionManager;
import com.tc.util.Assert;
import com.tc.util.UUID;
import java.util.Collections;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class TestActiveGroupManager implements GroupManager<GroupMessage> {

  private final ServerID                                  thisNodeID;
  private final CopyOnWriteArrayList<GroupEventsListener> groupListeners   = new CopyOnWriteArrayList<>();
  private final Map<String, GroupMessageListener<GroupMessage>>         messageListeners = new ConcurrentHashMap<>();
  private final List<SendToMessage>                       sendToMessages   = new LinkedList<>();
  private final List<SendToMessage>                       sendAllMessages  = new LinkedList<>();
  private boolean                                         connected        = true;

  public TestActiveGroupManager(String hostname, int port) {
    thisNodeID = new ServerID(new Node(hostname, port).getServerNodeName(), UUID.getUUID().toString().getBytes());
  }

  @Override
  public ServerID getLocalNodeID() {
    return thisNodeID;
  }

  @Override
  public boolean isNodeConnected(NodeID sid) {
    return connected;
  }
  
  public void setConnected(boolean connected) {
    this.connected = connected;
  }

  @Override
  public NodeID join(GroupConfiguration groupConfiguration) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void disconnect() {
    throw new UnsupportedOperationException("Not supported yet."); 
  }

  @Override
  public void registerForGroupEvents(GroupEventsListener listener) {
    groupListeners.add(listener);
  }

  @Override
  public void unregisterForGroupEvents(GroupEventsListener listener) {
    groupListeners.remove(listener);
  }

  @Override
  public <M extends GroupMessage> void registerForMessages(Class<? extends M> msgClass, GroupMessageListener<M> listener) {
    messageListeners.put(msgClass.getName(), (GroupMessageListener<GroupMessage>)listener);
  }

  @Override
  public <M extends GroupMessage> void routeMessages(Class<? extends M> msgClass, Sink<M> sink) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void sendAll(GroupMessage msg) {
    sendAllMessages.add(new SendToMessage(null, msg));
  }

  @Override
  public void sendAll(GroupMessage msg, Set<? extends NodeID> nodeIDs) {
    throw new UnsupportedOperationException();
  }

  @Override
  public GroupResponse sendAllAndWaitForResponse(GroupMessage msg) {
    throw new UnsupportedOperationException();
  }

  @Override
  public GroupResponse sendAllAndWaitForResponse(GroupMessage msg, Set<? extends NodeID> nodeIDs) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void sendTo(NodeID nodeID, GroupMessage msg) {
    sendToMessages.add(new SendToMessage(nodeID, msg));
  }

  @Override
  public void sendTo(Set<String> nodes, GroupMessage msg) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void sendToWithSentCallback(NodeID node, GroupMessage msg, Runnable sentCallback) throws GroupException {
    Assert.fail("NOT CALLED IN CURRENT TESTS");
  }

  @Override
  public GroupMessage sendToAndWaitForResponse(NodeID nodeID, GroupMessage msg) {
    throw new UnsupportedOperationException();
  }

  @Override
  public GroupResponse<GroupMessage> sendToAndWaitForResponse(Set<String> nodes, GroupMessage msg) throws GroupException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setZapNodeRequestProcessor(ZapNodeRequestProcessor processor) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void zapNode(NodeID nodeID, int type, String reason) {
    throw new UnsupportedOperationException();
  }

  public void messageReceived(NodeID fromNode, GroupMessage msg) {
    GroupMessageListener listener = messageListeners.get(msg.getClass().getName());
    if (listener != null) {
      listener.messageReceived(fromNode, msg);
    } else {
      String errorMsg = "No Route for " + msg + " from " + fromNode;
      throw new AssertionError(errorMsg);
    }
  }

  public void fireNodeEvent(NodeID nodeID, boolean joined) {
    for (GroupEventsListener listener : groupListeners) {
      if (joined) {
        listener.nodeJoined(nodeID);
      } else {
        listener.nodeLeft(nodeID);
      }
    }
  }

  @Override
  public void shutdown() {
    
  }

  static class SendToMessage {
    private final NodeID       nodeID;
    private final GroupMessage groupMessage;

    public SendToMessage(NodeID nodeID, GroupMessage groupMessage) {
      this.nodeID = nodeID;
      this.groupMessage = groupMessage;
    }

    public NodeID getNodeID() {
      return this.nodeID;
    }

    public GroupMessage getGroupMessage() {
      return this.groupMessage;
    }
  }

  List<SendToMessage> getSendToMessages() {
    return sendToMessages;
  }

  List<SendToMessage> getSendAllMessages() {
    return sendAllMessages;
  }

  @Override
  public boolean isServerConnected(String nodeName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void closeMember(ServerID next) {

    //
  }

  @Override
  public void closeMember(String name) {

  }

  @Override
  public Map<String, ?> getStateMap() {
    return Collections.emptyMap();
  }

  @Override
  public TCConnectionManager getConnectionManager() {
    return null;
  }

  @Override
  public boolean isStopped() {
    return false;
  }

  @Override
  public int getBufferCount() {
    return 0;
  }


}
