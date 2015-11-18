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
import com.tc.async.api.EventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.l2.msg.ReplicationMessage;
import com.tc.l2.msg.ReplicationMessageAck;
import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.object.EntityID;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.objectserver.entity.ServerEntityRequestImpl;
import com.tc.objectserver.persistence.EntityPersistor;
import com.tc.objectserver.persistence.TransactionOrderPersistor;
import com.tc.util.Assert;
import java.util.Optional;

import org.terracotta.exception.EntityException;


public class ReplicatedTransactionHandler {
  private static final TCLogger LOGGER = TCLogging.getLogger(ReplicatedTransactionHandler.class);
  private final EntityManager entityManager;
  private final EntityPersistor entityPersistor;
  private final GroupManager groupManager;
  private final TransactionOrderPersistor orderedTransactions;
  private final StateManager stateManager;
  
  public ReplicatedTransactionHandler(StateManager state, TransactionOrderPersistor transactionOrderPersistor, 
      EntityManager manager, EntityPersistor entityPersistor, GroupManager groupManager) {
    this.stateManager = state;
    this.entityManager = manager;
    this.entityPersistor = entityPersistor;
    this.groupManager = groupManager;
    this.orderedTransactions = transactionOrderPersistor;
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
  };
  
  public EventHandler<ReplicationMessage> getEventHandler() {
    return eventHorizon;
  }

  private void processMessage(ReplicationMessage rep) throws EntityException {
    if (rep.getType() == ReplicationMessage.REPLICATE) {
      if (!rep.getOldestTransactionOnClient().isNull()) {
        orderedTransactions.updateWithNewMessage(rep.getSource(), rep.getTransactionID(), rep.getOldestTransactionOnClient());
      } else {
        orderedTransactions.removeTrackingForClient(rep.getSource());
      }
      if (true) {
        if (rep.getReplicationType() == ReplicationMessage.ReplicationType.CREATE_ENTITY) {
          long consumerID = entityPersistor.getNextConsumerID();
          entityManager.createEntity(rep.getEntityDescriptor().getEntityID(), rep.getVersion(), consumerID);
          this.entityPersistor.entityCreated(rep.getEntityDescriptor().getEntityID(), rep.getVersion(), consumerID, rep.getExtendedData());
        }
        Optional<ManagedEntity> entity = entityManager.getEntity(rep.getEntityDescriptor().getEntityID(),rep.getVersion());
        if (entity.isPresent()) {
          ServerEntityRequest request = make(rep);
          if (request != null) {
            entity.get().addRequest(request);
          }
        }
        if (rep.getReplicationType() == ReplicationMessage.ReplicationType.DESTROY_ENTITY) {
          entityManager.destroyEntity(rep.getEntityDescriptor().getEntityID());
        }
      }
//  when is the right time to send the ack?
      try {
        groupManager.sendTo(rep.messageFrom(), new ReplicationMessageAck(rep.getMessageID()));
      } catch (GroupException ge) {
//  Passive must have died.  Swallow the exception
        LOGGER.info("passive died on ack", ge);
      }
      return;
    } else if (rep.getType() == ReplicationMessage.SYNC) {
      syncMessageReceived(rep);
      return;
    }

    throw new RuntimeException();
  }
  
  private void syncMessageReceived(ReplicationMessage sync) {
    EntityID eid = sync.getEntityDescriptor().getEntityID();
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

    switch (sync.getReplicationType()) {
      case SYNC_END:
        if (!stateManager.isActiveCoordinator()) {
          stateManager.moveToPassiveStandbyState();
        }
        break;
      case SYNC_BEGIN:
        break;
      case SYNC_ENTITY_BEGIN:
      case SYNC_ENTITY_END:
      case SYNC_ENTITY_CONCURRENCY_BEGIN:
      case SYNC_ENTITY_CONCURRENCY_END:
      case SYNC_ENTITY_CONCURRENCY_PAYLOAD:
        try {
          Optional<ManagedEntity> entity = entityManager.getEntity(eid,sync.getVersion());
          if (entity.isPresent()) {
              ServerEntityRequest request = make(sync);
              if (request != null) {
                entity.get().addRequest(request);
              }
          }
        } catch (EntityException ee) {
          throw new RuntimeException(ee);
        }
        break;
      default:
        throw new AssertionError("not a sync message");
    }
  }
    
  private ServerEntityRequest make(ReplicationMessage rep) {
    return new ServerEntityRequestImpl(rep.getEntityDescriptor(), decodeReplicationType(rep.getReplicationType()),
        rep.getExtendedData(), rep.getTransactionID(), rep.getOldestTransactionOnClient(), rep.getSource(), false, Optional.empty());
  }

  private static ServerEntityAction decodeReplicationType(ReplicationMessage.ReplicationType networkType) {
    switch(networkType) {
      case NOOP:
        return ServerEntityAction.NOOP;
      case CREATE_ENTITY:
        return ServerEntityAction.CREATE_ENTITY;
      case INVOKE_ACTION:
        return ServerEntityAction.INVOKE_ACTION;
//      case GET_ENTITY:
//  should not happen
//        return ServerEntityAction.FETCH_ENTITY;
      case RELEASE_ENTITY:
        return ServerEntityAction.RELEASE_ENTITY;
      case DESTROY_ENTITY:
        return ServerEntityAction.DESTROY_ENTITY;
//      case PROMOTE_ENTITY_TO_ACTIVE:
//  should not happen
//        return ServerEntityAction.PROMOTE_ENTITY_TO_ACTIVE;
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
}
