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
import com.tc.l2.msg.ReplicationMessage;
import com.tc.l2.msg.ReplicationMessageAck;
import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.object.EntityID;
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
import java.util.LinkedList;
import java.util.Optional;

import org.terracotta.exception.EntityException;


public class ReplicatedTransactionHandler {
  private static final TCLogger LOGGER = TCLogging.getLogger(ReplicatedTransactionHandler.class);
  private final EntityManager entityManager;
  private final EntityPersistor entityPersistor;
  private final GroupManager groupManager;
  private final TransactionOrderPersistor orderedTransactions;
  private final StateManager stateManager;
  private final ManagedEntity platform;
  
  private final SyncState state = new SyncState();
  
  public ReplicatedTransactionHandler(StateManager state, TransactionOrderPersistor transactionOrderPersistor, 
      EntityManager manager, EntityPersistor entityPersistor, GroupManager groupManager) {
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
      } catch (EntityException e) {
        // We don't expect to see an exception executing a replicated message.
        // TODO:  Find a better way to handle this error.
        Assert.failure("Unexpected exception executing replicated message", e);
      }
    }

    @Override
    protected void initialize(ConfigurationContext context) {
      ServerConfigurationContext scxt = (ServerConfigurationContext)context;
  //  when this spins up, send  request to active and ask for sync
      scxt.getL2Coordinator().getReplicatedClusterStateManager().setCurrentState(scxt.getL2Coordinator().getStateManager().getCurrentState());
      requestPassiveSync();
    }
    
    
  };
  
  public EventHandler<ReplicationMessage> getEventHandler() {
    return eventHorizon;
  }

  private void processMessage(ReplicationMessage rep) throws EntityException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Received replicated " + rep.getReplicationType() + " on " + rep.getEntityID() + "/" + rep.getConcurrency());
    }
    if (rep.getType() == ReplicationMessage.REPLICATE) {
      if (!state.defer(rep)) {
        if (!rep.getOldestTransactionOnClient().isNull()) {
          orderedTransactions.updateWithNewMessage(rep.getSource(), rep.getTransactionID(), rep.getOldestTransactionOnClient());
        } else {
          orderedTransactions.removeTrackingForClient(rep.getSource());
        }
        if (true) {
          if (rep.getReplicationType() == ReplicationMessage.ReplicationType.CREATE_ENTITY) {
            long consumerID = entityPersistor.getNextConsumerID();
            entityManager.createEntity(rep.getEntityDescriptor().getEntityID(), rep.getVersion(), consumerID);
            entityPersistor.entityCreated(rep.getEntityDescriptor().getEntityID(), rep.getVersion(), consumerID, rep.getExtendedData());
          }
          Optional<ManagedEntity> entity = entityManager.getEntity(rep.getEntityDescriptor().getEntityID(),rep.getVersion());

          if (rep.getReplicationType() == ReplicationMessage.ReplicationType.DESTROY_ENTITY) {
            entityManager.destroyEntity(rep.getEntityDescriptor().getEntityID());
            entityPersistor.entityDeleted(rep.getEntityDescriptor().getEntityID());
          }
          if (entity.isPresent()) {
            ServerEntityRequest request = make(rep);
            if (request != null) {
              if (request.getAction() == ServerEntityAction.INVOKE_ACTION) {
                entity.get().addInvokeRequest(request, rep.getExtendedData());
              } else {
                entity.get().addLifecycleRequest(request, rep.getExtendedData());
              }
            }
          }
        }
//  when is the right time to send the ack?
        acknowledge(rep);
      }
      return;
    } else if (rep.getType() == ReplicationMessage.SYNC) {
//  when is the right time to send the ack?  send it early for passive sync to keep the messages flowing
//  TODO:  need some kind of feedback mechanism to slow sync if needed
      try {
        groupManager.sendTo(rep.messageFrom(), new ReplicationMessageAck(rep.getMessageID()));
      } catch (GroupException ge) {
//  Passive must have died.  Swallow the exception
        LOGGER.info("passive died on ack", ge);
      }
      syncMessageReceived(rep);
      return;
    } else if (rep.getType() == ReplicationMessage.START) {
      acknowledge(rep);
      return;
    }

    throw new RuntimeException();
  }
  
  private void requestPassiveSync() {
    NodeID node = stateManager.getActiveNodeID();
    try {
      groupManager.sendTo(node, new ReplicationMessageAck(ReplicationMessage.START));
    } catch (GroupException ge) {
      LOGGER.warn("can't request passive sync", ge);
    }
  }  
  
  private void syncMessageReceived(ReplicationMessage sync) {
    EntityID eid = sync.getEntityDescriptor().getEntityID();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Sync Message " + sync.getReplicationType() + " for " + eid + "/" + sync.getConcurrency());
    }
    long version = sync.getVersion();
    if (!eid.equals(EntityID.NULL_ID)) {
      try {
        if (!this.entityManager.getEntity(eid, sync.getVersion()).isPresent()) {
          long consumerID = entityPersistor.getNextConsumerID();
          this.entityManager.createEntity(eid, sync.getVersion(), consumerID);
          this.entityPersistor.entityCreated(eid, sync.getVersion(), consumerID, sync.getExtendedData());
        }
      } catch (EntityException state) {
//  TODO: this needs to be controlled.  
        LOGGER.warn("entity has already been created", state);
      }
    }
    
    Deque<ReplicationMessage> deferred = null;
    ServerEntityRequestImpl request = make(sync);
    
    try {
      Optional<ManagedEntity> entity = entityManager.getEntity(eid, version);
      if (entity.isPresent()) {
        entity.get().processSyncMessage(request, sync.getExtendedData(), sync.getConcurrency());
      } else {
        platform.processSyncMessage(request, sync.getExtendedData(), sync.getConcurrency());
      }
    } catch (EntityException ee) {
      throw new RuntimeException(ee);
    }
    
    switch (sync.getReplicationType()) {
      case SYNC_END:
        request.waitForDone();
        if (!stateManager.isActiveCoordinator()) {
          stateManager.moveToPassiveStandbyState();
        }
        break;
      case SYNC_BEGIN:
        request.waitForDone();
        break;
      case SYNC_ENTITY_CONCURRENCY_BEGIN:
        request.waitForDone();
        state.start(eid, sync.getConcurrency());
        break;
      case SYNC_ENTITY_CONCURRENCY_END:
        request.waitForDone();
        deferred = state.end(eid, sync.getConcurrency());
        break;
      case SYNC_ENTITY_BEGIN:
      case SYNC_ENTITY_END:
        request.waitForDone();
      case SYNC_ENTITY_CONCURRENCY_PAYLOAD:
        break;
      default:
        throw new AssertionError("not a sync message");
    }
    
    if (deferred != null) {
      while(!deferred.isEmpty()) {
        ReplicationMessage r = deferred.pop();
        r.setMessageOrginator(ServerID.NULL_ID);
        try {
          processMessage(r);
        } catch (EntityException ee) {
          throw new RuntimeException(ee);
        }
      }
    }
  }
    
  private ServerEntityRequestImpl make(ReplicationMessage rep) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Making " + rep.getReplicationType() + " for " + 
          rep.getEntityDescriptor().getEntityID() + "/" + rep.getConcurrency());
    }
    return new ServerEntityRequestImpl(rep.getEntityDescriptor(), decodeReplicationType(rep.getReplicationType()), rep.getTransactionID(), rep.getOldestTransactionOnClient(), rep.getSource(), false, Optional.empty());
  }
  
  private void acknowledge(ReplicationMessage rep) {
//  when is the right time to send the ack?
    try {
      groupManager.sendTo(rep.messageFrom(), new ReplicationMessageAck(rep.getMessageID()));
    } catch (GroupException ge) {
//  Passive must have died.  Swallow the exception
      LOGGER.info("passive died on ack", ge);
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
    
    private EntityID syncing;
    private int currentKey = -1;
    private boolean destroyed = false;
    
    
    public void start(EntityID eid, int concurrency) {
      if (destroyed && !syncing.equals(eid)) {
        destroyed = false;
      }
      syncing = eid;
      currentKey = concurrency;
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Starting " + eid + "/" + currentKey);
      }
    }
    
    public Deque<ReplicationMessage> end(EntityID eid, int concurrency) {
      try {
        if (!eid.equals(syncing) || concurrency != currentKey) {
          throw new AssertionError();
        }
        syncing = null;
        concurrency = -1;
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Ending " + eid + "/" + currentKey);
        }
        return defer;
      } finally {
        defer = new LinkedList<>();
      }
    }
    
    private boolean defer(ReplicationMessage rep) {
      EntityID eid = rep.getEntityDescriptor().getEntityID();
      if (syncing != null) {
        if (eid.equals(syncing)) {
          if (destroyed) {
//  blackhole this request.  The entity has been destroyed. 
            acknowledge(rep);
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Dropping " + rep.getReplicationType() + " for " + eid + "/" + rep.getConcurrency() + " due to destroy");
            }
            return true;
          } else if (rep.getReplicationType() == ReplicationMessage.ReplicationType.DESTROY_ENTITY) {
            defer.forEach(q->acknowledge(q));
            defer.clear();
            defer.add(rep);
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Destroying " + rep.getReplicationType() + " for " + eid + "/" + rep.getConcurrency());
            }
            destroyed = true;
          } else if (currentKey == rep.getConcurrency()) {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Deferring " + rep.getReplicationType() + " for " + eid + "/" + rep.getConcurrency());
            }
            defer.add(rep);
            return true;
          }
        }
      }
      return false;
    }
  }  
}
