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
import com.tc.config.ClusterInfo;
import com.tc.config.HaConfig;
import com.tc.config.NodesStore;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.state.StateChangeListener;
import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.StripeID;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/*
 * Delay nodeJoined event until StripeID received from peer. Send local StripeID to connected nodes when created after
 * active elected.
 */
public class StripeTCGroupManagerImpl implements GroupManager<AbstractGroupMessage>, GroupEventsListener, StripeIDEventListener,
    StateChangeListener {
  private static final TCLogger                           logger                  = TCLogging
                                                                                      .getLogger(StripeTCGroupManagerImpl.class);

  private final StripeIDMismatchNotificationProcessor     stripeIDMismatchProcessor;
  private final StripeIDStateManager                      stripeIDStateManager;
  private final GroupManager<AbstractGroupMessage> groupManager;
  private final GroupID                                   thisGroupID;
  private final Map<ServerID, Member>                     members                 = new ConcurrentHashMap<>();
  private final Map<GroupID, Member>                      lastLeftActiveMap       = new ConcurrentHashMap<>();
  private final CopyOnWriteArrayList<GroupEventsListener> groupListeners          = new CopyOnWriteArrayList<>();
  private final Set<NodeID>                               pendingLocalStripeIDSet = new HashSet<>();
  private final QuarantineManager                         quarantineManager       = new QuarantineManager();
  private volatile boolean                                isActive                = false;
  private boolean                                         isLocalStripeIDReady    = false;

  private final ClusterInfo                               clusterInfo;

  public StripeTCGroupManagerImpl(GroupManager groupManager, HaConfig haConfig,
                                  StripeIDStateManager stripeIDStateManager,
                                  StripeIDMismatchNotificationProcessor stripeIDMismatchProcessor) {
    this.groupManager = groupManager;
    this.thisGroupID = haConfig.getThisGroupID();
    groupManager.registerForGroupEvents(this);
    this.stripeIDMismatchProcessor = stripeIDMismatchProcessor;
    this.stripeIDStateManager = stripeIDStateManager;
    this.stripeIDStateManager.registerForStripeIDEvents(this);
    this.clusterInfo = haConfig.getClusterInfo();

    groupManager.registerForMessages(StripeIDGroupMessage.class, new StripeIDMessageRouter());
    groupManager.registerForMessages(StripeIDMismatchGroupMessage.class, new StripeIDMismatchNotificationRouter());
  }

  @Override
  public void nodeJoined(NodeID nodeID) {

    if (!verifyClusterMember(nodeID)) { return; }

    // if pending for local StripeID
    synchronized (pendingLocalStripeIDSet) {
      if (!isLocalStripeIDReady) {
        pendingLocalStripeIDSet.add(nodeID);
        return;
      }
    }

    // send local StripeID
    sendStripeIDTo(nodeID, getLocalStripeID());

    // fire nodeJoin to listeners after StripeID received from peer
  }

  @Override
  public void nodeLeft(NodeID nodeID) {
    Member m = members.remove(nodeID);

    boolean isNodeHeldBack;
    synchronized (quarantineManager) {
      isNodeHeldBack = quarantineManager.remove(nodeID);
    }
    synchronized (pendingLocalStripeIDSet) {
      isNodeHeldBack = isNodeHeldBack || pendingLocalStripeIDSet.remove(nodeID);
    }

    // re-fire event only nodeJoin passed through earlier
    if (!isNodeHeldBack) {
      refireGroupEvent(nodeID, false);
      // remap if left one was joining as an active
      if (m != null && m.isActive()) {
        lastLeftActiveMap.put(m.getGroupID(), m);
        if (isRemapAllowed()) remapIfSplitBrainLoserLeft(nodeID, m);
      }
    }
  }

  /*
   * Receive StripeID from peer node
   */
  private void processPeerStripeID(NodeID fromNode, GroupID groupID, StripeID stripeID, boolean isActiveState,
                                   boolean overwrite) {

    if (this.isActive && isActiveState && this.thisGroupID.equals(groupID)) {
      logger.warn("Ignoring StripeID from another active in the same group " + groupID + " " + stripeID);
      return;
    }

    if (!verifyClusterMember(fromNode)) { return; }
    if (!verifyGroupID(fromNode, groupID)) { return; }

    members.put((ServerID) fromNode, new Member(groupID, stripeID, isActiveState));

    // if local node is not ready then temporarily quarantine incoming nodes (DEV-3965).
    synchronized (pendingLocalStripeIDSet) {
      if (!isLocalStripeIDReady) {
        quarantine(fromNode, groupID, stripeID, isActiveState, StripeIDMismatchGroupMessage.MISMATCH_NOT_READY_YET);
        return;
      }
    }

    // only save StripeID from active
    if (isActiveState) {
      if (!verifyOrSaveStripeID(groupID, stripeID, overwrite)) {
        boolean doQuarantine = true;

        // re-map if possible
        if (isRemapAllowed() && !overwrite && stripeIDProviderLeft(groupID)) {
          doQuarantine = !verifyOrSaveStripeID(groupID, stripeID, true);
        }

        if (doQuarantine) {
          quarantine(fromNode, groupID, stripeID, isActiveState, StripeIDMismatchGroupMessage.ERROR_MISMATCH_STRIPEID);
          return;
        }
      }
      // new stripeID saved, un-quarantine passive with same StripeID
      unQuarantine(groupID, stripeID);
    } else {
      // quarantine passive if StripeID.NULL or mismatch
      StripeID mine = stripeIDStateManager.getStripeID(groupID);
      if (!mine.equals(stripeID)) {
        int errType = StripeID.NULL_ID.equals(mine) ? StripeIDMismatchGroupMessage.MISMATCH_TEMPORARY
            : StripeIDMismatchGroupMessage.ERROR_MISMATCH_STRIPEID;
        quarantine(fromNode, groupID, stripeID, isActiveState, errType);
        return;
      }
    }

    refireGroupEvent(fromNode, true);
  }

  private boolean isClusterMember(ServerID serverID) {
    return clusterInfo.hasServerInCluster(serverID.getName());
  }

  /*
   * Allow remap if no txn yet.
   */
  private boolean isRemapAllowed() {
    return false;
  }

  private boolean stripeIDProviderLeft(GroupID groupID) {
    // if last left active has current StripeID
    Member m = lastLeftActiveMap.get(groupID);
    return (m != null && m.getStripeID().equals(stripeIDStateManager.getStripeID(groupID)));
  }

  // if there is a quarantined node from same group and joined as active
  // That means split-brain and loser killed
  private void remapIfSplitBrainLoserLeft(NodeID nodeID, Member m) {
    GroupID groupID = m.getGroupID();
    if (!GroupID.NULL_ID.equals(groupID) && !StripeID.NULL_ID.equals(m.getStripeID())) {
      boolean doRemap = false;
      NodeID quarantinedNodeID = null;
      StripeID stripeID = null;
      synchronized (quarantineManager) {
        // locate one joins as active with
        Set<NodeID> idSet = quarantineManager.getNodeIDSet(groupID);
        for (NodeID id : idSet) {
          if (quarantineManager.isActive(id)) {
            stripeID = quarantineManager.getStripeID(id);
            if (stripeID != null && !StripeID.NULL_ID.equals(stripeID)) {
              quarantinedNodeID = id;
              quarantineManager.remove(quarantinedNodeID);
              doRemap = true;
              break;
            }
          }
        }
      }
      if (doRemap) {
        if (stripeIDStateManager.verifyOrSaveStripeID(groupID, stripeID, true)) {
          refireGroupEvent(quarantinedNodeID, true);
          // recover those with same stripeID
          unQuarantine(groupID, stripeID);
        }
      }
    }
  }

  private void quarantine(NodeID nodeID, GroupID groupID, StripeID stripeID, boolean isActiveState, int errorType) {
    String mesg;
    switch (errorType) {
      case StripeIDMismatchGroupMessage.MISMATCH_TEMPORARY:
        mesg = "Passive joined with " + stripeID + ". Waiting for active to join to confirm StripeID, quarantine "
               + nodeID;
        logger.info(mesg);
        break;
      case StripeIDMismatchGroupMessage.MISMATCH_NOT_READY_YET:
        mesg = "Local node is not ready for " + stripeID + ", quarantine " + nodeID;
        logger.info(mesg);
        break;
      default:
        mesg = "Mismatch StripeID " + stripeID + ", quarantine " + nodeID;
        logger.warn(mesg);
        break;
    }
    stripeIDMismatchError(nodeID, errorType, mesg, true);
    quarantineManager.add(nodeID, groupID, stripeID, isActiveState);
  }

  private void unQuarantine(GroupID groupID, StripeID stripeID) {
    Set<NodeID> resurrectSet = quarantineManager.unQuarantine(groupID, stripeID);
    for (NodeID nodeID : resurrectSet) {
      refireGroupEvent(nodeID, true);
    }
  }

  private boolean isQuarantinedNode(NodeID nodeID, GroupMessage msg) {
    synchronized (quarantineManager) {
      if (quarantineManager.quarantinedSet().contains(nodeID)) {
        logger.warn("Drop message to quarantined node " + nodeID + " " + msg);
        return true;
      } else {
        return false;
      }
    }
  }

  private Set<? extends NodeID> filterOut(Set<? extends NodeID> orig, Set<? extends NodeID> set, GroupMessage msg) {
    if (set.size() == 0) {
      return orig;
    } else {
      Set<NodeID> newSet = new HashSet<>(orig);
      for (NodeID nid : set) {
        if (newSet.contains(nid)) {
          newSet.remove(nid);
          logger.warn("Drop message to quarantined node " + nid + " " + msg);
        }
      }
      return newSet;
    }
  }

  /*
   * Act as a filter to drop messages from quarantined nodes.
   */
  private class messageFilter implements GroupMessageListener {
    private final GroupMessageListener listener;

    private messageFilter(GroupMessageListener listener) {
      this.listener = listener;
    }

    @Override
    public void messageReceived(NodeID fromNode, GroupMessage msg) {
      if (quarantineManager.contains(fromNode)) {
        logger.warn("Drop message from quarantined node " + fromNode + " " + msg);
      } else {
        listener.messageReceived(fromNode, msg);
      }
    }
  }

  /*
   * StripeID mismatch notifier
   */
  private void stripeIDMismatchError(NodeID nodeID, int type, String reason, boolean skipLog) {
    if (!stripeIDMismatchProcessor.acceptOutgoingStripeIDMismatchNotification(nodeID, type, reason)) {
      logger.warn("Ignoring stripeID mismatch error message since " + stripeIDMismatchProcessor + " asked us to : "
                  + nodeID + " type = " + type + " reason = " + reason);
    } else {
      if (!skipLog) {
        logger.warn("Ignoring node : " + nodeID + " type = " + type + " reason = " + reason);
      }
      AbstractGroupMessage msg = StripeIDMismatchGroupMessageFactory.createStripeIDMismatchGroupMessage(type, reason,
                                                                                                this.thisGroupID);
      try {
        sendTo(nodeID, msg);
      } catch (GroupException e) {
        logger.error("Error sending stripeID mismatch message to " + nodeID + " msg = " + msg);
      }
    }
  }

  private void stripeIDMismatchError(NodeID nodeID, int type, String reason) {
    stripeIDMismatchError(nodeID, type, reason, false);
  }

  private boolean verifyOrSaveStripeID(GroupID gid, StripeID stripeID, boolean isRemap) {
    Assert.assertNotNull("GroupID can not be null", gid);
    return stripeIDStateManager.verifyOrSaveStripeID(gid, stripeID, isRemap);
  }

  /*
   * Verify that member is a cluster member
   */
  private boolean verifyClusterMember(NodeID nodeID) {
    if (!isClusterMember((ServerID) nodeID)) {
      stripeIDMismatchError(nodeID, StripeIDMismatchGroupMessage.ERROR_NOT_CLUSTER_MEMBER,
                            "Ignoring non-cluster member " + nodeID);
      logger.warn("Not a cluster member, quarantine " + nodeID);
      quarantineManager.add(nodeID, GroupID.NULL_ID, StripeID.NULL_ID, false);
      return false;
    }
    return true;
  }

  /*
   * Verify received GroupID match Config
   */
  private boolean verifyGroupID(NodeID nodeID, GroupID groupID) {
    GroupID configGroupID = clusterInfo.getGroupIDFromNodeName((((ServerID) nodeID).getName()));
    if (!groupID.equals(configGroupID)) {
      stripeIDMismatchError(nodeID, StripeIDMismatchGroupMessage.ERROR_MISMATCH_GROUPID, "Ignoring mismatch GroupID "
                                                                                         + nodeID + " " + groupID);
      logger.warn("Mismatch GroupID, quarantine " + nodeID + " " + groupID);
      quarantineManager.add(nodeID, GroupID.NULL_ID, StripeID.NULL_ID, false);
      return false;
    } else {
      return true;
    }
  }

  private void sendStripeIDTo(NodeID nodeID, StripeID stripeID) {
    AbstractGroupMessage msg = StripeIDGroupMessageFactory.createStripeIDGroupMessage(thisGroupID, stripeID, isActive, false);
    logger.info("Send StripeID to " + nodeID + " " + msg);
    try {
      groupManager.sendTo(nodeID, msg);
    } catch (GroupException e) {
      logger.error("Error sending StripID message to " + nodeID + " " + e);
    }
  }

  private void sendStripeIDToAll(StripeID stripeID) {
    AbstractGroupMessage msg = StripeIDGroupMessageFactory.createStripeIDGroupMessage(thisGroupID, stripeID, isActive, false);
    for (ServerID sid : members.keySet()) {
      logger.info("Send StripeID to " + sid + " " + msg);
      try {
        groupManager.sendTo(sid, msg);
      } catch (GroupException e) {
        logger.error("Error sending StripID message to " + sid + " " + e);
      }
    }
  }

  private void refireGroupEvent(NodeID nodeID, boolean joined) {
    for (GroupEventsListener listener : groupListeners) {
      if (joined) {
        listener.nodeJoined(nodeID);
      } else {
        listener.nodeLeft(nodeID);
      }
    }
  }

  @Override
  public NodeID getLocalNodeID() {
    return groupManager.getLocalNodeID();
  }

  @Override
  public boolean isNodeConnected(NodeID sid) {
    return groupManager.isNodeConnected(sid);
  }

  @Override
  public NodeID join(Node thisNode, NodesStore nodesStore) throws GroupException {
    return groupManager.join(thisNode, nodesStore);
  }

  @Override
  public void registerForGroupEvents(GroupEventsListener listener) {
    groupListeners.add(listener);
  }

  @Override
  public <M extends AbstractGroupMessage> void registerForMessages(Class<? extends M> msgClass, GroupMessageListener<M> listener) {
    groupManager.registerForMessages(msgClass, new messageFilter(listener));
  }

  @Override
  public <N extends AbstractGroupMessage> void routeMessages(Class<? extends N> msgClass, Sink<N> sink) {
    groupManager.routeMessages(msgClass, sink);
  }

  @Override
  public void sendAll(AbstractGroupMessage msg) {
    sendAll(msg, members.keySet());
  }

  @Override
  public void sendAll(AbstractGroupMessage msg, Set<? extends NodeID> nodeIDs) {
    groupManager.sendAll(msg, filterOut(nodeIDs, quarantineManager.quarantinedSet(), msg));
  }

  @Override
  public GroupResponse sendAllAndWaitForResponse(AbstractGroupMessage msg) throws GroupException {
    return sendAllAndWaitForResponse(msg, members.keySet());
  }

  @Override
  public GroupResponse sendAllAndWaitForResponse(AbstractGroupMessage msg, Set<? extends NodeID>nodeIDs) throws GroupException {
    return groupManager.sendAllAndWaitForResponse(msg, filterOut(nodeIDs, quarantineManager.quarantinedSet(), msg));
  }

  @Override
  public void sendTo(NodeID nodeID, AbstractGroupMessage msg) throws GroupException {
    if (!isQuarantinedNode(nodeID, msg)) {
      groupManager.sendTo(nodeID, msg);
    }
  }

  @Override
  public AbstractGroupMessage sendToAndWaitForResponse(NodeID nodeID, AbstractGroupMessage msg) throws GroupException {
    return isQuarantinedNode(nodeID, msg) ? null : groupManager.sendToAndWaitForResponse(nodeID, msg);
  }

  @Override
  public void setZapNodeRequestProcessor(ZapNodeRequestProcessor processor) {
    groupManager.setZapNodeRequestProcessor(processor);
  }

  @Override
  public void zapNode(NodeID nodeID, int type, String reason) {
    groupManager.zapNode(nodeID, type, reason);
  }

  private StripeID getLocalStripeID() {
    return stripeIDStateManager.getStripeID(thisGroupID);
  }

  /*
   * Local StripeID which already saved to DB and then forward to connected L2s..
   */
  @Override
  public void notifyStripeIDCreated(StripeID stripeID) {
    synchronized (pendingLocalStripeIDSet) {
      isLocalStripeIDReady = true;
      for (NodeID nodeID : pendingLocalStripeIDSet) {
        sendStripeIDTo(nodeID, stripeID);
      }
      pendingLocalStripeIDSet.clear();
      if (isActive) {
        // DEV-3964: send StripeID to those local joins as a passive
        sendStripeIDToAll(stripeID);
      }
    }
    // DEV-3965: rescue nodes which were temporarily quarantined when local was not ready
    logger.info("Node is ready to evaluate quarantined nodes which were quarantined temporarily");
    Set<NodeID> resurrectSet = quarantineManager.unQuarantineAllMatched(stripeIDStateManager.getStripeIDMap(false));
    for (NodeID nodeID : resurrectSet) {
      refireGroupEvent(nodeID, true);
    }
  }

  @Override
  public void notifyStripeIDMapReady() {
    // Ignore
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    StringBuilder strBuilder = new StringBuilder();
    strBuilder.append(StripeTCGroupManagerImpl.class.getSimpleName()).append(" [ ");
    strBuilder.append("lastLeftActiveMap: {");
    for (Entry<GroupID, Member> entry : this.lastLeftActiveMap.entrySet()) {
      strBuilder.append(entry.getKey()).append(" -> ").append(entry.getValue()).append("  ");
    }
    strBuilder.append("}\n\t");

    strBuilder.append("members: {");
    for (Entry<ServerID, Member> entry : this.members.entrySet()) {
      strBuilder.append(entry.getKey()).append(" -> ").append(entry.getValue()).append("  ");
    }
    strBuilder.append("}\n\t");

    strBuilder.append("pendingLocalStripeIDSet: {");
    synchronized (this.pendingLocalStripeIDSet) {
      for (NodeID nodeID : this.pendingLocalStripeIDSet) {
        strBuilder.append(nodeID).append(" ");
      }
      strBuilder.append("} ]\n");
    }

    out.indent().print(strBuilder.toString()).flush();
    return out;
  }

  /*
   * StripeID message receiver
   */
  private final class StripeIDMessageRouter implements GroupMessageListener {

    @Override
    public void messageReceived(NodeID fromNode, GroupMessage msg) {
      StripeIDGroupMessage stripeIDMsg = (StripeIDGroupMessage) msg;
      logger.info("Receive StripeID from " + fromNode + " " + msg);
      processPeerStripeID(fromNode, stripeIDMsg.getGroupID(), stripeIDMsg.getStripeID(), stripeIDMsg.isActive(),
                          stripeIDMsg.isRemap());
    }
  }

  /*
   * StripeID mismatch message receiver
   */
  private final class StripeIDMismatchNotificationRouter implements GroupMessageListener {

    @Override
    public void messageReceived(NodeID fromNode, GroupMessage msg) {
      StripeIDMismatchGroupMessage errorMsg = (StripeIDMismatchGroupMessage) msg;
      stripeIDMismatchProcessor.incomingStripeIDMismatchNotification(fromNode, errorMsg.getErrorType(),
                                                                     errorMsg.getReason(), errorMsg.getGroupID());
    }
  }

  // for testing purpose only
  Set<NodeID> getQuarantinedNodeIDSet(GroupID groupID) {
    return quarantineManager.getNodeIDSet(groupID);
  }

  private class QuarantineManager {
    final private Map<NodeID, Member> membersMap = new HashMap<>();

    public synchronized void add(NodeID nodeID, GroupID groupID, StripeID stripeID, boolean isActiveState) {
      membersMap.put(nodeID, new Member(groupID, stripeID, isActiveState));
    }

    public synchronized boolean remove(NodeID nodeID) {
      return (membersMap.remove(nodeID) != null);
    }

    public synchronized GroupID getGroupID(NodeID nodeID) {
      Member m = membersMap.get(nodeID);
      return (m != null) ? m.getGroupID() : null;
    }

    public synchronized StripeID getStripeID(NodeID nodeID) {
      Member m = membersMap.get(nodeID);
      return (m != null) ? m.getStripeID() : null;
    }

    public synchronized boolean isActive(NodeID nodeID) {
      Member m = membersMap.get(nodeID);
      return (m != null) ? m.isActive() : false;
    }

    public synchronized boolean contains(NodeID nodeID) {
      return membersMap.containsKey(nodeID);
    }

    public synchronized Set<? extends NodeID> quarantinedSet() {
      return Collections.unmodifiableSet(membersMap.keySet());
    }

    public synchronized Set<NodeID> getNodeIDSet(GroupID groupID) {
      Set<NodeID> idSet = new HashSet<>();
      for (NodeID nodeID : membersMap.keySet()) {
        if (groupID.equals(membersMap.get(nodeID).getGroupID())) {
          idSet.add(nodeID);
        }
      }
      return idSet;
    }

    public synchronized Set<NodeID> unQuarantine(GroupID groupID, StripeID stripeID) {
      Set<NodeID> resurrectSet = new HashSet<>();
      for (Iterator<NodeID> i = membersMap.keySet().iterator(); i.hasNext();) {
        NodeID nodeID = i.next();
        if (groupID.equals(getGroupID(nodeID)) && stripeID.equals(getStripeID(nodeID))) {
          logger.info("Un-quarantine " + nodeID + " " + groupID);
          i.remove();
          resurrectSet.add(nodeID);
        }
      }
      return resurrectSet;
    }

    public synchronized Set<NodeID> unQuarantineAllMatched(Map<GroupID, StripeID> stripeIDMap) {
      Set<NodeID> resurrectSet = new HashSet<>();
      Map<GroupID, StripeID> map = new HashMap<>(stripeIDMap);

      // un-quarantine new active first, just in case both active and passive quarantined
      for (Iterator<NodeID> i = membersMap.keySet().iterator(); i.hasNext();) {
        NodeID nodeID = i.next();
        if (membersMap.get(nodeID).isActive()) {
          GroupID groupID = getGroupID(nodeID);
          StripeID stripeID = getStripeID(nodeID);
          if (!stripeID.isNull() && map.get(groupID).isNull()) {
            if (verifyOrSaveStripeID(groupID, stripeID, false)) {
              logger.info("Un-quarantine active " + nodeID + " " + groupID);
              i.remove();
              resurrectSet.add(nodeID);
              map.put(groupID, stripeID);
            }
          }
        }
      }

      for (Iterator<NodeID> i = membersMap.keySet().iterator(); i.hasNext();) {
        NodeID nodeID = i.next();
        GroupID groupID = getGroupID(nodeID);
        StripeID stripeID = getStripeID(nodeID);
        if (!stripeID.isNull() && map.get(groupID).equals(stripeID)) {
          logger.info("Un-quarantine " + nodeID + " " + groupID);
          i.remove();
          resurrectSet.add(nodeID);
        } else if (!stripeID.isNull() && map.get(groupID).isNull()) {
          if (verifyOrSaveStripeID(groupID, stripeID, false)) {
            logger.info("Un-quarantine " + nodeID + " " + groupID);
            i.remove();
            resurrectSet.add(nodeID);
            map.put(groupID, stripeID);
          }
        } else {
          logger.info("Still quarantine " + nodeID + " " + groupID + " " + stripeID);
        }
      }
      return resurrectSet;
    }
  }

  private static class Member {

    final private GroupID  groupID;
    final private StripeID stripeID;
    final private boolean  isActive;

    public Member(GroupID groupID, StripeID stripeID, boolean isActive) {
      this.groupID = groupID;
      this.stripeID = stripeID;
      this.isActive = isActive;
    }

    public GroupID getGroupID() {
      return this.groupID;
    }

    public StripeID getStripeID() {
      return this.stripeID;
    }

    public boolean isActive() {
      return this.isActive;
    }

    @Override
    public String toString() {
      return "Member [" + this.groupID + ", " + this.stripeID + "]";
    }
  }

  /*
   * Listen to StateManager to know active-passive state
   */
  @Override
  public void l2StateChanged(StateChangedEvent sce) {
    if (sce.movedToActive()) {
      isActive = true;
      if (isLocalStripeIDReady) {
        // DEV-3964: passive joins before active, got temporarily quarantined.
        // Re-send StripeID if passive promoted to active
        logger.info("Moved to active, send StripeID to all");
        sendStripeIDToAll(getLocalStripeID());
      }
    } else if (sce.getOldState() == StateManager.ACTIVE_COORDINATOR) {
      isActive = false;
    }
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
