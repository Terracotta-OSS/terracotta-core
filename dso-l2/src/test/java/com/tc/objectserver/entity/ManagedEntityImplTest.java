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
import com.tc.entity.VoltronEntityMessage;
import com.tc.net.ClientID;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.TestEntity;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.ServerEntityService;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.exception.EntityAlreadyExistsException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityUserException;

import com.tc.net.NodeID;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.objectserver.core.api.ITopologyEventCollector;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.services.InternalServiceRegistry;
import com.tc.util.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Set;
import org.mockito.Matchers;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;


public class ManagedEntityImplTest {
  private EntityID entityID;
  private ClientInstanceID clientInstanceID;
  private long version;
  private ManagedEntityImpl managedEntity;
  private Sink<VoltronEntityMessage> loopback;
  private InternalServiceRegistry serviceRegistry;
  private ServerEntityService<? extends ActiveServerEntity<EntityMessage, EntityResponse>, ? extends PassiveServerEntity<EntityMessage, EntityResponse>> serverEntityService;
  private ActiveServerEntity<EntityMessage, EntityResponse> activeServerEntity;
  private PassiveServerEntity<EntityMessage, EntityResponse> passiveServerEntity;
  private RequestProcessor requestMulti;
  private ClientEntityStateManager clientEntityStateManager;
  private ITopologyEventCollector eventCollector;
  private NodeID nodeID;
  private ClientDescriptor clientDescriptor;
  private EntityDescriptor entityDescriptor;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    nodeID = mock(NodeID.class);
    entityID = new EntityID(TestEntity.class.getName(), "foo");
    clientInstanceID = new ClientInstanceID(1);
    version = 1;
    entityDescriptor = new EntityDescriptor(entityID, clientInstanceID, version);
    serviceRegistry = mock(InternalServiceRegistry.class);
    
    loopback = mock(Sink.class);

    requestMulti = mock(RequestProcessor.class);
    activeServerEntity = mock(ActiveServerEntity.class);
    passiveServerEntity = mock(PassiveServerEntity.class);
    serverEntityService = getServerEntityService(this.activeServerEntity, this.passiveServerEntity);
    clientEntityStateManager = mock(ClientEntityStateManager.class);
    eventCollector = mock(ITopologyEventCollector.class);

    // We will start this in a passive state, as the general test case.
    boolean isInActiveState = false;
    managedEntity = new ManagedEntityImpl(entityID, version, loopback, serviceRegistry, clientEntityStateManager, eventCollector, requestMulti, (ServerEntityService<? extends ActiveServerEntity<EntityMessage, EntityResponse>, ? extends PassiveServerEntity<EntityMessage, EntityResponse>>)serverEntityService, isInActiveState);
    clientDescriptor = new ClientDescriptorImpl(nodeID, entityDescriptor);
    Mockito.doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((Runnable)invocation.getArguments()[3]).run();
        return null;
      }
    }).when(requestMulti).scheduleRequest(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt());
    Thread.currentThread().setName(ServerConfigurationContext.VOLTRON_MESSAGE_STAGE);
  }

  @SuppressWarnings("unchecked")
  private ServerEntityService<? extends ActiveServerEntity<EntityMessage, EntityResponse>, ? extends PassiveServerEntity<EntityMessage, EntityResponse>> getServerEntityService(ActiveServerEntity<EntityMessage, EntityResponse> activeServerEntity, PassiveServerEntity<EntityMessage, EntityResponse> passiveServerEntity) {
    ServerEntityService<? extends ActiveServerEntity<EntityMessage, EntityResponse>, ? extends PassiveServerEntity<EntityMessage, EntityResponse>> entityService = mock(ServerEntityService.class);
    doReturn(activeServerEntity).when(entityService).createActiveEntity(any(ServiceRegistry.class), any(byte[].class));
    doReturn(passiveServerEntity).when(entityService).createPassiveEntity(any(ServiceRegistry.class), any(byte[].class));
    return entityService;
  }

  @Test
  public void testCreateActive() throws Exception {
    managedEntity.addLifecycleRequest(mockPromoteToActiveRequest(), null);
    String config = "foo";
    ServerEntityRequest request = mockCreateEntityRequest();
    managedEntity.addLifecycleRequest(request, mockCreatePayload(config));
    verify(serverEntityService).createActiveEntity(serviceRegistry, serialize(config));
    verify(request).complete();
  }

  @Test
  public void testCreatePassive() throws Exception {
    String config = "foo";
    ServerEntityRequest request = mockCreateEntityRequest();
    managedEntity.addLifecycleRequest(request, mockCreatePayload(config));
    verify(serverEntityService).createPassiveEntity(serviceRegistry, serialize(config));
    verify(request).complete();
  }

  @Test
  public void testDoubleCreate() throws Exception {
    ServerEntityRequest request = mockCreateEntityRequest();
    managedEntity.addLifecycleRequest(mockCreateEntityRequest(), mockCreatePayload("foo"));
    managedEntity.addLifecycleRequest(request, mockCreatePayload("bar"));
    verify(request).failure(any(EntityAlreadyExistsException.class));
    verify(request, never()).complete();
  }

  @Test
  public void testGetEntityMissing() throws Exception {
    // Get is only defined on active.
    managedEntity.addLifecycleRequest(mockPromoteToActiveRequest(),  null);
    
    com.tc.net.ClientID requester = new com.tc.net.ClientID(0);
    ServerEntityRequest request = mockGetRequest(requester);
    managedEntity.addLifecycleRequest(request, null);

    verify(clientEntityStateManager, never()).addReference(requester, new EntityDescriptor(entityID, clientInstanceID, version));
    verify(request).complete();
  }

  @Test
  public void testGetEntityExists() throws Exception {
    byte[] config = new byte[0];
    when(activeServerEntity.getConfig()).thenReturn(config);
    
    managedEntity.addLifecycleRequest(mockCreateEntityRequest(),  mockCreatePayload("foo"));
    managedEntity.addLifecycleRequest(mockPromoteToActiveRequest(), null);
    
    com.tc.net.ClientID requester = new com.tc.net.ClientID(0);
    ServerEntityRequest request = mockGetRequest(requester);
    managedEntity.addLifecycleRequest(request, null);

    verify(clientEntityStateManager).addReference(requester, new EntityDescriptor(entityID, clientInstanceID, version));
    verify(request).complete(config);
  }

  @Test
  public void testPerformActionMissingEntity() throws Exception {
    ServerEntityRequest request = mockInvokeRequest();
    managedEntity.addInvokeRequest(request, new byte[0], ConcurrencyStrategy.MANAGEMENT_KEY);
    verify(request).failure(any(EntityNotFoundException.class));
  }

  @Test
  public void testPerformAction() throws Exception {
    managedEntity.addLifecycleRequest(mockCreateEntityRequest(), null);
    managedEntity.addLifecycleRequest(mockPromoteToActiveRequest(), null);
    
    byte[] payload = { 0 };
    byte[] returnValue = { 1 };
    when(activeServerEntity.getMessageCodec()).thenReturn(new MessageCodec<EntityMessage, EntityResponse>(){
      @Override
      public byte[] serialize(EntityResponse response) {
        return returnValue;
      }
      
      @Override
      public EntityMessage deserialize(byte[] payload) {
        return new EntityMessage() {};
      }
      @Override
      public EntityMessage deserializeForSync(int concurrencyKey, byte[] payload) {
        Assert.fail("Synchronization not used in this test");
        return null;
      }
    });
    when(activeServerEntity.invoke(eq(clientDescriptor), any(EntityMessage.class))).thenReturn(new EntityResponse() {});
    ServerEntityRequest invokeRequest = mockInvokeRequest();
    managedEntity.addInvokeRequest(invokeRequest, payload, ConcurrencyStrategy.MANAGEMENT_KEY);
    
    verify(activeServerEntity).invoke(eq(clientDescriptor), any(EntityMessage.class));
    verify(invokeRequest).complete(returnValue);
    verify(loopback).addSingleThreaded(Matchers.any(NoopEntityMessage.class));
  }
  
  @Test
  public void testExclusiveExecution() throws Exception {
    MessageCodec codec = new MessageCodec() {
      @Override
      public EntityMessage deserialize(byte[] payload) throws MessageCodecException {
        return new EntityMessage() {
          @Override
          public String toString() {
            return new String(payload);
          }
        };
      }

      @Override
      public EntityMessage deserializeForSync(int concurrencyKey, byte[] payload) throws MessageCodecException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }

      @Override
      public byte[] serialize(EntityResponse response) throws MessageCodecException {
        return new byte[0];
      }
    };
    ConcurrencyStrategy basic = new ConcurrencyStrategy() {
      @Override
      public int concurrencyKey(EntityMessage message) {
        String key = message.toString();
        return Integer.parseInt(key);
      }

      @Override
      public Set getKeysForSynchronization() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }
    };
    when(activeServerEntity.getConcurrencyStrategy()).thenReturn(basic);
    when(activeServerEntity.getMessageCodec()).thenReturn(codec);
    managedEntity.addLifecycleRequest(mockPromoteToActiveRequest(), new byte[0]);
    managedEntity.addLifecycleRequest(mockCreateEntityRequest(), new byte[0]);

    Deque<Integer> queued = new LinkedList<>();
    Deque<Runnable> blockers = new LinkedList<>();
    Mockito.doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        int key = (Integer)invocation.getArguments()[4];
        if (key == ConcurrencyStrategy.MANAGEMENT_KEY) {
          blockers.add((Runnable)invocation.getArguments()[3]);
        } else {
          queued.add(key);
        }
        return null;
      }
    }).when(requestMulti).scheduleRequest(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt());
    Mockito.doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        managedEntity.addInvokeRequest(mockNoopRequest(), null, ConcurrencyStrategy.UNIVERSAL_KEY);
        return null;
      }
    }).when(loopback).addSingleThreaded(Matchers.any());

    managedEntity.addInvokeRequest(mockInvokeRequest(), Integer.toString(ConcurrencyStrategy.MANAGEMENT_KEY).getBytes(), ConcurrencyStrategy.MANAGEMENT_KEY);
    for (int x=1;x<=24;x++) {
      int key = (x == 12) ? ConcurrencyStrategy.MANAGEMENT_KEY : x;
      managedEntity.addInvokeRequest(mockInvokeRequest(), Integer.toString(key).getBytes(), ConcurrencyStrategy.MANAGEMENT_KEY);
    }
//  only thing in the queue should be the MGMT action    
    Assert.assertTrue(queued.isEmpty());
    Runnable r = blockers.pop();
    Assert.assertNotNull(r);
// run the mgmt action so defer is cleared    
    r.run();
    Assert.assertTrue(queued.size() == 11);
    int index = 1;
    while (!queued.isEmpty()) {
      int check = index++;
      Assert.assertEquals(Integer.toString(check), queued.pop().toString());
    }
    r = blockers.pop();
    Assert.assertNotNull(r);
    r.run();
    index = 13; //  12 was skipped
     while (!queued.isEmpty()) {
      int check = index++;
      Assert.assertEquals(Integer.toString(check), queued.pop().toString());
    }
    Assert.assertEquals(index, 25);
    verify(loopback, times(2)).addSingleThreaded(Matchers.any(NoopEntityMessage.class));
    
  }

  @Test
  public void testCodecException() throws Exception {
    managedEntity.addLifecycleRequest(mockCreateEntityRequest(), null);
    managedEntity.addLifecycleRequest(mockPromoteToActiveRequest(), null);
    
    byte[] payload = { 0 };
    when(activeServerEntity.getMessageCodec()).thenReturn(new MessageCodec<EntityMessage, EntityResponse>(){
      @Override
      public byte[] serialize(EntityResponse response) {
        // We should never reach this - should fail before the invoke.
        Assert.fail();
        return null;
      }
      @Override
      public EntityMessage deserialize(byte[] payload) throws MessageCodecException {
        // We want to simulate a failure.
        throw new MessageCodecException("failure", null);
      }
      @Override
      public EntityMessage deserializeForSync(int concurrencyKey, byte[] payload) {
        Assert.fail("Synchronization not used in this test");
        return null;
      }
    });
    ServerEntityRequest invokeRequest = mockInvokeRequest();
    managedEntity.addInvokeRequest(invokeRequest, payload, ConcurrencyStrategy.MANAGEMENT_KEY);
    
    verify(activeServerEntity, never()).invoke(any(ClientDescriptor.class), any(EntityMessage.class));
    verify(invokeRequest, never()).complete(any(byte[].class));
    verify(invokeRequest).failure(any(EntityUserException.class));
  }

  @Test
  public void testGetAndRelease() throws Exception {
    // Get and release are only relevant on the active.
    managedEntity.addLifecycleRequest(mockPromoteToActiveRequest(), null);
    
    // Create the entity.
    ServerEntityRequest createRequest = mockCreateEntityRequest();
    managedEntity.addLifecycleRequest(createRequest,  null);
    verify(createRequest).complete();
    
    // Run the GET and verify that connected() call was received by the entity.
    com.tc.net.ClientID requester = new com.tc.net.ClientID(0);
    ServerEntityRequest getRequest = mockGetRequest(requester);
    managedEntity.addLifecycleRequest(getRequest,  null);
    verify(activeServerEntity).connected(clientDescriptor);
    verify(getRequest).complete(null);
    
    // Run the RELEASE and verify that disconnected() call was received by the entity.
    ServerEntityRequest releaseRequest = mockReleaseRequest(requester);
    managedEntity.addLifecycleRequest(releaseRequest, null);
    verify(activeServerEntity).disconnected(clientDescriptor);
    verify(releaseRequest).complete();
  }

  
  @Test
  public void testCreatePassiveGetAndReleaseActive() throws Exception {
    // Create the entity.
    ServerEntityRequest createRequest = mockCreateEntityRequest();
    managedEntity.addLifecycleRequest(createRequest, null);
    verify(createRequest).complete();
    
    // Verify that it was created as a passive.
    verify(passiveServerEntity).createNew();
    verify(activeServerEntity, never()).createNew();
    
    // Now, switch modes to active.
    managedEntity.addLifecycleRequest(mockPromoteToActiveRequest(), null);
    verify(activeServerEntity).loadExisting();
    
    // Verify that we fail to create it again.
    ServerEntityRequest failedCreateRequest = mockCreateEntityRequest();
    managedEntity.addLifecycleRequest(failedCreateRequest, null);
    verify(failedCreateRequest).failure(any(EntityAlreadyExistsException.class));
    verify(failedCreateRequest, never()).complete();
    
    // Verify that we can get and release, just like with any other active.
    com.tc.net.ClientID requester = new com.tc.net.ClientID(0);
    ServerEntityRequest getRequest = mockGetRequest(requester);
    managedEntity.addLifecycleRequest(getRequest, null);
    verify(activeServerEntity).connected(clientDescriptor);
    verify(getRequest).complete(null);
    
    // Run the RELEASE and verify that disconnected() call was received by the entity.
    ServerEntityRequest releaseRequest = mockReleaseRequest(requester);
    managedEntity.addLifecycleRequest(releaseRequest, null);
    verify(activeServerEntity).disconnected(clientDescriptor);
    verify(releaseRequest).complete();
  }

  @Test
  public void testDestroy() throws Exception {
    managedEntity.addLifecycleRequest(mockPromoteToActiveRequest(), null);
    managedEntity.addLifecycleRequest(mockCreateEntityRequest(), null);
    
    managedEntity.addLifecycleRequest(mockRequestForAction(ServerEntityAction.DESTROY_ENTITY), null);

    verify(activeServerEntity).destroy();
  }
  
  private byte[] mockCreatePayload(Serializable config) throws IOException {
    return serialize(config);
  }

  private ServerEntityRequest mockCreateEntityRequest() throws IOException {
    ServerEntityRequest request = mockRequestForAction(ServerEntityAction.CREATE_ENTITY);
    return request;
  }

  private ServerEntityRequest mockInvokeRequest() {
    ServerEntityRequest request = mockRequestForAction(ServerEntityAction.INVOKE_ACTION);
    return request;
  }
  
  private ServerEntityRequest mockGetRequest(com.tc.net.ClientID requester) {
    ServerEntityRequest request = mockRequestForAction(ServerEntityAction.FETCH_ENTITY);
    when(request.getNodeID()).thenReturn(requester);
    return request;
  }

  private ServerEntityRequest mockReleaseRequest(com.tc.net.ClientID requester) {
    ServerEntityRequest request = mockRequestForAction(ServerEntityAction.RELEASE_ENTITY);
    when(request.getNodeID()).thenReturn(requester);
    return request;
  }
    
  private ServerEntityRequest mockNoopRequest() {
    ServerEntityRequest request = mock(ServerEntityRequest.class);
    when(request.getSourceDescriptor()).thenReturn(new ClientDescriptorImpl(ClientID.NULL_ID, entityDescriptor));
    when(request.getAction()).thenReturn(ServerEntityAction.NOOP);
    return request;
  }
  
  private ServerEntityRequest mockPromoteToActiveRequest() {
    return mockRequestForAction(ServerEntityAction.PROMOTE_ENTITY_TO_ACTIVE);
  }

  private ServerEntityRequest mockRequestForAction(ServerEntityAction action) {
    ServerEntityRequest request = mock(ServerEntityRequest.class);
    when(request.getSourceDescriptor()).thenReturn(clientDescriptor);
    when(request.getAction()).thenReturn(action);
    return request;
  }

  private byte[] serialize(Serializable serializable) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(serializable);
    }
    return baos.toByteArray();
  }
}
