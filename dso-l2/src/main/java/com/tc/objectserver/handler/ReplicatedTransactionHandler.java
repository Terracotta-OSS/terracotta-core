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
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.objectserver.entity.MessagePayload;
import com.tc.l2.msg.ReplicationAckTuple;
import com.tc.l2.msg.ReplicationMessage;
import com.tc.l2.msg.ReplicationMessageAck;
import com.tc.l2.msg.ReplicationResultCode;
import com.tc.l2.msg.SyncReplicationActivity;
import com.tc.l2.msg.SyncReplicationActivity.ActivityType;
import com.tc.l2.state.ServerMode;
import com.tc.l2.state.StateManager;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.FetchID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.api.ResultCapture;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.entity.BarrierCompletion;
import com.tc.objectserver.entity.NoopResultCapture;
import com.tc.objectserver.entity.PassiveResultCapture;
import com.tc.objectserver.entity.PlatformEntity;
import com.tc.objectserver.entity.ResultCaptureImpl;
import com.tc.objectserver.persistence.Persistor;
import com.tc.properties.TCPropertiesImpl;
import com.tc.tracing.Trace;
import com.tc.util.Assert;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.exception.EntityException;


public class ReplicatedTransactionHandler {
  private static final int DEFAULT_BATCH_LIMIT = 64;
  private static final int DEFAULT_INFLIGHT_MESSAGES = 1;
  private static final int maximumBatchSize = TCPropertiesImpl.getProperties().getInt("passive-active.batchsize", DEFAULT_BATCH_LIMIT);
  private static final int idealMessagesInFlight = TCPropertiesImpl.getProperties().getInt("passive-active.inflight", DEFAULT_INFLIGHT_MESSAGES);

  private static final Logger PLOGGER = LoggerFactory.getLogger(MessagePayload.class);
  private static final Logger LOGGER = LoggerFactory.getLogger(ReplicatedTransactionHandler.class);

  private final EntityManager entityManager;
  private final Persistor persistor;
  private final GroupManager<AbstractGroupMessage> groupManager;
  private final StateManager stateManager;
  private final ManagedEntity platform;
  
  private final SyncState state = new SyncState();
  
  // This MUST be manipulated under lock - it is the batch of ack messages we are accumulating until the network is ready for another message.
  private NodeID cachedMessageAckFrom;
  private GroupMessageBatchContext<ReplicationMessageAck, ReplicationAckTuple> cachedBatchAck;
  private final Sink<Runnable> sentToActive;
  
  public ReplicatedTransactionHandler(StateManager state, Stage<Runnable> sendToActive, Persistor persistor, 
      EntityManager manager, GroupManager<AbstractGroupMessage> groupManager) {
    this.stateManager = state;
    this.sentToActive = sendToActive.getSink();
    this.entityManager = manager;
    this.persistor = persistor;
    this.groupManager = groupManager;
    try {
      platform = entityManager.getEntity(EntityDescriptor.createDescriptorForLifecycle(PlatformEntity.PLATFORM_ID, PlatformEntity.VERSION)).get();
    } catch (EntityException ee) {
      throw new RuntimeException(ee);
    }
  }

  private final EventHandler<ReplicationMessage> eventHorizon = new AbstractEventHandler<ReplicationMessage>() {
    @Override
    public void handleEvent(ReplicationMessage message) throws EventHandlerException {
      try {
        processMessage(message);
      } catch (Throwable t) {
        // We don't expect to see an exception executing a replicated message.
        // TODO:  Find a better way to handle this error.
        throw Assert.failure("Unexpected exception executing replicated message", t);
      }
    }

    @Override
    protected void initialize(ConfigurationContext context) {
      ServerConfigurationContext scxt = (ServerConfigurationContext)context;
  //  when this spins up, send  request to active and ask for sync
      scxt.getL2Coordinator().getReplicatedClusterStateManager().setCurrentState(scxt.getL2Coordinator().getStateManager().getCurrentMode().getState());
      if (stateManager.getCurrentMode() == ServerMode.UNINITIALIZED) {
        requestPassiveSync();
      }
    }

    @Override
    public void destroy() {
      ServerEntityRequest req = new ServerEntityRequest() {
        @Override
        public ServerEntityAction getAction() {
          return ServerEntityAction.FAILOVER_FLUSH;
        }

        @Override
        public ClientID getNodeID() {
          return ClientID.NULL_ID;
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
        public ClientInstanceID getClientInstance() {
          return ClientInstanceID.NULL_ID;
        }

        @Override
        public boolean requiresReceived() {
          return false;
        }

        @Override
        public Set<NodeID> replicateTo(Set<NodeID> passives) {
          return Collections.emptySet();
        }
      };
  //    MGMT_KEY because the request processor needs to be flushed
      for (ManagedEntity me : entityManager.getAll()) {
        BarrierCompletion latch = new BarrierCompletion();
        me.clearQueue();
        me.addRequestMessage(req,
            MessagePayload.emptyPayload(), 
            new ResultCaptureImpl(null, (result)->latch.complete(), null, exception->Assert.fail()));
        latch.waitForCompletion();
      }
      BarrierCompletion latch = new BarrierCompletion();
      platform.addRequestMessage(req, MessagePayload.emptyPayload(), 
          new ResultCaptureImpl(null, (result)->latch.complete(), null, exception->Assert.fail()));

    }    
  };

  public EventHandler<ReplicationMessage> getEventHandler() {
    return eventHorizon;
  }

  private void processMessage(ReplicationMessage rep) throws EntityException {
    if (PLOGGER.isDebugEnabled()) {
      PLOGGER.debug("RECEIVED:" + rep.getDebugId());
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("BATCH:" + rep.getSequenceID());
    }
    ServerID activeSender = (ServerID) rep.messageFrom();
    for (SyncReplicationActivity activity : rep.getActivities()) {
      EntityID eid = null;
      if (activity.getActivityType() != ActivityType.SYNC_BEGIN) {
        Optional<ManagedEntity> opt = entityManager.getEntity(EntityDescriptor.createDescriptorForInvoke(activity.getFetchID(), activity.getClientInstanceID()));
        eid = opt.map(ManagedEntity::getID).orElse(activity.getEntityID());
        Long fid = opt.map(ManagedEntity::getConsumerID).orElse(activity.getFetchID().toLong());
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("RECEIVING:" + eid + ":" + fid + " " + activity.getActivityType() + " " + activity.getActivityID().id);
        }
      }
      if (activity.isSyncActivity()) {
        if (SyncReplicationActivity.ActivityType.SYNC_BEGIN == activity.getActivityType()) {
          syncBeginEntityListReceived(activeSender, activity);
        } else {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Sync:" + eid + " " + activity.getActivityType());
          }
          syncActivityReceived(activeSender, activity);
        }
      } else {
        if (state.ignore(activity)) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Ignoring:" + eid + " " + activity.getActivityType());
          }
          acknowledge(activeSender, activity, ReplicationResultCode.NONE);
        } else if (state.defer(activeSender, activity)) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Deferring:" + eid + " " + activity.getActivityType());
          }
        } else {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Applying:" + eid + " " + activity.getActivityType());
          }
          replicatedActivityReceived(activeSender, activity);
        }
      }
    }
  }

  private void syncBeginEntityListReceived(ServerID activeSender, SyncReplicationActivity activity) throws EntityException {
    ackReceived(activeSender, activity, null);
    beforeSyncAction(activity);
    
    // In this case, we want to createCapture all the provided entities.
    SyncReplicationActivity.EntityCreationTuple[] entityTuples = activity.getEntitiesToCreateForSync();
    Assert.assertNotNull(entityTuples);
    // Note that these are provided in the order they must be instantiated so just walk the list.
    for (SyncReplicationActivity.EntityCreationTuple tuple : entityTuples) {
      EntityID eid = tuple.id;
      long version = tuple.version;
      long consumerID = tuple.consumerID;
      byte[] config = tuple.configPayload;
      boolean canDelete = tuple.canDelete;
      
      if (!this.entityManager.getEntity(EntityDescriptor.createDescriptorForLifecycle(eid, version)).isPresent()) {
        this.entityManager.createEntity(eid, version, consumerID, canDelete);
        this.persistor.getEntityPersistor().entityCreatedNoJournal(eid, version, consumerID, canDelete, config);
      } else {
        Assert.fail("this entity should not be here");
      }
    }
    afterSyncAction(activity);
    // This is somewhat strange in that these entities won't actually contain any data or receive replicated messages
    //  until the SYNC_ENTITY_BEGIN for this specific entity is received.
    acknowledge(activeSender, activity, ReplicationResultCode.SUCCESS);
  }

//  don't need to worry about resends here for lifecycle messages.  active will filer them  
  private void replicatedActivityReceived(ServerID activeSender, SyncReplicationActivity activity) throws EntityException {
    Trace trace = new Trace(String.valueOf(activity.getActivityID().id), "Replication");
    trace.start();
    ClientID sourceNodeID = activity.getSource();
    TransactionID transactionID = activity.getTransactionID();
    TransactionID oldestTransactionOnClient = activity.getOldestTransactionOnClient();

    Future<Void> tmpFuture = null;
    // Note that we only want to persist the messages with a true sourceNodeID.  Synthetic invocations and sync messages
    // don't have one (although sync messages shouldn't come down this path).
    if (sourceNodeID != null && !sourceNodeID.isNull() && transactionID.isValid()) {
      Assert.assertTrue(oldestTransactionOnClient.isValid());
      tmpFuture = this.persistor.getTransactionOrderPersistor().updateWithNewMessage(sourceNodeID, transactionID,oldestTransactionOnClient);
    }

    final Future<Void> transactionOrderPersistenceFuture = tmpFuture;

    byte[] extendedData = activity.getExtendedData();

    // Create the request, since it is how we will generically return complete.
    ServerEntityRequest request = activityToLocalRequest(activity);
    // If we satisfied this as a known re-send, don't add the request to the entity.
    if (request.getAction() == ServerEntityAction.CREATE_ENTITY) {
// The common pattern for this is to pass an empty array on success ("found") or an exception on failure ("not found").
      this.persistor.getEntityPersistor().setNextConsumerID(activity.getFetchID().toLong());
      try {
        // TODO:  When a permanent entity is being synced, should we be creating it or ensuring it already exists?
        boolean canDelete = !sourceNodeID.isNull();
        ManagedEntity temp = entityManager.createEntity(activity.getEntityID(), activity.getVersion(), activity.getFetchID().toLong(), canDelete);
        Assert.assertTrue(temp.getConsumerID() + " == " + activity.getFetchID().toLong(), temp.getConsumerID() == activity.getFetchID().toLong());
        temp.addRequestMessage(request, MessagePayload.rawDataOnly(extendedData), createCapture(()->ackReceived(activeSender, activity, transactionOrderPersistenceFuture),
          (result) -> {
            if (canDelete) {
              this.persistor.getEntityPersistor().entityCreated(sourceNodeID, transactionID.toLong(), oldestTransactionOnClient.toLong(), activity.getEntityID(), activity.getVersion(), activity.getFetchID().toLong(), canDelete, extendedData);
              acknowledge(activeSender, activity, ReplicationResultCode.SUCCESS);
            } else {
              this.persistor.getEntityPersistor().entityCreatedNoJournal(activity.getEntityID(), activity.getVersion(), activity.getFetchID().toLong(), canDelete, extendedData);
              acknowledge(activeSender, activity, ReplicationResultCode.SUCCESS);
            }
          }, (exception) -> {
            this.persistor.getEntityPersistor().entityCreateFailed(activity.getEntityID(), sourceNodeID, transactionID.toLong(), oldestTransactionOnClient.toLong(), exception);
            LOGGER.debug("create fail:" + temp.getID());
            acknowledge(activeSender, activity, ReplicationResultCode.FAIL);
          }));
      } catch (EntityException ee) {
        acknowledge(activeSender, activity, ReplicationResultCode.FAIL);
        this.persistor.getEntityPersistor().entityCreateFailed(activity.getEntityID(), sourceNodeID, transactionID.toLong(), oldestTransactionOnClient.toLong(), ee);
      }
    } else {
    // At this point, we can now look up the managed entity (used later).
      Assert.assertFalse(activity.getActivityType(), activity.getFetchID().isNull());
      EntityDescriptor desp = EntityDescriptor.createDescriptorForInvoke(activity.getFetchID(), ClientInstanceID.NULL_ID);
      Optional<ManagedEntity> entity = entityManager.getEntity(desp);
      if (entity.isPresent()) {
        ManagedEntity entityInstance = entity.get();
        MessagePayload payload = MessagePayload.syncPayloadNormal(extendedData, activity.getConcurrency());
        if (null != request.getAction()) switch (request.getAction()) {
          case RECONFIGURE_ENTITY:  
            entityInstance.addRequestMessage(request, payload, createCapture(()->ackReceived(activeSender, activity, transactionOrderPersistenceFuture),
              (result)->{
                //  store the new configuration in the persistor
                this.persistor.getEntityPersistor().entityReconfigureSucceeded(sourceNodeID, transactionID.toLong(), oldestTransactionOnClient.toLong(), entityInstance.getID(), entityInstance.getVersion(), payload.getRawPayload());
                acknowledge(activeSender, activity, ReplicationResultCode.SUCCESS);
              } , (exception) -> {
                this.persistor.getEntityPersistor().entityReconfigureFailed(sourceNodeID, transactionID.toLong(), oldestTransactionOnClient.toLong(), exception);
                acknowledge(activeSender, activity, ReplicationResultCode.FAIL);
              }));
            break;
          case DESTROY_ENTITY:
            entityInstance.addRequestMessage(request, payload, createCapture(()->ackReceived(activeSender, activity, transactionOrderPersistenceFuture),
              (result)-> {
                this.persistor.getEntityPersistor().entityDestroyed(sourceNodeID, transactionID.toLong(), oldestTransactionOnClient.toLong(), entityInstance.getID());
                acknowledge(activeSender, activity, ReplicationResultCode.SUCCESS);
              }, (exception) -> {
               this.persistor.getEntityPersistor().entityDestroyFailed(sourceNodeID, transactionID.toLong(), oldestTransactionOnClient.toLong(), exception);
                LOGGER.debug("destroy fail:" + entityInstance.getID());
               acknowledge(activeSender, activity, ReplicationResultCode.FAIL);
              }));
            break;
          case FETCH_ENTITY:
          case RELEASE_ENTITY:
            entityInstance.addRequestMessage(request, payload, createCapture(()->ackReceived(activeSender, activity, transactionOrderPersistenceFuture),
              (result)-> {
                acknowledge(activeSender, activity, ReplicationResultCode.SUCCESS);
              }, (exception) -> {
                LOGGER.debug("fetch/release fail:" + entityInstance.getID());
                acknowledge(activeSender, activity, ReplicationResultCode.FAIL);
              }));
            break;
          case MANAGED_ENTITY_GC:
            if (entityInstance.isRemoveable()) {
        // if the entity is removeable, remove it from the system and don't schedule anything
              LOGGER.debug("removing " + entityInstance.getID());
              entityManager.removeDestroyed(activity.getFetchID());
              break;
            } else {
              //  fallthrough
            }
          case FAILOVER_FLUSH:
            // will cause a MGMT_KEY flush in the entity so any actions are through the system
            // before failing over to the passive
            entityInstance.addRequestMessage(request, payload, new NoopResultCapture());
            break;
          case ORDER_PLACEHOLDER_ONLY:
            // go ahead and ack right away and don't schedule, no need, work is done
            acknowledge(activeSender, activity, ReplicationResultCode.SUCCESS);
            break;
          default:
            entityInstance.addRequestMessage(request, payload, createCapture(()->ackReceived(activeSender, activity, transactionOrderPersistenceFuture),
                (result)-> acknowledge(activeSender, activity, ReplicationResultCode.SUCCESS), 
                (exception) -> acknowledge(activeSender, activity, ReplicationResultCode.FAIL)));
            break;
        }
      } else {
   //  fail, just ack
        acknowledge(activeSender, activity, ReplicationResultCode.FAIL);
      }
    }
    trace.end();
  }
  
  private ResultCapture createCapture(Runnable received, Consumer<byte[]> completed, Consumer<EntityException> failure) {
    return new PassiveResultCapture(received, completed, failure);
  }
  
  private void establishNewPassive() {
    entityManager.resetReferences();
  }
  
  private void requestPassiveSync() {
    NodeID node = stateManager.getActiveNodeID();
    Assert.assertTrue(entityManager.getAll().stream().allMatch((e)->e.getID().equals(PlatformEntity.PLATFORM_ID)));
    moveToPassiveUnitialized(node);
    try {
      LOGGER.info("Requesting Passive Sync from " + node);
      groupManager.sendTo(node, ReplicationMessageAck.createSyncRequestMessage());
    } catch (GroupException ge) {
      LOGGER.warn("can't request passive sync", ge);
    }
  }  
  
  private void syncActivityReceived(ServerID activeSender, SyncReplicationActivity activity) {
    Trace trace = new Trace(String.valueOf(activity.getActivityID().id), "Sync");
    trace.start();
    SyncReplicationActivity.ActivityType thisActivityType = activity.getActivityType();
    FetchID fetch = activity.getFetchID();
    EntityDescriptor descriptor = EntityDescriptor.createDescriptorForInvoke(fetch, ClientInstanceID.NULL_ID);
    
    // This should have been handled in its own path.
    Assert.assertTrue(SyncReplicationActivity.ActivityType.SYNC_BEGIN != thisActivityType);
    
    beforeSyncAction(activity);
    
    if ((SyncReplicationActivity.ActivityType.SYNC_ENTITY_BEGIN == thisActivityType) && !fetch.isNull()) {
      try {
        // This should have already been created.
        Assert.assertTrue(this.entityManager.getEntity(descriptor).isPresent());
        
        // Now we can actually start synchronizing the entity.
        // NOTE:  We need to update the reference count at this point.
        int referenceCount = activity.getReferenceCount();
        MessagePayload payload = MessagePayload.syncPayloadCreation(activity.getExtendedData(), referenceCount);
        BasicServerEntityRequest request = new BasicServerEntityRequest(ServerEntityAction.RECEIVE_SYNC_CREATE_ENTITY, activity.getSource(), activity.getClientInstanceID(), activity.getTransactionID(), activity.getOldestTransactionOnClient());
        this.entityManager.getEntity(descriptor).get().addRequestMessage(request, payload, createCapture(
          null, 
          (result)->{/* do nothing - this in-between state is temporary*/}, 
          (exception)->{acknowledge(activeSender, activity, ReplicationResultCode.FAIL);}));
      } catch (EntityException exception) {
//  TODO: this needs to be controlled.  
        LOGGER.warn("entity has already been created", exception);
      }
    }
        
    try {
      Optional<ManagedEntity> entity = entityManager.getEntity(descriptor);
      if (entity.isPresent()) {
        // Note that we might have just created this as SYNC_ENTITY_BEGIN, above, so createCapture the payload based on the message type.
        MessagePayload payload = null;
        if (SyncReplicationActivity.ActivityType.SYNC_ENTITY_BEGIN == thisActivityType) {
          payload = MessagePayload.emptyPayload();
        } else if (SyncReplicationActivity.ActivityType.SYNC_BEGIN == thisActivityType) {
          payload = MessagePayload.emptyPayload();
        } else {
          int concurrencyKey = activity.getConcurrency();
          payload = MessagePayload.syncPayloadNormal(activity.getExtendedData(), concurrencyKey);
        }
        entity.get().addRequestMessage(activityToLocalRequest(activity), payload, createCapture(
            null, 
            (result)->acknowledge(activeSender, activity, ReplicationResultCode.SUCCESS), 
            (exception)->acknowledge(activeSender, activity, ReplicationResultCode.FAIL)));
      } else {
        // We should have already created this.
        Assert.assertFalse(SyncReplicationActivity.ActivityType.SYNC_ENTITY_BEGIN == thisActivityType);
        Assert.assertFalse(SyncReplicationActivity.ActivityType.ORDERING_PLACEHOLDER == thisActivityType);
        if (!fetch.isNull()) {
          throw new AssertionError();
        } else {
          MessagePayload payload = MessagePayload.syncPayloadNormal(activity.getExtendedData(), activity.getConcurrency());
          platform.addRequestMessage(activityToLocalRequest(activity), payload, createCapture(null, (result)-> {
            if (SyncReplicationActivity.ActivityType.SYNC_END == thisActivityType) {
              try {
                this.persistor.getEntityPersistor().layer(new ObjectInputStream(new ByteArrayInputStream(payload.getRawPayload())));
              } catch (IOException ioe) {
                throw new RuntimeException(ioe);
              }
              moveToPassiveStandBy();
            }
            acknowledge(activeSender, activity, ReplicationResultCode.SUCCESS);
          }, (exception)->acknowledge(activeSender, activity, ReplicationResultCode.FAIL)));
        }
      }
    } catch (EntityException ee) {
      throw new RuntimeException(ee);
    } finally {
      afterSyncAction(activity);
    }
    trace.end();
  }
  
  private void start() {
    state.start();
  }
  
  private void start(FetchID fetch) {
    state.startEntity(fetch);
  }
  
  private void start(FetchID fetch, int concurrency) {
    state.startConcurrency(fetch, concurrency);
  }
  
  private void finish() {
    scheduleDeferred(state.finish());
  }
  
  private void finish(FetchID fetch) {
    state.endEntity(fetch);
  }
  
  private void finish(FetchID fetch, int concurrency) {
    scheduleDeferred(state.endConcurrency(fetch, concurrency));
  }
  
  private void scheduleDeferred(Deque<DeferredContainer> deferred) {
    if (deferred != null) {
      while(!deferred.isEmpty()) {
        DeferredContainer r = deferred.pop();
        try {
          replicatedActivityReceived(r.activeSender, r.activity);
        } catch (EntityException ee) {
          throw new RuntimeException(ee);
        }
      }
    }
  }
  
  private void moveToPassiveUnitialized(NodeID connectedTo) {
    if (!stateManager.isActiveCoordinator()) {
      stateManager.moveToPassiveSyncing(connectedTo);
    }
  }
  
  private void moveToPassiveStandBy() {
    if (!stateManager.isActiveCoordinator()) {
      stateManager.moveToPassiveStandbyState();
    }
  }

  private ServerEntityRequest activityToLocalRequest(SyncReplicationActivity activity) {
    ActivityType activityType = activity.getActivityType();
    ClientID source = activity.getSource();
    ClientInstanceID instance = activity.getClientInstanceID();
    TransactionID transactionID = activity.getTransactionID();
    TransactionID oldestTransactionID = activity.getOldestTransactionOnClient();
    Assert.assertTrue(ActivityType.SYNC_BEGIN != activityType);
    return new BasicServerEntityRequest(decodeReplicationType(activityType), source, instance, transactionID, oldestTransactionID);
  }
  
  private void beforeSyncAction(SyncReplicationActivity activity) {
    switch (activity.getActivityType()) {
      case SYNC_START:
        establishNewPassive();
        break;
      case SYNC_BEGIN:
        start();
        break;
      case SYNC_ENTITY_BEGIN:
        start(activity.getFetchID());
        break;
      case SYNC_ENTITY_CONCURRENCY_BEGIN:
        start(activity.getFetchID(), activity.getConcurrency());
        break;
      default:
        break;
    }
  }

  private void afterSyncAction(SyncReplicationActivity activity) {
    switch (activity.getActivityType()) {
      case SYNC_END:
        finish();
        break;
      case SYNC_ENTITY_END:
        finish(activity.getFetchID());
        break;
      case SYNC_ENTITY_CONCURRENCY_END:
        finish(activity.getFetchID(), activity.getConcurrency());// finish inline so messages are requeued from the proper sync
        break;
      default:
        break;
    }
  }

  private void ackReceived(ServerID activeSender, SyncReplicationActivity activity, Future<Void> future) {
    if (!activeSender.equals(ServerID.NULL_ID)) {
      if(future != null) {
        try {
          future.get();
        } catch (InterruptedException | ExecutionException e) {
          throw new RuntimeException("Caught exception while persisting transaction order", e);
        }
      }
      prepareAckForSend(activeSender, activity.getActivityID(), ReplicationResultCode.RECEIVED);
    }
  }

  private void acknowledge(ServerID activeSender, SyncReplicationActivity activity, ReplicationResultCode code) {
//  when is the right time to send the ack?
    if (!activeSender.equals(ServerID.NULL_ID)) {
      prepareAckForSend(activeSender, activity.getActivityID(), code);
    }
  }
  
  private ReplicationMessageAck createAckMessage(ReplicationAckTuple initialActivity) {
    ReplicationMessageAck message = ReplicationMessageAck.createBatchAck();
    message.addToBatch(initialActivity);
    return message;
  }

  private synchronized void prepareAckForSend(NodeID sender, SyncReplicationActivity.ActivityID respondTo, ReplicationResultCode code) {
    // The batch context is cached and constructed lazily when the sender changes.
    if (!sender.equals(this.cachedMessageAckFrom)) {
      this.cachedMessageAckFrom = sender;
      this.cachedBatchAck = new GroupMessageBatchContext<>(this::createAckMessage, this.groupManager, this.cachedMessageAckFrom, maximumBatchSize, idealMessagesInFlight, (node)->sendToActive());
    }
    
    boolean didCreate = this.cachedBatchAck.batchMessage(new ReplicationAckTuple(respondTo, code));

    // If we created this message, enqueue the decision to flush it (the other case where we may flush is network
    //  available).
    if (didCreate) {
      sendToActive();
    }
  }
  
  private void sendToActive() {
    // If we created this message, enqueue the decision to flush it (the other case where we may flush is network
    //  available).
    if (!stateManager.isActiveCoordinator()) {
      this.sentToActive.addToSink(()->{
        try {
          this.cachedBatchAck.flushBatch();
        } catch (GroupException group) {
          //  ignore, active is gone
        }
      });
    }
  }

  private static ServerEntityAction decodeReplicationType(SyncReplicationActivity.ActivityType networkType) {
    switch(networkType) {
      case SYNC_BEGIN:
        throw Assert.failure("Shouldn't decode this type into an internal action");
      case SYNC_START:
      case SYNC_END:
      case ORDERING_PLACEHOLDER:
        return ServerEntityAction.ORDER_PLACEHOLDER_ONLY;
      case LOCAL_ENTITY_GC:
        return ServerEntityAction.MANAGED_ENTITY_GC;
      case FLUSH_LOCAL_PIPELINE:
        // Note that these are never replicated from the active but we do synthesize them, internally, in some cases.
        return ServerEntityAction.LOCAL_FLUSH;
      case CREATE_ENTITY:
        return ServerEntityAction.CREATE_ENTITY;
      case RECONFIGURE_ENTITY:
        return ServerEntityAction.RECONFIGURE_ENTITY;
      case INVOKE_ACTION:
        return ServerEntityAction.INVOKE_ACTION;
      case DESTROY_ENTITY:
        return ServerEntityAction.DESTROY_ENTITY;
      case FETCH_ENTITY:
        return ServerEntityAction.FETCH_ENTITY;
      case RELEASE_ENTITY:
        return ServerEntityAction.RELEASE_ENTITY;
      case SYNC_ENTITY_BEGIN:
        return ServerEntityAction.RECEIVE_SYNC_ENTITY_START_SYNCING;
      case SYNC_ENTITY_CONCURRENCY_BEGIN:
        return ServerEntityAction.RECEIVE_SYNC_ENTITY_KEY_START;
      case SYNC_ENTITY_CONCURRENCY_PAYLOAD:
        return ServerEntityAction.RECEIVE_SYNC_PAYLOAD;
      case SYNC_ENTITY_CONCURRENCY_END:
        return ServerEntityAction.RECEIVE_SYNC_ENTITY_KEY_END;
      case SYNC_ENTITY_END:
        return ServerEntityAction.RECEIVE_SYNC_ENTITY_END;
      case DISCONNECT_CLIENT:
        return ServerEntityAction.DISCONNECT_CLIENT;
      default:
        throw new AssertionError("bad replication type: " + networkType);
    }
  }  
  
 private class SyncState {
    private LinkedList<DeferredContainer> defer = new LinkedList<>();
 //  at this point, id based checking is legacy.  Everything should have a fetchid.  TODO: remove 
    private final Set<FetchID> syncdFetches = new HashSet<>();
    private final Set<Integer> syncdKeys = new HashSet<>();
 //  at this point, id based checking is legacy.  Everything should have a fetchid 
    private FetchID syncingFetch = FetchID.NULL_ID;
    private int currentKey = -1;
    private boolean finished = false;
    private boolean started = false;
    
    private void start() {
      started = true;
    }
    
    private void startEntity(FetchID fetch) {
      assertStarted(null);
      Assert.assertTrue(syncingFetch.isNull());
      syncingFetch = fetch;
// these keys are never sync'd only replicated so add them to the set
      syncdKeys.add(ConcurrencyStrategy.MANAGEMENT_KEY);
      syncdKeys.add(ConcurrencyStrategy.UNIVERSAL_KEY);
      LOGGER.debug("Starting " + fetch);
    }
    
    private void endEntity(FetchID fetch) {
      assertStarted(null);
      Assert.assertEquals(syncingFetch, fetch);
      syncdFetches.add(fetch);
      syncdKeys.clear();
      syncingFetch = FetchID.NULL_ID;
      LOGGER.debug("Ending " + fetch);
    }
    
    private void startConcurrency(FetchID fetch, int concurrency) {
      assertStarted(null);
      Assert.assertEquals(syncingFetch, fetch);
      currentKey = concurrency;
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Starting " + fetch + "/" + currentKey);
      }
    }
    
    private Deque<DeferredContainer> endConcurrency(FetchID fetch, int concurrency) {
      assertStarted(null);
      try {
        Assert.assertEquals(syncingFetch, fetch);
        Assert.assertEquals(currentKey, concurrency);
        syncdKeys.add(concurrency);
        currentKey = -1;
        return defer;
      } finally {
        defer = new LinkedList<>();
      }
    }
    
    private Deque<DeferredContainer> finish() {
      assertStarted(null);
      syncdFetches.clear();
      finished = true;
      return defer;
    }
    
    private boolean ignore(SyncReplicationActivity activity) {
      if (!started) {
 // this passive has never been sync'd to anything, ignore all messages
        return true;
      }
      if (finished) {
//  done with sync, need to apply everything now
        return false;
      }
      
      return false;
    }

    private boolean defer(ServerID activeSender, SyncReplicationActivity activity) {
      assertStarted(activity);
      if (finished) {
//  done with sync, need to apply everything now
        return false;
      }
//  everything else, check
      FetchID fetch = activity.getFetchID();
      if (syncdFetches.contains(fetch)) {
        return false;
      } 
      
      SyncReplicationActivity.ActivityType activityType = activity.getActivityType();

      if (fetch.equals(syncingFetch)) {
        int concurrencyKey = activity.getConcurrency();
        if (syncdKeys.contains(concurrencyKey)) {
          return false;
        } else if (SyncReplicationActivity.ActivityType.CREATE_ENTITY == activityType) {
          return true;
        } else if (SyncReplicationActivity.ActivityType.ORDERING_PLACEHOLDER == activityType) {
//  ORDERING_PLACEHOLDER requests cannot be deferred
          return false;
        } else if (SyncReplicationActivity.ActivityType.DESTROY_ENTITY == activityType) {
//          Assert.fail("destroy received during a sync of an entity " + activity);
          return false;
        } else if (currentKey == concurrencyKey) {
          defer.add(new DeferredContainer(activeSender, activity));
          return true;
        } else if (concurrencyKey == ConcurrencyStrategy.UNIVERSAL_KEY) {
          // if a message comes on the universal key, make sure it lags at least one step by deferrign the 
          // operation.  This prevents the invoke from possibly outracing the creation message at the start 
          // of sync.  Consider deferring all universal key operations to the end of entity sync.
          defer.add(new DeferredContainer(activeSender, activity));
          return true;
        }
      }
      return false;
    }
    
    /**
     * Note that this state machine the started flag can be used to assert consistency.
     * 
     * The start flag starts the valid stream for a passive.  A passive can only accept valid 
     * messages after sync has started on the server.  Prior to that, everything is invalid
     * and can be safely ignored.  Messages can be received prior to the start sync message
     * because replication started as soon as the active detects a connect from a passive.
     * 
     * Sync start begins after the passive has successfully connected and requested to be sync'd
     * 
     * NOTE: it is possible in the multiple passive scenario, for a stream to start a new 
     * active but in this case, the server will have already been sync'd and thus valid
     */
    private void assertStarted(SyncReplicationActivity activity) {
      // These should short-circuit quickly, not creating an expensive check overhead.
      Assert.assertTrue(activity, started);
    }
  }
 
  public static class BasicServerEntityRequest implements ServerEntityRequest {
    private final ServerEntityAction action;
    private final ClientID source;
    private final ClientInstanceID instance;
    private final TransactionID transaction;
    private final TransactionID oldest;

    public BasicServerEntityRequest(ServerEntityAction action, ClientID source, ClientInstanceID instance, TransactionID transaction, TransactionID oldest) {
      this.action = action;
      this.source = source;
      this.instance = instance;
      this.transaction = transaction;
      this.oldest = oldest;
    }

    @Override
    public ServerEntityAction getAction() {
      return action;
    }

    @Override
    public ClientID getNodeID() {
      return source;
    }

    @Override
    public TransactionID getTransaction() {
      return transaction;
    }

    @Override
    public TransactionID getOldestTransactionOnClient() {
      return oldest;
    }

    @Override
    public ClientInstanceID getClientInstance() {
      return instance;
    }

    @Override
    public boolean requiresReceived() {
      return false;
    }

    @Override
    public Set<NodeID> replicateTo(Set<NodeID> passives) {
      return Collections.emptySet();
    }

    @Override
    public String toString() {
      return "BasicServerEntityRequest{" + "action=" + action + ", source=" + source + ", instance=" + instance + ", transaction=" + transaction + ", oldest=" + oldest + '}';
    }
  }


  private static class DeferredContainer {
    public final ServerID activeSender;
    public final SyncReplicationActivity activity;
    public DeferredContainer(ServerID activeSender, SyncReplicationActivity activity) {
      this.activeSender = activeSender;
      this.activity = activity;
    }
  }

  public static class SedaToken {
    
  }
}
