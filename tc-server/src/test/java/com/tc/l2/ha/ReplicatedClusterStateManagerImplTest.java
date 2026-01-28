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
import org.junit.Before;
import org.junit.Test;

import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
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
}
