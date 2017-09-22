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
package com.tc.objectserver.entity;

import com.tc.async.api.Sink;
import com.tc.async.api.SpecializedEventContext;
import com.tc.l2.msg.SyncReplicationActivity;
import com.tc.net.ServerID;
import com.tc.net.groups.GroupManager;
import com.tc.objectserver.handler.AddPassiveMessage;
import com.tc.objectserver.handler.ProcessTransactionHandler;
import com.tc.objectserver.handler.RemovePassiveMessage;
import com.tc.objectserver.handler.ReplicationSender;
import com.tc.objectserver.handler.ReplicationSenderMessage;
import com.tc.objectserver.handler.SyncActivityMessage;
import com.tc.objectserver.persistence.EntityPersistor;
import com.tc.stats.Stats;
import com.tc.util.Assert;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ActiveToPassiveReplicationTest {
  
  ServerID passive;
  Sink<ReplicationSenderMessage> replicationSenderMessageSink;
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
    replicationSenderMessageSink = new Sink<ReplicationSenderMessage>() {
      @Override
      public void addSingleThreaded(ReplicationSenderMessage context) {
        if(context instanceof RemovePassiveMessage) {
          ((RemovePassiveMessage)context).getCompletionHandler().run();
        } else if(context instanceof AddPassiveMessage) {
          ((AddPassiveMessage)context).getCompletionHandler().run();
        } else if(context instanceof SyncActivityMessage) {
          ((SyncActivityMessage)context).getCompletionHandler().accept(true);
        }
      }

      @Override
      public void addMultiThreaded(ReplicationSenderMessage context) {

      }

      @Override
      public void addSpecialized(SpecializedEventContext specialized) {

      }

      @Override
      public int size() {
        return 0;
      }

      @Override
      public void clear() {

      }

      @Override
      public void close() {

      }

      @Override
      public void enableStatsCollection(boolean enable) {

      }

      @Override
      public boolean isStatsCollectionEnabled() {
        return false;
      }

      @Override
      public Stats getStats(long frequency) {
        return null;
      }

      @Override
      public Stats getStatsAndReset(long frequency) {
        return null;
      }

      @Override
      public void resetStats() {

      }
    };
    ReplicationSender replicate = mock(ReplicationSender.class);
    replication = new ActiveToPassiveReplication(mock(ProcessTransactionHandler.class), Collections.singleton(passive), mock(EntityPersistor.class), replicate, mock(GroupManager.class), replicationSenderMessageSink);
  }
  
  @Test
  public void testNodeLeft() throws Exception {
    replication.enterActiveState();
    replication.nodeJoined(passive);
    SyncReplicationActivity activity = mock(SyncReplicationActivity.class);
    when(activity.getActivityID()).thenReturn(SyncReplicationActivity.ActivityID.getNextID());
    ActivePassiveAckWaiter ack = replication.replicateActivity(activity, Collections.singleton(passive));
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
