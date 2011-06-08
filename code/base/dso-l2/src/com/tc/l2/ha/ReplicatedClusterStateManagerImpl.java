/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.ha;

import com.tc.async.api.Sink;
import com.tc.exception.TCRuntimeException;
import com.tc.l2.api.ReplicatedClusterStateManager;
import com.tc.l2.msg.ClusterStateMessage;
import com.tc.l2.msg.ClusterStateMessageFactory;
import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupMessage;
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

import java.util.Iterator;

public class ReplicatedClusterStateManagerImpl implements ReplicatedClusterStateManager, GroupMessageListener,
    ConnectionIDFactoryListener, PrettyPrintable {

  private static final TCLogger logger   = TCLogging.getLogger(ReplicatedClusterStateManagerImpl.class);

  private final GroupManager    groupManager;
  private final ClusterState    state;
  private final StateManager    stateManager;
  private final Sink            channelLifeCycleSink;

  private boolean               isActive = false;

  public ReplicatedClusterStateManagerImpl(GroupManager groupManager, StateManager stateManager,
                                           ClusterState clusterState, ConnectionIDFactory factory,
                                           Sink channelLifeCycleSink) {
    this.groupManager = groupManager;
    this.stateManager = stateManager;
    this.state = clusterState;
    this.channelLifeCycleSink = channelLifeCycleSink;
    groupManager.registerForMessages(ClusterStateMessage.class, this);
    factory.registerForConnectionIDEvents(this);
  }

  public synchronized void goActiveAndSyncState() {
    state.generateStripeIDIfNeeded();
    state.syncActiveState();

    // Sync state to external passive servers
    publishToAll(ClusterStateMessageFactory.createClusterStateMessage(state));

    isActive = true;
    notifyAll();
  }

  public synchronized void publishClusterState(NodeID nodeID) throws GroupException {
    waitUntilActive();
    ClusterStateMessage msg = (ClusterStateMessage) groupManager
        .sendToAndWaitForResponse(nodeID, ClusterStateMessageFactory.createClusterStateMessage(state));
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

  // TODO:: Sync only once a while to the passives
  public synchronized void publishNextAvailableObjectID(long minID) {
    state.setNextAvailableObjectID(minID);
    publishToAll(ClusterStateMessageFactory.createNextAvailableObjectIDMessage(state));
  }

  public synchronized void publishNextAvailableDGCID(long nextGcIteration) {
    state.setNextAvailableDGCId(nextGcIteration);
    publishToAll(ClusterStateMessageFactory.createNextAvailableDGCIterationMessage(state));
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
                  + " while I am the cluster co-ordinator. This is bad. Sending NG response. ");
      sendNGSplitBrainResponse(fromNode, msg);
      groupManager.zapNode(fromNode, L2HAZapNodeRequestProcessor.SPLIT_BRAIN,
                           "Recd ClusterStateMessage from : " + fromNode + " while in ACTIVE-COORDINATOR state"
                               + L2HAZapNodeRequestProcessor.getErrorString(new Throwable()));
    } else {
      // XXX:: Is it a good idea to check if the message we are receiving is from the active server that we think is
      // active ? There is a race between publishing active and pushing cluster state and hence we don't do the check.
      // May be someday these two messages will merge into one.
      msg.initState(state);
      state.syncSequenceState();
      sendChannelLifeCycleEventsIfNecessary(msg);
      sendOKResponse(fromNode, msg);
    }
  }

  private void sendChannelLifeCycleEventsIfNecessary(ClusterStateMessage msg) {
    if (msg.getType() == ClusterStateMessage.NEW_CONNECTION_CREATED) {
      // Not really needed, but just in case
      channelLifeCycleSink.add(new NodeStateEventContext(NodeStateEventContext.CREATE, new ClientID(msg
          .getConnectionID().getChannelID())));
    } else if (msg.getType() == ClusterStateMessage.CONNECTION_DESTROYED) {
      // this is needed to clean up some data structures internally
      // NOTE :: It is ok to add this event context directly to the channel life cycle handler (and not wrap around a
      // InBandMoveToNextSink like in active) because there are no stages before the transactions are added to
      // server transaction manager.
      // XXX::FIXME:: The above statement is true only when this event is fixed to be fired from active after all txns
      // are acked in the active.
      channelLifeCycleSink.add(new NodeStateEventContext(NodeStateEventContext.REMOVE, new ClientID(msg
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

  private void sendNGSplitBrainResponse(NodeID fromNode, ClusterStateMessage msg) {
    try {
      groupManager.sendTo(fromNode, ClusterStateMessageFactory.createNGSplitBrainResponse(msg));
    } catch (GroupException e) {
      logger.error("Error handling message : " + msg, e);
    }
  }

  public void fireNodeLeftEvent(NodeID nodeID) {
    // this is needed to clean up some data structures internally
    channelLifeCycleSink.add(new NodeStateEventContext(NodeStateEventContext.REMOVE, nodeID));
  }

  public synchronized void setCurrentState(State currentState) {
    this.state.setCurrentState(currentState);
  }

  public synchronized PrettyPrinter prettyPrint(PrettyPrinter out) {
    StringBuilder strBuilder = new StringBuilder();
    strBuilder.append(ReplicatedClusterStateManagerImpl.class.getSimpleName() + " [ ");
    strBuilder.append(this.state).append(" ").append(this.stateManager);
    strBuilder.append(" ]");
    out.indent().print(strBuilder.toString()).flush();
    return out;
  }
}
