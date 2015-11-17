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
import com.tc.net.groups.AbstractGroupMessage;
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
import com.tc.util.Assert;

import java.util.Optional;


public class PassiveSyncHandler {
  private static final TCLogger logger = TCLogging.getLogger(PassiveSyncHandler.class);
  private final StateManager stateManager;
  private final EntityManager entityManager;
  private final EntityPersistor entityPersistor;

  public PassiveSyncHandler(StateManager stateManager, GroupManager<AbstractGroupMessage> groupManager, EntityManager entityManager, EntityPersistor entityPersistor) {
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
      case PassiveSyncMessage.ENTITY_BEGIN: {
        // If we sync an entity which already exists, something is seriously wrong and we shouldn't proceed.
        // XXX: THIS SHOULD BE AN ASSERTION BUT IT IS A CHECK AS A TEMPORARY STOP-GAP WHILE SYNC IS IMPLEMENTED!
        if (!lookupEntityWrapper(sync).isPresent()) {
          long consumerID = this.entityPersistor.getNextConsumerID();
          EntityID entityID = sync.getEntityID();
          long version = sync.getVersion();
          try {
            this.entityManager.createEntity(entityID, version, consumerID);
          } catch (EntityException e) {
            // We can't allow the passive to be missing part of the active data model.
            logAndAssertFailure("Failed to create entity in passive synchronization", e);
          }
          this.entityPersistor.entityCreated(entityID, version, consumerID, sync.getPayload());
          Optional<ManagedEntity> entityWrapper = lookupEntityWrapper(sync);
          // If we failed to create the entity, something is seriously wrong and we can't proceed.
          Assert.assertTrue(entityWrapper.isPresent());
          ManagedEntity entity = entityWrapper.get();
          // We need to create the entity.
          entity.addRequest(makeInternalRequest(sync, ServerEntityAction.CREATE_ENTITY));
          // ...and then tell the entity that we are about to start synchronization.
          entity.addRequest(makeInternalRequest(sync, ServerEntityAction.RECEIVE_SYNC_ENTITY_START));
        }
        break;
      }
      case PassiveSyncMessage.ENTITY_END:
        lookupEntityAndDispatch(sync, ServerEntityAction.RECEIVE_SYNC_ENTITY_END);
        break;
      case PassiveSyncMessage.ENTITY_CONCURRENCY_BEGIN:
        lookupEntityAndDispatch(sync, ServerEntityAction.RECEIVE_SYNC_ENTITY_KEY_START);
        break;
      case PassiveSyncMessage.ENTITY_CONCURRENCY_END:
        lookupEntityAndDispatch(sync, ServerEntityAction.RECEIVE_SYNC_ENTITY_KEY_END);
        break;
      case PassiveSyncMessage.ENTITY_CONCURRENCY_PAYLOAD:
        lookupEntityAndDispatch(sync, ServerEntityAction.RECEIVE_SYNC_PAYLOAD);
        break;
    }
  }

  private void lookupEntityAndDispatch(PassiveSyncMessage sync, ServerEntityAction action) {
    Optional<ManagedEntity> entityWrapper = lookupEntityWrapper(sync);
    // If we failed to find the entity, something is seriously wrong and we can't proceed.
    Assert.assertTrue(entityWrapper.isPresent());
    ManagedEntity entity = entityWrapper.get();
    // Tell the entity we are done.
    entity.addRequest(makeInternalRequest(sync, action));
  }

  private void logAndAssertFailure(String message, EntityException e) {
    logger.fatal(message, e);
    Assert.failure(message, e);
  }

  private Optional<ManagedEntity> lookupEntityWrapper(PassiveSyncMessage sync) {
    EntityID entityID = sync.getEntityID();
    long version = sync.getVersion();
    Optional<ManagedEntity> entityWrapper = null;
    try {
      entityWrapper = this.entityManager.getEntity(entityID, version);
    } catch (EntityException e) {
      logAndAssertFailure("Fatal exception during passive synchronization", e);
    }
    return entityWrapper;
  }
  
  public PassiveSyncFilter createFilter() {
    return new PassiveSyncFilter() {

      @Override
      public boolean filter(ReplicationMessage message) {
        return false;
      }
    };
  }
  
  private ServerEntityRequest makeInternalRequest(PassiveSyncMessage rep, ServerEntityAction actionType) {
    return new ServerEntityRequestImpl(new EntityDescriptor(rep.getEntityID(), ClientInstanceID.NULL_ID, rep.getVersion()), actionType,
        rep.getPayload(), TransactionID.NULL_ID, null, ClientID.NULL_ID, false, Optional.empty(), rep.getConcurrencyKey());
  }  
}
