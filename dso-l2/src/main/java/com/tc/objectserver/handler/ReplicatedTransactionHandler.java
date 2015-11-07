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
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.api.ManagedEntity;
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
  private final PassiveSyncFilter filter;

  public ReplicatedTransactionHandler(PassiveSyncFilter filter, TransactionOrderPersistor transactionOrderPersistor, 
      EntityManager manager, EntityPersistor entityPersistor, GroupManager groupManager) {
    this.entityManager = manager;
    this.entityPersistor = entityPersistor;
    this.groupManager = groupManager;
    this.orderedTransactions = transactionOrderPersistor;
    this.filter = filter;
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
        if (!filter.filter(rep)) {
          int action = rep.getAction();
          if (action == ReplicationMessage.CREATE_ENTITY) {
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
          if (ReplicationMessage.DESTROY_ENTITY == rep.getAction()) {
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
    }

    throw new RuntimeException();
  }
  
  private ServerEntityRequest make(ReplicationMessage rep) {
    Type type = rep.getVoltronType();
    if (type == null) {
      return null;
    }
    return new ServerEntityRequestImpl(rep.getEntityDescriptor(), ProcessTransactionHandler.decodeMessageType(rep.getVoltronType()),
        rep.getExtendedData(), rep.getTransactionID(), rep.getOldestTransactionOnClient(), rep.getSource(), false, Optional.empty());
  }
}
