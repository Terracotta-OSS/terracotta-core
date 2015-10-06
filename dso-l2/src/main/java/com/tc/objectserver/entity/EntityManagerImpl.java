package com.tc.objectserver.entity;

import com.tc.net.ClientID;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.ServerEntityService;

import com.tc.net.NodeID;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.services.TerracottaServiceProviderRegistry;
import com.tc.util.Assert;
import java.util.Collection;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class EntityManagerImpl implements EntityManager {
  private final ConcurrentMap<EntityID, ManagedEntity> entities = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, ServerEntityService<? extends ActiveServerEntity, ? extends PassiveServerEntity>> entityServices = new ConcurrentHashMap<>();

  private final TerracottaServiceProviderRegistry serviceRegistry;
  private final ClientEntityStateManager clientEntityStateManager;
  
  private final RequestProcessor processorPipeline;
  private boolean shouldCreateActiveEntities;

  public EntityManagerImpl(TerracottaServiceProviderRegistry serviceRegistry, ClientEntityStateManager clientEntityStateManager, RequestProcessor processor) {
    this.serviceRegistry = serviceRegistry;
    this.clientEntityStateManager = clientEntityStateManager;
    this.processorPipeline = processor;
    // By default, the server starts up in a passive mode so we will create passive entities.
    this.shouldCreateActiveEntities = false;
  }

  @Override
  public void enterActiveState() {
    // We can't enter active twice.
    Assert.assertFalse(this.shouldCreateActiveEntities);
    
    // Set the state of the manager.
    this.shouldCreateActiveEntities = true;
    
    // Walk all existing entities, recreating them as active.
    // NOTE:  While it would seem more direct (and not require adding new request types) to distinguish active/passive
    //  via ManagedEntity implementations, we would need to ensure that all pending requests for a ManagedEntity had
    //  been processed.  Thus, we will use addRequest, unless we can prove state of the entity request queue, at this point.
    for(ManagedEntity entity : this.entities.values()) {
      InternalRequest request = new InternalRequest(entity.getID(), entity.getVersion(), ServerEntityAction.PROMOTE_ENTITY_TO_ACTIVE, null);
      entity.addRequest(request);
    }
  }

  @Override
  public void createEntity(EntityID id, long version, long consumerID) {
    // Valid entity versions start at 1.
    Assert.assertTrue(version > 0);
    ManagedEntity temp = new ManagedEntityImpl(id, version, serviceRegistry.subRegistry(consumerID),
        clientEntityStateManager, processorPipeline, getVersionCheckedService(id, version), this.shouldCreateActiveEntities);
    if (entities.putIfAbsent(id, temp) != null) {
      throw new IllegalStateException("Double create for entity " + id);
    }
  }

  @Override
  public void loadExisting(EntityID entityID, long recordedVersion, long consumerID, byte[] configuration) {
    // Valid entity versions start at 1.
    Assert.assertTrue(recordedVersion > 0);
    ManagedEntity temp = new ManagedEntityImpl(entityID, recordedVersion, serviceRegistry.subRegistry(consumerID), clientEntityStateManager, processorPipeline, getVersionCheckedService(entityID, recordedVersion), this.shouldCreateActiveEntities);
    if (entities.putIfAbsent(entityID, temp) != null) {
      throw new IllegalStateException("Double create for entity " + entityID);
    }
    InternalRequest request = new InternalRequest(entityID, recordedVersion, ServerEntityAction.LOAD_EXISTING_ENTITY, configuration);
    temp.addRequest(request);
  }

  @Override
  public void destroyEntity(EntityID id) {
    if (entities.remove(id) == null) {
      throw new IllegalStateException("Deleted an non-existent entity " + id);
    }
  }

  @Override
  public Optional<ManagedEntity> getEntity(EntityID id, long version) {
    // Valid entity versions start at 1.
    Assert.assertTrue(version > 0);
    // Ask the service which provides this type of entity whether or not this is the version it supports.
    // Note that we ignore the return value, only interested in validating that the version is consistent.
    getVersionCheckedService(id, version);
    return Optional.ofNullable(entities.get(id));
  }
  
  public Collection<ManagedEntity> getAll() {
    return entities.values();
  }
  
  private ServerEntityService<? extends ActiveServerEntity, ? extends PassiveServerEntity> getVersionCheckedService(EntityID entityID, long version) {
    // Valid entity versions start at 1.
    Assert.assertTrue(version > 0);
    String typeName = entityID.getClassName();
    ServerEntityService<? extends ActiveServerEntity, ? extends PassiveServerEntity> service = entityServices.get(typeName);
    if (service == null) {
      service = ServerEntityFactory.getService(typeName, EntityManagerImpl.class.getClassLoader());
      // getService only fails to resolve by throwing.
      Assert.assertNotNull(service);
      Object oldService = entityServices.putIfAbsent(typeName, service);
      // This needs to be null or else there was some kind of unexpected concurrent access which would have caused failure or a duplicate entry.
      Assert.assertNull(oldService);
    }
    
    // We must have a service by now or we would have thrown.
    Assert.assertNotNull(service);
    long serviceVersion = service.getVersion();
    if (serviceVersion != version) {
      throw new IllegalArgumentException("Version " + version + " not supported by " + typeName + "entity factory (expects version " + serviceVersion + ")");
    }
    return service;
  }
  
  /**
   * This implementation does nothing beyond providing the desired action type.
   */
  private static class InternalRequest implements ServerEntityRequest {
    private final EntityID entity;
    private final long version;
    private final ServerEntityAction action;
    private final byte[] payload;
    
    public InternalRequest(EntityID id, long version, ServerEntityAction action, byte[] payload) {
      this.entity = id;
      this.version = version;
      this.action = action;
      this.payload = payload;
    }
    @Override
    public ServerEntityAction getAction() {
      return this.action;
    }
    @Override
    public NodeID getNodeID() {
      return ClientID.NULL_ID;
    }
    @Override
    public ClientDescriptor getSourceDescriptor() {
      return new ClientDescriptorImpl(ClientID.NULL_ID, new EntityDescriptor(entity, ClientInstanceID.NULL_ID, version));
    }
    @Override
    public byte[] getPayload() {
      return this.payload;
    }
    @Override
    public void complete() {
      // No special action.
    }
    @Override
    public void complete(byte[] value) {
      // This call is not expected in the InternalRequest use-case.
      throw new UnsupportedOperationException("Complete does not support a value");
    }
    @Override
    public void failure(Exception e) {
      throw new UnsupportedOperationException("Failure not expected for InternalRequest handling", e);
    }
    @Override
    public void received() {
      // Not expected.
      throw new UnsupportedOperationException("Received is not expected");
    }

    @Override
    public TransactionID getTransaction() {
      return TransactionID.NULL_ID;
    }

    @Override
    public TransactionID getOldestTransactionOnClient() {
      return TransactionID.NULL_ID;
    }

    @Override
    public boolean requiresReplication() {
      // These are internal requests so they are never replicated.
      return false;
    }
  }
}
