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

import com.tc.async.api.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.async.api.Sink;
import com.tc.async.api.StageManager;
import com.tc.async.impl.StageController;
import com.tc.exception.TCServerRestartException;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.ha.L2HAZapNodeRequestProcessor;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.l2.msg.L2StateMessage;
import static com.tc.l2.state.ServerMode.ACTIVE;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.utils.L2Utils;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ManagementTopologyEventCollector;
import com.tc.objectserver.impl.Topology;
import com.tc.objectserver.impl.TopologyManager;
import com.tc.util.Assert;
import com.tc.util.State;

import java.util.EnumSet;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.terracotta.server.ServerEnv;
import org.terracotta.tripwire.TripwireFactory;
import com.tc.objectserver.persistence.ServerPersistentState;
import java.util.function.Predicate;


public class StateManagerImpl implements StateManager {
  private static final Logger logger = LoggerFactory.getLogger(StateManagerImpl.class);

  private final Logger consoleLogger;
  private final GroupManager<AbstractGroupMessage> groupManager;
  private final Predicate<NodeID>       startElection;
  private final ElectionManagerImpl        electionMgr;
  private final ConsistencyManager     availabilityMgr;
  private final TopologyManager topologyManager;
  private final StageController stateChangeSink;
  private final ManagementTopologyEventCollector eventCollector;
  private final Sink<ElectionContext> electionSink;
  private final Sink<StateChangedEvent> publishSink;
  private final WeightGeneratorFactory weightsFactory;

  private final CopyOnWriteArrayList<StateChangeListener> listeners           = new CopyOnWriteArrayList<>();
  // Used to determine whether or not the L2HACoordinator has started up and told us to start (it puts us into the
  //  started state - startElection()).
  private boolean didStartElection;
  private final ServerPersistentState  clusterStatePersistor;

  private NodeID                       activeNode          = ServerID.NULL_ID;
  private NodeID                       syncedTo          = ServerID.NULL_ID;
  private volatile ServerMode               state               = ServerMode.INITIAL;
  private final ServerMode               startState;
  private final ElectionGate                      elections  = new ElectionGate();

  private Enrollment verification = null;

  public StateManagerImpl(Logger consoleLogger, Predicate<NodeID> canStartElection, GroupManager<AbstractGroupMessage> groupManager,
                          StageController controller, ManagementTopologyEventCollector eventCollector, StageManager mgr,
                          int expectedServers, int electionTimeInSec, WeightGeneratorFactory weightFactory,
                          ConsistencyManager availabilityMgr,
                          ServerPersistentState serverPersitenceState, TopologyManager topologyManager) {
    this.consoleLogger = consoleLogger;
    this.groupManager = groupManager;
    this.startElection = canStartElection;
    this.stateChangeSink = controller;
    this.eventCollector = eventCollector;
    this.weightsFactory = weightFactory;
    this.availabilityMgr = availabilityMgr;
    this.topologyManager = topologyManager;
    this.electionMgr = new ElectionManagerImpl(groupManager, electionTimeInSec);
    this.electionSink = mgr.createStage(ServerConfigurationContext.L2_STATE_ELECTION_HANDLER, ElectionContext.class, this.electionMgr.getEventHandler(), 1, 1024, false, false).getSink();
    this.publishSink = mgr.createStage(ServerConfigurationContext.L2_STATE_CHANGE_STAGE, StateChangedEvent.class, EventHandler.consumer(this::publishStateChange), 1, 1024, false, false).getSink();
    this.clusterStatePersistor = serverPersitenceState;
    this.startState = serverPersitenceState.getInitialMode();
  }

  @Override
  public Map<String, ?> getStateMap() {
    LinkedHashMap<String, Object> map = new LinkedHashMap<>();
    map.put("startState", this.startState);
    map.put("currentState", this.state);
    map.put("active", this.activeNode);
    map.put("consistency", this.availabilityMgr.getStateMap());
    return map;
  }

  @Override
  public synchronized ServerMode getCurrentMode() {
    return this.state;
  }
  
  private Enrollment createVerificationEnrollment() {
    return availabilityMgr.createVerificationEnrollment(syncedTo, weightsFactory);
  }
  
  private boolean electionStarted() {
    boolean isElectionStarted = elections.electionStarted();
    if (isElectionStarted) {
      synchronized(this) {
        verification = createVerificationEnrollment();
      }
    }
    return isElectionStarted;
  }
  
  private boolean electionFinished() {
    synchronized (this) {
      verification = null;
    }
    return elections.electionFinished();
  }
// for tests  
  public void waitForDeclaredActive() throws InterruptedException {
    synchronized (this) {
      while(activeNode.isNull() && state != ServerMode.ACTIVE) {
        wait();
      }
    }
//  now make sure elections are done
    elections.waitForElectionToFinish();
  }
// for tests  
  public void waitForElectionsToFinish() throws InterruptedException {
//  now make sure elections are done
    elections.waitForElectionToFinish();
  }

  /*
   * XXX:: If ACTIVE went dead before any passive moved to STANDBY state, then the cluster is hung and there is no going
   * around it. If ACTIVE in persistent mode, it can come back and recover the cluster
   */
  @Override
  public void initializeAndStartElection() {
    debugInfo("Starting election");
    // Went down as either PASSIVE_STANDBY or UNITIALIZED, either way we need to wait for the active to zap, just skip
    // the election and wait for a zap.
    info("Starting election initial state:" + startState);
    if (canStartElection()) {
      runElection();
    } else {
      info("Ignoring Election request since not in right state: " + this.state);
    }
    // Now that we are ready, put us into the started state and notify anyone stuck waiting.
    synchronized (this) {
      Assert.assertFalse(this.didStartElection);
      this.didStartElection = true;
      this.notifyAll();
    }
  }

  @Override
  public void shutdown() {
    moveToStopState();
    logger.info("shutting down elections");
    this.electionMgr.shutdown();
  }

/**
 * A brief explanation of elections.  There are two phases of election.  The first is started
 * here with a broadcast to all connected servers initiated with the addition of an ElectionContext
 * to the election sink.  This only occurs when the the server is not active and is not connected 
 * to a current active.  This broadcast election attempts to select the best server participating 
 * in election to be the next active server.  The selected active then confirms election results 
 * {@link #verifyElectionWonResults(com.tc.l2.msg.L2StateMessage) verifyElectionWonResults}.  If all
 * participating servers agree, the selected server transitions to active and starts the second 
 * phase of election.  
 * 
 * This second phase of election is a verification step where active servers perform a peer to peer 
 * negotiation with passives that connect to it to make sure the data contained on the connecting 
 * server is properly in sequence with the new active server.  One of three things can happen in this 
 * step.  
 * <ul>
 *   <li>The active server verifies that the connecting passive has data that is behind and not in sync
 * with the current active, the connecting passive is zapped and resync'd</li>
 *   <li>The active server verifies that the connecting passive has data that is in-sync with the selected
 * active and allows connection as a continuing passive.  This normally happens when failover occurs in a
 * 3+ node stripe</li>
 *   <li>The active server verifies that the connecting passive has data that is ahead of the selected
 * active.  The selected active then clears its own data and restarts allowing another server to take 
 * over as active</li>
 * </ul>
 * 
 * This second phase election also occurs when any new server connects to a currently active server.
 */
  private void runElection() {
    if (!electionStarted()) {
      return;
    }

    // This topology will be used throughout the election process
    Topology lockedTopology = this.topologyManager.getTopology();

    NodeID myNodeID = getLocalNodeID();
    // Only new L2 if the DB was empty (no previous state) and the current state is START (as in before any elections
    // concluded)
    boolean isNew = isFreshServer();
    if (getActiveNodeID().isNull()) {
      debugInfo("Running election - isNew: " + isNew);
      electionSink.addToSink(new ElectionContext(myNodeID, lockedTopology.getServers(), isNew, weightsFactory, state.getState(), (nodeid)-> {
        boolean rerun = false;
        if (nodeid == myNodeID) {
          debugInfo("Won Election, moving to active state. myNodeID/winner=" + myNodeID);
          if (startState != ServerMode.RELAY && getCurrentMode().canBeActive() && clusterStatePersistor.isDBClean() && 
              this.availabilityMgr.requestTransition(this.state, nodeid, lockedTopology, ConsistencyManager.Transition.MOVE_TO_ACTIVE)) {
            moveToActiveState(electionMgr.passiveStandbys(), lockedTopology);
          } else {
            if (!clusterStatePersistor.isDBClean()) {
              logger.info("rerunning election because " + nodeid + " must be synced to an active");
            } else {
              logger.info("rerunning election because " + nodeid + " not allowed to transition");
            }
            rerun = true;
          }
        } else if (nodeid.isNull()) {
          rerun = true;
        } else {
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
        moveToStartStateIfBootstrapping();
        if (rerun && canStartElection()) {
          electionMgr.reset(ServerID.NULL_ID, null);
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
        L2Utils.handleInterrupted(logger, e);
      }
      timeout = timeout - (System.currentTimeMillis() - start);
    }
    debugInfo("Wait for other active to declare as active over. Declared? activeNodeId.isNull() = "
              + activeNode.isNull() + ", activeNode=" + activeNode);
    return !activeNode.isNull();
  }
  // should be called from synchronized code
  private synchronized void setActiveNodeID(NodeID nodeID) {
    info("SETTING activeNode=" + nodeID);
    this.activeNode = nodeID;
    if (!nodeID.isNull()) {
      // only set synced to if this is a real node
      this.syncedTo = nodeID;
    }
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
    listeners.forEach((listener) -> {
      listener.l2StateChanged(sce);
    });
  }

  private void moveToPassiveReady(L2StateMessage src) {
    Enrollment winningEnrollment = src.getEnrollment();
    NodeID active = src.messageFrom();
    
    electionMgr.reset(active, winningEnrollment);
        
    long[] weights = winningEnrollment.getWeights();
    //  term is always the last weight,  this value should
    //  not be used, it is currently only for informational
    //  purposes
    if (weights.length == weightsFactory.size()) {
      long term = weights[weights.length -1];
      if (term > 0 && term < Long.MAX_VALUE) {
        availabilityMgr.setCurrentTerm(term);
      }
    }

    // active is already set 
    if (!getActiveNodeID().isNull()) {
      logger.info("active already set");
      if (!getActiveNodeID().equals(active)) {
        zapAndResyncLocalNode("server already syncing with an active");
      }
      return;
    }
    
    if (startState.containsData()) {
      // in this case, a passive cannot be added to a running cluster with data.  zap and restart
      zapAndResyncLocalNode("server contains stale data.");
      return;
    }
    
    setActiveNodeID(active);
    if (startState == ServerMode.RELAY) {
      switchToState(ServerMode.RELAY, EnumSet.of(ServerMode.INITIAL, ServerMode.START, ServerMode.RELAY));
    } else {
      logger.info("moving to passive " + state + " " + src + " " + active);
      logger.info("verification = {}", getVerificationEnrollment());
    
      ServerMode newState = (!winningEnrollment.getNodeID().isNull() && getVerificationEnrollment().equals(winningEnrollment)) ? 
              ServerMode.PASSIVE : ServerMode.UNINITIALIZED;
      Set<ServerMode> modes = newState == ServerMode.UNINITIALIZED ? EnumSet.of(ServerMode.INITIAL, ServerMode.START,ServerMode.UNINITIALIZED) :
              EnumSet.of(ServerMode.PASSIVE);
      try {
        ServerMode oldState = switchToState(newState, modes);
        switch (oldState) {
          case INITIAL:
          case START:
            Assert.assertEquals(ServerMode.UNINITIALIZED, newState);
            break;
          case UNINITIALIZED:
            Assert.assertEquals(ServerMode.UNINITIALIZED, newState);
            // double election, ignore
            break;
          case PASSIVE:
            Assert.assertEquals(ServerMode.PASSIVE, newState);
            break;
          default:
            throw new IllegalStateException(state + " at move to passive ready");
        }
      } catch (IllegalStateException state) {
        zapAndResyncLocalNode(state.getMessage());
      }
    }
  }
  
  @Override
  public void moveToPassiveSyncing(NodeID connectedTo) {
    ServerMode old = switchToState(ServerMode.SYNCING, EnumSet.of(ServerMode.UNINITIALIZED, ServerMode.RELAY));
    if (old == ServerMode.RELAY) {
      setActiveNodeID(connectedTo);    
    }
    Assert.assertEquals(connectedTo, getActiveNodeID());
  }

  @Override
  public void moveToPassiveStandbyState() {
    ServerMode old = switchToState(ServerMode.PASSIVE, EnumSet.complementOf(EnumSet.of(ServerMode.ACTIVE)));
    if (old != ServerMode.PASSIVE) {
      clusterStatePersistor.setDBClean(true);
    } else {
      info("Already in " + state);
    }
  }

  @Override
  public void moveToStopState() {
      switchToState(ServerMode.STOP, EnumSet.allOf(ServerMode.class));
  }
  
  @Override
  public void moveToDiagnosticMode() {
      switchToState(ServerMode.DIAGNOSTIC, EnumSet.of(ServerMode.INITIAL));
  }
  
  @Override
  public void moveToRelayMode() {
      switchToState(ServerMode.RELAY, EnumSet.of(ServerMode.INITIAL, ServerMode.RELAY));
  }
  
  @Override
  public void moveToPassiveUnitialized() {
      switchToState(ServerMode.UNINITIALIZED, EnumSet.of(ServerMode.INITIAL));
  }
  
  private void moveToStartStateIfBootstrapping() {
    try {
      if (getCurrentMode() == ServerMode.INITIAL) {
        switchToState(ServerMode.START, EnumSet.of(ServerMode.INITIAL));
      }
    } catch (IllegalStateException state) {
      logger.info("request to start denied. Current state: {}", this.getCurrentMode());
    }
  }

  @Override
  public boolean moveToStopStateIf(Set<ServerMode> validStates) {
    try {
      switchToState(ServerMode.STOP, EnumSet.of(this.getCurrentMode()));
      return true;
    } catch (IllegalStateException state) {
      logger.info("request to stop denied. Current state: {}", this.getCurrentMode());
      return false;
    }
  }

  private void moveToActiveState(Set<ServerID> passives, Topology topology) {
    ServerMode oldState = switchToState(ServerMode.ACTIVE, EnumSet.of(ServerMode.INITIAL, ServerMode.START, ServerMode.PASSIVE));
    // TODO :: If state == START_STATE publish cluster ID
    debugInfo("Moving to active state");
    for (NodeID peer : passives) {
      if (!this.availabilityMgr.requestTransition(state, peer, topology, ConsistencyManager.Transition.ADD_PASSIVE)) {
        groupManager.zapNode(peer, L2HAZapNodeRequestProcessor.COMMUNICATION_ERROR, "unable to add passive");
      }
    }
    Enrollment verify = createVerificationEnrollment();
    electionMgr.declareWinner(verify, oldState.getState());
  }
  
  private synchronized ServerMode switchToState(ServerMode newState, Set<ServerMode> validOldStates) throws IllegalStateException {
    if (newState.requiresElection()) {
      synchronizedWaitForStart();
    }
    if (!validOldStates.contains(state)) {
      throw new IllegalStateException("Cant move to " + newState + " from " + state + " valid states " + validOldStates);
    }
    try {
      if (state != newState) {
        logger.debug("Switching to " + newState);
        TripwireFactory.createServerStateEvent(newState.toString(), ACTIVE == newState).commit();
        publishSink.addToSink(new StateChangedEvent(state.getState(), newState.getState()));
      }
      return state;
    } finally {
      state = newState;
      notifyAll();
    }
  }

  private void publishStateChange(StateChangedEvent event) {
    // publish new state to TCServer first to implement conditional shutdown operations
    State newState = event.getCurrentState();
//    stateChangeSink.addToSink(event);
    if (event.movedToActive()) {
//  if this server just became active
      long activate = System.currentTimeMillis();
      try {
        activate = ServerEnv.getServer().getActivateTime();
      } catch (Exception e) {
        // ignore
      }
      eventCollector.serverDidEnterState(newState, activate);
    } else {
      eventCollector.serverDidEnterState(newState, System.currentTimeMillis());      
    }

    if (StateManager.convert(event.getOldState()) != StateManager.convert(newState)) {
      this.stateChangeSink.transition(event.getOldState(), newState);
    }
    fireStateChangedEvent(event);
    info("Moved to " + newState, true);
  }

  @Override
  public synchronized NodeID getActiveNodeID() {
    if (state == ServerMode.ACTIVE) {
      return getLocalNodeID();
    }
    return activeNode;
  }

  private synchronized boolean isFreshServer() {
    return state.isStartup() && startState.isStartup();
  }
  
  private synchronized boolean canStartElection() {
    return state.canStartElection();
  }

  @Override
  public synchronized boolean isActiveCoordinator() {
    return (state == ServerMode.ACTIVE);
  }

  public synchronized boolean isPassiveUnitialized() {
    return (state == ServerMode.UNINITIALIZED);
  }
  
  public synchronized boolean isPassiveStandby() {
    return (state == ServerMode.PASSIVE);
  }
  
  private synchronized Enrollment getVerificationEnrollment() {
    return verification != null ? verification : createVerificationEnrollment();
  }

  @Override
  public void handleClusterStateMessage(L2StateMessage clusterMsg) {
    synchronized (this) {
      synchronizedWaitForStart();
    }
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
          handleElectionWonMessage(clusterMsg);
          break;
        case L2StateMessage.ELECTION_WON_ALREADY:
          handleElectionAlreadyWonMessage(clusterMsg);
          break;
        case L2StateMessage.RESULT_AGREED:
        case L2StateMessage.RESULT_CONFLICT:
          // just swallow this message, it is orphaned from another election
          break;
        default:
          throw new AssertionError("This message shouldn't have been routed here : " + clusterMsg);
      }
    } catch (GroupException ge) {
      logger.error("Caught Exception while handling Message : " + clusterMsg, ge);
    }
  }
  
  private void handleElectionWonMessage(L2StateMessage clusterMsg) {
    debugInfo("Received election_won msg: " + clusterMsg);
    boolean verify = checkIfPeerWinsVerificationElection(clusterMsg) && !isActiveCoordinator();
    boolean transition = verify && availabilityMgr.requestTransition(state, clusterMsg.getEnrollment().getNodeID(), ConsistencyManager.Transition.CONNECT_TO_ACTIVE);
    if (transition) {
      moveToPassiveReady(clusterMsg);
    } else {
      groupManager.closeMember(clusterMsg.messageFrom());
    }
  }

  private void handleElectionAlreadyWonMessage(L2StateMessage clusterMsg) {
    debugInfo("Received election_already_won msg: " + clusterMsg);
    // if two actives are split-brain and they try and zap each other
    // the ZapProcessor will resolve the differences
    if (isActiveCoordinator()) {
      logger.info("split-brain detected");
    }
    verifyActiveDeclarationAndRespond(clusterMsg);
  }
  
  private boolean checkIfPeerWinsVerificationElection(L2StateMessage clusterMsg) {
    Enrollment winningEnrollment = clusterMsg.getEnrollment();
    Enrollment verify = getVerificationEnrollment();
    int len = Math.min(winningEnrollment.getWeights().length, verify.getWeights().length);
    // if weights are equal, the default is for the active to continue as active
    boolean peerWins = !isActiveCoordinator();
    for (int x=0;x<len;x++) {
      if (winningEnrollment.getWeights()[x] != verify.getWeights()[x]) {
        peerWins = false;
        break;
      }
    }
    peerWins |= winningEnrollment.wins(verify);
    logger.info("verifying election won results isActive:{} remote:{} local:{} remoteWins:{}", isActiveCoordinator(), winningEnrollment, verify, peerWins);
    return peerWins;
  }
  
  private void zapAndResyncLocalNode(String msg) {
    clusterStatePersistor.setDBClean(false);
    throw new TCServerRestartException("Clear and resync - " + msg); 
  }

  private synchronized void handleElectionResultMessage(L2StateMessage msg) throws GroupException {
    if (activeNode.equals(msg.getEnrollment().getNodeID())) {
      Assert.assertFalse(ServerID.NULL_ID.equals(activeNode));
      // This wouldn't normally happen, but we agree - so ack
      AbstractGroupMessage resultAgreed = L2StateMessage.createResultAgreedMessage(msg, msg.getEnrollment(), state.getState());
      logger.info("Agreed with Election Result from " + msg.messageFrom() + " : " + resultAgreed);
      groupManager.sendTo(msg.messageFrom(), resultAgreed);
    } else if (state == ServerMode.ACTIVE || !activeNode.isNull()
               || (msg.getEnrollment().isANewCandidate() && !state.isStartup())) {
      // Condition 1 :
      // Obviously an issue.
      // Condition 2 :
      // This shouldn't happen normally, but is possible when there is some weird network error where A sees B,
      // B sees A/C and C sees B and A is active and C is trying to run election
      // Force other node to rerun election so that we can abort
      // Condition 3 :
      // We don't want new L2s to win an election when there are old L2s in PASSIVE states.
      AbstractGroupMessage resultConflict = L2StateMessage.createResultConflictMessage(msg, msg.getEnrollment(), state.getState());
      warn("WARNING :: Active Node = " + activeNode + " , " + state
           + " received ELECTION_RESULT message from another node : " + msg + " : Forcing re-election "
           + resultConflict);
      groupManager.sendTo(msg.messageFrom(), resultConflict);
    } else {
      debugInfo("ElectionMgr handling election result msg: " + msg);
      electionMgr.handleElectionResultMessage(msg, state.getState());
    }
  }

  private void handleElectionAbort(L2StateMessage clusterMsg) {
    electionMgr.handleElectionAbort(clusterMsg, state.getState());
    // if two actives are split-brain and they try and zap each other
    // the ZapProcessor will resolve the differences
    if (isActiveCoordinator()) {
      logger.info("split-brain detected");
    }
    verifyActiveDeclarationAndRespond(clusterMsg);
  }
  
  private void verifyActiveDeclarationAndRespond(L2StateMessage clusterMsg) {
    boolean verify = checkIfPeerWinsVerificationElection(clusterMsg) && !isActiveCoordinator();
    boolean transition = verify && availabilityMgr.requestTransition(state, clusterMsg.getEnrollment().getNodeID(), ConsistencyManager.Transition.CONNECT_TO_ACTIVE);

    if (transition) {
      sendVerificationOKResponse(clusterMsg);
      moveToPassiveReady(clusterMsg);
    } else {
      sendVerificationNGResponse(clusterMsg);
    }
  }

  private void handleStartElectionRequest(L2StateMessage msg) throws GroupException {
    if (getCurrentMode() == ServerMode.ACTIVE) {
      // This is either a new L2 joining a cluster or a renegade L2. Force it to abort
      Enrollment verify = createVerificationEnrollment();
      AbstractGroupMessage abortMsg = L2StateMessage.createAbortElectionMessage(msg, verify, state.getState());
      info("Forcing Abort Election for " + msg + " with " + abortMsg);
      L2StateMessage response = (L2StateMessage)groupManager.sendToAndWaitForResponse(msg.messageFrom(), abortMsg);
      validatePeerResponseToActiveDelaration(response);
    } else {
      if (!electionMgr.handleStartElectionRequest(msg, state.getState())) {
//  another server started an election.  Unclear which server is now active, clear the active and run our own election
        startElectionIfNecessary(ServerID.NULL_ID);
      }
    }
  }

  // notify new node
  @Override
  public void publishActiveState(NodeID nodeID) throws GroupException {
    debugInfo("Publishing active state to nodeId: " + nodeID);
    Assert.assertTrue(isActiveCoordinator());
    Enrollment verify = createVerificationEnrollment();
    AbstractGroupMessage msg = L2StateMessage.createElectionWonAlreadyMessage(verify, state.getState());
    L2StateMessage response = (L2StateMessage) groupManager.sendToAndWaitForResponse(nodeID, msg);
    validatePeerResponseToActiveDelaration(response);
  }

  private void validatePeerResponseToActiveDelaration(L2StateMessage response) throws GroupException {
    if (response != null) {
      NodeID nodeID = response.messageFrom();
      ServerMode peerState = StateManager.convert(response.getState());
      if (response.getType() != L2StateMessage.RESULT_AGREED) {        
        String error = "Recd wrong response from : " + nodeID + " : msg = " + response + " while publishing Active State";
        logger.info(error);
        
        boolean doesPeerWin = checkIfPeerWinsVerificationElection(response);
        if (doesPeerWin) {
        //  this agrees with the negative response, the peer server should take over
          if (peerState == ACTIVE) {
            // will be zapped.  the only way to get here is because this server is active
            Assert.assertTrue(isActiveCoordinator());
          } else if (peerState.canBeActive()) {
            zapAndResyncLocalNode("Passive has more recent data compared to active, node is restarting");
          } else {
            logger.info("Node peer has more current data yet cannot be active.  Server is going dormant until next election.");
          }
        } else {
          if (peerState == ACTIVE) {
            // split-brain and the peer is old.  zap it
            groupManager.zapNode(nodeID, L2HAZapNodeRequestProcessor.SPLIT_BRAIN, getVerificationEnrollment() + " wins over " + response.getEnrollment());
          } else if (peerState.canBeActive()) {
            logger.info("results not agreed for verification election in state: {}", peerState);
          } else {
            // the peer does not win and connot be active, zap and re-sync it
            logger.info("results not agreed for verification election in state: {}", peerState);
          }
        }
      } else {
        // result agreed, do nothing
      }
    }
  }

  @Override
  public Set<ServerID> getPassiveStandbys() {
    return electionMgr.passiveStandbys();
  }

  @Override
  public void startElectionIfNecessary(NodeID disconnectedNode) {
    synchronizedWaitForStart();
    Assert.assertFalse(disconnectedNode.equals(getLocalNodeID()));
    boolean elect = false;
    
    synchronized (this) {
      if (state.isStartup() || (!disconnectedNode.isNull() && disconnectedNode.equals(activeNode))) {
        // ACTIVE Node is gone
        info("ACTIVE is gone");
        setActiveNodeID(ServerID.NULL_ID);
      }
      if (state == ServerMode.SYNCING && this.activeNode.equals(disconnectedNode)) {
        //  need to zap and start over.  The active being synced to is gone.
        logger.error("Passive only partially synced when active disappeared.");
        elect = true;
//        zapAndResyncLocalNode("Passive only partially synced when active disappeared.  Restarting"); 
      } else if (state != ServerMode.ACTIVE && activeNode.isNull()) {
        elect = true;
      }
    }
    if (elect && startElection.test(disconnectedNode)) {
      info("Starting Election to determine cluster wide ACTIVE L2");
      runElection();
    } else {
      debugInfo("Not starting election even though node left: " + disconnectedNode);
    }
  }

  private void sendVerificationOKResponse(L2StateMessage msg) {
    try {
      Assert.assertTrue(msg.getType() != L2StateMessage.ELECTION_WON);
      ServerMode sendState = state.isStartup() ? startState : state;
      groupManager.sendTo(msg.messageFrom(), L2StateMessage.createResultAgreedMessage(msg, getVerificationEnrollment(), sendState.getState()));
    } catch (GroupException e) {
      logger.error("Error handling message : " + msg, e);
    }
  }

  private void sendVerificationNGResponse(L2StateMessage msg) {
    try {
    Assert.assertTrue(msg.getType() != L2StateMessage.ELECTION_WON);
      ServerMode sendState = state.isStartup() ? startState : state;
      groupManager.sendTo(msg.messageFrom(), L2StateMessage.createResultConflictMessage(msg, getVerificationEnrollment(), sendState.getState()));
    } catch (GroupException e) {
      logger.error("Error handling message : " + msg, e);
    }
  }

  @Override
  public String toString() {
    return StateManagerImpl.class.getSimpleName() + ":" + this.state.toString();
  }

  private void info(String message) {
    info(message, false);
  }

  private void info(String message, boolean console) {
    if (console) {
      consoleLogger.info(message);
    } else {
      logger.info(message);
    }
  }

  private void warn(String message) {
    warn(message, false);
  }

  private void warn(String message, boolean console) {
    if (console) {
      consoleLogger.warn(message);
    } else {
      logger.warn(message);
    }
  }

  private static void debugInfo(String message) {
    logger.debug(message);
  }

  /**
   * Internal helper which MUST BE CALLED UNDER MONITOR to wait until the L2HACoordinator has made its initial call to
   * put the receiver into a valid state.
   * This addresses a race between the L2HACoordinator thread, which initializes this object, and the other threads
   * which notify it when messages arrive.
   */
  private void synchronizedWaitForStart() {
    while (!this.didStartElection && this.state.isStartup()) {
      try {
        wait();
      } catch (InterruptedException e) {
        L2Utils.handleInterrupted(logger, e);
      }
    }
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
    
    public synchronized void waitForElectionToFinish() throws InterruptedException {
      while (electionInProgress) {
        wait();
      }
    }    
  }
}
