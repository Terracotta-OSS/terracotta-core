/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.ha;

import com.tc.async.api.Sink;
import com.tc.exception.TCRuntimeException;
import com.tc.l2.api.ReplicatedClusterStateManager;
import com.tc.l2.msg.ClusterStateMessage;
import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupMessageListener;
import com.tc.net.groups.GroupResponse;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.net.protocol.transport.ConnectionIDFactoryListener;
import com.tc.objectserver.context.NodeStateEventContext;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.State;

public class ReplicatedClusterStateManagerImpl implements ReplicatedClusterStateManager, GroupMessageListener<ClusterStateMessage>,
    ConnectionIDFactoryListener, PrettyPrintable {

  private static final TCLogger logger   = TCLogging.getLogger(ReplicatedClusterStateManagerImpl.class);

  private final GroupManager<AbstractGroupMessage>    groupManager;
  private final ClusterState    state;
  private final StateManager    stateManager;
  private final Sink<NodeStateEventContext>            channelLifeCycleSink;

  private boolean               isActive = false;

  public ReplicatedClusterStateManagerImpl(GroupManager<AbstractGroupMessage> groupManager, StateManager stateManager,
                                           ClusterState clusterState, ConnectionIDFactory factory,
                                           Sink<NodeStateEventContext> channelLifeCycleSink) {
    this.groupManager = groupManager;
    this.stateManager = stateManager;
    this.state = clusterState;
    this.channelLifeCycleSink = channelLifeCycleSink;
    groupManager.registerForMessages(ClusterStateMessage.class, this);
    factory.registerForConnectionIDEvents(this);
  }

  @Override
  public synchronized void goActiveAndSyncState() {
    state.generateStripeIDIfNeeded();
    state.syncActiveState();

    // Sync state to external passive servers
    publishToAll(ClusterStateMessage.createClusterStateMessage(state));

    isActive = true;
    notifyAll();
  }

  @Override
  public synchronized void publishClusterState(NodeID nodeID) throws GroupException {
    waitUntilActive();
    ClusterStateMessage msg = (ClusterStateMessage) groupManager
        .sendToAndWaitForResponse(nodeID, ClusterStateMessage.createClusterStateMessage(state));
    validateResponse(nodeID, msg);
  }

  private void waitUntilActive() {
    while (!isActive) {
      logger.info("Waiting since ReplicatedClusterStateManager hasn't gone ACTIVE yet ...");
      try {
        wait(3000);
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
    }
  }

  private void validateResponse(NodeID nodeID, ClusterStateMessage msg) {
    if (msg == null || msg.getType() != ClusterStateMessage.OPERATION_SUCCESS) {
      logger.error("Recd wrong response from : " + nodeID + " : msg = " + msg
                   + " while publishing Cluster State: Killing the node");
      groupManager
          .zapNode(nodeID,
                   (msg != null && msg.getType() == ClusterStateMessage.OPERATION_FAILED_SPLIT_BRAIN ? L2HAZapNodeRequestProcessor.SPLIT_BRAIN
                       : L2HAZapNodeRequestProcessor.PROGRAM_ERROR),
                   "Recd wrong response from : " + nodeID + " while publishing Cluster State"
                       + L2HAZapNodeRequestProcessor.getErrorString(new Throwable()));
    }
  }

  @Override
  public synchronized void connectionIDCreated(ConnectionID connectionID) {
    Assert.assertTrue(stateManager.isActiveCoordinator());
    state.addNewConnection(connectionID);
    publishToAll(ClusterStateMessage.createNewConnectionCreatedMessage(connectionID));
  }

  @Override
  public synchronized void connectionIDDestroyed(ConnectionID connectionID) {
    Assert.assertTrue(stateManager.isActiveCoordinator());
    state.removeConnection(connectionID);
    publishToAll(ClusterStateMessage.createConnectionDestroyedMessage(connectionID));
  }

  private void publishToAll(AbstractGroupMessage message) {
    try {
      GroupResponse<AbstractGroupMessage> gr = groupManager.sendAllAndWaitForResponse(message);
      for (AbstractGroupMessage resp : gr.getResponses()) {
        ClusterStateMessage msg = (ClusterStateMessage) resp;
        validateResponse(msg.messageFrom(), msg);
      }
    } catch (GroupException e) {
      // TODO:: Is this extreme ?
      throw new AssertionError(e);
    }
  }

  @Override
  public void messageReceived(NodeID fromNode, ClusterStateMessage msg) {
    handleClusterStateMessage(fromNode, msg);
  }

  private void handleClusterStateMessage(NodeID fromNode, ClusterStateMessage msg) {
    if (stateManager.isActiveCoordinator()) {
      logger.warn("Recd ClusterStateMessage from " + fromNode
                  + " while I am the cluster co-ordinator. This is bad. Sending NG response. ");
      sendNGSplitBrainResponse(fromNode, msg);
      groupManager.zapNode(fromNode, L2HAZapNodeRequestProcessor.SPLIT_BRAIN,
                           "Recd ClusterStateMessage from : " + fromNode + " while in ACTIVE-COORDINATOR state"
                               + L2HAZapNodeRequestProcessor.getErrorString(new Throwable()));
    } else {
      // XXX:: Is it a good idea to check if the message we are receiving is from the active server that we think is
      // active ? There is a race between publishing active and pushing cluster state and hence we don't do the check.
      // May be someday these two messages will merge into one.
      if (msg.isSplitBrainMessage()) {
        return; // About to get zapped no need to actually do anything with the split brain message.
      }
      msg.initState(state);
      state.syncSequenceState();
      sendChannelLifeCycleEventsIfNecessary(msg);
      sendOKResponse(fromNode, msg);
    }
  }

  private void sendChannelLifeCycleEventsIfNecessary(ClusterStateMessage msg) {
    if (msg.getType() == ClusterStateMessage.NEW_CONNECTION_CREATED) {
      // Not really needed, but just in case
      NodeID nodeID = new ClientID(msg.getConnectionID().getChannelID());
      channelLifeCycleSink.addMultiThreaded(new NodeStateEventContext(NodeStateEventContext.CREATE, nodeID, msg.getConnectionID().getProductId()));
    } else if (msg.getType() == ClusterStateMessage.CONNECTION_DESTROYED) {
      // this is needed to clean up some data structures internally
      // NOTE :: It is ok to add this event context directly to the channel life cycle handler (and not wrap around a
      // InBandMoveToNextSink like in active) because there are no stages before the transactions are added to
      // server transaction manager.
      // XXX::FIXME:: The above statement is true only when this event is fixed to be fired from active after all txns
      // are acked in the active.
      NodeID nodeID = new ClientID(msg.getConnectionID().getChannelID());
      channelLifeCycleSink.addMultiThreaded(new NodeStateEventContext(NodeStateEventContext.REMOVE, nodeID, msg.getConnectionID().getProductId()));
    }
  }

  private void sendOKResponse(NodeID fromNode, ClusterStateMessage msg) {
    try {
      groupManager.sendTo(fromNode, ClusterStateMessage.createOKResponse(msg));
    } catch (GroupException e) {
      logger.error("Error handling message : " + msg, e);
    }
  }

  private void sendNGSplitBrainResponse(NodeID fromNode, ClusterStateMessage msg) {
    try {
      groupManager.sendTo(fromNode, ClusterStateMessage.createNGSplitBrainResponse(msg));
    } catch (GroupException e) {
      logger.error("Error handling message : " + msg, e);
    }
  }

  @Override
  public void fireNodeLeftEvent(NodeID nodeID) {
    // this is needed to clean up some data structures internally
    channelLifeCycleSink.addMultiThreaded(new NodeStateEventContext(NodeStateEventContext.REMOVE, nodeID, null));
  }

  @Override
  public synchronized void setCurrentState(State currentState) {
    this.state.setCurrentState(currentState);
  }

  @Override
  public synchronized PrettyPrinter prettyPrint(PrettyPrinter out) {
    StringBuilder strBuilder = new StringBuilder();
    strBuilder.append(ReplicatedClusterStateManagerImpl.class.getSimpleName() + " [ ");
    strBuilder.append(this.state).append(" ").append(this.stateManager);
    strBuilder.append(" ]");
    out.indent().print(strBuilder.toString()).flush();
    return out;
  }
}