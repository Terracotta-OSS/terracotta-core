/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.async.api.Sink;
import com.tc.config.ClusterInfo;
import com.tc.config.NodesStore;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

// Note that this class is only used in testing.
public class VirtualTCGroupManagerImpl<M extends GroupMessage> implements GroupManager<M>, GroupEventsListener, GroupMessageListener<M> {
  private static final TCLogger                           logger           = TCLogging
                                                                               .getLogger(VirtualTCGroupManagerImpl.class);
  private final GroupManager<M>                              groupManager;
  private final CopyOnWriteArrayList<GroupEventsListener> groupListeners   = new CopyOnWriteArrayList<>();
  private final Map<String, GroupMessageListener<M>>         messageListeners = new ConcurrentHashMap<>();
  private final Set<NodeID>                               groupNodeIDs     = new CopyOnWriteArraySet<>();
  private final ClusterInfo                               serverNamesOfThisGroup;

  public VirtualTCGroupManagerImpl(GroupManager<M> groupManager, ClusterInfo serverNamesOfThisGroup) {
    this.groupManager = groupManager;
    groupManager.registerForGroupEvents(this);
    this.serverNamesOfThisGroup = serverNamesOfThisGroup;
  }

  @Override
  public void closeMember(ServerID serverID) {
    this.groupManager.closeMember(serverID);
  }

  @Override
  public NodeID getLocalNodeID() {
    return groupManager.getLocalNodeID();
  }

  @Override
  public NodeID join(Node thisNode, NodesStore nodesStore) {
    // NOP here, the underlying groupManager should have already joined to the entire clustered.
    return this.groupManager.getLocalNodeID();
  }

  @Override
  public void registerForGroupEvents(GroupEventsListener listener) {
    groupListeners.add(listener);
  }

  @Override
  public <N extends M> void registerForMessages(Class<N> msgClass, GroupMessageListener<N> listener) {
    Class<M> castClass = ((Class<M>)msgClass);
    GroupMessageListener<M> prev = messageListeners.put(castClass.getName(), (GroupMessageListener<M>)listener);
    if (prev != null) {
      logger.warn("Previous listener removed : " + prev);
    }
    groupManager.registerForMessages(castClass, this);
  }

  @Override
  public void messageReceived(NodeID from, M msg) {
    if (!isThisGroup(from)) return;
    GroupMessageListener<M> listener = messageListeners.get(msg.getClass().getName());
    if (listener != null) {
      listener.messageReceived(from, msg);
    } else {
      String errorMsg = "No Route for " + msg + " from " + from;
      logger.error(errorMsg);
      throw new AssertionError(errorMsg);
    }
  }

  @Override
  public <N extends M> void routeMessages(Class<N> msgClass, Sink<N> sink) {
    registerForMessages(msgClass, new RouteGroupMessagesToSink<>(msgClass.getName(), sink));
  }

  @Override
  public void sendAll(M msg) {
    groupManager.sendAll(msg, groupNodeIDs);
  }

  @Override
  public void sendAll(M msg, Set<? extends NodeID> nodeIDs) {
    groupManager.sendAll(msg, nodeIDs);
  }

  @Override
  public GroupResponse<M> sendAllAndWaitForResponse(M msg) throws GroupException {
    return groupManager.sendAllAndWaitForResponse(msg, groupNodeIDs);
  }

  @Override
  public GroupResponse<M> sendAllAndWaitForResponse(M msg, Set<? extends NodeID> nodeIDs) throws GroupException {
    return groupManager.sendAllAndWaitForResponse(msg, nodeIDs);
  }

  @Override
  public void sendTo(NodeID nodeID, M msg) throws GroupException {
    Assert.assertTrue(isThisGroup(nodeID));
    groupManager.sendTo(nodeID, msg);
  }

  @Override
  public M sendToAndWaitForResponse(NodeID nodeID, M msg) throws GroupException {
    Assert.assertTrue(isThisGroup(nodeID));
    return groupManager.sendToAndWaitForResponse(nodeID, msg);
  }

  /**
   * FIXME:: Currently we simply pass the zapNode request process to the underlying group comm. This is ok as we have
   * only one Virtual Group Comm in the system using the underlying group comm and also since Active Active Group Comm
   * doesn't send zap requests as of now. But this might change in the future. Active-Active group comm can participate
   * in deciding a winner in a split brain scenario. Then this has to change.
   */
  @Override
  public void setZapNodeRequestProcessor(ZapNodeRequestProcessor processor) {
    groupManager.setZapNodeRequestProcessor(processor);
  }

  @Override
  public void zapNode(NodeID nodeID, int type, String reason) {
    Assert.assertTrue(isThisGroup(nodeID));
    groupManager.zapNode(nodeID, type, reason);
  }

  @Override
  public void nodeJoined(NodeID nodeID) {
    if (!isThisGroup(nodeID)) return;
    groupNodeIDs.add(nodeID);
    fireNodeEvent(nodeID, true);
  }

  @Override
  public void nodeLeft(NodeID nodeID) {
    if (!isThisGroup(nodeID)) return;
    groupNodeIDs.remove(nodeID);
    fireNodeEvent(nodeID, false);
  }

  /*
   * for testing purpose only
   */
  protected GroupManager<M> getBaseTCGroupManager() {
    return groupManager;
  }

  private boolean isThisGroup(NodeID nodeID) {
    Assert.assertTrue(nodeID instanceof ServerID);
    ServerID serverID = (ServerID) nodeID;
    return serverNamesOfThisGroup.hasServerInGroup(serverID.getName());
  }

  private void fireNodeEvent(NodeID nodeID, boolean joined) {
    if (logger.isDebugEnabled()) logger.debug("VirtualTCGroupManager fireNodeEvent: joined = " + joined + ", node = "
                                              + nodeID);
    for (GroupEventsListener listener : groupListeners) {
      if (joined) {
        listener.nodeJoined(nodeID);
      } else {
        listener.nodeLeft(nodeID);
      }
    }
  }

  @Override
  public boolean isNodeConnected(NodeID sid) {
    return groupManager.isNodeConnected(sid);
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    StringBuilder strBuffer = new StringBuilder();
    strBuffer.append(VirtualTCGroupManagerImpl.class.getSimpleName()).append(" [ ");

    strBuffer.append("groupNodeIDs: {").append(this.groupNodeIDs).append("} ]");

    out.indent().print(strBuffer.toString()).flush();
    out.visit(this.groupManager).flush();

    return out;
  }

  @Override
  public boolean isServerConnected(String nodeName) {
    return this.groupManager.isServerConnected(nodeName);
  }
}
