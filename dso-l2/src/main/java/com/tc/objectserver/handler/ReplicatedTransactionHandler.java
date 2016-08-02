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
import com.tc.l2.msg.ReplicationMessage;
import com.tc.l2.msg.ReplicationMessageAck;
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
import com.tc.objectserver.entity.ClientDescriptorImpl;
import com.tc.objectserver.entity.PlatformEntity;
import com.tc.objectserver.entity.ServerEntityRequestResponse;
import com.tc.objectserver.persistence.EntityPersistor;
import com.tc.objectserver.persistence.TransactionOrderPersistor;
import com.tc.util.Assert;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.BooleanSupplier;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.exception.EntityException;


public class ReplicatedTransactionHandler {
  private static final TCLogger LOGGER = TCLogging.getLogger(ReplicatedTransactionHandler.class);
  private final EntityManager entityManager;
  private final EntityPersistor entityPersistor;
  private final GroupManager<AbstractGroupMessage> groupManager;
  private final TransactionOrderPersistor orderedTransactions;
  private final StateManager stateManager;
  private final ManagedEntity platform;
  
  private Sink<ReplicationMessage> loopback;
  
  private final SyncState state = new SyncState();
  
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
      setLoopback(scxt.getStage(ServerConfigurationContext.PASSIVE_REPLICATION_STAGE, ReplicationMessage.class).getSink());
      if (stateManager.getCurrentState().equals(StateManager.PASSIVE_UNINITIALIZED)) {
        requestPassiveSync();
      }
    }

    @Override
    public void destroy() {
      CountDownLatch latch = new CountDownLatch(1);
      ServerEntityRequest req = new ServerEntityRequest() {
        @Override
        public ServerEntityAction getAction() {
          return ServerEntityAction.NOOP;
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
      platform.addRequestMessage(req, new MessagePayload(new byte[0], null, ConcurrencyStrategy.MANAGEMENT_KEY), (result)->latch.countDown(), null);
      try {
        latch.await();
      } catch (InterruptedException ie) {
        throw new RuntimeException(ie);
      }
    }    
  };
  
  private void setLoopback(Sink<ReplicationMessage> loop) {
    this.loopback = loop;
  }
  
  public EventHandler<ReplicationMessage> getEventHandler() {
    return eventHorizon;
  }

  private void processMessage(ReplicationMessage rep) throws EntityException {
    switch (rep.getType()) {
      case ReplicationMessage.REPLICATE:
        if (state.ignore(rep)) {
          LOGGER.debug("Ignoring:" + rep);
          acknowledge(rep);
        } else if (state.defer(rep)) {
          LOGGER.debug("Deferring:" + rep);
        } else {
          LOGGER.debug("Applying:" + rep);
          replicatedMessageReceived(rep);
        }
        break;
      case ReplicationMessage.SYNC:
        if (!state.destroyed(rep.getEntityID())) {
          syncMessageReceived(rep);
        } else {
          acknowledge(rep);
        }
        break;
      case ReplicationMessage.START:
        throw new AssertionError("unexpected message type " + rep);
      default:
        // This is an unexpected replicated message type.
        throw new RuntimeException();
    }
  }
//  don't need to worry about resends here for lifecycle messages.  active will filer them  
  private void replicatedMessageReceived(ReplicationMessage rep) throws EntityException {
    ClientID sourceNodeID = rep.getSource();
    TransactionID transactionID = rep.getTransactionID();
    TransactionID oldestTransactionOnClient = rep.getOldestTransactionOnClient();
    EntityDescriptor descriptor = rep.getEntityDescriptor();

    // Send the RECEIVED ack before we run this.
    ackReceived(rep);
    
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

    long version = rep.getVersion();
    EntityID entityID = descriptor.getEntityID();
    byte[] extendedData = rep.getExtendedData();

    ReplicationMessage.ReplicationType replicationType = rep.getReplicationType();

    // At this point, we can now look up the managed entity (used later).
    Optional<ManagedEntity> entity = entityManager.getEntity(entityID,version);

    // Create the request, since it is how we will generically return complete.
    ServerEntityRequest request = make(rep);
    // If we satisfied this as a known re-send, don't add the request to the entity.
    if (request.getAction() == ServerEntityAction.CREATE_ENTITY) {
// The common pattern for this is to pass an empty array on success ("found") or an exception on failure ("not found").
      long consumerID = this.entityPersistor.getNextConsumerID();
      try {
        LOGGER.debug("entity create called " + entityID);
        ManagedEntity temp = entityManager.createEntity(entityID, descriptor.getClientSideVersion(), consumerID, !sourceNodeID.isNull());
        temp.addRequestMessage(request, new MessagePayload(extendedData, null), 
          (result) -> {
            if (!sourceNodeID.isNull()) {
              entityPersistor.entityCreated(sourceNodeID, transactionID.toLong(), oldestTransactionOnClient.toLong(), entityID, descriptor.getClientSideVersion(), consumerID, true /*from client checked*/, extendedData);
              acknowledge(rep);
            } else {
              entityPersistor.entityCreatedNoJournal(entityID, descriptor.getClientSideVersion(), consumerID, true, extendedData);
              acknowledge(rep);
            }
          }, (exception) -> {
            entityManager.removeDestroyed(entityID);
            entityPersistor.entityCreateFailed(sourceNodeID, transactionID.toLong(), oldestTransactionOnClient.toLong(), exception);
            acknowledge(rep);
          });
      } catch (EntityException ee) {
        acknowledge(rep);
        if (!sourceNodeID.isNull()) {
          entityPersistor.entityCreateFailed(sourceNodeID, transactionID.toLong(), oldestTransactionOnClient.toLong(), ee);
        }
      }
    } else if (entity.isPresent()) {
      ManagedEntity entityInstance = entity.get();
      EntityMessage msg = null;
      try {
        if (rep.getReplicationType() == ReplicationMessage.ReplicationType.INVOKE_ACTION) {
          msg = entityInstance.getCodec().decodeMessage(extendedData);
        }
      } catch (MessageCodecException codec) {
        throw new RuntimeException(codec);
      }
      MessagePayload payload = new MessagePayload(extendedData, msg, rep.getConcurrency());
      if (null != request.getAction()) switch (request.getAction()) {
        case RECONFIGURE_ENTITY:  
          entity.get().addRequestMessage(request, payload, 
            (result)->{
              entityPersistor.entityReconfigureSucceeded(sourceNodeID, transactionID.toLong(), oldestTransactionOnClient.toLong(), entityID, version, result);
              acknowledge(rep);
            } , (exception) -> {
              entityPersistor.entityReconfigureFailed(sourceNodeID, transactionID.toLong(), oldestTransactionOnClient.toLong(), exception);
              acknowledge(rep);
            });
          break;
        case DESTROY_ENTITY:
          entityInstance.addRequestMessage(request, payload, 
            (result)-> {
              if (!entityManager.removeDestroyed(entityID)) {
                throw new AssertionError();
              }
              entityPersistor.entityDestroyed(sourceNodeID, transactionID.toLong(), oldestTransactionOnClient.toLong(), entityID);
              acknowledge(rep);
            }, (exception) -> {
              entityPersistor.entityDestroyFailed(sourceNodeID, transactionID.toLong(), oldestTransactionOnClient.toLong(), exception);
              acknowledge(rep);
            });
          break;
        default:
          entityInstance.addRequestMessage(request, payload, (result)-> acknowledge(rep), (exception) -> acknowledge(rep));
          break;
      }
    }
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
  
  private void syncMessageReceived(ReplicationMessage sync) {
    EntityID eid = sync.getEntityDescriptor().getEntityID();
    long version = sync.getVersion();
    
    ackReceived(sync);
    beforeSyncAction(sync);
    
    if (sync.getReplicationType() == ReplicationMessage.ReplicationType.SYNC_ENTITY_BEGIN && !eid.equals(EntityID.NULL_ID)) {
      try {
        if (!this.entityManager.getEntity(eid, sync.getVersion()).isPresent()) {
          long consumerID = entityPersistor.getNextConsumerID();
 //  repurposed concurrency id to tell passive if entity can be deleted 0 for deletable and 1 for not deletable
          ManagedEntity temp = this.entityManager.createEntity(eid, sync.getVersion(), consumerID, sync.getConcurrency() == 0);          
          // We record this in the persistor but not record it in the journal since it has no originating client and can't be re-sent. 
          this.entityPersistor.entityCreatedNoJournal(eid, version, consumerID, !sync.getSource().isNull(), sync.getExtendedData());
        } else {
          Assert.fail("this entity should not be here");
        }
      } catch (EntityException exception) {
//  TODO: this needs to be controlled.  
        LOGGER.warn("entity has already been created", exception);
      }
    }
        
    try {
      Optional<ManagedEntity> entity = entityManager.getEntity(eid, version);
      if (entity.isPresent()) {
        EntityMessage msg = null;
        try {
          if (sync.getReplicationType() == ReplicationMessage.ReplicationType.SYNC_ENTITY_CONCURRENCY_PAYLOAD) {
              msg = this.entityManager.getSyncMessageCodec(eid).decode(sync.getConcurrency(), sync.getExtendedData());
          }
        } catch (MessageCodecException codec) {
          throw new RuntimeException(codec);
        }
        MessagePayload payload = new MessagePayload(sync.getExtendedData(), msg, sync.getConcurrency());
        entity.get().addRequestMessage(make(sync), payload, (result)->acknowledge(sync), (exception)->acknowledge(sync));
        if (sync.getReplicationType() != ReplicationMessage.ReplicationType.SYNC_ENTITY_CONCURRENCY_PAYLOAD) {
          entity.get().addRequestMessage(makeNoop(eid, version), MessagePayload.EMPTY, null, null);
        }
      } else {
        if (!eid.equals(EntityID.NULL_ID)) {
          throw new AssertionError();
        } else {
          MessagePayload payload = new MessagePayload(sync.getExtendedData(), null, sync.getConcurrency());
          platform.addRequestMessage(make(sync), payload, (result)-> {
            if (sync.getReplicationType() == ReplicationMessage.ReplicationType.SYNC_END) {
              moveToPassiveStandBy();
            }
            acknowledge(sync);
          }, (exception)->acknowledge(sync));
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
  
  private void scheduleDeferred(Deque<ReplicationMessage> deferred) {
    if (deferred != null) {
      while(!deferred.isEmpty()) {
        ReplicationMessage r = deferred.pop();
        try {
          Assert.assertTrue(r.getType() == ReplicationMessage.REPLICATE);
          replicatedMessageReceived(r);
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
  
  private ServerEntityRequest makeNoop(EntityID eid, long version) {
    // Anything created within this class represents a replicated message.
    boolean isReplicatedMessage = true;
    return new ServerEntityRequestResponse(new EntityDescriptor(eid, ClientInstanceID.NULL_ID, version), ServerEntityAction.NOOP, TransactionID.NULL_ID, TransactionID.NULL_ID, ClientID.NULL_ID, true, Optional.empty(), isReplicatedMessage);
  }
      
  private ServerEntityRequest make(ReplicationMessage rep) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Making " + rep);
    }

    return new BasicServerEntityRequest(decodeReplicationType(rep.getReplicationType()), rep.getSource(),  
      rep.getTransactionID(), rep.getOldestTransactionOnClient(), rep.getEntityDescriptor());
  }
  
  private void beforeSyncAction(ReplicationMessage rep) {
    EntityID eid = rep.getEntityDescriptor().getEntityID();
    switch (rep.getReplicationType()) {
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
        start(eid, rep.getConcurrency());
        break;
      case SYNC_ENTITY_CONCURRENCY_END:
        finish(eid, rep.getConcurrency());// finish inline so messages are requeued from the proper sync
        break;
      default:
        break;
    }
  }

  private void ackReceived(ReplicationMessage rep) {
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("acking(received) " + rep);
      }
      if (!rep.messageFrom().equals(ServerID.NULL_ID)) {
        groupManager.sendTo(rep.messageFrom(), ReplicationMessageAck.createReceivedAck(rep.getMessageID()));
      }
    } catch (GroupException ge) {
      // Active must have died.  Swallow the exception after logging.
      LOGGER.warn("active died on received ack", ge);
    }
  }

  private void acknowledge(ReplicationMessage rep) {
//  when is the right time to send the ack?
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("acking(completed) " + rep);
      }
      if (!rep.messageFrom().equals(ServerID.NULL_ID)) {
        groupManager.sendTo(rep.messageFrom(), ReplicationMessageAck.createCompletedAck(rep.getMessageID()));
      }
    } catch (GroupException ge) {
      // Active must have died.  Swallow the exception after logging.
      LOGGER.warn("active died on ack", ge);
    }
  }

  private static ServerEntityAction decodeReplicationType(ReplicationMessage.ReplicationType networkType) {
    switch(networkType) {
      case SYNC_BEGIN:
      case SYNC_END:
      case NOOP:
        return ServerEntityAction.NOOP;
      case CREATE_ENTITY:
        return ServerEntityAction.CREATE_ENTITY;
      case RECONFIGURE_ENTITY:
        return ServerEntityAction.RECONFIGURE_ENTITY;
      case INVOKE_ACTION:
        return ServerEntityAction.INVOKE_ACTION;
      case RELEASE_ENTITY:
        return ServerEntityAction.RELEASE_ENTITY;
      case DESTROY_ENTITY:
        return ServerEntityAction.DESTROY_ENTITY;
      case SYNC_ENTITY_BEGIN:
        return ServerEntityAction.RECEIVE_SYNC_ENTITY_START;
      case SYNC_ENTITY_CONCURRENCY_BEGIN:
        return ServerEntityAction.RECEIVE_SYNC_ENTITY_KEY_START;
      case SYNC_ENTITY_CONCURRENCY_PAYLOAD:
        return ServerEntityAction.RECEIVE_SYNC_PAYLOAD;
      case SYNC_ENTITY_CONCURRENCY_END:
        return ServerEntityAction.RECEIVE_SYNC_ENTITY_KEY_END;
      case SYNC_ENTITY_END:
        return ServerEntityAction.RECEIVE_SYNC_ENTITY_END;
      default:
        throw new AssertionError("bad replication type");
    }
  }  
  
 private class SyncState {
    private LinkedList<ReplicationMessage> defer = new LinkedList<>();
    
    private final Set<EntityID> syncdEntities = new HashSet<>();
    private final Set<Integer> syncdKeys = new HashSet<>();
    private EntityID syncing;
    private int currentKey = -1;
    private boolean destroyed = false;
    private boolean finished = false;
    private boolean started = false;
    
    private void start() {
      started = true;
    }
    
    private void startEntity(EntityID eid) {
      Assert.assertNull(syncing);
      syncing = eid;
      LOGGER.debug("Starting " + eid);
    }
    
    private boolean destroyed(EntityID eid) {
      return (eid.equals(syncing) && destroyed) || syncdEntities.contains(eid);
    }
    
    private void endEntity(EntityID eid) {
      Assert.assertEquals(syncing, eid);
      syncdEntities.add(eid);
      syncdKeys.clear();
      syncing = null;
      LOGGER.debug("Ending " + eid);
    }
    
    private void startConcurrency(EntityID eid, int concurrency) {
      if (destroyed && !syncing.equals(eid)) {
        destroyed = false;
      }
      Assert.assertEquals(syncing, eid);
      currentKey = concurrency;
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Starting " + eid + "/" + currentKey);
      }
    }
    
    private Deque<ReplicationMessage> endConcurrency(EntityID eid, int concurrency) {
      try {
        if (!eid.equals(syncing) || concurrency != currentKey) {
          throw new AssertionError();
        }
        Assert.assertEquals(syncing, eid);
        Assert.assertEquals(currentKey, concurrency);
        syncdKeys.add(concurrency);
        currentKey = -1;
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Ending " + eid + "/" + currentKey);
        }
        return defer;
      } finally {
        defer = new LinkedList<>();
      }
    }
    
    private void finish() {
      syncdEntities.clear();
      finished = true;
    }
    
    private boolean ignore(ReplicationMessage rep) {
      if (finished) {
//  done with sync, need to apply everything now
        return false;
      }
      if (rep.getReplicationType() == ReplicationMessage.ReplicationType.NOOP) {
        return false;
      }
      EntityID eid = rep.getEntityDescriptor().getEntityID();
//  everything else, check
      if (eid.equals(syncing)) {
        if (destroyed) {
//  blackhole this request.  The entity has been destroyed. 
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Dropping " + rep + " due to destroy");
          }
          return true;
        } else if (rep.getConcurrency() == currentKey) {
          return false;
        } else if (!syncdKeys.contains(rep.getConcurrency())) {
//  ignore, haven't gotten to this key yet
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Ignoring " + rep);
          }
          return true;
        }
      }
      return false;
    }

    private boolean defer(ReplicationMessage rep) {
      if (finished) {
//  done with sync, need to apply everything now
        return false;
      }
//  everything else, check
      EntityID eid = rep.getEntityDescriptor().getEntityID();
      if (syncdEntities.contains(eid)) {
        return false;
      } 
      
      if (rep.getReplicationType() == ReplicationMessage.ReplicationType.CREATE_ENTITY) {
        syncdEntities.add(eid);
        destroyed = false;
        return false;
      }
      
      if (eid.equals(syncing)) {
        if (syncdKeys.contains(rep.getConcurrency())) {
          return false;
        } else if (rep.getReplicationType() == ReplicationMessage.ReplicationType.NOOP) {
//  NOOP requests cannot be deferred
          return false;
        } else if (rep.getReplicationType() == ReplicationMessage.ReplicationType.DESTROY_ENTITY) {
          defer.clear();
          defer.add(rep);
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Destroying " + rep);
          }
          destroyed = true;
          return false;
        } else if (currentKey == rep.getConcurrency()) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Deferring " + rep);
          }
          defer.add(rep);
          return true;
        }
      }
      return false;
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
}
