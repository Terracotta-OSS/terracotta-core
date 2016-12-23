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

import com.tc.exception.EntityBusyException;
import com.tc.exception.EntityReferencedException;
import com.tc.exception.TCServerRestartException;
import com.tc.exception.TCShutdownServerException;
import com.tc.l2.msg.ReplicationMessage;
import com.tc.l2.msg.SyncReplicationActivity;
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
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.ExecutionStrategy;
import org.terracotta.exception.EntityConfigurationException;
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
  //  for destroy, passives need to reference count to understand if entity is deletable
  private int clientReferenceCount = 0;
  private final boolean canDelete;
  private volatile boolean isDestroyed;
  
  private final MessageCodec<EntityMessage, EntityResponse> codec;
  private final SyncMessageCodec<EntityMessage> syncCodec;
  private volatile ActiveServerEntity<EntityMessage, EntityResponse> activeServerEntity;
  private volatile ConcurrencyStrategy<EntityMessage> concurrencyStrategy;
  private volatile ExecutionStrategy<EntityMessage> executionStrategy;

  private final DefermentQueue<SchedulingRunnable> runnables = new DefermentQueue<>(TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.ENTITY_DEFERMENT_QUEUE_SIZE, 1024));

  private volatile PassiveServerEntity<EntityMessage, EntityResponse> passiveServerEntity;
  //  reconnect access has to be exclusive.  it is out-of-band from normal invoke access
  private final ReadWriteLock reconnectAccessLock = new ReentrantReadWriteLock();
  private final ManagedEntitySyncInterop interop = new ManagedEntitySyncInterop();
  // NOTE:  This may be removed in the future if we change how we access the config from the ServerEntityService but
  //  it presently holds the config we used when we first created passiveServerEntity (if it isn't null).  It is used
  //  when we promote to an active.
  private byte[] constructorInfo;

  ManagedEntityImpl(EntityID id, long version, long consumerID, BiConsumer<EntityID, Long> loopback, InternalServiceRegistry registry, ClientEntityStateManager clientEntityStateManager, ITopologyEventCollector eventCollector,
                    RequestProcessor process, EntityServerService<EntityMessage, EntityResponse> factory,
                    boolean isInActiveState, int references) {
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
    this.canDelete = references >= 0;
    this.clientReferenceCount = references;
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
    
  private ResultCapture createManagedEntityResponse(Consumer<byte[]> completion, Consumer<EntityException> exception, Object debug, boolean lifecycle) {
    return new ResultCapture(completion, exception, debug, lifecycle);
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
    ResultCapture resp;
    switch (request.getAction()) {
      case NOOP:
        resp = createManagedEntityResponse(completion, exception, request, false);
        processNoopMessage(request, resp);
        break;
      case CREATE_ENTITY:
      case DESTROY_ENTITY:
      case FETCH_ENTITY:
      case RECONFIGURE_ENTITY:
      case RELEASE_ENTITY:
        resp = createManagedEntityResponse(completion, exception, request, true);
        processLifecycleEntity(request, data, resp);
        break;
      case INVOKE_ACTION:
        resp = createManagedEntityResponse(completion, exception, request, false);
        processInvokeRequest(request, resp, data, data.getConcurrency());
        break;
      case RECEIVE_SYNC_ENTITY_START:
      case RECEIVE_SYNC_ENTITY_END:
      case RECEIVE_SYNC_ENTITY_KEY_START:
      case RECEIVE_SYNC_ENTITY_KEY_END:
      case RECEIVE_SYNC_PAYLOAD:
        Assert.assertTrue(!this.isInActiveState);
        resp = createManagedEntityResponse(completion, exception, request, false);
        processSyncMessage(request, resp, data, data.getConcurrency());
        break;
      default:
        throw new IllegalArgumentException("Unknown request " + request);
    }
    return resp;
  }
  
  private void processLifecycleEntity(ServerEntityRequest create, MessagePayload data, ResultCapture resp) {
    boolean schedule = true;
    if (this.isInActiveState) {
//  this is the process transaction handler thread adding a lifecycle to the message flow of this entity.  
//  before it can proceed, need to make sure any syncs are completed so the concurrency strategy is not changed out from 
//  under it.
      switch(create.getAction()) {
        case CREATE_ENTITY:
        case DESTROY_ENTITY:
        case RECONFIGURE_ENTITY:
          if (data.canBeBusy()) {
            schedule = interop.tryStartLifecycle();
          } else {
            interop.startLifecycle();
          }
          break;
        case FETCH_ENTITY:
        case RELEASE_ENTITY:
          if (data.canBeBusy()) {
            schedule = interop.tryStartReference();
          } else {
            interop.startReference();
          }
          break;
        default:
          throw new AssertionError("unexpected");
      }
    }
    if (schedule) {
      scheduleInOrder(getEntityDescriptorForSource(create.getSourceDescriptor()), create, resp, data , ()-> {
        invokeLifecycleOperation(create, data, resp);
      }, ConcurrencyStrategy.MANAGEMENT_KEY);
    } else {
      resp.failure(new EntityBusyException(id.getClassName(), id.getEntityName(), "entity is busy in sync, retry"));
    }
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
      Assert.assertTrue(Thread.currentThread().getName().contains(ServerConfigurationContext.PASSIVE_REPLICATION_STAGE) ||
        Thread.currentThread().getName().contains(ServerConfigurationContext.L2_STATE_CHANGE_STAGE));
    }
    
    SchedulingRunnable next = new SchedulingRunnable(desc, request, payload, r, ckey);
    results.setWaitFor(next);
    
    for (SchedulingRunnable msg : runnables) {
      msg.start();
    }

    if (!runnables.offer(next)) {
      next.start();
    } else if (Thread.currentThread().getName().contains(ServerConfigurationContext.L2_STATE_CHANGE_STAGE)) {
      for (SchedulingRunnable sr : runnables.queue) {
        logger.fatal(runnables + " " + this.id + " " + sr);
      }
      Assert.fail();
    }
    
    return next;
  }
  
  @Override
  public boolean clearQueue() {
    while (!runnables.isEmpty()) {
      SchedulingRunnable sr = runnables.checkDeferred();
      if (sr != null) {
        sr.start();
      }
      runnables.pause();
    }
    return true;
  }
  
  private void processInvokeRequest(final ServerEntityRequest request, ResultCapture response, MessagePayload message, int key) {
    ClientDescriptor client = request.getSourceDescriptor();
    if (isInActiveState) {
      key = this.concurrencyStrategy.concurrencyKey(message.decodeRawMessage(raw->this.codec.decodeMessage(raw)));
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
  
  private void invokeLifecycleOperation(final ServerEntityRequest request, MessagePayload payload, ResultCapture resp) {
    Lock read = reconnectAccessLock.readLock();
    logger.info("Client:" + request.getNodeID() + " Invoking lifecycle " + request.getAction() + " on " + getID());
    read.lock();
    try {
      switch (request.getAction()) {
        case CREATE_ENTITY:
          createEntity(resp, payload.getRawPayload());
          break;
        case FETCH_ENTITY:
          getEntity(request, resp, payload.getRawPayload());
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
    } catch (ConfigurationException ce) {
      // Wrap this exception.
      EntityConfigurationException wrapper = new EntityConfigurationException(id.getClassName(), id.getEntityName(), ce);
      logger.error("configuration error during a lifecyle operation ", wrapper);
      resp.failure(wrapper);
    } catch (TCShutdownServerException shutdown) {
      throw shutdown;
    } catch (TCServerRestartException shutdown) {
      throw shutdown;
    } catch (Exception e) {
      // Wrap this exception.
      EntityUserException wrapper = new EntityUserException(id.getClassName(), id.getEntityName(), e);
      logger.error("caught exception during invoke ", wrapper);
      throw new RuntimeException(wrapper);
    } finally {
      read.unlock();
      if (this.isInActiveState) {
        interop.finishLifecycle();
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
            performAction(request, response, message);
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
            receiveSyncEntityPayload(response, message);
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
    try {
      createEntity(response, constructor);
    } catch (ConfigurationException ce) {
      throw new TCShutdownServerException("unable to create entity on passive sync " + this.id);
    }
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

  private void receiveSyncEntityPayload(ResultCapture response, MessagePayload message) {
    // This only makes sense if we have a passive instance.
    Assert.assertNotNull(this.passiveServerEntity);
    this.passiveServerEntity.invoke(message.decodeRawMessage(raw->syncCodec.decode(message.getConcurrency(), raw)));
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
        Assert.assertTrue(clientReferenceCount < 0);
        response.failure(new PermanentEntityException(entityDescriptor.getEntityID().getClassName(), entityDescriptor.getEntityID().getEntityName()));
      } else if (clientReferenceCount == 0) {
        Assert.assertTrue(!isInActiveState || clientEntityStateManager.verifyNoReferences(entityDescriptor.getEntityID()));
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
        Assert.assertTrue(!isInActiveState || !clientEntityStateManager.verifyNoReferences(entityDescriptor.getEntityID()));
        response.failure(new EntityReferencedException(entityDescriptor.getEntityID().getClassName(), entityDescriptor.getEntityID().getEntityName()));        
      }
    }
  }

  private void reconfigureEntity(ResultCapture reconfigureEntityRequest, byte[] constructorInfo) throws ConfigurationException {
    byte[] oldconfig = this.constructorInfo;
    if (this.isDestroyed || (this.activeServerEntity == null && this.passiveServerEntity == null)) {
      reconfigureEntityRequest.failure(new EntityNotFoundException(this.getID().getClassName(), this.getID().getEntityName()));
      return;
    }
    this.constructorInfo = constructorInfo;
    // Create the appropriate kind of entity, based on our active/passive state.
    if (this.isInActiveState) {
      if (null == this.activeServerEntity) {
        throw new IllegalStateException("Active entity " + id + " does not exists.");
      } else {
        this.activeServerEntity = this.factory.reconfigureEntity(this.registry, this.activeServerEntity, constructorInfo);
        this.concurrencyStrategy = this.factory.getConcurrencyStrategy(constructorInfo);
        this.executionStrategy = this.factory.getExecutionStrategy(constructorInfo);
      }
    } else {
      if (null == this.passiveServerEntity) {
        throw new IllegalStateException("Passive entity " + id + " does not exists.");
      } else {
        this.passiveServerEntity = this.factory.reconfigureEntity(this.registry, this.passiveServerEntity, this.constructorInfo);
        Assert.assertNull(this.concurrencyStrategy);
        Assert.assertNull(this.executionStrategy);
        // TODO: Store the configuration in case we promote.
      }
    }
    reconfigureEntityRequest.complete(oldconfig);
    // Fire the event that the entity was created.
    this.eventCollector.entityWasReloaded(this.getID(), this.consumerID, this.isInActiveState);
  }
  
  private void createEntity(ResultCapture response, byte[] constructorInfo) throws ConfigurationException {
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
        this.executionStrategy = this.factory.getExecutionStrategy(constructorInfo);
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
                  executor.scheduleSync(ReplicationMessage.createActivityContainer(SyncReplicationActivity.createPayloadMessage(id, version, concurrencyKey, message, "")), passive).waitForReceived();
                } catch (EntityUserException eu) {
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
    
    response.complete();
  }
  
  private void performAction(ServerEntityRequest wrappedRequest, ResultCapture response, MessagePayload message) {
    Assert.assertNotNull(message);
    EntityMessage em = message.decodeRawMessage(raw->this.codec.decodeMessage(raw));
    if (this.isInActiveState) {
      if (null == this.activeServerEntity) {
        throw new IllegalStateException("Actions on a non-existent entity.");
      } else {
        try {
          final int concurrencyKey = this.concurrencyStrategy.concurrencyKey(em);
          this.retirementManager.registerWithMessage(em, concurrencyKey);
          ExecutionStrategy.Location loc = this.executionStrategy.getExecutionLocation(em);
          if (loc.runOnActive()) {
            byte[] er = runWithHelper(()->codec.encodeResponse(this.activeServerEntity.invoke(wrappedRequest.getSourceDescriptor(), em)));
            response.complete(er);
          } else {
            response.complete(new byte[0]);
          }
        } catch (EntityUserException e) {
          response.failure(e);
          throw new RuntimeException(e);
        }
      }
    } else {
      if (null == this.passiveServerEntity) {
        throw new IllegalStateException("Actions on a non-existent entity.");
      } else {
        this.passiveServerEntity.invoke(em);
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
    try {
      this.loadExisting(configuration);
    } catch (ConfigurationException ce) {
      throw new TCShutdownServerException("unable to create entity on passive sync " + this.id);
    }
  }

  private void getEntity(ServerEntityRequest getEntityRequest, ResultCapture response, byte[] extendedData) {
    if (this.isDestroyed) {
      response.failure(new EntityNotFoundException(id.getClassName(), id.getEntityName()));
    } else {
      if (canDelete) {
        clientReferenceCount += 1;
        Assert.assertTrue(clientReferenceCount > 0);
      }
      if (this.isInActiveState) {
        ClientDescriptor sourceDescriptor = getEntityRequest.getSourceDescriptor();
        EntityDescriptor entityDescriptor = getEntityDescriptorForSource(sourceDescriptor);
        // The FETCH can only come directly from a client so we can down-cast.
        ClientID clientID = (ClientID) getEntityRequest.getNodeID();
        boolean added = clientEntityStateManager.addReference(clientID, entityDescriptor);
        Assert.assertTrue(added);
        // Fire the event that the client fetched the entity.
        this.eventCollector.clientDidFetchEntity(clientID, entityDescriptor, sourceDescriptor);
        // finally notify the entity that it was fetched
        this.activeServerEntity.connected(sourceDescriptor);
        if (getEntityRequest.getTransaction().equals(TransactionID.NULL_ID)) {
//   this is a reconnection, handle the extended reconnect data
          this.activeServerEntity.handleReconnect(sourceDescriptor, extendedData);
        }
      }
      response.complete(this.constructorInfo);
    }
  }

  private void releaseEntity(ServerEntityRequest request, ResultCapture response) {
    if (this.isDestroyed) {
      response.failure(new EntityNotFoundException(id.getClassName(), id.getEntityName()));
    } else {
      if (canDelete) {
        clientReferenceCount -= 1;
        Assert.assertTrue(clientReferenceCount >= 0);
      }
      if (this.isInActiveState) {
        ClientDescriptor sourceDescriptor = request.getSourceDescriptor();
        EntityDescriptor entityDescriptor = getEntityDescriptorForSource(sourceDescriptor);
        // The RELEASE can only come directly from a client so we can down-cast.
        ClientID clientID = (ClientID) request.getNodeID();
        boolean removed = clientEntityStateManager.removeReference(clientID, entityDescriptor);
        Assert.assertTrue(removed);
        
        this.activeServerEntity.disconnected(sourceDescriptor);
        // Fire the event that the client released the entity.
        this.eventCollector.clientDidReleaseEntity(clientID, entityDescriptor);
      }
      response.complete();
    }
  }
  
  private EntityDescriptor getEntityDescriptorForSource(ClientDescriptor sourceDescriptor) {
    // We are in internal code so downcast the descriptor.
    ClientDescriptorImpl rawDescriptor = (ClientDescriptorImpl)sourceDescriptor;
    return rawDescriptor.getEntityDescriptor();
  }
  
  @Override
  public void resetReferences(int count) {
    this.clientReferenceCount = count;
  }
  
  @Override
  public void promoteEntity() throws ConfigurationException {
    // Can't enter active state twice.
    Assert.assertFalse(this.isInActiveState);
    Assert.assertNull(this.activeServerEntity);
//  checking destroyed here should be fine.  no other threads should be touching during promote
    this.isInActiveState = true;
// any clients, previously connected will reconnect or not.  Failed reconnects will be cleaned
// up on passives 
    this.clientReferenceCount = 0;

    if (!this.isDestroyed) {
      if (null != this.passiveServerEntity) {
        this.activeServerEntity = factory.createActiveEntity(this.registry, this.constructorInfo);
        this.concurrencyStrategy = factory.getConcurrencyStrategy(this.constructorInfo);
        this.executionStrategy = factory.getExecutionStrategy(this.constructorInfo);
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
// iterate through all the concurrency keys of an entity
    EntityDescriptor entityDescriptor = new EntityDescriptor(this.id, ClientInstanceID.NULL_ID, this.version);
//  this is simply a barrier to make sure all actions are flushed before sync is started (hence, it has a null passive).
    PassiveSyncServerEntityRequest req = new PassiveSyncServerEntityRequest(passive);
// wait for future is ok, occuring on sync executor thread
    BarrierCompletion opComplete = new BarrierCompletion();
    this.executor.scheduleRequest(entityDescriptor, new ServerEntityRequestImpl(entityDescriptor, ServerEntityAction.NOOP, ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, Collections.emptySet()), MessagePayload.EMPTY, ()-> { 
        Assert.assertTrue(this.isInActiveState);
        if (!this.isDestroyed) {
          executor.scheduleSync(ReplicationMessage.createActivityContainer(SyncReplicationActivity.createStartEntityMessage(id, version, constructorInfo, canDelete ? this.clientReferenceCount : ManagedEntity.UNDELETABLE_ENTITY)), passive).waitForCompleted();
        }
        opComplete.complete();
      }, true, ConcurrencyStrategy.MANAGEMENT_KEY).waitForCompleted();
    //  wait for completed above waits for acknowledgment from the passive
    //  waitForCompletion below waits for completion of the local request processor
    opComplete.waitForCompletion();
    interop.syncStarted();
// wait for future is ok, occuring on sync executor thread
    try {
      if (!this.isDestroyed) {
        for (Integer concurrency : concurrencyStrategy.getKeysForSynchronization()) {
    // make sure that concurrency key is in the valid range
          //  MGMT_KEY and UNIVERSAL keys are not valid for sync
          Assert.assertTrue(concurrency > 0);  
          // We don't actually use the message in the direct strategy so this is safe.
          //  don't care about the result
                                              
          BarrierCompletion sectionComplete = new BarrierCompletion();
          this.executor.scheduleRequest(entityDescriptor, req, MessagePayload.EMPTY,  ()->invoke(req, new ResultCapture(result->sectionComplete.complete(), null, null, false), MessagePayload.EMPTY, concurrency), true, concurrency).waitForCompleted();
        //  wait for completed above waits for acknowledgment from the passive
        //  waitForCompletion below waits for completion of the local request processor
          sectionComplete.waitForCompletion();
          executor.scheduleSync(ReplicationMessage.createActivityContainer(SyncReplicationActivity.createEndEntityKeyMessage(id, version, concurrency)), passive).waitForCompleted();
        }
  //  end passive sync for an entity
  // wait for future is ok, occuring on sync executor thread
        executor.scheduleSync(ReplicationMessage.createActivityContainer(SyncReplicationActivity.createEndEntityMessage(id, version)), passive).waitForCompleted();
      }
    } finally {
      interop.syncFinished();
    }
  }  
  
  public void startSync() {
    interop.startSync();
  }

  private void loadExisting(byte[] constructorInfo) throws ConfigurationException {
    this.constructorInfo = constructorInfo;
    // Create the appropriate kind of entity, based on our active/passive state.
    if (this.isInActiveState) {
      if (null != this.activeServerEntity) {
        throw new IllegalStateException("Active entity " + id + " already exists.");
      } else {
        this.activeServerEntity = factory.createActiveEntity(registry, constructorInfo);
        this.concurrencyStrategy = factory.getConcurrencyStrategy(constructorInfo);
        this.executionStrategy = factory.getExecutionStrategy(constructorInfo);
//  only active entities have load existing.  passive entities that have persistent state will either transition to active
//  or be zapped
        this.activeServerEntity.loadExisting();
// Fire the event that the entity was reloaded.  This should only happen on active entities.  Passives with either transition to active soon or be destroyed
        this.eventCollector.entityWasReloaded(this.getID(), this.consumerID, this.isInActiveState);
      }
    } else {
      if (null != this.passiveServerEntity) {
        throw new IllegalStateException("Passive entity " + id + " already exists.");
      } else {
        this.passiveServerEntity = factory.createPassiveEntity(registry, constructorInfo);
        Assert.assertNull(this.concurrencyStrategy);
      }
    }
    this.isDestroyed = false;
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
    private boolean waitForReplication = false;

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
      boolean replicate = payload.shouldReplicate();
      switch(request.getAction()) {
        case CREATE_ENTITY:
        case DESTROY_ENTITY:
        case FETCH_ENTITY:
        case RECONFIGURE_ENTITY:
        case RELEASE_ENTITY:
          if (replicate == false && isInActiveState) {
            logger.warn("Ignoring replication flag. All lifecycle operations are replicated " + request.getAction());
          }
          replicate = true;
          break;
        default:
          break;
      }
      if (isActive() && request.getAction() == ServerEntityAction.INVOKE_ACTION) {
        ExecutionStrategy.Location loc = executionStrategy.getExecutionLocation(payload.decodeRawMessage(raw->codec.decodeMessage(raw)));
        if (loc != ExecutionStrategy.Location.IGNORE) {
          replicate = loc.runOnPassive();
        }
      } 
      waitForReplication = replicate;
      waitFor = executor.scheduleRequest(desc, request, payload, this, replicate, concurrency);
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
    
    private synchronized ActivePassiveAckWaiter waitForPassives() {
      try {
        while (waitFor == null) {
          this.wait();
        }
        if (waitForReplication) {
          waitFor.waitForCompleted();
        } else {
          waitFor.waitForReceived();
        }
        return waitForReplication ? waitFor : null;
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
      try {
        return deferCleared;
      } finally {
        deferCleared = false;
      }
    }
    
    synchronized boolean clear() {
     try {
        notifyAll();
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
    private final boolean lifecycle;
    private final Object debugID;

    public ResultCapture(Consumer<byte[]> result, Consumer<EntityException> error, Object debugID, boolean lifecycle) {
      this.result = result;
      this.error = error;
      this.lifecycle = lifecycle;
      this.debugID = debugID;
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
          ActivePassiveAckWaiter waiter = setOnce.waitForPassives();
          if (lifecycle) {
            if (waiter.verifyLifecycleResult(true)) {
              logger.warn("ZAP occurred while processing " + debugID);
            }
          }
        }
        result.accept(null);
      }
      finish();
    }  
    
    public void complete(byte[] value) {
      if (result != null) {
        if (setOnce != null) {
          ActivePassiveAckWaiter waiter = setOnce.waitForPassives();
          if (lifecycle) {
            if (waiter.verifyLifecycleResult(true)) {
              logger.warn("ZAP occurred while processing " + debugID);
            }
          }
        }
        result.accept(value);
      }
      finish();
    }
    
    public void failure(EntityException ee) {
      if (error != null) {
        if (setOnce != null) {
          ActivePassiveAckWaiter waiter = setOnce.waitForPassives();
          if (lifecycle) {
            if (waiter.verifyLifecycleResult(false)) {
              logger.warn("ZAP occurred while processing " + debugID);
            }
          }
        }
        error.accept(ee);
      }
      finish();
    }
  }
}
