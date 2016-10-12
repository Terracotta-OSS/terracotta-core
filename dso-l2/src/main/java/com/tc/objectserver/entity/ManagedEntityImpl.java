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

import com.tc.exception.EntityReferencedException;
import com.tc.l2.msg.PassiveSyncMessage;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.CommonServerEntity;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.PassiveSynchronizationChannel;
import org.terracotta.entity.EntityServerService;
import org.terracotta.entity.StateDumpable;
import org.terracotta.entity.StateDumper;
import org.terracotta.entity.SyncMessageCodec;

import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.objectserver.core.api.ITopologyEventCollector;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.handler.RetirementManager;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.services.InternalServiceRegistry;
import com.tc.util.Assert;
import com.tc.util.concurrent.FlightControl;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;
import org.terracotta.exception.EntityAlreadyExistsException;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityUserException;
import java.util.function.Consumer;
import java.util.concurrent.TimeUnit;
import org.terracotta.exception.PermanentEntityException;


public class ManagedEntityImpl implements ManagedEntity {
  private static final TCLogger logger   = TCLogging.getLogger(ManagedEntityImpl.class);

  private final RequestProcessor executor;
  private final RetirementManager retirementManager;

  private final EntityID id;
  private final long version;
  private final long consumerID;
  private final InternalServiceRegistry registry;
  private final ClientEntityStateManager clientEntityStateManager;
  private final ITopologyEventCollector eventCollector;
  private final EntityServerService<EntityMessage, EntityResponse> factory;
  // PTH sink so things can be injected into the stream
  private final BiConsumer<EntityID, Long> noopLoopback;
  // isInActiveState defines which entity type to check/create - we need the flag to represent the pre-create state.
  private boolean isInActiveState;
  private final boolean canDelete;
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
  private final FlightControl syncingThreads = new FlightControl();
  private final FlightControl reconfiguringInflight = new FlightControl();
  // NOTE:  This may be removed in the future if we change how we access the config from the ServerEntityService but
  //  it presently holds the config we used when we first created passiveServerEntity (if it isn't null).  It is used
  //  when we promote to an active.
  private byte[] constructorInfo;

  ManagedEntityImpl(EntityID id, long version, long consumerID, BiConsumer<EntityID, Long> loopback, InternalServiceRegistry registry, ClientEntityStateManager clientEntityStateManager, ITopologyEventCollector eventCollector,
                    RequestProcessor process, EntityServerService<EntityMessage, EntityResponse> factory,
                    boolean isInActiveState, boolean canDelete) {
    this.id = id;
    this.isDestroyed = true;
    this.version = version;
    this.consumerID = consumerID;
    this.noopLoopback = loopback;
    this.registry = registry;
    this.clientEntityStateManager = clientEntityStateManager;
    this.eventCollector = eventCollector;
    this.factory = factory;
    this.executor = process;
    // Create the RetirementManager here, since it is currently scoped per-entity.
    this.retirementManager = new RetirementManager();
    this.isInActiveState = isInActiveState;
    this.canDelete = canDelete;
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
    
  private ResultCapture createManagedEntityResponse(Consumer<byte[]> completion, Consumer<EntityException> exception) {
    return new ResultCapture(completion, exception);
  }
/**
 * This is the main entry point for interaction with the managed entity.  A request consists of a defining action, a 
 * payload and 2 consumers to consume the result of the interaction.  Only one of the consumers will be called as a result 
 * of the action.  In the case of a successful interaction, the first consumer is passed the raw byte[] result of the interaction.
 * On failure, the EntityException is passed to the second consumer is passed the exception.  
 * @param request - defines the type of action requested and who requested it
 * @param data - The entity defined data to accompany the request
 * @param completion - called on successful completion of an action
 * @param exception - called when an exception occurs
 * @return a completion object which can be invoked to wait for the requested action to complete
 */
  @Override
  public SimpleCompletion addRequestMessage(ServerEntityRequest request, MessagePayload data, Consumer<byte[]> completion, Consumer<EntityException> exception) {
    ResultCapture resp = createManagedEntityResponse(completion, exception);
    switch (request.getAction()) {
      case NOOP:
        processNoopMessage(request, resp);
        break;
      case CREATE_ENTITY:
      case DESTROY_ENTITY:
      case FETCH_ENTITY:
      case RECONFIGURE_ENTITY:
      case RELEASE_ENTITY:
        processLifecycleEntity(request, data, resp);
        break;
      case INVOKE_ACTION:
        processInvokeRequest(request, resp, data, data.getConcurrency());
        break;
      case RECEIVE_SYNC_ENTITY_START:
      case RECEIVE_SYNC_ENTITY_END:
      case RECEIVE_SYNC_ENTITY_KEY_START:
      case RECEIVE_SYNC_ENTITY_KEY_END:
      case RECEIVE_SYNC_PAYLOAD:
        Assert.assertTrue(!this.isInActiveState);
        processSyncMessage(request, resp, data, data.getConcurrency());
        break;
      default:
        throw new IllegalArgumentException("Unknown request " + request);
    }
    return resp;
  }
  
  private void processLifecycleEntity(ServerEntityRequest create, MessagePayload data, ResultCapture resp) {
    if (this.isInActiveState && create.getAction() == ServerEntityAction.RECONFIGURE_ENTITY) {
//  this is the process transaction handler thread adding a reconfigure to the message flow of this entity.  
//  before it can proceed, need to make sure any syncs are completed so the concurrency strategy is not changed out from 
//  under it.
      waitForSyncToFinish();
    }
    scheduleInOrder(getEntityDescriptorForSource(create.getSourceDescriptor()), create, resp, data , ()-> {
      invokeLifecycleOperation(create, data, resp);
    }, ConcurrencyStrategy.MANAGEMENT_KEY);
  }
  
  private void processNoopMessage(ServerEntityRequest request, ResultCapture resp) {
    Assert.assertTrue(request.getAction() == ServerEntityAction.NOOP);
    scheduleInOrder(getEntityDescriptorForSource(request.getSourceDescriptor()), request, resp, MessagePayload.EMPTY, resp::complete, ConcurrencyStrategy.UNIVERSAL_KEY);
  }
  
  private SchedulingRunnable scheduleInOrder(EntityDescriptor desc, ServerEntityRequest request, ResultCapture results, MessagePayload payload, Runnable r, int ckey) {
// this all makes sense because this is only called by the PTH single thread
// deferCleared is cleared by one of the request queues
    if (isInActiveState) {
      Assert.assertTrue(Thread.currentThread().getName().contains(ServerConfigurationContext.VOLTRON_MESSAGE_STAGE));
    } else {
      Assert.assertTrue(Thread.currentThread().getName().contains(ServerConfigurationContext.PASSIVE_REPLICATION_STAGE));
    }
    
    SchedulingRunnable next = new SchedulingRunnable(desc, request, payload, r, ckey);
    results.setWaitFor(next);
    
    for (SchedulingRunnable msg : runnables) {
      msg.start();
    }

    if (!runnables.offer(next)) {
      next.start();
    }
    
    return next;
  }
  
  private void processInvokeRequest(final ServerEntityRequest request, ResultCapture response, MessagePayload message, int key) {
    ClientDescriptor client = request.getSourceDescriptor();
    if (isInActiveState) {
      key = this.concurrencyStrategy.concurrencyKey(message.getEntityMessage());
    }
    int locked = key;
    scheduleInOrder(getEntityDescriptorForSource(client), request, response, message, ()->invoke(request, response, message, locked), locked);
  }

  private void processSyncMessage(ServerEntityRequest sync, ResultCapture response, MessagePayload syncPayload, int concurrencyKey) {
    if (sync.getAction() == ServerEntityAction.RECEIVE_SYNC_ENTITY_START || 
        sync.getAction() == ServerEntityAction.RECEIVE_SYNC_ENTITY_END) {
      scheduleInOrder(getEntityDescriptorForSource(sync.getSourceDescriptor()), sync, response, syncPayload, 
          ()-> {
            invokeLifecycleOperation(sync, syncPayload, response);
          }, 
        ConcurrencyStrategy.MANAGEMENT_KEY
      );
    } else if (sync.getAction() == ServerEntityAction.RECEIVE_SYNC_PAYLOAD) {
      scheduleInOrder(getEntityDescriptorForSource(sync.getSourceDescriptor()), sync, response, syncPayload, 
        ()-> {
          invoke(sync, response, syncPayload, concurrencyKey);
        }, concurrencyKey);
    } else {
      scheduleInOrder(getEntityDescriptorForSource(sync.getSourceDescriptor()), sync, response, syncPayload, 
        ()->{
          invoke(sync, response, null, concurrencyKey);
        }, concurrencyKey);
    }
  }

  @Override
  public void dumpStateTo(StateDumper stateDumper) {
    if(activeServerEntity != null) {
      // Entities can optionally implement StateDumpable, so we do a instanceof check before calling dump state method
      if(activeServerEntity instanceof StateDumpable) {
        ((StateDumpable) activeServerEntity).dumpStateTo(stateDumper);
      }
    }

    if(passiveServerEntity != null) {
      // Entities can optionally implement StateDumpable, so we do a instanceof check before calling dump state method
      if(passiveServerEntity instanceof StateDumpable) {
        ((StateDumpable) passiveServerEntity).dumpStateTo(stateDumper);
      }
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
  
  private void invokeLifecycleOperation(final ServerEntityRequest request, MessagePayload payload, ResultCapture resp) {
    Lock read = reconnectAccessLock.readLock();
    if (logger.isDebugEnabled()) {
      logger.debug("Client:" + request.getNodeID() + " Invoking lifecycle " + request.getAction() + " on " + getID());
    }
    read.lock();
    try {
      switch (request.getAction()) {
        case CREATE_ENTITY:
          createEntity(resp, payload.getRawPayload());
          break;
        case FETCH_ENTITY:
          getEntity(request, resp);
          break;
        case RELEASE_ENTITY:
          releaseEntity(request, resp);
          break;
        case RECONFIGURE_ENTITY:
          reconfigureEntity(resp, payload.getRawPayload());
          break;
        case DESTROY_ENTITY:
//  all request queues are flushed because this action is on the MGMT_KEY
          destroyEntity(request, resp);
          break;
        case RECEIVE_SYNC_ENTITY_START:
          receiveSyncEntityStart(resp, payload.getRawPayload());
          break;
        case RECEIVE_SYNC_ENTITY_END:
          receiveSyncEntityEnd(resp);
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
      if (request.getAction() == ServerEntityAction.RECONFIGURE_ENTITY) {
        reconfigureFinished();
      }
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
  private void invoke(ServerEntityRequest request, ResultCapture response, MessagePayload message, int concurrencyKey) {
    Lock read = reconnectAccessLock.readLock();
      try {
        read.lock();
        if (logger.isDebugEnabled()) {
          logger.debug("Invoking " + request.getAction() + " on " + getID() + "/" + concurrencyKey);
        }
        switch (request.getAction()) {
          case INVOKE_ACTION:
            performAction(request, response, message.getEntityMessage());
            break;
          case REQUEST_SYNC_ENTITY:
            performSync(response, request.replicateTo(executor.passives()), concurrencyKey);
            break;
          case RECEIVE_SYNC_ENTITY_KEY_START:
            receiveSyncEntityKeyStart(response, concurrencyKey);
            break;
          case RECEIVE_SYNC_ENTITY_KEY_END:
            receiveSyncEntityKeyEnd(response, concurrencyKey);
            break;
          case RECEIVE_SYNC_PAYLOAD:
            receiveSyncEntityPayload(response, message.getEntityMessage());
            break;
          case NOOP:
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
  
  private void receiveSyncEntityStart(ResultCapture response, byte[] constructor) {
    if (this.passiveServerEntity != null) {
      throw new AssertionError("not null " + this.getID());
    }
    Assert.assertNull(this.passiveServerEntity);
//  going to start by building the passive instance
    createEntity(response, constructor);
//  it better be a passive instance
    Assert.assertNotNull(this.passiveServerEntity);
    this.passiveServerEntity.startSyncEntity();
  }

  private void receiveSyncEntityEnd(ResultCapture response) {
    // This only makes sense if we have a passive instance.
    Assert.assertNotNull(this.passiveServerEntity);
    this.passiveServerEntity.endSyncEntity();
    response.complete();
    // No retire on passive.
    Assert.assertFalse(this.isInActiveState);
  }

  private void receiveSyncEntityKeyStart(ResultCapture response, int concurrencyKey) {
    // This only makes sense if we have a passive instance.
    Assert.assertNotNull(this.passiveServerEntity);
    this.passiveServerEntity.startSyncConcurrencyKey(concurrencyKey);
    response.complete();
    // No retire on passive.
    Assert.assertFalse(this.isInActiveState);
  }

  private void receiveSyncEntityKeyEnd(ResultCapture response, int concurrencyKey) {
    // This only makes sense if we have a passive instance.
    Assert.assertNotNull(this.passiveServerEntity);
    this.passiveServerEntity.endSyncConcurrencyKey(concurrencyKey);
    response.complete();
    // No retire on passive.
    Assert.assertFalse(this.isInActiveState);
  }

  private void receiveSyncEntityPayload(ResultCapture response, EntityMessage message) {
    // This only makes sense if we have a passive instance.
    Assert.assertNotNull(this.passiveServerEntity);
    this.passiveServerEntity.invoke(message);
    response.complete();
    // No retire on passive.
    Assert.assertFalse(this.isInActiveState);
  }
  
  @Override
  public boolean isDestroyed() {
    return this.isDestroyed;
  }

  @Override
  public boolean isActive() {
    return this.isInActiveState;
  }
  
  @Override
  public boolean isRemoveable() {
    return this.isDestroyed && runnables.isEmpty() && runnables.deferCleared;
  }

  private void destroyEntity(ServerEntityRequest request, ResultCapture response) {
    CommonServerEntity<EntityMessage, EntityResponse> commonServerEntity = this.isInActiveState
        ? activeServerEntity
        : passiveServerEntity;
    ClientDescriptor sourceDescriptor = request.getSourceDescriptor();
    EntityDescriptor entityDescriptor = getEntityDescriptorForSource(sourceDescriptor);
    if (this.isDestroyed) {
      response.failure(new EntityNotFoundException(entityDescriptor.getEntityID().getClassName(), entityDescriptor.getEntityID().getEntityName()));        
    } else if (null != commonServerEntity) {
      // We want to ensure that nobody somehow has a reference to this entity.
      if (!this.canDelete) {
        response.failure(new PermanentEntityException(entityDescriptor.getEntityID().getClassName(), entityDescriptor.getEntityID().getEntityName()));
      } else if (clientEntityStateManager.verifyNoReferences(entityDescriptor.getEntityID())) {
        Assert.assertFalse(this.isDestroyed);
        commonServerEntity.destroy();
        this.retirementManager.entityWasDestroyed();
        if (this.isInActiveState) {
          this.activeServerEntity = null;
        } else {
          this.passiveServerEntity = null;
        }
        this.isDestroyed = true;
        eventCollector.entityWasDestroyed(id);    
        response.complete();
      } else {
        response.failure(new EntityReferencedException(entityDescriptor.getEntityID().getClassName(), entityDescriptor.getEntityID().getEntityName()));        
      }
    }
  }

  private void reconfigureEntity(ResultCapture reconfigureEntityRequest, byte[] constructorInfo) {
    byte[] oldconfig = this.constructorInfo;
    if (this.isDestroyed || (this.activeServerEntity == null && this.passiveServerEntity == null)) {
      reconfigureEntityRequest.failure(new EntityNotFoundException(this.getID().getClassName(), this.getID().getEntityName()));
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
        this.passiveServerEntity = this.factory.createPassiveEntity(this.registry, this.constructorInfo);
        Assert.assertNull(this.concurrencyStrategy);
        // Store the configuration in case we promote.
        entityToCreate = this.passiveServerEntity;
      }
    }
    reconfigureEntityRequest.complete(oldconfig);
    // We currently don't support loading an entity from a persistent back-end and this call is in response to creating a new
    //  instance so make that call.
    entityToCreate.loadExisting();
    // Fire the event that the entity was created.
    this.eventCollector.entityWasReloaded(this.getID(), this.consumerID, this.isInActiveState);
  }
  
  private void createEntity(ResultCapture response, byte[] constructorInfo) {
    if (!this.isDestroyed && (this.activeServerEntity != null || this.passiveServerEntity != null)) {
      response.failure(new EntityAlreadyExistsException(this.getID().getClassName(), this.getID().getEntityName()));
//  failed to create, destroyed
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
        this.passiveServerEntity = this.factory.createPassiveEntity(this.registry, this.constructorInfo);
        Assert.assertNull(this.concurrencyStrategy);
        // Store the configuration in case we promote.
        entityToCreate = this.passiveServerEntity;
      }
    }
    this.isDestroyed = false;
    eventCollector.entityWasCreated(id, this.consumerID, isInActiveState);
    response.complete();
    // We currently don't support loading an entity from a persistent back-end and this call is in response to creating a new
    //  instance so make that call.
    entityToCreate.createNew();
  }

  private void performSync(ResultCapture response, Set<NodeID> passives, int concurrencyKey) {
    if (!this.isDestroyed) {
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
                  executor.scheduleSync(PassiveSyncMessage.createPayloadMessage(id, version, concurrencyKey, message), passive).waitForCompleted();
                } catch (EntityUserException | InterruptedException eu) {
                // TODO: do something reasoned here
                  throw new RuntimeException(eu);
                }
              }
            }
          };
        //  start is handled by the sync request that triggered this action
          this.activeServerEntity.synchronizeKeyToPassive(syncChannel, concurrencyKey);
        }
      } else {
        throw new IllegalStateException("syncing a passive entity");
      }
    }
//  whether the entity is destroyed or not, if arrived here. end sync needs to be called
    for (NodeID passive : passives) {
      try {
        executor.scheduleSync(PassiveSyncMessage.createEndEntityKeyMessage(id, version, concurrencyKey), passive).waitForCompleted();
      } catch (InterruptedException ie) {
      // TODO: do something reasoned here
        throw new RuntimeException(ie);
      }
    }
    response.complete();
  }
  
  private void performAction(ServerEntityRequest wrappedRequest, ResultCapture response, EntityMessage message) {
    Assert.assertNotNull(message);
    if (this.isInActiveState) {
      if (null == this.activeServerEntity) {
        throw new IllegalStateException("Actions on a non-existent entity.");
      } else {
        final int concurrencyKey = (null != this.concurrencyStrategy)
            ? this.concurrencyStrategy.concurrencyKey(message)
            : ConcurrencyStrategy.MANAGEMENT_KEY;
        try {
          this.retirementManager.registerWithMessage(message, concurrencyKey);
          byte[] er = runWithHelper(()->codec.encodeResponse(this.activeServerEntity.invoke(wrappedRequest.getSourceDescriptor(), message)));
          response.complete(er);
        } catch (EntityUserException e) {
          response.failure(e);
          throw new RuntimeException(e);
        }
      }
    } else {
      if (null == this.passiveServerEntity) {
        throw new IllegalStateException("Actions on a non-existent entity.");
      } else {
        this.passiveServerEntity.invoke(message);
        response.complete();
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
  public RetirementManager getRetirementManager() {
    return this.retirementManager;
  }

  @Override
  public void loadEntity(byte[] configuration) {
    this.loadExisting(configuration);
  }

  private void getEntity(ServerEntityRequest getEntityRequest, ResultCapture response) {
    if (this.isInActiveState) {
      if (this.isDestroyed) {
        response.failure(new EntityNotFoundException(id.getClassName(), id.getEntityName()));
      } else if (null != this.activeServerEntity) {
        ClientDescriptor sourceDescriptor = getEntityRequest.getSourceDescriptor();
        EntityDescriptor entityDescriptor = getEntityDescriptorForSource(sourceDescriptor);
        // The FETCH can only come directly from a client so we can down-cast.
        ClientID clientID = (ClientID) getEntityRequest.getNodeID();
        clientEntityStateManager.addReference(clientID, entityDescriptor);
        response.complete(this.constructorInfo);
        // Fire the event that the client fetched the entity.
        this.eventCollector.clientDidFetchEntity(clientID, this.getID(), sourceDescriptor);
        // finally notify the entity that it was fetched
        this.activeServerEntity.connected(sourceDescriptor);
      } else {
        response.complete();
      }
    } else {
      throw new IllegalStateException("GET called on passive entity.");
    }
  }

  private void releaseEntity(ServerEntityRequest request, ResultCapture response) {
    if (this.isInActiveState) {
      if (this.isDestroyed) {
        response.failure(new EntityNotFoundException(id.getClassName(), id.getEntityName()));
      } else if (null != this.activeServerEntity) {
        ClientDescriptor sourceDescriptor = request.getSourceDescriptor();
        EntityDescriptor entityDescriptor = getEntityDescriptorForSource(sourceDescriptor);
        // The RELEASE can only come directly from a client so we can down-cast.
        ClientID clientID = (ClientID) request.getNodeID();
        clientEntityStateManager.removeReference(clientID, entityDescriptor);
        this.activeServerEntity.disconnected(sourceDescriptor);
        // Fire the event that the client released the entity.
        this.eventCollector.clientDidReleaseEntity(clientID, this.getID());
        response.complete();
      } else {
        response.complete();
      }
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
//  checking destroyed here should be fine.  no other threads should be touching during promote
    if (!this.isDestroyed) {
      this.isInActiveState = true;
      if (null != this.passiveServerEntity) {
        this.activeServerEntity = factory.createActiveEntity(this.registry, this.constructorInfo);
        this.concurrencyStrategy = factory.getConcurrencyStrategy(this.constructorInfo);
        this.activeServerEntity.loadExisting();
        this.passiveServerEntity = null;
        // Fire the event that the entity was reloaded.
        this.eventCollector.entityWasReloaded(this.getID(), this.consumerID, true);
      } else {
        throw new IllegalStateException("no entity to promote");
      }
    }
  }
  
  @Override
  public void sync(NodeID passive) {
    try {
  // wait for future is ok, occuring on sync executor thread
      executor.scheduleSync(PassiveSyncMessage.createStartEntityMessage(id, version, constructorInfo, canDelete), passive).waitForCompleted();
  // iterate through all the concurrency keys of an entity
      EntityDescriptor entityDescriptor = new EntityDescriptor(this.id, ClientInstanceID.NULL_ID, this.version);
  //  this is simply a barrier to make sure all actions are flushed before sync is started (hence, it has a null passive).
      PassiveSyncServerEntityRequest req = new PassiveSyncServerEntityRequest(passive);
// wait for future is ok, occuring on sync executor thread
      BarrierCompletion opComplete = new BarrierCompletion();
      this.executor.scheduleRequest(entityDescriptor, new ServerEntityRequestImpl(entityDescriptor, ServerEntityAction.NOOP, ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, Collections.emptySet()), MessagePayload.EMPTY, ()-> { 
          Assert.assertTrue(this.isInActiveState);
          opComplete.complete();
        }, ConcurrencyStrategy.MANAGEMENT_KEY).waitForCompleted();
      //  wait for completed above waits for acknowledgment from the passive
      //  waitForCompletion below waits for completion of the local request processor
      opComplete.waitForCompletion();
      waitForReconfigureToFinish();
      try {
        for (Integer concurrency : concurrencyStrategy.getKeysForSynchronization()) {
          // We don't actually use the message in the direct strategy so this is safe.
          //  don't care about the result
          BarrierCompletion sectionComplete = new BarrierCompletion();
          this.executor.scheduleRequest(entityDescriptor, req, MessagePayload.EMPTY,  () -> {invoke(req, new ResultCapture(null, null), MessagePayload.EMPTY, concurrency);sectionComplete.complete();}, concurrency).waitForCompleted();
        //  wait for completed above waits for acknowledgment from the passive
        //  waitForCompletion below waits for completion of the local request processor
          sectionComplete.waitForCompletion();
        }
      } finally {
        syncFinished();
      }
  //  end passive sync for an entity
  // wait for future is ok, occuring on sync executor thread
      executor.scheduleSync(PassiveSyncMessage.createEndEntityMessage(id, version), passive).waitForCompleted();
    } catch (InterruptedException e) {
      throw new AssertionError("sync failed", e);
    }
  }  
  /**
   * sync and reconfigure MUST be exclusive events from each other. 
   * this method is called by sync before stepping the concurrency keys
   */
  private void waitForReconfigureToFinish() {
 // synchronize on reconfigure to prevent deadlock
    synchronized (reconfiguringInflight) {
      reconfiguringInflight.waitForOperationsToComplete();
      syncingThreads.startOperation();
    }
  }
  /**
   * sync and reconfigure MUST be exclusive events from each other. 
   * this method is called before reconfigure is scheduled
   */  
  private void waitForSyncToFinish() {
 // synchronize on reconfigure to prevent deadlock
    synchronized (reconfiguringInflight) {
      syncingThreads.waitForOperationsToComplete();
      reconfiguringInflight.startOperation();
    }
  }
  /**
   * sync and reconfigure MUST be exclusive events from each other. 
   * sync is finished stepping the keys
   */
  private void syncFinished() {
    syncingThreads.finishOperation();
  }
  /**
   * sync and reconfigure MUST be exclusive events from each other. 
   * reconfigure has completed
   */
  private void reconfigureFinished() {
    reconfiguringInflight.finishOperation();
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
    this.isDestroyed = false;
    entityToLoad.loadExisting();
    // Fire the event that the entity was reloaded.
    this.eventCollector.entityWasReloaded(this.getID(), this.consumerID, this.isInActiveState);
  }

  private static class PassiveSyncServerEntityRequest implements ServerEntityRequest {
    private final NodeID passive;
    private final ServerEntityAction action;
    
    public PassiveSyncServerEntityRequest(NodeID passive) {
      action = ServerEntityAction.REQUEST_SYNC_ENTITY;
      this.passive = passive;
    }

    @Override
    public ClientID getNodeID() {
      return ClientID.NULL_ID;
    }

    @Override
    public Set<NodeID> replicateTo(Set<NodeID> passives) {
      return passive == null ? Collections.emptySet() : Collections.singleton(passive);
    }

    @Override
    public ClientDescriptor getSourceDescriptor() {
      return null;
    }

    @Override
    public ServerEntityAction getAction() {
      return action;
    }

    @Override
    public TransactionID getTransaction() {
      return TransactionID.NULL_ID;
    }

    @Override
    public TransactionID getOldestTransactionOnClient() {
      return TransactionID.NULL_ID;
    }
  }
  
  private class SchedulingRunnable implements Runnable {
    private final EntityDescriptor desc;
    private final ServerEntityRequest request;
    private final MessagePayload payload;
    private final Runnable original;
    private final int concurrency;
    private ActivePassiveAckWaiter  waitFor;

    public SchedulingRunnable(EntityDescriptor desc, ServerEntityRequest request, MessagePayload payload, Runnable r, int concurrency) {
      this.desc = desc;
      this.request = request;
      this.payload = payload;
      this.original = r;
      this.concurrency = concurrency;
    }
        
    private synchronized void start() {
      if (concurrency == ConcurrencyStrategy.MANAGEMENT_KEY) {
        runnables.activate();
      }
      waitFor = executor.scheduleRequest(desc, request, payload, this, concurrency);
      this.notifyAll();
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
    
    public synchronized void waitForPassives() {
      try {
        while (waitFor == null) {
          this.wait();
        }
        waitFor.waitForCompleted();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
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
    
    boolean isEmpty() {
      return queue.isEmpty();
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
  
  public static class ResultCapture implements SimpleCompletion {
    private final Consumer<byte[]> result;
    private final Consumer<EntityException> error;
    private SchedulingRunnable setOnce;
    private boolean done = false;

    public ResultCapture(Consumer<byte[]> result, Consumer<EntityException> error) {
      this.result = result;
      this.error = error;
    }
    
    public void setWaitFor(SchedulingRunnable waitFor) {
      Assert.assertNull(setOnce);
      setOnce = waitFor;
    }
    
    public synchronized void finish() {
      Assert.assertFalse(done);
      done = true;
      this.notify();
    }
       
    public void waitForCompletion() {
      this.waitForCompletion(0, TimeUnit.MILLISECONDS);
    }
    
    public synchronized void waitForCompletion(long timeout, TimeUnit units) {
      boolean interrupted = false;
      while (!done) {
        try {
          this.wait(units.toMillis(timeout));
        } catch (InterruptedException ie) {
          interrupted = true;
        }
      }
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
    
    public void complete() {
      if (result != null) {
        if (setOnce != null) {
          setOnce.waitForPassives();
        }
        result.accept(null);
      }
      finish();
    }  
    
    public void complete(byte[] value) {
      if (result != null) {
        if (setOnce != null) {
          setOnce.waitForPassives();
        }
        result.accept(value);
      }
      finish();
    }
    
    public void failure(EntityException ee) {
      if (error != null) {
        if (setOnce != null) {
          setOnce.waitForPassives();
        }
        error.accept(ee);
      }
      finish();
    }
  }
}
