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

import org.terracotta.exception.EntityException;
import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.l2.msg.PassiveSyncMessage;
import com.tc.l2.msg.ReplicationMessage;
import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.groups.GroupManager;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.objectserver.entity.ServerEntityRequestImpl;
import com.tc.objectserver.persistence.EntityPersistor;
import java.util.Optional;


public class PassiveSyncHandler {
  private static final TCLogger logger = TCLogging.getLogger(PassiveSyncHandler.class);
  private final StateManager stateManager;
  private final EntityManager entityManager;
  private final EntityPersistor entityPersistor;

  public PassiveSyncHandler(StateManager stateManager, GroupManager groupManager, EntityManager entityManager, EntityPersistor entityPersistor) {
    this.stateManager = stateManager;
    this.entityManager = entityManager;
    this.entityPersistor = entityPersistor;
  }

  private final EventHandler<PassiveSyncMessage> eventHorizon = new AbstractEventHandler<PassiveSyncMessage>() {
    @Override
    public void handleEvent(PassiveSyncMessage message) throws EventHandlerException {
      messageReceived(message);
    }
  };
  
  public EventHandler<PassiveSyncMessage> getEventHandler() {
    return eventHorizon;
  }

  private void messageReceived(PassiveSyncMessage sync) {
    switch (sync.getType()) {
      case PassiveSyncMessage.END:
        if (!stateManager.isActiveCoordinator()) {
          stateManager.moveToPassiveStandbyState();
        }
        break;
      case PassiveSyncMessage.BEGIN:
        //  do something
        break;
      case PassiveSyncMessage.ENTITY_BEGIN:
        try {
          if (!this.entityManager.getEntity(sync.getEntityID(), sync.getVersion()).isPresent()) {
            long consumerID = entityPersistor.getNextConsumerID();
            this.entityManager.createEntity(sync.getEntityID(), sync.getVersion(), consumerID);
            this.entityPersistor.entityCreated(sync.getEntityID(), sync.getVersion(), consumerID, sync.getPayload());
            Optional<ManagedEntity> entity = entityManager.getEntity(sync.getEntityID(),sync.getVersion());
            if (entity.isPresent()) {
              entity.get().addRequest(make(sync));
            }
          }
        } catch (EntityException state) {
//  TODO: this needs to be controlled.  
          logger.warn("entity has already been created", state);
        }
        break;
      case PassiveSyncMessage.ENTITY_END:
        //  do something?
        break;
      case PassiveSyncMessage.ENTITY_CONCURRENCY_BEGIN:
        // do something?
        break;
      case PassiveSyncMessage.ENTITY_CONCURRENCY_END:
        // do something?
        break;
      case PassiveSyncMessage.ENTITY_CONCURRENCY_PAYLOAD:
        // do something
        break;
    }
  }
  
  public PassiveSyncFilter createFilter() {
    return new PassiveSyncFilter() {

      @Override
      public boolean filter(ReplicationMessage message) {
        return false;
      }
    };
  }
  
  private ServerEntityRequest make(PassiveSyncMessage rep) {
    return new ServerEntityRequestImpl(new EntityDescriptor(rep.getEntityID(), ClientInstanceID.NULL_ID, rep.getVersion()), ServerEntityAction.CREATE_ENTITY,
        rep.getPayload(), TransactionID.NULL_ID, null, ClientID.NULL_ID, false, Optional.empty());
  }  
}
