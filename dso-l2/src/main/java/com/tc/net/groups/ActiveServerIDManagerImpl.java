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

import com.google.common.collect.Sets;
import com.tc.config.NodesStore;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.msg.ActiveJoinMessage;
import com.tc.l2.state.StateChangeListener;
import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.operatorevent.OperatorEventsActiveServerConnectionListener;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class ActiveServerIDManagerImpl implements ActiveServerIDManager, GroupMessageListener, StateChangeListener,
    GroupEventsListener, PrettyPrintable {
  private static final TCLogger            logger    = TCLogging.getLogger(ActiveServerIDManagerImpl.class);

  private final StateManager               stateManager;
  private final GroupManager               groupManager;
  private final GroupID                    thisGroupID;
  private final List<ActiveServerListener> listeners = new CopyOnWriteArrayList<>();
  private final IDMapping                  idMapping = new IDMapping();
  private final JoinManager                joinManager;

  public ActiveServerIDManagerImpl(StateManager stateManager, GroupManager groupManager, GroupID thisGroupID,
                                   NodesStore nodesStore) {
    this.stateManager = stateManager;
    this.groupManager = groupManager;
    this.thisGroupID = thisGroupID;

    this.stateManager.registerForStateChangeEvents(this);
    this.groupManager.registerForMessages(ActiveJoinMessage.class, this);
    this.groupManager.registerForGroupEvents(this);
    listeners.add(new OperatorEventsActiveServerConnectionListener(nodesStore));

    joinManager = new JoinManager(this.groupManager);
  }

  @Override
  public void addActiveServerListener(ActiveServerListener listener) {
    this.listeners.add(listener);
  }

  @Override
  public void removeActiveServerListener(ActiveServerListener listener) {
    this.listeners.remove(listener);
  }

  @Override
  public ServerID getActiveServerIDFor(GroupID groupID) {
    ServerID sid = this.idMapping.get(groupID);
    if (sid == null) {
      logger.warn("Non-exist serverID mapping for " + groupID);
    }
    return sid;
  }

  @Override
  public Set<ServerID> getAllActiveServerIDs() {
    return this.idMapping.getAllActiveServerIDs();
  }

  @Override
  public void l2StateChanged(StateChangedEvent sce) {
    if (sce.movedToActive()) {
      ServerID sid = (ServerID) this.groupManager.getLocalNodeID();
      activeJoined(this.thisGroupID, sid);
      // broadcast new joined active to the world
      GroupMessage msg = ActiveJoinMessage.createActiveJoinMessage(this.thisGroupID, sid);
      this.groupManager.sendAll(msg);
    } else if (sce.getOldState() == StateManager.ACTIVE_COORDINATOR) {
      // This is currently not possible as active servers never move to passive standby, they can die and someone else
      // can become active. In future that might change.
      activeLeft(this.thisGroupID);
      // broadcast active left to the world
      GroupMessage msg = ActiveJoinMessage.createActiveLeftMessage(this.thisGroupID);
      this.groupManager.sendAll(msg);
    }
  }

  @Override
  public void messageReceived(NodeID fromNode, GroupMessage msg) {
    ActiveJoinMessage message = (ActiveJoinMessage) msg;
    switch (message.getType()) {
      case ActiveJoinMessage.ACTIVE_JOIN:
        activeJoined(message.getGroupID(), message.getServerID());
        break;
      case ActiveJoinMessage.ACTIVE_LEFT:
        activeLeft(message.getGroupID());
        break;
      case ActiveJoinMessage.ACTIVE_REQUEST_JOIN:
        activeRequestJoined(message.getGroupID(), message.getServerID());
        break;
      default:
        throw new RuntimeException("Invalid ActiveJoinMessage type " + message.getType());
    }
  }

  private void activeRequestJoined(GroupID groupID, ServerID serverID) {
    if (this.stateManager.isActiveCoordinator() && this.thisGroupID.equals(groupID)) {
      logger.info("Receive request-join message for this group " + groupID + " from " + serverID);
      GroupMessage msg = ActiveJoinMessage
          .createActiveJoinMessage(this.thisGroupID, (ServerID) this.groupManager.getLocalNodeID());
      try {
        this.groupManager.sendTo(serverID, msg);
      } catch (GroupException e) {
        logger.error("Error sending ActiveJoin Message to : " + serverID);
      }
    }
  }

  private void activeJoined(GroupID groupID, ServerID serverID) {
    synchronized (this) {
      ServerID oldServerID = this.idMapping.get(groupID);

      // NOP if joined already
      if (serverID.equals(oldServerID)) { return; }

      if (this.groupManager.getLocalNodeID().equals(oldServerID)) {
        // Local Node is already active for this group, don't allow any change in the mapping.
        logger.warn("Ignoring Active Joined message from " + groupID + ":" + serverID
                    + " since LOCAL node  is active : local Node " + oldServerID);
        return;
      }

      // Ignore activeJoin if old one is still alive
      if (oldServerID != null && this.groupManager.isNodeConnected(oldServerID)) {
        logger.warn("Ignoring Active Joined message from " + groupID + ":" + serverID + " having active node "
                    + oldServerID);
        if (!groupID.equals(this.thisGroupID)) {
          joinManager.ignoreJoinAndBlackListServer(oldServerID, groupID, serverID);
        }
        return;
      }

      // update local mapping, but before that notify remote server if local node is active
      if (!this.thisGroupID.equals(groupID) && this.stateManager.isActiveCoordinator()) {
        // We are potentially sending more than one active join event (most cases two) shouldn't hurt though. We are
        // doing it to make sure that nodes that are active knows about all other active servers first. (to avoid some
        // weirdness in split brain cases).
        // FIXME:: The is an assumption that this message is processed before any other message from this node is
        // processed in the other node (serverID). Make this explicit.
        sendActiveJoinMessage(serverID);
      }
      this.idMapping.put(groupID, serverID);
      this.joinManager.delist(serverID);
      logger.info("Active server " + serverID + " joined group : " + groupID + " Old Active Server : " + oldServerID);
    }
    fireActiveServerJoined(groupID, serverID);
  }

  @Override
  public boolean isBlackListedServer(NodeID serverID) {
    return this.joinManager.isBlackListedServer(serverID);
  }

  private void fireActiveServerJoined(GroupID groupID, ServerID serverID) {
    for (ActiveServerListener listener : this.listeners) {
      listener.activeServerJoined(groupID, serverID);
    }
  }

  private void activeLeft(GroupID groupID) {
    // remove from mapping
    ServerID serverID = this.idMapping.remove(groupID);
    logger.info("Active server " + serverID + " left group : " + groupID);
    fireActiveServerLeft(groupID, serverID);
  }

  private void fireActiveServerLeft(GroupID groupID, ServerID serverID) {
    for (ActiveServerListener listener : this.listeners) {
      listener.activeServerLeft(groupID, serverID);
    }
  }

  @Override
  public void nodeJoined(NodeID nodeID) {
    if (this.stateManager.isActiveCoordinator()) {
      sendActiveJoinMessage(nodeID);
    }
  }

  private void sendActiveJoinMessage(NodeID nodeID) {
    try {
      GroupMessage msg = ActiveJoinMessage
          .createActiveJoinMessage(this.thisGroupID, (ServerID) this.groupManager.getLocalNodeID());
      this.groupManager.sendTo(nodeID, msg);
    } catch (GroupException e) {
      logger.error("Error sending ActiveJoin Message to : " + nodeID);
      // XXX::Probably not a good idea to zap the node until we fix zap node processor to work across active passive
      // and active active
      // groupManager.zapNode(nodeID, type, reason);
    }

  }

  @Override
  public void nodeLeft(NodeID nodeID) {
    boolean isLeftFromSameGroup = false;

    Entry<GroupID, ServerID> e = this.idMapping.getEntry(nodeID);
    if (e != null) {
      if (e.getValue().equals(nodeID)) {
        if (this.thisGroupID.equals(e.getKey())) {
          isLeftFromSameGroup = true;
        }
        // Active server left
        activeLeft(e.getKey());
      }
    }

    this.joinManager.nodeLeft(nodeID);

    // broadcast active to the world for any node left from same group, to restore mapping in case of split brain
    // scenario
    if (isLeftFromSameGroup && this.stateManager.isActiveCoordinator()) {
      GroupMessage msg = ActiveJoinMessage
          .createActiveJoinMessage(this.thisGroupID, (ServerID) this.groupManager.getLocalNodeID());
      this.groupManager.sendAll(msg);
    }
  }

  @Override
  public GroupID getLocalGroupID() {
    return this.thisGroupID;
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.visit(this.idMapping);
    out.visit(this.joinManager);
    return out;
  }

  private static class IDMapping implements PrettyPrintable {
    private final Map<GroupID, ServerID> groupIDToServerIDMap = new HashMap<>();

    public synchronized ServerID get(GroupID groupID) {
      return this.groupIDToServerIDMap.get(groupID);
    }

    public synchronized void put(GroupID groupID, ServerID serverID) {
      this.groupIDToServerIDMap.put(groupID, serverID);
    }

    public synchronized ServerID remove(GroupID groupID) {
      return this.groupIDToServerIDMap.remove(groupID);
    }

    public synchronized Entry<GroupID, ServerID> getEntry(NodeID nodeID) {
      for (Entry<GroupID, ServerID> e : this.groupIDToServerIDMap.entrySet()) {
        if (e.getValue().equals(nodeID)) { return e; }
      }
      return null;
    }

    public synchronized Set<ServerID> getAllActiveServerIDs() {
      Set<ServerID> s = new HashSet<>();
      for (ServerID serverID : this.groupIDToServerIDMap.values()) {
        s.add(serverID);
      }
      return s;
    }

    @Override
    public synchronized PrettyPrinter prettyPrint(PrettyPrinter out) {
      StringBuilder strBuffer = new StringBuilder();
      strBuffer.append(IDMapping.class.getSimpleName()).append(" [ ");
      strBuffer.append("groupIDToServerIDMap.IDMapping: {");
      for (Entry<GroupID, ServerID> entry : this.groupIDToServerIDMap.entrySet()) {
    strBuffer.append(entry.getKey()).append(" -> ").append(entry.getValue()).append("  ");
   }
      strBuffer.append("} ]");
      out.indent().print(strBuffer.toString()).flush();
      return out;
    }

  }

  private static class JoinManager implements PrettyPrintable {
    private final GroupManager           groupManager;
    private final Map<ServerID, GroupID> ignoredJoins       = new HashMap<>();
    private final Set<ServerID>          blackListedServers = new HashSet<>();

    JoinManager(GroupManager groupManager) {
      this.groupManager = groupManager;
    }

    public synchronized void ignoreJoinAndBlackListServer(ServerID existingServer, GroupID groupID,
                                                          ServerID ignoredServer) {
      this.ignoredJoins.put(existingServer, groupID);
      this.blackListedServers.add(ignoredServer);
    }

    public synchronized void nodeLeft(NodeID nodeID) {
      this.blackListedServers.remove(nodeID);

      // The nodeLeft comes after message broadcast, previous join was ignored
      // ask group to join again
      // DEV-4092: Split-brain scenario, the real active was blacklisted and low-water-mark messages were dropped. By
      // disconnecting connection, make active to re-send messages after re-join.
      if (this.ignoredJoins.containsKey(nodeID)) {
        logger.info("Active " + nodeID + " left from " + this.ignoredJoins.get(nodeID)
                    + " , ask ignored nodes to join again by disconnecting all blacklisted servers."
                    + blackListedServers);
        this.ignoredJoins.clear();
        for (ServerID sid : Sets.newHashSet(blackListedServers)) {
          this.groupManager.closeMember(sid);
        }
        this.blackListedServers.clear();
      }
    }

    public synchronized void delist(ServerID serverID) {
      this.blackListedServers.remove(serverID);
    }

    public synchronized boolean isBlackListedServer(NodeID serverID) {
      return this.blackListedServers.contains(serverID);
    }

    @Override
    public synchronized PrettyPrinter prettyPrint(PrettyPrinter out) {
      StringBuilder strBuffer = new StringBuilder();
      strBuffer.append(JoinManager.class.getSimpleName()).append(" [ ");
      strBuffer.append("groupIDToServerIDMap.JoinManager: {");

      strBuffer.append("ignoredJoins: {");
      for (Entry<ServerID, GroupID> entry : this.ignoredJoins.entrySet()) {
    strBuffer.append(entry.getKey()).append(" -> ").append(entry.getValue()).append("  ");
   }
      strBuffer.append("}\n\t");

      strBuffer.append("blackListedServers: {").append(this.blackListedServers).append("} ]");
      out.indent().print(strBuffer.toString()).flush();
      return out;
    }

    // for test purpose only
    synchronized int getIgnoredJoinsSize() {
      return this.ignoredJoins.size();
    }

    // for test purpose only
    synchronized int getBlackListedServersSize() {
      return this.blackListedServers.size();
    }

  }

  // for test purpose only
  int getIgnoredJoinsSize() {
    return this.joinManager.getIgnoredJoinsSize();
  }

  // for test purpose only
  int getBlackListedServersSize() {
    return this.joinManager.getBlackListedServersSize();
  }
}