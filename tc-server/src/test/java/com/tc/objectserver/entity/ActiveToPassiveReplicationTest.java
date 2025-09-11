/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
package com.tc.objectserver.entity;

import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tc.async.api.Sink;
import com.tc.l2.msg.SyncReplicationActivity;
import com.tc.l2.state.ConsistencyManager;
import com.tc.l2.state.ConsistencyManager.Transition;
import com.tc.l2.state.ServerMode;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.groups.GroupManager;
import com.tc.object.session.SessionID;
import com.tc.objectserver.handler.ProcessTransactionHandler;
import com.tc.objectserver.persistence.EntityPersistor;
import com.tc.util.Assert;
import org.junit.Ignore;


public class ActiveToPassiveReplicationTest {
  
  ServerID passive;
  private ActiveToPassiveReplication replication;
  private ReplicationSender replicate;
  private ConsistencyManager consistency;
  private GroupManager group;
  
  
  public ActiveToPassiveReplicationTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp() {
    passive = mock(ServerID.class);
    when(passive.getUID()).thenReturn("test".getBytes());
    when(passive.getName()).thenReturn("test");
    replicate = mock(ReplicationSender.class);
    when(replicate.addPassive(any(ServerID.class), any(SessionID.class), anyInt(), any(SyncReplicationActivity.class))).thenReturn(Boolean.TRUE);
    consistency = mock(ConsistencyManager.class);
    when(consistency.requestTransition(any(ServerMode.class), any(NodeID.class), any(Transition.class))).thenReturn(Boolean.TRUE);
    ProcessTransactionHandler pth = mock(ProcessTransactionHandler.class);
    when(pth.snapshotEntityList(any(Predicate.class))).thenReturn(Collections.emptyList());
    group = mock(GroupManager.class);
    when(group.isNodeConnected(any(NodeID.class))).thenReturn(Boolean.TRUE);
    replication = new ActiveToPassiveReplication(consistency, pth, mock(EntityPersistor.class), replicate, mock(Sink.class), group);
  }
  
  @Ignore("Issue-#1380") @Test
  public void testNodeLeft() throws Exception {
    // Setup a CountDownLatch to coordinate between the main thread and the removal thread
    final java.util.concurrent.CountDownLatch setupComplete = new java.util.concurrent.CountDownLatch(1);
    final java.util.concurrent.CountDownLatch removalComplete = new java.util.concurrent.CountDownLatch(1);
    
    // Initialize the replication system
    replication.enterActiveState(Collections.emptySet());
    replication.nodeJoined(passive);
    
    // Create and configure the activity
    SyncReplicationActivity activity = mock(SyncReplicationActivity.class);
    when(activity.getActivityID()).thenReturn(SyncReplicationActivity.ActivityID.getNextID());
    
    // Start passive sync
    replication.startPassiveSync(passive);
    
    // Create a waiter for the replication activity
    ActivePassiveAckWaiter ack = replication.replicateActivity(activity, replication.passives());
    
    // Signal that setup is complete
    setupComplete.countDown();
    
    // Create a thread to remove the passive node after setup is complete
    Future<?> removal = Executors.newSingleThreadExecutor().submit(() -> {
      try {
        // Wait for the setup to complete before removing the passive
        setupComplete.await(30, TimeUnit.SECONDS);
        
        // Remove the passive that is about to be waited on
        replication.nodeLeft(passive);
        
        // Signal that removal is complete
        removalComplete.countDown();
      } catch (InterruptedException i) {
        throw new RuntimeException("node left failed", i);
      }
    });
    
    // Wait for the removal to complete with a generous timeout
    boolean removalSucceeded = removalComplete.await(30, TimeUnit.SECONDS);
    Assert.assertTrue("Passive node removal timed out", removalSucceeded);
    
    // Wait for the replication to complete
    ack.waitForCompleted();
    Assert.assertTrue(ack.isCompleted());
    
    // Make sure adding more waiters don't include removed passive
    SyncReplicationActivity nowait = mock(SyncReplicationActivity.class);
    when(nowait.getActivityID()).thenReturn(SyncReplicationActivity.ActivityID.getNextID());
    when(activity.getActivityID()).thenReturn(SyncReplicationActivity.ActivityID.getNextID());
    ActivePassiveAckWaiter ack2 = replication.replicateActivity(nowait, replication.passives());
    Assert.assertTrue(ack2.isCompleted());
    
    // Wait for passive sync to finish with increased timeout and better error reporting
    int turns = 1;
    int maxTurns = 25;
    long syncTimeout = 30000; // 15 seconds
    
    while (!replication.finishPassiveSync(syncTimeout)) {
      if (turns++ > maxTurns) {
        Assert.fail("Failed to finish passive sync after " + maxTurns + " attempts with " +
                    syncTimeout + "ms timeout each");
      }
    }
    
    // Ensure the removal thread completed
    removal.get(5, TimeUnit.SECONDS);
    
    // Verify no waiters are left
    Assert.assertTrue("Waiters should be empty after test", replication.getWaiters().isEmpty());
  }


  @Test
  public void testNodeStuckRemoveBeforeAdd() throws Exception {
    // Initialize the replication system
    replication.enterActiveState(Collections.emptySet());
    
    // Create and configure the activity
    SyncReplicationActivity activity = mock(SyncReplicationActivity.class);
    when(activity.getActivityID()).thenReturn(SyncReplicationActivity.ActivityID.getNextID());
    
    // Configure consistency manager to initially reject REMOVE_PASSIVE transitions
    when(consistency.requestTransition(any(ServerMode.class), any(NodeID.class), eq(Transition.REMOVE_PASSIVE))).thenReturn(Boolean.FALSE);
    
    // Simulate node leaving and rejoining
    replication.nodeLeft(passive);
    replication.nodeJoined(passive);
    
    // Verify passive sync fails when node is stuck in removal
    Assert.assertFalse(replication.startPassiveSync(passive));
    Mockito.verify(group).closeMember(eq(passive));
    
    // Verify no passive was added
    Mockito.verify(replicate, Mockito.never()).addPassive(eq(passive), any(SessionID.class), any(Integer.class), any(SyncReplicationActivity.class));
    
    // Now allow REMOVE_PASSIVE transitions to succeed
    when(consistency.requestTransition(any(ServerMode.class), any(NodeID.class), eq(Transition.REMOVE_PASSIVE))).thenReturn(Boolean.TRUE);
    
    // Use a timeout approach instead of indefinite polling
    long startTime = System.currentTimeMillis();
    long timeout = 30000; // 30 seconds
    boolean syncSucceeded = false;
    
    while (System.currentTimeMillis() - startTime < timeout) {
      if (replication.startPassiveSync(passive)) {
        syncSucceeded = true;
        break;
      }
      // Use shorter sleep intervals
      Thread.sleep(100);
    }
    
    // Verify sync eventually succeeded
    Assert.assertTrue("Passive sync did not succeed within timeout", syncSucceeded);
    
    // Verify passive was added
    Mockito.verify(replicate).addPassive(eq(passive), any(SessionID.class), any(Integer.class), any(SyncReplicationActivity.class));
  }
  
  @After
  public void tearDown() {
  }
}
