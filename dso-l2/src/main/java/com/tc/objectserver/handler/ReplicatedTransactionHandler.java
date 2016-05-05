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
import com.tc.objectserver.entity.PlatformEntity;
import com.tc.objectserver.entity.ServerEntityRequestImpl;
import com.tc.objectserver.persistence.EntityPersistor;
import com.tc.objectserver.persistence.TransactionOrderPersistor;
import com.tc.util.Assert;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import org.terracotta.entity.ConcurrencyStrategy;

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
      requestPassiveSync();
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
  
  private void replicatedMessageReceived(ReplicationMessage rep) throws EntityException {
    ClientID sourceNodeID = rep.getSource();
    TransactionID transactionID = rep.getTransactionID();
    TransactionID oldestTransactionOnClient = rep.getOldestTransactionOnClient();

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
    EntityID entityID = rep.getEntityDescriptor().getEntityID();
    byte[] extendedData = rep.getExtendedData();
    ReplicationMessage.ReplicationType replicationType = rep.getReplicationType();
    boolean didAlreadyHandle = false;

    if (ReplicationMessage.ReplicationType.CREATE_ENTITY == replicationType) {
      long consumerID = entityPersistor.getNextConsumerID();
      // Call the common helper to either create the entity on our behalf or succeed/fail, as last time, if this is a re-send.
      didAlreadyHandle = EntityExistenceHelpers.createEntityReturnWasCached(this.entityPersistor, this.entityManager, sourceNodeID, transactionID, oldestTransactionOnClient, entityID, version, consumerID, extendedData);
    }
    if (ReplicationMessage.ReplicationType.RECONFIGURE_ENTITY == replicationType) {
      byte[] cachedResult = EntityExistenceHelpers.reconfigureEntityReturnCachedResult(this.entityPersistor, this.entityManager, sourceNodeID, transactionID, oldestTransactionOnClient, entityID, version, extendedData);
      didAlreadyHandle = (null != cachedResult);
    }
    // At this point, we can now look up the managed entity (used later).
    Optional<ManagedEntity> entity = entityManager.getEntity(entityID,version);
    if (ReplicationMessage.ReplicationType.DESTROY_ENTITY == replicationType) {
      // Call the common helper to either destroy the entity on our behalf or succeed/fail, as last time, if this is a re-send.
      didAlreadyHandle = EntityExistenceHelpers.destroyEntityReturnWasCached(this.entityPersistor, this.entityManager, sourceNodeID, transactionID, oldestTransactionOnClient, entityID);
    }

    // Create the request, since it is how we will generically return complete.
    ServerEntityRequest request = make(rep);
    // If we satisfied this as a known re-send, don't add the request to the entity.
    if (didAlreadyHandle) {
      request.complete();
    } else {
      // Handle the DOES_EXIST, as a special case, since we don't tell the entity about it.
      if (ReplicationMessage.ReplicationType.DOES_EXIST == replicationType) {
        // We don't actually care about the response.  We just need the question and answer to be recorded, at this point in time.
        EntityExistenceHelpers.doesExist(this.entityPersistor, sourceNodeID, transactionID, oldestTransactionOnClient, entityID);
        request.complete();
      } else {
        if (entity.isPresent()) {
          if (request != null) {
            ManagedEntity entityInstance = entity.get();
            if (null != request.getAction()) switch (request.getAction()) {
              case INVOKE_ACTION:
              case NOOP:
                entityInstance.addInvokeRequest(request, extendedData, rep.getConcurrency());
                break;
              default:
                entityInstance.addLifecycleRequest(request, extendedData);
                break;
            }
          }
        } else {
// must be syncing and this entity has not been sync'd yet.  just complete like 
// we weren't here
          request.complete();
        }
      }
    }
  }
  
  private void requestPassiveSync() {
    NodeID node = stateManager.getActiveNodeID();
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
    if (sync.getReplicationType() == ReplicationMessage.ReplicationType.SYNC_ENTITY_BEGIN && !eid.equals(EntityID.NULL_ID)) {
      try {
        if (!this.entityManager.getEntity(eid, sync.getVersion()).isPresent()) {
          long consumerID = entityPersistor.getNextConsumerID();
          this.entityManager.createEntity(eid, sync.getVersion(), consumerID);
          // We record this in the persistor but not record it in the journal since it has no originating client and can't be re-sent. 
          this.entityPersistor.entityCreatedNoJournal(eid, version, consumerID, sync.getExtendedData());
        }
      } catch (EntityException exception) {
//  TODO: this needs to be controlled.  
        LOGGER.warn("entity has already been created", exception);
      }
    }
        
    try {
      Optional<ManagedEntity> entity = entityManager.getEntity(eid, version);
      if (entity.isPresent()) {
        entity.get().addSyncRequest(make(sync), sync.getExtendedData(), sync.getConcurrency());
        if (sync.getReplicationType() != ReplicationMessage.ReplicationType.SYNC_ENTITY_CONCURRENCY_PAYLOAD) {
          entity.get().addSyncRequest(makeNoop(eid, version), new byte[0], ConcurrencyStrategy.MANAGEMENT_KEY);
        }
      } else {
        if (!eid.equals(EntityID.NULL_ID)) {
          throw new AssertionError();
        } else {
          platform.addSyncRequest(make(sync), sync.getExtendedData(), ConcurrencyStrategy.MANAGEMENT_KEY);
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
    return new ServerEntityRequestImpl(new EntityDescriptor(eid, ClientInstanceID.NULL_ID, version), ServerEntityAction.NOOP, TransactionID.NULL_ID, TransactionID.NULL_ID, ClientID.NULL_ID, true, Optional.empty());
  }
      
  private ServerEntityRequestImpl make(ReplicationMessage rep) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Making " + rep);
    }
    EntityID eid = rep.getEntityDescriptor().getEntityID();
    
    switch (rep.getReplicationType()) {
      case SYNC_BEGIN:
        start();
        return new ServerEntityRequestWithCompletion(rep.getEntityDescriptor(), decodeReplicationType(rep.getReplicationType()), 
          rep.getTransactionID(), rep.getOldestTransactionOnClient(), rep.getSource(), false, ()->acknowledge(rep));
      case SYNC_END:
        finish();
        return new ServerEntityRequestWithCompletion(rep.getEntityDescriptor(), decodeReplicationType(rep.getReplicationType()), 
          rep.getTransactionID(), rep.getOldestTransactionOnClient(), rep.getSource(), false, ()->{moveToPassiveStandBy();acknowledge(rep);});
      case SYNC_ENTITY_BEGIN:
        start(eid);
        return new ServerEntityRequestWithCompletion(rep.getEntityDescriptor(), decodeReplicationType(rep.getReplicationType()), 
          rep.getTransactionID(), rep.getOldestTransactionOnClient(), rep.getSource(), false, ()->acknowledge(rep));      
      case SYNC_ENTITY_END:
        finish(eid);
        return new ServerEntityRequestWithCompletion(rep.getEntityDescriptor(), decodeReplicationType(rep.getReplicationType()), 
          rep.getTransactionID(), rep.getOldestTransactionOnClient(), rep.getSource(), false, ()->acknowledge(rep));      
      case SYNC_ENTITY_CONCURRENCY_BEGIN:
        start(eid, rep.getConcurrency());
        return new ServerEntityRequestWithCompletion(rep.getEntityDescriptor(), decodeReplicationType(rep.getReplicationType()), 
          rep.getTransactionID(), rep.getOldestTransactionOnClient(), rep.getSource(), false, ()->acknowledge(rep));
      case SYNC_ENTITY_CONCURRENCY_END:
        finish(eid, rep.getConcurrency());
        return new ServerEntityRequestWithCompletion(rep.getEntityDescriptor(), decodeReplicationType(rep.getReplicationType()), 
          rep.getTransactionID(), rep.getOldestTransactionOnClient(), rep.getSource(), false, ()->acknowledge(rep));
      default:
        return new ServerEntityRequestWithCompletion(rep.getEntityDescriptor(), decodeReplicationType(rep.getReplicationType()), 
          rep.getTransactionID(), rep.getOldestTransactionOnClient(), rep.getSource(), false, ()->acknowledge(rep));
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
      case DOES_EXIST:
        return ServerEntityAction.DOES_EXIST;
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
    }
    
    private boolean destroyed(EntityID eid) {
      return (eid.equals(syncing) && destroyed) || syncdEntities.contains(eid);
    }
    
    private void endEntity(EntityID eid) {
      Assert.assertEquals(syncing, eid);
      syncdEntities.add(eid);
      syncdKeys.clear();
      syncing = null;
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
  
  private static class ServerEntityRequestWithCompletion extends ServerEntityRequestImpl {
    
    private final Runnable onComplete;

    public ServerEntityRequestWithCompletion(EntityDescriptor descriptor, ServerEntityAction action, TransactionID transaction, TransactionID oldest, ClientID src, boolean requiresReplication, Runnable onComplete) {
      super(descriptor, action, transaction, oldest, src, requiresReplication, Optional.empty());
      this.onComplete = onComplete;
    }

    @Override
    public synchronized void complete() {
      super.complete();
      completion();
    }

    @Override
    public synchronized void complete(byte[] value) {
      super.complete(value); //To change body of generated methods, choose Tools | Templates.
      completion();
   }
    
    private void completion() {
      if (onComplete != null) {
        onComplete.run();
      }
    }
  }
}
