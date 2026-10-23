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
package com.tc.l2.ha;

import com.tc.config.ServerConfigurationManager;
import com.tc.l2.msg.ClusterStateMessage;
import com.tc.l2.state.ServerMode;
import com.tc.net.NodeID;
import com.tc.net.StripeID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;

import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for ReplicatedClusterStateManagerImpl, specifically testing the fix for issue #1389
 * which removes the race condition between publishing state and shutdown.
 *
 * @author mscott
 */
public class ReplicatedClusterStateManagerImplTest {

  private GroupManager<AbstractGroupMessage> groupManager;
  private ClusterState clusterState;
  private ServerConfigurationManager configurationProvider;
  private Supplier<ServerMode> currentModeSupplier;
  private NodeID targetNode;
  private ReplicatedClusterStateManagerImpl manager;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    groupManager = mock(GroupManager.class);
    clusterState = mock(ClusterState.class);
    configurationProvider = mock(ServerConfigurationManager.class);
    currentModeSupplier = mock(Supplier.class);
    targetNode = mock(NodeID.class);
  }

  /**
   * Test that publishClusterState sends a message when the server is in ACTIVE mode.
   * This is the expected behavior when the server is properly active.
   */
  @Test
  public void testPublishClusterStateWhenActive() throws GroupException {
    // Given: Server is in ACTIVE mode
    when(currentModeSupplier.get()).thenReturn(ServerMode.ACTIVE);
    when(configurationProvider.getSyncData()).thenReturn(new byte[]{1, 2, 3});

    // Mock StripeID to avoid NullPointerException in ClusterStateMessage
    StripeID stripeID = mock(StripeID.class);
    when(stripeID.getName()).thenReturn("stripe-1");
    when(clusterState.getStripeID()).thenReturn(stripeID);

    manager = new ReplicatedClusterStateManagerImpl(
        groupManager,
        currentModeSupplier,
        clusterState,
        configurationProvider
    );

    // When: publishClusterState is called
    manager.publishClusterState(targetNode);

    // Then: Configuration sync data should be set and message should be sent
    verify(clusterState, times(1)).setConfigSyncData(any(byte[].class));
    verify(groupManager, times(1)).sendTo(eq(targetNode), any(ClusterStateMessage.class));
  }

  /**
   * Test that publishClusterState does NOT send a message when the server is in STOP mode.
   * This tests the fix for issue #1389 - preventing the race condition during shutdown.
   */
  @Test
  public void testPublishClusterStateWhenStopping() throws GroupException {
    // Given: Server is in STOP mode (shutting down)
    when(currentModeSupplier.get()).thenReturn(ServerMode.STOP);

    manager = new ReplicatedClusterStateManagerImpl(
        groupManager,
        currentModeSupplier,
        clusterState,
        configurationProvider
    );

    // When: publishClusterState is called during shutdown
    manager.publishClusterState(targetNode);

    // Then: No configuration sync data should be set and no message should be sent
    verify(clusterState, never()).setConfigSyncData(any(byte[].class));
    verify(groupManager, never()).sendTo(eq(targetNode), any(ClusterStateMessage.class));
  }

  /**
   * Test that publishClusterState does NOT send a message when the server is in PASSIVE mode.
   * This ensures the fix works for all non-ACTIVE states.
   */
  @Test
  public void testPublishClusterStateWhenPassive() throws GroupException {
    // Given: Server is in PASSIVE mode
    when(currentModeSupplier.get()).thenReturn(ServerMode.PASSIVE);

    manager = new ReplicatedClusterStateManagerImpl(
        groupManager,
        currentModeSupplier,
        clusterState,
        configurationProvider
    );

    // When: publishClusterState is called while passive
    manager.publishClusterState(targetNode);

    // Then: No configuration sync data should be set and no message should be sent
    verify(clusterState, never()).setConfigSyncData(any(byte[].class));
    verify(groupManager, never()).sendTo(eq(targetNode), any(ClusterStateMessage.class));
  }

  /**
   * Test that publishClusterState does NOT send a message when the server is UNINITIALIZED.
   * This ensures the fix works during early server lifecycle.
   */
  @Test
  public void testPublishClusterStateWhenUninitialized() throws GroupException {
    // Given: Server is in UNINITIALIZED mode
    when(currentModeSupplier.get()).thenReturn(ServerMode.UNINITIALIZED);

    manager = new ReplicatedClusterStateManagerImpl(
        groupManager,
        currentModeSupplier,
        clusterState,
        configurationProvider
    );

    // When: publishClusterState is called before initialization
    manager.publishClusterState(targetNode);

    // Then: No configuration sync data should be set and no message should be sent
    verify(clusterState, never()).setConfigSyncData(any(byte[].class));
    verify(groupManager, never()).sendTo(eq(targetNode), any(ClusterStateMessage.class));
  }

  /**
   * Test the race condition scenario: server transitions from ACTIVE to STOP
   * between multiple publishClusterState calls.
   */
  @Test
  public void testPublishClusterStateRaceConditionScenario() throws GroupException {
    // Given: Server starts in ACTIVE mode
    when(currentModeSupplier.get()).thenReturn(ServerMode.ACTIVE);
    when(configurationProvider.getSyncData()).thenReturn(new byte[]{1, 2, 3});

    // Mock StripeID to avoid NullPointerException in ClusterStateMessage
    StripeID stripeID = mock(StripeID.class);
    when(stripeID.getName()).thenReturn("stripe-1");
    when(clusterState.getStripeID()).thenReturn(stripeID);

    manager = new ReplicatedClusterStateManagerImpl(
        groupManager,
        currentModeSupplier,
        clusterState,
        configurationProvider
    );

    // When: First call succeeds while ACTIVE
    manager.publishClusterState(targetNode);

    // Then: Message should be sent
    verify(groupManager, times(1)).sendTo(eq(targetNode), any(ClusterStateMessage.class));

    // Given: Server transitions to STOP mode
    when(currentModeSupplier.get()).thenReturn(ServerMode.STOP);

    // When: Second call happens during shutdown
    manager.publishClusterState(targetNode);

    // Then: No additional message should be sent (still only 1 from before)
    verify(groupManager, times(1)).sendTo(eq(targetNode), any(ClusterStateMessage.class));
  }

  /**
   * Test that a COMPLETE_STATE cluster state message received while in REPLICA mode is NOT applied
   * to local cluster state. DR replica servers must not absorb cluster state from another cluster.
   */
  @Test
  public void testClusterStateMessageNotAppliedOnReplica() {
    // Given: Server is in REPLICA mode (DR replication scenario)
    when(currentModeSupplier.get()).thenReturn(ServerMode.REPLICA);

    manager = new ReplicatedClusterStateManagerImpl(
        groupManager,
        currentModeSupplier,
        clusterState,
        configurationProvider
    );
    when(clusterState.getStripeID()).thenReturn(new StripeID("test"));
    when(clusterState.getAllConnections()).thenReturn(Collections.emptySet());
    when(clusterState.getConfigSyncData()).thenReturn(new byte[0]);
    when(clusterState.getStartGlobalMessageID()).thenReturn(1L);
    when(clusterState.getNextAvailableChannelID()).thenReturn(1L);
    // Build a real COMPLETE_STATE message (no serialization needed — initMessage skipped,
    // we construct directly to avoid touching a real ClusterState for the sender side)
    ClusterStateMessage msg = ClusterStateMessage.createClusterStateMessage(clusterState);

    // When: The message is delivered as if received from a remote node
    manager.messageReceived(targetNode, msg);

    // Then: initState must never be called — the replica must not absorb cross-cluster state
    verify(clusterState, never()).setNextAvailableChannelID(anyLong());
    verify(clusterState, never()).addNewConnection(any());
    verify(clusterState, never()).setStripeID(any());
    verify(clusterState, never()).setConfigSyncData(any());
    verify(clusterState, never()).setStartGlobalMessageID(anyLong());
  }

  /**
   * Test that a NEW_CONNECTION_CREATED cluster state message received while in REPLICA mode
   * is NOT applied to local cluster state.
   */
  @Test
  public void testConnectionCreatedMessageNotAppliedOnReplica() {
    // Given: Server is in REPLICA mode
    when(currentModeSupplier.get()).thenReturn(ServerMode.REPLICA);

    manager = new ReplicatedClusterStateManagerImpl(
        groupManager,
        currentModeSupplier,
        clusterState,
        configurationProvider
    );

    com.tc.net.protocol.transport.ConnectionID connID =
        mock(com.tc.net.protocol.transport.ConnectionID.class);
    ClusterStateMessage msg = ClusterStateMessage.createNewConnectionCreatedMessage(connID);

    // When: The message is delivered
    manager.messageReceived(targetNode, msg);

    // Then: The connection must not be added to local state
    verify(clusterState, never()).addNewConnection(any());
  }

  /**
   * Test that a COMPLETE_STATE cluster state message received while in REPLICA_START mode
   * is NOT applied to local cluster state.
   */
  @Test
  public void testClusterStateMessageNotAppliedOnReplicaStart() {
    // Given: Server is in REPLICA_START mode (DR replication, before fully connected)
    when(currentModeSupplier.get()).thenReturn(ServerMode.REPLICA_START);

    manager = new ReplicatedClusterStateManagerImpl(
        groupManager,
        currentModeSupplier,
        clusterState,
        configurationProvider
    );

    when(clusterState.getStripeID()).thenReturn(new StripeID("test"));
    when(clusterState.getAllConnections()).thenReturn(Collections.emptySet());
    when(clusterState.getConfigSyncData()).thenReturn(new byte[0]);
    when(clusterState.getStartGlobalMessageID()).thenReturn(1L);
    when(clusterState.getNextAvailableChannelID()).thenReturn(1L);

    ClusterStateMessage msg = ClusterStateMessage.createClusterStateMessage(clusterState);

    // When: The message is delivered
    manager.messageReceived(targetNode, msg);

    // Then: initState must never be called on a REPLICA_START server
    verify(clusterState, never()).setNextAvailableChannelID(anyLong());
    verify(clusterState, never()).addNewConnection(any());
    verify(clusterState, never()).setStripeID(any());
    verify(clusterState, never()).setConfigSyncData(any());
    verify(clusterState, never()).setStartGlobalMessageID(anyLong());
  }

  /**
   * Confirm the inverse: a COMPLETE_STATE message received while in PASSIVE (standby) mode
   * IS applied to local state, as an in-cluster passive must sync from its active.
   */
  @Test
  public void testClusterStateMessageIsAppliedOnPassiveStandby() {
    // Given: Server is in PASSIVE (standby) mode
    when(currentModeSupplier.get()).thenReturn(ServerMode.PASSIVE);

    manager = new ReplicatedClusterStateManagerImpl(
        groupManager,
        currentModeSupplier,
        clusterState,
        configurationProvider
    );

    when(clusterState.getStripeID()).thenReturn(new StripeID("test"));
    when(clusterState.getAllConnections()).thenReturn(Collections.emptySet());
    when(clusterState.getConfigSyncData()).thenReturn(new byte[0]);
    when(clusterState.getStartGlobalMessageID()).thenReturn(1L);
    when(clusterState.getNextAvailableChannelID()).thenReturn(1L);

    ClusterStateMessage msg = ClusterStateMessage.createClusterStateMessage(clusterState);

    // When: The message is delivered from the in-cluster active
    manager.messageReceived(targetNode, msg);

    // Then: The state IS synced — in-cluster passives do process the message
    verify(clusterState).syncSequenceState();
    verify(clusterState).setNextAvailableChannelID(anyLong());
    verify(clusterState, never()).addNewConnection(any());
    verify(clusterState).setStripeID(any());
    verify(clusterState).setConfigSyncData(any());
    verify(clusterState).setStartGlobalMessageID(anyLong());
    verify(configurationProvider).sync(any());
  }
}
