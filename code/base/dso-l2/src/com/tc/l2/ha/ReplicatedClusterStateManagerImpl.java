/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.ha;

import com.tc.async.api.Sink;
import com.tc.l2.api.ReplicatedClusterStateManager;
import com.tc.l2.msg.ClusterStateMessage;
import com.tc.l2.msg.ClusterStateMessageFactory;
import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupMessage;
import com.tc.net.groups.GroupMessageListener;
import com.tc.net.groups.GroupResponse;
import com.tc.net.groups.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.net.protocol.transport.ConnectionIDFactoryListener;
import com.tc.objectserver.context.ChannelStateEventContext;
import com.tc.util.Assert;
import com.tc.util.UUID;

import java.util.Iterator;

public class ReplicatedClusterStateManagerImpl implements ReplicatedClusterStateManager, GroupMessageListener,
    ConnectionIDFactoryListener {

  private static final TCLogger logger = TCLogging.getLogger(ReplicatedClusterStateManagerImpl.class);

  private final GroupManager    groupManager;
  private final ClusterState    state;
  private final StateManager    stateManager;
  private final Sink            channelLifeCycleSink;

  public ReplicatedClusterStateManagerImpl(GroupManager groupManager, StateManager stateManager,
                                           ClusterState clusterState, ConnectionIDFactory factory,
                                           Sink channelLifeCycleSink) {
    this.groupManager = groupManager;
    this.stateManager = stateManager;
    state = clusterState;
    this.channelLifeCycleSink = channelLifeCycleSink;
    groupManager.registerForMessages(ClusterStateMessage.class, this);
    factory.registerForConnectionIDEvents(this);
  }

  public synchronized void goActiveAndSyncState() {
    generateClusterIDIfNeeded();

    // Sync state to internal DB
    state.syncInternal();

    // Sync state to external passive servers
    publishToAll(ClusterStateMessageFactory.createClusterStateMessage(state));
  }

  private void generateClusterIDIfNeeded() {
    if (state.getClusterID() == null) {
      // This is the first time an L2 goes active in the cluster of L2s. Generate a new clusterID. this will stick.
      state.setClusterID(UUID.getUUID().toString());
    }
  }

  public synchronized void publishClusterState(NodeID nodeID) throws GroupException {
    ClusterStateMessage msg = (ClusterStateMessage) groupManager
        .sendToAndWaitForResponse(nodeID, ClusterStateMessageFactory.createClusterStateMessage(state));
    validateResponse(nodeID, msg);
  }

  private void validateResponse(NodeID nodeID, ClusterStateMessage msg) {
    if (msg == null || msg.getType() != ClusterStateMessage.OPERATION_SUCCESS) {
      logger.error("Recd wrong response from : " + nodeID + " : msg = " + msg
                   + " while publishing Next Available ObjectID: Killing the node");
      groupManager.zapNode(nodeID);
    }
  }

  // TODO:: Sync only once a while to the passives
  public synchronized void publishNextAvailableObjectID(long minID) {
    state.setNextAvailableObjectID(minID);
    publishToAll(ClusterStateMessageFactory.createNextAvailableObjectIDMessage(state));
  }

  // TODO:: Sync only once a while to the passives
  public void publishNextAvailableGlobalTransactionID(long minID) {
    state.setNextAvailableGlobalTransactionID(minID);
    publishToAll(ClusterStateMessageFactory.createNextAvailableGlobalTransactionIDMessage(state));
  }

  public synchronized void connectionIDCreated(ConnectionID connectionID) {
    Assert.assertTrue(stateManager.isActiveCoordinator());
    state.addNewConnection(connectionID);
    publishToAll(ClusterStateMessageFactory.createNewConnectionCreatedMessage(connectionID));
  }

  public synchronized void connectionIDDestroyed(ConnectionID connectionID) {
    Assert.assertTrue(stateManager.isActiveCoordinator());
    state.removeConnection(connectionID);
    publishToAll(ClusterStateMessageFactory.createConnectionDestroyedMessage(connectionID));
  }

  private void publishToAll(GroupMessage message) {
    try {
      GroupResponse gr = groupManager.sendAllAndWaitForResponse(message);
      for (Iterator i = gr.getResponses().iterator(); i.hasNext();) {
        ClusterStateMessage msg = (ClusterStateMessage) i.next();
        validateResponse(msg.messageFrom(), msg);
      }
    } catch (GroupException e) {
      // TODO:: Is this extreme ?
      throw new AssertionError(e);
    }
  }

  public void messageReceived(NodeID fromNode, GroupMessage msg) {
    if (msg instanceof ClusterStateMessage) {
      ClusterStateMessage clusterMsg = (ClusterStateMessage) msg;
      handleClusterStateMessage(fromNode, clusterMsg);
    } else {
      throw new AssertionError("ReplicatedClusterStateManagerImpl : Received wrong message type :"
                               + msg.getClass().getName() + " : " + msg);
    }
  }

  private void handleClusterStateMessage(NodeID fromNode, ClusterStateMessage msg) {
    if (stateManager.isActiveCoordinator()) {
      logger.warn("Recd ClusterStateMessage from " + fromNode
                  + " while I am the cluster co-ordinator. This is bad. Ignoring the message");
      return;
    }
    msg.initState(state);
    sendChannelLifeCycleEventsIfNecessary(msg);
    sendOKResponse(fromNode, msg);
  }

  private void sendChannelLifeCycleEventsIfNecessary(ClusterStateMessage msg) {
    if (msg.getType() == ClusterStateMessage.NEW_CONNECTION_CREATED) {
      // Not really needed, but just in case
      channelLifeCycleSink.add(new ChannelStateEventContext(ChannelStateEventContext.CREATE, new ChannelID(msg
          .getConnectionID().getChannelID())));
    } else if (msg.getType() == ClusterStateMessage.CONNECTION_DESTROYED) {
      // this is needed to clean up some data structures internally
      channelLifeCycleSink.add(new ChannelStateEventContext(ChannelStateEventContext.REMOVE, new ChannelID(msg
          .getConnectionID().getChannelID())));
    }
  }

  private void sendOKResponse(NodeID fromNode, ClusterStateMessage msg) {
    try {
      groupManager.sendTo(fromNode, ClusterStateMessageFactory.createOKResponse(msg));
    } catch (GroupException e) {
      logger.error("Error handling message : " + msg, e);
    }
  }
}
