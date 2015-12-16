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

import com.tc.net.ClientID;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.ServerEntityService;
import org.terracotta.exception.EntityAlreadyExistsException;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityVersionMismatchException;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.terracotta.entity.EntityResponse;


public class EntityManagerImpl implements EntityManager {
  private final ConcurrentMap<EntityID, ManagedEntity> entities = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, ServerEntityService<? extends ActiveServerEntity<EntityMessage, EntityResponse>, ? extends PassiveServerEntity<EntityMessage, EntityResponse>>> entityServices = new ConcurrentHashMap<>();

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
    ManagedEntity platform = createPlatformEntity();
    entities.put(platform.getID(), platform);
  }

  private ManagedEntity createPlatformEntity() {
    return new PlatformEntity(processorPipeline);
  }

  @Override
  public void enterActiveState() {
    // We can't enter active twice.
    Assert.assertFalse(this.shouldCreateActiveEntities);
    
    // Set the state of the manager.
    this.shouldCreateActiveEntities = true;
    processorPipeline.enterActiveState();
    // Walk all existing entities, recreating them as active.
    // NOTE:  While it would seem more direct (and not require adding new request types) to distinguish active/passive
    //  via ManagedEntity implementations, we would need to ensure that all pending requests for a ManagedEntity had
    //  been processed.  Thus, we will use addRequest, unless we can prove state of the entity request queue, at this point.
    for(ManagedEntity entity : this.entities.values()) {
      InternalRequest request = new InternalRequest(entity.getID(), entity.getVersion(), ServerEntityAction.PROMOTE_ENTITY_TO_ACTIVE);
      entity.addLifecycleRequest(request, null);
      request.waitForCompletion();
    }
  }

  @Override
  public void createEntity(EntityID id, long version, long consumerID) throws EntityException {
    // Valid entity versions start at 1.
    Assert.assertTrue(version > 0);
    ManagedEntity temp = new ManagedEntityImpl(id, version, serviceRegistry.subRegistry(consumerID),
        clientEntityStateManager, processorPipeline, getVersionCheckedService(id, version), this.shouldCreateActiveEntities);
    if (entities.putIfAbsent(id, temp) != null) {
      throw new EntityAlreadyExistsException(id.getClassName(), id.getEntityName());
    }
  }

  @Override
  public void loadExisting(EntityID entityID, long recordedVersion, long consumerID, byte[] configuration) throws EntityException {
    // Valid entity versions start at 1.
    Assert.assertTrue(recordedVersion > 0);
    ManagedEntity temp = new ManagedEntityImpl(entityID, recordedVersion, serviceRegistry.subRegistry(consumerID), clientEntityStateManager, processorPipeline, getVersionCheckedService(entityID, recordedVersion), this.shouldCreateActiveEntities);
    if (entities.putIfAbsent(entityID, temp) != null) {
      throw new IllegalStateException("Double create for entity " + entityID);
    }
    InternalRequest request = new InternalRequest(entityID, recordedVersion, ServerEntityAction.LOAD_EXISTING_ENTITY);
    temp.addLifecycleRequest(request, configuration);
  }

  @Override
  public void destroyEntity(EntityID id) throws EntityException {
    if (entities.remove(id) == null) {
      throw new EntityNotFoundException(id.getClassName(), id.getEntityName());
    }
  }

  @Override
  public Optional<ManagedEntity> getEntity(EntityID id, long version) throws EntityException {
    Assert.assertNotNull(id);
    if (EntityID.NULL_ID.equals(id)) {
//  short circuit for null entity, it's never here
      return Optional.empty();
    }
    // Valid entity versions start at 1.
    Assert.assertTrue(version > 0);
    ManagedEntity entity = entities.get(id);
    if (entity != null) {
      //  check the provided version against the version of the entity
      if (entity.getVersion() != version) {
        throw new EntityVersionMismatchException(id.getClassName(), id.getEntityName(), entity.getVersion(), version);
      }
    }
    return Optional.ofNullable(entity);
  }
  
  public Collection<ManagedEntity> getAll() {
    return new ArrayList<>(entities.values());
  }
  
  private ServerEntityService<? extends ActiveServerEntity<EntityMessage, EntityResponse>, ? extends PassiveServerEntity<EntityMessage, EntityResponse>> getVersionCheckedService(EntityID entityID, long version) throws EntityVersionMismatchException {
    // Valid entity versions start at 1.
    Assert.assertTrue(version > 0);
    String typeName = entityID.getClassName();
    ServerEntityService<? extends ActiveServerEntity<EntityMessage, EntityResponse>, ? extends PassiveServerEntity<EntityMessage, EntityResponse>> service = entityServices.get(typeName);
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
      throw new EntityVersionMismatchException(typeName, entityID.getEntityName(), serviceVersion, version);
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
    private boolean complete = false;

    public InternalRequest(EntityID id, long version, ServerEntityAction action) {
      this.entity = id;
      this.version = version;
      this.action = action;
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
    public synchronized void complete() {
      complete = true;
      notifyAll();
    }
    @Override
    public void complete(byte[] value) {
      // This call is not expected in the InternalRequest use-case.
      throw new UnsupportedOperationException("Complete does not support a value");
    }
    @Override
    public void failure(EntityException e) {
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
    
    public synchronized void waitForCompletion() {
      boolean interrupted = false;
      try {
        while(!complete) {
          try {
            wait();
          } catch (InterruptedException ie) {
            interrupted = true;
          }
        }
      } finally {
        if (interrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }
}

