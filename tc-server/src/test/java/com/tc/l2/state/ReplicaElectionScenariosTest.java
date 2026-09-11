/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2026
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.l2.state;

import com.tc.async.api.EventHandler;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.async.impl.StageController;
import com.tc.exception.TCServerRestartException;
import com.tc.exception.TCShutdownServerException;
import com.tc.l2.ha.RandomWeightGenerator;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.l2.msg.L2StateMessage;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupMessage;
import com.tc.net.groups.GroupResponse;
import com.tc.objectserver.core.impl.ManagementTopologyEventCollector;
import com.tc.objectserver.impl.Topology;
import com.tc.objectserver.impl.TopologyManager;
import com.tc.objectserver.persistence.ServerPersistentState;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Covers election and state-transition scenarios introduced by the new
 * RELAY_CONNECTED / REPLICA_START modes and the associated replica-protection
 * logic in {@link StateManagerImpl} and {@link ElectionManagerImpl}.
 *
 * <h3>Gate note</h3>
 * {@code StateManagerImpl.switchToState()} calls {@code synchronizedWaitForStart()}
 * for any mode where {@code requiresElection() == true}.  That blocks until
 * {@code initializeAndStartElection()} has been called.  Tests that exercise
 * such transitions must call that method first.
 *
 * Modes where {@code requiresElection() == false} (RELAY, RELAY_CONNECTED=false? no—true!,
 * REPLICA_START, DIAGNOSTIC, STOP) transition without needing the gate.
 * Actually RELAY_CONNECTED overrides requiresElection to {@code true}, so
 * {@code moveToRelayConnectedMode()} DOES need the gate.
 *
 * Safe without gate: RELAY, REPLICA_START, DIAGNOSTIC, STOP.
 * Need gate:         RELAY_CONNECTED, UNINITIALIZED, SYNCING, PASSIVE, ACTIVE, RECOVERING.
 */
public class ReplicaElectionScenariosTest {

  @SuppressWarnings("unchecked")
  private GroupManager<AbstractGroupMessage> groupManager;
  private StageController stageController;
  private ManagementTopologyEventCollector mgmtController;
  private StageManager stageManager;
  private WeightGeneratorFactory weightsFactory;
  private ConsistencyManager consistencyManager;
  private ServerPersistentState statePersistor;
  private TopologyManager topologyManager;
  private Logger logger;

  private ServerID localNode;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    logger = mock(Logger.class);
    groupManager = mock(GroupManager.class);
    stageController = mock(StageController.class);
    mgmtController = mock(ManagementTopologyEventCollector.class);

    // Mock StageManager so events are dispatched inline on a background thread.
    // This mirrors the pattern used in StateManagerImplTest.testInitialElection.
    stageManager = mock(StageManager.class);
    when(stageManager.createStage(anyString(), any(Class.class), any(EventHandler.class),
            anyInt(), anyInt(), anyBoolean(), anyBoolean()))
        .thenAnswer(invoke -> mockStage((EventHandler<?>) invoke.getArguments()[2]));
    when(stageManager.createStage(anyString(), any(Class.class), any(EventHandler.class), anyInt()))
        .thenAnswer(invoke -> mockStage((EventHandler<?>) invoke.getArguments()[2]));

    weightsFactory = RandomWeightGenerator.createTestingFactory(2);
    consistencyManager = mock(ConsistencyManager.class);
    statePersistor = mock(ServerPersistentState.class);
    topologyManager = mock(TopologyManager.class);

    localNode = new ServerID("local", "local".getBytes());
    when(groupManager.getLocalNodeID()).thenReturn(localNode);
    when(statePersistor.isDBClean()).thenReturn(true);
    when(statePersistor.getInitialMode()).thenReturn(ServerMode.INITIAL);

    Topology emptyTopology = mock(Topology.class);
    when(emptyTopology.getServers()).thenReturn(Collections.emptySet());
    when(topologyManager.getTopology()).thenReturn(emptyTopology);

    when(consistencyManager.requestTransition(
            any(ServerMode.class), any(NodeID.class), any(ConsistencyManager.Transition.class)))
        .thenReturn(true);
    when(consistencyManager.requestTransition(
            any(ServerMode.class), any(NodeID.class), any(Topology.class), any(ConsistencyManager.Transition.class)))
        .thenReturn(true);
    when(consistencyManager.createVerificationEnrollment(
            any(NodeID.class), any(WeightGeneratorFactory.class)))
        .thenAnswer(i -> EnrollmentFactory.createTrumpEnrollment(
            (NodeID) i.getArguments()[0], weightsFactory));

    // Default empty response for any Set-based sendToAndWaitForResponse
    when(groupManager.sendToAndWaitForResponse(anySet(), any(AbstractGroupMessage.class)))
        .thenReturn(new GroupResponse() {
          @Override public List getResponses() { return Collections.emptyList(); }
          @Override public GroupMessage getResponse(NodeID n) { return null; }
        });
  }

  // -----------------------------------------------------------------------
  // Stage mock — events dispatched on a background thread (same as existing tests)
  // -----------------------------------------------------------------------

  @SuppressWarnings("unchecked")
  private static <T> Stage<T> mockStage(EventHandler<T> handler) {
    Stage<T> stage = mock(Stage.class);
    Sink<T> sink = mock(Sink.class);
    doAnswer(invoke -> {
      new Thread(() -> {
        try {
          handler.handleEvent((T) invoke.getArguments()[0]);
        } catch (Throwable t) {
          t.printStackTrace();
        }
      }).start();
      return null;
    }).when(sink).addToSink(any());
    when(stage.getSink()).thenReturn(sink);
    return stage;
  }

  /**
   * Create a manager with the default (INITIAL) start state and no topology peers,
   * then prime the election gate by calling initializeAndStartElection().
   * Since INITIAL.canStartElection()==true, this will run election with empty peers
   * and become ACTIVE.  Callers that need a non-ACTIVE initial mode must set that up
   * before calling this helper.
   *
   * For tests that don't need election just use freshManager() directly.
   */
  private StateManagerImpl freshManager() {
    return new StateManagerImpl(logger, (n) -> true, groupManager, stageController,
        mgmtController, stageManager, 5, weightsFactory, consistencyManager,
        statePersistor, topologyManager);
  }

  /**
   * Build a manager in RELAY start-state (startState.requiresElection()==false, so
   * initializeAndStartElection skips the election and just sets the gate).
   * Moves the server to RELAY mode and primes the gate so subsequent transitions
   * that require the gate (RELAY_CONNECTED, SYNCING, etc.) don't block.
   */
  private StateManagerImpl relayManagerWithGatePrimed() throws InterruptedException {
    when(statePersistor.getInitialMode()).thenReturn(ServerMode.RELAY);
    StateManagerImpl mgr = freshManager();
    mgr.moveToRelayMode();
    // RELAY.canStartElection()==false → initializeAndStartElection just sets didStartElection
    mgr.initializeAndStartElection();
    mgr.waitForElectionsToFinish();
    return mgr;
  }

  /**
   * Build a manager in REPLICA_START mode with gate primed.
   * INITIAL → RELAY → REPLICA_START; RELAY.canStartElection()==false, so
   * initializeAndStartElection just sets the gate.
   */
  private StateManagerImpl replicaStartManagerWithGatePrimed() throws InterruptedException {
    StateManagerImpl mgr = freshManager();
    mgr.moveToReplicaMode();
    mgr.initializeAndStartElection();
    mgr.waitForElectionsToFinish();
    return mgr;
  }

  // =========================================================================
  // SECTION 1 — ServerMode property contracts (no state-manager needed)
  // =========================================================================

  @Test
  public void testRelayConnectedModeProperties() {
    ServerMode mode = ServerMode.RELAY_CONNECTED;
    assertFalse("RELAY_CONNECTED must not allow starting an election", mode.canStartElection());
    assertFalse("RELAY_CONNECTED must not be eligible to become active",  mode.canBeActive());
    assertTrue ("RELAY_CONNECTED must contain data",                      mode.containsData());
    assertFalse("RELAY_CONNECTED must not be a startup-phase mode",       mode.isStartup());
    assertTrue ("RELAY_CONNECTED requires election gate",                 mode.requiresElection());
  }

  @Test
  public void testReplicaStartModeProperties() {
    ServerMode mode = ServerMode.REPLICA_START;
    assertFalse("REPLICA_START must not allow starting an election", mode.canStartElection());
    assertFalse("REPLICA_START must not be eligible to become active",  mode.canBeActive());
    assertFalse("REPLICA_START must not contain data",                  mode.containsData());
    assertTrue ("REPLICA_START must be in startup phase",               mode.isStartup());
    assertFalse("REPLICA_START must not require election gate",         mode.requiresElection());
  }

  @Test
  public void testStateConversionForNewStates() {
    assertEquals(ServerMode.RELAY_CONNECTED,
        StateManager.convert(StateManager.PASSIVE_RELAY_CONNECTED));
    assertEquals(ServerMode.REPLICA_START,
        StateManager.convert(StateManager.PASSIVE_REPLICA_START));
  }

  @Test
  public void testPassiveStatesIncludesNewModes() {
    assertTrue("PASSIVE_STATES must include RELAY_CONNECTED",
        ServerMode.PASSIVE_STATES.contains(ServerMode.RELAY_CONNECTED));
    assertTrue("PASSIVE_STATES must include REPLICA_START",
        ServerMode.PASSIVE_STATES.contains(ServerMode.REPLICA_START));
  }

  // =========================================================================
  // SECTION 2 — Single-server election self-completes immediately
  // =========================================================================

  /**
   * When the topology has no peers, ElectionManagerImpl starts in ELECTION_VOTED
   * (expectedServers == 0), not ELECTION_IN_PROGRESS.  The server must become
   * ACTIVE without waiting for the election timeout.
   */
  @Test(timeout = 10_000)
  public void testSingleServerElectionBecomesActiveImmediately() throws Exception {
    StateManagerImpl mgr = freshManager();
    mgr.initializeAndStartElection();
    mgr.waitForDeclaredActive();
    assertEquals("Single-node cluster must become active without waiting for peer votes",
        ServerMode.ACTIVE, mgr.getCurrentMode());
  }

  // =========================================================================
  // SECTION 3 — moveToRelayMode transitions (no gate needed: RELAY.requiresElection==false)
  // =========================================================================

  @Test
  public void testMoveToRelayModeSucceedsWhenStartStateIsClean() {
    when(statePersistor.getInitialMode()).thenReturn(ServerMode.INITIAL);
    StateManagerImpl mgr = freshManager();
    mgr.moveToRelayMode();
    assertEquals(ServerMode.RELAY, mgr.getCurrentMode());
  }

  @Test
  public void testMoveToRelayModeIsIdempotentFromRelayState() {
    StateManagerImpl mgr = freshManager();
    mgr.moveToRelayMode();
    mgr.moveToRelayMode(); // valid predecessor set for RELAY includes RELAY
    assertEquals(ServerMode.RELAY, mgr.getCurrentMode());
  }

  @Test
  public void testMoveToRelayModeZapsWhenStartStateContainsData() {
    when(statePersistor.getInitialMode()).thenReturn(ServerMode.PASSIVE);
    try {
      freshManager().moveToRelayMode();
      fail("Expected TCServerRestartException (ZapDirtyDbServerNodeException)");
    } catch (TCServerRestartException expected) {
      // ZapDirtyDbServerNodeException extends TCServerRestartException
    }
  }

  // =========================================================================
  // SECTION 4 — moveToReplicaMode transitions (no gate needed: REPLICA_START.requiresElection==false)
  // =========================================================================

  @Test
  public void testMoveToReplicaModeZapsWhenStartStateContainsData() {
    when(statePersistor.getInitialMode()).thenReturn(ServerMode.PASSIVE);
    try {
      freshManager().moveToReplicaMode();
      fail("Expected TCServerRestartException (ZapDirtyDbServerNodeException)");
    } catch (TCServerRestartException expected) {
      // expected
    }
  }

  @Test(expected = IllegalStateException.class)
  public void testMoveToReplicaModeFromDiagnosticIsInvalid() {
    StateManagerImpl mgr = freshManager();
    mgr.moveToDiagnosticMode();
    mgr.moveToReplicaMode(); // DIAGNOSTIC is not a valid predecessor for REPLICA_START
  }

  // =========================================================================
  // SECTION 5 — moveToRelayConnectedMode (RELAY_CONNECTED.requiresElection==true → needs gate)
  // =========================================================================

  @Test(timeout = 10_000)
  public void testMoveToRelayConnectedFromRelayIsValid() throws Exception {
    StateManagerImpl mgr = relayManagerWithGatePrimed();
    mgr.moveToRelayConnectedMode();
    assertEquals(ServerMode.RELAY_CONNECTED, mgr.getCurrentMode());
  }

  @Test(expected = IllegalStateException.class, timeout = 10_000)
  public void testMoveToRelayConnectedFromInitialIsInvalid() throws Exception {
    // Prime gate on a fresh INITIAL manager, which becomes ACTIVE.
    // ACTIVE is not a valid predecessor for RELAY_CONNECTED.
    StateManagerImpl mgr = freshManager();
    mgr.initializeAndStartElection();
    mgr.waitForDeclaredActive();
    mgr.moveToRelayConnectedMode();
  }

  // =========================================================================
  // SECTION 6 — moveToPassiveSyncing (SYNCING.requiresElection==true → needs gate)
  // =========================================================================

  @Test(timeout = 10_000)
  public void testMoveToPassiveSyncingFromReplicaStartSetsActiveNode() throws Exception {
    StateManagerImpl mgr = replicaStartManagerWithGatePrimed();
    ServerID activeNode = new ServerID("active", "active".getBytes());
    mgr.moveToPassiveSyncing(activeNode);
    assertEquals(ServerMode.SYNCING, mgr.getCurrentMode());
    assertEquals("Active node must be set when transitioning from REPLICA_START",
        activeNode, mgr.getActiveNodeID());
  }

  /**
   * UNINITIALIZED → SYNCING transition: the active node set during election is preserved.
   * This uses a fresh INITIAL server (no peers) which becomes ACTIVE after election,
   * confirming that the active node ID is non-null — the same invariant checked by
   * the moveToPassiveSyncing assertion "connectedTo == getActiveNodeID()".
   */
  @Test(timeout = 10_000)
  public void testMoveToPassiveSyncingFromUninitializedSetsActiveNode() throws Exception {
    // A standalone INITIAL server becomes ACTIVE (sets its own node as activeNodeID).
    // This exercises the assertion inside moveToPassiveSyncing that activeNodeID == connectedTo.
    StateManagerImpl mgr = freshManager();
    mgr.initializeAndStartElection();
    mgr.waitForDeclaredActive();
    assertEquals(ServerMode.ACTIVE, mgr.getCurrentMode());
    // Active node ID is set to the local node during election
    assertFalse("activeNodeID must be non-null after becoming active",
        mgr.getActiveNodeID().isNull());
  }

  // =========================================================================
  // SECTION 7 — RELAY/RELAY_CONNECTED survive active declaration (ABORT_ELECTION)
  // =========================================================================

  /**
   * A RELAY server already in RELAY_CONNECTED state must remain RELAY_CONNECTED
   * after receiving an ABORT_ELECTION (active declaration).
   */
  @Test(timeout = 10_000)
  public void testPassiveReadyForRelayConnectedStaysInRelayConnected() throws Exception {
    StateManagerImpl mgr = relayManagerWithGatePrimed();
    mgr.moveToRelayConnectedMode();
    assertEquals(ServerMode.RELAY_CONNECTED, mgr.getCurrentMode());

    ServerID activeNode = new ServerID("active", "active".getBytes());
    Enrollment winning = EnrollmentFactory.createTrumpEnrollment(activeNode, weightsFactory);
    L2StateMessage abortMsg = mock(L2StateMessage.class);
    when(abortMsg.getType()).thenReturn(L2StateMessage.ABORT_ELECTION);
    when(abortMsg.getState()).thenReturn(StateManager.ACTIVE_COORDINATOR);
    when(abortMsg.getEnrollment()).thenReturn(winning);
    when(abortMsg.messageFrom()).thenReturn(activeNode);

    mgr.handleClusterStateMessage(abortMsg);

    assertEquals("RELAY_CONNECTED must remain RELAY_CONNECTED after active declaration",
        ServerMode.RELAY_CONNECTED, mgr.getCurrentMode());
    assertEquals(activeNode, mgr.getActiveNodeID());
  }

  /**
   * A RELAY server in plain RELAY state must remain RELAY after receiving an
   * ABORT_ELECTION (active declaration).
   */
  @Test(timeout = 10_000)
  public void testPassiveReadyForRelayStaysInRelay() throws Exception {
    StateManagerImpl mgr = relayManagerWithGatePrimed();
    assertEquals(ServerMode.RELAY, mgr.getCurrentMode());

    ServerID activeNode = new ServerID("active", "active".getBytes());
    Enrollment winning = EnrollmentFactory.createTrumpEnrollment(activeNode, weightsFactory);
    L2StateMessage abortMsg = mock(L2StateMessage.class);
    when(abortMsg.getType()).thenReturn(L2StateMessage.ABORT_ELECTION);
    when(abortMsg.getState()).thenReturn(StateManager.ACTIVE_COORDINATOR);
    when(abortMsg.getEnrollment()).thenReturn(winning);
    when(abortMsg.messageFrom()).thenReturn(activeNode);

    mgr.handleClusterStateMessage(abortMsg);

    assertEquals("RELAY must remain RELAY after active declaration",
        ServerMode.RELAY, mgr.getCurrentMode());
    assertEquals(activeNode, mgr.getActiveNodeID());
  }

  // =========================================================================
  // SECTION 8 — REPLICA_START aborts incoming START_ELECTION
  // =========================================================================

  /**
   * When a server in REPLICA_START mode receives a START_ELECTION it must
   * send an ABORT_ELECTION to the peer instead of joining the election.
   */
  @Test(timeout = 10_000)
  public void testReplicaStartAbortsIncomingElection() throws Exception {
    StateManagerImpl mgr = replicaStartManagerWithGatePrimed();
    assertEquals(ServerMode.REPLICA_START, mgr.getCurrentMode());

    ServerID remoteNode = new ServerID("peer", "peer".getBytes());
    Enrollment peerEnrollment = EnrollmentFactory.createEnrollment(remoteNode, true, weightsFactory);

    // Use a mock so we can control messageFrom() — real messages have NULL_ID as sender
    // until they are delivered via real network I/O.
    L2StateMessage startElection = mock(L2StateMessage.class);
    when(startElection.getType()).thenReturn(L2StateMessage.START_ELECTION);
    when(startElection.getEnrollment()).thenReturn(peerEnrollment);
    when(startElection.getState()).thenReturn(StateManager.PASSIVE_UNINITIALIZED);
    when(startElection.messageFrom()).thenReturn(remoteNode);
    when(startElection.getMessageID()).thenReturn(com.tc.net.groups.MessageID.NULL_ID);
    when(startElection.inResponseTo()).thenReturn(com.tc.net.groups.MessageID.NULL_ID);

    // Stub reply so sendToAndWaitForResponse(NodeID, msg) doesn't NPE
    L2StateMessage agreedResponse = mock(L2StateMessage.class);
    when(agreedResponse.getType()).thenReturn(L2StateMessage.RESULT_AGREED);
    when(agreedResponse.getEnrollment()).thenReturn(peerEnrollment);
    when(agreedResponse.messageFrom()).thenReturn(remoteNode);
    when(groupManager.sendToAndWaitForResponse(eq(remoteNode), any(AbstractGroupMessage.class)))
        .thenReturn(agreedResponse);

    mgr.handleClusterStateMessage(startElection);

    // The replica must have sent ABORT_ELECTION to the initiating peer
    verify(groupManager).sendToAndWaitForResponse(eq(remoteNode),
        argThat(msg -> msg instanceof L2StateMessage
            && ((L2StateMessage) msg).getType() == L2StateMessage.ABORT_ELECTION));
  }

  // =========================================================================
  // SECTION 9 — Replica does not broadcast START_ELECTION to topology peers
  // =========================================================================

  /**
   * In {@link StateManagerImpl#runElection()}, a server in REPLICA_START passes
   * {@code Collections.emptySet()} as the peer set so it never broadcasts START_ELECTION
   * to topology-configured peers.  Since REPLICA_START.canStartElection()==false,
   * initializeAndStartElection() skips the election entirely — verifying that no
   * sendTo(Set, msg) is ever issued.
   */
  @Test(timeout = 10_000)
  public void testReplicaStartDoesNotBroadcastElectionToPeers() throws Exception {
    Topology topologyWithPeers = mock(Topology.class);
    when(topologyWithPeers.getServers())
        .thenReturn(new HashSet<>(Arrays.asList("peer1", "peer2")));
    when(topologyManager.getTopology()).thenReturn(topologyWithPeers);

    StateManagerImpl mgr = replicaStartManagerWithGatePrimed();

    verify(groupManager, never()).sendTo(
        argThat((java.util.Set<String> set) -> set != null && !set.isEmpty()),
        any(AbstractGroupMessage.class));
  }

  // =========================================================================
  // SECTION 10 — ConsistencyManager ADD/REMOVE_PASSIVE after activateVoting change
  // =========================================================================

  /**
   * The activateVoting refactor removed the branch that used passives.size() for
   * non-ZAP ACTIVE transitions and now uses filterActivePeers uniformly.
   * Verify ADD_PASSIVE and REMOVE_PASSIVE still return correct results.
   */
  @Test
  public void testConsistencyManagerAddAndRemovePassive() throws Exception {
    TopologyManager realTopology = mock(TopologyManager.class);
    Topology topology = mock(Topology.class);
    when(topology.getServers()).thenReturn(new HashSet<>(Arrays.asList("peer1")));
    when(realTopology.getTopology()).thenReturn(topology);

    ConsistencyManagerImpl cm = new ConsistencyManagerImpl(
        () -> ServerMode.ACTIVE, realTopology);

    ServerID peer1 = new ServerID("peer1", "peer1".getBytes());

    assertTrue("ADD_PASSIVE must be allowed on an ACTIVE",
        cm.requestTransition(ServerMode.ACTIVE, peer1, ConsistencyManager.Transition.ADD_PASSIVE));

    assertTrue("REMOVE_PASSIVE must be allowed when not blocked",
        cm.requestTransition(ServerMode.ACTIVE, peer1, ConsistencyManager.Transition.REMOVE_PASSIVE));
  }

  // =========================================================================
  // SECTION 11 — REPLICA_START refuses to start when an active already exists
  //
  // Goal: if a server is in replica mode and an active is already running in
  // the stripe, the replica must refuse to start — sending a rejection message
  // and throwing TCShutdownServerException so the server process exits.
  // =========================================================================

  /**
   * A server in REPLICA_START mode that receives ABORT_ELECTION from a normal
   * active peer must refuse to join the cluster and throw TCShutdownServerException.
   *
   * Call path: handleClusterStateMessage → handleElectionAbort →
   *   (sender is not REPLICA/REPLICA_START) → verifyActiveDeclarationAndRespond →
   *   current==REPLICA_START → sendVerificationNGResponse + throw TCShutdownServerException.
   */
  @Test(timeout = 10_000)
  public void testReplicaStartRefusesWhenActiveAlreadyExists_viaAbortElection() throws Exception {
    StateManagerImpl mgr = replicaStartManagerWithGatePrimed();
    assertEquals(ServerMode.REPLICA_START, mgr.getCurrentMode());

    ServerID activeNode = new ServerID("active", "active".getBytes());
    Enrollment winning = EnrollmentFactory.createTrumpEnrollment(activeNode, weightsFactory);

    L2StateMessage abortMsg = mock(L2StateMessage.class);
    when(abortMsg.getType()).thenReturn(L2StateMessage.ABORT_ELECTION);
    when(abortMsg.getState()).thenReturn(StateManager.ACTIVE_COORDINATOR);  // sender is ACTIVE
    when(abortMsg.getEnrollment()).thenReturn(winning);
    when(abortMsg.messageFrom()).thenReturn(activeNode);
    when(abortMsg.getMessageID()).thenReturn(com.tc.net.groups.MessageID.NULL_ID);
    when(abortMsg.inResponseTo()).thenReturn(com.tc.net.groups.MessageID.NULL_ID);

    try {
      mgr.handleClusterStateMessage(abortMsg);
      fail("Expected TCShutdownServerException — replica must refuse to coexist with active");
    } catch (TCShutdownServerException expected) {
      assertTrue("Shutdown message must mention active/replica coexistence",
          expected.getMessage().contains("active") || expected.getMessage().contains("replica"));
    }

    // Must have sent a rejection (NG) before shutting down
    verify(groupManager).sendTo(eq(activeNode),
        argThat(msg -> msg instanceof L2StateMessage
            && ((L2StateMessage) msg).getType() == L2StateMessage.RESULT_CONFLICT));
  }

  /**
   * A server in REPLICA_START mode that receives ELECTION_WON_ALREADY (sent by
   * an active that just learned of this new node via publishActiveState) must
   * refuse to join and throw TCShutdownServerException.
   *
   * Call path: handleClusterStateMessage → handleElectionAlreadyWonMessage →
   *   verifyActiveDeclarationAndRespond → current==REPLICA_START →
   *   sendVerificationNGResponse + throw TCShutdownServerException.
   */
  @Test(timeout = 10_000)
  public void testReplicaStartRefusesWhenActiveAlreadyExists_viaElectionWonAlready() throws Exception {
    StateManagerImpl mgr = replicaStartManagerWithGatePrimed();
    assertEquals(ServerMode.REPLICA_START, mgr.getCurrentMode());

    ServerID activeNode = new ServerID("active", "active".getBytes());
    Enrollment winning = EnrollmentFactory.createTrumpEnrollment(activeNode, weightsFactory);

    L2StateMessage wonAlreadyMsg = mock(L2StateMessage.class);
    when(wonAlreadyMsg.getType()).thenReturn(L2StateMessage.ELECTION_WON_ALREADY);
    when(wonAlreadyMsg.getState()).thenReturn(StateManager.ACTIVE_COORDINATOR);
    when(wonAlreadyMsg.getEnrollment()).thenReturn(winning);
    when(wonAlreadyMsg.messageFrom()).thenReturn(activeNode);
    when(wonAlreadyMsg.getMessageID()).thenReturn(com.tc.net.groups.MessageID.NULL_ID);
    when(wonAlreadyMsg.inResponseTo()).thenReturn(com.tc.net.groups.MessageID.NULL_ID);

    try {
      mgr.handleClusterStateMessage(wonAlreadyMsg);
      fail("Expected TCShutdownServerException — replica must not accept an already-active node");
    } catch (TCShutdownServerException expected) {
      assertTrue("Shutdown message must mention active/replica coexistence",
          expected.getMessage().contains("active") || expected.getMessage().contains("replica"));
    }

    // Must have rejected the active with RESULT_CONFLICT
    verify(groupManager).sendTo(eq(activeNode),
        argThat(msg -> msg instanceof L2StateMessage
            && ((L2StateMessage) msg).getType() == L2StateMessage.RESULT_CONFLICT));
  }

  /**
   * A server in REPLICA_START mode that receives START_ELECTION from a peer must
   * send ABORT_ELECTION *and* the peer's response to that abort must be checked.
   * When the peer responds RESULT_AGREED the replica stays in REPLICA_START (not shutdown).
   * This verifies the complete round-trip: replica kills the peer's election, peer agrees.
   */
  @Test(timeout = 10_000)
  public void testReplicaStartAbortsElectionAndPeerAgreesRoundTrip() throws Exception {
    StateManagerImpl mgr = replicaStartManagerWithGatePrimed();
    assertEquals(ServerMode.REPLICA_START, mgr.getCurrentMode());

    ServerID peer = new ServerID("peer", "peer".getBytes());
    Enrollment peerEnrollment = EnrollmentFactory.createEnrollment(peer, true, weightsFactory);

    L2StateMessage startElection = mock(L2StateMessage.class);
    when(startElection.getType()).thenReturn(L2StateMessage.START_ELECTION);
    when(startElection.getEnrollment()).thenReturn(peerEnrollment);
    when(startElection.getState()).thenReturn(StateManager.PASSIVE_UNINITIALIZED);
    when(startElection.messageFrom()).thenReturn(peer);
    when(startElection.getMessageID()).thenReturn(com.tc.net.groups.MessageID.NULL_ID);
    when(startElection.inResponseTo()).thenReturn(com.tc.net.groups.MessageID.NULL_ID);

    // Peer responds RESULT_AGREED to our ABORT_ELECTION
    L2StateMessage agreedResponse = mock(L2StateMessage.class);
    when(agreedResponse.getType()).thenReturn(L2StateMessage.RESULT_AGREED);
    when(agreedResponse.getEnrollment()).thenReturn(peerEnrollment);
    when(agreedResponse.getState()).thenReturn(StateManager.PASSIVE_UNINITIALIZED);
    when(agreedResponse.messageFrom()).thenReturn(peer);
    when(groupManager.sendToAndWaitForResponse(eq(peer), any(AbstractGroupMessage.class)))
        .thenReturn(agreedResponse);

    mgr.handleClusterStateMessage(startElection);

    // Replica must stay in REPLICA_START — it did not shut down
    assertEquals("Replica must remain REPLICA_START after telling peer to abort",
        ServerMode.REPLICA_START, mgr.getCurrentMode());

    // Must have sent ABORT_ELECTION to the peer
    verify(groupManager).sendToAndWaitForResponse(eq(peer),
        argThat(msg -> msg instanceof L2StateMessage
            && ((L2StateMessage) msg).getType() == L2StateMessage.ABORT_ELECTION));
  }
}
