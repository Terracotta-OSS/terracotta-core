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
import com.tc.exception.VoltronEntityUserExceptionWrapper;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.ReconnectionRejectedException;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageType;
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
import com.tc.objectserver.entity.ReconnectListener;
import com.tc.objectserver.entity.ReferenceMessage;
import com.tc.objectserver.entity.ServerEntityRequestResponse;
import com.tc.objectserver.persistence.EntityData;
import com.tc.objectserver.persistence.Persistor;
import com.tc.util.Assert;
import com.tc.util.SparseList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.entity.ReconnectRejectedException;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityNotFoundException;


public class ProcessTransactionHandler implements ReconnectListener {
  private static final TCLogger LOGGER = TCLogging.getLogger(ProcessTransactionHandler.class);
  
  private final Persistor persistor;
  private final Runnable stateManagerCleanup;
  
  private final EntityManager entityManager;
  private final DSOChannelManager dsoChannelManager;
  
  // Data required for handling transaction resends.
  private List<ReferenceMessage> references;
  private SparseList<ResendVoltronEntityMessage> resendReplayList;
  private List<ResendVoltronEntityMessage> resendNewList;
  private boolean reconnecting = true;
  
  private Sink<TCMessage> multiSend;
  private ConcurrentHashMap<ClientID, TCMessage> invokeReturn = new ConcurrentHashMap<>();
  private ConcurrentHashMap<TransactionID, Future<Void>> transactionOrderPersistenceFutures = new ConcurrentHashMap<>();
  
  private void sendMultiResponse(VoltronEntityMultiResponse response) {
    multiSend.addSingleThreaded(response);
  }
  
  @Override
  public synchronized void reconnectComplete() {
    reconnecting = false;
    notify();
  }
  
  private final AbstractEventHandler<TCMessage> multiSender = new AbstractEventHandler<TCMessage>() {
    @Override
    public void handleEvent(TCMessage context) throws EventHandlerException {
      NodeID destinationID = context.getDestinationNodeID();
      invokeReturn.remove((ClientID)destinationID, context);
      if(context instanceof VoltronEntityMultiResponse) {
        VoltronEntityMultiResponse voltronEntityMultiResponse = (com.tc.entity.VoltronEntityMultiResponse) context;
        voltronEntityMultiResponse.stopAdding();
        for (TransactionID transactionID : voltronEntityMultiResponse.getReceivedTransactions()) {
          waitForTransactionOrderPersistenceFuture(transactionID);
        }
      } else if(context instanceof VoltronEntityAppliedResponse) {
        waitForTransactionOrderPersistenceFuture(((VoltronEntityAppliedResponse)context).getTransactionID());
      } else {
        Assert.fail("Unexpected message type: " + context.getClass());
      }
      boolean didSend = context.send();
      if (!didSend) {
        // It is possible for this send to fail.  Typically, it means that the client has disconnected.
        LOGGER.warn("Failed to send message to: " + destinationID);
      } else if (LOGGER.isDebugEnabled()) {
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
      processAllResends(message);
      ClientID sourceNodeID = message.getSource();
      EntityDescriptor descriptor = message.getEntityDescriptor();
      ServerEntityAction action = decodeMessageType(message.getVoltronType());
      EntityMessage entityMessage = message.getEntityMessage();
      byte[] extendedData = message.getExtendedData();

      TransactionID transactionID = message.getTransactionID();
      boolean doesRequireReplication = message.doesRequireReplication();
      TransactionID oldestTransactionOnClient = message.getOldestTransactionOnClient();
      boolean requestedReceived = message.doesRequestReceived();
      
      ProcessTransactionHandler.this.addMessage(sourceNodeID, descriptor, action, MessagePayload.commonMessagePayloadBusy(extendedData, entityMessage, doesRequireReplication), transactionID, oldestTransactionOnClient, requestedReceived);
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
      
      server.getClientHandshakeManager().addReconnectListener(ProcessTransactionHandler.this);
    }
  };
  public AbstractEventHandler<VoltronEntityMessage> getVoltronMessageHandler() {
    return this.voltronHandler;
  }

  public ProcessTransactionHandler(Persistor persistor, DSOChannelManager channelManager, EntityManager entityManager, Runnable stateManagerCleanup) {
    this.persistor = persistor;
    this.dsoChannelManager = channelManager;
    this.entityManager = entityManager;
    this.stateManagerCleanup = stateManagerCleanup;
    
    this.references = new LinkedList<>();
    this.resendReplayList = new SparseList<>();
    this.resendNewList = new LinkedList<>();
  }
  /**
   * This is a confusing method used in a confusing way.  This is used to snapshot the current
   * set of ManagedEntities.  There is synchronization in the EntityManager so a clean snapshot 
   * can be taken.  The runnable is functionality that is passed in that must run under lock.
   * entities while the snapshot is being captured.  Once the live set of entities is established, 
   * startSync is called on each one so that internal state of the entity is locked down until 
   * the sync has happened on that particular entity
   */
  public Iterable<ManagedEntity> snapshotEntityList(Consumer<List<ManagedEntity>> runFirst) {
    return entityManager.snapshot(runFirst);
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

// only the process transaction thread will add messages here except for on reconnect
  private void addMessage(ClientID sourceNodeID, EntityDescriptor descriptor, ServerEntityAction action, MessagePayload entityMessage, TransactionID transactionID, TransactionID oldestTransactionOnClient, boolean requiresReceived) {
    // Version error or duplicate creation requests will manifest as exceptions here so catch them so we can send them back
    //  over the wire as an error in the request.
    
    // This is active-side processing so this is never a replicated message.
    boolean isReplicatedMessage = false;
    // In the general case, however, we need to pass this as a real ServerEntityRequest, into the entityProcessor.
    ServerEntityRequestResponse serverEntityRequest = new ServerEntityRequestResponse(descriptor, action, transactionID, oldestTransactionOnClient, sourceNodeID, ()->safeGetChannel(sourceNodeID), requiresReceived, isReplicatedMessage);
    // Before we pass this on to the entity or complete it, directly, we can send the received() ACK, since we now know the message order.
    // Note that we only want to persist the messages with a true sourceNodeID.  Synthetic invocations and sync messages
    // don't have one (although sync messages shouldn't come down this path).
    Future<Void> transactionOrderPersistenceFuture = null;
    // if the client is valid and the transaction id is valid, then this came from a real client
    // and the client expects to be able to reconnect
    if (sourceNodeID != null && !sourceNodeID.isNull() && transactionID.isValid()) {
      Assert.assertTrue(oldestTransactionOnClient.isValid());
        // This client still needs transaction order persistence.
      transactionOrderPersistenceFuture = this.persistor.getTransactionOrderPersistor().updateWithNewMessage(sourceNodeID, transactionID, oldestTransactionOnClient);
      serverEntityRequest.setTransactionOrderPersistenceFuture(transactionOrderPersistenceFuture);
    }
    if (ServerEntityAction.CREATE_ENTITY == action) {
      // The common pattern for this is to pass an empty array on success ("found") or an exception on failure ("not found").
      long consumerID = this.persistor.getEntityPersistor().getNextConsumerID();
      serverEntityRequest.setAutoRetire();
      try {
        boolean canDelete = !sourceNodeID.isNull();
        EntityID entityID = descriptor.getEntityID();
        ManagedEntity temp = entityManager.createEntity(entityID, descriptor.getClientSideVersion(), consumerID, canDelete);
        temp.addRequestMessage(serverEntityRequest, entityMessage, serverEntityRequest::received, 
          (result) -> {
            if (!sourceNodeID.isNull()) {
              this.persistor.getEntityPersistor().entityCreated(sourceNodeID, transactionID.toLong(), oldestTransactionOnClient.toLong(), entityID, descriptor.getClientSideVersion(), consumerID, canDelete, entityMessage.getRawPayload());
              serverEntityRequest.complete();
            } else {
              this.persistor.getEntityPersistor().entityCreatedNoJournal(entityID, descriptor.getClientSideVersion(), consumerID, canDelete, entityMessage.getRawPayload());
              serverEntityRequest.complete();
            }
          }, (exception) -> {
            this.persistor.getEntityPersistor().entityCreateFailed(sourceNodeID, transactionID.toLong(), oldestTransactionOnClient.toLong(), exception);
            serverEntityRequest.failure(exception);
          });
      } catch (EntityException ee) {
        if (!sourceNodeID.isNull()) {
          this.persistor.getEntityPersistor().entityCreateFailed(sourceNodeID, transactionID.toLong(), oldestTransactionOnClient.toLong(), ee);
        }
        serverEntityRequest.failure(ee);
      }
    } else {
      ManagedEntity entity = null;
      try {
        // At this point, we can now look up the actual managed entity.
        Optional<ManagedEntity> optionalEntity = entityManager.getEntity(descriptor);
        if (optionalEntity.isPresent()) {
          entity = optionalEntity.get();
        } else {
          if (!descriptor.isIndexed()) {
            throw new EntityNotFoundException(descriptor.getEntityID().getClassName(), descriptor.getEntityID().getEntityName());
          } else {
            throw new AssertionError("fetched entity not found");
          }
        }
        // Note that it is possible to trigger an exception when decoding a message in addInvokeRequest.
        if (ServerEntityAction.INVOKE_ACTION == action) {
          ManagedEntity locked = entity;
          try {
            if(transactionOrderPersistenceFuture != null) {
              transactionOrderPersistenceFutures.put(transactionID, transactionOrderPersistenceFuture);
            }
            EntityMessage message = entityMessage.decodeMessage(raw->locked.getCodec().decodeMessage(raw));
            
            locked.addRequestMessage(serverEntityRequest, entityMessage, ()->addSequentially(sourceNodeID, addto->addto.addReceived(transactionID)), (result)-> {
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
                VoltronEntityAppliedResponse failMessage = (VoltronEntityAppliedResponse)channel.createMessage(TCMessageType.VOLTRON_ENTITY_COMPLETED_RESPONSE);
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
            serverEntityRequest.failure(new VoltronEntityUserExceptionWrapper(new EntityUserException("Caught MessageCodecException while decoding message", codec)));
            serverEntityRequest.retired();
          }
        } else if (ServerEntityAction.RECONFIGURE_ENTITY == action) {
          serverEntityRequest.setAutoRetire();
          entity.addRequestMessage(serverEntityRequest, entityMessage, serverEntityRequest::received, 
            (result)-> {
              EntityExistenceHelpers.recordReconfigureEntity(this.persistor.getEntityPersistor(), entityManager, serverEntityRequest.getNodeID(), serverEntityRequest.getTransaction(), serverEntityRequest.getOldestTransactionOnClient(), descriptor.getEntityID(), descriptor.getClientSideVersion(), entityMessage.getRawPayload(), null);
              serverEntityRequest.complete(result);
            }, (exception) -> {  
              EntityExistenceHelpers.recordReconfigureEntity(this.persistor.getEntityPersistor(), entityManager, serverEntityRequest.getNodeID(), serverEntityRequest.getTransaction(), serverEntityRequest.getOldestTransactionOnClient(), descriptor.getEntityID(), descriptor.getClientSideVersion(), entityMessage.getRawPayload(), exception);
              serverEntityRequest.failure(exception);
            });
        }  else if (ServerEntityAction.DESTROY_ENTITY == action) {
          serverEntityRequest.setAutoRetire();
          entity.addRequestMessage(serverEntityRequest, entityMessage, serverEntityRequest::received, 
            (result) -> {
              EntityExistenceHelpers.recordDestroyEntity(this.persistor.getEntityPersistor(), entityManager, sourceNodeID, transactionID, oldestTransactionOnClient, descriptor.getEntityID(), null);
              serverEntityRequest.complete();
            }, (exception) -> {
              EntityExistenceHelpers.recordDestroyEntity(this.persistor.getEntityPersistor(), entityManager, sourceNodeID, transactionID, oldestTransactionOnClient, descriptor.getEntityID(), exception);
              serverEntityRequest.failure(exception);
            });
        } else if (ServerEntityAction.FETCH_ENTITY == action || ServerEntityAction.RELEASE_ENTITY == action) {
          serverEntityRequest.setAutoRetire();
          entity.addRequestMessage(serverEntityRequest, entityMessage, serverEntityRequest::received,
            (result) -> {
              serverEntityRequest.complete(result);
            }, (exception) -> {
              if (exception.getCause() instanceof ReconnectRejectedException) {
                disconnectClientDueToFailure(sourceNodeID);
              } else {
                serverEntityRequest.failure(exception);
              }
            });
        } else {
          if (ServerEntityAction.MANAGED_ENTITY_GC == action && entity.isRemoveable()) {
              LOGGER.debug("removing " + entity.getID());
              entityManager.removeDestroyed(descriptor.getFetchID());
            // no scheduling needed
          } else {
            // if this is the MANAGED_ENTTIY_GC and not removable then still need to flush
            serverEntityRequest.setAutoRetire();
            entity.addRequestMessage(serverEntityRequest, entityMessage, serverEntityRequest::received, serverEntityRequest::complete, serverEntityRequest::failure);
          }
        }  
      } catch (EntityException ee) {
        serverEntityRequest.failure(ee);
        serverEntityRequest.retired();
      }
    }
  }

  private void waitForTransactionOrderPersistenceFuture(TransactionID transactionID) {
    Future<Void> future = transactionOrderPersistenceFutures.get(transactionID);
    if(future != null) {
      try {
        future.get();
      } catch (InterruptedException | ExecutionException e) {
        throw new RuntimeException(e);
      } finally {
        transactionOrderPersistenceFutures.remove(transactionID);
      }
    }
  }
  
  private void disconnectClientDueToFailure(ClientID clientID) {
    safeGetChannel(clientID).ifPresent(channel->channel.close());
  }
  
  public void loadExistingEntities() {
    // issue-439: We need to sort these entities, ascending by consumerID.
    List<EntityData.Value> sortingList = new ArrayList<EntityData.Value>(this.persistor.getEntityPersistor().loadEntityData());
    Collections.sort(sortingList, new Comparator<EntityData.Value>() {
      @Override
      public int compare(EntityData.Value o1, EntityData.Value o2) {
        long firstID = o1.consumerID;
        long secondID = o2.consumerID;
        // NOTE:  The ids are unique.
        Assert.assertTrue(firstID != secondID);
        return (firstID > secondID)
            ? 1
            : -1;
      }});
    
    for(EntityData.Value entityValue : sortingList) {
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
  
  public void handleResentReferenceMessage(ReferenceMessage msg) {
    this.references.add(msg);
  }

  public void handleResentMessage(ResendVoltronEntityMessage resentMessage) {
    boolean cached = false;
    byte[] result = null;
    int index = -1;
    try {
      switch (resentMessage.getVoltronType()) {
        case CREATE_ENTITY:
          cached = this.persistor.getEntityPersistor().wasEntityCreatedInJournal(resentMessage.getSource(), resentMessage.getTransactionID().toLong());
          break;
        case DESTROY_ENTITY:
          cached = this.persistor.getEntityPersistor().wasEntityDestroyedInJournal(resentMessage.getSource(), resentMessage.getTransactionID().toLong());
          break;
        case RECONFIGURE_ENTITY:
          result = this.persistor.getEntityPersistor().reconfiguredResultInJournal(resentMessage.getSource(), resentMessage.getTransactionID().toLong());
          if (result != null) {
            cached = true;
          }
          break;
        case FETCH_ENTITY:
        case RELEASE_ENTITY:
        default:
          index = this.persistor.getTransactionOrderPersistor().getIndexToReplay(resentMessage.getSource(), resentMessage.getTransactionID());
          break;
      }
      if (cached) {
        ServerEntityRequestResponse response = new ServerEntityRequestResponse(EntityDescriptor.NULL_ID, ServerEntityAction.CREATE_ENTITY, resentMessage.getTransactionID(), resentMessage.getOldestTransactionOnClient(), resentMessage.getSource(), ()->safeGetChannel(resentMessage.getSource()), false, false);
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
      ServerEntityRequestResponse response = new ServerEntityRequestResponse(EntityDescriptor.NULL_ID, ServerEntityAction.CREATE_ENTITY, resentMessage.getTransactionID(), resentMessage.getOldestTransactionOnClient(), resentMessage.getSource(), ()->safeGetChannel(resentMessage.getSource()), false, false);
      response.received();
      response.failure(ee);
      response.retired();
    }
  }
  
  private void processAllResends(VoltronEntityMessage trigger) {
 //   TODO:  investigate the need to fold FETCH and RELEASE resends on top of each other
    if (this.references == null && this.resendReplayList == null && this.resendNewList == null) {
      return;
    } else {
      LOGGER.debug("RESENDS:START");
      synchronized (this) {
        while (reconnecting) {
          try {
            this.wait();
          } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
          }
        }
      }
    }

    this.stateManagerCleanup.run();

    // Clear the transaction order persistor since we are starting fresh.
    this.persistor.getTransactionOrderPersistor().clearAllRecords();
    
    for (ReferenceMessage msg : this.references) {
      LOGGER.debug("RESENDS:" + msg);
      try {
        EntityID eid = this.entityManager.getEntity(msg.getEntityDescriptor()).get().getID();
        Assert.assertEquals(eid, msg.getEntityDescriptor().getEntityID());
      } catch (EntityException ee) {
        // throwing here is big trouble, means the reference is gone but the client thinks it's still there
        throw new RuntimeException(ee);
      }
      executeResend(msg);
    }
    this.references = null;
    
    // Replay all the already-ordered messages.
    for (ResendVoltronEntityMessage message : this.resendReplayList) {
      LOGGER.debug("RESENDS:" + message);
      executeResend(message);
    }
    this.resendReplayList = null;
    
    // Replay all the new messages found during resends.
    for (ResendVoltronEntityMessage message : this.resendNewList) {
      LOGGER.debug("RESENDS:" + message);
      executeResend(message);
    }
//  remove tracking for any resent create journal entries
    this.persistor.getEntityPersistor().removeTrackingForClient(ClientID.NULL_ID);
    LOGGER.debug("RESENDS:END");
    this.resendNewList = null;
    
  }

  private Optional<MessageChannel> safeGetChannel(NodeID id) {
    try {
      return Optional.of(dsoChannelManager.getActiveChannel(id));
    } catch (NoSuchChannelException e) {
      return Optional.empty();
    }
  }

  private void executeResend(VoltronEntityMessage message) {
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
    MessagePayload payload = MessagePayload.commonMessagePayloadNotBusy(extendedData, entityMessage, doesRequireReplication);
    payload.setDebugId(message.toString());
    
    ProcessTransactionHandler.this.addMessage(sourceNodeID, descriptor, action, payload, transactionID, oldestTransactionOnClient, false);
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
      case LOCAL_PIPELINE_FLUSH:
        action = ServerEntityAction.LOCAL_FLUSH;
        break;
      case LOCAL_ENTITY_GC:
        action = ServerEntityAction.MANAGED_ENTITY_GC;
        break;
      default:
        // Unknown request type.
        Assert.fail();
        break;
    }
    return action;
  }
}
