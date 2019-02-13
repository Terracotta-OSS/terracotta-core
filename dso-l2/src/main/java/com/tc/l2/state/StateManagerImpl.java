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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.async.api.Sink;
import com.tc.async.api.StageManager;
import com.tc.exception.TCServerRestartException;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.ha.L2HAZapNodeRequestProcessor;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.l2.msg.L2StateMessage;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.persistence.ClusterStatePersistor;
import com.tc.server.TCServer;
import com.tc.server.TCServerMain;
import com.tc.util.Assert;
import java.util.Arrays;

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
  private final Sink<StateChangedEvent> stateChangeSink;
  private final Sink<ElectionContext> electionSink;
  private final WeightGeneratorFactory weightsFactory;

  private final CopyOnWriteArrayList<StateChangeListener> listeners           = new CopyOnWriteArrayList<>();
  // Used to determine whether or not the L2HACoordinator has started up and told us to start (it puts us into the
  //  started state - startElection()).
  private boolean didStartElection;
  private final ClusterStatePersistor  clusterStatePersistor;

  private NodeID                       activeNode          = ServerID.NULL_ID;
  private NodeID                       syncdTo          = ServerID.NULL_ID;
  private volatile ServerMode               state               = ServerMode.START;
  private ServerMode               startState               = null;
  private final ElectionGate                      elections  = new ElectionGate();
  private final TCServer tcServer;

  // Known servers from previous election
  Set<NodeID> prevKnownServers = new HashSet<>();

  // Known servers from current election
  Set<NodeID> currKnownServers = new HashSet<>();

  public StateManagerImpl(Logger consoleLogger, GroupManager<AbstractGroupMessage> groupManager,
                          Sink<StateChangedEvent> stateChangeSink, StageManager mgr, 
                          int expectedServers, int electionTimeInSec, WeightGeneratorFactory weightFactory,
                          ConsistencyManager availabilityMgr, 
                          ClusterStatePersistor clusterStatePersistor,
                          TCServer tcServer) {
    this.consoleLogger = consoleLogger;
    this.groupManager = groupManager;
    this.stateChangeSink = stateChangeSink;
    this.weightsFactory = weightFactory;
    this.availabilityMgr = availabilityMgr;
    this.electionMgr = new ElectionManagerImpl(groupManager, expectedServers, electionTimeInSec);
    this.electionSink = mgr.createStage(ServerConfigurationContext.L2_STATE_ELECTION_HANDLER, ElectionContext.class, this.electionMgr.getEventHandler(), 1, 1024).getSink();
    this.clusterStatePersistor = clusterStatePersistor;
    this.tcServer = tcServer;
  }

  @Override
  public Map<String, ?> getStateMap() {
    LinkedHashMap<String, Object> map = new LinkedHashMap<>();
    map.put("startState", this.startState);
    map.put("currentState", this.state);
    map.put("active", this.activeNode);
    map.put("syncedTo", this.syncdTo);
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
  public ServerMode getCurrentMode() {
    return this.state;
  }
  
  private boolean electionStarted() {
    boolean isElectionStarted = elections.electionStarted();
    if(isElectionStarted) {
      synchronized(this) {
        prevKnownServers.clear();
        prevKnownServers.addAll(currKnownServers);
        currKnownServers.clear();
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
      while(activeNode.isNull()) {
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
    startState = StateManager.convert(clusterStatePersistor.getInitialState());
    // Went down as either PASSIVE_STANDBY or UNITIALIZED, either way we need to wait for the active to zap, just skip
    // the election and wait for a zap.
    info("Starting election initial state:" + startState);
    if (state == ServerMode.START || state == ServerMode.PASSIVE) {
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
    boolean isNew = state == ServerMode.START && startState == ServerMode.START;
    if (getActiveNodeID().isNull()) {
      debugInfo("Running election - isNew: " + isNew);
      electionSink.addToSink(new ElectionContext(myNodeID, isNew, weightsFactory, state.getState(), (nodeid)-> {
        boolean rerun = false;
        if (nodeid == myNodeID) {
          debugInfo("Won Election, moving to active state. myNodeID/winner=" + myNodeID);
          if (startState != ServerMode.START && startState != ServerMode.ACTIVE) {
            info("Skipping election and waiting for the active to zap since this L2 did not go down as active.", true);
          } else if (clusterStatePersistor.isDBClean() && 
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
          electionMgr.reset(null);
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
    listeners.forEach((listener) -> {
      listener.l2StateChanged(sce);
    });
  }

  private synchronized void moveToPassiveReady(L2StateMessage src) {
    Enrollment winningEnrollment = src.getEnrollment();
    NodeID active = src.messageFrom();
    
    electionMgr.reset(winningEnrollment);
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
    
    long[] verify = weightsFactory.generateVerificationSequence();
    logger.info("moving to passive ready " + state + " " + src + " " + this.syncdTo);
    logger.info("verification = " + Arrays.toString(verify));
    switch (state) {
      case START:
        setActiveNodeID(active);
        state = (startState == ServerMode.START) ? ServerMode.UNINITIALIZED : startState;
        if (state != ServerMode.PASSIVE && state != ServerMode.UNINITIALIZED) {
// TODO:  make sure this is the proper way to handle this.
          logger.error("caught in an unclean state " + state);
          zapAndResyncLocalNode("Caught in an inconsistent state.  Restarting with a new DB");
        } 
        info("Moved to " + state, true);
        publishStateChange(new StateChangedEvent(START_STATE, state.getState()));
        break;
      case UNINITIALIZED:
        // double election
        Assert.assertTrue(syncdTo.isNull());
        setActiveNodeID(active);
        break;
      case SYNCING:
        setActiveNodeID(active);
        if (!syncdTo.equals(winningEnrollment.getNodeID())) {
          // TODO:  make sure this is the proper way to handle this.
          logger.error("Passive only partially synced when active disappeared.  Restarting");
          zapAndResyncLocalNode("Passive only partially synced when active disappeared.  Restarting");
        } 
        break;
      case PASSIVE:
        if (src.getType() == L2StateMessage.ELECTION_WON &&
          winningEnrollment.getNodeID().equals(syncdTo)) {
          Assert.assertEquals(src.getState(), ServerMode.PASSIVE.getState());
          setActiveNodeID(active);
        } else {
          zapAndResyncLocalNode("Cannot replace active");
        }
        break;
      default:
        throw new IllegalStateException(state + " at move to passive ready");
    }
  }
  
  @Override
  public synchronized void moveToPassiveSyncing(NodeID connectedTo) {
    synchronizedWaitForStart();
    if (state == ServerMode.UNINITIALIZED) {
      syncdTo = connectedTo;
      publishStateChange(new StateChangedEvent(state.getState(), PASSIVE_SYNCING));
      state = ServerMode.SYNCING;
      info("Moved to " + state, true);
    } 
  }

  @Override
  public synchronized void moveToPassiveStandbyState() {
    synchronizedWaitForStart();
    if (state == ServerMode.ACTIVE) {
      // TODO:: Support this later
      throw new AssertionError("Cant move to " + PASSIVE_STANDBY + " from " + ACTIVE_COORDINATOR + " at least for now");
    } else if (state != ServerMode.PASSIVE) {
      clusterStatePersistor.setDBClean(true);
      publishStateChange(new StateChangedEvent(state.getState(), PASSIVE_STANDBY));
      state = ServerMode.PASSIVE;
      info("Moved to " + state, true);
    } else {
      info("Already in " + state);
    }
  }

  private synchronized void moveToActiveState() {
    synchronizedWaitForStart();
    if (state == ServerMode.START || state == ServerMode.PASSIVE) {
      ServerMode oldState = this.state;
      // TODO :: If state == START_STATE publish cluster ID
      debugInfo("Moving to active state");
      StateChangedEvent event = new StateChangedEvent(state.getState(), ACTIVE_COORDINATOR);
      state = ServerMode.ACTIVE;
      publishStateChange(event);
      setActiveNodeID(getLocalNodeID());
      info("Becoming " + state, true);
      // we are moving from passive standby to active state with a new election but we need to use previous election
      // known servers list as they are in sync in with previous active
      currKnownServers.clear();
      currKnownServers.addAll(prevKnownServers);
      electionMgr.declareWinner(EnrollmentFactory.createVerificationEnrollment(syncdTo, weightsFactory), oldState.getState());
    } else {
      throw new AssertionError("Cant move to " + ACTIVE_COORDINATOR + " from " + state);
    }
  }

  private void publishStateChange(StateChangedEvent event) {
    // publish new state to TCServer first to implement conditional shutdown operations
    tcServer.l2StateChanged(event);
    stateChangeSink.addToSink(event);
  }

  @Override
  public synchronized NodeID getActiveNodeID() {
    return activeNode;
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
  public boolean isActiveCoordinator() {
    return (state == ServerMode.ACTIVE);
  }

  public boolean isPassiveUnitialized() {
    return (state == ServerMode.UNINITIALIZED);
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
    if (state == ServerMode.ACTIVE) {
      // Can't get Election Won from another node : Split brain
      String error = state + " Received Election Won Msg : " + clusterMsg
                     + ". A Terracotta server tried to join the mirror group as a second ACTIVE";
      logger.error(error);
      verifyElectionWonResults(clusterMsg);
    } else {
      // There is no active server for this node or the other node just detected a failure of ACTIVE server and ran an
      // election and is sending the results. This can happen if this node for some reason is not able to detect that
      // the active is down but the other node did. Go with the new active.
      switch (startState) {
        case START:
          if (availabilityMgr.requestTransition(state, clusterMsg.getEnrollment().getNodeID(), ConsistencyManager.Transition.CONNECT_TO_ACTIVE)) {
//  can replace active if and only if the election is won and the servers are in the same place as 
//  determined by maximum weights
            moveToPassiveReady(clusterMsg);
          } else {
            throw new AssertionError("connect to active transition should never fail");
          }
          if (clusterMsg.getType() == L2StateMessage.ELECTION_WON_ALREADY) {
            sendOKResponse(clusterMsg);
          }
          break;
        case ACTIVE:
        case PASSIVE:
          verifyElectionWonResults(clusterMsg);
          break;
        default:
          zapAndResyncLocalNode("clean partial data");
      }
    }
  }
  
  private void verifyElectionWonResults(L2StateMessage clusterMsg) {
    int messageType = clusterMsg.getType();
    Enrollment winningEnrollment = clusterMsg.getEnrollment();
    Enrollment verification = EnrollmentFactory.createVerificationEnrollment(getLocalNodeID(), weightsFactory);
    boolean peerWins = Arrays.equals(winningEnrollment.getWeights(), verification.getWeights()) ||
            winningEnrollment.wins(verification);
    if (peerWins) {
      if (messageType == L2StateMessage.ELECTION_WON_ALREADY || messageType == L2StateMessage.ABORT_ELECTION) {
//  other server wins, they will zap us
        sendNGResponse(clusterMsg.messageFrom(), clusterMsg, winningEnrollment);
      } else {
//  ELECTION_WON is a broadcast not requiring or expecting a return message, zap local server to re-sync
        zapAndResyncLocalNode("Split-brain and this node is older, restart to resync");
      }
    } else {
//  zap the other node, it is older
      sendNGResponse(clusterMsg.messageFrom(), clusterMsg, verification);
    }
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

  private synchronized void handleElectionAbort(L2StateMessage clusterMsg) {
    if (state != ServerMode.ACTIVE && startState == ServerMode.START) {
  //  only try and reattach to active if state is not active and no persistent data restart
      debugInfo("ElectionMgr handling election abort");
      electionMgr.handleElectionAbort(clusterMsg, state.getState());
      sendOKResponse(clusterMsg);
      if (availabilityMgr.requestTransition(state, clusterMsg.getEnrollment().getNodeID(), ConsistencyManager.Transition.CONNECT_TO_ACTIVE)) {
        moveToPassiveReady(clusterMsg); // something happened with the network between servers, connot replace active  
      } else {
        throw new AssertionError("connect to active transition should never fail");
      }
    } else {
      verifyElectionWonResults(clusterMsg);
    }
  }

  private synchronized void handleStartElectionRequest(L2StateMessage msg) throws GroupException {
    if (state == ServerMode.ACTIVE) {
      //  if this is not a new candidate, zap it and start over
      if (!msg.getEnrollment().isANewCandidate() && msg.getState().equals(START_STATE)) {
        groupManager.zapNode(msg.messageFrom(), L2HAZapNodeRequestProcessor.SPLIT_BRAIN, "");
      } else {
        // This is either a new L2 joining a cluster or a renegade L2. Force it to abort
        AbstractGroupMessage abortMsg = L2StateMessage.createAbortElectionMessage(msg, EnrollmentFactory
            .createVerificationEnrollment(getLocalNodeID(), weightsFactory), state.getState());
        info("Forcing Abort Election for " + msg + " with " + abortMsg);
        L2StateMessage response = (L2StateMessage)groupManager.sendToAndWaitForResponse(msg.messageFrom(), abortMsg);
        validateResponse(response);
      }
    } else {
      currKnownServers.add(msg.getEnrollment().getNodeID());
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
    AbstractGroupMessage msg = L2StateMessage.createElectionWonAlreadyMessage(EnrollmentFactory
        .createVerificationEnrollment(getLocalNodeID(), weightsFactory), state.getState());
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
      final String errMesg = "A Terracotta server tried to join the mirror group as PASSIVE STANDBY but with dirty db, "
          + " Zapping " +  nodeID + " to allow it to resync data from active";
      if (response.getType() != L2StateMessage.RESULT_AGREED) {
        String error = "Recd wrong response from : " + nodeID + " : msg = " + response + " while publishing Active State";
        logger.error(error);
        Enrollment verification = response.getEnrollment();
        if (verification.getNodeID().equals(getLocalNodeID())) {
        // throwing this exception will initiate a zap elsewhere
          throw new GroupException(error);
        } else {
          zapAndResyncLocalNode("Passive has more recent data compared to active, restart"); 
        }
      } else if (response.getState().equals(PASSIVE_STANDBY) && !currKnownServers.contains(nodeID)) {
        logger.error(errMesg);
        this.groupManager.zapNode(nodeID, L2HAZapNodeRequestProcessor.NODE_JOINED_WITH_DIRTY_DB, errMesg);
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
      if (state == ServerMode.SYNCING && syncdTo.equals(disconnectedNode)) {
        //  need to zap and start over.  The active being synced to is gone.
        logger.error("Passive only partially synced when active disappeared.  Restarting");
        zapAndResyncLocalNode("Passive only partially synced when active disappeared.  Restarting"); 
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

  private void sendOKResponse(L2StateMessage msg) {
    try {
      groupManager.sendTo(msg.messageFrom(), L2StateMessage.createResultAgreedMessage(msg, msg.getEnrollment(), state.getState()));
    } catch (GroupException e) {
      logger.error("Error handling message : " + msg, e);
    }
  }

  private void sendNGResponse(NodeID fromNode, L2StateMessage msg, Enrollment winner) {
    try {
      groupManager.sendTo(fromNode, L2StateMessage.createResultConflictMessage(msg, winner, state.getState()));
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
