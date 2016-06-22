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
package com.tc.l2.state;

import com.tc.async.api.Sink;
import com.tc.async.api.StageManager;
import com.tc.exception.ZapDirtyDbServerNodeException;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.ha.L2HAZapNodeRequestProcessor;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.l2.msg.L2StateMessage;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.TSAManagementEventPayload;
import com.tc.management.TerracottaRemoteManagement;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.persistence.ClusterStatePersistor;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.operatorevent.TerracottaOperatorEventLogger;
import com.tc.operatorevent.TerracottaOperatorEventLogging;
import com.tc.util.Assert;
import com.tc.util.State;

import java.util.concurrent.CopyOnWriteArrayList;

public class StateManagerImpl implements StateManager {

  private static final TCLogger        logger              = TCLogging.getLogger(StateManagerImpl.class);

  private final TCLogger               consoleLogger;
  private final GroupManager<AbstractGroupMessage> groupManager;
  private final ElectionManagerImpl        electionMgr;
  private final Sink<StateChangedEvent> stateChangeSink;
  private final Sink<ElectionContext> electionSink;
  private final WeightGeneratorFactory weightsFactory;

  private final CopyOnWriteArrayList<StateChangeListener> listeners           = new CopyOnWriteArrayList<>();
  private volatile boolean       initiated;
  private final ClusterStatePersistor  clusterStatePersistor;

  private NodeID                       activeNode          = ServerID.NULL_ID;
  private NodeID                       syncdTo          = ServerID.NULL_ID;
  private volatile State               state               = START_STATE;
  private State               startState               = null;
  private final ElectionGate                      elections  = new ElectionGate();
  TerracottaOperatorEventLogger        operatorEventLogger = TerracottaOperatorEventLogging.getEventLogger();

  public StateManagerImpl(TCLogger consoleLogger, GroupManager<AbstractGroupMessage> groupManager, 
                          Sink<StateChangedEvent> stateChangeSink, StageManager mgr, 
                          StateManagerConfig stateManagerConfig, WeightGeneratorFactory weightFactory,
                          ClusterStatePersistor clusterStatePersistor) {
    this.consoleLogger = consoleLogger;
    this.groupManager = groupManager;
    this.stateChangeSink = stateChangeSink;
    this.weightsFactory = weightFactory;
    this.electionMgr = new ElectionManagerImpl(groupManager, stateManagerConfig);
    this.electionSink = mgr.createStage(ServerConfigurationContext.L2_STATE_ELECTION_HANDLER, ElectionContext.class, this.electionMgr.getEventHandler(), 1, 1024).getSink();
    this.clusterStatePersistor = clusterStatePersistor;
  }

  @Override
  public State getCurrentState() {
    return this.state;
  }
  
  private boolean electionStarted() {
    return elections.electionStarted();
  }
  
  private boolean electionFinished() {
    return elections.electionFinished();
  }
// for tests  
  public void waitForDeclaredActive() throws InterruptedException {
    synchronized (this) {
      while(activeNode.isNull()) {
        wait();
      }
    }
//  now make sure elections are done
    elections.waitForElectionToFinish();
  }
  
  public void endElection() {
    initiated = false;
    state = STOP_STATE;
    activeNode = ServerID.NULL_ID;
  }
  /*
   * XXX:: If ACTIVE went dead before any passive moved to STANDBY state, then the cluster is hung and there is no going
   * around it. If ACTIVE in persistent mode, it can come back and recover the cluster
   */
  @Override
  public void startElection() {
    debugInfo("Starting election");
    initiated = true;
    startState = clusterStatePersistor.getInitialState();
    // Went down as either PASSIVE_STANDBY or UNITIALIZED, either way we need to wait for the active to zap, just skip
    // the election and wait for a zap.
    info("Starting election initial state:" + startState);
    if (startState != null && !startState.equals(ACTIVE_COORDINATOR)) {
      info("Skipping election and waiting for the active to zap since this L2 did not go down as active.");
    } else if (state == START_STATE || state == PASSIVE_STANDBY) {
      runElection();
    } else {
      info("Ignoring Election request since not in right state");
    }
  }

  private void runElection() {
    if (!electionStarted()) {
      return;
    }
    NodeID myNodeID = getLocalNodeID();
    // Only new L2 if the DB was empty (no previous state) and the current state is START (as in before any elections
    // concluded)
    boolean isNew = state == START_STATE && startState == null;
    if (getActiveNodeID().isNull()) {
      debugInfo("Running election - isNew: " + isNew);
      electionSink.addSingleThreaded(new ElectionContext(myNodeID, isNew, weightsFactory, (nodeid)-> {
        boolean rerun = false;
        if (nodeid == myNodeID) {
          debugInfo("Won Election, moving to active state. myNodeID/winner=" + myNodeID);
          moveToActiveState();
        } else if (nodeid.isNull()) {
          Assert.fail();
        } else {
          electionMgr.reset(null);
          // Election is lost, but we wait for the active node to declare itself as winner. If this doesn't happen in a
          // finite time we restart the election. This is to prevent some weird cases where two nodes might end up
          // thinking the other one is the winner.
          // @see MNK-518
          debugInfo("Lost election, waiting for winner to declare as active, winner=" + nodeid);
          if (!waitUntilActiveNodeIDNotNull(electionMgr.getElectionTime())) {
            logger.info("rerunning election because " + nodeid + " never declared active");
            rerun = true;
          }
        }
        electionFinished();
        if (rerun) {
          runElection();
        }
      }));
    } else {
      electionFinished();
    }
  }

  private synchronized boolean waitUntilActiveNodeIDNotNull(long timeout) {
    while (activeNode.isNull() && timeout > 0) {
      long start = System.currentTimeMillis();
      try {
        wait(timeout);
      } catch (InterruptedException e) {
        logger.warn("Interrupted while waiting for ACTIVE to declare WON message ! ", e);
        break;
      }
      timeout = timeout - (System.currentTimeMillis() - start);
    }
    debugInfo("Wait for other active to declare as active over. Declared? activeNodeId.isNull() = "
              + activeNode.isNull() + ", activeNode=" + activeNode);
    return !activeNode.isNull();
  }
  // should be called from synchronized code
  private void setActiveNodeID(NodeID nodeID) {
    debugInfo("SETTING activeNode=" + nodeID);
    this.activeNode = nodeID;
    notifyAll();
  }

  private NodeID getLocalNodeID() {
    return groupManager.getLocalNodeID();
  }

  @Override
  public void registerForStateChangeEvents(StateChangeListener listener) {
    listeners.add(listener);
  }

  @Override
  public void fireStateChangedEvent(StateChangedEvent sce) {
    for (StateChangeListener listener : listeners) {
      listener.l2StateChanged(sce);
    }
  }

  private synchronized void moveToPassiveReady(Enrollment winningEnrollment) {
    electionMgr.reset(winningEnrollment);
    logger.debug("moving to passive ready " + state + " " + winningEnrollment);
    if (state == START_STATE) {
      setActiveNodeID(winningEnrollment.getNodeID());
      state = PASSIVE_UNINITIALIZED;
      info("Moved to " + state, true);
      fireStateChangedOperatorEvent();
      stateChangeSink.addSingleThreaded(new StateChangedEvent(START_STATE, state));
    } else if (state == PASSIVE_UNINITIALIZED) {
// double election
      Assert.assertTrue(syncdTo.isNull());
      setActiveNodeID(winningEnrollment.getNodeID());
    } else if (state == PASSIVE_SYNCING) {
      setActiveNodeID(winningEnrollment.getNodeID());
      if (!syncdTo.equals(winningEnrollment.getNodeID())) {
// TODO:  make sure this is the proper way to handle this.
        logger.fatal("Passive only partially synced when active disappeared.  Restarting");
        throw new ZapDirtyDbServerNodeException("Passive only partially synced when active disappeared.  Restarting");
      }
    } else if (state == ACTIVE_COORDINATOR) {
      // TODO:: Support this later
      throw new AssertionError("Cant move to " + PASSIVE_UNINITIALIZED + " from " + ACTIVE_COORDINATOR
                               + " at least for now");
    } else {
      //  PASSIVE_STANDBY
      setActiveNodeID(winningEnrollment.getNodeID());
    }
  }
  
  @Override
  public synchronized void moveToPassiveSyncing(NodeID connectedTo) {
    if (state == PASSIVE_UNINITIALIZED) {
      syncdTo = connectedTo;
      stateChangeSink.addSingleThreaded(new StateChangedEvent(state, PASSIVE_SYNCING));
      state = PASSIVE_SYNCING;
      info("Moved to " + state, true);
      fireStateChangedOperatorEvent();
    } 
  }

  @Override
  public synchronized void moveToPassiveStandbyState() {
    if (state == ACTIVE_COORDINATOR) {
      // TODO:: Support this later
      throw new AssertionError("Cant move to " + PASSIVE_STANDBY + " from " + ACTIVE_COORDINATOR + " at least for now");
    } else if (state != PASSIVE_STANDBY) {
      stateChangeSink.addSingleThreaded(new StateChangedEvent(state, PASSIVE_STANDBY));
      state = PASSIVE_STANDBY;
      info("Moved to " + state, true);
      fireStateChangedOperatorEvent();
    } else {
      info("Already in " + state);
    }
  }

  private synchronized void moveToActiveState() {
    if (state == START_STATE || state == PASSIVE_STANDBY) {
      // TODO :: If state == START_STATE publish cluster ID
      debugInfo("Moving to active state");
      StateChangedEvent event = new StateChangedEvent(state, ACTIVE_COORDINATOR);
      state = ACTIVE_COORDINATOR;
      stateChangeSink.addSingleThreaded(event);
      setActiveNodeID(getLocalNodeID());
      info("Becoming " + state, true);
      fireStateChangedOperatorEvent();
      electionMgr.declareWinner(getLocalNodeID());
    } else {
      throw new AssertionError("Cant move to " + ACTIVE_COORDINATOR + " from " + state);
    }
  }

  @Override
  public synchronized NodeID getActiveNodeID() {
    return activeNode;
  }

  @Override
  public boolean isActiveCoordinator() {
    return (state == ACTIVE_COORDINATOR);
  }

  public boolean isPassiveUnitialized() {
    return (state == PASSIVE_UNINITIALIZED);
  }

  @Override
  public void handleClusterStateMessage(L2StateMessage clusterMsg) {
    debugInfo("Received cluster state message: " + clusterMsg);
    try {
      switch (clusterMsg.getType()) {
        case L2StateMessage.START_ELECTION:
          handleStartElectionRequest(clusterMsg);
          break;
        case L2StateMessage.ABORT_ELECTION:
          handleElectionAbort(clusterMsg);
          break;
        case L2StateMessage.ELECTION_RESULT:
          handleElectionResultMessage(clusterMsg);
          break;
        case L2StateMessage.ELECTION_WON:
        case L2StateMessage.ELECTION_WON_ALREADY:
          handleElectionWonMessage(clusterMsg);
          break;
        default:
          throw new AssertionError("This message shouldn't have been routed here : " + clusterMsg);
      }
    } catch (GroupException ge) {
      logger.error("Zapping Node : Caught Exception while handling Message : " + clusterMsg, ge);
      groupManager.zapNode(clusterMsg.messageFrom(), L2HAZapNodeRequestProcessor.COMMUNICATION_ERROR,
                           "Error handling Election Message " + L2HAZapNodeRequestProcessor.getErrorString(ge));
    }
  }

  private synchronized void handleElectionWonMessage(L2StateMessage clusterMsg) {
    debugInfo("Received election_won or election_already_won msg: " + clusterMsg);
    Enrollment winningEnrollment = clusterMsg.getEnrollment();
    if (state == ACTIVE_COORDINATOR) {
      // Can't get Election Won from another node : Split brain
      String error = state + " Received Election Won Msg : " + clusterMsg
                     + ". A Terracotta server tried to join the mirror group as a second ACTIVE";
      logger.error(error);
      if (clusterMsg.getType() == L2StateMessage.ELECTION_WON_ALREADY) {
        sendNGResponse(clusterMsg.messageFrom(), clusterMsg);
      }
      groupManager.zapNode(winningEnrollment.getNodeID(), L2HAZapNodeRequestProcessor.SPLIT_BRAIN, error);
    } else if (activeNode.isNull() || activeNode.equals(winningEnrollment.getNodeID())
               || clusterMsg.getType() == L2StateMessage.ELECTION_WON) {
      // There is no active server for this node or the other node just detected a failure of ACTIVE server and ran an
      // election and is sending the results. This can happen if this node for some reason is not able to detect that
      // the active is down but the other node did. Go with the new active.
      if (startState == null || startState == START_STATE) {
        moveToPassiveReady(winningEnrollment);
        if (clusterMsg.getType() == L2StateMessage.ELECTION_WON_ALREADY) {
          sendOKResponse(clusterMsg.messageFrom(), clusterMsg);
        }
      } else {
        //this server was started with persistent data, this server could be either a ACTIVE or PASSIVE before
        // it went down. In any case, don't start this server in passive state.
// TODO: send a negative response so the active zaps this server.  This is a bad way to agree.  find a better way
        if (clusterMsg.getType() == L2StateMessage.ELECTION_WON_ALREADY) {
          sendNGResponse(clusterMsg.messageFrom(), clusterMsg);
        }
      }
    } else {
      // This is done to solve DEV-1532. Node sent ELECTION_WON_ALREADY message but our ACTIVE is intact.
      logger.warn("Conflicting Election Won  Msg : " + clusterMsg + " since I already have a ACTIVE Node : "
                  + activeNode + ". Sending NG response");
      // The reason we send a response for ELECTION_WON_ALREADY message is that if we don't agree we don't want the
      // other server to send us cluster state messages.
      sendNGResponse(clusterMsg.messageFrom(), clusterMsg);
    }
  }

  private synchronized void handleElectionResultMessage(L2StateMessage msg) throws GroupException {
    if (activeNode.equals(msg.getEnrollment().getNodeID())) {
      Assert.assertFalse(ServerID.NULL_ID.equals(activeNode));
      // This wouldn't normally happen, but we agree - so ack
      AbstractGroupMessage resultAgreed = L2StateMessage.createResultAgreedMessage(msg, msg.getEnrollment());
      logger.info("Agreed with Election Result from " + msg.messageFrom() + " : " + resultAgreed);
      groupManager.sendTo(msg.messageFrom(), resultAgreed);
    } else if (state == ACTIVE_COORDINATOR || !activeNode.isNull()
               || (msg.getEnrollment().isANewCandidate() && state != START_STATE)) {
      // Condition 1 :
      // Obviously an issue.
      // Condition 2 :
      // This shouldn't happen normally, but is possible when there is some weird network error where A sees B,
      // B sees A/C and C sees B and A is active and C is trying to run election
      // Force other node to rerun election so that we can abort
      // Condition 3 :
      // We don't want new L2s to win an election when there are old L2s in PASSIVE states.
      AbstractGroupMessage resultConflict = L2StateMessage.createResultConflictMessage(msg, EnrollmentFactory
          .createTrumpEnrollment(getLocalNodeID(), weightsFactory));
      warn("WARNING :: Active Node = " + activeNode + " , " + state
           + " received ELECTION_RESULT message from another node : " + msg + " : Forcing re-election "
           + resultConflict);
      groupManager.sendTo(msg.messageFrom(), resultConflict);
    } else {
      debugInfo("ElectionMgr handling election result msg: " + msg);
      electionMgr.handleElectionResultMessage(msg);
    }
  }

  private synchronized void handleElectionAbort(L2StateMessage clusterMsg) {
    if (state == ACTIVE_COORDINATOR) {
      // Cant get Abort back to ACTIVE, if so then there is a split brain
      String error = state + " Received Abort Election  Msg : Possible split brain detected ";
      logger.error(error);
      groupManager.zapNode(clusterMsg.messageFrom(), L2HAZapNodeRequestProcessor.SPLIT_BRAIN, error);
    } else {
      debugInfo("ElectionMgr handling election abort");
      electionMgr.handleElectionAbort(clusterMsg);
      moveToPassiveReady(clusterMsg.getEnrollment());
    }
  }

  private synchronized void handleStartElectionRequest(L2StateMessage msg) throws GroupException {
    if (state == ACTIVE_COORDINATOR) {
      // This is either a new L2 joining a cluster or a renegade L2. Force it to abort
      AbstractGroupMessage abortMsg = L2StateMessage.createAbortElectionMessage(msg, EnrollmentFactory
          .createTrumpEnrollment(getLocalNodeID(), weightsFactory));
      info("Forcing Abort Election for " + msg + " with " + abortMsg);
      groupManager.sendTo(msg.messageFrom(), abortMsg);      
    } else if (!electionMgr.handleStartElectionRequest(msg)) {
//  another server started an election.  Unclear which server is now active, clear the active and run our own election
      startElectionIfNecessary(ServerID.NULL_ID);
    }
  }

  // notify new node
  @Override
  public void publishActiveState(NodeID nodeID) throws GroupException {
    debugInfo("Publishing active state to nodeId: " + nodeID);
    Assert.assertTrue(isActiveCoordinator());
    AbstractGroupMessage msg = L2StateMessage.createElectionWonAlreadyMessage(EnrollmentFactory
        .createTrumpEnrollment(getLocalNodeID(), weightsFactory));
    L2StateMessage response = (L2StateMessage) groupManager.sendToAndWaitForResponse(nodeID, msg);
    validateResponse(nodeID, response);
  }

  private void validateResponse(NodeID nodeID, L2StateMessage response) throws GroupException {
    if (response == null || response.getType() != L2StateMessage.RESULT_AGREED) {
      String error = "Recd wrong response from : " + nodeID + " : msg = " + response + " while publishing Active State";
      logger.error(error);
      // throwing this exception will initiate a zap elsewhere
      throw new GroupException(error);
    }
  }

  @Override
  public void startElectionIfNecessary(NodeID disconnectedNode) {
    Assert.assertFalse(disconnectedNode.equals(getLocalNodeID()));
    boolean elect = false;
    if (!initiated) {
//  election has never been initiated.  do not participate
      return;
    }

    synchronized (this) {
      if (state == START_STATE || (!disconnectedNode.isNull() && disconnectedNode.equals(activeNode))) {
        // ACTIVE Node is gone
        setActiveNodeID(ServerID.NULL_ID);
      }
      if (state != ACTIVE_COORDINATOR && activeNode.isNull()) {
        elect = true;
      }
    }
    if (elect) {
      info("Starting Election to determine cluster wide ACTIVE L2");
      runElection();
    } else {
      debugInfo("Not starting election even though node left: " + disconnectedNode);
    }
  }

  private void sendOKResponse(NodeID fromNode, L2StateMessage msg) {
    try {
      groupManager.sendTo(fromNode, L2StateMessage.createResultAgreedMessage(msg, msg.getEnrollment()));
    } catch (GroupException e) {
      logger.error("Error handling message : " + msg, e);
    }
  }

  private void sendNGResponse(NodeID fromNode, L2StateMessage msg) {
    try {
      groupManager.sendTo(fromNode, L2StateMessage.createResultConflictMessage(msg, msg.getEnrollment()));
    } catch (GroupException e) {
      logger.error("Error handling message : " + msg, e);
    }
  }

  @Override
  public String toString() {
    return StateManagerImpl.class.getSimpleName() + ":" + this.state.toString();
  }

  private void fireStateChangedOperatorEvent() {
    TSAManagementEventPayload tsaManagementEventPayload = new TSAManagementEventPayload("TSA.L2.STATE_CHANGE");
    tsaManagementEventPayload.getAttributes().put("State", state.getName());
    TerracottaRemoteManagement.getRemoteManagementInstance().sendEvent(tsaManagementEventPayload.toManagementEvent());
    operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createClusterNodeStateChangedEvent(state
        .getName()));
  }

  private void info(String message) {
    info(message, false);
  }

  private void info(String message, boolean console) {
    logger.info(message);
    if (console) {
      consoleLogger.info(message);
    }
  }

  private void warn(String message) {
    warn(message, false);
  }

  private void warn(String message, boolean console) {
    logger.warn(message);
    if (console) {
      consoleLogger.warn(message);
    }
  }

  private static void debugInfo(String message) {
    logger.debug(message);
  }
  
  private static class ElectionGate {
    private boolean electionInProgress = false;
    
    public synchronized boolean electionStarted() {
      try {
        return !electionInProgress;
      } finally {
        electionInProgress = true;
        notifyAll();
      }
    }
    
    public synchronized boolean electionFinished() {
      try {
        return electionInProgress;
      } finally {
        electionInProgress = false;
        notifyAll();
      }
    }    
    
    public synchronized boolean isFinished() {
      return electionInProgress;
    }
    
    public synchronized void waitForElectionToFinish() throws InterruptedException {
      while (electionInProgress) {
        wait();
      }
    }    
  }
}
