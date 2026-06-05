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

import com.tc.async.api.StageManager;
import com.tc.async.impl.StageController;
import com.tc.async.impl.StageManagerImpl;
import com.tc.l2.ha.RandomWeightGenerator;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupManager;
import com.tc.objectserver.core.impl.ManagementTopologyEventCollector;
import com.tc.objectserver.impl.TopologyManager;
import com.tc.objectserver.persistence.ServerPersistentState;
import com.tc.util.concurrent.QueueFactory;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;


import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for replica protection logic added in commit e9f4287aa4.
 * Verifies that replicas cannot participate in elections and are properly protected.
 */
public class StateManagerReplicaProtectionTest {

  private StateManagerImpl stateManager;
  private GroupManager<AbstractGroupMessage> groupManager;
  private StageController stageController;
  private ManagementTopologyEventCollector mgmtController;
  private StageManager stageManager;
  private WeightGeneratorFactory weightGeneratorFactory;
  private ConsistencyManager consistencyManager;
  private ServerPersistentState statePersistor;
  private TopologyManager topologyManager;
  private Logger logger;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    logger = mock(Logger.class);
    groupManager = mock(GroupManager.class);
    stageController = mock(StageController.class);
    mgmtController = mock(ManagementTopologyEventCollector.class);
    stageManager = new StageManagerImpl(new ThreadGroup("test"), new QueueFactory());
    weightGeneratorFactory = RandomWeightGenerator.createTestingFactory(2);
    consistencyManager = mock(ConsistencyManager.class);
    statePersistor = mock(ServerPersistentState.class);
    topologyManager = mock(TopologyManager.class);

    ServerID localNode = new ServerID("test", "test".getBytes());
    when(groupManager.getLocalNodeID()).thenReturn(localNode);
    when(statePersistor.isDBClean()).thenReturn(true);
    when(statePersistor.getInitialMode()).thenReturn(ServerMode.INITIAL);
    when(topologyManager.getTopology()).thenReturn(mock(com.tc.objectserver.impl.Topology.class));
    when(consistencyManager.requestTransition(any(ServerMode.class), any(NodeID.class), any(ConsistencyManager.Transition.class))).thenReturn(true);
    when(consistencyManager.createVerificationEnrollment(any(NodeID.class), any(WeightGeneratorFactory.class)))
        .thenAnswer(i -> EnrollmentFactory.createTrumpEnrollment((NodeID)i.getArguments()[0], weightGeneratorFactory));
  }

  @Test
  public void testReplicaDoesNotParticipateInElection() throws Exception {
    // Set up state manager in REPLICA mode
    when(statePersistor.getInitialMode()).thenReturn(ServerMode.REPLICA);

    stateManager = new StateManagerImpl(logger, (n) -> true, groupManager, stageController,
        mgmtController, stageManager, 5, weightGeneratorFactory, consistencyManager,
        statePersistor, topologyManager);

    // Move to REPLICA mode
    stateManager.moveToReplicaMode();
    assertEquals(ServerMode.REPLICA_START, stateManager.getCurrentMode());

    // Verify REPLICA_START mode properties that prevent election participation
    assertFalse("Replica should not be able to start elections",
        stateManager.getCurrentMode().canStartElection());
    assertFalse("Replica should not be able to become active",
        stateManager.getCurrentMode().canBeActive());
  }

  @Test
  public void testReplicaStartModeProperties() throws Exception {
    // Set up state manager in REPLICA mode
    stateManager = new StateManagerImpl(logger, (n) -> true, groupManager, stageController,
        mgmtController, stageManager, 5, weightGeneratorFactory, consistencyManager,
        statePersistor, topologyManager);

    stateManager.moveToReplicaMode();

    // Verify REPLICA_START mode has correct properties
    assertEquals(ServerMode.REPLICA_START, stateManager.getCurrentMode());
    assertFalse("REPLICA_START should not be able to start elections",
        stateManager.getCurrentMode().canStartElection());
    assertFalse("REPLICA_START should not be able to become active",
        stateManager.getCurrentMode().canBeActive());
    assertFalse("REPLICA_START should not contain data",
        stateManager.getCurrentMode().containsData());
    assertTrue("REPLICA_START should be in startup mode",
        stateManager.getCurrentMode().isStartup());
    assertFalse("REPLICA_START should not require election",
        stateManager.getCurrentMode().requiresElection());
  }

  @Test
  public void testReplicaModeComparison() throws Exception {
    stateManager = new StateManagerImpl(logger, (n) -> true, groupManager, stageController,
        mgmtController, stageManager, 5, weightGeneratorFactory, consistencyManager,
        statePersistor, topologyManager);

    stateManager.moveToReplicaMode();

    // Verify REPLICA_START is different from other passive modes
    assertEquals(ServerMode.REPLICA_START, stateManager.getCurrentMode());
    assertNotEquals("REPLICA_START should be different from PASSIVE",
        ServerMode.PASSIVE, stateManager.getCurrentMode());
    assertNotEquals("REPLICA_START should be different from RELAY",
        ServerMode.RELAY, stateManager.getCurrentMode());
    assertNotEquals("REPLICA_START should be different from SYNCING",
        ServerMode.SYNCING, stateManager.getCurrentMode());
  }

  @Test
  public void testReplicaDoesNotStartElection() throws Exception {
    // Replicas should not participate in elections at all
    stateManager = new StateManagerImpl(logger, (n) -> true, groupManager, stageController,
        mgmtController, stageManager, 5, weightGeneratorFactory, consistencyManager,
        statePersistor, topologyManager);

    // Move to REPLICA_START mode
    stateManager.moveToReplicaMode();
    assertEquals(ServerMode.REPLICA_START, stateManager.getCurrentMode());

    // Verify replica mode doesn't allow election start
    assertFalse(stateManager.getCurrentMode().canStartElection());
  }

  @Test
  public void testReplicaCannotBeActive() throws Exception {
    // Test that REPLICA_START mode cannot become active
    stateManager = new StateManagerImpl(logger, (n) -> true, groupManager, stageController,
        mgmtController, stageManager, 5, weightGeneratorFactory, consistencyManager,
        statePersistor, topologyManager);

    stateManager.moveToReplicaMode();

    // Verify REPLICA_START mode cannot be active
    assertFalse(stateManager.getCurrentMode().canBeActive());

    // Verify it doesn't contain data (fresh start)
    assertFalse(stateManager.getCurrentMode().containsData());

    // Verify it's in startup mode
    assertTrue(stateManager.getCurrentMode().isStartup());
  }

  @Test
  public void testReplicaModeTransitionsCorrectly() throws Exception {
    stateManager = new StateManagerImpl(logger, (n) -> true, groupManager, stageController,
        mgmtController, stageManager, 5, weightGeneratorFactory, consistencyManager,
        statePersistor, topologyManager);

    // Test transition from INITIAL to RELAY
    assertEquals(ServerMode.INITIAL, stateManager.getCurrentMode());
    stateManager.moveToRelayMode();
    assertEquals(ServerMode.RELAY, stateManager.getCurrentMode());

    // Test that RELAY can transition to REPLICA_START
    stateManager.moveToReplicaMode();
    assertEquals(ServerMode.REPLICA_START, stateManager.getCurrentMode());

    // Verify RELAY and REPLICA_START share similar properties
    assertFalse("Both RELAY and REPLICA_START should not require elections",
        ServerMode.RELAY.requiresElection());
    assertFalse("Both RELAY and REPLICA_START should not require elections",
        ServerMode.REPLICA_START.requiresElection());
  }
}
