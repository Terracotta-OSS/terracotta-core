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

import com.tc.async.api.EventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Sink;
import com.tc.async.api.SpecializedEventContext;
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
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.FetchID;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.objectserver.entity.MessagePayload;
import com.tc.objectserver.entity.PlatformEntity;
import com.tc.objectserver.entity.SimpleCompletion;
import com.tc.objectserver.persistence.EntityPersistor;
import com.tc.objectserver.persistence.Persistor;
import com.tc.objectserver.persistence.TransactionOrderPersistor;
import com.tc.stats.Stats;
import com.tc.util.Assert;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.MessageCodec;


public class ReplicatedTransactionHandlerTest {
  private EntityPersistor entityPersistor;
  private TransactionOrderPersistor transactionOrderPersistor;
  private ReplicatedTransactionHandler rth;
  private ClientID source;
  private ForwardingSink<ReplicationMessage> loopbackSink;
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
    
    Persistor persistor = mock(Persistor.class);
    when(persistor.getEntityPersistor()).thenReturn(this.entityPersistor);
    when(persistor.getTransactionOrderPersistor()).thenReturn(this.transactionOrderPersistor);
    
    this.stateManager = mock(StateManager.class);
    this.entityManager = mock(EntityManager.class);
    this.groupManager = mock(GroupManager.class);
    Mockito.doAnswer((Answer<Void>) (InvocationOnMock invocation) -> {
      ((Runnable)invocation.getArguments()[2]).run();
      return null;
    }).when(groupManager).sendToWithSentCallback(Mockito.any(), Mockito.any(), Mockito.any());
    this.platform = mock(ManagedEntity.class);
    Mockito.doAnswer((Answer<SimpleCompletion>) (InvocationOnMock invocation) -> {
      ((Consumer<byte[]>)invocation.getArguments()[3]).accept(null);
      return null;
    }).when(platform).addRequestMessage(any(ServerEntityRequest.class), any(MessagePayload.class), any(Runnable.class), any(Consumer.class), any(Consumer.class));
    when(entityManager.getEntity(Matchers.eq(EntityDescriptor.createDescriptorForLifecycle(PlatformEntity.PLATFORM_ID, 1L)))).thenReturn(Optional.of(platform));
    this.rth = new ReplicatedTransactionHandler(stateManager, persistor, this.entityManager, this.groupManager);
    this.rth.setOutgoingResponseSink(new ForwardingSink<ReplicatedTransactionHandler.SedaToken>(this.rth.getOutgoingResponseHandler()));
    // We need to do things like serialize/deserialize this so we can't easily use a mocked source.
    this.source = new ClientID(1);
    
    MessageChannel messageChannel = mock(MessageChannel.class);
    when(messageChannel.createMessage(TCMessageType.VOLTRON_ENTITY_COMPLETED_RESPONSE)).thenReturn(mock(VoltronEntityAppliedResponse.class));
    when(messageChannel.createMessage(TCMessageType.VOLTRON_ENTITY_RECEIVED_RESPONSE)).thenReturn(mock(VoltronEntityReceivedResponse.class));
    
    DSOChannelManager channelManager = mock(DSOChannelManager.class);
    when(channelManager.getActiveChannel(this.source)).thenReturn(messageChannel);
        
    this.loopbackSink = new ForwardingSink<ReplicationMessage>(this.rth.getEventHandler());
  }
    
  private void sendNoop(EntityID eid, FetchID fetch, ServerEntityAction action) {
    ReplicationMessage flush = ReplicationMessage.createLocalContainer(SyncReplicationActivity.createFlushLocalPipelineMessage(fetch, action == ServerEntityAction.DESTROY_ENTITY));
    loopbackSink.addSingleThreaded(flush);
  }
  
  @Test
  public void testEntityNoIgnoresDuringSyncOfKey() throws Exception {
    EntityID eid = new EntityID("foo", "bar");
    FetchID fetch = new FetchID(10L);
    ServerID sid = new ServerID("test", "test".getBytes());
    ManagedEntity entity = mock(ManagedEntity.class);
    SyncReplicationActivity activity = mock(SyncReplicationActivity.class);
    int rand = 1;
    when(activity.getConcurrency()).thenReturn(rand);
    when(activity.getActivityType()).thenReturn(SyncReplicationActivity.ActivityType.INVOKE_ACTION);
    when(activity.getEntityID()).thenReturn(eid);
    when(activity.getFetchID()).thenReturn(fetch);
    when(activity.getOldestTransactionOnClient()).thenReturn(TransactionID.NULL_ID);
    when(activity.getExtendedData()).thenReturn(new byte[0]);
    when(activity.getActivityID()).thenReturn(SyncReplicationActivity.ActivityID.getNextID());
    ReplicationMessage msg = mock(ReplicationMessage.class);
    when(msg.messageFrom()).thenReturn(sid);
    when(msg.getActivities()).thenReturn(Collections.singletonList(activity));
    when(entity.getCodec()).thenReturn(mock(MessageCodec.class));
    when(this.entityManager.getEntity(Matchers.any())).thenReturn(Optional.empty());
    when(this.entityManager.createEntity(Matchers.any(), anyLong(), anyLong(), anyBoolean())).then((invoke)->{
      when(this.entityManager.getEntity(Matchers.any())).thenReturn(Optional.of(entity));
      return entity;
    });
    Mockito.doAnswer(invocation->{
      Runnable received = (Runnable)invocation.getArguments()[2];
      if (received != null) {
        // received
        received.run();
      }
      Consumer<byte[]> consumer = (Consumer)invocation.getArguments()[3];
      if (consumer != null) {
        // ack
        consumer.accept(new byte[0]);
      }
      // NOTE:  We don't retire replicated messages.
      return null;
    }).when(entity).addRequestMessage(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any());
    SyncReplicationActivity.EntityCreationTuple[] entitiesToSync = {
        new SyncReplicationActivity.EntityCreationTuple(eid, 1, 10L, new byte[0], true)
    };
    int referenceCount = 0;
    this.loopbackSink.addSingleThreaded(createReceivedActivity(SyncReplicationActivity.createStartSyncMessage(entitiesToSync)));
    this.loopbackSink.addSingleThreaded(createReceivedActivity(SyncReplicationActivity.createStartEntityMessage(entitiesToSync[0].id, entitiesToSync[0].version, new FetchID(entitiesToSync[0].consumerID), entitiesToSync[0].configPayload, referenceCount)));
    this.loopbackSink.addSingleThreaded(createReceivedActivity(SyncReplicationActivity.createStartEntityKeyMessage(entitiesToSync[0].id, entitiesToSync[0].version, new FetchID(entitiesToSync[0].consumerID), rand)));
    this.loopbackSink.addSingleThreaded(msg);
    this.loopbackSink.addSingleThreaded(createReceivedActivity(SyncReplicationActivity.createEndEntityKeyMessage(entitiesToSync[0].id, entitiesToSync[0].version, new FetchID(entitiesToSync[0].consumerID), rand)));
    this.loopbackSink.addSingleThreaded(createReceivedActivity(SyncReplicationActivity.createEndEntityMessage(entitiesToSync[0].id, entitiesToSync[0].version, new FetchID(entitiesToSync[0].consumerID))));
    this.loopbackSink.addSingleThreaded(createReceivedActivity(SyncReplicationActivity.createEndSyncMessage(new byte[0])));
    verify(activity).getExtendedData();
    // Note that we want to verify 2 ACK messages:  RECEIVED and COMPLETED.
    verify(groupManager, times(2)).sendToWithSentCallback(Matchers.eq(sid), Matchers.any(), Matchers.any());
  }  
  
  @Test
  public void testEntityGetsConcurrencyKey() throws Exception {
    EntityID eid = new EntityID("foo", "bar");
    FetchID fetch = new FetchID(1L);
    ServerID sid = new ServerID("test", "test".getBytes());
    ManagedEntity entity = mock(ManagedEntity.class);
    SyncReplicationActivity activity = mock(SyncReplicationActivity.class);
    int rand = new Random().nextInt();
    when(activity.getConcurrency()).thenReturn(rand);
    when(activity.getActivityType()).thenReturn(SyncReplicationActivity.ActivityType.INVOKE_ACTION);
    when(activity.getEntityID()).thenReturn(eid);
    when(activity.getFetchID()).thenReturn(fetch);
    when(activity.getOldestTransactionOnClient()).thenReturn(TransactionID.NULL_ID);
    when(activity.getActivityID()).thenReturn(SyncReplicationActivity.ActivityID.getNextID());
    ReplicationMessage msg = mock(ReplicationMessage.class);
    MessageCodec codec = mock(MessageCodec.class);
    when(msg.messageFrom()).thenReturn(sid);
    when(msg.getActivities()).thenReturn(Collections.singletonList(activity));
    when(this.entityManager.getEntity(Matchers.any())).thenReturn(Optional.of(entity));
    when(entity.getCodec()).thenReturn(codec);
    when(this.entityManager.getMessageCodec(Matchers.any())).thenReturn(codec);
    Mockito.doAnswer(invocation->{
      Runnable received = (Runnable)invocation.getArguments()[2];
      if (received != null) {
        received.run();
      }
      Consumer<byte[]> consumer = (Consumer)invocation.getArguments()[3];
      if (consumer != null) {
        consumer.accept(new byte[0]);
      }
      // NOTE:  We don't retire replicated messages.
      return null;
    }).when(entity).addRequestMessage(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any());
    this.loopbackSink.addSingleThreaded(createReceivedActivity(SyncReplicationActivity.createStartSyncMessage(new SyncReplicationActivity.EntityCreationTuple[0])));
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
    verify(platform).addRequestMessage(Matchers.any(ServerEntityRequest.class), Matchers.any(MessagePayload.class), Matchers.any(), Matchers.any(), Matchers.any());
  }
  
  @Test
  public void testLifecycleGetsJournaled() throws Exception {
    EntityID eid = new EntityID("test","test");
    FetchID fetch = new FetchID(1L);
    ClientID cid = new ClientID(1L);
    TransactionID tid = new TransactionID(1L);
    TransactionID old = new TransactionID(0L);
    ReplicationMessage start = ReplicationMessage.createLocalContainer(SyncReplicationActivity.createStartSyncMessage(new SyncReplicationActivity.EntityCreationTuple[0]));
    ByteArrayOutputStream raw = new ByteArrayOutputStream();
    ObjectOutputStream out = new ObjectOutputStream(raw);
    out.writeInt(0);
    out.close();
    ReplicationMessage end = ReplicationMessage.createLocalContainer(SyncReplicationActivity.createEndSyncMessage(raw.toByteArray()));
    ReplicationMessage create = ReplicationMessage.createLocalContainer(SyncReplicationActivity.createLifecycleMessage(eid,1L,fetch,cid,tid,old,SyncReplicationActivity.ActivityType.CREATE_ENTITY,new byte[0]));
    ManagedEntity entity = mock(ManagedEntity.class);
    when(entity.getConsumerID()).thenReturn(1L);
    when(entity.addRequestMessage(any(), any(), any(), any(), any())).then((in)->{
      ((Consumer)in.getArguments()[3]).accept(new byte[0]);
      return mock(SimpleCompletion.class);
    });
    when(entityManager.createEntity(any(), anyLong(), anyLong(), anyBoolean())).thenReturn(entity);
    when(entityManager.getEntity(EntityDescriptor.NULL_ID)).thenReturn(Optional.empty());
    this.rth.getEventHandler().handleEvent(start);
    this.rth.getEventHandler().handleEvent(end);
    this.rth.getEventHandler().handleEvent(create);
    verify(entityPersistor).entityCreated(eq(cid), eq(tid.toLong()), eq(old.toLong()), eq(eid), eq(1L), eq(1L), eq(true), any(byte[].class));
  }
  
  @Test
  public void testManagedEntityGC() throws Exception {
    EntityID entityID = new EntityID("TEST", "TEST");
    FetchID fetchID = new FetchID(1L);
    ManagedEntity entity = mock(ManagedEntity.class);
    Mockito.doAnswer(invoked->{
      if (((ServerEntityRequest)invoked.getArguments()[0]).getAction() != ServerEntityAction.LOCAL_FLUSH) {
        sendNoop(entityID, fetchID, ((ServerEntityRequest)invoked.getArguments()[0]).getAction());
      }
      return null;
    }).when(entity).addRequestMessage(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    when(entity.getID()).thenReturn(entityID);
    when(entity.getConsumerID()).thenReturn(1L);
    when(this.entityManager.getEntity(Matchers.any())).then(invoke->{
      EntityDescriptor desp = (EntityDescriptor)invoke.getArguments()[0];
      if (desp.isIndexed() && desp.getFetchID().equals(fetchID)) {
        return Optional.of(entity);
      } else if (!desp.isIndexed() && desp.getEntityID().equals(entityID)) {
        return Optional.of(entity);
      } else {
        return Optional.empty();
      }
    });
    
    when(this.entityManager.createEntity(Mockito.eq(entityID), Mockito.anyLong(), anyLong(), anyBoolean())).thenReturn(entity);
    this.rth.getEventHandler().handleEvent(ReplicationMessage.createLocalContainer(SyncReplicationActivity.createStartSyncMessage(new SyncReplicationActivity.EntityCreationTuple[0])));
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    ObjectOutputStream out = new ObjectOutputStream(bout);
    out.writeInt(0);
    out.close();
    this.rth.getEventHandler().handleEvent(ReplicationMessage.createLocalContainer(SyncReplicationActivity.createEndSyncMessage(bout.toByteArray())));
    ReplicationMessage request = createMockRequest(SyncReplicationActivity.createLifecycleMessage(entityID, 1L, fetchID, ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, SyncReplicationActivity.ActivityType.CREATE_ENTITY, new byte[0]));
    try {
      this.rth.getEventHandler().handleEvent(request);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
    ReplicationMessage destroy = createMockRequest(SyncReplicationActivity.createLifecycleMessage(entityID, 1L, fetchID, ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, SyncReplicationActivity.ActivityType.DESTROY_ENTITY, new byte[0]));
    when(entity.isRemoveable()).thenReturn(Boolean.TRUE);
    this.rth.getEventHandler().handleEvent(destroy);
    verify(entityManager).removeDestroyed(eq(fetchID));
  }
  
  @Test
  public void testTestDefermentDuringSync() throws Exception {
    ManagedEntity entity = mock(ManagedEntity.class);
    MessageCodec codec = mock(MessageCodec.class);
    when(this.entityManager.getEntity(Matchers.any())).thenReturn(Optional.empty());
    when(this.entityManager.createEntity(Matchers.any(), anyLong(), anyLong(), anyBoolean())).then((invoke)->{
      when(this.entityManager.getEntity(Matchers.any())).thenReturn(Optional.of(entity));
      return entity;
    });
    when(this.entityManager.getMessageCodec(Matchers.any())).thenReturn(codec);
    when(entity.getCodec()).thenReturn(codec);
    
    Mockito.doAnswer(invocation->{
      ServerEntityRequest req = (ServerEntityRequest)invocation.getArguments()[0];
      // We will ignore the EntityMessage at index [1].
      // NOTE:  We don't retire replicated messages.
      verifySequence(req, ((MessagePayload)invocation.getArguments()[1]).getRawPayload(), ((MessagePayload)invocation.getArguments()[1]).getConcurrency());
      return null;
    }).when(entity).addRequestMessage(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any());
    Mockito.doAnswer(invocation->{
      ServerEntityRequest req = (ServerEntityRequest)invocation.getArguments()[0];
      // NOTE:  We don't retire replicated messages.
      MessagePayload payload = (MessagePayload)invocation.getArguments()[1];
      byte[] raw = (null != payload) ? payload.getRawPayload() : null;
      int concurrency = (null != payload) ? payload.getConcurrency() : ConcurrencyStrategy.MANAGEMENT_KEY;
      verifySequence(req, raw, concurrency);
      return null;
    }).when(entity).addRequestMessage(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any());
    mockPassiveSync(rth);
  }
  
  private ServerEntityRequest last;
  private int lastSid = 0;
  private int concurrency = 0;
  private boolean invoked = false;
  
  private void verifySequence(ServerEntityRequest req, byte[] payload, int c) {
    switch(req.getAction()) {
      case RECEIVE_SYNC_CREATE_ENTITY:
        Assert.assertNull(last);
        last = req;
        Assert.assertEquals(0, concurrency);
        break;
      case RECEIVE_SYNC_ENTITY_START_SYNCING:
        Assert.assertTrue(last.getAction() == ServerEntityAction.RECEIVE_SYNC_CREATE_ENTITY);
        last = req;
        Assert.assertEquals(0, concurrency);
        break;
      case RECEIVE_SYNC_ENTITY_KEY_START:
        Assert.assertTrue(last.getAction() == ServerEntityAction.RECEIVE_SYNC_ENTITY_START_SYNCING || last.getAction() == ServerEntityAction.RECEIVE_SYNC_ENTITY_KEY_END);
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
    FetchID fetch = new FetchID(10L);
    long VERSION = 1;
    byte[] config = new byte[0];
    boolean canDelete = true;
    SyncReplicationActivity.EntityCreationTuple[] entitiesToSync = {
        new SyncReplicationActivity.EntityCreationTuple(eid, VERSION, 10L, config, canDelete)
    };
    send(SyncReplicationActivity.createStartSyncMessage(entitiesToSync));
    send(SyncReplicationActivity.createStartEntityMessage(eid, VERSION, new FetchID(10L), config, 0));
    send(SyncReplicationActivity.createStartEntityKeyMessage(eid, VERSION, new FetchID(10L), 1));
    send(SyncReplicationActivity.createPayloadMessage(eid, VERSION, new FetchID(10L), 1, config, ""));
    send(SyncReplicationActivity.createEndEntityKeyMessage(eid, VERSION, new FetchID(10L), 1));
    send(SyncReplicationActivity.createStartEntityKeyMessage(eid, VERSION, new FetchID(10L), 2));
    send(SyncReplicationActivity.createPayloadMessage(eid, VERSION, new FetchID(10L), 2, config, ""));
    send(SyncReplicationActivity.createEndEntityKeyMessage(eid, VERSION, new FetchID(10L), 2));  
    send(SyncReplicationActivity.createStartEntityKeyMessage(eid, VERSION, new FetchID(10L), 3));
    send(SyncReplicationActivity.createPayloadMessage(eid, VERSION, new FetchID(10L), 3, config, ""));
    send(SyncReplicationActivity.createEndEntityKeyMessage(eid, VERSION, new FetchID(10L), 3));  
    send(SyncReplicationActivity.createStartEntityKeyMessage(eid, VERSION, new FetchID(10L), 4));
//  defer a few replicated messages with sequence as payload
    send(createMockReplicationMessage(fetch, ByteBuffer.wrap(new byte[Integer.BYTES]).putInt(1).array(), 4));
    send(createMockReplicationMessage(fetch, ByteBuffer.wrap(new byte[Integer.BYTES]).putInt(2).array(), 4));
    send(createMockReplicationMessage(fetch, ByteBuffer.wrap(new byte[Integer.BYTES]).putInt(3).array(), 4));
    send(SyncReplicationActivity.createPayloadMessage(eid, 1, new FetchID(10L), 4, config, ""));
//  defer a few replicated messages with sequence as payload
    send(createMockReplicationMessage(fetch, ByteBuffer.wrap(new byte[Integer.BYTES]).putInt(4).array(), 4));
    send(createMockReplicationMessage(fetch, ByteBuffer.wrap(new byte[Integer.BYTES]).putInt(5).array(), 4));
    send(createMockReplicationMessage(fetch, ByteBuffer.wrap(new byte[Integer.BYTES]).putInt(6).array(), 4));
    send(SyncReplicationActivity.createEndEntityKeyMessage(eid, 1, new FetchID(10L), 4)); 
    send(SyncReplicationActivity.createEndEntityMessage(eid, VERSION, new FetchID(10L)));
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
  
  private SyncReplicationActivity createMockReplicationMessage(FetchID fetchID, byte[] payload, int concurrency) {
    return SyncReplicationActivity.createInvokeMessage(fetchID,
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


  private static class ForwardingSink<M> extends NoStatsSink<M> {
    private final EventHandler<M> target;

    public ForwardingSink(EventHandler<M> voltronMessageHandler) {
      this.target = voltronMessageHandler;
    }

    @Override
    public void addSingleThreaded(M context) {
      try {
        this.target.handleEvent(context);
      } catch (EventHandlerException e) {
        Assert.fail();
      }
    }
    @Override
    public void addMultiThreaded(M context) {
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
  
  ReplicationMessage createMockRequest(SyncReplicationActivity act) {
    ReplicationMessage msg = ReplicationMessage.createLocalContainer(act);
    return msg;
  }
}
