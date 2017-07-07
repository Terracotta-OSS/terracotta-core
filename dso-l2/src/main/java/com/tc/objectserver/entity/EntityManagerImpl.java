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

import com.tc.exception.TCShutdownServerException;
import com.tc.object.EntityDescriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityServerService;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityVersionMismatchException;

import com.tc.object.EntityID;
import com.tc.object.FetchID;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.core.api.ITopologyEventCollector;
import com.tc.services.TerracottaServiceProviderRegistry;
import com.tc.util.Assert;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;
import org.terracotta.exception.EntityNotProvidedException;
import com.tc.objectserver.api.ManagementKeyCallback;


public class EntityManagerImpl implements EntityManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(EntityManagerImpl.class);
  private final ConcurrentMap<EntityID, FetchID> entities = new ConcurrentHashMap<>();
  private final ConcurrentMap<FetchID, ManagedEntity> entityIndex = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, EntityServerService<EntityMessage, EntityResponse>> entityServices = new ConcurrentHashMap<>();

  private final ClassLoader creationLoader;
  private final TerracottaServiceProviderRegistry serviceRegistry;
  private final ClientEntityStateManager clientEntityStateManager;
  private final ITopologyEventCollector eventCollector;
  
  private final ManagementKeyCallback flushLocalPipeline;
  
  private final RequestProcessor processorPipeline;
  private boolean shouldCreateActiveEntities;
  
  private final Semaphore snapshotLock = new Semaphore(1); // sync and create or destroy are mutually exclusive
  
  // The sort comparator.
  private final Comparator<ManagedEntity> consumerIdSorter = new Comparator<ManagedEntity>() {
    @Override
    public int compare(ManagedEntity o1, ManagedEntity o2) {
      long firstID = o1.getConsumerID();
      long secondID = o2.getConsumerID();
      // NOTE:  The ids are unique.
      Assert.assertTrue(firstID != secondID);
      return (firstID > secondID)
          ? 1
          : -1;
    }};


  public EntityManagerImpl(TerracottaServiceProviderRegistry serviceRegistry, 
      ClientEntityStateManager clientEntityStateManager, ITopologyEventCollector eventCollector, 
      RequestProcessor processor, ManagementKeyCallback flushLocalPipeline) {
    this.serviceRegistry = serviceRegistry;
    this.clientEntityStateManager = clientEntityStateManager;
    this.eventCollector = eventCollector;
    this.processorPipeline = processor;
    // By default, the server starts up in a passive mode so we will create passive entities.
    this.shouldCreateActiveEntities = false;
    this.creationLoader = Thread.currentThread().getContextClassLoader();
    this.flushLocalPipeline = flushLocalPipeline;
    ManagedEntity platform = createPlatformEntity();
    entities.put(platform.getID(), new FetchID(0L));
    entityIndex.put(new FetchID(0L), platform);
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
    //  locking the snapshotting until full active is achieved 
    snapshotLock.acquireUninterruptibly();
    try {
      // Tell our implementation-provided services to become active (since they might have modes of operation).
      this.serviceRegistry.notifyServerDidBecomeActive();

      // Set the state of the manager.
      this.shouldCreateActiveEntities = true;
      // We can promote directly because this method is only called from PTH initialize 
      //  thus, this only happens once RTH is spun down and PTH is beginning to spin up.  We know the request queues are clear
      try {
        // issue-439: We need to sort these entities, ascending by consumerID.
        List<ManagedEntity> sortingList = new ArrayList<ManagedEntity>(this.entityIndex.values());
        Collections.sort(sortingList, this.consumerIdSorter);
        for (ManagedEntity entity : sortingList) {
          entity.promoteEntity();
        }
      } catch (ConfigurationException ce) {
        LOGGER.warn("failure to promote all entities.  Server is crashing", ce);
        throw new TCShutdownServerException("failure to promote all entities.  Server is crashing");
      }
  //  only enter active state after all the entities have promoted to active
      processorPipeline.enterActiveState();
    } finally {
      snapshotLock.release();
    }
  }

  @Override
  public ManagedEntity createEntity(EntityID id, long version, long consumerID, boolean canDelete) throws EntityException {
    // Valid entity versions start at 1.
    Assert.assertTrue(version > 0);
    EntityServerService service = getVersionCheckedService(id, version);
    snapshotLock.acquireUninterruptibly();
    try {
    //  if active, reuse the managed entity if it is mapped to an id.  if passive, MUST map the id to the index of the managed entity
      FetchID current = entities.compute(id, (eid, fetch)-> shouldCreateActiveEntities ? Optional.ofNullable(fetch).orElse(new FetchID(consumerID)) : new FetchID(consumerID));
      
      ManagedEntity temp = entityIndex.computeIfAbsent(current, (fetch)->
        new ManagedEntityImpl(id, version, consumerID, flushLocalPipeline, serviceRegistry.subRegistry(consumerID),
          clientEntityStateManager, eventCollector, processorPipeline, service, shouldCreateActiveEntities, canDelete));

      return temp;
    } finally {
      snapshotLock.release();
    }
  }

  @Override
  public void loadExisting(EntityID entityID, long recordedVersion, long consumerID, boolean canDelete, byte[] configuration) throws EntityException {
    // Valid entity versions start at 1.
    Assert.assertTrue(recordedVersion > 0);
    EntityServerService service = getVersionCheckedService(entityID, recordedVersion);
    FetchID set = new FetchID(consumerID);
    Object checkNull = entities.put(entityID, set);
    Assert.assertNull(checkNull); //  must be null, nothing should be competing
    ManagedEntity temp = new ManagedEntityImpl(entityID, recordedVersion, consumerID, flushLocalPipeline, 
          serviceRegistry.subRegistry(consumerID), clientEntityStateManager, this.eventCollector, 
          processorPipeline, service, this.shouldCreateActiveEntities, canDelete);
    
    checkNull = entityIndex.put(set, temp);
    Assert.assertNull(checkNull); //  must be null, nothing should be competing
    try {
      temp.loadEntity(configuration);
    } catch (ConfigurationException ce) {
      LOGGER.warn("failure to load an existing entity.  Server is crashing", ce);
      throw new TCShutdownServerException("failure to load an existing entity.  Server is crashing");
    }
  }

  @Override
  public boolean removeDestroyed(FetchID id) {
    snapshotLock.acquireUninterruptibly();
    try {
      ManagedEntity e = entityIndex.computeIfPresent(id,(fetch,entity)->{
        if (entity.isRemoveable()) {
          entities.remove(entity.getID(), fetch);
          return null;
        } else {
          return entity;
        }
      });
      
      if (e == null) {
        LOGGER.debug("removed " + id);
        return true;
      } else {
        return false;
      }
    } finally {
      snapshotLock.release();
    }
  }

  @Override
  public Optional<ManagedEntity> getEntity(EntityDescriptor descriptor) throws EntityException {
    if (descriptor.isIndexed()) {
      return getEntity(descriptor.getFetchID());
    } else {
      return getEntity(descriptor.getEntityID(), descriptor.getClientSideVersion());
    }
  }
  
  private Optional<ManagedEntity> getEntity(FetchID idx) {
    Assert.assertFalse(idx.isNull());
    return Optional.ofNullable(this.entityIndex.get(idx));
  }
  
  private Optional<ManagedEntity> getEntity(EntityID id, long version) throws EntityException {
    Assert.assertNotNull(id);
    if (EntityID.NULL_ID == id) {
//  just do instance check, believe it or not, equality check is expensive due to frequency called
//  short circuit for null entity, it's never here
      return Optional.empty();
    }
    FetchID fetch = entities.get(id);
    ManagedEntity entity = null;
    if (fetch != null) {
      entity = entityIndex.get(fetch);
      //  if the version in the descriptor is not valid, don't check 
      //  check the provided version against the version of the entity
      if (version > 0 && entity.getVersion() != version) {
        throw new EntityVersionMismatchException(id.getClassName(), id.getEntityName(), entity.getVersion(), version);
      }
    }
    return Optional.ofNullable(entity);
  }

  @Override
  public Collection<ManagedEntity> getAll() {
    return new ArrayList<>(entityIndex.values());
  }
  
  @Override
  public List<ManagedEntity> snapshot(Consumer<List<ManagedEntity>> runFirst) {
    snapshotLock.acquireUninterruptibly();
    try {
      List<ManagedEntity> sortingList = new ArrayList<ManagedEntity>(this.entityIndex.values());
      Collections.sort(sortingList, this.consumerIdSorter);
      if (runFirst != null) {
        runFirst.accept(sortingList);
      }
      return sortingList;
    } finally {
      snapshotLock.release();
    }
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
  public void resetReferences() {
    for (ManagedEntity me : entityIndex.values()) {
      me.resetReferences(0);
    }
  }

  @Override
  public MessageCodec<? extends EntityMessage, ? extends EntityResponse> getMessageCodec(EntityDescriptor eid) {
    ManagedEntity e = this.entityIndex.get(eid.getFetchID());
    if (e != null) {
      return e.getCodec();
    }
    return null;
  }

  @Override
  public String toString() {
    return "EntityManagerImpl{" + "entities=" + entities.keySet() + '}';
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumpCollector) {
    // We want to dump a size, minimally acting as a heading for the section (in case there is nothing).
    Set<Map.Entry<EntityID, FetchID>> entries = entities.entrySet();
    stateDumpCollector.addState("Number of entities", Integer.toString(entries.size()));
    for (Map.Entry<EntityID, FetchID> entry : entries) {
      EntityID entityID = entry.getKey();
      try {
        entityIndex.get(entry.getValue()).addStateTo(stateDumpCollector.subStateDumpCollector(entityID.getClassName() + ":" + entityID.getEntityName()));
      } catch (Throwable t) {
        stateDumpCollector.subStateDumpCollector(entityID.getClassName() + ":" + entityID.getEntityName()).addState("exception", t.getMessage());
      }
    }
  }
}

