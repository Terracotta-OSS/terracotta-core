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
import com.tc.async.api.EventHandlerException;
import com.tc.entity.ResendVoltronEntityMessage;
import com.tc.entity.VoltronEntityMessage;
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
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.entity.ServerEntityRequestImpl;
import com.tc.objectserver.persistence.EntityData;
import com.tc.objectserver.persistence.EntityPersistor;
import com.tc.objectserver.persistence.TransactionOrderPersistor;
import com.tc.util.Assert;
import com.tc.util.SparseList;
import java.util.ArrayList;
import java.util.Iterator;

import java.util.List;
import java.util.Optional;
import java.util.Vector;

import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityUserException;


public class ProcessTransactionHandler {
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

    @Override
    protected void initialize(ConfigurationContext context) {
      super.initialize(context); 
      ServerConfigurationContext server = (ServerConfigurationContext)context;
      
      server.getL2Coordinator().getReplicatedClusterStateManager().setCurrentState(server.getL2Coordinator().getStateManager().getCurrentState());
      server.getL2Coordinator().getReplicatedClusterStateManager().goActiveAndSyncState();
//  go right to active state.  this only gets initialized once ACTIVE-COORDINATOR is entered
      entityManager.enterActiveState();
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
  
  public Iterable<ManagedEntity> getEntityList() {
    return new Iterable<ManagedEntity>() {
      @Override
      public Iterator<ManagedEntity> iterator() {
        synchronized (ProcessTransactionHandler.this) {
          return new ArrayList<ManagedEntity>(entityManager.getAll()).iterator();
        }
      }
    };
  }
// TODO:  Make sure that the ReplicatedTransactionHandler is flushed before 
//   adding any new messages to the PTH
  private synchronized void addMessage(NodeID sourceNodeID, EntityDescriptor descriptor, ServerEntityAction action, byte[] extendedData, TransactionID transactionID, boolean doesRequireReplication, TransactionID oldestTransactionOnClient) {
    // Version error or duplicate creation requests will manifest as exceptions here so catch them so we can send them back
    //  over the wire as an error in the request.
    EntityID entityID = descriptor.getEntityID();
    ManagedEntity entity = null;
    EntityException uncaughtException = null;
    try {
      // The create/destroy cases are passed to the entityManager.
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
    } catch (EntityException e) {
      uncaughtException = e;
    } catch (Exception e) {
      // Wrap the exception.
      uncaughtException = new EntityUserException(entityID.getClassName(), entityID.getEntityName(), e);
    }
    
    // In the general case, however, we need to pass this as a real ServerEntityRequest, into the entityProcessor.
    ServerEntityRequest serverEntityRequest = new ServerEntityRequestImpl(descriptor, action, transactionID, oldestTransactionOnClient, sourceNodeID, doesRequireReplication, safeGetChannel(sourceNodeID));
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
      // The common pattern for this is to pass an empty array on success ("found") or an exception on failure ("not found").
      if (entityFound) {
        // We special-case the DOES_EXIST check to complete without interacting with the entity.
        if (ServerEntityAction.DOES_EXIST == action) {
          serverEntityRequest.complete();
        } else if (ServerEntityAction.INVOKE_ACTION == action) {
          entity.addInvokeRequest(serverEntityRequest, extendedData);
        } else {
          entity.addLifecycleRequest(serverEntityRequest, extendedData);
        }
      } else {
        serverEntityRequest.failure(new EntityNotFoundException(entityID.getClassName(), entityID.getEntityName()));
      }
    } else {
      // If there was an exception of any sort, just pass it back as a failure.
      serverEntityRequest.failure(uncaughtException);
    }
  }

  public void loadExistingEntities() {
    for(EntityData.Value entityValue : this.entityPersistor.loadEntityData()) {
      Assert.assertTrue(entityValue.version > 0);
      Assert.assertTrue(entityValue.consumerID > 0);
      EntityID entityID = new EntityID(entityValue.className, entityValue.entityName);
      try {
        entityManager.loadExisting(entityID, entityValue.version, entityValue.consumerID, entityValue.configuration);
      } catch (EntityException e) {
        // We aren't expecting to fail loading anything from the existing set.
        throw new IllegalArgumentException(e);
      }
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

  private static ServerEntityAction decodeMessageType(VoltronEntityMessage.Type type) {
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
