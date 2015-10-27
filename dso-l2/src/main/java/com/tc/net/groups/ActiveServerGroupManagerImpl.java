/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.net.groups;

import com.tc.async.api.Sink;
import com.tc.config.NodesStore;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.text.PrettyPrinter;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class provides an easy mechanism to send and receive messages to an active server in a group and to broadcast a
 * message to all active servers in the cluster. Anybody interacting with this class need not worry about the actual
 * ServerID of the active server for a particular Group since that can keep changing.
 * <P>
 * Instead they can send messages to a group (using the groupID) and the message will automatically be routed to the
 * active server in that group if one such server exists.
 * <p>
 * Hence all interfaces to this class takes GroupID (instead of ServerID like other implementations of GroupManager)
 * except for one exception. @see messageReceived() below.
 */
public class ActiveServerGroupManagerImpl implements GroupManager<AbstractGroupMessage>, GroupMessageListener<AbstractGroupMessage>, ActiveServerListener {

  private static final TCLogger                           logger           = TCLogging
                                                                               .getLogger(ActiveServerGroupManagerImpl.class);

  private final ActiveServerIDManager                     activeServerIDManager;
  private final GroupManager<AbstractGroupMessage>                              groupManager;
  private final CopyOnWriteArrayList<GroupEventsListener> groupListeners   = new CopyOnWriteArrayList<>();
  private final Map<String, GroupMessageListener<? extends GroupMessage>>         messageListeners = new ConcurrentHashMap<>();

  public ActiveServerGroupManagerImpl(ActiveServerIDManager activeServerIDManager, GroupManager<AbstractGroupMessage> groupManager) {
    this.activeServerIDManager = activeServerIDManager;
    this.groupManager = groupManager;
    activeServerIDManager.addActiveServerListener(this);
  }

  /**
   * @returns this GroupID
   */
  @Override
  public NodeID getLocalNodeID() {
    return this.activeServerIDManager.getLocalGroupID();
  }

  /**
   * @returns this GroupID
   */
  @Override
  public NodeID join(Node thisNode, NodesStore nodesStore) {
    // The underlying group manager should already be joined elsewhere, return local GroupID
    return this.activeServerIDManager.getLocalGroupID();
  }

  @Override
  public void registerForGroupEvents(GroupEventsListener listener) {
    this.groupListeners.add(listener);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <N extends AbstractGroupMessage> void registerForMessages(Class<N> msgClass, GroupMessageListener<N> listener) {
    GroupMessageListener<? extends GroupMessage> prev = this.messageListeners.put(msgClass.getName(), listener);
    if (prev != null) {
      logger.warn("Previous listener removed : " + prev);
    }
    // TODO:  Find a correct way of rationalizing these types in the generic.
    Class<N> castMessageClass = msgClass;
    GroupMessageListener<N> castThis = (GroupMessageListener<N>)this;
    this.groupManager.registerForMessages(castMessageClass, castThis);
  }

  /**
   * XXX:: We deliberately don't convert the serverID to GroupID while delivering the messages to avoid a race. We might
   * receive a message from a node before it publishes (or we processed the message) declaring its the active server in
   * the group. Hence a look up of the serverID to GroupID might return null when you do so. We could avoid this race by
   * falling back on the config, but will prove to be a pain when we want to change the config dynamically later.
   * <p>
   * So its left to the message to contain the necessary info regarding the group from which the message is sent if the
   * receiver needs that kind of information. NOTE:: With the new split brain (blackListed server logic) the above
   * comments is not accurate. Its left to explain some history.
   */
  @Override
  public void messageReceived(NodeID fromNode, AbstractGroupMessage msg) {
    if (this.activeServerIDManager.isBlackListedServer(fromNode)) {
      logger.warn("Dropping message from blacklisted server : " + fromNode + " Dropped Message : " + msg);
      return;
    }
    GroupMessageListener<? extends GroupMessage> listener = this.messageListeners.get(msg.getClass().getName());
    if (listener != null) {
      ((GroupMessageListener<GroupMessage>)listener).messageReceived(fromNode, msg);
    } else {
      String errorMsg = "No Route for " + msg + " from " + fromNode;
      logger.error(errorMsg);
      throw new AssertionError(errorMsg);
    }
  }

  @Override
  public <N extends AbstractGroupMessage> void routeMessages(Class<N> msgClass, Sink<N> sink) {
    registerForMessages(msgClass, new RouteGroupMessagesToSink(msgClass.getName(), sink));
  }

  @Override
  public void sendAll(AbstractGroupMessage msg) {
    Set<ServerID> activeServers = this.activeServerIDManager.getAllActiveServerIDs();
    removeLocalNodeID(activeServers);
    this.groupManager.sendAll(msg, activeServers);
  }

  private void removeLocalNodeID(Set<ServerID> activeServers) {
    activeServers.remove(this.groupManager.getLocalNodeID());
  }

  @Override
  public void sendAll(AbstractGroupMessage msg, Set<? extends NodeID> nodeIDs) {
    Set<ServerID> activeServers = getActiveServersFor(nodeIDs);
    removeLocalNodeID(activeServers);
    this.groupManager.sendAll(msg, activeServers);
  }

  private Set<ServerID> getActiveServersFor(Set<? extends NodeID> nodeIDs) {
    Set<ServerID> activeServers = new HashSet<>();
    for (NodeID nodeID : nodeIDs) {
      GroupID groupID = (GroupID) nodeID;
      ServerID activeServer = this.activeServerIDManager.getActiveServerIDFor(groupID);
      if (activeServer != null) {
        activeServers.add(activeServer);
      } else {
        logger.warn("Active server for " + groupID + " not known. Skipping sending message to group " + groupID);
      }
    }
    return activeServers;
  }

  @Override
  public GroupResponse sendAllAndWaitForResponse(AbstractGroupMessage msg) throws GroupException {
    Set<ServerID> activeServers = this.activeServerIDManager.getAllActiveServerIDs();
    removeLocalNodeID(activeServers);
    return this.groupManager.sendAllAndWaitForResponse(msg, activeServers);
  }

  @Override
  public GroupResponse sendAllAndWaitForResponse(AbstractGroupMessage msg, Set<? extends NodeID> nodeIDs) throws GroupException {
    Set<ServerID> activeServers = getActiveServersFor(nodeIDs);
    removeLocalNodeID(activeServers);
    return this.groupManager.sendAllAndWaitForResponse(msg, activeServers);
  }

  @Override
  public void sendTo(NodeID node, AbstractGroupMessage msg) throws GroupException {
    ServerID activeServer = this.activeServerIDManager.getActiveServerIDFor((GroupID) node);
    if (activeServer == null) {
      logger.warn("Active server for " + node + " not known. Skipping sending message to group " + node);
    } else if (activeServer.equals(this.groupManager.getLocalNodeID())) {
      logger.warn("Active server for " + node + " is local node. " + this.groupManager.getLocalNodeID()
                  + " Can't send message to self. Skipping sending message to group " + node);
    } else {
      this.groupManager.sendTo(activeServer, msg);
    }
  }

  @Override
  public AbstractGroupMessage sendToAndWaitForResponse(NodeID nodeID, AbstractGroupMessage msg) throws GroupException {
    ServerID activeServer = this.activeServerIDManager.getActiveServerIDFor((GroupID) nodeID);
    if (activeServer == null) {
      logger.warn("Active server for " + nodeID + " not known. Skipping sending message to group " + nodeID);
      return null;
    } else if (activeServer.equals(this.groupManager.getLocalNodeID())) {
      logger.warn("Active server for " + nodeID + " is local node. " + this.groupManager.getLocalNodeID()
                  + " Can't send message to self. Skipping sending message to group " + nodeID);
      return null;
    } else {
      return this.groupManager.sendToAndWaitForResponse(activeServer, msg);
    }
  }

  /**
   * Not supported now, in future we may have to support these to make more intelligent decisions on split brain.
   */
  @Override
  public void setZapNodeRequestProcessor(ZapNodeRequestProcessor processor) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported now, in future we may have to support these to make more intelligent decisions on split brain.
   */
  @Override
  public void zapNode(NodeID nodeID, int type, String reason) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void activeServerJoined(GroupID groupID, ServerID serverID) {
    fireNodeEvent(groupID, true);
  }

  @Override
  public void activeServerLeft(GroupID groupID, ServerID serverID) {
    fireNodeEvent(groupID, false);
  }

  private void fireNodeEvent(NodeID nodeID, boolean joined) {
    for (GroupEventsListener listener : this.groupListeners) {
      if (joined) {
        listener.nodeJoined(nodeID);
      } else {
        listener.nodeLeft(nodeID);
      }
    }
  }

  @Override
  public boolean isNodeConnected(NodeID sid) {
    return this.groupManager.isNodeConnected(sid);
  }

  // no need to dump anything here as group manager and activeServerIDManager
  // register themselves for dumping separately
  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.indent().print(getClass().getName()).flush();
    return out;
  }

  @Override
  public boolean isServerConnected(String nodeName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void closeMember(ServerID next) {
    this.groupManager.closeMember(next);
  }
}
