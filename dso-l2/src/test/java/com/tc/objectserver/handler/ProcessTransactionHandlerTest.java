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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.classloader.ServiceLocator;
import com.tc.entity.NetworkVoltronEntityMessage;
import com.tc.entity.VoltronEntityAppliedResponse;
import com.tc.entity.VoltronEntityMessage;
import com.tc.entity.VoltronEntityReceivedResponse;
import com.tc.entity.VoltronEntityRetiredResponse;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.api.ReplicatedClusterStateManager;
import com.tc.l2.state.ServerMode;
import com.tc.l2.state.StateManager;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.FetchID;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ManagementTopologyEventCollector;
import com.tc.objectserver.entity.ClientEntityStateManager;
import com.tc.objectserver.entity.ClientEntityStateManagerImpl;
import com.tc.objectserver.entity.EntityManagerImpl;
import com.tc.objectserver.entity.LocalPipelineFlushMessage;
import com.tc.objectserver.entity.PassiveReplicationBroker;
import com.tc.objectserver.entity.RequestProcessor;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.persistence.EntityData;
import com.tc.objectserver.persistence.EntityPersistor;
import com.tc.objectserver.persistence.Persistor;
import com.tc.objectserver.persistence.TransactionOrderPersistor;
import com.tc.objectserver.testentity.TestEntity;
import com.tc.services.InternalServiceRegistry;
import com.tc.services.TerracottaServiceProviderRegistry;
import com.tc.stats.Stats;
import com.tc.util.Assert;
import com.tc.util.State;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import org.mockito.Matchers;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import org.terracotta.monitoring.IMonitoringProducer;


public class ProcessTransactionHandlerTest {
  private static final String TEST_ENTITY_CLASS_NAME = "com.tc.objectserver.testentity.TestEntity";
  
  private TerracottaServiceProviderRegistry terracottaServiceProviderRegistry;
  private EntityPersistor entityPersistor;
  private TransactionOrderPersistor transactionOrderPersistor;
  private ProcessTransactionHandler processTransactionHandler;
  private ClientID source;
  private ForwardingSink loopbackSink;
  private RunnableSink requestProcessorSink;
  private ClientEntityStateManager clientEntityStateManager;
  private ManagementTopologyEventCollector eventCollector;
  private EntityManagerImpl entityManager;
  
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Before
  public void setUp() throws Exception {
    this.terracottaServiceProviderRegistry = mock(TerracottaServiceProviderRegistry.class);
    when(this.terracottaServiceProviderRegistry.subRegistry(any(Long.class))).thenReturn(mock(InternalServiceRegistry.class));
    this.entityPersistor = mock(EntityPersistor.class);
    this.transactionOrderPersistor = mock(TransactionOrderPersistor.class);

    Persistor persistor = mock(Persistor.class);
    when(persistor.getEntityPersistor()).thenReturn(this.entityPersistor);
    when(persistor.getTransactionOrderPersistor()).thenReturn(this.transactionOrderPersistor);
    
    this.source = mock(ClientID.class);
    
    when(this.entityPersistor.getNextConsumerID()).thenReturn(1L);
    MessageChannel messageChannel = mock(MessageChannel.class);
    when(messageChannel.createMessage(TCMessageType.VOLTRON_ENTITY_COMPLETED_RESPONSE)).thenReturn(mock(VoltronEntityAppliedResponse.class));
    when(messageChannel.createMessage(TCMessageType.VOLTRON_ENTITY_RECEIVED_RESPONSE)).thenReturn(mock(VoltronEntityReceivedResponse.class));
    when(messageChannel.createMessage(TCMessageType.VOLTRON_ENTITY_RETIRED_RESPONSE)).thenReturn(mock(VoltronEntityRetiredResponse.class));
    
    DSOChannelManager channelManager = mock(DSOChannelManager.class);
    when(channelManager.getActiveChannel(this.source)).thenReturn(messageChannel);
    when(channelManager.getActiveChannel(Matchers.eq(ClientID.NULL_ID))).thenThrow(new NoSuchChannelException());
    
    StageManager stageManager = mock(StageManager.class);
    this.requestProcessorSink = new RunnableSink();
    Stage runnableStage = mock(Stage.class);
    when(runnableStage.getSink()).thenReturn(this.requestProcessorSink);
    
    this.clientEntityStateManager = new ClientEntityStateManagerImpl();
    this.eventCollector = new ManagementTopologyEventCollector(mock(IMonitoringProducer.class));
    this.eventCollector.serverDidEnterState(StateManager.ACTIVE_COORDINATOR, 0);
    when(stageManager.createStage(eq(ServerConfigurationContext.REQUEST_PROCESSOR_STAGE), any(), any(), anyInt(), anyInt(), anyBoolean())).thenReturn(runnableStage);
    when(stageManager.createStage(eq(ServerConfigurationContext.REQUEST_PROCESSOR_DURING_SYNC_STAGE), any(), any(), anyInt(), anyInt(), anyBoolean())).thenReturn(runnableStage);
    RequestProcessor processor = new RequestProcessor(stageManager, true);
    PassiveReplicationBroker broker = mock(PassiveReplicationBroker.class);
    when(broker.passives()).thenReturn(Collections.emptySet());
    processor.setReplication(broker);
    entityManager = new EntityManagerImpl(this.terracottaServiceProviderRegistry, clientEntityStateManager, eventCollector, processor, this::sendNoop, new ServiceLocator(this.getClass().getClassLoader()));
    entityManager.setMessageSink(mock(Sink.class));

    this.processTransactionHandler = new ProcessTransactionHandler(persistor, channelManager, entityManager, mock(Runnable.class));

    this.loopbackSink = new ForwardingSink(this.processTransactionHandler.getVoltronMessageHandler());
    Stage mockStage = mock(Stage.class);
    when(mockStage.getSink()).thenReturn(this.loopbackSink);
    when(stageManager.getStage(any(), any())).thenReturn(mockStage);

    ServerConfigurationContext cxt = mock(ServerConfigurationContext.class);
    ReplicatedClusterStateManager rep = mock(ReplicatedClusterStateManager.class);
    L2Coordinator l2 = mock(L2Coordinator.class);
    when(l2.getReplicatedClusterStateManager()).thenReturn(rep);
    when(cxt.getL2Coordinator()).thenReturn(l2);
    StateManager state = mock(StateManager.class);
    when(state.getCurrentMode()).thenReturn(ServerMode.ACTIVE);
    when(l2.getStateManager()).thenReturn(state);
    Stage stage = mock(Stage.class);
    when(stage.getSink()).thenReturn(mock(Sink.class));
    when(cxt.getStage(anyString(), any(Class.class))).thenReturn(stage);
    when(cxt.getClientHandshakeManager()).thenReturn(mock(ServerClientHandshakeManager.class));
    this.processTransactionHandler.getVoltronMessageHandler().initializeContext(cxt);
    this.processTransactionHandler.reconnectComplete();
    Thread.currentThread().setName(ServerConfigurationContext.VOLTRON_MESSAGE_STAGE);
  }
  
  private void sendNoop(EntityID eid, FetchID fetch, ServerEntityAction action) {
    loopbackSink.addToSink(new LocalPipelineFlushMessage(EntityDescriptor.createDescriptorForInvoke(fetch, ClientInstanceID.NULL_ID), (action == ServerEntityAction.DESTROY_ENTITY)));
  }
  
  @After
  public void tearDown() throws Exception {
    this.processTransactionHandler.getVoltronMessageHandler().destroy();
  }
  
  @Test
  public void testGetUnknownEntity() throws Exception {
    // Send in the GET as a simple request.
    String entityName = "foo";
    EntityID entityID = createMockEntity(entityName);
    NetworkVoltronEntityMessage request = createMockRequest(VoltronEntityMessage.Type.FETCH_ENTITY, entityID, new TransactionID(1));
    this.processTransactionHandler.getVoltronMessageHandler().handleEvent(request);
  }
  
  @Test
  public void testLoadExisting() throws Exception {
    // Set up a believable collection of persistent entities.
    EntityData.Value data = new EntityData.Value();
    data.className = TestEntity.class.getCanonicalName();
    data.version = TestEntity.VERSION;
    data.consumerID = 1;
    data.entityName = "foo";
    data.configuration = new byte[0];
    when(this.entityPersistor.loadEntityData()).thenReturn(Collections.singleton(data));
    
    // Now, run the test - we don't expect any problems.
    this.processTransactionHandler.loadExistingEntities();
  }
  
  @Test
  public void testFailOnLoadVersionMismatch() throws Exception {
    EntityData.Value data = new EntityData.Value();
    data.className = TEST_ENTITY_CLASS_NAME;
    data.version = TestEntity.VERSION + 1;
    data.consumerID = 1;
    data.entityName = "foo";
    data.configuration = new byte[0];
    when(this.entityPersistor.loadEntityData()).thenReturn(Collections.singleton(data));
    
    // Now, run the test - we expect an IllegalArgumentException.
    try {
      this.processTransactionHandler.loadExistingEntities();
      // We shouldn't continue past the previous line.
      Assert.fail();
    } catch (IllegalArgumentException e) {
      // Expected.
    }
  }
  
  @Test
  public void testManagedEntityGC() throws Exception {
    // Send in the GET as a simple request.
    String entityName = "foo";
    EntityID entityID = createMockEntity(entityName);
    NetworkVoltronEntityMessage request = createMockRequest(VoltronEntityMessage.Type.CREATE_ENTITY, entityID, new TransactionID(1));
    this.processTransactionHandler.getVoltronMessageHandler().handleEvent(request);
    this.requestProcessorSink.runUntilEmpty();
    Assert.assertTrue(entityManager.getEntity(EntityDescriptor.createDescriptorForLifecycle(entityID, 1L)).isPresent());
    NetworkVoltronEntityMessage destroy = createMockRequest(VoltronEntityMessage.Type.DESTROY_ENTITY, entityID, new TransactionID(1));
    this.processTransactionHandler.getVoltronMessageHandler().handleEvent(destroy);
    this.requestProcessorSink.runUntilEmpty();
    Assert.assertFalse(entityManager.getEntity(EntityDescriptor.createDescriptorForLifecycle(entityID, 1L)).isPresent());
  }
    

  @Test
  public void testChannelManagement() throws Exception {    
    // Set up the channel.
    try {
    this.requestProcessorSink.runUntilEmpty();
    String entityName = "foo";
    EntityID entityID = createMockEntity(entityName);
    NetworkVoltronEntityMessage createRequest = createMockRequest(VoltronEntityMessage.Type.CREATE_ENTITY, entityID, new TransactionID(1));
    this.processTransactionHandler.getVoltronMessageHandler().handleEvent(createRequest);
    this.requestProcessorSink.runUntilEmpty();
    NetworkVoltronEntityMessage fetchRequest = createMockRequest(VoltronEntityMessage.Type.FETCH_ENTITY, entityID, new TransactionID(2));
    this.processTransactionHandler.getVoltronMessageHandler().handleEvent(fetchRequest);
    this.requestProcessorSink.runUntilEmpty();
    this.clientEntityStateManager.clientDisconnected((ClientID)this.source);
    this.requestProcessorSink.runUntilEmpty();
    } catch (Throwable e) {
      e.printStackTrace();
      throw e;
    }
  }

  /**
   * Tests to make sure that even synthetic messages (those without client channels) are still properly retired.
   * This test emulates how EntityMessengerService interacts with the handler.
   */
  @Test
  public void testRetireSyntheticMessage() throws Exception {
    // Create the entity ID.
    String entityName = "foo";
    EntityID entityID = createMockEntity(entityName);
    
    // We first need to create an entity we can invoke, later.
    NetworkVoltronEntityMessage createRequest = createMockRequest(VoltronEntityMessage.Type.CREATE_ENTITY, entityID, new TransactionID(1));
    this.processTransactionHandler.getVoltronMessageHandler().handleEvent(createRequest);
    this.requestProcessorSink.runUntilEmpty();
    
    // Create the message with no sender.
    ClientID sender = new ClientID(-1);
    NetworkVoltronEntityMessage invokeRequest = createMockRequestWithSender(VoltronEntityMessage.Type.INVOKE_ACTION, entityID, new TransactionID(2), sender);
    // Send the message.
    try {
      this.processTransactionHandler.getVoltronMessageHandler().handleEvent(invokeRequest);
      this.requestProcessorSink.runUntilEmpty();

      // The only way to observe that this was properly cleaned is to destroy it and ensure that there are no assertion failures when the RetirementManager comes down.
      NetworkVoltronEntityMessage destroyRequest = createMockRequest(VoltronEntityMessage.Type.DESTROY_ENTITY, entityID, new TransactionID(3));
      this.processTransactionHandler.getVoltronMessageHandler().handleEvent(destroyRequest);
      this.requestProcessorSink.runUntilEmpty();
    } catch (Throwable t) {
      t.printStackTrace();
      throw t;
    }
  }


  /**
   * This is pulled out as its own helper since the mocked EntityIDs aren't .equals() each other so using the same
   * instance gives convenient de facto equality.
   */
  private EntityID createMockEntity(String entityName) {
    EntityID entityID = mock(EntityID.class);
    // We will use the TestEntity since we only want to proceed with the check, not actually create it (this is from entity-api).
    when(entityID.getClassName()).thenReturn(TEST_ENTITY_CLASS_NAME);
    when(entityID.getEntityName()).thenReturn(entityName);
    return entityID;
  }

  private NetworkVoltronEntityMessage createMockRequest(VoltronEntityMessage.Type type, EntityID entityID, TransactionID transactionID) {
    return createMockRequestWithSender(type, entityID, transactionID, this.source);
  }

  private NetworkVoltronEntityMessage createMockRequestWithSender(VoltronEntityMessage.Type type, EntityID entityID, TransactionID transactionID, ClientID sender) {
    NetworkVoltronEntityMessage request = mock(NetworkVoltronEntityMessage.class);
    when(request.getSource()).thenReturn(sender);
    when(request.getVoltronType()).thenReturn(type);
    EntityDescriptor entityDescriptor = null;
    switch(type) {
      case INVOKE_ACTION:
        entityDescriptor = EntityDescriptor.createDescriptorForInvoke(new FetchID(1L), new ClientInstanceID(1));
        break;
      case FETCH_ENTITY:
        entityDescriptor = EntityDescriptor.createDescriptorForFetch(entityID, 1, new ClientInstanceID(1));
        break;
      default:
        entityDescriptor = EntityDescriptor.createDescriptorForLifecycle(entityID, 1);
        break;
    }

    when(request.getEntityDescriptor()).thenReturn(entityDescriptor);
    when(request.getTransactionID()).thenReturn(transactionID);
    when(request.getOldestTransactionOnClient()).thenReturn(new TransactionID(1));
    // Return an empty byte[], for now.
    when(request.getExtendedData()).thenReturn(new byte[0]);
    return request;
  }


  public static abstract class NoStatsSink<T> implements Sink<T> {

  }


  public static class RunnableSink extends NoStatsSink<Runnable> {
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
    public void addToSink(Runnable context) {
      this.runnableQueue.add(context);
    }
  }


  private static class ForwardingSink extends NoStatsSink<VoltronEntityMessage> {
    private final AbstractEventHandler<VoltronEntityMessage> target;

    public ForwardingSink(AbstractEventHandler<VoltronEntityMessage> voltronMessageHandler) {
      this.target = voltronMessageHandler;
    }

    @Override
    public void addToSink(VoltronEntityMessage context) {
      try {
        this.target.handleEvent(context);
      } catch (EventHandlerException e) {
        Assert.fail();
      }
    }
  }
}
