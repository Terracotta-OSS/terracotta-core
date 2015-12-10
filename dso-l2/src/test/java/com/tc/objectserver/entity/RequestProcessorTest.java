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

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.async.api.Sink;
import com.tc.net.NodeID;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import java.util.Collections;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.MessageCodec;


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
  public void testBasicScheduleRequest() {
    System.out.println("scheduleRequest");
    ManagedEntityImpl entity = mock(ManagedEntityImpl.class);
    ServerEntityRequest request = mock(ServerEntityRequest.class);
    when(request.getAction()).thenReturn(ServerEntityAction.INVOKE_ACTION);
    when(request.requiresReplication()).thenReturn(Boolean.FALSE);
    when(request.getPayload()).thenReturn(new byte[0]);
    Sink<Runnable> dump = mock(Sink.class);
    RequestProcessor instance = new RequestProcessor(dump);
    int expResult = ConcurrencyStrategy.UNIVERSAL_KEY;
    instance.scheduleRequest(entity, mock(EntityDescriptor.class), request, new ByteArrayMessage(request.getPayload()), ConcurrencyStrategy.UNIVERSAL_KEY, mock(MessageCodec.class));
    
    verify(dump).addMultiThreaded(Matchers.any());
  }
  
  @Test
  public void testConcurrencyStrategy() {
    System.out.println("concurrency");
    PassiveReplicationBroker broker = mock(PassiveReplicationBroker.class);
    EntityID testid = new EntityID("MockEntity", "foo");
    EntityDescriptor descriptor = new EntityDescriptor(testid, ClientInstanceID.NULL_ID, 1);
    ManagedEntityImpl entity = mock(ManagedEntityImpl.class);
    when(entity.getID()).thenReturn(testid);
    ConcurrencyStrategy strategy = mock(ConcurrencyStrategy.class);
    ServerEntityRequest request = mock(ServerEntityRequest.class);
    when(request.getAction()).thenReturn(ServerEntityAction.INVOKE_ACTION);
    int key = Math.abs((int)(Math.random() * Integer.MAX_VALUE));
    when(request.requiresReplication()).thenReturn(Boolean.FALSE);
    byte[] payload = intToArray(key);
    when(request.getPayload()).thenReturn(payload);
    Sink dump = mock(Sink.class);
    RequestProcessor instance = new RequestProcessor(dump);
    instance.setReplication(broker);
    int expResult = key;
    ByteArrayMessage byteArrayMessage = new ByteArrayMessage(request.getPayload());
    int concurrencyKey = byteArrayMessage.messageAsInt();
    instance.scheduleRequest(entity, descriptor, request, byteArrayMessage, concurrencyKey, mock(MessageCodec.class));

    verify(dump).addMultiThreaded(Matchers.argThat(new MultiThreadedEventMatcher(testid, key)));
  }

  @Test
  public void testUniversalKey() {
    System.out.println("univeral key");
    PassiveReplicationBroker broker = mock(PassiveReplicationBroker.class);
    EntityID testid = new EntityID("MockEntity", "foo");
    EntityDescriptor descriptor = new EntityDescriptor(testid, ClientInstanceID.NULL_ID, 1);
    ManagedEntityImpl entity = mock(ManagedEntityImpl.class);
    when(entity.getID()).thenReturn(testid);
    ServerEntityRequest request = mock(ServerEntityRequest.class);
    when(request.getAction()).thenReturn(ServerEntityAction.INVOKE_ACTION);
    when(request.requiresReplication()).thenReturn(Boolean.FALSE);
    Sink dump = mock(Sink.class);

    RequestProcessor instance = new RequestProcessor(dump);
    instance.setReplication(broker);
    int expResult = ConcurrencyStrategy.UNIVERSAL_KEY;
    instance.scheduleRequest(entity, descriptor, request, new ByteArrayMessage(new byte[0]), ConcurrencyStrategy.UNIVERSAL_KEY, mock(MessageCodec.class));

    verify(dump).addMultiThreaded(Matchers.argThat(new MultiThreadedEventMatcher(testid, expResult)));
  }

  @Test
  public void testManagementKey() {
    System.out.println("management key");
    PassiveReplicationBroker broker = mock(PassiveReplicationBroker.class);
    EntityID testid = new EntityID("MockEntity", "foo");
    EntityDescriptor descriptor = new EntityDescriptor(testid, ClientInstanceID.NULL_ID, 1);
    ManagedEntityImpl entity = mock(ManagedEntityImpl.class);
    when(entity.getID()).thenReturn(testid);
    ServerEntityRequest request = mock(ServerEntityRequest.class);
    when(request.requiresReplication()).thenReturn(Boolean.FALSE);
    when(request.getAction()).thenReturn(ServerEntityAction.CREATE_ENTITY);
    Sink dump = mock(Sink.class);

    RequestProcessor instance = new RequestProcessor(dump);
    instance.setReplication(broker);
    instance.scheduleRequest(entity, descriptor, request, new ByteArrayMessage(new byte[0]), ConcurrencyStrategy.MANAGEMENT_KEY, mock(MessageCodec.class));

    verify(dump).addMultiThreaded(Matchers.argThat(new MultiThreadedEventMatcher(testid, ConcurrencyStrategy.MANAGEMENT_KEY)));
  }
  
  @Test
  public void testReplicationCall() {
    System.out.println("replication");
    EntityID testid = new EntityID("MockEntity", "foo");
    EntityDescriptor descriptor = new EntityDescriptor(testid, ClientInstanceID.NULL_ID, 1);
    
    ManagedEntityImpl entity = mock(ManagedEntityImpl.class);
    when(entity.getID()).thenReturn(testid);
    when(entity.getVersion()).thenReturn(1L);

    ServerEntityRequest request = mock(ServerEntityRequest.class);
    when(request.getAction()).thenReturn(ServerEntityAction.INVOKE_ACTION);
    when(request.requiresReplication()).thenReturn(Boolean.TRUE);
    when(request.getOldestTransactionOnClient()).thenReturn(TransactionID.NULL_ID);
    when(request.getTransaction()).thenReturn(TransactionID.NULL_ID);
    when(request.getPayload()).thenReturn(new byte[0]);
    when(request.getNodeID()).thenReturn(mock(NodeID.class));
    
    Sink dump = mock(Sink.class);

    PassiveReplicationBroker broker = mock(PassiveReplicationBroker.class);
    when(broker.passives()).thenReturn(Collections.singleton(mock(NodeID.class)));
    when(broker.replicateMessage(Matchers.any(), Matchers.any())).thenReturn(NoReplicationBroker.NOOP_FUTURE);
    RequestProcessor instance = new RequestProcessor(dump);
    instance.setReplication(broker);
    
    instance.scheduleRequest(entity, descriptor, request, new ByteArrayMessage(request.getPayload()), ConcurrencyStrategy.UNIVERSAL_KEY, mock(MessageCodec.class));
//  assume args from mocked request are passed.  just testing execution
    verify(broker).replicateMessage(Matchers.any(), Matchers.any());
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
  
  private class MultiThreadedEventMatcher extends BaseMatcher<ServerEntityRequest> {
    
    private final int rawKey;
    private final EntityID entity;

    public MultiThreadedEventMatcher(EntityID id, int rawKey) {
      entity = id;
      this.rawKey = rawKey;
    }
    
    @Override
    public boolean matches(Object item) {
      if (item instanceof MultiThreadedEventContext) {
        Object schedulingKey = ((MultiThreadedEventContext)item).getSchedulingKey();
        return (rawKey == ConcurrencyStrategy.UNIVERSAL_KEY)
            ? (null == schedulingKey)
            : schedulingKey.equals(entity.hashCode() ^ rawKey);
      }
      return false;
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("my matcher");
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
