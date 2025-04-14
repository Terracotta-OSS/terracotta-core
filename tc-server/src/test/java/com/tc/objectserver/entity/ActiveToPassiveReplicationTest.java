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

import com.tc.async.api.Sink;
import com.tc.l2.msg.SyncReplicationActivity;
import com.tc.net.ServerID;
import com.tc.net.groups.GroupManager;
import com.tc.objectserver.handler.ProcessTransactionHandler;
import com.tc.objectserver.persistence.EntityPersistor;
import com.tc.util.Assert;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.tc.l2.state.ConsistencyManager;
import com.tc.l2.state.ConsistencyManager.Transition;
import com.tc.l2.state.ServerMode;
import com.tc.net.NodeID;
import com.tc.object.session.SessionID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import org.junit.Ignore;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;


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
  
  @Test
  public void testNodeLeft() throws Exception {
    replication.enterActiveState(Collections.emptySet());
    replication.nodeJoined(passive);
    SyncReplicationActivity activity = mock(SyncReplicationActivity.class);
    when(activity.getActivityID()).thenReturn(SyncReplicationActivity.ActivityID.getNextID());
    replication.startPassiveSync(passive);
    ActivePassiveAckWaiter ack = replication.replicateActivity(activity, replication.passives());
    Future<?> removal = Executors.newSingleThreadExecutor().submit(()->{
      try {
        TimeUnit.MILLISECONDS.sleep(3000);
// remove the passive that is about to be waited on
        replication.nodeLeft(passive);
      } catch (InterruptedException i) {
        throw new RuntimeException("node left failed", i);
      }
    });
    ack.waitForCompleted();
    Assert.assertTrue(ack.isCompleted());
    // make sure adding more waiters don't include removed passive
    SyncReplicationActivity nowait = mock(SyncReplicationActivity.class);
    when(nowait.getActivityID()).thenReturn(SyncReplicationActivity.ActivityID.getNextID());
    when(activity.getActivityID()).thenReturn(SyncReplicationActivity.ActivityID.getNextID());
    ActivePassiveAckWaiter ack2 = replication.replicateActivity(nowait, replication.passives());
    Assert.assertTrue(ack2.isCompleted());
    int turns = 1;
    while (!replication.finishPassiveSync(10000)) {
      if (turns++ > 12) {
          Assert.fail();
      }
    }
    removal.get();
    Assert.assertTrue(replication.getWaiters().isEmpty());
  }


  @Test
  public void testNodeStuckRemoveBeforeAdd() throws Exception {
    replication.enterActiveState(Collections.emptySet());
    SyncReplicationActivity activity = mock(SyncReplicationActivity.class);
    when(activity.getActivityID()).thenReturn(SyncReplicationActivity.ActivityID.getNextID());
    when(consistency.requestTransition(any(ServerMode.class), any(NodeID.class), eq(Transition.REMOVE_PASSIVE))).thenReturn(Boolean.FALSE);
    replication.nodeLeft(passive);
    replication.nodeJoined(passive);
    Assert.assertFalse(replication.startPassiveSync(passive));
    Mockito.verify(group).closeMember(eq(passive));
    when(consistency.requestTransition(any(ServerMode.class), any(NodeID.class), eq(Transition.REMOVE_PASSIVE))).thenReturn(Boolean.TRUE);
    Mockito.verify(replicate, Mockito.never()).addPassive(eq(passive), any(SessionID.class), any(Integer.class), any(SyncReplicationActivity.class));
    while (!replication.startPassiveSync(passive)) {
      Thread.sleep(1000);
    }
    Mockito.verify(replicate).addPassive(eq(passive), any(SessionID.class), any(Integer.class), any(SyncReplicationActivity.class));
  }
  
  @After
  public void tearDown() {
  }
}
