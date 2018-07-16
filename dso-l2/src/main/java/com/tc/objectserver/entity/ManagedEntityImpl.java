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

import com.tc.async.api.DirectExecutionMode;
import com.tc.async.api.Sink;
import com.tc.entity.VoltronEntityMessage;
import com.tc.exception.EntityBusyException;
import com.tc.exception.EntityReferencedException;
import com.tc.exception.TCServerRestartException;
import com.tc.exception.TCShutdownServerException;
import com.tc.exception.VoltronEntityUserExceptionWrapper;
import com.tc.exception.VoltronWrapperException;
import com.tc.l2.msg.SyncReplicationActivity;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.FetchID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.api.ManagementKeyCallback;
import com.tc.objectserver.api.ResultCapture;
import com.tc.objectserver.api.Retiree;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ManagementTopologyEventCollector;
import com.tc.objectserver.handler.RetirementManager;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.services.InternalServiceRegistry;
import com.tc.services.MappedStateCollector;
import com.tc.tracing.Trace;
import com.tc.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.CommonServerEntity;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.EntityServerService;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.ExecutionStrategy;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.PassiveSynchronizationChannel;
import org.terracotta.entity.ReconnectRejectedException;
import org.terracotta.entity.SyncMessageCodec;
import org.terracotta.exception.EntityAlreadyExistsException;
import org.terracotta.exception.EntityConfigurationException;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityServerException;
import org.terracotta.exception.EntityServerUncaughtException;
import org.terracotta.exception.PermanentEntityException;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class ManagedEntityImpl implements ManagedEntity {
  private static final Logger logger   = LoggerFactory.getLogger(ManagedEntityImpl.class);

  private final RequestProcessor executor;
  private final RetirementManager retirementManager;

  private final EntityID id;
  private final FetchID fetchID;
  private final long version;
  private final long consumerID;
  private final InternalServiceRegistry registry;
  private final Sink<VoltronEntityMessage> messageSelf;
  private final ClientEntityStateManager clientEntityStateManager;
  private final ManagementTopologyEventCollector eventCollector;
  private final EntityServerService<EntityMessage, EntityResponse> factory;
  // PTH sink so things can be injected into the stream
  private final ManagementKeyCallback flushLocalPipeline;
  // isInActiveState defines which entity type to check/create - we need the flag to represent the pre-create state.
  private boolean isInActiveState;
  //  for destroy, passives need to reference count to understand if entity is deletable
  private int clientReferenceCount = 0;
  private final boolean canDelete;
  private volatile boolean isDestroyed;
  // We only track a single CreateListener since it is intended to be the EntityMessengerService attached to this.
  private final List<LifecycleListener> createListener = new CopyOnWriteArrayList<>();
  
  private final MessageCodec<EntityMessage, EntityResponse> codec;
  private final SyncMessageCodec<EntityMessage> syncCodec;
  private volatile ActiveServerEntity<EntityMessage, EntityResponse> activeServerEntity;
  private volatile ConcurrencyStrategy<EntityMessage> concurrencyStrategy;
  private volatile ExecutionStrategy<EntityMessage> executionStrategy;
  private volatile ActiveServerEntity.ReconnectHandler reconnect;

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
    
  ManagedEntityImpl(EntityID id, long version, long consumerID, ManagementKeyCallback flushLocalPipeline, InternalServiceRegistry registry, ClientEntityStateManager clientEntityStateManager, ManagementTopologyEventCollector eventCollector,
                    Sink<VoltronEntityMessage> msg,
                    RequestProcessor process, EntityServerService<EntityMessage, EntityResponse> factory,
                    boolean isInActiveState, boolean canDelete) {
    this.id = id;
    this.isDestroyed = true;
    this.version = version;
    this.consumerID = consumerID;
    // using consumerID to seed fetch id
    this.fetchID = new FetchID(consumerID);
    this.flushLocalPipeline = flushLocalPipeline;
    this.registry = registry;
    this.messageSelf = msg;
    Assert.assertNotNull(this.messageSelf);
    this.clientEntityStateManager = clientEntityStateManager;
    this.eventCollector = eventCollector;
    this.factory = factory;
    this.executor = process;
    // Create the RetirementManager here, since it is currently scoped per-entity.
    this.retirementManager = new RetirementManager();
    this.isInActiveState = isInActiveState;
    this.canDelete = canDelete;
    this.clientReferenceCount = canDelete ? 0 : ManagedEntity.UNDELETABLE_ENTITY;
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
  
  private void notifyEntityCreated() {
    CommonServerEntity entity = (this.isInActiveState) ? this.activeServerEntity : this.passiveServerEntity;
    createListener.forEach((l)->l.entityCreated(this));
  }
  
  private void notifyEntityDestroyed() {
    CommonServerEntity entity = (this.isInActiveState) ? this.activeServerEntity : this.passiveServerEntity;
    createListener.forEach((l)->l.entityDestroyed(this));
    createListener.clear();
  }
/**
 * This is the main entry point for interaction with the managed entity.A request consists of a defining action, a 
 payload and 2 consumers to consume the result of the interaction.  Only one of the consumers will be called as a result 
 of the action.  In the case of a successful interaction, the first consumer is passed the raw byte[] result of the interaction.
 On failure, the EntityException is passed to the second consumer is passed the exception.  
 * @param request - defines the type of action requested and who requested it
 * @param data - The entity defined data to accompany the request
 * @param capture
 * @return a completion object which can be invoked to wait for the requested action to complete
 */

  @Override
  public void addRequestMessage(ServerEntityRequest request, MessagePayload data, ResultCapture resp) {
    if (logger.isDebugEnabled()) {
      logger.debug("add " + request.getAction() + " " + this.id + " " + this.fetchID);
    }
    Trace.activeTrace().log("ManagedEntityImpl.addRequestMessage");
    switch (request.getAction()) {
      case LOCAL_FLUSH:
      case ORDER_PLACEHOLDER_ONLY:
      case MANAGED_ENTITY_GC:
      case FAILOVER_FLUSH:
        processLegacyNoopMessage(request, resp);
        break;
      case LOCAL_FLUSH_AND_SYNC:
        // We expect this to be filtered at a higher level.
        Assert.fail(request.getAction() + " should be filtered before reaching this point");
        resp = null;
        break;
      case CREATE_ENTITY:
      case DESTROY_ENTITY:
      case FETCH_ENTITY:
      case RECONFIGURE_ENTITY:
      case RELEASE_ENTITY:
      case DISCONNECT_CLIENT:
        processLifecycleEntity(request, data, resp);
        break;
      case INVOKE_ACTION:
        processInvokeRequest(request, resp, data, data.getConcurrency());
        break;
      case RECEIVE_SYNC_CREATE_ENTITY:
        Assert.assertTrue(!this.isInActiveState);
        processSyncCreateMessage(request, resp, data);
        break;
      case RECEIVE_SYNC_ENTITY_START_SYNCING:
      case RECEIVE_SYNC_ENTITY_END:
        Assert.assertTrue(!this.isInActiveState);
        processSyncStartEndMessage(request, resp, data);
        break;
      case RECEIVE_SYNC_ENTITY_KEY_START:
      case RECEIVE_SYNC_ENTITY_KEY_END:
      case RECEIVE_SYNC_PAYLOAD:
        Assert.assertTrue(!this.isInActiveState);
        processSyncPayloadOtherMessage(request, resp, data, data.getConcurrency());
        break;
      default:
        throw new IllegalArgumentException("Unknown request " + request);
    }
  }
  
  private void processLifecycleEntity(ServerEntityRequest create, MessagePayload data, ResultCapture resp) {
    Trace.activeTrace().log("ManagedEntityImpl.processLifecycleEntity");
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
        case DISCONNECT_CLIENT:
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
      scheduleInOrder(create, resp, data , ()-> {
        invokeLifecycleOperation(create, data, resp);
      }, ConcurrencyStrategy.MANAGEMENT_KEY);
    } else {
      if (!isActive()) {
        throw new AssertionError();
      }
      resp.failure(new EntityBusyException(id.getClassName(), id.getEntityName(), "entity is busy in sync, retry"));
    }
  }
  
  // TODO:  Make sure that this is actually required in the cases where it is called or if some of these sites are
  //  related to the scope expansion of the legacy "NOOP" request type.
  private void processLegacyNoopMessage(ServerEntityRequest request, ResultCapture resp) {
 // local flush should use MGMT_KEY so the entire pipeline is flushed, everything else can use UNIVERSAL
    int key = request.getAction() == ServerEntityAction.FAILOVER_FLUSH ? ConcurrencyStrategy.MANAGEMENT_KEY : ConcurrencyStrategy.UNIVERSAL_KEY;
    scheduleInOrder(request, resp, MessagePayload.emptyPayload(), resp::complete, key);
  }
//  synchronized here because this method must be mutually exclusive with clearQueue
  private synchronized SchedulingRunnable scheduleInOrder(ServerEntityRequest request, ResultCapture results, MessagePayload payload, Runnable r, int ckey) {
    Trace.activeTrace().log("ManagedEntityImpl.scheduleInOrder");
// this all makes sense because this is only called by the PTH single thread
// deferCleared is cleared by one of the request queues
    if (!DirectExecutionMode.isActivated()) {
      if (isInActiveState) {
        Assert.assertTrue(Thread.currentThread().getName().contains(ServerConfigurationContext.VOLTRON_MESSAGE_STAGE));
      } else {
        Assert.assertTrue(Thread.currentThread().getName().contains(ServerConfigurationContext.PASSIVE_REPLICATION_STAGE) ||
          Thread.currentThread().getName().contains(ServerConfigurationContext.L2_STATE_CHANGE_STAGE));
      }
    }
    
    SchedulingRunnable next = new SchedulingRunnable(request, payload, r, ckey);
    if (logger.isDebugEnabled()) {
      logger.debug("Scheduling " + next.request.getAction() + " on " + getID() + ":" + getConsumerID());
    }
    
    if (isActive()) {
// only if this is active is waiting required.  This is set to wait for the 
// passives to complete before presenting results back to the client
      results.setWaitFor(next::waitForPassives);
    }
    
    for (SchedulingRunnable msg : runnables) {
      if (logger.isDebugEnabled()) {
        logger.debug("Starting " + msg.request.getAction() + " on " + getID() + ":" + getConsumerID());
      }
      msg.start();
    }

    if (!runnables.offer(next)) {
      if (logger.isDebugEnabled()) {
        logger.debug("Starting offered " + next.request.getAction() + " on " + getID() + ":" + getConsumerID());
      }
      Assert.assertTrue(next, runnables.isEmpty() && runnables.deferCleared);
      next.start();
    }

    return next;
  }
//  synchronized here because this method must be mutually exclusive with scheduleInOrder
  @Override
  public synchronized boolean clearQueue() {
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
    Trace.activeTrace().log("ManagedEntityImpl.processInvokeRequest");
    if (isInActiveState) {
      try {
        key = this.concurrencyStrategy.concurrencyKey(message.decodeMessage(raw->this.codec.decodeMessage(raw)));
      } catch (MessageCodecException codec) {
        // use the universal key because this is going to result in error downstream
        key = ConcurrencyStrategy.UNIVERSAL_KEY;
      }
    }
    int locked = key;
    scheduleInOrder(request, response, message, ()->invoke(request, response, message, locked), locked);
  }

  private void processSyncCreateMessage(ServerEntityRequest sync, ResultCapture response, MessagePayload syncPayload) {
    ServerEntityAction action = sync.getAction();
    Assert.assertTrue(action == ServerEntityAction.RECEIVE_SYNC_CREATE_ENTITY);
    scheduleInOrder(sync, response, syncPayload, 
        ()-> {
          invokeLifecycleOperation(sync, syncPayload, response);
        }, 
      ConcurrencyStrategy.MANAGEMENT_KEY
    );
  }

  private void processSyncStartEndMessage(ServerEntityRequest sync, ResultCapture response, MessagePayload syncPayload) {
    ServerEntityAction action = sync.getAction();
    Assert.assertTrue(
        action == ServerEntityAction.RECEIVE_SYNC_ENTITY_START_SYNCING
        || action == ServerEntityAction.RECEIVE_SYNC_ENTITY_END
    );
    scheduleInOrder(sync, response, syncPayload, 
        ()-> {
          invokeLifecycleOperation(sync, syncPayload, response);
        }, 
      ConcurrencyStrategy.MANAGEMENT_KEY
    );
  }

  private void processSyncPayloadOtherMessage(ServerEntityRequest sync, ResultCapture response, MessagePayload syncPayload, int concurrencyKey) {
    ServerEntityAction action = sync.getAction();
    Assert.assertTrue(action != ServerEntityAction.RECEIVE_SYNC_CREATE_ENTITY);
    Assert.assertTrue(action != ServerEntityAction.RECEIVE_SYNC_ENTITY_START_SYNCING);
    Assert.assertTrue(action != ServerEntityAction.RECEIVE_SYNC_ENTITY_END);
    
    if (action == ServerEntityAction.RECEIVE_SYNC_PAYLOAD) {
      scheduleInOrder(sync, response, syncPayload, 
        ()-> {
          invoke(sync, response, syncPayload, concurrencyKey);
        }, concurrencyKey);
    } else {
      scheduleInOrder(sync, response, syncPayload, 
        ()->{
          invoke(sync, response, null, concurrencyKey);
        }, concurrencyKey);
    }
  }

  @Override
  public Map<String, Object> getState() {
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("fetchID", this.fetchID.toString());
    props.put("entityID", this.id.toString());
    props.put("consumerID", this.consumerID);
    props.put("referenceCount", this.clientReferenceCount);
    props.put("waitForExclusive", this.runnables.getState());
    props.put("retirement", this.retirementManager.getState());
    props.put("destroyed", this.isDestroyed);
    props.put("active", this.isInActiveState);
    props.put("removeable", this.isRemoveable());
    MappedStateCollector mapped = new MappedStateCollector(this.id.getEntityName());
    try {
      if(activeServerEntity != null) {
        activeServerEntity.addStateTo(mapped);
      }

      if(passiveServerEntity != null) {
        passiveServerEntity.addStateTo(mapped);
      }
    } catch (Throwable t) {
      logger.warn("unable to collect state for " + getID(), t);
      props.put("unable to collect state for " + getID(), t.getLocalizedMessage());
    }
    props.put("entityState", mapped.getMap());
    return props;
  }
  
  private byte[] encodeResponse(EntityResponse payload, ResultCapture capture) {
    try {
      return payload == null ? new byte[0] : codec.encodeResponse(payload);
    } catch (MessageCodecException ce) {
      capture.failure(new EntityServerException(id.getClassName(), id.getEntityName(), "error encoding response", ce));
    }
    return null;
  }
  
  private EntityMessage decodeMessage(MessagePayload payload, ResultCapture capture) {
    try {
      return payload.decodeMessage(r->codec.decodeMessage(r));
    } catch (MessageCodecException ce) {
      capture.failure(new EntityServerException(id.getClassName(), id.getEntityName(), "error parsing message", ce));
    }
    return null;
  }
  
  private void invokeLifecycleOperation(final ServerEntityRequest request, MessagePayload payload, ResultCapture resp) {
    Trace trace = new Trace(request.getTraceID(), "ManagedEntityImpl.invokeLifecycleOperation");
    trace.start();
    Lock read = reconnectAccessLock.readLock();
    logger.info("Client:" + request.getNodeID() + ":" + request.getClientInstance() + " Invoking lifecycle " + request.getAction() + " on " + getID() + ":" + this.fetchID);
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
        case RECEIVE_SYNC_CREATE_ENTITY:
          // Update our reference count.
          this.resetReferences(payload.getReferenceCount());
          receiveSyncCreateEntity(resp, payload.getRawPayload());
          break;
        case RECEIVE_SYNC_ENTITY_START_SYNCING:
          /// NOTE:  There is currently an assumption that the sync entity start completes after the entity has been
          //  created but before it is actually told that it will start to sync.  This may be a bug but will be
          //  preserved, for now, to minimize extraneous behavioral changes.
          resp.complete();
          receiveSyncEntityStartSyncing();
          break;
        case RECEIVE_SYNC_ENTITY_END:
          receiveSyncEntityEnd(resp);
          break;
        case DISCONNECT_CLIENT:
          disconnectClientFromEntity(request.getNodeID());
          resp.complete();
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
      EntityServerUncaughtException wrapper = new EntityServerUncaughtException(this.getID().getClassName(), this.getID().getEntityName(), "caught exception during invoke ", e);
      logger.error("caught exception during invoke ", wrapper);
      throw wrapper;
    } finally {
      read.unlock();
      if (this.isInActiveState) {
        interop.finishLifecycle();
      }
    }
    trace.end();
  }
  
  private void disconnectClientFromEntity(ClientID cid) {
    if (isActive()) {
      this.activeServerEntity.notifyDestroyed(new ClientSourceIdImpl(cid.toLong()));
      List<EntityDescriptor> eds = this.clientEntityStateManager.clientDisconnectedFromEntity(cid, this.fetchID);
      eventCollector.clientDisconnectedFromEntity(cid, fetchID, eds);
      eds.forEach(ed->messageSelf.addToSink(new ReferenceMessage(cid, false, ed, null)));
    } else {
      this.passiveServerEntity.notifyDestroyed(new ClientSourceIdImpl(cid.toLong()));
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
    Trace trace = new Trace(request.getTraceID(), "ManagedEntityImpl.invoke");
    trace.start();
    if (request.requiresReceived()) {
      response.waitForReceived(); // waits for received on passives
    }  
    response.received(); // call received locally
    
    Lock read = reconnectAccessLock.readLock();
      try {
        read.lock();
        if (logger.isDebugEnabled()) {
          logger.debug(request.getAction() + " on " + getID() + "/" + concurrencyKey + " with " + message);
        }
        switch (request.getAction()) {
          case INVOKE_ACTION:
            Optional.ofNullable(decodeMessage(message, response))
                .ifPresent(em->performAction(request, em , response, concurrencyKey));
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
          case LOCAL_FLUSH:
          case LOCAL_FLUSH_AND_SYNC:
          case ORDER_PLACEHOLDER_ONLY:
            // These types are all for message order - none of them come in through this invoke path.
            throw new IllegalArgumentException("Flow-only request observed in invoke path: " + request);
          default:
            throw new IllegalArgumentException("Unknown request " + request);
        }
      } catch (Exception e) {
        logger.error("caught exception during invoke ", e);
        throw new RuntimeException(e);
      } finally {
        read.unlock();
      }
      trace.end();
  }
  
  private void receiveSyncCreateEntity(ResultCapture response, byte[] constructor) {
    Assert.assertNull("passiveServerEntity should be null for entity " + this.getID(), this.passiveServerEntity);
//  going to start by building the passive instance
    try {
      createEntity(response, constructor);
    } catch (ConfigurationException ce) {
      String errMsg = "unable to create an entity " + this.getID() + " on passive sync ";
      logger.error(errMsg, ce);
      throw new TCShutdownServerException(errMsg, ce);
    }
  }

  private void receiveSyncEntityStartSyncing() {
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
    try {

      this.passiveServerEntity.invokePassive(new InvokeContextImpl(message.getConcurrency()),
                                             message.decodeMessage(raw -> syncCodec.decode(message.getConcurrency(),
                                                                                              raw)));
    } catch (EntityUserException | MessageCodecException e) {
      logger.error("Caught EntityUserException during sync invoke", e);
      throw new RuntimeException("Caught EntityUserException during sync invoke", e);
    }
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
  public synchronized boolean isRemoveable() {
    return this.isDestroyed && runnables.isEmpty() && runnables.deferCleared;
  }

  private void destroyEntity(ServerEntityRequest request, ResultCapture response) {
    CommonServerEntity<EntityMessage, EntityResponse> commonServerEntity = this.isInActiveState
        ? activeServerEntity
        : passiveServerEntity;
    EntityDescriptor entityDescriptor = EntityDescriptor.createDescriptorForLifecycle(id, version);
    if (this.isDestroyed) {
      response.failure(new EntityNotFoundException(entityDescriptor.getEntityID().getClassName(), entityDescriptor.getEntityID().getEntityName()));        
    } else if (null != commonServerEntity) {
      // We want to ensure that nobody somehow has a reference to this entity.
      if (!this.canDelete) {
        Assert.assertTrue(clientReferenceCount < 0);
        response.failure(new VoltronWrapperException(new PermanentEntityException(entityDescriptor.getEntityID().getClassName(), entityDescriptor.getEntityID().getEntityName())));
      } else if (clientReferenceCount == 0) {
        Assert.assertTrue(!isInActiveState || clientEntityStateManager.verifyNoEntityReferences(this.fetchID));
        Assert.assertFalse(this.isDestroyed);
        commonServerEntity.destroy();
        this.retirementManager.entityWasDestroyed();
        notifyEntityDestroyed();
        if (this.isInActiveState) {
          this.activeServerEntity = null;
        } else {
          this.passiveServerEntity = null;
        }
        this.isDestroyed = true;
        eventCollector.entityWasDestroyed(id, consumerID);    
        response.complete();
      } else {
        Assert.assertTrue(!isInActiveState || !clientEntityStateManager.verifyNoEntityReferences(this.fetchID));
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
    notifyEntityDestroyed();
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
    notifyEntityCreated();
    
    reconfigureEntityRequest.complete(oldconfig);
    // Fire the event that the entity was created.
    this.eventCollector.entityWasReloaded(this.getID(), this.consumerID, this.isInActiveState);
  }
    
  private void createEntity(ResultCapture response, byte[] constructorInfo) throws ConfigurationException {
    Trace.activeTrace().log("ManagedEntityImpl.createEntity");

    if (!this.isDestroyed && (this.activeServerEntity != null || this.passiveServerEntity != null)) {
      response.failure(new EntityAlreadyExistsException(this.getID().getClassName(), this.getID().getEntityName()));
//  failed to create, destroyed
      return;
    }
    this.constructorInfo = constructorInfo;
    // Create the appropriate kind of entity, based on our active/passive state.
    if (this.isInActiveState) {
      if (null != this.activeServerEntity) {
        throw new IllegalStateException("Active entity " + id + " already exists.");
      } else {
        ActiveServerEntity<EntityMessage, EntityResponse> tmpEntity = this.factory.createActiveEntity(this.registry, this.constructorInfo);
        tmpEntity.createNew();
        this.activeServerEntity = tmpEntity;
        this.concurrencyStrategy = this.factory.getConcurrencyStrategy(constructorInfo);
        this.executionStrategy = this.factory.getExecutionStrategy(constructorInfo);
      }
    } else {
      if (null != this.passiveServerEntity) {
        throw new IllegalStateException("Passive entity " + id + " already exists.");
      } else {
        PassiveServerEntity<EntityMessage, EntityResponse> tmpEntity = this.factory.createPassiveEntity(this.registry, this.constructorInfo);
        tmpEntity.createNew();
        this.passiveServerEntity = tmpEntity;
        Assert.assertNull(this.concurrencyStrategy);
      }
    }
        
    notifyEntityCreated();
    this.isDestroyed = false;

    eventCollector.entityWasCreated(id, this.consumerID, isInActiveState);
    response.complete();
  }

  private void performSync(ResultCapture response, Set<NodeID> passives, int concurrencyKey) {
    if (!this.isDestroyed) {
      if (this.isInActiveState) {
        if (null == this.activeServerEntity) {
          throw new IllegalStateException("Actions on a non-existent entity.");
        } else {
          // Create the channel which will send the payloads over the wire.
          PassiveSynchronizationChannel<EntityMessage> syncChannel = new EntityMessagePassiveSynchronizationChannelImpl(
            passives,
            concurrencyKey);
        //  start is handled by the sync request that triggered this action
          this.activeServerEntity.synchronizeKeyToPassive(syncChannel, concurrencyKey);
        }
      } else {
        throw new IllegalStateException("syncing a passive entity");
      }
    }
    
    response.complete();
  }
  
  private void performAction(ServerEntityRequest wrappedRequest,
                             EntityMessage message,
                             ResultCapture response,
                             int concurrencyKey) {
    Trace.activeTrace().log("ManagedEntityImpl.performAction");
    Assert.assertNotNull(message);
    ClientDescriptorImpl clientDescriptor = new ClientDescriptorImpl(wrappedRequest.getNodeID(),
                                                                     wrappedRequest.getClientInstance());
    long currentId = wrappedRequest.getTransaction().toLong();
    long oldestId = wrappedRequest.getOldestTransactionOnClient().toLong();

    if (Trace.isTraceEnabled()) {
      Trace.activeTrace().log("invoking " + message);
    }
    if (this.isInActiveState) {
      if (null == this.activeServerEntity) {
        throw new IllegalStateException("Actions on a non-existent entity. active:" + this.isActive() + " " + message.toString());
      } else {
        this.retirementManager.registerWithMessage(message, concurrencyKey, new Retiree() {
          @Override
          public void retired() {
            response.retired();
          }

          @Override
          public TransactionID getTransaction() {
            return wrappedRequest.getTransaction();
          }

          @Override
          public String getTraceID() {
            return wrappedRequest.getTraceID();
          }
        });
        try {
          ExecutionStrategy.Location loc = this.executionStrategy.getExecutionLocation(message);
          if (loc.runOnActive()) {
            Trace trace = Trace.activeTrace().subTrace("invokeActive");
            trace.start();
            EntityResponse resp = this.activeServerEntity.invokeActive(
              new ActiveInvokeContextImpl<>(clientDescriptor, concurrencyKey, oldestId, currentId, 
                  ()->retirementManager.holdMessage(message),
                  (r)->response.message(decodeResponse(r)), 
                  (e)->response.failure(convertException(e)),
                  ()->{
                    // returns true of the message has been completed 
                    // and held count is zero so the message should be retired
                    if (retirementManager.releaseMessage(message)) {
                      retirementManager.retireMessage(message);
                    }
                  }
              ), message);
            byte[] er = encodeResponse(resp, response);
            trace.end();
            if (er != null) {
              response.complete(er);
            }
            retirementManager.retireMessage(message);
          } else {
            response.complete(new byte[0]);
            retirementManager.retireMessage(message);
          }
        } catch (EntityUserException e) {
          //on Active, log error and send the exception to the client - don't crash server
          logger.error("Caught EntityUserException during invoke", e);
          response.failure(new VoltronEntityUserExceptionWrapper(e));
          retirementManager.retireMessage(message);
        }
      }
    } else {
      if (null == this.passiveServerEntity) {
        throw new IllegalStateException("Actions on a non-existent entity. active:" + this.isActive() + " " + message.toString());
      } else {
        try {
          Trace trace = Trace.activeTrace().subTrace("invokePassive");
          trace.start();
          this.passiveServerEntity.invokePassive(
            new InvokeContextImpl(new ClientSourceIdImpl(wrappedRequest.getNodeID().toLong()),
                                  concurrencyKey,
                                  oldestId,
                                  currentId),
            message);
          trace.end();
        } catch (EntityUserException e) {
          //on passives, just log the exception - don't crash server
          logger.error("Caught EntityUserException during invoke", e);
        }
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
  
  private EntityException convertException(Exception e) {
    if (e instanceof EntityException) {
      return (EntityException)e;
    } else {
      return new EntityServerException(id.getClassName(), id.getEntityName(), e.getMessage(), e);
    }
  }
  
  private byte[] decodeResponse(EntityResponse response) {
    try {
      return codec.encodeResponse(response);
    } catch (MessageCodecException ce) {
      throw new RuntimeException(ce);
    }
  }

  @Override
  public void loadEntity(byte[] configuration) throws ConfigurationException {
    this.loadExisting(configuration);
  }

  private void getEntity(ServerEntityRequest getEntityRequest, ResultCapture response, byte[] extendedData) {
    if (this.isDestroyed) {
      response.failure(new EntityNotFoundException(id.getClassName(), id.getEntityName()));
    } else {
      if (canDelete) {
        clientReferenceCount += 1;
        Assert.assertTrue(clientReferenceCount > 0);
      }
      // The FETCH can only come directly from a client so we can down-cast.
      ClientID clientID = (ClientID) getEntityRequest.getNodeID();
      ClientDescriptorImpl descriptor = new ClientDescriptorImpl(clientID, getEntityRequest.getClientInstance());
      boolean added = clientEntityStateManager.addReference(descriptor, this.fetchID);
      if (this.isInActiveState) {
        Assert.assertTrue(added);
        // Fire the event that the client fetched the entity.
        this.eventCollector.clientDidFetchEntity(clientID, this.id, this.consumerID, getEntityRequest.getClientInstance());
        // finally notify the entity that it was fetched
        this.activeServerEntity.connected(descriptor);
        if (getEntityRequest.getTransaction().equals(TransactionID.NULL_ID)) {
//   this is a reconnection, handle the extended reconnect data
          try {
            if (this.reconnect == null) {
              throw new ReconnectRejectedException("no reconnect handler registered");
            } else {
              this.reconnect.handleReconnect(descriptor, extendedData);
            }
          } catch (ReconnectRejectedException rejected) {
            response.failure(new EntityServerException(this.getID().getClassName(), this.getID().getEntityName(), rejected.getMessage(), rejected));
            return;
          } catch (Exception e) {
//  something happened during reconnection, force a disconnection, see ProcessTransactionHandler.disconnectClientDueToFailure for handling
            logger.warn("unexpected exception.  rejecting reconnection of " + descriptor.getNodeID() + " to " + this.id, e);
            response.failure(new EntityServerException(this.getID().getClassName(), this.getID().getEntityName(), e.getMessage(), new ReconnectRejectedException(e.getMessage(), e)));
            return;
          } 
        }
      } else {
//  clientEntityStateManager is only tracking knowledge of clients on passives
//  it is allowed to be unexact due to passive failover and reference counts 
//  being part of the sync process
      }
      ByteBuffer buffer = ByteBuffer.allocate(this.constructorInfo.length + Long.BYTES);
      buffer.putLong(this.consumerID);
      buffer.put(this.constructorInfo);
      response.complete(buffer.array());
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

      ClientID clientID = (ClientID) request.getNodeID();
      ClientDescriptorImpl clientInstance = new ClientDescriptorImpl(clientID, request.getClientInstance());
      boolean removed = clientEntityStateManager.removeReference(clientInstance);

      if (this.isInActiveState) {
        Assert.assertTrue(removed);
        this.activeServerEntity.disconnected(clientInstance);
        // Fire the event that the client released the entity.
        this.eventCollector.clientDidReleaseEntity(clientID, this.id, this.consumerID, request.getClientInstance());
      } else {
//  clientEntityStateManager is only tracking knowledge of clients on passives
//  it is allowed to be unexact due to passive failover and reference counts 
//  being part of the sync process
      }
      response.complete();
    }
  }
  
  @Override
  public void resetReferences(int count) {
    if (canDelete) {
      this.clientReferenceCount = count;
    } else {
      Assert.assertEquals(this.clientReferenceCount, ManagedEntityImpl.UNDELETABLE_ENTITY);
    }
  }
  
  @Override
  public Runnable promoteEntity() throws ConfigurationException {
    // Can't enter active state twice.
    Assert.assertFalse(this.isInActiveState);
    Assert.assertNull(this.activeServerEntity);
//  checking destroyed here should be fine.  no other threads should be touching during promote
    this.isInActiveState = true;
// any clients, previously connected will reconnect or not.  Failed reconnects will be cleaned
// up on passives 
    if (canDelete) {
      this.clientReferenceCount = 0;
    } else {
      Assert.assertEquals(this.clientReferenceCount, ManagedEntityImpl.UNDELETABLE_ENTITY);
    }
    if (!this.isDestroyed) {
      logger.info("Promoting " + getID() + " to active entity");
      if (null != this.passiveServerEntity) {
        notifyEntityDestroyed();
        this.passiveServerEntity = null;
        this.activeServerEntity = factory.createActiveEntity(this.registry, this.constructorInfo);
        this.concurrencyStrategy = factory.getConcurrencyStrategy(this.constructorInfo);
        this.executionStrategy = factory.getExecutionStrategy(this.constructorInfo);
        this.activeServerEntity.loadExisting();
        notifyEntityCreated();
        // Fire the event that the entity was reloaded.
        this.eventCollector.entityWasReloaded(this.getID(), this.consumerID, true);
        
        reconnect = this.activeServerEntity.startReconnect();
        if (reconnect != null) {
          return ()->{
            if (reconnect != null) {
              reconnect.close();
              reconnect = null;
            }
          };
        }
      } else {
        throw new IllegalStateException("no entity to promote");
      }
    }
    return null;
  }

  @Override
  public void sync(NodeID passive) {
//  this is simply a barrier to make sure all actions are flushed before sync is started (hence, it has a null passive).
    PassiveSyncServerEntityRequest req = new PassiveSyncServerEntityRequest(passive);
// wait for future is ok, occuring on sync executor thread
    BarrierCompletion syncStart = new BarrierCompletion();
    this.executor.scheduleRequest(interop.isSyncing(), this.id, this.version, this.fetchID, new ServerEntityRequestImpl(ClientInstanceID.NULL_ID, ServerEntityAction.LOCAL_FLUSH_AND_SYNC, ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, false), MessagePayload.emptyPayload(), (w)-> { 
        Assert.assertTrue(this.isInActiveState);
        if (!this.isDestroyed) {
          executor.scheduleSync(SyncReplicationActivity.createStartEntityMessage(id, version, fetchID, constructorInfo, canDelete ? this.clientReferenceCount : ManagedEntity.UNDELETABLE_ENTITY), passive).waitForCompleted();
        }
        interop.syncStarted();
        syncStart.complete();
      }, true, ConcurrencyStrategy.MANAGEMENT_KEY);
    //  wait for completed above waits for acknowledgment from the passive
    //  waitForCompletion below waits for completion of the local request processor
    syncStart.waitForCompletion();
// wait for future is ok, occuring on sync executor thread
    try {
      if (!this.isDestroyed) {
        for (Integer concurrency : concurrencyStrategy.getKeysForSynchronization()) {
    // make sure that concurrency key is in the valid range
          //  MGMT_KEY and UNIVERSAL keys are not valid for sync
          Assert.assertTrue(concurrency > 0);  

          if (activeServerEntity != null) {
            activeServerEntity.prepareKeyForSynchronizeOnPassive(new EntityMessagePassiveSynchronizationChannelImpl(Collections.singleton(passive), concurrency), concurrency);
          }
          // We don't actually use the message in the direct strategy so this is safe.
          //  don't care about the result
          BarrierCompletion sectionComplete = new BarrierCompletion();
          this.executor.scheduleRequest(interop.isSyncing(), this.id, this.version, this.fetchID, req, MessagePayload.emptyPayload(),  (w)->invoke(req, new ResultCaptureImpl(null, result->sectionComplete.complete(), null, exception->{throw new RuntimeException("bad message", exception);}), MessagePayload.emptyPayload(), concurrency), true, concurrency);

        //  wait for completed above waits for acknowledgment from the passive
        //  waitForCompletion below waits for completion of the local request processor
          sectionComplete.waitForCompletion();
          executor.scheduleSync(SyncReplicationActivity.createEndEntityKeyMessage(id, version, fetchID, concurrency), passive).waitForCompleted();
        }
  //  end passive sync for an entity
  // wait for future is ok, occuring on sync executor thread
        executor.scheduleSync(SyncReplicationActivity.createEndEntityMessage(id, version, fetchID), passive).waitForCompleted();
      }
    } finally {
      BarrierCompletion syncComplete = new BarrierCompletion();
      this.executor.scheduleRequest(interop.isSyncing(), this.id, this.version, this.fetchID, 
        new ServerEntityRequestImpl(ClientInstanceID.NULL_ID, ServerEntityAction.LOCAL_FLUSH_AND_SYNC, ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, false), 
        MessagePayload.emptyPayload(), 
        (w)-> { 
          Assert.assertTrue(this.isInActiveState);
          interop.syncFinished();
          syncComplete.complete();
        }, true, ConcurrencyStrategy.MANAGEMENT_KEY);
      //  wait for completed above waits for acknowledgment from the passive
      //  waitForCompletion below waits for completion of the local request processor
      syncComplete.waitForCompletion();
    }
  }  

  @Override
  public SyncReplicationActivity.EntityCreationTuple startSync() {
    interop.startSync();
    clearQueue();
    if (!this.isDestroyed) {
      return new SyncReplicationActivity.EntityCreationTuple(this.id, this.version, this.consumerID, this.constructorInfo, canDelete);
    } else {
      return null;
    }
  }

  @Override
  public long getConsumerID() {
    return this.consumerID;
  }

  @Override
  public void addLifecycleListener(LifecycleListener listener) {
    this.createListener.add(listener);
  }
  
  private void loadExisting(byte[] constructorInfo) throws ConfigurationException {
    logger.info("loadExisting entity: " + getID());
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
        notifyEntityCreated();
// Fire the event that the entity was reloaded.  This should only happen on active entities.  Passives with either transition to active soon or be destroyed
        this.eventCollector.entityWasReloaded(this.getID(), this.consumerID, this.isInActiveState);
      }
    } else {
      if (null != this.passiveServerEntity) {
        throw new IllegalStateException("Passive entity " + id + " already exists.");
      } else {
        this.passiveServerEntity = factory.createPassiveEntity(registry, constructorInfo);
        notifyEntityCreated();
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
    public boolean requiresReceived() {
      return false;
    }

    @Override
    public Set<NodeID> replicateTo(Set<NodeID> passives) {
      return passive == null ? Collections.emptySet() : Collections.singleton(passive);
    }

    @Override
    public ClientInstanceID getClientInstance() {
      return ClientInstanceID.NULL_ID;
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
  
  private class SchedulingRunnable implements Consumer<ActivePassiveAckWaiter> {
    private final ServerEntityRequest request;
    private final MessagePayload payload;
    private final Runnable original;
    private final int concurrency;
    private ActivePassiveAckWaiter  waitFor;

    public SchedulingRunnable(ServerEntityRequest request, MessagePayload payload, Runnable r, int concurrency) {
      this.request = request;
      this.payload = payload;
      this.original = r;
      this.concurrency = concurrency;
    }
        
    private void start() {
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
        try {
          ExecutionStrategy.Location loc = executionStrategy.getExecutionLocation(payload.decodeMessage(raw->codec.decodeMessage(raw)));
          if (loc != ExecutionStrategy.Location.IGNORE) {
            replicate = loc.runOnPassive();
          }
        } catch (MessageCodecException codec) {
          replicate = false;
        }
      } 
      executor.scheduleRequest(interop.isSyncing(), id, version, fetchID, request, payload, this, replicate, concurrency);
    }
    
    private synchronized void setWaitFor(ActivePassiveAckWaiter waiter) {
      this.waitFor = waiter;
      notifyAll();
    }
    
    public void accept(ActivePassiveAckWaiter waiter) {
      try {
        setWaitFor(waiter);
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
        ServerEntityAction action = request.getAction();
        if (request.getAction() == ServerEntityAction.CREATE_ENTITY && isDestroyed()) {
        //  must be a failed create, mimic destroy
          action = ServerEntityAction.DESTROY_ENTITY;
        }
        flushLocalPipeline.completed(id, fetchID, action);
      }
    }
    
    private synchronized ActivePassiveAckWaiter waitForPassives() {
      try {
        while (waitFor == null) {
          this.wait();
        }
        return waitFor;
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public String toString() {
      return "SchedulingRunnable{" + "request=" + request + ", payload=" + payload.getDebugId() + ", concurrency=" + concurrency + ", waitFor=" + waitFor + '}';
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
    
    private Map<String, Object> getState() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("deferring", !this.deferCleared);
      map.put("queue", queue.stream().map(String::valueOf).collect(Collectors.toList()));
      return map;
    }
  }

  private class EntityMessagePassiveSynchronizationChannelImpl implements PassiveSynchronizationChannel<EntityMessage> {
    private final List<NodeID> passives;
    private final int concurrencyKey;

    public EntityMessagePassiveSynchronizationChannelImpl(Collection<NodeID> passives, int concurrencyKey) {
      this.passives = new ArrayList<>(passives);
      Collections.sort(this.passives);
      this.concurrencyKey = concurrencyKey;
    }

    @Override
//  TODO:  what should be done about exception handling?
    public void synchronizeToPassive(EntityMessage payload) {
      for (NodeID passive : passives) {
        try {
          byte[] message = syncCodec.encode(concurrencyKey, payload);
          ActivePassiveAckWaiter waiter = executor.scheduleSync(SyncReplicationActivity.createPayloadMessage(id, version, fetchID,
                                             concurrencyKey, message, ""), passive);
          //  wait for the passive to receive before sending the next
          waiter.waitForReceived();
        } catch (MessageCodecException ce) {
          throw new RuntimeException(ce);
        }
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      EntityMessagePassiveSynchronizationChannelImpl channel = (EntityMessagePassiveSynchronizationChannelImpl) o;

      return passives.equals(channel.passives);
    }

    @Override
    public int hashCode() {
      int result = passives.hashCode();
      return result;
    }
  }
}
