/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.entity.ResendVoltronEntityMessage;
import com.tc.entity.VoltronEntityMessage;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.state.StateChangeListener;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.objectserver.entity.ServerEntityRequestImpl;
import com.tc.objectserver.persistence.EntityData;
import com.tc.objectserver.persistence.EntityPersistor;
import com.tc.objectserver.persistence.TransactionOrderPersistor;
import com.tc.util.Assert;
import com.tc.util.SparseList;

import java.util.List;
import java.util.Optional;
import java.util.Vector;


public class ProcessTransactionHandler implements StateChangeListener {
  private final EntityPersistor entityPersistor;
  private final TransactionOrderPersistor transactionOrderPersistor;
  
  private EntityManager entityManager;
  private DSOChannelManager dsoChannelManager;
  
  // Data required for handling transaction resends.
  private SparseList<ResendVoltronEntityMessage> resendReplayList;
  private List<ResendVoltronEntityMessage> resendNewList;

  private final AbstractEventHandler<VoltronEntityMessage> voltronHandler = new AbstractEventHandler<VoltronEntityMessage>() {
    @Override
    public void handleEvent(VoltronEntityMessage message) throws EventHandlerException {
      NodeID sourceNodeID = message.getSource();
      EntityDescriptor descriptor = message.getEntityDescriptor();
      ServerEntityAction action = decodeMessageType(message.getVoltronType());
      byte[] extendedData = message.getExtendedData();
      TransactionID transactionID = message.getTransactionID();
      boolean doesRequireReplication = message.doesRequireReplication();
      TransactionID oldestTransactionOnClient = message.getOldestTransactionOnClient();
      
      ProcessTransactionHandler.this.addMessage(sourceNodeID, descriptor, action, extendedData, transactionID, doesRequireReplication, oldestTransactionOnClient);
    }
  };
  public AbstractEventHandler<VoltronEntityMessage> getVoltronMessageHandler() {
    return this.voltronHandler;
  }

  /**
   * There is an ordering problem in how this object is constructed (there is a dependency loop) so this
   * allows us to inject the components which are built "on top of" the ProcessTransactionHandler.
   */
  public void setLateBoundComponents(DSOChannelManager dsoManager, EntityManager entityManager) {
    Assert.assertNull(this.dsoChannelManager);
    Assert.assertNull(this.entityManager);
    this.dsoChannelManager = dsoManager;
    this.entityManager = entityManager;
  }

  public ProcessTransactionHandler(EntityPersistor entityPersistor, TransactionOrderPersistor transactionOrderPersistor) {
    this.entityPersistor = entityPersistor;
    this.transactionOrderPersistor = transactionOrderPersistor;
    
    this.resendReplayList = new SparseList<>();
    this.resendNewList = new Vector<>();
  }
// TODO:  Make sure that the ReplicatedTransactionHandler is flushed before 
//   adding any new messages to the PTH
  private void addMessage(NodeID sourceNodeID, EntityDescriptor descriptor, ServerEntityAction action, byte[] extendedData, TransactionID transactionID, boolean doesRequireReplication, TransactionID oldestTransactionOnClient) {
    // Version error or duplicate creation requests will manifest as exceptions here so catch them so we can send them back
    //  over the wire as an error in the request.
    ManagedEntity entity = null;
    Exception uncaughtException = null;
    try {
      // The create/destroy cases are passed to the entityManager.
      EntityID entityID = descriptor.getEntityID();
      if (ServerEntityAction.CREATE_ENTITY == action) {
        long clientSideVersion = descriptor.getClientSideVersion();
        long consumerID = this.entityPersistor.getNextConsumerID();
        entityManager.createEntity(entityID, clientSideVersion, consumerID);
        this.entityPersistor.entityCreated(entityID, clientSideVersion, consumerID, extendedData);
      }
      Optional<ManagedEntity> optionalEntity = entityManager.getEntity(entityID, descriptor.getClientSideVersion());
      if (optionalEntity.isPresent()) {
        entity = optionalEntity.get();
      }
      if (ServerEntityAction.DESTROY_ENTITY == action) {
        entityManager.destroyEntity(entityID);
        this.entityPersistor.entityDeleted(entityID);
      }
    } catch (Exception e) {
      uncaughtException = e;
    }
    
    // In the general case, however, we need to pass this as a real ServerEntityRequest, into the entityProcessor.
    ServerEntityRequest serverEntityRequest = new ServerEntityRequestImpl(descriptor, action,  extendedData, transactionID, oldestTransactionOnClient, sourceNodeID, doesRequireReplication, safeGetChannel(sourceNodeID));
    // Before we pass this on to the entity or complete it, directly, we can send the received() ACK, since we now know the message order.
    if (null != oldestTransactionOnClient) {
      // This client still needs transaction order persistence.
      this.transactionOrderPersistor.updateWithNewMessage(sourceNodeID, transactionID, oldestTransactionOnClient);
    } else {
      // This is probably a disconnect: we can discard transaction order persistence for this client.
      this.transactionOrderPersistor.removeTrackingForClient(sourceNodeID);
    }
    serverEntityRequest.received();
    if (null == uncaughtException) {
      // If no exception has been fired, do any special handling required by the message type.
      boolean entityFound = (null != entity);
      if (ServerEntityAction.DOES_EXIST == action) {
        // The common pattern for this is to pass an empty array on success ("found") or an exception on failure ("not found").
        if (entityFound) {
          serverEntityRequest.complete();
        } else {
          serverEntityRequest.failure(new RuntimeException("Entity not found!"));
        }
      } else if (entityFound) {
        entity.addRequest(serverEntityRequest);
      } else if (ServerEntityAction.FETCH_ENTITY == action) {
        serverEntityRequest.failure(new RuntimeException("Entity not found!"));
      }
    } else {
      // If there was an exception of any sort, just pass it back as a failure.
      serverEntityRequest.failure(uncaughtException);
    }
  }

  @Override
  public void l2StateChanged(StateChangedEvent sce) {
    // Interpret the change of state to determine whether entities should be created as active or passive.
    // Additionally, we may need to promote existing entities, if this is a fail-over.
    if (sce.movedToActive()) {
      // For now, this is the only case we use since we default to passive mode and this is where we change to active.
      entityManager.enterActiveState();
    }
  }

  public void loadExistingEntities() {
    for(EntityData.Value entityValue : this.entityPersistor.loadEntityData()) {
      Assert.assertTrue(entityValue.version > 0);
      Assert.assertTrue(entityValue.consumerID > 0);
      EntityID entityID = new EntityID(entityValue.className, entityValue.entityName);
      entityManager.loadExisting(entityID, entityValue.version, entityValue.consumerID, entityValue.configuration);
    }
  }

  public void handleResentMessage(ResendVoltronEntityMessage resentMessage) {
    int index = this.transactionOrderPersistor.getIndexToReplay(resentMessage.getSource(), resentMessage.getTransactionID());
    if (index >= 0) {
      this.resendReplayList.insert(index, resentMessage);
    } else {
      this.resendNewList.add(resentMessage);
    }
  }
  
  public void executeAllResends() {
    // Clear the transaction order persistor since we are starting fresh.
    this.transactionOrderPersistor.clearAllRecords();
    
    // Replay all the already-ordered messages.
    for (ResendVoltronEntityMessage message : this.resendReplayList) {
      executeResend(message);
    }
    this.resendReplayList = null;
    
    // Replay all the new messages found during resends.
    for (ResendVoltronEntityMessage message : this.resendNewList) {
      executeResend(message);
    }
    this.resendNewList = null;
  }


  private Optional<MessageChannel> safeGetChannel(NodeID id) {
    try {
      return Optional.of(dsoChannelManager.getActiveChannel(id));
    } catch (NoSuchChannelException e) {
      return Optional.empty();
    }
  }

  private void executeResend(ResendVoltronEntityMessage message) {
    NodeID sourceNodeID = message.getSource();
    EntityDescriptor descriptor = message.getEntityDescriptor();
    ServerEntityAction action = decodeMessageType(message.getVoltronType());
    byte[] extendedData = message.getExtendedData();
    TransactionID transactionID = message.getTransactionID();
    boolean doesRequireReplication = message.doesRequireReplication();
    TransactionID oldestTransactionOnClient = message.getOldestTransactionOnClient();
    
    ProcessTransactionHandler.this.addMessage(sourceNodeID, descriptor, action, extendedData, transactionID, doesRequireReplication, oldestTransactionOnClient);
  }

  public static ServerEntityAction decodeMessageType(VoltronEntityMessage.Type type) {
    // Decode the appropriate server-internal action from this request type.
    ServerEntityAction action = null;
    switch (type) {
      case FETCH_ENTITY:
        action = ServerEntityAction.FETCH_ENTITY;
        break;
      case RELEASE_ENTITY:
        action = ServerEntityAction.RELEASE_ENTITY;
        break;
      case DOES_EXIST:
        action = ServerEntityAction.DOES_EXIST;
        break;
      case CREATE_ENTITY:
        action = ServerEntityAction.CREATE_ENTITY;
        break;
      case DESTROY_ENTITY:
        action = ServerEntityAction.DESTROY_ENTITY;
        break;
      case INVOKE_ACTION:
        action = ServerEntityAction.INVOKE_ACTION;
        break;
      default:
        // Unknown request type.
        Assert.fail();
        break;
    }
    return action;
  }
}
