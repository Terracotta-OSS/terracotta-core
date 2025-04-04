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

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.EntityID;
import com.tc.object.FetchID;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.objectserver.entity.RequestProcessor.EntityRequest;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.EntityMessage;


public class RequestProcessorTest {
  
  public RequestProcessorTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp() {
  }
  
  @After
  public void tearDown() {
  }
  
  /**
   * Test of scheduleRequest method, of class RequestProcessor.
   */
  @Test
  public <E> void testMinimumProcessingThreads() throws Exception {
    StageManager mgr = mock(StageManager.class);
    TCPropertiesImpl.getProperties().overwriteTcPropertiesFromConfig(new HashMap<>());
    int minProcs = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.MIN_ENTITY_PROCESSOR_THREADS);
    when(mgr.createStage(anyString(), any(), any(), anyInt(), anyInt(), anyBoolean(), anyBoolean())).then(inv->{
      Assert.assertThat((Integer)inv.getArguments()[3], greaterThanOrEqualTo(minProcs));
      return mock(Stage.class);
    });
    RequestProcessor instance = new RequestProcessor(mgr, true);
    // one for sync stage and once for regular
    verify(mgr, times(2)).createStage(anyString(), any(), any(), anyInt(), anyInt(), anyBoolean(), anyBoolean());
  }  

  /**
   * Test of scheduleRequest method, of class RequestProcessor.
   */
  @Test
  public void testBasicScheduleRequest() {
    System.out.println("scheduleRequest");
    ServerEntityRequest request = mock(ServerEntityRequest.class);
    when(request.getAction()).thenReturn(ServerEntityAction.INVOKE_ACTION);
    when(request.replicateTo(anySet())).thenReturn(Collections.emptySet());
    Sink<EntityRequest> dump = mock(Sink.class);
    RequestProcessor instance = new RequestProcessor(dump);

    instance.scheduleRequest(false, mock(EntityID.class), 1L, new FetchID(1L), request, MessagePayload.emptyPayload(), (w)->{}, true, ConcurrencyStrategy.UNIVERSAL_KEY);
    
    verify(dump).addToSink(ArgumentMatchers.any());
  }
  
  @Test
  public void testConcurrencyStrategy() {
    System.out.println("concurrency");
    PassiveReplicationBroker broker = mock(PassiveReplicationBroker.class);
    EntityID testid = new EntityID("MockEntity", "foo");
    ManagedEntityImpl entity = mock(ManagedEntityImpl.class);
    when(entity.getID()).thenReturn(testid);
    ConcurrencyStrategy strategy = mock(ConcurrencyStrategy.class);
    ServerEntityRequest request = mock(ServerEntityRequest.class);
    when(request.getAction()).thenReturn(ServerEntityAction.INVOKE_ACTION);
    int key = Math.abs((int)(Math.random() * Integer.MAX_VALUE));
    when(request.replicateTo(anySet())).thenReturn(Collections.emptySet());
    TCByteBuffer payload = TCByteBufferFactory.wrap(intToArray(key));

    Sink dump = mock(Sink.class);
    RequestProcessor instance = new RequestProcessor(dump);
    instance.setReplication(broker);
    int expResult = key;

    instance.scheduleRequest(false, testid, 1L, new FetchID(1L), request, MessagePayload.commonMessagePayloadBusy(payload, null, true), (w)->{}, true, key);

    verify(dump).addToSink(ArgumentMatchers.argThat(new MultiThreadedEventMatcher(testid, key)));
  }

  @Test
  public void testUniversalKey() {
    System.out.println("univeral key");
    PassiveReplicationBroker broker = mock(PassiveReplicationBroker.class);
    EntityID testid = new EntityID("MockEntity", "foo");
    ManagedEntityImpl entity = mock(ManagedEntityImpl.class);
    when(entity.getID()).thenReturn(testid);
    ServerEntityRequest request = mock(ServerEntityRequest.class);
    when(request.getAction()).thenReturn(ServerEntityAction.INVOKE_ACTION);
    when(request.replicateTo(anySet())).thenReturn(Collections.emptySet());
    Sink dump = mock(Sink.class);

    RequestProcessor instance = new RequestProcessor(dump);
    instance.setReplication(broker);
    int expResult = ConcurrencyStrategy.UNIVERSAL_KEY;
    instance.scheduleRequest(false, testid, 1L, new FetchID(1L), request, MessagePayload.emptyPayload(), (w)->{}, true, ConcurrencyStrategy.UNIVERSAL_KEY);

    verify(dump).addToSink(ArgumentMatchers.argThat(new MultiThreadedEventMatcher(testid, expResult)));
  }

  @Test
  public void testManagementKey() {
    System.out.println("management key");
    PassiveReplicationBroker broker = mock(PassiveReplicationBroker.class);
    EntityID testid = new EntityID("MockEntity", "foo");
    ManagedEntity entity = mock(ManagedEntity.class);
    when(entity.getID()).thenReturn(testid);
    ServerEntityRequest request = mock(ServerEntityRequest.class);
    when(request.replicateTo(anySet())).thenReturn(Collections.emptySet());
    when(request.getAction()).thenReturn(ServerEntityAction.CREATE_ENTITY);
    Sink dump = mock(Sink.class);

    RequestProcessor instance = new RequestProcessor(dump);
    instance.setReplication(broker);
    instance.scheduleRequest(false, testid, 1L, new FetchID(1L), request, MessagePayload.emptyPayload(), (w)->{}, true, ConcurrencyStrategy.MANAGEMENT_KEY);

    verify(dump).addToSink(ArgumentMatchers.argThat(new MultiThreadedEventMatcher(testid, ConcurrencyStrategy.MANAGEMENT_KEY)));
  }
  
  @Test
  public void testReplicationCall() {
    System.out.println("replication");
    EntityID testid = new EntityID("MockEntity", "foo");
    
    ManagedEntityImpl entity = mock(ManagedEntityImpl.class);
    when(entity.getID()).thenReturn(testid);
    when(entity.getVersion()).thenReturn(1L);

    ServerEntityRequest request = mock(ServerEntityRequest.class);
    when(request.getAction()).thenReturn(ServerEntityAction.INVOKE_ACTION);
    when(request.replicateTo(anySet())).thenAnswer(new Answer<Set<NodeID>>() {
      @Override
      public Set<NodeID> answer(InvocationOnMock invocation) throws Throwable {
        return (Set<NodeID>)invocation.getArguments()[0];
      }
    });
    when(request.getOldestTransactionOnClient()).thenReturn(TransactionID.NULL_ID);
    when(request.getTransaction()).thenReturn(TransactionID.NULL_ID);
    when(request.getNodeID()).thenReturn(mock(ClientID.class));
    
    Sink dump = mock(Sink.class);

    PassiveReplicationBroker broker = mock(PassiveReplicationBroker.class);
    when(broker.passives()).thenReturn(Collections.singleton(mock(SessionID.class)));
    when(broker.replicateActivity(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(NoReplicationBroker.NOOP_WAITER);
    doAnswer(i->{
      EntityRequest req = (EntityRequest)i.getArguments()[0];
      req.run();
      return null;
    }).when(dump).addToSink(any());
    RequestProcessor instance = new RequestProcessor(dump);
    instance.setReplication(broker);
    
    instance.scheduleRequest(false, testid, 1L, new FetchID(1L), request, MessagePayload.emptyPayload(), (w)->{}, true, ConcurrencyStrategy.UNIVERSAL_KEY);
    
    verify(broker, times(0)).replicateActivity(ArgumentMatchers.any(), ArgumentMatchers.any());
    
    instance.enterActiveState();
    
    instance.scheduleRequest(false, testid, 1L, new FetchID(1L), request, MessagePayload.emptyPayload(), (w)->{}, true, ConcurrencyStrategy.UNIVERSAL_KEY);
//  assume args from mocked request are passed.  just testing execution
    verify(broker).replicateActivity(ArgumentMatchers.any(), ArgumentMatchers.any());
//    verify(broker).replicateMessage(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),Matchers.any(), Matchers.any());
  }
  
  private static byte[] intToArray(int val) {
    byte[] four = new byte[4];
    for (int x=0;x<four.length;x++) {
      four[x] = (byte)((val >> ((3 - x) * Byte.SIZE)) & 0x00ff);
    }
    return four;
  }
  
  private static int arrayToInt(byte[] val) {
    int end = 0;
    for (int x=0;x<val.length;x++) {
      end <<= Byte.SIZE;
      end |= (val[x] & 0x00ff);
    }
    return end;
  }  
  
  private class MultiThreadedEventMatcher implements ArgumentMatcher<MultiThreadedEventContext> {
    
    private final int rawKey;
    private final EntityID entity;

    public MultiThreadedEventMatcher(EntityID id, int rawKey) {
      entity = id;
      this.rawKey = rawKey;
    }
    
    @Override
    public boolean matches(MultiThreadedEventContext item) {
      if (item instanceof MultiThreadedEventContext) {
        Object schedulingKey = ((MultiThreadedEventContext)item).getSchedulingKey();
        return (rawKey == ConcurrencyStrategy.UNIVERSAL_KEY)
            ? (null == schedulingKey)
            : schedulingKey.equals(entity.hashCode() ^ rawKey);
      }
      return false;
    }
  }
  
  private static class ByteArrayMessage implements EntityMessage {
    private final byte[] message;
    public ByteArrayMessage(byte[] message) {
      this.message = message;
    }
    public int messageAsInt() {
      return arrayToInt(this.message);
    }
  }
}
