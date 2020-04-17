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
import java.util.function.Consumer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;


public class ActiveToPassiveReplicationTest {
  
  ServerID passive;
  private ActiveToPassiveReplication replication;
  
  
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
    ReplicationSender replicate = mock(ReplicationSender.class);
    when(replicate.addPassive(any(NodeID.class), any(SessionID.class), anyInt(), any(SyncReplicationActivity.class))).thenReturn(Boolean.TRUE);
    ConsistencyManager cmgr = mock(ConsistencyManager.class);
    when(cmgr.requestTransition(any(ServerMode.class), any(NodeID.class), any(Transition.class))).thenReturn(Boolean.TRUE);
    ProcessTransactionHandler pth = mock(ProcessTransactionHandler.class);
    when(pth.snapshotEntityList(any(Consumer.class))).thenReturn(Collections.emptyList());
    replication = new ActiveToPassiveReplication(cmgr, pth, mock(EntityPersistor.class), replicate, mock(Sink.class), mock(GroupManager.class));
  }
  
  @Test
  public void testNodeLeft() throws Exception {
    replication.enterActiveState(Collections.emptySet());
    replication.nodeJoined(passive);
    SyncReplicationActivity activity = mock(SyncReplicationActivity.class);
    when(activity.getActivityID()).thenReturn(SyncReplicationActivity.ActivityID.getNextID());
    replication.startPassiveSync(passive);
    ActivePassiveAckWaiter ack = replication.replicateActivity(activity, replication.passives());
    Thread it = new Thread(()->{
      try {
        TimeUnit.MILLISECONDS.sleep(100);
// remove the passive that is about to be waited on
        replication.nodeLeft(passive);
      } catch (InterruptedException i) {
        
      }
    });
    it.start();
    ack.waitForCompleted();
    Assert.assertTrue(ack.isCompleted());
    // make sure adding more waiters don't include removed passive
    SyncReplicationActivity nowait = mock(SyncReplicationActivity.class);
    when(nowait.getActivityID()).thenReturn(SyncReplicationActivity.ActivityID.getNextID());
    when(activity.getActivityID()).thenReturn(SyncReplicationActivity.ActivityID.getNextID());
    ActivePassiveAckWaiter ack2 = replication.replicateActivity(nowait, replication.passives());
    Assert.assertTrue(ack2.isCompleted());
    replication.finishPassiveSync(10000);
    Assert.assertTrue(replication.getWaiters().isEmpty());
  }
  
  @After
  public void tearDown() {
  }

  // TODO add test methods here.
  // The methods must be annotated with annotation @Test. For example:
  //
  // @Test
  // public void hello() {}
}
