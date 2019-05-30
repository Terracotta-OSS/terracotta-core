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
import com.tc.net.groups.ZapNodeRequestProcessor;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ManagementTopologyEventCollector;
import com.tc.objectserver.persistence.ClusterStatePersistor;
import com.tc.server.TCServer;
import com.tc.server.TCServerMain;
import com.tc.util.Assert;
import com.tc.util.State;
import java.util.Arrays;
import java.util.EnumSet;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;


public class StateManagerImpl implements StateManager {
  private static final Logger logger = LoggerFactory.getLogger(StateManagerImpl.class);

  private final Logger consoleLogger;
  private final GroupManager<AbstractGroupMessage> groupManager;
  private final ElectionManagerImpl        electionMgr;
  private final ConsistencyManager     availabilityMgr;
  private final StageController stateChangeSink;
  private final ManagementTopologyEventCollector eventCollector;
  private final Sink<ElectionContext> electionSink;
  private final Sink<StateChangedEvent> publishSink;
  private final WeightGeneratorFactory weightsFactory;

  private final CopyOnWriteArrayList<StateChangeListener> listeners           = new CopyOnWriteArrayList<>();
  // Used to determine whether or not the L2HACoordinator has started up and told us to start (it puts us into the
  //  started state - startElection()).
  private boolean didStartElection;
  private final ClusterStatePersistor  clusterStatePersistor;

  private NodeID                       activeNode          = ServerID.NULL_ID;
  private NodeID                       syncedTo          = ServerID.NULL_ID;
  private volatile ServerMode               state               = ServerMode.START;
  private final ServerMode               startState;
  private final ElectionGate                      elections  = new ElectionGate();
  private final TCServer tcServer;

  // Known servers from previous election
  Set<NodeID> prevKnownServers = new HashSet<>();

  // Known servers from current election
  Set<NodeID> currKnownServers = new HashSet<>();
  
  Enrollment verification = null;

  public StateManagerImpl(Logger consoleLogger, GroupManager<AbstractGroupMessage> groupManager,
                          StageController controller, ManagementTopologyEventCollector eventCollector, StageManager mgr, 
                          int expectedServers, int electionTimeInSec, WeightGeneratorFactory weightFactory,
                          ConsistencyManager availabilityMgr, 
                          ClusterStatePersistor clusterStatePersistor,
                          TCServer tcServer) {
    this.consoleLogger = consoleLogger;
    this.groupManager = groupManager;
    this.stateChangeSink = controller;
    this.eventCollector = eventCollector;
    this.weightsFactory = weightFactory;
    this.availabilityMgr = availabilityMgr;
    this.electionMgr = new ElectionManagerImpl(groupManager, expectedServers, electionTimeInSec);
    this.electionSink = mgr.createStage(ServerConfigurationContext.L2_STATE_ELECTION_HANDLER, ElectionContext.class, this.electionMgr.getEventHandler(), 1, 1024).getSink();
    this.publishSink = mgr.createStage(ServerConfigurationContext.L2_STATE_CHANGE_STAGE, StateChangedEvent.class, EventHandler.consumer(this::publishStateChange), 1, 1024).getSink();
    this.clusterStatePersistor = clusterStatePersistor;
    this.tcServer = tcServer;
    this.startState = StateManager.convert(clusterStatePersistor.getInitialState());
  }

  @Override
  public Map<String, ?> getStateMap() {
    LinkedHashMap<String, Object> map = new LinkedHashMap<>();
    map.put("startState", this.startState);
    map.put("currentState", this.state);
    map.put("active", this.activeNode);
    if (this.availabilityMgr instanceof ConsistencyManagerImpl) {
      ConsistencyManagerImpl cc = (ConsistencyManagerImpl)this.availabilityMgr;
      map.put("requestedActions", cc.getActions());
      map.put("availabilityRestriction", cc.isVoting());
      map.put("availabilityStuck", cc.isBlocked());
    } else {
      // no useful information to report
    }
    return map;
  }

  @Override
  public synchronized ServerMode getCurrentMode() {
    return this.state;
  }
  
  private synchronized Enrollment createVerificationEnrollment() {
    return availabilityMgr.createVerificationEnrollment(syncedTo, weightsFactory);
  }
  
  private boolean electionStarted() {
    boolean isElectionStarted = elections.electionStarted();
    if(isElectionStarted) {
      synchronized(this) {
        prevKnownServers.clear();
        prevKnownServers.addAll(currKnownServers);
        currKnownServers.clear();
        verification = createVerificationEnrollment();
      }
    }
    return isElectionStarted;
  }
  
  private boolean electionFinished() {
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
    NodeID myNodeID = getLocalNodeID();
    // Only new L2 if the DB was empty (no previous state) and the current state is START (as in before any elections
    // concluded)
    boolean isNew = isFreshServer();
    if (getActiveNodeID().isNull()) {
      debugInfo("Running election - isNew: " + isNew);
      electionSink.addToSink(new ElectionContext(myNodeID, isNew, weightsFactory, state.getState(), (nodeid)-> {
        boolean rerun = false;
        if (nodeid == myNodeID) {
          debugInfo("Won Election, moving to active state. myNodeID/winner=" + myNodeID);
          if (clusterStatePersistor.isDBClean() && 
              this.availabilityMgr.requestTransition(this.state, nodeid, ConsistencyManager.Transition.MOVE_TO_ACTIVE)) {
            moveToActiveState();
          } else {
            if (!clusterStatePersistor.isDBClean()) {
              logger.info("rerunning election because " + nodeid + " must be synced to an active");
            } else {
              logger.info("rerunning election because " + nodeid + " not allowed to transition");
            }
            rerun = true;
          }
        } else if (nodeid.isNull()) {
          Assert.fail();
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
        if (rerun) {
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
  private synchronized void setActiveNodeID(NodeID nodeID) {
    debugInfo("SETTING activeNode=" + nodeID);
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
    if (availabilityMgr instanceof ConsistencyManagerImpl) {
      long[] weights = winningEnrollment.getWeights();
      //  if the weight sizes are the same, we can assume that the generators 
      //  were the same on both ends.  The last one is the current term of the 
      //  winning election
      if (weights.length == weightsFactory.size()) {
        long term = weights[weights.length -1];
        if (term > 0) {
          ((ConsistencyManagerImpl)availabilityMgr).setCurrentTerm(term);
        }
      }
    }
    
    logger.info("moving to passive ready " + state + " " + src + " " + active);
    logger.info("verification = {}", getVerificationEnrollment());
    if (startState.containsData()) {
      // in this case, a passive cannot be added to a running cluster with data.  zap and restart
      zapAndResyncLocalNode("server contains stale data.");
      return;
    }
    ServerMode newState = (!winningEnrollment.getNodeID().isNull() && getVerificationEnrollment().equals(winningEnrollment)) ? 
            ServerMode.PASSIVE : ServerMode.UNINITIALIZED;
    Set<ServerMode> modes = newState == ServerMode.UNINITIALIZED ? EnumSet.of(ServerMode.START,ServerMode.UNINITIALIZED) :
            EnumSet.of(ServerMode.PASSIVE);
    try {
      setActiveNodeID(active);
      ServerMode oldState = switchToState(newState, modes);
      switch (oldState) {
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
      zapAndResyncLocalNode("resync data.");
    }
  }
  
  @Override
  public void moveToPassiveSyncing(NodeID connectedTo) {
    ServerMode old = switchToState(ServerMode.SYNCING, EnumSet.of(ServerMode.UNINITIALIZED));
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

  private void moveToActiveState() {
    refreshKnownServers();
    ServerMode oldState = switchToState(ServerMode.ACTIVE, EnumSet.of(ServerMode.START, ServerMode.PASSIVE));
    // TODO :: If state == START_STATE publish cluster ID
    debugInfo("Moving to active state");
    for (NodeID peer : prevKnownServers) {
      if (!this.availabilityMgr.requestTransition(state, peer, ConsistencyManager.Transition.ADD_PASSIVE)) {
        groupManager.zapNode(peer, L2HAZapNodeRequestProcessor.COMMUNICATION_ERROR, "unable to add passive");
      }
    }
    Enrollment verify = createVerificationEnrollment();
    electionMgr.declareWinner(verify, oldState.getState());
  }
  
  private synchronized void refreshKnownServers() {
    // we are moving from passive standby to active state with a new election but we need to use previous election
    // known servers list as they are in sync in with previous active
    currKnownServers.clear();
    currKnownServers.addAll(prevKnownServers);
  }
  
  private synchronized ServerMode switchToState(ServerMode newState, Set<ServerMode> validOldStates) throws IllegalStateException {
    synchronizedWaitForStart();
    if (!validOldStates.contains(state)) {
      throw new IllegalStateException("Cant move to " + newState + " from " + state + " valid states " + validOldStates);
    }
    try {
      logger.debug("Switching to " + newState);
      if (state != newState) {
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
    tcServer.l2StateChanged(event);
    State newState = event.getCurrentState();
//    stateChangeSink.addToSink(event);
    if (event.movedToActive()) {
//  if this server just became active
      eventCollector.serverDidEnterState(newState, TCServerMain.getServer().getActivateTime());      
    } else {
      eventCollector.serverDidEnterState(newState, System.currentTimeMillis());      
    }
    this.stateChangeSink.transition(event.getOldState(), newState);

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
    return state == ServerMode.START && startState == ServerMode.START;
  }
  
  private synchronized boolean canStartElection() {
    return state == ServerMode.START || state == ServerMode.PASSIVE;
  }
  /**
   * This will be called just before we start processing resends, so any server joins
   * after this call will be out of sync with this active, so we need to remove all
   * servers which are not connected yet
   */
  @Override
  public synchronized void cleanupKnownServers() {
    currKnownServers.removeIf(node->!groupManager.isNodeConnected(node));
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
    return verification;
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
        default:
          throw new AssertionError("This message shouldn't have been routed here : " + clusterMsg);
      }
    } catch (GroupException ge) {
      logger.error("Zapping Node : Caught Exception while handling Message : " + clusterMsg, ge);
      groupManager.zapNode(clusterMsg.messageFrom(), L2HAZapNodeRequestProcessor.COMMUNICATION_ERROR,
                           "Error handling Election Message " + L2HAZapNodeRequestProcessor.getErrorString(ge));
    }
  }
  
  private void handleElectionWonMessage(L2StateMessage clusterMsg) {
    debugInfo("Received election_won msg: " + clusterMsg);
    boolean verify = verifyElectionWonResults(clusterMsg);
    boolean transition = availabilityMgr.requestTransition(state, clusterMsg.getEnrollment().getNodeID(), ConsistencyManager.Transition.CONNECT_TO_ACTIVE);
    if (verify && transition) {
      moveToPassiveReady(clusterMsg);
    } else {
      groupManager.closeMember((ServerID)clusterMsg.messageFrom());
    }
  }

  private void handleElectionAlreadyWonMessage(L2StateMessage clusterMsg) {
    debugInfo("Received election_already_won msg: " + clusterMsg);
    // if two actives are split-brain and they try and zap each other
    // the ZapProcessor will resolve the differences
    if (isActiveCoordinator()) {
//  split brain, handle this by sending normal enrollment to determine who wins
      logger.info("split-brain detected");
    }
    boolean verify = verifyElectionWonResults(clusterMsg);
    boolean transition = availabilityMgr.requestTransition(state, clusterMsg.getEnrollment().getNodeID(), ConsistencyManager.Transition.CONNECT_TO_ACTIVE);
    if (verify && transition) {
      sendVerificationOKResponse(clusterMsg);
      moveToPassiveReady(clusterMsg);
    } else {
      sendVerificationNGResponse(clusterMsg);
    }
  }
  
  private boolean verifyElectionWonResults(L2StateMessage clusterMsg) {
    Enrollment winningEnrollment = clusterMsg.getEnrollment();
    Enrollment verify = getVerificationEnrollment();
    boolean peerWins = Arrays.equals(winningEnrollment.getWeights(), verify.getWeights()) ||
            winningEnrollment.wins(verify);
    return peerWins;
  }
  
  private void zapAndResyncLocalNode(String msg) {
    clusterStatePersistor.setDBClean(false);
    throw new TCServerRestartException("Restarting the server - " + msg); 
  }

  private synchronized void handleElectionResultMessage(L2StateMessage msg) throws GroupException {
    if (activeNode.equals(msg.getEnrollment().getNodeID())) {
      Assert.assertFalse(ServerID.NULL_ID.equals(activeNode));
      // This wouldn't normally happen, but we agree - so ack
      AbstractGroupMessage resultAgreed = L2StateMessage.createResultAgreedMessage(msg, msg.getEnrollment(), state.getState());
      logger.info("Agreed with Election Result from " + msg.messageFrom() + " : " + resultAgreed);
      groupManager.sendTo(msg.messageFrom(), resultAgreed);
    } else if (state == ServerMode.ACTIVE || !activeNode.isNull()
               || (msg.getEnrollment().isANewCandidate() && state != ServerMode.START)) {
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
    boolean verify = verifyElectionWonResults(clusterMsg);
    boolean transition = availabilityMgr.requestTransition(state, clusterMsg.getEnrollment().getNodeID(), ConsistencyManager.Transition.CONNECT_TO_ACTIVE);

    if (verify && transition) {
        sendVerificationOKResponse(clusterMsg);
        moveToPassiveReady(clusterMsg);
      } else {
        sendVerificationOKResponse(clusterMsg);
      }
  }

  private void handleStartElectionRequest(L2StateMessage msg) throws GroupException {
    if (getCurrentMode() == ServerMode.ACTIVE) {
      // This is either a new L2 joining a cluster or a renegade L2. Force it to abort
      Enrollment verify = createVerificationEnrollment();
      AbstractGroupMessage abortMsg = L2StateMessage.createAbortElectionMessage(msg, verify, state.getState());
      info("Forcing Abort Election for " + msg + " with " + abortMsg);
      L2StateMessage response = (L2StateMessage)groupManager.sendToAndWaitForResponse(msg.messageFrom(), abortMsg);
      validateResponse(response);
    } else {
      synchronized (this) {
        currKnownServers.add(msg.getEnrollment().getNodeID());
      }
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
    validateResponse(response);
  }

  //used in testing
  public synchronized void addKnownServersList(Set<NodeID> nodeIDs) {
    currKnownServers.addAll(nodeIDs);
  }

  private void validateResponse(L2StateMessage response) throws GroupException {
    if (response != null) {
      NodeID nodeID = response.messageFrom();
      ServerMode peerState = StateManager.convert(response.getState());
      if (response.getType() != L2StateMessage.RESULT_AGREED) {
        String error = "Recd wrong response from : " + nodeID + " : msg = " + response + " while publishing Active State";
        logger.error(error);
        if (peerState == ACTIVE) {
        //  split brain.  use enrollment to determine which active should continue
          Enrollment e = response.getEnrollment();
          Enrollment mine = EnrollmentFactory.createEnrollment(nodeID, false, weightsFactory);
          if (mine.wins(e)) {
            groupManager.zapNode(nodeID, L2HAZapNodeRequestProcessor.SPLIT_BRAIN, mine + " wins over " + e);
          } else {
            // if the other wins, expect them to zap us as soon as our response is sent
          }      
        } else if (peerState.canBeActive()) {
          zapAndResyncLocalNode("Passive has more recent data compared to active, restart"); 
        } else {
          logger.info("Server peer has more current data yet cannot be active.  Server is going dormant until next election.");
        }
      }
    }
  }

  @Override
  public void startElectionIfNecessary(NodeID disconnectedNode) {
    synchronized (this) {
      synchronizedWaitForStart();
    }
    Assert.assertFalse(disconnectedNode.equals(getLocalNodeID()));
    boolean elect = false;

    synchronized (this) {
      currKnownServers.remove(disconnectedNode);
      if (state == ServerMode.START || (!disconnectedNode.isNull() && disconnectedNode.equals(activeNode))) {
        // ACTIVE Node is gone
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
    if (elect) {
      info("Starting Election to determine cluster wide ACTIVE L2");
      runElection();
    } else {
      debugInfo("Not starting election even though node left: " + disconnectedNode);
    }
  }

  private void sendVerificationOKResponse(L2StateMessage msg) {
    try {
      Assert.assertTrue(msg.getType() != L2StateMessage.ELECTION_WON);
      ServerMode sendState = state == ServerMode.START ? startState : state;
      groupManager.sendTo(msg.messageFrom(), L2StateMessage.createResultAgreedMessage(msg, getVerificationEnrollment(), sendState.getState()));
    } catch (GroupException e) {
      logger.error("Error handling message : " + msg, e);
    }
  }

  private void sendVerificationNGResponse(L2StateMessage msg) {
    try {
    Assert.assertTrue(msg.getType() != L2StateMessage.ELECTION_WON);
      ServerMode sendState = state == ServerMode.START ? startState : state;
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
    while (!this.didStartElection) {
      try {
        wait();
      } catch (InterruptedException e) {
        // We don't expect interruption.
        throw Assert.failure("synchronizedWaitForStart() interrupted", e);
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
