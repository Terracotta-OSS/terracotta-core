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
import com.tc.l2.msg.ReplicationEnvelope;
import com.tc.l2.msg.ReplicationMessage;
import com.tc.net.ServerID;
import com.tc.net.groups.MessageID;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.util.Assert;
import java.util.Collections;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author mscott
 */
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
    Iterable<ManagedEntity> entities = mock(Iterable.class);
    Sink<ReplicationEnvelope> replicate = mock(Sink.class);
    replication = new ActiveToPassiveReplication(Collections.singleton(passive), entities, replicate);
  }
  
  @Test
  public void testNodeLeft() throws Exception {
    replication.enterActiveState();
    replication.nodeJoined(passive);
    ReplicationMessage msg = mock(ReplicationMessage.class);
    MessageID id = new MessageID(1);
    when(msg.getMessageID()).thenReturn(id);
    ReplicationEnvelope env = mock(ReplicationEnvelope.class);
    when(msg.target(Matchers.any(), Matchers.any())).thenReturn(env);
    Future<Void> ack = replication.replicateMessage(msg, Collections.singleton(passive));
    Thread target = Thread.currentThread();
    Thread it = new Thread(()->{
      try {
        TimeUnit.MILLISECONDS.sleep(100);
// remove the passive that is about to be waited on
        replication.nodeLeft(passive);
      } catch (InterruptedException i) {
        
      }
    });
    it.start();
    try {
      ack.get(1, TimeUnit.SECONDS);
    } catch (InterruptedException ie) {
      Assert.fail("test failed");
    }
    Assert.assertTrue(ack.isDone());
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
