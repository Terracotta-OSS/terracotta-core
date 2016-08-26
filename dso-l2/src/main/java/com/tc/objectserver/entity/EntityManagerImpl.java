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

import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityServerService;
import org.terracotta.entity.StateDumper;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityVersionMismatchException;

import com.tc.object.EntityID;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.core.api.ITopologyEventCollector;
import com.tc.services.TerracottaServiceProviderRegistry;
import com.tc.util.Assert;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.SyncMessageCodec;
import org.terracotta.exception.EntityNotProvidedException;


public class EntityManagerImpl implements EntityManager {
  private final ConcurrentMap<EntityID, ManagedEntity> entities = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, EntityServerService<EntityMessage, EntityResponse>> entityServices = new ConcurrentHashMap<>();

  private final ClassLoader creationLoader;
  private final TerracottaServiceProviderRegistry serviceRegistry;
  private final ClientEntityStateManager clientEntityStateManager;
  private final ITopologyEventCollector eventCollector;
  
  private final BiConsumer<EntityID, Long> noopLoopback;
  
  private final RequestProcessor processorPipeline;
  private boolean shouldCreateActiveEntities;

  public EntityManagerImpl(TerracottaServiceProviderRegistry serviceRegistry, 
      ClientEntityStateManager clientEntityStateManager, ITopologyEventCollector eventCollector, 
      RequestProcessor processor, BiConsumer<EntityID, Long> noopLoopback) {
    this.serviceRegistry = serviceRegistry;
    this.clientEntityStateManager = clientEntityStateManager;
    this.eventCollector = eventCollector;
    this.processorPipeline = processor;
    // By default, the server starts up in a passive mode so we will create passive entities.
    this.shouldCreateActiveEntities = false;
    this.creationLoader = Thread.currentThread().getContextClassLoader();
    this.noopLoopback = noopLoopback;
    ManagedEntity platform = createPlatformEntity();
    entities.put(platform.getID(), platform);
  }

  private ManagedEntity createPlatformEntity() {
    return new PlatformEntity(processorPipeline);
  }

  @Override
  public ClassLoader getEntityLoader() {
    return this.creationLoader;
  }

  @Override
  public void enterActiveState() {
    // We can't enter active twice.
    Assert.assertFalse(this.shouldCreateActiveEntities);
    
    // Set the state of the manager.
    this.shouldCreateActiveEntities = true;
    // We can promote directly because this method is only called from PTH initialize 
    //  thus, this only happens once RTH is spun down and PTH is beginning to spin up.  We know the request queues are clear
    for (ManagedEntity entity : this.entities.values()) {
      entity.promoteEntity();
    }
//  only enter active state after all the entities have promoted to active
    processorPipeline.enterActiveState();
  }

  @Override
  public ManagedEntity createEntity(EntityID id, long version, long consumerID, boolean canDelete) throws EntityException {
    // Valid entity versions start at 1.
    Assert.assertTrue(version > 0);
    ManagedEntity temp = new ManagedEntityImpl(id, version, consumerID, noopLoopback, serviceRegistry.subRegistry(consumerID),
        clientEntityStateManager, this.eventCollector, processorPipeline, getVersionCheckedService(id, version), this.shouldCreateActiveEntities, canDelete);
    ManagedEntity exists = entities.putIfAbsent(id, temp);
    return exists != null ? exists : temp;
  }

  @Override
  public void loadExisting(EntityID entityID, long recordedVersion, long consumerID, boolean canDelete, byte[] configuration) throws EntityException {
    // Valid entity versions start at 1.
    Assert.assertTrue(recordedVersion > 0);
    ManagedEntity temp = new ManagedEntityImpl(entityID, recordedVersion, consumerID, noopLoopback, serviceRegistry.subRegistry(consumerID), clientEntityStateManager, this.eventCollector, processorPipeline, getVersionCheckedService(entityID, recordedVersion), this.shouldCreateActiveEntities, canDelete);
    if (entities.putIfAbsent(entityID, temp) != null) {
      throw new IllegalStateException("Double create for entity " + entityID);
    }    
    temp.loadEntity(configuration);
  }

  @Override
  public boolean removeDestroyed(EntityID id) {
    boolean removed = false;
    ManagedEntity e = entities.get(id);
    if (e != null && e.isDestroyed()) {
      if (entities.remove(id) != null) {
        removed = true;
      }
    }
    return removed;
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
  
  private EntityServerService<EntityMessage, EntityResponse> getVersionCheckedService(EntityID entityID, long version) throws EntityVersionMismatchException, EntityNotProvidedException {
    // Valid entity versions start at 1.
    Assert.assertTrue(version > 0);
    String typeName = entityID.getClassName();
    EntityServerService<EntityMessage, EntityResponse> service = entityServices.get(typeName);
    if (service == null) {
      try {
        service = ServerEntityFactory.getService(typeName, this.creationLoader);
      } catch (ClassNotFoundException notfound) {
        throw new EntityNotProvidedException(typeName, entityID.getEntityName());
      }
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

  @Override
  public MessageCodec<EntityMessage, EntityResponse> getMessageCodec(EntityID eid) {
    EntityServerService<EntityMessage, EntityResponse> service = entityServices.get(eid.getClassName());
    if (service != null) {
      return service.getMessageCodec();
    }
    return null;
  }

  @Override
  public SyncMessageCodec<EntityMessage> getSyncMessageCodec(EntityID eid) {
    EntityServerService<EntityMessage, EntityResponse> service = entityServices.get(eid.getClassName());
    if (service != null) {
      return service.getSyncMessageCodec();
    }
    return null;
  }  

  @Override
  public String toString() {
    return "EntityManagerImpl{" + "entities=" + entities.keySet() + '}';
  }

  @Override
  public void dumpStateTo(StateDumper stateDumper) {
    for (Map.Entry<EntityID, ManagedEntity> entry : entities.entrySet()) {
      EntityID entityID = entry.getKey();
      entry.getValue().dumpStateTo(stateDumper.subStateDumper(entityID.getClassName() + ":" + entityID.getEntityName()));
    }
  }
}

