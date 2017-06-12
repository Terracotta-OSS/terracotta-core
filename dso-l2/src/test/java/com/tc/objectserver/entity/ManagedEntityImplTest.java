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
import com.tc.net.ClientID;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityID;
import com.tc.object.FetchID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.api.ManagementKeyCallback;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.objectserver.api.ServerEntityResponse;
import com.tc.objectserver.core.api.ITopologyEventCollector;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.entity.RequestProcessor.EntityRequest;
import com.tc.objectserver.testentity.TestEntity;
import com.tc.services.InternalServiceRegistry;
import com.tc.util.Assert;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.CommonServerEntity;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.EntityServerService;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.ExecutionStrategy;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.entity.SyncMessageCodec;
import org.terracotta.exception.EntityAlreadyExistsException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class ManagedEntityImplTest {
  private EntityID entityID;
  private ClientInstanceID clientInstanceID;
  private long version;
  private long consumerID;
  private ManagedEntityImpl managedEntity;
  private ManagementKeyCallback loopback;
  private InternalServiceRegistry serviceRegistry;
  private EntityServerService<EntityMessage, EntityResponse> serverEntityService;
  private ActiveServerEntity<EntityMessage, EntityResponse> activeServerEntity;
  private PassiveServerEntity<EntityMessage, EntityResponse> passiveServerEntity;
  private Sink             executionSink;
  private RequestProcessor requestMulti;
  private ClientEntityStateManager clientEntityStateManager;
  private ITopologyEventCollector eventCollector;
  private ClientID nodeID;
  private ClientDescriptor clientDescriptor;
  private static ExecutorService exec;
  private static ExecutorService pth;
  private InvokeContextImpl invokeContext;

  @BeforeClass
  public static void setupClass() {
    exec = Executors.newSingleThreadExecutor();
    pth = Executors.newSingleThreadExecutor();
  }
  
  @AfterClass
  public static void teardownClass() {
    exec.shutdown();
    pth.shutdown();
  }

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    nodeID = mock(ClientID.class);
    entityID = new EntityID(TestEntity.class.getName(), "foo");
    clientInstanceID = new ClientInstanceID(1);
    version = 1;
    consumerID = 1;
    serviceRegistry = mock(InternalServiceRegistry.class);
    
    loopback = mock(ManagementKeyCallback.class);
    
    Mockito.doAnswer((invocation) -> {
      TestingResponse helper = mockResponse();
      invokeOnTransactionHandler(()->managedEntity.addRequestMessage(mockLocalFlushRequest(), MessagePayload.emptyPayload(), null, helper::complete, helper::failure));
      return null;
    }).when(loopback).completed(Mockito.any(EntityID.class), Mockito.any(FetchID.class), Mockito.any(ServerEntityAction.class));
    
    executionSink = mock(Sink.class);
    PassiveReplicationBroker broker = mock(PassiveReplicationBroker.class);
    when(broker.passives()).thenReturn(Collections.emptySet());
    RequestProcessor processor = new RequestProcessor(executionSink);
    processor.setReplication(broker);
    
    requestMulti = processor;
    
    Mockito.doAnswer((invoke)->{
      System.out.println(invoke.getArguments()[0]);
      exec.submit((Runnable)invoke.getArguments()[0]);
      return null;
    }).when(executionSink).addMultiThreaded(Matchers.any());
    
    activeServerEntity = mock(ActiveServerEntity.class);
    passiveServerEntity = mock(PassiveServerEntity.class);
    serverEntityService = getServerEntityService(this.activeServerEntity, this.passiveServerEntity);
    clientEntityStateManager = mock(ClientEntityStateManager.class);
    when(clientEntityStateManager.addReference(any(ClientDescriptorImpl.class), any(EntityID.class))).thenReturn(Boolean.TRUE);
    when(clientEntityStateManager.removeReference(any(ClientDescriptorImpl.class))).thenReturn(Boolean.TRUE);
    eventCollector = mock(ITopologyEventCollector.class);
    // We will start this in a passive state, as the general test case.
    boolean isInActiveState = false;
    managedEntity = new ManagedEntityImpl(entityID, version, consumerID, loopback, serviceRegistry, clientEntityStateManager, eventCollector, requestMulti, serverEntityService, isInActiveState, true);
    clientDescriptor = new ClientDescriptorImpl(nodeID, clientInstanceID);
    invokeContext=new InvokeContextImpl(clientDescriptor, 1, 1);
    invokeOnTransactionHandler(()->Thread.currentThread().setName(ServerConfigurationContext.PASSIVE_REPLICATION_STAGE));
  }
  
  private void invokeOnTransactionHandler(Runnable r) throws ExecutionException, InterruptedException {
    pth.submit((Callable)()->{
      r.run();
      return null;
    }).get();
  }
  
  private MessagePayload mockInvokePayload() {
    return MessagePayload.commonMessagePayloadBusy(new byte[0], mock(EntityMessage.class), true);
  }
  
  @SuppressWarnings("unchecked")
  private EntityServerService<EntityMessage, EntityResponse> getServerEntityService(ActiveServerEntity<EntityMessage, EntityResponse> activeServerEntity, PassiveServerEntity<EntityMessage, EntityResponse> passiveServerEntity) throws ConfigurationException {
    EntityServerService<EntityMessage, EntityResponse> entityService = mock(EntityServerService.class);
    doReturn(activeServerEntity).when(entityService).createActiveEntity(any(ServiceRegistry.class), any(byte[].class));
    doReturn(passiveServerEntity).when(entityService).createPassiveEntity(any(ServiceRegistry.class), any(byte[].class));
    when(entityService.reconfigureEntity(any(ServiceRegistry.class), any(CommonServerEntity.class), any(byte[].class))).then(invoke->{
      return invoke.getArguments()[1];
    });
    when(entityService.getExecutionStrategy(any(byte[].class))).thenReturn((message) -> {
      if (message instanceof LocationInvoke) {
        return ((LocationInvoke)message).getLocation();
      }
      return ExecutionStrategy.Location.BOTH;
    });
    when(entityService.getConcurrencyStrategy(any(byte[].class))).thenReturn(new ConcurrencyStrategy<EntityMessage>() {
      @Override
      public int concurrencyKey(EntityMessage message) {
        return MANAGEMENT_KEY;
      }

      @Override
      public Set<Integer> getKeysForSynchronization() {
        return Collections.emptySet();
      }
    });
    when(entityService.getMessageCodec()).thenReturn(new MessageCodec<EntityMessage, EntityResponse>() {
      @Override
      public byte[] encodeMessage(EntityMessage message) throws MessageCodecException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }
      
      @Override
      public EntityMessage decodeMessage(byte[] payload) throws MessageCodecException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }

      @Override
      public byte[] encodeResponse(EntityResponse response) throws MessageCodecException {
        return new byte[0];
      }

      @Override
      public EntityResponse decodeResponse(byte[] payload) throws MessageCodecException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }
    });
    return entityService;
  }
  
  @Test 
  public void testReconfigureEntity() throws Exception {
    // first create a passive entity and then promote
    ServerEntityRequest request = mockCreateEntityRequest();
    TestingResponse response = mockResponse();
    String config = "foo";
    MessagePayload arg = mockCreatePayload(config);
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(request, arg, null, response::complete, response::failure));
    response.waitFor();
    verify(response).complete(Mockito.any());
    promote();

    // first create a passive entity and then promote
    ServerEntityRequest request2 = mockReconfigureEntityRequest();
    TestingResponse response2 = mockResponse();
    String config2 = "foo2";
    MessagePayload arg2 = mockReconfigurePayload(config2);
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(request2, arg2, null, response2::complete, response2::failure));
    response2.waitFor();
    verify(response2).complete(Mockito.any());
    
    // We expected to see this as a result of the promotion.
    verify(serverEntityService).reconfigureEntity(Matchers.eq(serviceRegistry), eq(this.activeServerEntity), Matchers.eq(arg2.getRawPayload()));
  }
  
  
  @Test 
  public void testPromotePermanentEntity() throws Exception {
    // first create a passive entity and then promote
    managedEntity = new ManagedEntityImpl(entityID, version, consumerID, loopback, serviceRegistry, clientEntityStateManager, eventCollector, requestMulti, serverEntityService, false, false);
    ServerEntityRequest request = mockCreateEntityRequest();
    TestingResponse response = mockResponse();
    String config = "foo";
    MessagePayload arg = mockCreatePayload(config);
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(request, arg, null, response::complete, response::failure));
    response.waitFor();
    verify(response).complete(Mockito.any());
    promote();

    ServerEntityRequest request2 = mockDestroyEntityRequest();
    TestingResponse response2 = mockResponse();
    MessagePayload arg2 = mockInvokePayload();
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(request2, arg2, null, response2::complete, response2::failure));
    response2.waitFor();
    verify(response2).failure(Mockito.any());
  }
  
  @Test 
  public void testExecutionStrategy() throws Exception {
    // first create a passive entity and then promote
    ServerEntityRequest request = mockCreateEntityRequest();
    TestingResponse response = mockResponse();
    String config = "foo";
    MessagePayload arg = mockCreatePayload(config);
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(request, arg, null, response::complete, response::failure));
    response.waitFor();
    verify(response).complete(Mockito.any());
//  test passive execution
    ServerEntityRequest passiveInvoke = mockExecutionInvokeRequest(ExecutionStrategy.Location.PASSIVE);
    MessagePayload loc = mockLocationPayload(ExecutionStrategy.Location.PASSIVE);
    TestingResponse piResp = mockResponse();
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(passiveInvoke, loc, null, piResp::complete, piResp::failure));
    piResp.waitFor();
    verify(passiveServerEntity).invokePassive(eq(invokeContext), any(EntityMessage.class));
    
    promote();

//  test passive only on active execution
    ServerEntityRequest activeNoop = mockExecutionInvokeRequest(ExecutionStrategy.Location.PASSIVE);
    MessagePayload anoLoc = mockLocationPayload(ExecutionStrategy.Location.PASSIVE);
    TestingResponse anoResp = mockResponse();
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(activeNoop, anoLoc, null, anoResp::complete, anoResp::failure));
    anoResp.waitFor();
    verify(activeServerEntity, Mockito.never()).invokeActive(eq(invokeContext), any(EntityMessage.class));
    
    ServerEntityRequest active = mockExecutionInvokeRequest(ExecutionStrategy.Location.ACTIVE);
    MessagePayload activeLoc = mockLocationPayload(ExecutionStrategy.Location.ACTIVE);
    TestingResponse aResp = mockResponse();
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(active, activeLoc, null, aResp::complete, aResp::failure));
    aResp.waitFor();
    verify(activeServerEntity).invokeActive(eq(invokeContext), any(EntityMessage.class));
  }  

  @Test
  public void testCreateActive() throws Exception {
    // first create a passive entity and then promote
    ServerEntityRequest request = mockCreateEntityRequest();
    TestingResponse response = mockResponse();
    String config = "foo";
    MessagePayload arg = mockCreatePayload(config);
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(request, arg, null, response::complete, response::failure));
    response.waitFor();
    verify(response).complete(Mockito.any());
    invokeOnTransactionHandler(()->{try {managedEntity.promoteEntity();} catch (ConfigurationException ce) {throw new RuntimeException(ce);}});
    
    // We expected to see this as a result of the promotion.
    verify(serverEntityService).createActiveEntity(Matchers.eq(serviceRegistry), Matchers.eq(arg.getRawPayload()));
  }
  
  @Test
  public void testNoop() throws Exception {
    TestingResponse response = mockResponse();
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(mockLocalFlushRequest(), MessagePayload.emptyPayload(), null, response::complete, response::failure));
    response.waitFor();
    verify(response).complete(Matchers.any());
  }

  @Test
  public void testCreatePassive() throws Exception {
    String config = "foo";
    ServerEntityRequest request = mockCreateEntityRequest();
    TestingResponse response = mockResponse();
    MessagePayload arg = mockCreatePayload(config);
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(request, arg, null, response::complete, response::failure));
    response.waitFor();
    verify(serverEntityService).createPassiveEntity(Matchers.eq(serviceRegistry), Matchers.eq(arg.getRawPayload()));
    verify(response).complete(Mockito.any());
  }

  @Test
  public void testDoubleCreatePassive() throws Exception {
    ServerEntityRequest request = mockCreateEntityRequest();
    TestingResponse response = mockResponse();

    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(mockCreateEntityRequest(), mockCreatePayload("foo"), null,  null, null));
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(request, mockCreatePayload("bar"), null, response::complete, response::failure));
    response.waitFor();
    verify(response).failure(any(EntityAlreadyExistsException.class));
    // No retire on passive.
    verify(response, never()).complete();
  }

  @Test
  public void testDoubleCreateActive() throws Exception {
    TestingResponse response = mockResponse();
    promote();
// We want to pretend that we are the expected thread.
    ServerEntityRequest request = mockCreateEntityRequest();
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(mockCreateEntityRequest(), mockCreatePayload("foo"), null, null, null));
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(request, mockCreatePayload("bar"), null, response::complete, response::failure));
    response.waitFor();
    verify(response).failure(any(EntityAlreadyExistsException.class));
    verify(response, never()).complete();
  }

  @Test
  public void testGetEntityMissing() throws Exception {
    TestingResponse response = mockResponse();

    promote();
    
    ClientDescriptorImpl requester = new ClientDescriptorImpl(new com.tc.net.ClientID(0), new ClientInstanceID(1));
    ServerEntityRequest request = mockGetRequest(requester);
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(request, mockInvokePayload(), null, response::complete, response::failure));
    response.waitFor();
    verify(clientEntityStateManager, never()).addReference(any(ClientDescriptorImpl.class), any(EntityID.class));
    verify(response).failure(Mockito.any());
  }

  @Test
  public void testGetEntityExists() throws Exception {    
    TestingResponse response = mockResponse();
    MessagePayload config = mockCreatePayload("foo");
    TestingResponse resp = mockResponse();
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(mockCreateEntityRequest(), config, null, resp::complete, resp::failure));
    resp.waitFor();
    promote();

    ClientDescriptorImpl requester = new ClientDescriptorImpl(new com.tc.net.ClientID(0), new ClientInstanceID(1));
    ServerEntityRequest request = mockGetRequest(requester);
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(request, mockInvokePayload(),null, response::complete, response::failure));
    response.waitFor();
    verify(clientEntityStateManager).addReference(requester, entityID);
    ArgumentCaptor<byte[]> data = ArgumentCaptor.forClass(byte[].class);
    verify(response).complete(data.capture());
    byte[] raw = data.getValue();
    Assert.assertEquals(raw.length, config.getRawPayload().length + 8);
    byte[] sub = new byte[raw.length - 8];
    System.arraycopy(raw, 8, sub, 0, raw.length - 8);
    Assert.assertEquals(sub, config.getRawPayload());
  }

  @Test
  public void testPerformActionMissingEntityPassive() throws Exception {
    //  test no longer relevant
  }

  @Test
  public void testPerformActionMissingEntityActive() throws Exception {
    //  test no longer relevant
  }

  @Test
  public void testPerformAction() throws Exception {
    TestingResponse response = mockResponse();
    byte[] returnValue = { 1 };
    when(serverEntityService.getMessageCodec()).thenReturn(new MessageCodec<EntityMessage, EntityResponse>(){
      @Override
      public byte[] encodeResponse(EntityResponse response) {
        return returnValue;
      }
      
      @Override
      public EntityMessage decodeMessage(byte[] payload) {
        return new EntityMessage() {};
      }

      @Override
      public byte[] encodeMessage(EntityMessage message) throws MessageCodecException {
        return new byte[0];
      }

      @Override
      public EntityResponse decodeResponse(byte[] payload) throws MessageCodecException {
        return new EntityResponse() {};
      }
    });
    when(serverEntityService.getSyncMessageCodec()).thenReturn(new SyncMessageCodec<EntityMessage>(){
      @Override
      public byte[] encode(int concurrencyKey, EntityMessage message) throws MessageCodecException {
        throw new UnsupportedOperationException("not supported!");
      }

      @Override
      public EntityMessage decode(int concurrencyKey, byte[] payload) throws MessageCodecException {
        throw new UnsupportedOperationException("not supported!");
      }
    });
    when(serverEntityService.getConcurrencyStrategy(Mockito.any())).thenReturn(new ConcurrencyStrategy<EntityMessage>() {
      @Override
      public int concurrencyKey(EntityMessage m) {
        return 1;
      }

      @Override
      public Set<Integer> getKeysForSynchronization() {
        return Collections.singleton(1);
      }
    });
    managedEntity = new ManagedEntityImpl(entityID, version, consumerID, loopback, serviceRegistry, clientEntityStateManager, eventCollector, requestMulti, serverEntityService, false, true);
    TestingResponse resp = mockResponse();
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(mockCreateEntityRequest(), MessagePayload.emptyPayload(), null, resp::complete, resp::failure));
    resp.waitFor();
        
    promote();


    when(activeServerEntity.invokeActive(eq(invokeContext), any(EntityMessage.class))).thenReturn
      (new
                                                                                                      EntityResponse() {});
    ServerEntityRequest invokeRequest = mockInvokeRequest();
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(invokeRequest, mockInvokePayload(), null, response::complete, response::failure));
    response.waitFor();
    verify(activeServerEntity).invokeActive(eq(invokeContext), any(EntityMessage.class));
    verify(response).complete(returnValue);
  }
  
  @Test
  public void testNoopFlush() throws Exception {
    TestingResponse response = mockResponse();
    byte[] returnValue = { 1 };
    when(serverEntityService.getMessageCodec()).thenReturn(new MessageCodec<EntityMessage, EntityResponse>(){
      @Override
      public byte[] encodeResponse(EntityResponse response) {
        return returnValue;
      }

      @Override
      public EntityMessage decodeMessage(byte[] payload) {
        return new EntityMessage() {};
      }

      @Override
      public byte[] encodeMessage(EntityMessage message) throws MessageCodecException {
        return new byte[0];
      }

      @Override
      public EntityResponse decodeResponse(byte[] payload) throws MessageCodecException {
        return new EntityResponse() {};
      }
    });
    when(serverEntityService.getSyncMessageCodec()).thenReturn(new SyncMessageCodec<EntityMessage>(){
      @Override
      public byte[] encode(int concurrencyKey, EntityMessage message) throws MessageCodecException {
        throw new UnsupportedOperationException("not supported!");
      }

      @Override
      public EntityMessage decode(int concurrencyKey, byte[] payload) throws MessageCodecException {
        throw new UnsupportedOperationException("not supported!");
      }
    });
    when(serverEntityService.getConcurrencyStrategy(Mockito.any())).thenReturn(new ConcurrencyStrategy<EntityMessage>() {
      @Override
      public int concurrencyKey(EntityMessage m) {
        return 1;
      }

      @Override
      public Set<Integer> getKeysForSynchronization() {
        return Collections.singleton(1);
      }
    });
    managedEntity = new ManagedEntityImpl(entityID, version, consumerID, loopback, serviceRegistry, clientEntityStateManager, eventCollector, requestMulti, serverEntityService, false, true);
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(mockCreateEntityRequest(), MessagePayload.emptyPayload(), null, response::complete, response::failure));
    response.waitFor();
    
    promote();

    ServerEntityRequest mgmtInvoke = mockInvokeRequest();
    ServerEntityRequest deferInvoke = mockInvokeRequest();
        
    CyclicBarrier barrier = new CyclicBarrier(2);

    when(activeServerEntity.invokeActive(eq(invokeContext), any(EntityMessage.class))).then(
      (InvocationOnMock
                                                                                             invocation) -> {
      barrier.await();
      return new EntityResponse() {};
    });

    TestingResponse fin = mockResponse();
    TestingResponse helper = mockResponse();
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(deferInvoke, mockInvokePayload(), null, helper::complete, helper::failure));
    invokeOnTransactionHandler(()->{System.out.println(mgmtInvoke); managedEntity.addRequestMessage(mgmtInvoke, mockInvokePayload(), null, fin::complete, fin::failure);});
    barrier.await();
    barrier.await();
    fin.waitFor();
    
    verify(loopback, times(1)).completed(Matchers.any(), Matchers.any(FetchID.class), Matchers.any());
    verify(activeServerEntity, times(2)).invokeActive(eq(invokeContext), any(EntityMessage.class));
    verify(response, times(1)).complete(any());
    verify(fin, times(1)).complete(any());
  }
  
  @Test
  public void testExclusiveExecution() throws Exception {
    MessageCodec<EntityMessage, EntityResponse> codec = new MessageCodec<EntityMessage, EntityResponse>() {
      @Override
      public byte[] encodeResponse(EntityResponse response) {
        return new byte[0];
      }

      @Override
      public EntityMessage decodeMessage(byte[] payload) {
        return new EntityMessage() {
          @Override
          public String toString() {
            return new String(payload);
          }
        };
      }

      @Override
      public byte[] encodeMessage(EntityMessage message) throws MessageCodecException {
        return new byte[0];
      }

      @Override
      public EntityResponse decodeResponse(byte[] payload) throws MessageCodecException {
        return new EntityResponse() {
        };
      }
    };
    when(serverEntityService.getSyncMessageCodec()).thenReturn(new SyncMessageCodec<EntityMessage>(){
      @Override
      public byte[] encode(int concurrencyKey, EntityMessage message) throws MessageCodecException {
        throw new UnsupportedOperationException("not supported!");
      }

      @Override
      public EntityMessage decode(int concurrencyKey, byte[] payload) throws MessageCodecException {
        throw new UnsupportedOperationException("not supported!");
      }
    });
    ConcurrencyStrategy<EntityMessage> basic = new ConcurrencyStrategy<EntityMessage>() {
      @Override
      public int concurrencyKey(EntityMessage message) {
        String key = message.toString();
        return Integer.parseInt(key);
      }

      @Override
      public Set<Integer> getKeysForSynchronization() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }
    };
    when(this.serverEntityService.getConcurrencyStrategy(any(byte[].class))).thenReturn(basic);
    when(this.serverEntityService.getMessageCodec()).thenReturn(codec);
    TestingResponse response = mockResponse();
    invokeOnTransactionHandler(()->managedEntity = new ManagedEntityImpl(entityID, version, consumerID, loopback, serviceRegistry, clientEntityStateManager, eventCollector, requestMulti, serverEntityService, false, true));
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(mockCreateEntityRequest(), MessagePayload.emptyPayload(), null, response::complete, response::failure));
    response.waitFor();

    promote();

    Deque<Integer> queued = new LinkedList<>();
    Deque<Runnable> blockers = new LinkedList<>();
    
    Mockito.doAnswer((invoke)->{
        EntityRequest request = ((EntityRequest)invoke.getArguments()[0]);
        if (request.flush()) {
          blockers.add(request);
        } else if (request.getSchedulingKey() == null) {
//  drop it, these are noops
        } else {
          queued.add((Integer)request.getSchedulingKey());
        }
        return null;
    }).when(executionSink).addMultiThreaded(Matchers.any());

    Mockito.doAnswer(invocation->{
        TestingResponse resp = mockResponse();
        managedEntity.addRequestMessage(mockLocalFlushRequest(), MessagePayload.emptyPayload(), null, resp::complete, resp::failure);
        return mock(RequestProcessor.EntityRequest.class);
      }).when(loopback).completed(Mockito.any(EntityID.class), Mockito.any(FetchID.class), Mockito.any(ServerEntityAction.class));
    
    invokeOnTransactionHandler(()->{EntityMessage cstring = mock(EntityMessage.class);
      when(cstring.toString()).thenReturn(Integer.toString(ConcurrencyStrategy.MANAGEMENT_KEY));
      managedEntity.addRequestMessage(mockInvokeRequest(), MessagePayload.commonMessagePayloadBusy(Integer.toString(ConcurrencyStrategy.MANAGEMENT_KEY).getBytes(), cstring, true), null, null, null);
    });
    for (int x=1;x<=24;x++) {
      int key = (x == 12) ? ConcurrencyStrategy.MANAGEMENT_KEY : x;
      invokeOnTransactionHandler(()->{
        EntityMessage cstring = mock(EntityMessage.class);
        when(cstring.toString()).thenReturn(Integer.toString(key));
        managedEntity.addRequestMessage(mockInvokeRequest(), MessagePayload.commonMessagePayloadBusy(Integer.toString(key).getBytes(), cstring, true), null, null, null);
      });
    }
//  only thing in the queue should be the MGMT action    
    Assert.assertTrue(queued.isEmpty());
// run the mgmt action so defer is cleared    
    CountDownLatch latch1 = new CountDownLatch(1);
    invokeOnTransactionHandler(()->{blockers.pop().run();latch1.countDown();});
    latch1.await();
    
    Assert.assertTrue(queued.size() == 11);
    int index = 1;
    while (!queued.isEmpty()) {
      int check = index++;
      Assert.assertEquals(Integer.toString(check  ^ entityID.hashCode()), queued.pop().toString());
    }
    CountDownLatch latch2 = new CountDownLatch(1);
    invokeOnTransactionHandler(()->{blockers.pop().run();latch2.countDown();});
    latch2.await();
    index = 13; //  12 was skipped
     while (!queued.isEmpty()) {
      int check = index++;
      Assert.assertEquals(Integer.toString(check  ^ entityID.hashCode()), queued.pop().toString());
    }
    Assert.assertEquals(index, 25);
    verify(loopback, times(3)).completed(Mockito.any(EntityID.class), Mockito.any(FetchID.class), Mockito.any(ServerEntityAction.class));
  }

  @Test (expected = EntityUserException.class)
  public void testCodecException() throws Exception {
// this test is no longer relevant, decode is done in the hydrate stage or process/replicated transaction handler
    throw new EntityUserException("fake", new MessageCodecException("fake", new IOException()));
  }

  @Test
  public void testGetAndReleaseActive() throws Exception {
    // Create the entity.
    TestingResponse response1 = mockResponse();
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(mockCreateEntityRequest(), MessagePayload.emptyPayload(), null, response1::complete, response1::failure));
    response1.waitFor();
    verify(response1).complete(Mockito.any());
    
    // Get and release are only relevant on the active.
    promote();

    // Run the GET and verify that connected() call was received by the entity.
    ClientDescriptorImpl requester = new ClientDescriptorImpl(new com.tc.net.ClientID(0), new ClientInstanceID(1));
    ServerEntityRequest getRequest = mockGetRequest(requester);
    TestingResponse response2 = mockResponse();
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(getRequest,  mockInvokePayload(), null, response2::complete, response2::failure));
    response2.waitFor();
    verify(activeServerEntity).connected(eq(requester));
    verify(response2).complete(Mockito.any());
    
    // Run the RELEASE and verify that disconnected() call was received by the entity.
    ServerEntityRequest releaseRequest = mockReleaseRequest(requester);
    TestingResponse response3 = mockResponse();
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(releaseRequest, MessagePayload.emptyPayload(), null, response3::complete, response3::failure));
    response3.waitFor();
    verify(activeServerEntity).disconnected(eq(requester));
    verify(response3).complete(Mockito.any());
  }

  
  @Test
  public void testCreatePassiveGetAndReleaseActive() throws Exception {
    // Create the entity.
    Assert.assertFalse(managedEntity.isActive());
    TestingResponse response1 = mockResponse();
    ServerEntityRequest createRequest = mockCreateEntityRequest();
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(createRequest, MessagePayload.emptyPayload(), null, response1::complete, response1::failure));
    response1.waitFor();
    verify(response1).complete(Mockito.any());
    // Verify that it was created as a passive.
    verify(passiveServerEntity).createNew();
    verify(activeServerEntity, never()).createNew();
    
    // Now, switch modes to active.
    promote();
    verify(activeServerEntity).loadExisting();
    
    // Verify that we fail to create it again.
    ServerEntityRequest failedCreateRequest = mockCreateEntityRequest();
    TestingResponse response2 = mockResponse();
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(failedCreateRequest, MessagePayload.emptyPayload(), null, response2::complete, response2::failure));
    response2.waitFor();
    verify(response2).failure(any(EntityAlreadyExistsException.class));
    verify(response2, never()).complete(Mockito.any());
    
    // Verify that we can get and release, just like with any other active.
    ClientDescriptorImpl requester = new ClientDescriptorImpl(new com.tc.net.ClientID(0), new ClientInstanceID(1));
    ServerEntityRequest getRequest = mockGetRequest(requester);
    TestingResponse response3 = mockResponse();
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(getRequest, MessagePayload.emptyPayload(), null, response3::complete, response3::failure));
    response3.waitFor();
    verify(activeServerEntity).connected(eq(requester));
    verify(response3).complete(Mockito.any());
    
    // Run the RELEASE and verify that disconnected() call was received by the entity.
    ServerEntityRequest releaseRequest = mockReleaseRequest(requester);
    TestingResponse response4 = mockResponse();
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(releaseRequest, MessagePayload.emptyPayload(), null, response4::complete, response4::failure));
    response4.waitFor();
    verify(activeServerEntity).disconnected(eq(requester));
    verify(response4).complete(Mockito.any());
  }

  @Test
  public void testDestroy() throws Exception {
    TestingResponse response = mockResponse();
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(mockCreateEntityRequest(), MessagePayload.emptyPayload(), null, response::complete, response::failure));
    response.waitFor();
    promote();
    when(clientEntityStateManager.verifyNoReferences(Mockito.any())).thenReturn(Boolean.TRUE);
    TestingResponse response2 = mockResponse();
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(mockRequestForAction(ServerEntityAction.DESTROY_ENTITY), MessagePayload.emptyPayload(), null, response2::complete, response2::failure));
    response2.waitFor();
    verify(activeServerEntity).destroy();
  }

  @Test
  public void testCreateListener() throws Exception {
    // We will set this flag from inside the listener.
    final boolean[] indirectFlag = new boolean[1];
    managedEntity.setSuccessfulCreateListener(new ManagedEntity.CreateListener(){
      @Override
      public void entityCreationSucceeded(ManagedEntity sender) {
        indirectFlag[0] = true;
      }});
    TestingResponse response = mockResponse();
    invokeOnTransactionHandler(()->managedEntity.addRequestMessage(mockCreateEntityRequest(), MessagePayload.emptyPayload(), null, response::complete, response::failure));
    response.waitFor();
    
    Assert.assertTrue(indirectFlag[0]);
  }


  private MessagePayload mockCreatePayload(Serializable config) {
    try {
      return MessagePayload.commonMessagePayloadBusy(serialize(config), null, true);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }
  
  private MessagePayload mockLocationPayload(ExecutionStrategy.Location location) throws IOException {
    return MessagePayload.commonMessagePayloadBusy(new byte[0], new LocationInvoke() {
      @Override
      public ExecutionStrategy.Location getLocation() {
        return location;
      }
    }, true);
  }
  
  private MessagePayload mockReconfigurePayload(Serializable config) throws IOException {
    return MessagePayload.commonMessagePayloadBusy(serialize(config), null, true);
  }
  
  private TestingResponse mockResponse() {
    TestingResponse response = mock(TestingResponse.class);
    CountDownLatch latch = new CountDownLatch(1);
    doAnswer((invoke)->{System.out.println("complete " + latch);latch.countDown();return null;}).when(response).complete();
    doAnswer((invoke)->{System.out.println("complete " + latch);latch.countDown();return null;}).when(response).complete(Mockito.any(byte[].class));
    doAnswer((invoke)->{System.out.println("failure " + latch);latch.countDown();return null;}).when(response).failure(Mockito.any());
    doAnswer((invoke)->{System.out.println("waitFor " + latch);latch.await();flush();return null;}).when(response).waitFor();
    return response;
  }

  private ServerEntityRequest mockCreateEntityRequest() {
    ServerEntityRequest request = mockRequestForAction(ServerEntityAction.CREATE_ENTITY);
    when(request.getNodeID()).thenReturn(nodeID);
    when(request.getTransaction()).thenReturn(new TransactionID(1));
    when(request.getOldestTransactionOnClient()).thenReturn(new TransactionID(1));
    return request;
  }

  private ServerEntityRequest mockDestroyEntityRequest() {
    ServerEntityRequest request = mockRequestForAction(ServerEntityAction.DESTROY_ENTITY);
    when(request.getNodeID()).thenReturn(nodeID);
    when(request.getTransaction()).thenReturn(new TransactionID(1));
    when(request.getOldestTransactionOnClient()).thenReturn(new TransactionID(1));
    return request;
  }
  
  private ServerEntityRequest mockReconfigureEntityRequest() {
    ServerEntityRequest request = mockRequestForAction(ServerEntityAction.RECONFIGURE_ENTITY);
    when(request.getNodeID()).thenReturn(nodeID);
    when(request.getTransaction()).thenReturn(new TransactionID(1));
    when(request.getOldestTransactionOnClient()).thenReturn(new TransactionID(1));
    return request;
  }
  
  private ServerEntityRequest mockInvokeRequest() {
    ServerEntityRequest request = mockRequestForAction(ServerEntityAction.INVOKE_ACTION);
    when(request.getNodeID()).thenReturn(nodeID);
    when(request.getTransaction()).thenReturn(new TransactionID(1));
    when(request.getOldestTransactionOnClient()).thenReturn(new TransactionID(1));
    return request;
  }

  private ServerEntityRequest mockExecutionInvokeRequest(ExecutionStrategy.Location loc) {
    ServerEntityRequest request = mock(ServerEntityRequest.class);
    when(request.getClientInstance()).thenReturn(clientInstanceID);
    when(request.getAction()).thenReturn(ServerEntityAction.INVOKE_ACTION);
    when(request.getNodeID()).thenReturn(nodeID);
    when(request.getTransaction()).thenReturn(new TransactionID(1));
    when(request.getOldestTransactionOnClient()).thenReturn(new TransactionID(1));
    return request;
  }
  
  private ServerEntityRequest mockGetRequest(ClientDescriptorImpl requester) {
    ServerEntityRequest request = mockRequestForAction(ServerEntityAction.FETCH_ENTITY);
    when(request.getNodeID()).thenReturn(requester.getNodeID());
    when(request.getTransaction()).thenReturn(new TransactionID(1));
    when(request.getOldestTransactionOnClient()).thenReturn(new TransactionID(1));
    return request;
  }

  private ServerEntityRequest mockReleaseRequest(ClientDescriptorImpl requester) {
    ServerEntityRequest request = mockRequestForAction(ServerEntityAction.RELEASE_ENTITY);
    when(request.getNodeID()).thenReturn(requester.getNodeID());
    when(request.getTransaction()).thenReturn(new TransactionID(1));
    when(request.getOldestTransactionOnClient()).thenReturn(new TransactionID(1));
    return request;
  }
    
  private ServerEntityRequest mockLocalFlushRequest() {
    ServerEntityRequest request = mock(ServerEntityRequest.class);
    when(request.getClientInstance()).thenReturn(ClientInstanceID.NULL_ID);
    when(request.getAction()).thenReturn(ServerEntityAction.LOCAL_FLUSH);
    when(request.getTransaction()).thenReturn(new TransactionID(1));
    when(request.getOldestTransactionOnClient()).thenReturn(new TransactionID(1));
    return request;
  }

  private ServerEntityRequest mockRequestForAction(ServerEntityAction action) {
    ServerEntityRequest request = mock(ServerEntityRequest.class);
    when(request.getClientInstance()).thenReturn(clientInstanceID);
    when(request.getAction()).thenReturn(action);
    when(request.getNodeID()).thenReturn(nodeID);
    when(request.getTransaction()).thenReturn(new TransactionID(1));
    when(request.getOldestTransactionOnClient()).thenReturn(new TransactionID(1));
    return request;
  }

  private byte[] serialize(Serializable serializable) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(serializable);
    }
    return baos.toByteArray();
  }
  
  public interface TestingResponse extends ServerEntityResponse {

    void waitFor();
  
  }
  
  private void flush() throws Exception {
    pth.submit(()->{}).get();
    exec.submit(()->{}).get();
  }
  
  private void promote() throws Exception {
    flush();
    managedEntity.promoteEntity();
    invokeOnTransactionHandler(()->Thread.currentThread().setName(ServerConfigurationContext.VOLTRON_MESSAGE_STAGE));
  }
  
  public interface LocationInvoke extends EntityMessage {
    ExecutionStrategy.Location getLocation();
  }
}
