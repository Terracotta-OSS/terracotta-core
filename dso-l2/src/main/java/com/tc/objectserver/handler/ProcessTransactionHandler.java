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
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.entity.ResendVoltronEntityMessage;
import com.tc.entity.VoltronEntityAppliedResponse;
import com.tc.entity.VoltronEntityMessage;
import com.tc.entity.VoltronEntityMultiResponse;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.entity.MessagePayload;
import com.tc.objectserver.api.Retiree;
import com.tc.objectserver.entity.ServerEntityRequestResponse;
import com.tc.objectserver.persistence.EntityData;
import com.tc.objectserver.persistence.EntityPersistor;
import com.tc.objectserver.persistence.TransactionOrderPersistor;
import com.tc.util.Assert;
import com.tc.util.SparseList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityUserException;


public class ProcessTransactionHandler {
  private static final TCLogger LOGGER = TCLogging.getLogger(ProcessTransactionHandler.class);
  
  private final EntityPersistor entityPersistor;
  private final TransactionOrderPersistor transactionOrderPersistor;
  private final Runnable stateManagerCleanup;
  
  private final EntityManager entityManager;
  private final DSOChannelManager dsoChannelManager;
  
  // Data required for handling transaction resends.
  private SparseList<ResendVoltronEntityMessage> resendReplayList;
  private List<ResendVoltronEntityMessage> resendNewList;
  
  private Sink<TCMessage> multiSend;
  private ConcurrentHashMap<ClientID, TCMessage> invokeReturn = new ConcurrentHashMap<>();
  
  private void sendMultiResponse(VoltronEntityMultiResponse response) {
    multiSend.addSingleThreaded(response);
  }
  
  private final AbstractEventHandler<TCMessage> multiSender = new AbstractEventHandler<TCMessage>() {
    @Override
    public void handleEvent(TCMessage context) throws EventHandlerException {
      invokeReturn.remove((ClientID)context.getDestinationNodeID(), context);
      Assert.assertTrue(context.send());
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("sent " + context);
      }      
    }
  };
  public AbstractEventHandler<TCMessage> getMultiResponseSender() {
    return multiSender;
  }
  
  private final AbstractEventHandler<VoltronEntityMessage> voltronHandler = new AbstractEventHandler<VoltronEntityMessage>() {
    @Override
    public void handleEvent(VoltronEntityMessage message) throws EventHandlerException {
//  resends are only processed the first time an event is handled.  
//  resends are processed in this manner so invokes are scheduled by the expected stage thread
//  see ManagedEntityImpl.scheduleInOrder()
//  the call always happens and immediately returns if the resends have already been processed
      processAllResends();
      ClientID sourceNodeID = message.getSource();
      EntityDescriptor descriptor = message.getEntityDescriptor();
      ServerEntityAction action = decodeMessageType(message.getVoltronType());
      EntityMessage entityMessage = message.getEntityMessage();
      byte[] extendedData = message.getExtendedData();

      TransactionID transactionID = message.getTransactionID();
      boolean doesRequireReplication = message.doesRequireReplication();
      TransactionID oldestTransactionOnClient = message.getOldestTransactionOnClient();
      
      ProcessTransactionHandler.this.addMessage(sourceNodeID, descriptor, action, new MessagePayload(extendedData, entityMessage, doesRequireReplication), transactionID, oldestTransactionOnClient);
    }

    @Override
    protected void initialize(ConfigurationContext context) {
      super.initialize(context); 
      ServerConfigurationContext server = (ServerConfigurationContext)context;
      
      server.getL2Coordinator().getReplicatedClusterStateManager().setCurrentState(server.getL2Coordinator().getStateManager().getCurrentState());
      server.getL2Coordinator().getReplicatedClusterStateManager().goActiveAndSyncState();
      
      Stage<TCMessage> mss = server.getStage(ServerConfigurationContext.RESPOND_TO_REQUEST_STAGE, TCMessage.class);
      multiSend = mss.getSink();
      
//  go right to active state.  this only gets initialized once ACTIVE-COORDINATOR is entered
      entityManager.enterActiveState();
    }
  };
  public AbstractEventHandler<VoltronEntityMessage> getVoltronMessageHandler() {
    return this.voltronHandler;
  }

  public ProcessTransactionHandler(EntityPersistor entityPersistor, TransactionOrderPersistor transactionOrderPersistor, DSOChannelManager channelManager, EntityManager entityManager, Runnable stateManagerCleanup) {
    this.entityPersistor = entityPersistor;
    this.transactionOrderPersistor = transactionOrderPersistor;
    this.dsoChannelManager = channelManager;
    this.entityManager = entityManager;
    this.stateManagerCleanup = stateManagerCleanup;
    
    this.resendReplayList = new SparseList<>();
    this.resendNewList = new LinkedList<>();
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
  
  private void addSequentially(ClientID target, Predicate<VoltronEntityMultiResponse> adder) {
    boolean handled = false;
    while (!handled) {
      TCMessage old = invokeReturn.get(target);
      if (old instanceof VoltronEntityMultiResponse) {
        handled = adder.test((VoltronEntityMultiResponse)old);
      }
      if (!handled) {
        Optional<MessageChannel> channel = safeGetChannel(target);
        if (channel.isPresent()) {
          VoltronEntityMultiResponse vmr = (VoltronEntityMultiResponse)channel.get().createMessage(TCMessageType.VOLTRON_ENTITY_MULTI_RESPONSE);
          old = invokeReturn.putIfAbsent(target, vmr);
          if (old instanceof VoltronEntityMultiResponse) {
            handled = adder.test((VoltronEntityMultiResponse)old);
          } else {
            handled = adder.test(vmr);
            Assert.assertTrue(handled);
            sendMultiResponse(vmr);
          }
        } else {
          handled = true;
//  no more client.  ignore
        }
      }
    }
  }
  
  private static void retireMessagesForEntity(ManagedEntity entity, EntityMessage message) {
    List<Retiree> readyToRetire = entity.getRetirementManager().retireForCompletion(message);
    for (Retiree toRetire : readyToRetire) {
      if (null != toRetire) {
        toRetire.retired();
      }
    }
  }
// TODO:  Make sure that the ReplicatedTransactionHandler is flushed before 
//   adding any new messages to the PTH
  private synchronized void addMessage(ClientID sourceNodeID, EntityDescriptor descriptor, ServerEntityAction action, MessagePayload entityMessage, TransactionID transactionID, TransactionID oldestTransactionOnClient) {
    // Version error or duplicate creation requests will manifest as exceptions here so catch them so we can send them back
    //  over the wire as an error in the request.
    EntityID entityID = descriptor.getEntityID();
    
    // This is active-side processing so this is never a replicated message.
    boolean isReplicatedMessage = false;
    // In the general case, however, we need to pass this as a real ServerEntityRequest, into the entityProcessor.
    ServerEntityRequestResponse serverEntityRequest = new ServerEntityRequestResponse(descriptor, action, transactionID, oldestTransactionOnClient, sourceNodeID, ()->safeGetChannel(sourceNodeID), isReplicatedMessage);
    // Before we pass this on to the entity or complete it, directly, we can send the received() ACK, since we now know the message order.
    // Note that we only want to persist the messages with a true sourceNodeID.  Synthetic invocations and sync messages
    // don't have one (although sync messages shouldn't come down this path).
    if (!ClientInstanceID.NULL_ID.equals(sourceNodeID)) {
      if (null != oldestTransactionOnClient) {
        // This client still needs transaction order persistence.
        this.transactionOrderPersistor.updateWithNewMessage(sourceNodeID, transactionID, oldestTransactionOnClient);
      } else {
        // This is probably a disconnect: we can discard transaction order persistence for this client.
        this.transactionOrderPersistor.removeTrackingForClient(sourceNodeID);
        // And the entity journal persistence.
        this.entityPersistor.removeTrackingForClient(sourceNodeID);
      }
    }
    if (ServerEntityAction.INVOKE_ACTION != action) {
      serverEntityRequest.received();
    }
    if (ServerEntityAction.CREATE_ENTITY == action) {
      // The common pattern for this is to pass an empty array on success ("found") or an exception on failure ("not found").
      long consumerID = this.entityPersistor.getNextConsumerID();
      serverEntityRequest.setAutoRetire();
      try {
        ManagedEntity temp = entityManager.createEntity(entityID, descriptor.getClientSideVersion(), consumerID, !sourceNodeID.isNull());
        temp.addRequestMessage(serverEntityRequest, entityMessage,
          (result) -> {
            if (!sourceNodeID.isNull()) {
              entityPersistor.entityCreated(sourceNodeID, transactionID.toLong(), oldestTransactionOnClient.toLong(), entityID, descriptor.getClientSideVersion(), consumerID, true, entityMessage.getRawPayload());
              serverEntityRequest.complete();
            } else {
              entityPersistor.entityCreatedNoJournal(entityID, descriptor.getClientSideVersion(), consumerID, true, entityMessage.getRawPayload());
              serverEntityRequest.complete();
            }
          }, (exception) -> {
            entityPersistor.entityCreateFailed(sourceNodeID, transactionID.toLong(), oldestTransactionOnClient.toLong(), exception);
            serverEntityRequest.failure(exception);
          });
      } catch (EntityException ee) {
        if (!sourceNodeID.isNull()) {
          entityPersistor.entityCreateFailed(sourceNodeID, transactionID.toLong(), oldestTransactionOnClient.toLong(), ee);
        }
        serverEntityRequest.failure(ee);
      }
    } else {
      ManagedEntity entity = null;
      try {
        // At this point, we can now look up the actual managed entity.
        Optional<ManagedEntity> optionalEntity = entityManager.getEntity(entityID, descriptor.getClientSideVersion());
        if (optionalEntity.isPresent()) {
          entity = optionalEntity.get();
        } else {
          LOGGER.debug("entity not found " + serverEntityRequest.getAction() + " " + entityID.getClassName() + ", " + entityID.getEntityName());
          throw new EntityNotFoundException(entityID.getClassName(), entityID.getEntityName());
        }
        // Note that it is possible to trigger an exception when decoding a message in addInvokeRequest.
        if (ServerEntityAction.INVOKE_ACTION == action) {
          ManagedEntity locked = entity;
          try {
            addSequentially(sourceNodeID, addto->addto.addReceived(transactionID));

            EntityMessage message = entityMessage.decodeMessage(entity.getCodec());
            
            locked.addRequestMessage(serverEntityRequest, entityMessage, (result)-> {
              addSequentially(sourceNodeID, addTo->addTo.addResult(transactionID, result));
              RetirementManager retirementManager = locked.getRetirementManager();
              
              retirementManager.updateWithRetiree(message, new Retiree() {
                @Override
                public void retired() {
                  addSequentially(sourceNodeID, addTo->addTo.addRetired(serverEntityRequest.getTransaction()));
                }
                @Override
                public TransactionID getTransaction() {
                  return serverEntityRequest.getTransaction();
                }
              });
              
              retireMessagesForEntity(locked, message);
            }, (fail)-> {
              safeGetChannel(sourceNodeID).ifPresent(channel -> {
                VoltronEntityAppliedResponse failMessage = (VoltronEntityAppliedResponse)channel.createMessage(TCMessageType.VOLTRON_ENTITY_APPLIED_RESPONSE);
                failMessage.setFailure(transactionID, fail, false);
                invokeReturn.put(sourceNodeID, failMessage);
                multiSend.addSingleThreaded(failMessage);
              });
              
              locked.getRetirementManager().updateWithRetiree(message, new Retiree() {
                @Override
                public void retired() {
                  addSequentially(sourceNodeID, addTo->addTo.addRetired(serverEntityRequest.getTransaction()));
                }

                @Override
                public TransactionID getTransaction() {
                  return serverEntityRequest.getTransaction();
                }
              });
              
              retireMessagesForEntity(locked, message);
            });
          } catch (MessageCodecException codec) {
            serverEntityRequest.failure(new EntityUserException(locked.getID().getClassName(), locked.getID().getEntityName(), codec));
            serverEntityRequest.retired();
          }
        } else if (ServerEntityAction.RECONFIGURE_ENTITY == action) {
          serverEntityRequest.setAutoRetire();
          entity.addRequestMessage(serverEntityRequest, entityMessage,
            (result)-> {
              EntityExistenceHelpers.recordReconfigureEntity(entityPersistor, entityManager, serverEntityRequest.getNodeID(), serverEntityRequest.getTransaction(), serverEntityRequest.getOldestTransactionOnClient(), descriptor.getEntityID(), descriptor.getClientSideVersion(), entityMessage.getRawPayload(), null);
              serverEntityRequest.complete(result);
            }, (exception) -> {  
              EntityExistenceHelpers.recordReconfigureEntity(entityPersistor, entityManager, serverEntityRequest.getNodeID(), serverEntityRequest.getTransaction(), serverEntityRequest.getOldestTransactionOnClient(), descriptor.getEntityID(), descriptor.getClientSideVersion(), entityMessage.getRawPayload(), exception);
              serverEntityRequest.failure(exception);
            });
        }  else if (ServerEntityAction.DESTROY_ENTITY == action) {
          serverEntityRequest.setAutoRetire();
          entity.addRequestMessage(serverEntityRequest, entityMessage,
            (result) -> {
              EntityExistenceHelpers.recordDestroyEntity(entityPersistor, entityManager, sourceNodeID, transactionID, oldestTransactionOnClient, entityID, null);
              serverEntityRequest.complete();
            }, (exception) -> {
              EntityExistenceHelpers.recordDestroyEntity(entityPersistor, entityManager, sourceNodeID, transactionID, oldestTransactionOnClient, entityID, exception);
              serverEntityRequest.failure(exception);
            });
        } else {
          if (ServerEntityAction.NOOP == action && entity.isRemoveable()) {
            LOGGER.debug("removing " + entity.getID());
            entityManager.removeDestroyed(entity.getID());
          }
          serverEntityRequest.setAutoRetire();
          entity.addRequestMessage(serverEntityRequest, entityMessage, serverEntityRequest::complete, serverEntityRequest::failure);
        }  
      } catch (EntityException ee) {
        serverEntityRequest.failure(ee);
        serverEntityRequest.retired();
      }
    }
  }
  
  public void loadExistingEntities() {
    for(EntityData.Value entityValue : this.entityPersistor.loadEntityData()) {
      Assert.assertTrue(entityValue.version > 0);
      Assert.assertTrue(entityValue.consumerID > 0);
      EntityID entityID = new EntityID(entityValue.className, entityValue.entityName);
      try {
        entityManager.loadExisting(entityID, entityValue.version, entityValue.consumerID, entityValue.canDelete, entityValue.configuration);
      } catch (EntityException e) {
        // We aren't expecting to fail loading anything from the existing set.
        throw new IllegalArgumentException(e);
      }
    }
  }

  public void handleResentMessage(ResendVoltronEntityMessage resentMessage) {
    boolean cached = false;
    byte[] result = null;
    int index = -1;
    try {
      switch (resentMessage.getVoltronType()) {
        case CREATE_ENTITY:
          cached = entityPersistor.wasEntityCreatedInJournal(resentMessage.getSource(), resentMessage.getTransactionID().toLong());
          break;
        case DESTROY_ENTITY:
          cached = entityPersistor.wasEntityDestroyedInJournal(resentMessage.getSource(), resentMessage.getTransactionID().toLong());
          break;
        case RECONFIGURE_ENTITY:
          result = entityPersistor.reconfiguredResultInJournal(resentMessage.getSource(), resentMessage.getTransactionID().toLong());
          if (result != null) {
            cached = true;
          }
          break;
        case FETCH_ENTITY:
        case RELEASE_ENTITY:
//  are not replicated
          break;
        default:
          index = this.transactionOrderPersistor.getIndexToReplay(resentMessage.getSource(), resentMessage.getTransactionID());
          break;
      }
      if (cached) {
        ServerEntityRequestResponse response = new ServerEntityRequestResponse(EntityDescriptor.NULL_ID, ServerEntityAction.CREATE_ENTITY, resentMessage.getTransactionID(), resentMessage.getOldestTransactionOnClient(), resentMessage.getSource(), ()->safeGetChannel(resentMessage.getSource()), false);
        response.received();
        if (result != null) {
          response.complete(result);
        } else {
          response.complete();
        }
        response.retired();
      } else if (index >= 0) {
        this.resendReplayList.insert(index, resentMessage);     
      } else {
        this.resendNewList.add(resentMessage);
      }
    } catch (EntityException ee) {
      ServerEntityRequestResponse response = new ServerEntityRequestResponse(EntityDescriptor.NULL_ID, ServerEntityAction.CREATE_ENTITY, resentMessage.getTransactionID(), resentMessage.getOldestTransactionOnClient(), resentMessage.getSource(), ()->safeGetChannel(resentMessage.getSource()), false);
      response.received();
      response.failure(ee);
      response.retired();
    }
  }
  
  private void processAllResends() {
 //   TODO:  investigate the need to fold FETCH and RELEASE resends on top of each other
    if (this.resendReplayList == null && this.resendNewList == null) {
      return;
    }

    this.stateManagerCleanup.run();

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
//  remove tracking for any resent create journal entries
    entityPersistor.removeTrackingForClient(ClientID.NULL_ID);
    
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
    ClientID sourceNodeID = message.getSource();
    EntityDescriptor descriptor = message.getEntityDescriptor();
    ServerEntityAction action = decodeMessageType(message.getVoltronType());
    // Note that we currently don't expect messages which already have an EntityMessage instance to appear here.
    EntityMessage entityMessage = message.getEntityMessage();
    Assert.assertNull(entityMessage);
    byte[] extendedData = message.getExtendedData();

    TransactionID transactionID = message.getTransactionID();
    boolean doesRequireReplication = message.doesRequireReplication();
    TransactionID oldestTransactionOnClient = message.getOldestTransactionOnClient();
    
    ProcessTransactionHandler.this.addMessage(sourceNodeID, descriptor, action, new MessagePayload(extendedData, entityMessage, doesRequireReplication), transactionID, oldestTransactionOnClient);
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
      case CREATE_ENTITY:
        action = ServerEntityAction.CREATE_ENTITY;
        break;
      case RECONFIGURE_ENTITY:
        action = ServerEntityAction.RECONFIGURE_ENTITY;
        break;
      case DESTROY_ENTITY:
        action = ServerEntityAction.DESTROY_ENTITY;
        break;
      case INVOKE_ACTION:
        action = ServerEntityAction.INVOKE_ACTION;
        break;
      case NOOP:
        action = ServerEntityAction.NOOP;
        break;
      default:
        // Unknown request type.
        Assert.fail();
        break;
    }
    return action;
  }
}
