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
import org.terracotta.entity.MessageCodecException;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.PassiveSynchronizationChannel;
import org.terracotta.entity.ServerEntityService;
import org.terracotta.entity.SyncMessageCodec;

import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.objectserver.core.api.ITopologyEventCollector;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.services.InternalServiceRegistry;
import com.tc.util.Assert;
import static com.tc.util.Assert.assertNotNull;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
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
  private final InternalServiceRegistry registry;
  private final ClientEntityStateManager clientEntityStateManager;
  private final ITopologyEventCollector eventCollector;
  private final ServerEntityService<EntityMessage, EntityResponse> factory;
  // PTH sink so things can be injected into the stream
  private final BiConsumer<EntityID, Long> noopLoopback;
  // isInActiveState defines which entity type to check/create - we need the flag to represent the pre-create state.
  private boolean isInActiveState;
  private volatile boolean isDestroyed;
  
  private final MessageCodec<EntityMessage, EntityResponse> codec;
  private final SyncMessageCodec<EntityMessage> syncCodec;
  private volatile ActiveServerEntity<EntityMessage, EntityResponse> activeServerEntity;
  private volatile ConcurrencyStrategy<EntityMessage> concurrencyStrategy;

  private final DefermentQueue<SchedulingRunnable> runnables = new DefermentQueue<>(TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.ENTITY_DEFERMENT_QUEUE_SIZE, 1024));

  private volatile PassiveServerEntity<EntityMessage, EntityResponse> passiveServerEntity;
  //  reconnect access has to be exclusive.  it is out-of-band from normal invoke access
  private final ReadWriteLock reconnectAccessLock = new ReentrantReadWriteLock();
  // NOTE:  This may be removed in the future if we change how we access the config from the ServerEntityService but
  //  it presently holds the config we used when we first created passiveServerEntity (if it isn't null).  It is used
  //  when we promote to an active.
  private byte[] constructorInfo;

  ManagedEntityImpl(EntityID id, long version, BiConsumer<EntityID, Long> loopback, InternalServiceRegistry registry, ClientEntityStateManager clientEntityStateManager, ITopologyEventCollector eventCollector,
                    RequestProcessor process, ServerEntityService<EntityMessage, EntityResponse> factory,
                    boolean isInActiveState) {
    this.id = id;
    this.version = version;
    this.noopLoopback = loopback;
    this.registry = registry;
    this.clientEntityStateManager = clientEntityStateManager;
    this.eventCollector = eventCollector;
    this.factory = factory;
    this.executor = process;
    this.isInActiveState = isInActiveState;
    registry.setOwningEntity(this);
    this.codec = factory.getMessageCodec();
    this.syncCodec = factory.getSyncMessageCodec();
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
  public void addInvokeRequest(final ServerEntityRequest request, byte[] payload, int defaultKey) {
    if (request.getAction() == ServerEntityAction.NOOP) {
      scheduleInOrder(getEntityDescriptorForSource(request.getSourceDescriptor()), request, payload, () -> {
        request.complete();
        if (this.isInActiveState) {
          request.retired();
        }
      }, ConcurrencyStrategy.UNIVERSAL_KEY);
      return;
    }
    Assert.assertTrue(request.getAction() == ServerEntityAction.INVOKE_ACTION);
      // Invoke and payload requests need to wait for the entity creation so that they can request the concurrency strategy.
    if ((this.activeServerEntity == null) && (this.passiveServerEntity == null)) {
      request.failure(new EntityNotFoundException(this.getID().getClassName(), this.getID().getEntityName()));
      if (this.isInActiveState) {
        request.retired();
      }
    } else {
      // We only decode messages for INVOKE and RECEIVE_SYNC_PAYLOAD requests.
      CommonServerEntity<EntityMessage,EntityResponse> entity = (null != this.activeServerEntity) ? this.activeServerEntity : this.passiveServerEntity;
      Assert.assertNotNull(entity);
      Assert.assertNotNull(payload);
    
      EntityMessage message = null;
      try {
        message = runWithHelper(()->codec.decodeMessage(payload));
      } catch (EntityUserException e) {
        throw new RuntimeException(e);
      }
      // If we are still ok and managed to deserialize the message, continue.
      if (null != message) {
        // Since concurrency key is pulled out in different ways for these different message types, we will do that here.
        // on actives, the concurrency strategy is available on actives and null on passives.
        // see concurrencyStrategy assignment further up this method.  No concurrency strategy means use the default
        // key which on passives is the key used to run the action on the active.
        final int concurrencyKey = ((null != concurrencyStrategy)) ?
          concurrencyStrategy.concurrencyKey(message) : defaultKey;
        final EntityMessage safeMessage = message;
        processInvokeRequest(request, payload, safeMessage, concurrencyKey);
      } else {
        throw new RuntimeException("entity deserializer returned null while processing invoke request");
      }
    }
  }
  
  private void scheduleInOrder(EntityDescriptor desc, ServerEntityRequest request, byte[] payload, Runnable r, int ckey) {
// this all makes sense because this is only called by the PTH single thread
// deferCleared is cleared by one of the request queues
    if (isInActiveState) {
      Assert.assertTrue(Thread.currentThread().getName().contains(ServerConfigurationContext.VOLTRON_MESSAGE_STAGE));
    } else {
      Assert.assertTrue(Thread.currentThread().getName().contains(ServerConfigurationContext.PASSIVE_REPLICATION_STAGE));
    }
    
    SchedulingRunnable next = new SchedulingRunnable(desc, request, payload, r, ckey);
    
    for (SchedulingRunnable msg : runnables) {
      msg.start();
    }

    if (!runnables.offer(next)) {
      next.start();
    }
  }
  
  private void processInvokeRequest(final ServerEntityRequest request, byte[] payloadForReplicate, EntityMessage message, int key) {
    ClientDescriptor client = request.getSourceDescriptor();
    scheduleInOrder(getEntityDescriptorForSource(client), request, payloadForReplicate, ()->invoke(request, message, key), key);
  }

  @Override
  public void addSyncRequest(ServerEntityRequest sync, byte[] payload, int concurrencyKey) {
    processSyncMessage(sync, payload, concurrencyKey);
  }
    
  private void processSyncMessage(ServerEntityRequest sync, byte[] payload, int concurrencyKey) {
    if (sync.getAction() == ServerEntityAction.RECEIVE_SYNC_ENTITY_START || 
        sync.getAction() == ServerEntityAction.RECEIVE_SYNC_ENTITY_END) {
      scheduleInOrder(getEntityDescriptorForSource(sync.getSourceDescriptor()), sync, payload, ()-> invokeLifecycleOperation(sync, payload), ConcurrencyStrategy.MANAGEMENT_KEY);
    } else if (sync.getAction() == ServerEntityAction.RECEIVE_SYNC_PAYLOAD) {
      scheduleInOrder(getEntityDescriptorForSource(sync.getSourceDescriptor()), sync, payload, ()->{
        EntityMessage message = null;
        try {
          message = runWithHelper(()->syncCodec.decode(concurrencyKey, payload));
        } catch (EntityUserException e) {
          throw new RuntimeException(e);
        }
        // If we are still ok and managed to deserialize the message, continue.
        if (null != message) {
          invoke(sync, message, concurrencyKey);
        } else {
          throw new RuntimeException("entity deserializer returned null while processing sync message");
        }
      }, concurrencyKey);
    } else {
      scheduleInOrder(getEntityDescriptorForSource(sync.getSourceDescriptor()), sync, payload, ()->invoke(sync, null, concurrencyKey), concurrencyKey);
    }
  }

  private static interface CodecHelper<R> {
    public R run() throws MessageCodecException;
  }
  private <R> R runWithHelper(CodecHelper<R> helper) throws EntityUserException {
    R message = null;
    try {
      message = helper.run();
    } catch (MessageCodecException deserializationException) {
      throw new EntityUserException(this.getID().getClassName(), this.getID().getEntityName(), deserializationException);
    } catch (RuntimeException e) {
      // We first want to wrap this in a codec exception to convey the meaning of where this happened.
      MessageCodecException deserializationException = new MessageCodecException("Runtime exception in deserializer", e);
      throw new EntityUserException(this.getID().getClassName(), this.getID().getEntityName(), deserializationException);
    }
    return message;
  }

  @Override
  public void addLifecycleRequest(ServerEntityRequest create, byte[] data) {
    scheduleInOrder(getEntityDescriptorForSource(create.getSourceDescriptor()), create, data, ()-> invokeLifecycleOperation(create, data), ConcurrencyStrategy.MANAGEMENT_KEY);
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
    // Fire the event that the client fetched the entity.
    this.eventCollector.clientDidFetchEntity(clientID, this.getID(), clientDescriptor);
  }
  
  private void invokeLifecycleOperation(ServerEntityRequest request, byte[] payload) {
    Lock read = reconnectAccessLock.readLock();
      if (logger.isDebugEnabled()) {
        logger.debug("Invoking lifecycle " + request.getAction() + " on " + getID());
      }
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
          case RECONFIGURE_ENTITY:
            reconfigureEntity(request, payload);
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
          default:
            throw new IllegalArgumentException("Unknown request " + request);
        }
      } catch (Exception e) {
        // Wrap this exception.
        EntityUserException wrapper = new EntityUserException(id.getClassName(), id.getEntityName(), e);
        logger.error("caught exception during invoke ", wrapper);
        throw new RuntimeException(wrapper);
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
            performSync(request, request.replicateTo(executor.passives()), concurrencyKey);
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
          case NOOP:
            break;
          case DOES_EXIST:
            // Never occur on this level.
            throw new IllegalArgumentException("Unexpected request " + request);
          default:
            throw new IllegalArgumentException("Unknown request " + request);
        }
      } catch (Exception e) {
        // Wrap this exception.
        EntityUserException wrapper = new EntityUserException(id.getClassName(), id.getEntityName(), e);
        logger.error("caught exception during invoke ", wrapper);
        throw new RuntimeException(wrapper);
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
    // No retire on passive.
    Assert.assertFalse(this.isInActiveState);
  }

  private void receiveSyncEntityKeyStart(ServerEntityRequest request, int concurrencyKey) {
    // This only makes sense if we have a passive instance.
    Assert.assertNotNull(this.passiveServerEntity);
    this.passiveServerEntity.startSyncConcurrencyKey(concurrencyKey);
    request.complete();
    // No retire on passive.
    Assert.assertFalse(this.isInActiveState);
  }

  private void receiveSyncEntityKeyEnd(ServerEntityRequest request, int concurrencyKey) {
    // This only makes sense if we have a passive instance.
    Assert.assertNotNull(this.passiveServerEntity);
    this.passiveServerEntity.endSyncConcurrencyKey(concurrencyKey);
    request.complete();
    // No retire on passive.
    Assert.assertFalse(this.isInActiveState);
  }

  private void receiveSyncEntityPayload(ServerEntityRequest request, EntityMessage message) {
    // This only makes sense if we have a passive instance.
    Assert.assertNotNull(this.passiveServerEntity);
    this.passiveServerEntity.invoke(message);
    request.complete();
    // No retire on passive.
    Assert.assertFalse(this.isInActiveState);
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
    if (this.isInActiveState) {
      request.retired();
    }
    // Fire the event that the entity was destroyed.
    this.eventCollector.entityWasDestroyed(this.getID());
    this.isDestroyed = true;
  }

  private void reconfigureEntity(ServerEntityRequest reconfigureEntityRequest, byte[] constructorInfo) {
    byte[] oldconfig = this.constructorInfo;
    if (this.activeServerEntity == null && this.passiveServerEntity == null) {
      reconfigureEntityRequest.failure(new EntityAlreadyExistsException(this.getID().getClassName(), this.getID().getEntityName()));
      if (this.isInActiveState) {
        reconfigureEntityRequest.retired();
      }
      return;
    }
    this.constructorInfo = constructorInfo;
    CommonServerEntity<EntityMessage, EntityResponse> entityToCreate = null;
    // Create the appropriate kind of entity, based on our active/passive state.
    if (this.isInActiveState) {
      if (null == this.activeServerEntity) {
        throw new IllegalStateException("Active entity " + id + " does not exists.");
      } else {
        this.activeServerEntity = this.factory.createActiveEntity(this.registry, constructorInfo);
        this.concurrencyStrategy = this.factory.getConcurrencyStrategy(constructorInfo);
        entityToCreate = this.activeServerEntity;
      }
    } else {
      if (null == this.passiveServerEntity) {
        throw new IllegalStateException("Passive entity " + id + " does not exists.");
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("reconfigure passive entity " + this.getID() + " due to " + reconfigureEntityRequest.getAction());
        }
        this.passiveServerEntity = this.factory.createPassiveEntity(this.registry, this.constructorInfo);
        Assert.assertNull(this.concurrencyStrategy);
        // Store the configuration in case we promote.
        entityToCreate = this.passiveServerEntity;
      }
    }
    reconfigureEntityRequest.complete(oldconfig);
    if (this.isInActiveState) {
      reconfigureEntityRequest.retired();
    }
    // We currently don't support loading an entity from a persistent back-end and this call is in response to creating a new
    //  instance so make that call.
    entityToCreate.loadExisting();
    // Fire the event that the entity was created.
    this.eventCollector.entityWasReloaded(this.getID(), this.isInActiveState);
  }
  
  private void createEntity(ServerEntityRequest createEntityRequest, byte[] constructorInfo) {
    if (this.activeServerEntity != null || this.passiveServerEntity != null) {
      createEntityRequest.failure(new EntityAlreadyExistsException(this.getID().getClassName(), this.getID().getEntityName()));
      if (this.isInActiveState) {
        createEntityRequest.retired();
      }
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
        this.concurrencyStrategy = this.factory.getConcurrencyStrategy(constructorInfo);
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
        Assert.assertNull(this.concurrencyStrategy);
        // Store the configuration in case we promote.
        entityToCreate = this.passiveServerEntity;
      }
    }
    createEntityRequest.complete();
    if (this.isInActiveState) {
      createEntityRequest.retired();
    }
    // We currently don't support loading an entity from a persistent back-end and this call is in response to creating a new
    //  instance so make that call.
    entityToCreate.createNew();
    // Fire the event that the entity was created.
    this.eventCollector.entityWasCreated(this.getID(), this.isInActiveState);
  }

  private void performSync(ServerEntityRequest wrappedRequest, Set<NodeID> passives, int concurrencyKey) {
    if (this.isInActiveState) {
      if (null == this.activeServerEntity) {
        throw new IllegalStateException("Actions on a non-existent entity.");
      } else {
        // Create the channel which will send the payloads over the wire.
        PassiveSynchronizationChannel<EntityMessage> syncChannel = new PassiveSynchronizationChannel<EntityMessage>() {
          @Override
//  TODO:  what should be done about exception handling?
          public void synchronizeToPassive(EntityMessage payload) {
            for (NodeID passive : passives) {
              try {
                byte[] message = runWithHelper(()->syncCodec.encode(concurrencyKey, payload));
                executor.scheduleSync(PassiveSyncMessage.createPayloadMessage(id, version, concurrencyKey, message), passive).get();
              } catch (EntityUserException eu) {
              // TODO: do something reasoned here
                throw new RuntimeException(eu);
              } catch (ExecutionException ee) {
              // TODO: do something reasoned here
                throw new RuntimeException(ee);
              } catch (InterruptedException ie) {
              // TODO: do something reasoned here
                Thread.currentThread().interrupt();
                throw new RuntimeException(ie);
              }
            }
          }};
//  start is handled by the sync request that triggered this action
        this.activeServerEntity.synchronizeKeyToPassive(syncChannel, concurrencyKey);
        for (NodeID passive : passives) {
          try {
            executor.scheduleSync(PassiveSyncMessage.createEndEntityKeyMessage(id, version, concurrencyKey), passive).get();
          } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
        wrappedRequest.complete();
        if (this.isInActiveState) {
          wrappedRequest.retired();
        }
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
        try {
          byte[] er = runWithHelper(()->codec.encodeResponse(this.activeServerEntity.invoke(wrappedRequest.getSourceDescriptor(), message)));
          wrappedRequest.complete(er);
          wrappedRequest.retired();
        } catch (EntityUserException e) {
          wrappedRequest.failure(e);
          wrappedRequest.retired();
          throw new RuntimeException(e);
        }
      }
    } else {
      if (null == this.passiveServerEntity) {
        throw new IllegalStateException("Actions on a non-existent entity.");
      } else {
        this.passiveServerEntity.invoke(message);
        wrappedRequest.complete();
        // No retire on passive.
        Assert.assertFalse(this.isInActiveState);
      }
    }
  }
  
  @Override
  public MessageCodec<?, ?> getCodec() {
    return this.codec;
  }

  @Override
  public void loadEntity(byte[] configuration) {
    this.loadExisting(configuration);
  }

  private void getEntity(ServerEntityRequest getEntityRequest) {
    if (this.isInActiveState) {
      if (null != this.activeServerEntity) {
        ClientDescriptor sourceDescriptor = getEntityRequest.getSourceDescriptor();
        EntityDescriptor entityDescriptor = getEntityDescriptorForSource(sourceDescriptor);
        // The FETCH can only come directly from a client so we can down-cast.
        ClientID clientID = (ClientID) getEntityRequest.getNodeID();
        clientEntityStateManager.addReference(clientID, entityDescriptor);
        getEntityRequest.complete(this.constructorInfo);
        getEntityRequest.retired();
        // Fire the event that the client fetched the entity.
        this.eventCollector.clientDidFetchEntity(clientID, this.getID(), sourceDescriptor);
        // finally notify the entity that it was fetched
        this.activeServerEntity.connected(sourceDescriptor);
      } else {
        getEntityRequest.complete();
        getEntityRequest.retired();
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
        // Fire the event that the client released the entity.
        this.eventCollector.clientDidReleaseEntity(clientID, this.getID());
      }
      request.complete();
      request.retired();
    } else {
      throw new IllegalStateException("RELEASE called on passive entity.");
    }
  }
  
  private EntityDescriptor getEntityDescriptorForSource(ClientDescriptor sourceDescriptor) {
    // We are in internal code so downcast the descriptor.
    ClientDescriptorImpl rawDescriptor = (ClientDescriptorImpl)sourceDescriptor;
    return rawDescriptor.getEntityDescriptor();
  }
  
  @Override
  public void promoteEntity() {
    // Can't enter active state twice.
    Assert.assertFalse(this.isInActiveState);
    Assert.assertNull(this.activeServerEntity);
    
    this.isInActiveState = true;
    if (null != this.passiveServerEntity) {
      this.activeServerEntity = factory.createActiveEntity(this.registry, this.constructorInfo);
      this.concurrencyStrategy = factory.getConcurrencyStrategy(this.constructorInfo);
      this.activeServerEntity.loadExisting();
      this.passiveServerEntity = null;
      // Fire the event that the entity was reloaded.
      this.eventCollector.entityWasReloaded(this.getID(), true);
    } else {
      throw new IllegalStateException("no entity to promote");
    }
  }

  private void promoteEntity(ServerEntityRequest request) {
    promoteEntity();
    request.complete();
    // Even though this moves us to the active state, the request is purely internal so there is no retire.
  }

  @Override
  public void sync(NodeID passive) {
    if (!this.isDestroyed) {
      try {
    // wait for future is ok, occuring on sync executor thread
        executor.scheduleSync(PassiveSyncMessage.createStartEntityMessage(id, version, constructorInfo), passive).get();
    // iterate through all the concurrency keys of an entity
        EntityDescriptor entityDescriptor = new EntityDescriptor(this.id, ClientInstanceID.NULL_ID, this.version);
    //  this is simply a barrier to make sure all actions are flushed before sync is started (hence, it has a null passive).
        PassiveSyncServerEntityRequest barrier = new PassiveSyncServerEntityRequest(null);
    // wait for future is ok, occuring on sync executor thread
        executor.scheduleRequest(entityDescriptor, barrier, new byte[0], ()-> { 
          assertNotNull(this.activeServerEntity);
          assertNotNull(concurrencyStrategy);
          barrier.complete(); 
          barrier.retired();
        }, ConcurrencyStrategy.MANAGEMENT_KEY).get();

        for (Integer concurrency : concurrencyStrategy.getKeysForSynchronization()) {
          PassiveSyncServerEntityRequest req = new PassiveSyncServerEntityRequest(passive);
          // We don't actually use the message in the direct strategy so this is safe.
          executor.scheduleRequest(entityDescriptor, req, null, () -> invoke(req, null, concurrency), concurrency).get();
        }
    //  end passive sync for an entity
    // wait for future is ok, occuring on sync executor thread
        executor.scheduleSync(PassiveSyncMessage.createEndEntityMessage(id, version), passive).get();
      } catch (ExecutionException | InterruptedException e) {
        throw new AssertionError("sync failed", e);
      }
    }
  }  

  private void loadExisting(byte[] constructorInfo) {
    this.constructorInfo = constructorInfo;
    CommonServerEntity<EntityMessage, EntityResponse> entityToLoad = null;
    // Create the appropriate kind of entity, based on our active/passive state.
    if (this.isInActiveState) {
      if (null != this.activeServerEntity) {
        throw new IllegalStateException("Active entity " + id + " already exists.");
      } else {
        this.activeServerEntity = factory.createActiveEntity(registry, constructorInfo);
        this.concurrencyStrategy = factory.getConcurrencyStrategy(constructorInfo);
        entityToLoad = this.activeServerEntity;
      }
    } else {
      if (null != this.passiveServerEntity) {
        throw new IllegalStateException("Passive entity " + id + " already exists.");
      } else {
        this.passiveServerEntity = factory.createPassiveEntity(registry, constructorInfo);
        Assert.assertNull(this.concurrencyStrategy);
        // Store the configuration in case we promote.
        entityToLoad = this.passiveServerEntity;
      }
    }
    entityToLoad.loadExisting();
    // Fire the event that the entity was reloaded.
    this.eventCollector.entityWasReloaded(this.getID(), this.isInActiveState);
  }

  private static class PassiveSyncServerEntityRequest extends AbstractServerEntityRequest {
    private final NodeID passive;
    
    public PassiveSyncServerEntityRequest(NodeID passive) {
      super(ServerEntityAction.REQUEST_SYNC_ENTITY, null, null, ClientID.NULL_ID, false);
      this.passive = passive;
    }

    @Override
    public ClientID getNodeID() {
      return super.getNodeID();
    }

    @Override
    public Set<NodeID> replicateTo(Set<NodeID> passives) {
      return passive == null ? Collections.emptySet() : Collections.singleton(passive);
    }

    @Override
    public Optional<MessageChannel> getReturnChannel() {
      return Optional.empty();
    }

    @Override
    public ClientDescriptor getSourceDescriptor() {
      return null;
    }
  }
  
  private class SchedulingRunnable implements Runnable {
    private final EntityDescriptor desc;
    private final ServerEntityRequest request;
    private final byte[] payload;
    private final Runnable original;
    private final int concurrency;

    public SchedulingRunnable(EntityDescriptor desc, ServerEntityRequest request, byte[] payload, Runnable r, int concurrency) {
      this.desc = desc;
      this.request = request;
      this.payload = payload;
      this.original = r;
      this.concurrency = concurrency;
    }
        
    private void start() {
      if (concurrency == ConcurrencyStrategy.MANAGEMENT_KEY) {
        runnables.activate();
      }
      executor.scheduleRequest(desc, request, payload, this, concurrency);
    }
    
    public void run() {
      try {
        original.run();
      } finally {
        this.end();
      }
    }
    
    private void end() {
      if (concurrency == ConcurrencyStrategy.MANAGEMENT_KEY) {
        runnables.clear(); 
  //  there may be no more incoming messages on this entity to clear the 
  //  queue so if it is not empty, push a noop.  
        noopLoopback.accept(id, version);
      }
    }
  };
  
  private static class DefermentQueue<T> implements Iterable<T> {
    private final LinkedList<T> queue = new LinkedList<>();
    private final int limit;
    private volatile boolean deferCleared = true;

    public DefermentQueue(int limit) {
      this.limit = limit;
    }

    T checkDeferred() {
      if (deferCleared && !queue.isEmpty()) {
        return queue.pop();
      }
      return null;
    }
    
    boolean activate() {
      logger.debug("activated from " + Thread.currentThread().getName());
      try {
        return deferCleared;
      } finally {
        deferCleared = false;
      }
    }
    
    synchronized boolean clear() {
     logger.debug("cleared from " + Thread.currentThread().getName() + " with " + queue.size());
     try {
        notify();
        return deferCleared;
      } finally {
        deferCleared = true;
      }
    }
    
    boolean offer(T msg) {
      if (!deferCleared || !queue.isEmpty()) {
        queue.add(msg);
        if (queue.size() == limit) {
          pause();
        }
        return true;
      } else {
        return false;
      }
    }

    @Override
    public Iterator<T> iterator() {
      return new Iterator<T>() {
        T msg;
        
        @Override
        public boolean hasNext() {
          msg = checkDeferred();
          return msg != null;
        }

        @Override
        public T next() {
          return msg;
        }
      };
    }

    private synchronized void pause() {
      boolean interrupted = false;
      while (!deferCleared) {
        try {
          this.wait();
        } catch (InterruptedException ie) {
          interrupted = true;
        }
      }
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
