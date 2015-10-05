/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.l2.msg.PassiveSyncMessage;
import com.tc.l2.msg.ReplicationMessage;
import com.tc.l2.state.StateManager;
import com.tc.net.groups.GroupManager;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.persistence.EntityPersistor;

/**
 *
 */
public class PassiveSyncHandler {
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
        long consumerID = entityPersistor.getNextConsumerID();
        this.entityManager.createEntity(sync.getEntityID(), sync.getVersion(), consumerID);
        this.entityPersistor.entityCreated(sync.getEntityID(), sync.getVersion(), consumerID, sync.getPayload());
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
}
