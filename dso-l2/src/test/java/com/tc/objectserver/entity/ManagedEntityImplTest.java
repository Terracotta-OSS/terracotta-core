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

import org.junit.Before;
import org.junit.Test;
import org.terracotta.TestEntity;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.ServerEntityService;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.exception.EntityAlreadyExistsException;
import org.terracotta.exception.EntityNotFoundException;

import com.tc.net.NodeID;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.objectserver.entity.ManagedEntityImpl.ManagedEntityRequest;
import com.tc.util.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class ManagedEntityImplTest {
  private EntityID entityID;
  private ClientInstanceID clientInstanceID;
  private long version;
  private ManagedEntityImpl managedEntity;
  private ServiceRegistry serviceRegistry;
  private ServerEntityService<? extends ActiveServerEntity, ? extends PassiveServerEntity> serverEntityService;
  private ActiveServerEntity activeServerEntity;
  private PassiveServerEntity passiveServerEntity;
  private RequestProcessor requestMulti;
  private ClientEntityStateManager clientEntityStateManager;
  private NodeID nodeID;
  private ClientDescriptor clientDescriptor;
  private EntityDescriptor entityDescriptor;

  @Before
  public void setUp() throws Exception {
    nodeID = mock(NodeID.class);
    entityID = new EntityID(TestEntity.class.getName(), "foo");
    clientInstanceID = new ClientInstanceID(1);
    version = 1;
    entityDescriptor = new EntityDescriptor(entityID, clientInstanceID, version);
    serviceRegistry = mock(ServiceRegistry.class);

    requestMulti = mock(RequestProcessor.class);
    activeServerEntity = mock(ActiveServerEntity.class);
    passiveServerEntity = mock(PassiveServerEntity.class);
    serverEntityService = getServerEntityService(this.activeServerEntity, this.passiveServerEntity);
    clientEntityStateManager = mock(ClientEntityStateManager.class);

    // We will start this in a passive state, as the general test case.
    boolean isInActiveState = false;
    managedEntity = new ManagedEntityImpl(entityID, version, serviceRegistry, clientEntityStateManager, requestMulti, (ServerEntityService<? extends ActiveServerEntity<EntityMessage, EntityResponse>, ? extends PassiveServerEntity<EntityMessage, EntityResponse>>) serverEntityService, isInActiveState);
    clientDescriptor = new ClientDescriptorImpl(nodeID, entityDescriptor);
  }

  @SuppressWarnings("unchecked")
  private ServerEntityService<? extends ActiveServerEntity, ? extends PassiveServerEntity> getServerEntityService(ActiveServerEntity activeServerEntity, PassiveServerEntity passiveServerEntity) {
    ServerEntityService<? extends ActiveServerEntity, ? extends PassiveServerEntity> entityService = mock(ServerEntityService.class);
    doReturn(activeServerEntity).when(entityService).createActiveEntity(any(ServiceRegistry.class), any(byte[].class));
    doReturn(passiveServerEntity).when(entityService).createPassiveEntity(any(ServiceRegistry.class), any(byte[].class));
    return entityService;
  }

  @Test
  public void testCreateActive() throws Exception {
    invokeNoCodec(mockPromoteToActiveRequest(), ConcurrencyStrategy.UNIVERSAL_KEY, null);
    String config = "foo";
    ServerEntityRequest request = mockCreateEntityRequest(config);
    invokeNoCodec(request, ConcurrencyStrategy.UNIVERSAL_KEY, null);
    verify(serverEntityService).createActiveEntity(serviceRegistry, serialize(config));
    verify(request).complete();
  }

  @Test
  public void testCreatePassive() throws Exception {
    String config = "foo";
    ServerEntityRequest request = mockCreateEntityRequest(config);
    invokeNoCodec(request, ConcurrencyStrategy.UNIVERSAL_KEY, null);
    verify(serverEntityService).createPassiveEntity(serviceRegistry, serialize(config));
    verify(request).complete();
  }

  @Test
  public void testDoubleCreate() throws Exception {
    ServerEntityRequest request = mockCreateEntityRequest("bar");
    managedEntity.addRequest(request);
    invokeNoCodec(mockCreateEntityRequest("foo"), ConcurrencyStrategy.UNIVERSAL_KEY, null);
    invokeNoCodec(request, ConcurrencyStrategy.UNIVERSAL_KEY, null);
    verify(request).failure(any(EntityAlreadyExistsException.class));
    verify(request, never()).complete();
  }

  @Test
  public void testGetEntityMissing() throws Exception {
    // Get is only defined on active.
    invokeNoCodec(mockPromoteToActiveRequest(), ConcurrencyStrategy.UNIVERSAL_KEY, null);
    
    com.tc.net.ClientID requester = new com.tc.net.ClientID(0);
    ServerEntityRequest request = mockGetRequest(requester);
    invokeNoCodec(request, ConcurrencyStrategy.UNIVERSAL_KEY, null);

    verify(clientEntityStateManager, never()).addReference(requester, new EntityDescriptor(entityID, clientInstanceID, version));
    verify(request).complete();
  }

  @Test
  public void testGetEntityExists() throws Exception {
    byte[] config = new byte[0];
    when(activeServerEntity.getConfig()).thenReturn(config);
    
    invokeNoCodec(mockCreateEntityRequest("foo"), ConcurrencyStrategy.UNIVERSAL_KEY, null);
    invokeNoCodec(mockPromoteToActiveRequest(), ConcurrencyStrategy.UNIVERSAL_KEY, null);
    
    com.tc.net.ClientID requester = new com.tc.net.ClientID(0);
    ServerEntityRequest request = mockGetRequest(requester);
    invokeNoCodec(request, ConcurrencyStrategy.UNIVERSAL_KEY, null);

    verify(clientEntityStateManager).addReference(requester, new EntityDescriptor(entityID, clientInstanceID, version));
    verify(request).complete(config);
  }

  @Test
  public void testPerformActionMissingEntity() throws Exception {
    ServerEntityRequest request = mockInvokeRequest(new byte[0]);
    invokeNoCodec(request, ConcurrencyStrategy.UNIVERSAL_KEY, null);
    verify(request).failure(any(EntityNotFoundException.class));
  }

  @Test
  public void testPerformAction() throws Exception {
    invokeNoCodec(mockCreateEntityRequest(null), ConcurrencyStrategy.UNIVERSAL_KEY, null);
    invokeNoCodec(mockPromoteToActiveRequest(), ConcurrencyStrategy.UNIVERSAL_KEY, null);
    
    byte[] payload = { 0 };
    byte[] returnValue = { 1 };
    when(activeServerEntity.getMessageCodec()).thenReturn(new MessageCodec<EntityMessage, EntityResponse>(){
      @Override
      public EntityMessage deserialize(byte[] payload) {
        return new EntityMessage() {};
      }
      @Override
      public EntityMessage deserializeForSync(int concurrencyKey, byte[] payload) {
        Assert.fail("Synchronization not used in this test");
        return null;
      }});
    when(activeServerEntity.invoke(eq(clientDescriptor), any(EntityMessage.class))).thenReturn(returnValue);
    ServerEntityRequest invokeRequest = mockInvokeRequest(payload);
    invokeNoCodec(invokeRequest, ConcurrencyStrategy.UNIVERSAL_KEY, null);
    
    verify(activeServerEntity).invoke(eq(clientDescriptor), any(EntityMessage.class));
    verify(invokeRequest).complete(returnValue);
  }
  
  @Test
  public void testGetAndRelease() throws Exception {
    // Get and release are only relevant on the active.
    invokeNoCodec(mockPromoteToActiveRequest(), ConcurrencyStrategy.UNIVERSAL_KEY, null);
    
    // Create the entity.
    ServerEntityRequest createRequest = mockCreateEntityRequest(null);
    invokeNoCodec(createRequest, ConcurrencyStrategy.UNIVERSAL_KEY, null);
    verify(createRequest).complete();
    
    // Run the GET and verify that connected() call was received by the entity.
    com.tc.net.ClientID requester = new com.tc.net.ClientID(0);
    ServerEntityRequest getRequest = mockGetRequest(requester);
    invokeNoCodec(getRequest, ConcurrencyStrategy.UNIVERSAL_KEY, null);
    verify(activeServerEntity).connected(clientDescriptor);
    verify(getRequest).complete(null);
    
    // Run the RELEASE and verify that disconnected() call was received by the entity.
    ServerEntityRequest releaseRequest = mockReleaseRequest(requester);
    invokeNoCodec(releaseRequest, ConcurrencyStrategy.UNIVERSAL_KEY, null);
    verify(activeServerEntity).disconnected(clientDescriptor);
    verify(releaseRequest).complete();
  }

  
  @Test
  public void testCreatePassiveGetAndReleaseActive() throws Exception {
    // Create the entity.
    ServerEntityRequest createRequest = mockCreateEntityRequest(null);
    invokeNoCodec(createRequest, ConcurrencyStrategy.UNIVERSAL_KEY, null);
    verify(createRequest).complete();
    
    // Verify that it was created as a passive.
    verify(passiveServerEntity).createNew();
    verify(activeServerEntity, never()).createNew();
    
    // Now, switch modes to active.
    invokeNoCodec(mockPromoteToActiveRequest(), ConcurrencyStrategy.UNIVERSAL_KEY, null);
    verify(activeServerEntity).loadExisting();
    
    // Verify that we fail to create it again.
    ServerEntityRequest failedCreateRequest = mockCreateEntityRequest(null);
    invokeNoCodec(failedCreateRequest, ConcurrencyStrategy.UNIVERSAL_KEY, null);
    verify(failedCreateRequest).failure(any(EntityAlreadyExistsException.class));
    verify(failedCreateRequest, never()).complete();
    
    // Verify that we can get and release, just like with any other active.
    com.tc.net.ClientID requester = new com.tc.net.ClientID(0);
    ServerEntityRequest getRequest = mockGetRequest(requester);
    invokeNoCodec(getRequest, ConcurrencyStrategy.UNIVERSAL_KEY, null);
    verify(activeServerEntity).connected(clientDescriptor);
    verify(getRequest).complete(null);
    
    // Run the RELEASE and verify that disconnected() call was received by the entity.
    ServerEntityRequest releaseRequest = mockReleaseRequest(requester);
    invokeNoCodec(releaseRequest, ConcurrencyStrategy.UNIVERSAL_KEY, null);
    verify(activeServerEntity).disconnected(clientDescriptor);
    verify(releaseRequest).complete();
  }

  @Test
  public void testDestroy() throws Exception {
    invokeNoCodec(mockPromoteToActiveRequest(), ConcurrencyStrategy.UNIVERSAL_KEY, null);
    invokeNoCodec(mockCreateEntityRequest(null), ConcurrencyStrategy.UNIVERSAL_KEY, null);
    
    invokeNoCodec(mockRequestForAction(ServerEntityAction.DESTROY_ENTITY), ConcurrencyStrategy.UNIVERSAL_KEY, null);

    verify(activeServerEntity).destroy();
  }

  private ServerEntityRequest mockCreateEntityRequest(Serializable config) throws IOException {
    ServerEntityRequest request = mockRequestForAction(ServerEntityAction.CREATE_ENTITY);
    when(request.getPayload()).thenReturn(serialize(config));
    return request;
  }

  private ServerEntityRequest mockInvokeRequest(byte[] bytes) {
    ServerEntityRequest request = mockRequestForAction(ServerEntityAction.INVOKE_ACTION);
    when(request.getPayload()).thenReturn(bytes);
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

  private void invokeNoCodec(ServerEntityRequest request, int concurrencyKey, EntityMessage message) {
    ManagedEntityRequest wrapped = new ManagedEntityRequest(request, activeServerEntity.getMessageCodec());
    managedEntity.invoke(wrapped, concurrencyKey, message);
  }
}
