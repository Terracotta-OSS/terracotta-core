package com.tc.objectserver.entity;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.terracotta.TestEntity;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.ServerEntityService;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.test.categories.CheckShorts;

import com.tc.net.NodeID;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@Category(CheckShorts.class)
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
    managedEntity = new ManagedEntityImpl(entityID, serviceRegistry, clientEntityStateManager, requestMulti, serverEntityService, isInActiveState);
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
    managedEntity.invoke(mockPromoteToActiveRequest());
    String config = "foo";
    ServerEntityRequest request = mockCreateEntityRequest(config);
    managedEntity.invoke(request);
    verify(serverEntityService).createActiveEntity(serviceRegistry, serialize(config));
    verify(request).complete();
  }

  @Test
  public void testCreatePassive() throws Exception {
    String config = "foo";
    ServerEntityRequest request = mockCreateEntityRequest(config);
    managedEntity.invoke(request);
    verify(serverEntityService).createPassiveEntity(serviceRegistry, serialize(config));
    verify(request).complete();
  }

  @Test
  public void testDoubleCreate() throws Exception {
    ServerEntityRequest request = mockCreateEntityRequest("bar");
    managedEntity.addRequest(request);
    managedEntity.invoke(mockCreateEntityRequest("foo"));
    managedEntity.invoke(request);
    verify(request).failure(any(IllegalStateException.class));
    verify(request, never()).complete();
  }

  @Test
  public void testGetEntityMissing() throws Exception {
    // Get is only defined on active.
    managedEntity.invoke(mockPromoteToActiveRequest());
    
    com.tc.net.ClientID requester = new com.tc.net.ClientID(0);
    ServerEntityRequest request = mockGetRequest(requester);
    managedEntity.invoke(request);

    verify(clientEntityStateManager, never()).addReference(requester, new EntityDescriptor(entityID, clientInstanceID, version));
    verify(request).complete();
  }

  @Test
  public void testGetEntityExists() throws Exception {
    byte[] config = new byte[0];
    when(activeServerEntity.getConfig()).thenReturn(config);
    
    managedEntity.invoke(mockCreateEntityRequest("foo"));
    managedEntity.invoke(mockPromoteToActiveRequest());
    
    com.tc.net.ClientID requester = new com.tc.net.ClientID(0);
    ServerEntityRequest request = mockGetRequest(requester);
    managedEntity.invoke(request);

    verify(clientEntityStateManager).addReference(requester, new EntityDescriptor(entityID, clientInstanceID, version));
    verify(request).complete(config);
  }

  @Test
  public void testPerformActionMissingEntity() throws Exception {
    ServerEntityRequest request = mockInvokeRequest(new byte[0]);
    managedEntity.invoke(request);
    verify(request).failure(any(IllegalStateException.class));
  }

  @Test
  public void testPerformAction() throws Exception {
    managedEntity.invoke(mockCreateEntityRequest(null));
    managedEntity.invoke(mockPromoteToActiveRequest());
    
    byte[] payload = { 0 };
    byte[] returnValue = { 1 };
    when(activeServerEntity.invoke(clientDescriptor, payload)).thenReturn(returnValue);
    ServerEntityRequest invokeRequest = mockInvokeRequest(payload);
    managedEntity.invoke(invokeRequest);
    
    verify(activeServerEntity).invoke(clientDescriptor, payload);
    verify(invokeRequest).complete(returnValue);
  }
  
  @Test
  public void testGetAndRelease() throws Exception {
    // Get and release are only relevant on the active.
    managedEntity.invoke(mockPromoteToActiveRequest());
    
    // Create the entity.
    ServerEntityRequest createRequest = mockCreateEntityRequest(null);
    managedEntity.invoke(createRequest);
    verify(createRequest).complete();
    
    // Run the GET and verify that connected() call was received by the entity.
    com.tc.net.ClientID requester = new com.tc.net.ClientID(0);
    ServerEntityRequest getRequest = mockGetRequest(requester);
    managedEntity.invoke(getRequest);
    verify(activeServerEntity).connected(clientDescriptor);
    verify(getRequest).complete(null);
    
    // Run the RELEASE and verify that disconnected() call was received by the entity.
    ServerEntityRequest releaseRequest = mockReleaseRequest(requester);
    managedEntity.invoke(releaseRequest);
    verify(activeServerEntity).disconnected(clientDescriptor);
    verify(releaseRequest).complete();
  }

  
  @Test
  public void testCreatePassiveGetAndReleaseActive() throws Exception {
    // Create the entity.
    ServerEntityRequest createRequest = mockCreateEntityRequest(null);
    managedEntity.invoke(createRequest);
    verify(createRequest).complete();
    
    // Verify that it was created as a passive.
    verify(passiveServerEntity).createNew();
    verify(activeServerEntity, never()).createNew();
    
    // Now, switch modes to active.
    managedEntity.invoke(mockPromoteToActiveRequest());
    verify(activeServerEntity).loadExisting();
    
    // Verify that we fail to create it again.
    ServerEntityRequest failedCreateRequest = mockCreateEntityRequest(null);
    managedEntity.invoke(failedCreateRequest);
    verify(failedCreateRequest).failure(any(IllegalStateException.class));
    verify(failedCreateRequest, never()).complete();
    
    // Verify that we can get and release, just like with any other active.
    com.tc.net.ClientID requester = new com.tc.net.ClientID(0);
    ServerEntityRequest getRequest = mockGetRequest(requester);
    managedEntity.invoke(getRequest);
    verify(activeServerEntity).connected(clientDescriptor);
    verify(getRequest).complete(null);
    
    // Run the RELEASE and verify that disconnected() call was received by the entity.
    ServerEntityRequest releaseRequest = mockReleaseRequest(requester);
    managedEntity.invoke(releaseRequest);
    verify(activeServerEntity).disconnected(clientDescriptor);
    verify(releaseRequest).complete();
  }

  @Test
  public void testDestroy() throws Exception {
    managedEntity.invoke(mockPromoteToActiveRequest());
    managedEntity.invoke(mockCreateEntityRequest(null));
    
    managedEntity.invoke(mockRequestForAction(ServerEntityAction.DESTROY_ENTITY));

    verify(activeServerEntity).destroy();
    verify(serviceRegistry).destroy();
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
}
