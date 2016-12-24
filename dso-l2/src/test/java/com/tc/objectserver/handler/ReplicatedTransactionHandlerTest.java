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
package com.tc.objectserver.handler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;

import com.tc.async.api.EventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Sink;
import com.tc.async.api.SpecializedEventContext;
import com.tc.objectserver.entity.MessagePayload;
import com.tc.entity.VoltronEntityAppliedResponse;
import com.tc.entity.VoltronEntityReceivedResponse;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.l2.msg.ReplicationMessage;
import com.tc.l2.msg.SyncReplicationActivity;
import com.tc.l2.state.StateManager;
import com.tc.net.ClientID;
import com.tc.net.ServerID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupManager;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.objectserver.entity.ClientEntityStateManager;
import com.tc.objectserver.entity.PlatformEntity;
import com.tc.objectserver.persistence.EntityPersistor;
import com.tc.objectserver.persistence.TransactionOrderPersistor;
import com.tc.stats.Stats;
import com.tc.util.Assert;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import org.junit.Test;
import org.mockito.Matchers;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import org.mockito.Mockito;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.SyncMessageCodec;


public class ReplicatedTransactionHandlerTest {
  private EntityPersistor entityPersistor;
  private TransactionOrderPersistor transactionOrderPersistor;
  private ReplicatedTransactionHandler rth;
  private ClientID source;
  private ForwardingSink loopbackSink;
  private StateManager stateManager;
  private EntityManager entityManager;
  private ManagedEntity platform;
  private GroupManager<AbstractGroupMessage> groupManager;
  
  private long rid = 0;
  
  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    this.entityPersistor = mock(EntityPersistor.class);
    this.transactionOrderPersistor = mock(TransactionOrderPersistor.class);
    this.stateManager = mock(StateManager.class);
    this.entityManager = mock(EntityManager.class);
    this.groupManager = mock(GroupManager.class);
    Mockito.doAnswer((Answer) (InvocationOnMock invocation) -> {
      ((Runnable)invocation.getArguments()[2]).run();
      return null;
    }).when(groupManager).sendToWithSentCallback(Mockito.any(), Mockito.any(), Mockito.any());
    this.platform = mock(ManagedEntity.class);
    Mockito.doAnswer((Answer) (InvocationOnMock invocation) -> {
      ((Consumer)invocation.getArguments()[2]).accept(null);
      return null;
    }).when(platform).addRequestMessage(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    when(entityManager.getEntity(Matchers.eq(PlatformEntity.PLATFORM_ID), Matchers.eq(PlatformEntity.VERSION))).thenReturn(Optional.of(platform));
    this.rth = new ReplicatedTransactionHandler(stateManager, this.transactionOrderPersistor, this.entityManager, this.entityPersistor, this.groupManager);
    // We need to do things like serialize/deserialize this so we can't easily use a mocked source.
    this.source = new ClientID(1);
    
    MessageChannel messageChannel = mock(MessageChannel.class);
    when(messageChannel.createMessage(TCMessageType.VOLTRON_ENTITY_APPLIED_RESPONSE)).thenReturn(mock(VoltronEntityAppliedResponse.class));
    when(messageChannel.createMessage(TCMessageType.VOLTRON_ENTITY_RECEIVED_RESPONSE)).thenReturn(mock(VoltronEntityReceivedResponse.class));
    
    DSOChannelManager channelManager = mock(DSOChannelManager.class);
    when(channelManager.getActiveChannel(this.source)).thenReturn(messageChannel);
    
    this.loopbackSink = new ForwardingSink(this.rth.getEventHandler());
  }
  
  @Test
  public void testEntityNoIgnoresDuringSyncOfKey() throws Exception {
    EntityID eid = new EntityID("foo", "bar");
    EntityDescriptor descriptor = new EntityDescriptor(eid, ClientInstanceID.NULL_ID, 1);
    ServerID sid = new ServerID("test", "test".getBytes());
    ManagedEntity entity = mock(ManagedEntity.class);
    SyncReplicationActivity activity = mock(SyncReplicationActivity.class);
    int rand = 1;
    when(activity.getConcurrency()).thenReturn(rand);
    when(activity.getActivityType()).thenReturn(SyncReplicationActivity.ActivityType.INVOKE_ACTION);
    when(activity.getEntityID()).thenReturn(eid);
    when(activity.getEntityDescriptor()).thenReturn(descriptor);
    when(activity.getOldestTransactionOnClient()).thenReturn(TransactionID.NULL_ID);
    when(activity.getExtendedData()).thenReturn(new byte[0]);
    ReplicationMessage msg = mock(ReplicationMessage.class);
    when(msg.getType()).thenReturn(ReplicationMessage.REPLICATE);
    when(msg.messageFrom()).thenReturn(sid);
    when(msg.getActivity()).thenReturn(activity);
    when(entity.getCodec()).thenReturn(mock(MessageCodec.class));
    when(this.entityManager.getEntity(Matchers.any(), Matchers.anyInt())).thenReturn(Optional.empty());
    when(this.entityManager.createEntity(Matchers.any(), anyLong(), anyLong(), anyInt())).then((invoke)->{
      when(this.entityManager.getEntity(Matchers.any(), Matchers.anyInt())).thenReturn(Optional.of(entity));
      return entity;
    });
    Mockito.doAnswer(invocation->{
      Consumer consumer = (Consumer)invocation.getArguments()[2];
      if (consumer != null) {
        consumer.accept(new byte[0]);
      }
      // NOTE:  We don't retire replicated messages.
      return null;
    }).when(entity).addRequestMessage(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any());
    this.loopbackSink.addSingleThreaded(createReceivedActivity(SyncReplicationActivity.createStartSyncMessage()));
    this.loopbackSink.addSingleThreaded(createReceivedActivity(SyncReplicationActivity.createStartEntityMessage(eid, 1, new byte[0], 0)));
    this.loopbackSink.addSingleThreaded(createReceivedActivity(SyncReplicationActivity.createStartEntityKeyMessage(eid, 1, rand)));
    this.loopbackSink.addSingleThreaded(msg);
    this.loopbackSink.addSingleThreaded(createReceivedActivity(SyncReplicationActivity.createEndEntityKeyMessage(eid, 1, rand)));
    this.loopbackSink.addSingleThreaded(createReceivedActivity(SyncReplicationActivity.createEndEntityMessage(eid, 1)));
    this.loopbackSink.addSingleThreaded(createReceivedActivity(SyncReplicationActivity.createEndSyncMessage(new byte[0])));
//  verify there was an attempt to decode the invoke message
    verify(activity).getExtendedData();
    verify(entity).getCodec();
    // Note that we want to verify 2 ACK messages:  RECEIVED and COMPLETED.
    verify(groupManager, times(2)).sendToWithSentCallback(Matchers.eq(sid), Matchers.any(), Matchers.any());
  }  
  
  @Test
  public void testEntityGetsConcurrencyKey() throws Exception {
    EntityID eid = new EntityID("foo", "bar");
    EntityDescriptor descriptor = new EntityDescriptor(eid, ClientInstanceID.NULL_ID, 1);
    ServerID sid = new ServerID("test", "test".getBytes());
    ManagedEntity entity = mock(ManagedEntity.class);
    SyncReplicationActivity activity = mock(SyncReplicationActivity.class);
    int rand = new Random().nextInt();
    when(activity.getConcurrency()).thenReturn(rand);
    when(activity.getActivityType()).thenReturn(SyncReplicationActivity.ActivityType.INVOKE_ACTION);
    when(activity.getEntityID()).thenReturn(eid);
    when(activity.getEntityDescriptor()).thenReturn(descriptor);
    when(activity.getOldestTransactionOnClient()).thenReturn(TransactionID.NULL_ID);
    ReplicationMessage msg = mock(ReplicationMessage.class);
    MessageCodec codec = mock(MessageCodec.class);
    when(msg.getType()).thenReturn(ReplicationMessage.REPLICATE);
    when(msg.messageFrom()).thenReturn(sid);
    when(msg.getActivity()).thenReturn(activity);
    when(this.entityManager.getEntity(Matchers.any(), Matchers.anyInt())).thenReturn(Optional.of(entity));
    when(entity.getCodec()).thenReturn(codec);
    when(this.entityManager.getMessageCodec(Matchers.any())).thenReturn(codec);
    Mockito.doAnswer(invocation->{
      Consumer consumer = (Consumer)invocation.getArguments()[2];
      if (consumer != null) {
        consumer.accept(new byte[0]);
      }
      // NOTE:  We don't retire replicated messages.
      return null;
    }).when(entity).addRequestMessage(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any());
    this.loopbackSink.addSingleThreaded(createReceivedActivity(SyncReplicationActivity.createStartSyncMessage()));
    this.loopbackSink.addSingleThreaded(createReceivedActivity(SyncReplicationActivity.createEndSyncMessage(new byte[0])));
    this.loopbackSink.addSingleThreaded(msg);
    verify(activity).getExtendedData();
    verify(activity).getConcurrency();  // make sure RTH is pulling the concurrency from the message
    // Note that we want to verify 2 ACK messages:  RECEIVED and COMPLETED.
    verify(groupManager, times(2)).sendToWithSentCallback(Matchers.eq(sid), Matchers.any(), Matchers.any());
  }
  
  @Test
  public void testDestroy() throws Exception {
    this.rth.getEventHandler().destroy();
    verify(platform).addRequestMessage(Matchers.any(ServerEntityRequest.class), Matchers.any(MessagePayload.class), Matchers.any(), Matchers.any());
  }
  
  @Test
  public void testTestDefermentDuringSync() throws Exception {
    EntityID eid = new EntityID("foo", "bar");
    long VERSION = 1;
    ManagedEntity entity = mock(ManagedEntity.class);
    MessageCodec codec = mock(MessageCodec.class);
    SyncMessageCodec sync = mock(SyncMessageCodec.class);
    when(this.entityManager.getEntity(Matchers.any(), Matchers.anyInt())).thenReturn(Optional.empty());
    when(this.entityManager.createEntity(Matchers.any(), anyLong(), anyLong(), anyInt())).then((invoke)->{
      when(this.entityManager.getEntity(Matchers.any(), Matchers.anyInt())).thenReturn(Optional.of(entity));
      return entity;
    });
    when(this.entityManager.getMessageCodec(Matchers.eq(eid))).thenReturn(codec);
    when(entity.getCodec()).thenReturn(codec);
    
    Mockito.doAnswer(invocation->{
      ServerEntityRequest req = (ServerEntityRequest)invocation.getArguments()[0];
      // We will ignore the EntityMessage at index [1].
      // NOTE:  We don't retire replicated messages.
      verifySequence(req, ((MessagePayload)invocation.getArguments()[1]).getRawPayload(), ((MessagePayload)invocation.getArguments()[1]).getConcurrency());
      return null;
    }).when(entity).addRequestMessage(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any());
    Mockito.doAnswer(invocation->{
      ServerEntityRequest req = (ServerEntityRequest)invocation.getArguments()[0];
      // NOTE:  We don't retire replicated messages.
      verifySequence(req, ((MessagePayload)invocation.getArguments()[1]).getRawPayload(), ((MessagePayload)invocation.getArguments()[1]).getConcurrency());
      return null;
    }).when(entity).addRequestMessage(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any());
    mockPassiveSync(rth);
  }
  
  private ServerEntityRequest last;
  private int lastSid = 0;
  private int concurrency = 0;
  private boolean invoked = false;
  
  private void verifySequence(ServerEntityRequest req, byte[] payload, int c) {
    switch(req.getAction()) {
      case RECEIVE_SYNC_ENTITY_START:
        Assert.assertNull(last);
        last = req;
        Assert.assertEquals(0, concurrency);
        break;
      case RECEIVE_SYNC_ENTITY_KEY_START:
        Assert.assertTrue(last.getAction() == ServerEntityAction.RECEIVE_SYNC_ENTITY_START || last.getAction() == ServerEntityAction.RECEIVE_SYNC_ENTITY_KEY_END);
        last = req;
        Assert.assertEquals(0, concurrency);
        concurrency = c;
        break;
      case RECEIVE_SYNC_PAYLOAD:
        Assert.assertEquals(last.getAction(), ServerEntityAction.RECEIVE_SYNC_ENTITY_KEY_START);
        last = req;
        Assert.assertEquals(concurrency, c);
  //  make sure no invokes deferred to the end of a concurrency key
        Assert.assertFalse(invoked);
        break;
      case RECEIVE_SYNC_ENTITY_KEY_END:
        Assert.assertEquals(last.getAction(), ServerEntityAction.RECEIVE_SYNC_PAYLOAD);
        last = req;
        Assert.assertEquals(concurrency, c);
        concurrency = 0;
        break;
      case RECEIVE_SYNC_ENTITY_END:
        Assert.assertEquals(last.getAction(), ServerEntityAction.RECEIVE_SYNC_ENTITY_KEY_END);
        last = req;
        invoked = false;
        break;
      case INVOKE_ACTION:
        Assert.assertTrue(last.getAction() == ServerEntityAction.RECEIVE_SYNC_PAYLOAD);
        int sid = ByteBuffer.wrap(payload).getInt();
        Assert.assertEquals(lastSid + 1, sid);
        Assert.assertEquals(concurrency, c);
        lastSid = sid;
  //  make sure no invokes deferred to the end
        invoked = true;
  //  don't sert last
        break;
      default:
        break;
    }
  }
  
  private void mockPassiveSync(ReplicatedTransactionHandler rth) throws EventHandlerException {
//  start passive sync
    EntityID eid = new EntityID("foo", "bar");
    long VERSION = 1;
    byte[] config = new byte[0];
    send(SyncReplicationActivity.createStartSyncMessage());
    send(SyncReplicationActivity.createStartEntityMessage(eid, VERSION, config, 0));
    send(SyncReplicationActivity.createStartEntityKeyMessage(eid, VERSION, 1));
    send(SyncReplicationActivity.createPayloadMessage(eid, VERSION, 1, config, ""));
    send(SyncReplicationActivity.createEndEntityKeyMessage(eid, VERSION, 1));
    send(SyncReplicationActivity.createStartEntityKeyMessage(eid, VERSION, 2));
    send(SyncReplicationActivity.createPayloadMessage(eid, VERSION, 2, config, ""));
    send(SyncReplicationActivity.createEndEntityKeyMessage(eid, VERSION, 2));  
    send(SyncReplicationActivity.createStartEntityKeyMessage(eid, VERSION, 3));
    send(SyncReplicationActivity.createPayloadMessage(eid, VERSION, 3, config, ""));
    send(SyncReplicationActivity.createEndEntityKeyMessage(eid, VERSION, 3));  
    send(SyncReplicationActivity.createStartEntityKeyMessage(eid, VERSION, 4));
//  defer a few replicated messages with sequence as payload
    send(createMockReplicationMessage(eid, VERSION, ByteBuffer.wrap(new byte[Integer.BYTES]).putInt(1).array(), 4));
    send(createMockReplicationMessage(eid, VERSION, ByteBuffer.wrap(new byte[Integer.BYTES]).putInt(2).array(), 4));
    send(createMockReplicationMessage(eid, VERSION, ByteBuffer.wrap(new byte[Integer.BYTES]).putInt(3).array(), 4));
    send(SyncReplicationActivity.createPayloadMessage(eid, 1, 4, config, ""));
//  defer a few replicated messages with sequence as payload
    send(createMockReplicationMessage(eid, VERSION, ByteBuffer.wrap(new byte[Integer.BYTES]).putInt(4).array(), 4));
    send(createMockReplicationMessage(eid, VERSION, ByteBuffer.wrap(new byte[Integer.BYTES]).putInt(5).array(), 4));
    send(createMockReplicationMessage(eid, VERSION, ByteBuffer.wrap(new byte[Integer.BYTES]).putInt(6).array(), 4));
    send(SyncReplicationActivity.createEndEntityKeyMessage(eid, 1, 4)); 
    send(SyncReplicationActivity.createEndEntityMessage(eid, VERSION));
    send(SyncReplicationActivity.createEndSyncMessage(new byte[0]));
  }

  private long send(SyncReplicationActivity activity) throws EventHandlerException {
    ReplicationMessage msg = createReceivedActivity(activity);
    msg.setReplicationID(rid++);
    loopbackSink.addSingleThreaded(msg);
    return rid;
  }
  @After
  public void tearDown() throws Exception {
    this.rth.getEventHandler().destroy();
  }
  
  private SyncReplicationActivity createMockReplicationMessage(EntityID eid, long VERSION, byte[] payload, int concurrency) {
    return SyncReplicationActivity.createReplicatedMessage(new EntityDescriptor(eid, ClientInstanceID.NULL_ID, VERSION), 
        source, TransactionID.NULL_ID, TransactionID.NULL_ID, SyncReplicationActivity.ActivityType.INVOKE_ACTION, payload, concurrency, "");
  }

  private static abstract class NoStatsSink<T> implements Sink<T> {
    @Override
    public void enableStatsCollection(boolean enable) {
      throw new UnsupportedOperationException();
    }
    @Override
    public boolean isStatsCollectionEnabled() {
      throw new UnsupportedOperationException();
    }
    @Override
    public Stats getStats(long frequency) {
      throw new UnsupportedOperationException();
    }
    @Override
    public Stats getStatsAndReset(long frequency) {
      throw new UnsupportedOperationException();
    }
    @Override
    public void resetStats() {
      throw new UnsupportedOperationException();
    }
    @Override
    public void addSpecialized(SpecializedEventContext specialized) {
      throw new UnsupportedOperationException();
    }
    @Override
    public int size() {
      throw new UnsupportedOperationException();
    }
    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }
  }


  private static class ForwardingSink extends NoStatsSink<ReplicationMessage> {
    private final EventHandler<ReplicationMessage> target;

    public ForwardingSink(EventHandler<ReplicationMessage> voltronMessageHandler) {
      this.target = voltronMessageHandler;
    }

    @Override
    public void addSingleThreaded(ReplicationMessage context) {
      try {
        this.target.handleEvent(context);
      } catch (EventHandlerException e) {
        Assert.fail();
      }
    }
    @Override
    public void addMultiThreaded(ReplicationMessage context) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setClosed(boolean closed) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
    
    
}


  /**
   * Wraps the activity in a ReplicationMessage but also emulates its "received from network" state.
   */
  private static ReplicationMessage createReceivedActivity(SyncReplicationActivity activity) {
    ReplicationMessage sending = ReplicationMessage.createActivityContainer(activity);
    TCByteBufferOutput output = new TCByteBufferOutputStream();
    sending.serializeTo(output);
    TCByteBufferInput input = new TCByteBufferInputStream(output.toArray());
    ReplicationMessage receiving = new ReplicationMessage();
    try {
      receiving.deserializeFrom(input);
    } catch (IOException e) {
      // Not expected in test.
      e.printStackTrace();
      Assert.fail(e.getLocalizedMessage());
    }
    return receiving;
  }
}
