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
import com.tc.objectserver.entity.MessagePayload;
import com.tc.l2.msg.ReplicationAckTuple;
import com.tc.l2.msg.ReplicationMessage;
import com.tc.l2.msg.ReplicationMessageAck;
import com.tc.l2.msg.ReplicationResultCode;
import com.tc.l2.msg.SyncReplicationActivity;
import com.tc.l2.msg.SyncReplicationActivity.ActivityType;
import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.entity.BarrierCompletion;
import com.tc.objectserver.entity.ClientDescriptorImpl;
import com.tc.objectserver.entity.PlatformEntity;
import com.tc.objectserver.entity.ServerEntityRequestResponse;
import com.tc.objectserver.handler.GroupMessageBatchContext.IBatchableMessageFactory;
import com.tc.objectserver.persistence.EntityPersistor;
import com.tc.objectserver.persistence.TransactionOrderPersistor;
import com.tc.properties.TCPropertiesImpl;
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
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.exception.EntityException;


public class ReplicatedTransactionHandler {
  private static final int DEFAULT_BATCH_LIMIT = 64;
  private static final int DEFAULT_INFLIGHT_MESSAGES = 1;
  private static final TCLogger PLOGGER = TCLogging.getLogger(MessagePayload.class);
  private static final TCLogger LOGGER = TCLogging.getLogger(ReplicatedTransactionHandler.class);

  private final EntityManager entityManager;
  private final EntityPersistor entityPersistor;
  private final GroupManager<AbstractGroupMessage> groupManager;
  private final TransactionOrderPersistor orderedTransactions;
  private final StateManager stateManager;
  private final ManagedEntity platform;
  
  private final SyncState state = new SyncState();
  
  // This MUST be manipulated under lock - it is the batch of ack messages we are accumulating until the network is ready for another message.
  private NodeID cachedMessageAckFrom;
  private GroupMessageBatchContext<ReplicationMessageAck, ReplicationAckTuple> cachedBatchAck;
  private final Runnable handleMessageSend = new Runnable() {
    @Override
    public void run() {
      ReplicatedTransactionHandler.this.outgoingResponseSink.addSingleThreaded(ReplicatedTransactionHandler.this.selfMessageToken);
    }
  };
  
  public ReplicatedTransactionHandler(StateManager state, TransactionOrderPersistor transactionOrderPersistor, 
      EntityManager manager, EntityPersistor entityPersistor, GroupManager<AbstractGroupMessage> groupManager) {
    this.stateManager = state;
    this.entityManager = manager;
    this.entityPersistor = entityPersistor;
    this.groupManager = groupManager;
    this.orderedTransactions = transactionOrderPersistor;
    try {
      platform = entityManager.getEntity(PlatformEntity.PLATFORM_ID, PlatformEntity.VERSION).get();
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
      scxt.getL2Coordinator().getReplicatedClusterStateManager().setCurrentState(scxt.getL2Coordinator().getStateManager().getCurrentState());
      if (stateManager.getCurrentState().equals(StateManager.PASSIVE_UNINITIALIZED)) {
        requestPassiveSync();
      }
    }

    @Override
    public void destroy() {
      ServerEntityRequest req = new ServerEntityRequest() {
        @Override
        public ServerEntityAction getAction() {
          return ServerEntityAction.LOCAL_FLUSH_AND_DELETE;
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
        public ClientDescriptor getSourceDescriptor() {
          return new ClientDescriptorImpl(ClientID.NULL_ID, EntityDescriptor.NULL_ID);
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
            (result)->latch.complete(), exception->Assert.fail());
        latch.waitForCompletion();
      }
      BarrierCompletion latch = new BarrierCompletion();
      platform.addRequestMessage(req, MessagePayload.emptyPayload(), (result)->latch.complete(), null);
      latch.waitForCompletion();
    }    
  };

  /**
   * The outgoing response handler is where the passive enqueues any instructions to flush the outgoing ack channel
   *  (messages to ack messages from the active).
   */
  private final EventHandler<SedaToken> outgoingResponseHandler = new AbstractEventHandler<SedaToken>() {
    @Override
    public void handleEvent(SedaToken ignored) throws EventHandlerException {
      try {
        cachedBatchAck.flushBatch();
      } catch (GroupException e) {
        // We can't handle this here, but the next attempt to add to a batch will see the exception from this same
        //  context.
        LOGGER.error("Exception flushing ack batch context", e);
      }
    }
  };

  private Sink<SedaToken> outgoingResponseSink;
  private final SedaToken selfMessageToken = new SedaToken();


  public EventHandler<ReplicationMessage> getEventHandler() {
    return eventHorizon;
  }

  public EventHandler<SedaToken> getOutgoingResponseHandler() {
    return this.outgoingResponseHandler;
  }

  public void setOutgoingResponseSink(Sink<SedaToken> sink) {
    Assert.assertNull(this.outgoingResponseSink);
    this.outgoingResponseSink = sink;
  }

  private void processMessage(ReplicationMessage rep) throws EntityException {
    if (PLOGGER.isDebugEnabled()) {
      PLOGGER.debug("RECEIVED:" + rep.getDebugId());
    }
    ServerID activeSender = (ServerID) rep.messageFrom();
    for (SyncReplicationActivity activity : rep.getActivities()) {
      if (activity.isSyncActivity()) {
        if (SyncReplicationActivity.ActivityType.SYNC_BEGIN == activity.getActivityType()) {
          syncBeginEntityListReceived(activeSender, activity);
        } else {
          syncActivityReceived(activeSender, activity);
        }
      } else {
        if (state.ignore(activity)) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Ignoring:" + rep);
          }
          acknowledge(activeSender, activity, ReplicationResultCode.NONE);
        } else if (state.defer(activeSender, activity)) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Deferring:" + rep);
          }
        } else {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Applying:" + rep);
          }
          replicatedActivityReceived(activeSender, activity);
        }
      }
    }
  }

  private void syncBeginEntityListReceived(ServerID activeSender, SyncReplicationActivity activity) throws EntityException {
    ackReceived(activeSender, activity);
    beforeSyncAction(activity);
    
    // In this case, we want to create all the provided entities.
    SyncReplicationActivity.EntityCreationTuple[] entityTuples = activity.getEntitiesToCreateForSync();
    Assert.assertNotNull(entityTuples);
    // Note that these are provided in the order they must be instantiated so just walk the list.
    for (SyncReplicationActivity.EntityCreationTuple tuple : entityTuples) {
      EntityID eid = tuple.id;
      long version = tuple.version;
      byte[] config = tuple.configPayload;
      boolean canDelete = tuple.canDelete;
      
      if (!this.entityManager.getEntity(eid, version).isPresent()) {
        long consumerID = entityPersistor.getNextConsumerID();
        this.entityManager.createEntity(eid, version, consumerID, canDelete);
        this.entityPersistor.entityCreatedNoJournal(eid, version, consumerID, canDelete, config);
      } else {
        Assert.fail("this entity should not be here");
      }
    }
    // This is somewhat strange in that these entities won't actually contain any data or receive replicated messages
    //  until the SYNC_ENTITY_BEGIN for this specific entity is received.
    acknowledge(activeSender, activity, ReplicationResultCode.SUCCESS);
  }

//  don't need to worry about resends here for lifecycle messages.  active will filer them  
  private void replicatedActivityReceived(ServerID activeSender, SyncReplicationActivity activity) throws EntityException {
    ClientID sourceNodeID = activity.getSource();
    TransactionID transactionID = activity.getTransactionID();
    TransactionID oldestTransactionOnClient = activity.getOldestTransactionOnClient();
    EntityDescriptor descriptor = activity.getEntityDescriptor();

    // Send the RECEIVED ack before we run this.
    ackReceived(activeSender, activity);
    
    // Note that we only want to persist the messages with a true sourceNodeID.  Synthetic invocations and sync messages
    // don't have one (although sync messages shouldn't come down this path).
    if (!ClientInstanceID.NULL_ID.equals(sourceNodeID)) {
      if (!oldestTransactionOnClient.isNull()) {
        this.orderedTransactions.updateWithNewMessage(sourceNodeID, transactionID, oldestTransactionOnClient);
      } else {
        // This corresponds to a disconnect.
        this.orderedTransactions.removeTrackingForClient(sourceNodeID);
        this.entityPersistor.removeTrackingForClient(sourceNodeID);
      }
    }

    long version = descriptor.getClientSideVersion();
    EntityID entityID = descriptor.getEntityID();
    byte[] extendedData = activity.getExtendedData();

    // At this point, we can now look up the managed entity (used later).
    Optional<ManagedEntity> entity = entityManager.getEntity(entityID,version);

    // Create the request, since it is how we will generically return complete.
    ServerEntityRequest request = activityToLocalRequest(activity);
    // If we satisfied this as a known re-send, don't add the request to the entity.
    if (request.getAction() == ServerEntityAction.CREATE_ENTITY) {
// The common pattern for this is to pass an empty array on success ("found") or an exception on failure ("not found").
      long consumerID = this.entityPersistor.getNextConsumerID();
      try {
        // TODO:  When a permanent entity is being synced, should we be creating it or ensuring it already exists?
        boolean canDelete = !sourceNodeID.isNull();
        ManagedEntity temp = entityManager.createEntity(entityID, descriptor.getClientSideVersion(), consumerID, canDelete);
        temp.addRequestMessage(request, MessagePayload.rawDataOnly(extendedData), 
          (result) -> {
            if (!sourceNodeID.isNull()) {
              entityPersistor.entityCreated(sourceNodeID, transactionID.toLong(), oldestTransactionOnClient.toLong(), entityID, descriptor.getClientSideVersion(), consumerID, true /*from client checked*/, extendedData);
              acknowledge(activeSender, activity, ReplicationResultCode.SUCCESS);
            } else {
              entityPersistor.entityCreatedNoJournal(entityID, descriptor.getClientSideVersion(), consumerID, true, extendedData);
              acknowledge(activeSender, activity, ReplicationResultCode.SUCCESS);
            }
          }, (exception) -> {
            entityPersistor.entityCreateFailed(sourceNodeID, transactionID.toLong(), oldestTransactionOnClient.toLong(), exception);
            acknowledge(activeSender, activity, ReplicationResultCode.FAIL);
          });
      } catch (EntityException ee) {
        acknowledge(activeSender, activity, ReplicationResultCode.FAIL);
        if (!sourceNodeID.isNull()) {
          entityPersistor.entityCreateFailed(sourceNodeID, transactionID.toLong(), oldestTransactionOnClient.toLong(), ee);
        }
      }
    } else if (entity.isPresent()) {
      ManagedEntity entityInstance = entity.get();

      MessagePayload payload = MessagePayload.syncPayloadNormal(extendedData, activity.getConcurrency());
      if (null != request.getAction()) switch (request.getAction()) {
        case RECONFIGURE_ENTITY:  
          entity.get().addRequestMessage(request, payload, 
            (result)->{
              entityPersistor.entityReconfigureSucceeded(sourceNodeID, transactionID.toLong(), oldestTransactionOnClient.toLong(), entityID, version, result);
              acknowledge(activeSender, activity, ReplicationResultCode.SUCCESS);
            } , (exception) -> {
              entityPersistor.entityReconfigureFailed(sourceNodeID, transactionID.toLong(), oldestTransactionOnClient.toLong(), exception);
              acknowledge(activeSender, activity, ReplicationResultCode.FAIL);
            });
          break;
        case DESTROY_ENTITY:
          entityInstance.addRequestMessage(request, payload, 
            (result)-> {
              entityPersistor.entityDestroyed(sourceNodeID, transactionID.toLong(), oldestTransactionOnClient.toLong(), entityID);
              acknowledge(activeSender, activity, ReplicationResultCode.SUCCESS);
            }, (exception) -> {
             entityPersistor.entityDestroyFailed(sourceNodeID, transactionID.toLong(), oldestTransactionOnClient.toLong(), exception);
              acknowledge(activeSender, activity, ReplicationResultCode.FAIL);
            });
          break;
        case FETCH_ENTITY:
        case RELEASE_ENTITY:
          entityInstance.addRequestMessage(request, payload, 
            (result)-> {
              acknowledge(activeSender, activity, ReplicationResultCode.SUCCESS);
            }, (exception) -> {
              LOGGER.warn("fetch/release fail:" + activity);
              acknowledge(activeSender, activity, ReplicationResultCode.FAIL);
            });
          break;
        case LOCAL_FLUSH_AND_DELETE:
          if (entityInstance.isRemoveable()) {
            LOGGER.debug("removing " + entityInstance.getID());
            entityManager.removeDestroyed(entityInstance.getID());
          }
          //  fall-through to default
        default:
          entityInstance.addRequestMessage(request, payload, (result)-> acknowledge(activeSender, activity, ReplicationResultCode.SUCCESS), (exception) -> acknowledge(activeSender, activity, ReplicationResultCode.FAIL));
          break;
      }
    } else {
 //  fail, just ack
      acknowledge(activeSender, activity, ReplicationResultCode.FAIL);
    }
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
    SyncReplicationActivity.ActivityType thisActivityType = activity.getActivityType();
    EntityDescriptor descriptor = activity.getEntityDescriptor();
    EntityID eid = descriptor.getEntityID();
    long version = descriptor.getClientSideVersion();
    
    // This should have been handled in its own path.
    Assert.assertTrue(SyncReplicationActivity.ActivityType.SYNC_BEGIN != thisActivityType);
    
    ackReceived(activeSender, activity);
    beforeSyncAction(activity);
    
    if ((SyncReplicationActivity.ActivityType.SYNC_ENTITY_BEGIN == thisActivityType) && !eid.equals(EntityID.NULL_ID)) {
      try {
        // This should have already been created.
        Assert.assertTrue(this.entityManager.getEntity(eid, version).isPresent());
        
        // Now we can actually start synchronizing the entity.
        // NOTE:  We need to update the reference count at this point.
        int referenceCount = activity.getReferenceCount();
        MessagePayload payload = MessagePayload.syncPayloadCreation(activity.getExtendedData(), referenceCount);
        BasicServerEntityRequest request = new BasicServerEntityRequest(ServerEntityAction.RECEIVE_SYNC_CREATE_ENTITY, activity.getSource(), activity.getTransactionID(), activity.getOldestTransactionOnClient(), activity.getEntityDescriptor());
        this.entityManager.getEntity(eid, version).get().addRequestMessage(request, payload, (result)->{/* do nothing - this in-between state is temporary*/}, (exception)->{acknowledge(activeSender, activity, ReplicationResultCode.FAIL);});
      } catch (EntityException exception) {
//  TODO: this needs to be controlled.  
        LOGGER.warn("entity has already been created", exception);
      }
    }
        
    try {
      Optional<ManagedEntity> entity = entityManager.getEntity(eid, version);
      if (entity.isPresent()) {
        // Note that we might have just created this as SYNC_ENTITY_BEGIN, above, so create the payload based on the message type.
        MessagePayload payload = null;
        if (SyncReplicationActivity.ActivityType.SYNC_ENTITY_BEGIN == thisActivityType) {
          payload = MessagePayload.emptyPayload();
        } else if (SyncReplicationActivity.ActivityType.SYNC_BEGIN == thisActivityType) {
          payload = MessagePayload.emptyPayload();
        } else {
          int concurrencyKey = activity.getConcurrency();
          payload = MessagePayload.syncPayloadNormal(activity.getExtendedData(), concurrencyKey);
        }
        entity.get().addRequestMessage(activityToLocalRequest(activity), payload, (result)->acknowledge(activeSender, activity, ReplicationResultCode.SUCCESS), (exception)->acknowledge(activeSender, activity, ReplicationResultCode.FAIL));
        if (SyncReplicationActivity.ActivityType.SYNC_ENTITY_CONCURRENCY_PAYLOAD != thisActivityType) {
          entity.get().addRequestMessage(makeLocalFlush(eid, version), MessagePayload.emptyPayload(), null, null);
        }
      } else {
        // We should have already created this.
        Assert.assertFalse(SyncReplicationActivity.ActivityType.SYNC_ENTITY_BEGIN == thisActivityType);
        if (SyncReplicationActivity.ActivityType.ORDERING_PLACEHOLDER == thisActivityType) {
          // We only received this to ensure that we saved the transaction order - we can ack without doing anything.
          acknowledge(activeSender, activity, ReplicationResultCode.SUCCESS);
        } else if (!eid.equals(EntityID.NULL_ID)) {
          throw new AssertionError();
        } else {
          MessagePayload payload = MessagePayload.syncPayloadNormal(activity.getExtendedData(), activity.getConcurrency());
          platform.addRequestMessage(activityToLocalRequest(activity), payload, (result)-> {
            if (SyncReplicationActivity.ActivityType.SYNC_END == thisActivityType) {
              try {
                entityPersistor.layer(new ObjectInputStream(new ByteArrayInputStream(payload.getRawPayload())));
              } catch (IOException ioe) {
                throw new RuntimeException(ioe);
              }
              moveToPassiveStandBy();
            }
            acknowledge(activeSender, activity, ReplicationResultCode.SUCCESS);
          }, (exception)->acknowledge(activeSender, activity, ReplicationResultCode.FAIL));
        }
      }
    } catch (EntityException ee) {
      throw new RuntimeException(ee);
    }
  }
  
  private void start() {
    state.start();
  }
  
  private void start(EntityID eid) {
    state.startEntity(eid);
  }
  
  private void start(EntityID eid, int concurrency) {
    state.startConcurrency(eid, concurrency);
  }
  
  private void finish() {
    state.finish();
  }
  
  private void finish(EntityID eid) {
    state.endEntity(eid);
  }
  
  private void finish(EntityID eid, int concurrency) {
    scheduleDeferred(state.endConcurrency(eid, concurrency));
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
  
  private ServerEntityRequest makeLocalFlush(EntityID eid, long version) {
    // Anything created within this class represents a replicated message.
    boolean isReplicatedMessage = true;
    return new ServerEntityRequestResponse(new EntityDescriptor(eid, ClientInstanceID.NULL_ID, version), ServerEntityAction.LOCAL_FLUSH, TransactionID.NULL_ID, TransactionID.NULL_ID, ClientID.NULL_ID, ()->Optional.empty(), isReplicatedMessage);
  }
      
  private ServerEntityRequest activityToLocalRequest(SyncReplicationActivity activity) {
    ActivityType activityType = activity.getActivityType();
    ClientID source = ClientID.NULL_ID;
    TransactionID transactionID = TransactionID.NULL_ID;
    TransactionID oldestTransactionID = TransactionID.NULL_ID;
    EntityDescriptor descriptor = EntityDescriptor.NULL_ID;
    Assert.assertTrue(ActivityType.SYNC_BEGIN != activityType);
    return new BasicServerEntityRequest(decodeReplicationType(activityType), source, transactionID, oldestTransactionID, descriptor);
  }
  
  private void beforeSyncAction(SyncReplicationActivity activity) {
    EntityID eid = activity.getEntityDescriptor().getEntityID();
    switch (activity.getActivityType()) {
      case SYNC_START:
        establishNewPassive();
        break;
      case SYNC_BEGIN:
        start();
        break;
      case SYNC_END:
        finish();
        break;
      case SYNC_ENTITY_BEGIN:
        start(eid);
        break;
      case SYNC_ENTITY_END:
        finish(eid);
        break;
      case SYNC_ENTITY_CONCURRENCY_BEGIN:
        start(eid, activity.getConcurrency());
        break;
      case SYNC_ENTITY_CONCURRENCY_END:
        finish(eid, activity.getConcurrency());// finish inline so messages are requeued from the proper sync
        break;
      default:
        break;
    }
  }

  private void ackReceived(ServerID activeSender, SyncReplicationActivity activity) {
    if (!activeSender.equals(ServerID.NULL_ID)) {
      prepareAckForSend(activeSender, activity.getActivityID(), ReplicationResultCode.RECEIVED);
    }
  }

  private void acknowledge(ServerID activeSender, SyncReplicationActivity activity, ReplicationResultCode code) {
//  when is the right time to send the ack?
    if (!activeSender.equals(ServerID.NULL_ID)) {
      prepareAckForSend(activeSender, activity.getActivityID(), code);
    }
  }

  private synchronized void prepareAckForSend(NodeID sender, SyncReplicationActivity.ActivityID respondTo, ReplicationResultCode code) {
    // The batch context is cached and constructed lazily when the sender changes.
    if (!sender.equals(this.cachedMessageAckFrom)) {
      int maximumBatchSize = TCPropertiesImpl.getProperties().getInt("passive-active.batchsize", DEFAULT_BATCH_LIMIT);
      int idealMessagesInFlight = TCPropertiesImpl.getProperties().getInt("passive-active.inflight", DEFAULT_INFLIGHT_MESSAGES);
      IBatchableMessageFactory<ReplicationMessageAck, ReplicationAckTuple> factory = new IBatchableMessageFactory<ReplicationMessageAck, ReplicationAckTuple>() {
        @Override
        public ReplicationMessageAck createNewBatch(ReplicationAckTuple initialActivity, long id) {
          ReplicationMessageAck message = ReplicationMessageAck.createBatchAck();
          message.addToBatch(initialActivity);
          return message;
        }
      };
      this.cachedMessageAckFrom = sender;
      this.cachedBatchAck = new GroupMessageBatchContext<>(factory, this.groupManager, this.cachedMessageAckFrom, maximumBatchSize, idealMessagesInFlight, this.handleMessageSend);
    }
    
    boolean didCreate = this.cachedBatchAck.batchMessage(new ReplicationAckTuple(respondTo, code));

    // If we created this message, enqueue the decision to flush it (the other case where we may flush is network
    //  available).
    if (didCreate) {
      this.outgoingResponseSink.addSingleThreaded(this.selfMessageToken);
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
      default:
        throw new AssertionError("bad replication type: " + networkType);
    }
  }  
  
 private class SyncState {
    private LinkedList<DeferredContainer> defer = new LinkedList<>();
    
    private final Set<EntityID> syncdEntities = new HashSet<>();
    private final Set<Integer> syncdKeys = new HashSet<>();
    private EntityID syncing;
    private int currentKey = -1;
    private boolean finished = false;
    private boolean started = false;
    
    private void start() {
      started = true;
    }
    
    private void startEntity(EntityID eid) {
      assertStarted(null);
      Assert.assertNull(syncing);
      syncing = eid;
// these keys are never sync'd only replicated so add them to the set
      syncdKeys.add(ConcurrencyStrategy.MANAGEMENT_KEY);
      syncdKeys.add(ConcurrencyStrategy.UNIVERSAL_KEY);
      LOGGER.debug("Starting " + eid);
    }
    
    private void endEntity(EntityID eid) {
      assertStarted(null);
      Assert.assertEquals(syncing, eid);
      syncdEntities.add(eid);
      syncdKeys.clear();
      syncing = null;
      LOGGER.debug("Ending " + eid);
    }
    
    private void startConcurrency(EntityID eid, int concurrency) {
      assertStarted(null);
      Assert.assertEquals(syncing, eid);
      currentKey = concurrency;
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Starting " + eid + "/" + currentKey);
      }
    }
    
    private Deque<DeferredContainer> endConcurrency(EntityID eid, int concurrency) {
      assertStarted(null);
      try {
        if (!eid.equals(syncing) || concurrency != currentKey) {
          throw new AssertionError();
        }
        Assert.assertEquals(syncing, eid);
        Assert.assertEquals(currentKey, concurrency);
        syncdKeys.add(concurrency);
        currentKey = -1;
        return defer;
      } finally {
        defer = new LinkedList<>();
      }
    }
    
    private void finish() {
      assertStarted(null);
      syncdEntities.clear();
      finished = true;
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
      EntityID eid = activity.getEntityDescriptor().getEntityID();
//  everything else, check
      if (eid.equals(syncing)) {
        // Note that it is possible that the currently syncing entity has already been destroyed, in which case this message
        //  is either targeting something which doesn't exist or targets something which has been created over top of it.
        // In either case, we shouldn't ignore it.
        int concurrencyKey = activity.getConcurrency();
        if (currentKey == concurrencyKey) {
          return false;
        } else if (!syncdKeys.contains(concurrencyKey)) {
//  ignore, haven't gotten to this key yet
          return true;
        } else {
//  valid, already sync'd key, apply
          return false;
        }
      }
      if (SyncReplicationActivity.ActivityType.CREATE_ENTITY == activity.getActivityType()) {
        syncdEntities.add(eid);
        return false;
      } else if (SyncReplicationActivity.ActivityType.DESTROY_ENTITY == activity.getActivityType()) {
        // Since we sent the entire collection of entities, at the beginning, we always need to replicate destroys.
        return false;
        
      }
//  if not syncing or sync'd, just ignore it.
      return (!syncdEntities.contains(eid));
    }

    private boolean defer(ServerID activeSender, SyncReplicationActivity activity) {
      assertStarted(activity);
      if (finished) {
//  done with sync, need to apply everything now
        return false;
      }
//  everything else, check
      EntityID eid = activity.getEntityDescriptor().getEntityID();
      if (syncdEntities.contains(eid)) {
        return false;
      } 
      
      SyncReplicationActivity.ActivityType activityType = activity.getActivityType();
      if (SyncReplicationActivity.ActivityType.CREATE_ENTITY == activityType) {
        Assert.fail("create received during a sync of an entity " + syncing);
      }
      
      if (eid.equals(syncing)) {
        int concurrencyKey = activity.getConcurrency();
        if (syncdKeys.contains(concurrencyKey)) {
          return false;
        } else if (SyncReplicationActivity.ActivityType.ORDERING_PLACEHOLDER == activityType) {
//  ORDERING_PLACEHOLDER requests cannot be deferred
          return false;
        } else if (SyncReplicationActivity.ActivityType.DESTROY_ENTITY == activityType) {
          Assert.fail("destroy received during a sync of an entity " + syncing);
          return false;
        } else if (currentKey == concurrencyKey) {
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
    private final TransactionID transaction;
    private final TransactionID oldest;
    private final EntityDescriptor descriptor;

    public BasicServerEntityRequest(ServerEntityAction action, ClientID source, TransactionID transaction, TransactionID oldest, EntityDescriptor descriptor) {
      this.action = action;
      this.source = source;
      this.transaction = transaction;
      this.oldest = oldest;
      this.descriptor = descriptor;
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
    public ClientDescriptor getSourceDescriptor() {
      return new ClientDescriptorImpl(getNodeID(), this.descriptor);
    }

    @Override
    public Set<NodeID> replicateTo(Set<NodeID> passives) {
      return Collections.emptySet();
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
