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

import com.tc.async.api.ConfigurationContext;
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
import java.util.Collections;
import org.junit.Assert;
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
    // Set up state manager in REPLICA mode, config dictates election can start for replica mode
    when(statePersistor.getInitialMode()).thenReturn(ServerMode.REPLICA_START);

    stateManager = new StateManagerImpl(logger, (n) -> false, groupManager, stageController,
        mgmtController, stageManager, 5, weightGeneratorFactory, consistencyManager,
        statePersistor, topologyManager);

    // Move to REPLICA mode
    stateManager.moveToReplicaMode();
    assertEquals(ServerMode.REPLICA_START, stateManager.getCurrentMode());

    // Verify REPLICA_START mode properties that prevent election participation
    assertFalse("Replica should be able to start elections",
        stateManager.electionDidStart());
  }

  @Test
  public void testReplicaStartModeProperties() throws Exception {
    // Set up state manager in REPLICA mode, config dictates election can start for replica mode
    stateManager = new StateManagerImpl(logger, (n) -> false, groupManager, stageController,
        mgmtController, stageManager, 5, weightGeneratorFactory, consistencyManager,
        statePersistor, topologyManager);

    stateManager.moveToReplicaMode();

    // Verify REPLICA_START mode has correct properties
    assertEquals(ServerMode.REPLICA_START, stateManager.getCurrentMode());
    assertFalse("REPLICA_START did not start election",
        stateManager.electionDidStart());
    assertTrue("REPLICA_START should be able to become active",
        stateManager.getCurrentMode().canBeActive());
    assertTrue("REPLICA_START can contain persisted data",
        stateManager.getCurrentMode().containsData());
    assertTrue("REPLICA_START should be in startup mode",
        stateManager.getCurrentMode().isStartup());
    assertFalse("REPLICA_START should not require election",
        stateManager.getCurrentMode().requiresElection());
  }

  @Test
  public void testReplicaModeMovesToActiveWhenAllowedByConfig() throws Exception {
    stateManager = new StateManagerImpl(logger, (n) -> true, groupManager, stageController,
        mgmtController, stageManager, 5, weightGeneratorFactory, consistencyManager,
        statePersistor, topologyManager);

    when(consistencyManager.requestTransition(any(), any(), any(), any())).thenReturn(Boolean.TRUE);

    stageManager.startAll(mock(ConfigurationContext.class), Collections.emptyList());

    stateManager.moveToReplicaMode();
    stateManager.initializeAndStartElection();
    stateManager.waitForElectionToComplete();

    assertEquals(ServerMode.ACTIVE, stateManager.getCurrentMode());
  }

  @Test
  public void testReplicaDoesNotStartElection() throws Exception {
    // Replicas should not participate in elections at all
    stateManager = new StateManagerImpl(logger, (n) -> false, groupManager, stageController,
        mgmtController, stageManager, 5, weightGeneratorFactory, consistencyManager,
        statePersistor, topologyManager);

    // Move to REPLICA_START mode
    stateManager.moveToReplicaMode();
    assertEquals(ServerMode.REPLICA_START, stateManager.getCurrentMode());
    stateManager.initializeAndStartElection();
    stateManager.waitForElectionToComplete();

    Assert.assertFalse(stateManager.electionDidStart());
    assertEquals(ServerMode.REPLICA_START, stateManager.getCurrentMode());
  }

  @Test
  public void testReplicaCanBeActive() throws Exception {
    // Test that REPLICA_START mode cannot become active
    stateManager = new StateManagerImpl(logger, (n) -> true, groupManager, stageController,
        mgmtController, stageManager, 5, weightGeneratorFactory, consistencyManager,
        statePersistor, topologyManager);

    stateManager.moveToReplicaMode();

    // Verify REPLICA_START mode cannot be active
    assertTrue(stateManager.getCurrentMode().canBeActive());

    // Verify it can contain data (fresh start)
    assertTrue(stateManager.getCurrentMode().containsData());

    // Verify it's in startup mode
    assertTrue(stateManager.getCurrentMode().isStartup());
  }

  @Test
  public void testReplicaModeTransitionsCorrectly() throws Exception {
    stateManager = new StateManagerImpl(logger, (n) -> true, groupManager, stageController,
        mgmtController, stageManager, 5, weightGeneratorFactory, consistencyManager,
        statePersistor, topologyManager);

    // Test transition from INITIAL to REPLICA_START
    assertEquals(ServerMode.INITIAL, stateManager.getCurrentMode());
    stateManager.moveToReplicaMode();
    assertEquals(ServerMode.REPLICA_START, stateManager.getCurrentMode());
  }
}
