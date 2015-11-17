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
import com.tc.entity.VoltronEntityMessage.Type;
import com.tc.l2.msg.ReplicationMessage;
import com.tc.l2.msg.ReplicationMessageAck;
import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ServerID;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.object.EntityID;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.objectserver.entity.ServerEntityRequestImpl;
import com.tc.objectserver.persistence.EntityPersistor;
import com.tc.objectserver.persistence.TransactionOrderPersistor;
import com.tc.util.Assert;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;

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
        int action = rep.getAction();
        if (action == ReplicationMessage.ReplicationType.CREATE_ENTITY.ordinal()) {
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
        if (ReplicationMessage.ReplicationType.DESTROY_ENTITY.ordinal() == rep.getAction()) {
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
      messageReceived(rep);
      return;
    }

    throw new RuntimeException();
  }
  
  private void messageReceived(ReplicationMessage sync) {
    EntityID eid = sync.getEntityDescriptor().getEntityID();
    switch (sync.getReplicationType()) {
      case SYNC_END:
        if (!stateManager.isActiveCoordinator()) {
          stateManager.moveToPassiveStandbyState();
        }
        break;
      case SYNC_BEGIN:
        //  do something
        break;
      case SYNC_ENTITY_BEGIN:
        try {
          if (!this.entityManager.getEntity(eid, sync.getVersion()).isPresent()) {
            long consumerID = entityPersistor.getNextConsumerID();
            this.entityManager.createEntity(eid, sync.getVersion(), consumerID);
            this.entityPersistor.entityCreated(eid, sync.getVersion(), consumerID, sync.getExtendedData());
            Optional<ManagedEntity> entity = entityManager.getEntity(eid,sync.getVersion());
            if (entity.isPresent()) {
              ServerEntityRequest request = make(sync);
              if (request != null) {
                entity.get().addRequest(request);
              }
            }
          }
        } catch (EntityException state) {
//  TODO: this needs to be controlled.  
          LOGGER.warn("entity has already been created", state);
        }
        break;
      case SYNC_ENTITY_END:
        //  do something?
        break;
      case SYNC_ENTITY_CONCURRENCY_BEGIN:
        break;
      case SYNC_ENTITY_CONCURRENCY_END:
        break;
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
    Type type = rep.getVoltronType();
    if (type == null) {
      return null;
    }
    if (rep.getReplicationType() == ReplicationMessage.ReplicationType.NOOP) {
      return null;
    }
    return new ServerEntityRequestImpl(rep.getEntityDescriptor(), ProcessTransactionHandler.decodeMessageType(rep.getVoltronType()),
        rep.getExtendedData(), rep.getTransactionID(), rep.getOldestTransactionOnClient(), rep.getSource(), false, Optional.empty());
  }
}
