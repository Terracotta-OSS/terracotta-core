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
import com.tc.entity.NetworkVoltronEntityMessage;
import com.tc.entity.VoltronEntityAppliedResponse;
import com.tc.entity.VoltronEntityMessage;
import com.tc.entity.VoltronEntityReceivedResponse;
import com.tc.l2.msg.ReplicationMessage;
import com.tc.l2.state.StateManager;
import com.tc.net.ClientID;
import com.tc.net.ServerID;
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
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.objectserver.core.api.ITopologyEventCollector;
import com.tc.objectserver.entity.ClientEntityStateManager;
import com.tc.objectserver.entity.PlatformEntity;
import com.tc.objectserver.persistence.EntityPersistor;
import com.tc.objectserver.persistence.TransactionOrderPersistor;
import com.tc.services.TerracottaServiceProviderRegistry;
import com.tc.stats.Stats;
import com.tc.util.Assert;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;


public class ReplicatedTransactionHandlerTest {
  private TerracottaServiceProviderRegistry terracottaServiceProviderRegistry;
  private EntityPersistor entityPersistor;
  private TransactionOrderPersistor transactionOrderPersistor;
  private ReplicatedTransactionHandler rth;
  private ClientID source;
  private ForwardingSink loopbackSink;
  private RunnableSink requestProcessorSink;
  private ClientEntityStateManager clientEntityStateManager;
  private ITopologyEventCollector eventCollector;
  private StateManager stateManager;
  private EntityManager entityManager;
  private GroupManager groupManager;
  
  
  @Before
  public void setUp() throws Exception {
    this.terracottaServiceProviderRegistry = mock(TerracottaServiceProviderRegistry.class);
    this.entityPersistor = mock(EntityPersistor.class);
    this.transactionOrderPersistor = mock(TransactionOrderPersistor.class);
    this.stateManager = mock(StateManager.class);
    this.entityManager = mock(EntityManager.class);
    this.groupManager = mock(GroupManager.class);
    when(entityManager.getEntity(Matchers.eq(PlatformEntity.PLATFORM_ID), Matchers.eq(PlatformEntity.VERSION))).thenReturn(Optional.of(mock(ManagedEntity.class)));
    this.rth = new ReplicatedTransactionHandler(stateManager, this.transactionOrderPersistor, this.entityManager, this.entityPersistor, this.groupManager);
    this.source = mock(ClientID.class);
    
    MessageChannel messageChannel = mock(MessageChannel.class);
    when(messageChannel.createMessage(TCMessageType.VOLTRON_ENTITY_APPLIED_RESPONSE)).thenReturn(mock(VoltronEntityAppliedResponse.class));
    when(messageChannel.createMessage(TCMessageType.VOLTRON_ENTITY_RECEIVED_RESPONSE)).thenReturn(mock(VoltronEntityReceivedResponse.class));
    
    DSOChannelManager channelManager = mock(DSOChannelManager.class);
    when(channelManager.getActiveChannel(this.source)).thenReturn(messageChannel);
    
    this.loopbackSink = new ForwardingSink(this.rth.getEventHandler());
    this.requestProcessorSink = new RunnableSink();
    
    this.eventCollector = mock(ITopologyEventCollector.class);
    channelManager.addEventListener(clientEntityStateManager);
  }
  
  @Test
  public void testEntityGetsConcurrencyKey() throws Exception {
    EntityID eid = new EntityID("foo", "bar");
    EntityDescriptor descriptor = new EntityDescriptor(eid, ClientInstanceID.NULL_ID, 1);
    ServerID sid = new ServerID("test", "test".getBytes());
    ManagedEntity entity = mock(ManagedEntity.class);
    ReplicationMessage msg = mock(ReplicationMessage.class);
    int rand = new Random().nextInt();
    when(msg.getConcurrency()).thenReturn(rand);
    when(msg.getType()).thenReturn(ReplicationMessage.REPLICATE);
    when(msg.getReplicationType()).thenReturn(ReplicationMessage.ReplicationType.INVOKE_ACTION);
    when(msg.getEntityID()).thenReturn(eid);
    when(msg.messageFrom()).thenReturn(sid);
    when(msg.getEntityDescriptor()).thenReturn(descriptor);
    when(msg.getOldestTransactionOnClient()).thenReturn(TransactionID.NULL_ID);
    when(this.entityManager.getEntity(Matchers.any(), Matchers.anyInt())).thenReturn(Optional.of(entity));
    Mockito.doAnswer(invocation->{
      ((ServerEntityRequest)invocation.getArguments()[0]).complete(new byte[0]);
      return null;
    }).when(entity).addInvokeRequest(Matchers.any(), Matchers.any(), Matchers.eq(rand));
    this.loopbackSink.addSingleThreaded(msg);
    verify(entity).addInvokeRequest(Matchers.any(), Matchers.any(), Matchers.eq(rand));
    verify(groupManager).sendTo(Matchers.eq(sid), Matchers.any());
  }
  
  @After
  public void tearDown() throws Exception {
    this.rth.getEventHandler().destroy();
  }
  



  /**
   * This is pulled out as its own helper since the mocked EntityIDs aren't .equals() each other so using the same
   * instance gives convenient de facto equality.
   */
  private EntityID createMockEntity(String entityName) {
    EntityID entityID = mock(EntityID.class);
    // We will use the TestEntity since we only want to proceed with the check, not actually create it (this is from entity-api).
    when(entityID.getClassName()).thenReturn("org.terracotta.TestEntity");
    when(entityID.getEntityName()).thenReturn(entityName);
    return entityID;
  }

  private NetworkVoltronEntityMessage createMockRequest(VoltronEntityMessage.Type type, EntityID entityID, TransactionID transactionID) {
    NetworkVoltronEntityMessage request = mock(NetworkVoltronEntityMessage.class);
    when(request.getSource()).thenReturn(this.source);
    when(request.getVoltronType()).thenReturn(type);
    EntityDescriptor entityDescriptor = mock(EntityDescriptor.class);
    when(entityDescriptor.getClientSideVersion()).thenReturn((long) 1);
    when(entityDescriptor.getEntityID()).thenReturn(entityID);
    when(request.getEntityDescriptor()).thenReturn(entityDescriptor);
    when(request.getTransactionID()).thenReturn(transactionID);
    when(request.getOldestTransactionOnClient()).thenReturn(new TransactionID(0));
    return request;
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


  private static class RunnableSink extends NoStatsSink<Runnable> {
    private final Queue<Runnable> runnableQueue;

    public RunnableSink() {
      this.runnableQueue = new LinkedList<>();
    }

    public void runUntilEmpty() {
      while (!this.runnableQueue.isEmpty()) {
        Runnable task = this.runnableQueue.poll();
        task.run();
      }
    }
    @Override
    public void addSingleThreaded(Runnable context) {
      throw new UnsupportedOperationException();
    }
    @Override
    public void addMultiThreaded(Runnable context) {
      this.runnableQueue.add(context);
    }

    @Override
    public void setClosed(boolean closed) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
}
