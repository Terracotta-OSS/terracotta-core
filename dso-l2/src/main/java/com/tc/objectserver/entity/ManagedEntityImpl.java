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

import com.tc.l2.msg.PassiveSyncMessage;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.MessageChannel;

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.CommonServerEntity;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.PassiveSynchronizationChannel;
import org.terracotta.entity.ServerEntityService;
import org.terracotta.entity.ServiceRegistry;

import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.objectserver.core.api.ITopologyEventCollector;
import com.tc.util.Assert;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;
import org.terracotta.exception.EntityAlreadyExistsException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityUserException;


public class ManagedEntityImpl implements ManagedEntity {
  private static final TCLogger logger   = TCLogging.getLogger(ManagedEntityImpl.class);

  private final RequestProcessor executor;

  private final EntityID id;
  private final long version;
  private final ServiceRegistry registry;
  private final ClientEntityStateManager clientEntityStateManager;
  private final ITopologyEventCollector eventCollector;
  private final ServerEntityService<? extends ActiveServerEntity<EntityMessage, EntityResponse>, ? extends PassiveServerEntity<EntityMessage, EntityResponse>> factory;
  // isInActiveState defines which entity type to check/create - we need the flag to represent the pre-create state.
  private boolean isInActiveState;
  private volatile ActiveServerEntity<EntityMessage, EntityResponse> activeServerEntity;
  private volatile PassiveServerEntity<EntityMessage, EntityResponse> passiveServerEntity;
  //  reconnect access has to be exclusive.  it is out-of-band from normal invoke access
  private final ReadWriteLock reconnectAccessLock = new ReentrantReadWriteLock();
  // NOTE:  This may be removed in the future if we change how we access the config from the ServerEntityService but
  //  it presently holds the config we used when we first created passiveServerEntity (if it isn't null).  It is used
  //  when we promote to an active.
  private byte[] constructorInfo;

  ManagedEntityImpl(EntityID id, long version, ServiceRegistry registry, ClientEntityStateManager clientEntityStateManager, ITopologyEventCollector eventCollector,
                    RequestProcessor process, ServerEntityService<? extends ActiveServerEntity<EntityMessage, EntityResponse>, ? extends PassiveServerEntity<EntityMessage, EntityResponse>> factory,
                    boolean isInActiveState) {
    this.id = id;
    this.version = version;
    this.registry = registry;
    this.clientEntityStateManager = clientEntityStateManager;
    this.eventCollector = eventCollector;
    this.factory = factory;
    this.executor = process;
    this.isInActiveState = isInActiveState;
  }

  @Override
  public EntityID getID() {
    return id;
  }

  @Override
  public long getVersion() {
    return version;
  }

  @Override
  public void addInvokeRequest(final ServerEntityRequest request, byte[] payload) {
    ClientDescriptor client = request.getSourceDescriptor();
    Assert.assertTrue(request.getAction() == ServerEntityAction.INVOKE_ACTION);
      // Invoke and payload requests need to wait for the entity creation so that they can request the concurrency strategy.
    if (this.activeServerEntity == null && this.passiveServerEntity == null) {
      request.failure(new EntityNotFoundException(this.getID().getClassName(), this.getID().getEntityName()));
      return;
    }

    ConcurrencyStrategy<EntityMessage> concurrencyStrategy = activeServerEntity != null ? activeServerEntity.getConcurrencyStrategy() : null;
    // We only decode messages for INVOKE and RECEIVE_SYNC_PAYLOAD requests.
      // We might get replicated invokes before the create has completed
    CommonServerEntity<EntityMessage,EntityResponse> entity = (null != this.activeServerEntity) ? this.activeServerEntity : this.passiveServerEntity;
      Assert.assertNotNull(entity);
    MessageCodec<EntityMessage, EntityResponse> deserializer = entity.getMessageCodec();
    Assert.assertNotNull(deserializer);
      Assert.assertNotNull(payload);
    final EntityMessage message = deserializer.deserialize(payload);
    // Since concurrency key is pulled out in different ways for these different message types, we will do that here.
    final int concurrencyKey = ((null != concurrencyStrategy)) ?
      concurrencyStrategy.concurrencyKey(message) : ConcurrencyStrategy.MANAGEMENT_KEY;
    executor.scheduleRequest(getEntityDescriptorForSource(client), request, payload, ()->invoke(request, message, concurrencyKey), concurrencyKey);
    }

  @Override
  public void processSyncMessage(ServerEntityRequest sync, byte[] payload, int concurrencyKey) {
    if (sync.getAction() == ServerEntityAction.RECEIVE_SYNC_ENTITY_START || 
        sync.getAction() == ServerEntityAction.RECEIVE_SYNC_ENTITY_END) {
      executor.scheduleRequest(getEntityDescriptorForSource(sync.getSourceDescriptor()), sync, payload, ()->invoke(sync, payload), concurrencyKey);
    } else if (sync.getAction() == ServerEntityAction.RECEIVE_SYNC_PAYLOAD) {
      MessageCodec<EntityMessage, EntityResponse> codec = this.passiveServerEntity.getMessageCodec();
      executor.scheduleRequest(getEntityDescriptorForSource(sync.getSourceDescriptor()), sync, payload, ()->invoke(sync, codec.deserializeForSync(concurrencyKey, payload), concurrencyKey), concurrencyKey);
    } else {
      executor.scheduleRequest(getEntityDescriptorForSource(sync.getSourceDescriptor()), sync, payload, ()->invoke(sync, null, concurrencyKey), concurrencyKey);
    }
  }

  @Override
  public void addLifecycleRequest(ServerEntityRequest create, byte[] data) {
    executor.scheduleRequest(getEntityDescriptorForSource(create.getSourceDescriptor()), create, data, ()->invoke(create, data), ConcurrencyStrategy.MANAGEMENT_KEY);
  } 

  @Override
  public void reconnectClient(ClientID clientID, ClientDescriptor clientDescriptor, byte[] extendedReconnectData) {
    EntityDescriptor entityDescriptor = getEntityDescriptorForSource(clientDescriptor);
    clientEntityStateManager.addReference(clientID, entityDescriptor);
    if (!this.isInActiveState) {
      throw new IllegalStateException("server is not active");
    }
    Assert.assertNotNull(this.activeServerEntity);
    Lock write = reconnectAccessLock.writeLock();
    try {
      write.lock();
      this.activeServerEntity.connected(clientDescriptor);
      this.activeServerEntity.handleReconnect(clientDescriptor, extendedReconnectData);      
    } finally {
      write.unlock();
    }
  }
  
  private void invoke(ServerEntityRequest request, byte[] payload) {
    Lock read = reconnectAccessLock.readLock();
      try {
        read.lock();
        switch (request.getAction()) {
          case CREATE_ENTITY:
            createEntity(request, payload);
            break;
          case FETCH_ENTITY:
            getEntity(request);
            break;
          case RELEASE_ENTITY:
            releaseEntity(request);
            break;
          case DESTROY_ENTITY:
//  all request queues are flushed because this action is on the MGMT_KEY
            destroyEntity(request);
            break;
          case PROMOTE_ENTITY_TO_ACTIVE:
            promoteEntity(request);
            break;
          case RECEIVE_SYNC_ENTITY_START:
            receiveSyncEntityStart(request, payload);
            break;
          case RECEIVE_SYNC_ENTITY_END:
            receiveSyncEntityEnd(request);
            break;
          case LOAD_EXISTING_ENTITY:
            loadExisting(request, payload);
            break;
          default:
            throw new IllegalArgumentException("Unknown request " + request);
        }
      } catch (Exception e) {
        // Wrap this exception.
        EntityUserException wrapper = new EntityUserException(id.getClassName(), id.getEntityName(), e);
        request.failure(wrapper);
      } finally {
        read.unlock();
      }
  }

  /**
   * Runs on a concurrent thread from RequestProcessor (meaning that this could be invoked concurrently on the same entity)
   * to handle one request.
   *  
   * @param request The request to process
   * @param concurrencyKey The key this thread is processing by running this request
   * @param message 
   */
  private void invoke(ServerEntityRequest request, EntityMessage message, int concurrencyKey) {
    Lock read = reconnectAccessLock.readLock();
      try {
        read.lock();
        if (logger.isDebugEnabled()) {
          logger.debug("Invoking " + request.getAction() + " on " + getID() + "/" + concurrencyKey);
        }
        switch (request.getAction()) {
          case INVOKE_ACTION:
            performAction(request, message);
            break;
          case REQUEST_SYNC_ENTITY:
//  use typing for this distinction since it is server generated?
            performSync(request, concurrencyKey);
            break;
          case RECEIVE_SYNC_ENTITY_KEY_START:
            receiveSyncEntityKeyStart(request, concurrencyKey);
            break;
          case RECEIVE_SYNC_ENTITY_KEY_END:
            receiveSyncEntityKeyEnd(request, concurrencyKey);
            break;
          case RECEIVE_SYNC_PAYLOAD:
            receiveSyncEntityPayload(request, message);
            break;
          case DOES_EXIST:
          case NOOP:
            // Never occur on this level.
            throw new IllegalArgumentException("Unexpected request " + request);
          default:
            throw new IllegalArgumentException("Unknown request " + request);
        }
      } catch (Exception e) {
        // Wrap this exception.
        EntityUserException wrapper = new EntityUserException(id.getClassName(), id.getEntityName(), e);
        request.failure(wrapper);
      } finally {
        read.unlock();
      }
  }
  
  private void receiveSyncEntityStart(ServerEntityRequest request, byte[] constructor) {
    if (this.passiveServerEntity != null) {
      throw new AssertionError("not null " + this.getID());
    }
    Assert.assertNull(this.passiveServerEntity);
//  going to start by building the passive instance
    createEntity(request, constructor);
//  it better be a passive instance
    Assert.assertNotNull(this.passiveServerEntity);
    this.passiveServerEntity.startSyncEntity();
  }

  private void receiveSyncEntityEnd(ServerEntityRequest request) {
    // This only makes sense if we have a passive instance.
    Assert.assertNotNull(this.passiveServerEntity);
    this.passiveServerEntity.endSyncEntity();
    request.complete();
  }

  private void receiveSyncEntityKeyStart(ServerEntityRequest request, int concurrencyKey) {
    // This only makes sense if we have a passive instance.
    Assert.assertNotNull(this.passiveServerEntity);
    this.passiveServerEntity.startSyncConcurrencyKey(concurrencyKey);
    request.complete();
  }

  private void receiveSyncEntityKeyEnd(ServerEntityRequest request, int concurrencyKey) {
    // This only makes sense if we have a passive instance.
    Assert.assertNotNull(this.passiveServerEntity);
    this.passiveServerEntity.endSyncConcurrencyKey(concurrencyKey);
    request.complete();
  }

  private void receiveSyncEntityPayload(ServerEntityRequest request, EntityMessage message) {
    // This only makes sense if we have a passive instance.
    Assert.assertNotNull(this.passiveServerEntity);
    this.passiveServerEntity.invoke(message);
    request.complete();
  }

  private void destroyEntity(ServerEntityRequest request) {
    CommonServerEntity<EntityMessage, EntityResponse> commonServerEntity = this.isInActiveState
        ? activeServerEntity
        : passiveServerEntity;
    if (null != commonServerEntity) {
      ClientDescriptor sourceDescriptor = request.getSourceDescriptor();
      EntityDescriptor entityDescriptor = getEntityDescriptorForSource(sourceDescriptor);
      // We want to ensure that nobody somehow has a reference to this entity.
      clientEntityStateManager.verifyNoReferences(entityDescriptor);
      commonServerEntity.destroy();
    }
    request.complete();
  }

  private void createEntity(ServerEntityRequest createEntityRequest, byte[] constructorInfo) {
    if (this.activeServerEntity != null || this.passiveServerEntity != null) {
      createEntityRequest.failure(new EntityAlreadyExistsException(this.getID().getClassName(), this.getID().getEntityName()));
      return;
    }
    this.constructorInfo = constructorInfo;
    CommonServerEntity<EntityMessage, EntityResponse> entityToCreate = null;
    // Create the appropriate kind of entity, based on our active/passive state.
    if (this.isInActiveState) {
      if (null != this.activeServerEntity) {
        throw new IllegalStateException("Active entity " + id + " already exists.");
      } else {
        this.activeServerEntity = this.factory.createActiveEntity(this.registry, this.constructorInfo);
        entityToCreate = this.activeServerEntity;
      }
    } else {
      if (null != this.passiveServerEntity) {
        throw new IllegalStateException("Passive entity " + id + " already exists.");
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("created passive entity " + this.getID() + " due to " + createEntityRequest.getAction());
        }
        this.passiveServerEntity = this.factory.createPassiveEntity(this.registry, this.constructorInfo);
        // Store the configuration in case we promote.
        entityToCreate = this.passiveServerEntity;
      }
    }
    createEntityRequest.complete();
    // We currently don't support loading an entity from a persistent back-end and this call is in response to creating a new
    //  instance so make that call.
    entityToCreate.createNew();
  }

  private void performSync(ServerEntityRequest wrappedRequest, int concurrencyKey) {
    if (this.isInActiveState) {
      if (null == this.activeServerEntity) {
        throw new IllegalStateException("Actions on a non-existent entity.");
      } else {
        // Create the channel which will send the payloads over the wire.
        PassiveSynchronizationChannel syncChannel = new PassiveSynchronizationChannel() {
          @Override
//  TODO:  what should be done about exception handling?
          public void synchronizeToPassive(byte[] payload) {
            Future<Void> wait = executor.scheduleSync(PassiveSyncMessage.createPayloadMessage(id, version, concurrencyKey, payload), wrappedRequest.getNodeID());
            try {
              wait.get();
            } catch (ExecutionException ee) {
            // TODO: do something reasoned here
              throw new RuntimeException(ee);
            } catch (InterruptedException ie) {
            // TODO: do something reasoned here
              Thread.currentThread().interrupt();
              throw new RuntimeException(ie);
            }
          }};
//  start is handled by the sync request that triggered this action
        this.activeServerEntity.synchronizeKeyToPassive(syncChannel, concurrencyKey);
        executor.scheduleSync(PassiveSyncMessage.createEndEntityKeyMessage(id, version, concurrencyKey), wrappedRequest.getNodeID());
        wrappedRequest.complete();
      }
    } else {
      if (null == this.passiveServerEntity) {
        throw new IllegalStateException("Actions on a non-existent entity.");
      } else {
//  doing nothing for sync
      }
    }
  }
  
  private void performAction(ServerEntityRequest wrappedRequest, EntityMessage message) {
    if (this.isInActiveState) {
      if (null == this.activeServerEntity) {
        throw new IllegalStateException("Actions on a non-existent entity.");
      } else {
        MessageCodec<EntityMessage, EntityResponse> codec = this.activeServerEntity.getMessageCodec();
        byte[] er = codec.serialize(this.activeServerEntity.invoke(wrappedRequest.getSourceDescriptor(), message));
        wrappedRequest.complete(er);
      }
    } else {
      if (null == this.passiveServerEntity) {
        throw new IllegalStateException("Actions on a non-existent entity.");
      } else {
        this.passiveServerEntity.invoke(message);
        wrappedRequest.complete();
      }
    }
  }

  private void getEntity(ServerEntityRequest getEntityRequest) {
    if (this.isInActiveState) {
      if (null != this.activeServerEntity) {
        ClientDescriptor sourceDescriptor = getEntityRequest.getSourceDescriptor();
        EntityDescriptor entityDescriptor = getEntityDescriptorForSource(sourceDescriptor);
        // The FETCH can only come directly from a client so we can down-cast.
        ClientID clientID = (ClientID) getEntityRequest.getNodeID();
        clientEntityStateManager.addReference(clientID, entityDescriptor);
        this.activeServerEntity.connected(sourceDescriptor);
        getEntityRequest.complete(this.activeServerEntity.getConfig());
      } else {
        getEntityRequest.complete();
      }
    } else {
      throw new IllegalStateException("GET called on passive entity.");
    }
  }

  private void releaseEntity(ServerEntityRequest request) {
    if (this.isInActiveState) {
      if (null != this.activeServerEntity) {
        ClientDescriptor sourceDescriptor = request.getSourceDescriptor();
        EntityDescriptor entityDescriptor = getEntityDescriptorForSource(sourceDescriptor);
        // The RELEASE can only come directly from a client so we can down-cast.
        ClientID clientID = (ClientID) request.getNodeID();
        clientEntityStateManager.removeReference(clientID, entityDescriptor);
        this.activeServerEntity.disconnected(sourceDescriptor);
      }
      request.complete();
    } else {
      throw new IllegalStateException("RELEASE called on passive entity.");
    }
  }
  
  private EntityDescriptor getEntityDescriptorForSource(ClientDescriptor sourceDescriptor) {
    // We are in internal code so downcast the descriptor.
    ClientDescriptorImpl rawDescriptor = (ClientDescriptorImpl)sourceDescriptor;
    return rawDescriptor.getEntityDescriptor();
  }

  private void promoteEntity(ServerEntityRequest request) {
    // Can't enter active state twice.
    Assert.assertFalse(this.isInActiveState);
    Assert.assertNull(this.activeServerEntity);
    
    this.isInActiveState = true;
    if (null != this.passiveServerEntity) {
      this.activeServerEntity = factory.createActiveEntity(this.registry, constructorInfo);
      this.activeServerEntity.loadExisting();
      this.passiveServerEntity = null;
    } else {
      throw new IllegalStateException("no entity to promote");
    }
    request.complete();
  }

  @Override
  public void sync(NodeID passive) {
    executor.scheduleSync(PassiveSyncMessage.createStartEntityMessage(id, version, constructorInfo), passive);
// iterate through all the concurrency keys of an entity
    for (Integer concurrency : this.activeServerEntity.getConcurrencyStrategy().getKeysForSynchronization()) {
      PassiveSyncServerEntityRequest req = new PassiveSyncServerEntityRequest(id, version, passive);
      // We don't actually use the message in the direct strategy so this is safe.
      EntityDescriptor entityDescriptor = new EntityDescriptor(this.id, ClientInstanceID.NULL_ID, this.version);
      executor.scheduleRequest(entityDescriptor, req, null, () -> invoke(req, null, concurrency), concurrency);
      req.waitFor();
    }
//  end passive sync for an entity
    executor.scheduleSync(PassiveSyncMessage.createEndEntityMessage(id, version), passive);
  }

  private void loadExisting(ServerEntityRequest loadEntityRequest, byte[] constructorInfo) {
    this.constructorInfo = constructorInfo;
    CommonServerEntity<EntityMessage, EntityResponse> entityToLoad = null;
    // Create the appropriate kind of entity, based on our active/passive state.
    if (this.isInActiveState) {
      if (null != this.activeServerEntity) {
        throw new IllegalStateException("Active entity " + id + " already exists.");
      } else {
        this.activeServerEntity = factory.createActiveEntity(registry, constructorInfo);
        entityToLoad = this.activeServerEntity;
      }
    } else {
      if (null != this.passiveServerEntity) {
        throw new IllegalStateException("Passive entity " + id + " already exists.");
      } else {
        this.passiveServerEntity = factory.createPassiveEntity(registry, constructorInfo);
        // Store the configuration in case we promote.
        entityToLoad = this.passiveServerEntity;
      }
    }
    loadEntityRequest.complete();
    entityToLoad.loadExisting();
  }

  private static class PassiveSyncServerEntityRequest extends AbstractServerEntityRequest {
    
    public PassiveSyncServerEntityRequest(EntityID eid, long version, NodeID passive) {
      super(new EntityDescriptor(eid,ClientInstanceID.NULL_ID,version), ServerEntityAction.REQUEST_SYNC_ENTITY, null, null, passive, false);
    }

    @Override
    public boolean requiresReplication() {
      return true;
    }

    @Override
    public Optional<MessageChannel> getReturnChannel() {
      return Optional.empty();
    }

    @Override
    public ClientDescriptor getSourceDescriptor() {
      return null;
    }
    
    public synchronized void waitFor() {
      try {
        while (!isDone()) {
          this.wait();
        }
      } catch (InterruptedException ie) {
        //  TODO
        throw new RuntimeException(ie);
      }
    }

    @Override
    public synchronized void complete() {
      this.notifyAll();
      super.complete();
    }
    }
  }
