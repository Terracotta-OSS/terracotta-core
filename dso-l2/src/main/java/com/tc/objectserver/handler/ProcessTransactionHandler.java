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
import com.tc.async.api.DirectExecutionMode;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Stage;
import com.tc.async.impl.MonitoringEventCreator;
import com.tc.tracing.Trace;
import com.tc.entity.VoltronEntityAppliedResponse;
import com.tc.entity.VoltronEntityMessage;
import com.tc.entity.VoltronEntityMultiResponse;
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
import com.tc.objectserver.api.ResultCapture;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.entity.MessagePayload;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.objectserver.entity.AbstractServerEntityRequestResponse;
import com.tc.objectserver.entity.ActivePassiveAckWaiter;
import com.tc.objectserver.entity.ClientDisconnectMessage;
import com.tc.objectserver.entity.ReconnectListener;
import com.tc.objectserver.entity.ReferenceMessage;
import com.tc.objectserver.entity.ServerEntityRequestImpl;
import com.tc.objectserver.entity.ServerEntityRequestResponse;
import com.tc.objectserver.persistence.EntityData;
import com.tc.objectserver.persistence.Persistor;
import com.tc.services.ClientMessageSender;
import com.tc.services.EntityMessengerService;
import com.tc.util.Assert;
import com.tc.util.SparseList;
import com.tc.util.concurrent.SetOnceFlag;

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
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.ReconnectRejectedException;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityNotFoundException;

public class ProcessTransactionHandler implements ReconnectListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProcessTransactionHandler.class);

  private final Persistor persistor;
  private final Runnable stateManagerCleanup;

  private final EntityManager entityManager;
  private final DSOChannelManager dsoChannelManager;

  // Data required for handling transaction resends.
  private List<ReferenceMessage> references;
  private List<VoltronEntityMessage> reconnectDone;
  private SparseList<VoltronEntityMessage> resendReplayList;
  private List<VoltronEntityMessage> resendNewList;
  private boolean reconnecting = true;

  private Stage<ResponseMessage> multiSend;
  private final ConcurrentHashMap<ClientID, VoltronEntityMultiResponse> invokeReturn = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<ClientID, Integer> inflightFetch = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<TransactionID, Future<Void>> transactionOrderPersistenceFutures = new ConcurrentHashMap<>();

  @Override
  public synchronized void reconnectComplete() {
    reconnecting = false;
    notify();
  }

  private final AbstractEventHandler<ResponseMessage> multiSender = new AbstractEventHandler<ResponseMessage>() {
    @Override
    public void handleEvent(ResponseMessage context) throws EventHandlerException {
      NodeID destinationID = context.getResponse().getDestinationNodeID();
      TCMessage response = context.getResponse();
      
      invokeReturn.remove((ClientID) destinationID, response);
      if (response instanceof VoltronEntityMultiResponse) {
        VoltronEntityMultiResponse voltronEntityMultiResponse = (com.tc.entity.VoltronEntityMultiResponse)response;
        voltronEntityMultiResponse.stopAdding();
        waitForTransactions(voltronEntityMultiResponse);
      } else if (response instanceof VoltronEntityAppliedResponse) {
        waitForTransactionOrderPersistenceFuture(((VoltronEntityAppliedResponse)response).getTransactionID());
      } else {
        Assert.fail("Unexpected message type: " + response.getClass());
      }
      boolean didSend = response.send();
      if (!didSend) {
        // It is possible for this send to fail.  Typically, it means that the client has disconnected.
        LOGGER.warn("Failed to send message to: " + destinationID);
      } else if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("sent " + response);
      }
    }
  };

  public AbstractEventHandler<ResponseMessage> getMultiResponseSender() {
    return multiSender;
  }

  private void waitForTransactions(VoltronEntityMultiResponse vmr) {
    vmr.replay(new VoltronEntityMultiResponse.ReplayReceiver() {
      @Override
      public void received(TransactionID tid) {
        waitForTransactionOrderPersistenceFuture(tid);
      }

      @Override
      public void retired(TransactionID tid) {
      }

      @Override
      public void result(TransactionID tid, byte[] result) {
      }

      @Override
      public void message(ClientInstanceID cid, byte[] message) {
      }

      @Override
      public void message(TransactionID tid, byte[] message) {
      }
    });
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

      Consumer<byte[]> completion = null;
      Consumer<EntityException> exception = null;
      switch(message.getVoltronType()) {
        case DISCONNECT_CLIENT:
          ClientDisconnectMessage disconnect = (ClientDisconnectMessage)message;
          completion = (raw)->disconnect.run();
          exception = (e)->disconnect.run();
          break;
        case FETCH_ENTITY:
// track fetch calls through the pipeline so disconnects work properly
          inflightFetch.compute(sourceNodeID, (client, count)->count == null ? 1 : count+1);
          Assert.assertNull(completion);
          Consumer<?> var = (raw)->inflightFetch.compute(sourceNodeID, (client, count)->count == 1 ? null : count - 1);
          completion = (Consumer<byte[]>)var;
          exception = (Consumer<EntityException>)var;
          break;
        case INVOKE_ACTION:
          if (message instanceof EntityMessengerService.FakeEntityMessage) {
            completion = ((EntityMessengerService.FakeEntityMessage) message).getCompletionHandler();
            exception = ((EntityMessengerService.FakeEntityMessage) message).getExceptionHandler();
          }
          break;
        default:
          if (message instanceof Runnable) {
            completion = (raw)->((Runnable)message).run();
          }
          break;
      }
      MessagePayload payload =  MessagePayload.commonMessagePayload(extendedData, entityMessage, doesRequireReplication, !message.getSource().isNull());
      ProcessTransactionHandler.this.addMessage(sourceNodeID, descriptor, action, payload, transactionID, oldestTransactionOnClient, completion, exception, requestedReceived);
    }

    @Override
    protected void initialize(ConfigurationContext context) {
      super.initialize(context);
      ServerConfigurationContext server = (ServerConfigurationContext)context;

      server.getL2Coordinator().getReplicatedClusterStateManager().setCurrentState(server.getL2Coordinator().getStateManager().getCurrentMode().getState());
      server.getL2Coordinator().getReplicatedClusterStateManager().goActiveAndSyncState();

      multiSend = server.getStage(ServerConfigurationContext.RESPOND_TO_REQUEST_STAGE, ResponseMessage.class);

//  go right to active state.  this only gets initialized once ACTIVE-COORDINATOR is entered
      reconnectDone = entityManager.enterActiveState();

      server.getClientHandshakeManager().addReconnectListener(ProcessTransactionHandler.this);
    }
  };

  private final ClientMessageSender sender = new ClientMessageSender() {
    @Override
    public void send(ClientID client, ClientInstanceID clientInstance, byte[] payload) {
        addSequentially(client, msg->msg.addServerMessage(clientInstance, payload));
    }

    @Override
    public void send(ClientID client, TransactionID transaction, byte[] payload) {
        addSequentially(client, msg->msg.addServerMessage(transaction, payload));
    }
  };

  public AbstractEventHandler<VoltronEntityMessage> getVoltronMessageHandler() {
    return this.voltronHandler;
  }

  public ClientMessageSender getClientMessageSender() {
    return sender;
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

  boolean removeClient(ClientID target) {
    return !inflightFetch.containsKey(target);
  }

  private void addSequentially(ClientID target, Predicate<VoltronEntityMultiResponse> adder) {
    // don't bother if the client isNull, no where to send the message
    // if not, compute the result and schedule send if neccessary
    if (!target.isNull()) {
      invokeReturn.compute(target, (client, vmr)-> {
        if (vmr != null) {
          boolean added = adder.test(vmr);
          Assert.assertTrue(added);
        } else {
          Optional<MessageChannel> channel = safeGetChannel(target);
          if (channel.isPresent()) {
            vmr = (VoltronEntityMultiResponse)channel.get().createMessage(TCMessageType.VOLTRON_ENTITY_MULTI_RESPONSE);
            boolean added = adder.test(vmr);
            Assert.assertTrue(added);
            // direct execution must be under compute lock because not all 
            // messages are coming from the pipeline
            if (DirectExecutionMode.isActivated() && multiSend.isEmpty()) {
              waitForTransactions(vmr);
              vmr.send();
              vmr = null;
            } else {
              multiSend.getSink().addToSink(new ResponseMessage(vmr));
            }
          }
        }
        return vmr;
      });
    }
  }

// only the process transaction thread will add messages here except for on reconnect
  private void addMessage(ClientID sourceNodeID, EntityDescriptor descriptor, ServerEntityAction action, MessagePayload entityMessage, TransactionID transactionID, TransactionID oldestTransactionOnClient, Consumer<byte[]> chaincomplete, Consumer<EntityException> chainfail, boolean requiresReceived) {
    // Version error or duplicate creation requests will manifest as exceptions here so catch them so we can send them back
    //  over the wire as an error in the request.

    // This is active-side processing so this is never a replicated message.
    boolean isReplicatedMessage = false;
    // In the general case, however, we need to pass this as a real ServerEntityRequest, into the entityProcessor.
    // Before we pass this on to the entity or complete it, directly, we can send the received() ACK, since we now know the message order.
    // Note that we only want to persist the messages with a true sourceNodeID.  Synthetic invocations and sync messages
    // don't have one (although sync messages shouldn't come down this path).
    Future<Void> transactionOrderPersistenceFuture = null;
    // if the client is valid and the transaction id is valid, then this came from a real client
    // and the client expects to be able to reconnect
    ServerEntityRequestImpl request = new ServerEntityRequestImpl(descriptor.getClientInstanceID(), action, sourceNodeID, transactionID, transactionID, requiresReceived);
    if (sourceNodeID != null && !sourceNodeID.isNull() && transactionID.isValid()) {
      Assert.assertTrue(oldestTransactionOnClient.isValid());
      // This client still needs transaction order persistence.
      transactionOrderPersistenceFuture = this.persistor.getTransactionOrderPersistor().updateWithNewMessage(sourceNodeID, transactionID, oldestTransactionOnClient);
    }

    Trace trace = null;
    if (Trace.isTraceEnabled()) {
      trace = new Trace(request.getTraceID(), "ProcessTransactionHandler.AddMessage");
      trace.start();
      trace.log("Handling " + action);
    }

    if (ServerEntityAction.CREATE_ENTITY == action) {
      long consumerID = this.persistor.getEntityPersistor().getNextConsumerID();
      // The common pattern for this is to pass an empty array on success ("found") or an exception on failure ("not found").
      LifecycleResultsCapture capture = new LifecycleResultsCapture(descriptor.getEntityID(), descriptor.getClientSideVersion(), consumerID, request, chaincomplete, chainfail, entityMessage.getRawPayload(), isReplicatedMessage);
      capture.setTransactionOrderPersistenceFuture(transactionOrderPersistenceFuture);
      try {
        EntityID entityID = descriptor.getEntityID();
        ManagedEntity temp = entityManager.createEntity(entityID, descriptor.getClientSideVersion(), consumerID, !capture.getNodeID().isNull());
        temp.addRequestMessage(capture, entityMessage, capture);
      } catch (EntityException ee) {
        capture.failure(ee);
      }
    } else {
      // At this point, we can now look up the actual managed entity.
      Optional<ManagedEntity> optionalEntity = null;
      try {
        optionalEntity = entityManager.getEntity(descriptor);
      } catch (EntityException ee) {
        ServerEntityRequestResponse rr = new ServerEntityRequestResponse(request, ()->safeGetChannel(sourceNodeID), chaincomplete, chainfail, isReplicatedMessage);
        rr.failure(ee);
        return;
      }
      if (!optionalEntity.isPresent()) {
        if (!descriptor.isIndexed()) {
          ServerEntityRequestResponse rr = new ServerEntityRequestResponse(request, ()->safeGetChannel(sourceNodeID), chaincomplete, chainfail, isReplicatedMessage);
          rr.failure(new EntityNotFoundException(descriptor.getEntityID().getClassName(), descriptor.getEntityID().getEntityName()));
          return;
        } else {
          if (descriptor.getClientInstanceID() != ClientInstanceID.NULL_ID) {
            throw new AssertionError("fetched entity not found " + descriptor + " action:" + action + " " + sourceNodeID);
          } else {
            //  can be null because of flush or disconnect
            LOGGER.error("fetched entity not found " + descriptor + " action:" + action + " " + sourceNodeID);
            return;
          }
        }
      }
      ManagedEntity entity = optionalEntity.get();
      // Note that it is possible to trigger an exception when decoding a message in addInvokeRequest.
      if (ServerEntityAction.INVOKE_ACTION == action) {
        InvokeHandler handler = new InvokeHandler(request, chaincomplete, chainfail);
        if(transactionOrderPersistenceFuture != null) {
          transactionOrderPersistenceFutures.put(transactionID, transactionOrderPersistenceFuture);
        }
        entity.addRequestMessage(handler, entityMessage, handler);
      } else if (action.isLifecycle()) {
        EntityID eid;
        long version;
        long consumerID;
        if (descriptor.isIndexed()) {
          consumerID = descriptor.getFetchID().toLong();
          version = entity.getVersion();
          eid = entity.getID();
        } else {
          eid = descriptor.getEntityID();
          version = descriptor.getClientSideVersion();
          consumerID = entity.getConsumerID();
        }
        LifecycleResultsCapture capture = new LifecycleResultsCapture(eid, version, consumerID, request, chaincomplete, chainfail, entityMessage.getRawPayload(), isReplicatedMessage);
        capture.setTransactionOrderPersistenceFuture(transactionOrderPersistenceFuture);
        entity.addRequestMessage(capture, entityMessage, capture);
      } else if (action == ServerEntityAction.MANAGED_ENTITY_GC && entity.isRemoveable()) {
        // MANAGED_ENTITY_GC may not be removeable if the entity was immediately recreated 
        // after destroy.  If this is the case, just schedule the action and it will act like a flush
        LOGGER.debug("removing " + entity.getID());
        entityManager.removeDestroyed(descriptor.getFetchID());
        //  no need to schedule for an entity that is removed
      } else {
        ServerEntityRequestResponse rr = new ServerEntityRequestResponse(request, ()->safeGetChannel(sourceNodeID), chaincomplete, chainfail, isReplicatedMessage);
        rr.setTransactionOrderPersistenceFuture(transactionOrderPersistenceFuture);
        entity.addRequestMessage(rr, entityMessage, rr);
      }
      if (trace != null) {
        trace.end();
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

  public void handleResentMessage(VoltronEntityMessage resentMessage) {
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
        ServerEntityRequest request = new ServerEntityRequestImpl(ClientInstanceID.NULL_ID, ServerEntityAction.CREATE_ENTITY, resentMessage.getSource(), resentMessage.getTransactionID(), resentMessage.getOldestTransactionOnClient(), true);
        ServerEntityRequestResponse response = new ServerEntityRequestResponse(request, ()->safeGetChannel(resentMessage.getSource()), null, null, false);
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
      ServerEntityRequest request = new ServerEntityRequestImpl(ClientInstanceID.NULL_ID, ServerEntityAction.CREATE_ENTITY, resentMessage.getSource(), resentMessage.getTransactionID(), resentMessage.getOldestTransactionOnClient(), true);
      ServerEntityRequestResponse response = new ServerEntityRequestResponse(request, ()->safeGetChannel(resentMessage.getSource()), null, null, false);

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
//  reconnect done for all entities
    for (VoltronEntityMessage msg : this.reconnectDone) {
      LOGGER.debug("RECONNECT DONE:" + msg);
      executeResend(msg);
    }
    this.reconnectDone = null;

    // Replay all the already-ordered messages.
    for (VoltronEntityMessage message : this.resendReplayList) {
      LOGGER.debug("RESENDS:" + message);
      executeResend(message);
    }
    this.resendReplayList = null;

    // Replay all the new messages found during resends.
    for (VoltronEntityMessage message : this.resendNewList) {
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
      if (!id.isNull()) {
        return Optional.of(dsoChannelManager.getActiveChannel(id));
      }
    } catch (NoSuchChannelException e) {
      // ignore
    }
    return Optional.empty();
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

    boolean requestedReceived = message.doesRequestReceived();
    Consumer<byte[]> completion = null;
    if (message instanceof Runnable) {
      completion = (r)->((Runnable)message).run();
    }
    ProcessTransactionHandler.this.addMessage(sourceNodeID, descriptor, action, payload, transactionID, oldestTransactionOnClient, completion, null, requestedReceived);
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
      case DISCONNECT_CLIENT:
        action = ServerEntityAction.DISCONNECT_CLIENT;
        break;
      default:
        // Unknown request type.
        Assert.fail();
        break;
    }
    return action;
  }

  private class InvokeHandler extends AbstractServerEntityRequestResponse implements ResultCapture {

    private Supplier<ActivePassiveAckWaiter> waiter;
    private final SetOnceFlag sent = new SetOnceFlag();
    private final SetOnceFlag failure = new SetOnceFlag();

    InvokeHandler(ServerEntityRequest request, Consumer<byte[]> complete, Consumer<EntityException> failure) {
      super(request, complete, failure);
    }

    @Override
    public Optional<MessageChannel> getReturnChannel() {
      return safeGetChannel(getNodeID());
    }

    @Override
    public void received() {
      addSequentially(getNodeID(), adder->adder.addReceived(getTransaction()));
    }

    @Override
    public void failure(EntityException exp) {
      sendFailure(exp);
    }

    @Override
    public void complete(byte[] result) {
      sendResponse(result);
    }

    @Override
    public void complete() {
      sendResponse(new byte[0]);
    }

    @Override
    public void message(byte[] msg) {
      if (getNodeID().isNull()) {
        super.complete(msg);
      } else {
        addSequentially(getNodeID(), addTo->addTo.addServerMessage(getTransaction(), msg));
      }
    }

    @Override
    public void setWaitFor(Supplier<ActivePassiveAckWaiter> waiter) {
      this.waiter = waiter;
    }

    @Override
    public void waitForReceived() {
      this.waiter.get().waitForReceived();
    }

    private void sendResponse(byte[] result) {
      if (!failure.isSet() && sent.attemptSet()) {
        if (getNodeID().isNull()) {
          super.complete(result);
        } else {
          addSequentially(getNodeID(), addTo->addTo.addResult(getTransaction(), result));
        }
      } else {
        throw new AssertionError();
      }
    }

    private void sendFailure(EntityException exception) {
      if (failure.attemptSet()) {
        if (getNodeID().isNull()) {
          super.failure(exception);
        } else {
          safeGetChannel(getNodeID()).ifPresent(channel -> {
            VoltronEntityAppliedResponse failMessage = (VoltronEntityAppliedResponse)channel.createMessage(TCMessageType.VOLTRON_ENTITY_COMPLETED_RESPONSE);
            failMessage.setFailure(getTransaction(), exception);
            invokeReturn.compute(getNodeID(), (client, vmr)-> {
              if (vmr == null && DirectExecutionMode.isActivated() && multiSend.isEmpty()) {
                waitForTransactionOrderPersistenceFuture(failMessage.getTransactionID());
                failMessage.send();
              } else {
                multiSend.getSink().addToSink(new ResponseMessage(failMessage));
              }
              // unmap anything that was previously there
              return null;
            });
          });
        }
        MonitoringEventCreator.finish();
      } else {
        throw new AssertionError();
      }
    }

    @Override
    public void retired() {
      this.waiter.get().waitForCompleted();
      if (!getNodeID().isNull()) {
        Assert.assertTrue(sent.isSet());
        addSequentially(getNodeID(), addTo -> addTo.addRetired(InvokeHandler.this.getTransaction()));
      }
      MonitoringEventCreator.finish();
    }
  }

  private class LifecycleResultsCapture extends AbstractServerEntityRequestResponse implements ResultCapture {

    private final EntityID eid;
    private final long version;
    private final long consumerID;
    private final byte[] config;

    private Supplier<ActivePassiveAckWaiter> setOnce;

    public LifecycleResultsCapture(EntityID eid, long version, long consumerID, ServerEntityRequest request, Consumer<byte[]> complete, Consumer<EntityException> fail, byte[] config, boolean isReplicatedMessage) {
      super(request, complete, fail);
      this.eid = eid;
      this.version = version;
      this.consumerID = consumerID;
      this.config = config;
      super.autoRetire(true);
    }

    @Override
    public Optional<MessageChannel> getReturnChannel() {
      return safeGetChannel(getNodeID());
    }

    @Override
    public boolean requiresReceived() {
      return true;
    }

    @Override
    public void retired() {
      Assert.fail("retired should never be called on a lifecycle operation");
    }

    @Override
    public void message(byte[] message) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setWaitFor(Supplier<ActivePassiveAckWaiter> waiter) {
      this.setOnce = waiter;
    }

    @Override
    public void waitForReceived() {
      this.setOnce.get().waitForReceived();
    }

    @Override
    public void failure(EntityException e) {
      switch (this.getAction()) {
        case CREATE_ENTITY:
          persistor.getEntityPersistor().entityCreateFailed(eid, getNodeID(), getTransaction().toLong(), getOldestTransactionOnClient().toLong(), e);
          break;
        case RECONFIGURE_ENTITY:
          EntityExistenceHelpers.recordReconfigureEntity(persistor.getEntityPersistor(), entityManager, getNodeID(), getTransaction(), getOldestTransactionOnClient(), eid, version, null, e);
          break;
        case DESTROY_ENTITY:
          EntityExistenceHelpers.recordDestroyEntity(persistor.getEntityPersistor(), entityManager, getNodeID(), getTransaction(), getOldestTransactionOnClient(), eid, e);
          break;
        case FETCH_ENTITY:
        case RELEASE_ENTITY:
          if (e.getCause() instanceof ReconnectRejectedException) {
            disconnectClientDueToFailure(getNodeID());
            return;
          }
          break;
        default:

      }
      if (setOnce != null) {
        ActivePassiveAckWaiter waiter = setOnce.get();
        waiter.waitForCompleted();
        if (waiter.verifyLifecycleResult(false)) {
          LOGGER.warn("ZAP occurred while processing " + getAction() + " on " + this.eid);
        }
      }
      super.failure(e);
    }

    @Override
    public void complete() {
      switch(this.getAction()) {
        case CREATE_ENTITY:
          if (getNodeID().isNull()) {
            persistor.getEntityPersistor().entityCreatedNoJournal(eid, version, consumerID, false, config);
          } else {
            persistor.getEntityPersistor().entityCreated(getNodeID(), getTransaction().toLong(), getOldestTransactionOnClient().toLong(), eid, version, consumerID, !getNodeID().isNull(), config);
          }
          break;
        case RECONFIGURE_ENTITY:
          EntityExistenceHelpers.recordReconfigureEntity(persistor.getEntityPersistor(), entityManager, getNodeID(), getTransaction(), getOldestTransactionOnClient(), eid, version, config, null);
          break;
        case DESTROY_ENTITY:
          EntityExistenceHelpers.recordDestroyEntity(persistor.getEntityPersistor(), entityManager, getNodeID(), getTransaction(), getOldestTransactionOnClient(), eid, null);
          break;
        case RELEASE_ENTITY:
        case FETCH_ENTITY:
        default:
      }
      if (setOnce != null) {
        ActivePassiveAckWaiter waiter = setOnce.get();
        waiter.waitForCompleted();
        if (waiter.verifyLifecycleResult(true)) {
          LOGGER.warn("ZAP occurred while processing " + getAction() + " on " + this.eid);
        }
      }
      super.complete();
    }

    @Override
    public void complete(byte[] value) {
      switch(this.getAction()) {
        case CREATE_ENTITY:
          persistor.getEntityPersistor().entityCreated(getNodeID(), getTransaction().toLong(), getOldestTransactionOnClient().toLong(), eid, version, consumerID, !getNodeID().isNull(), config);
          break;
        case RECONFIGURE_ENTITY:
          EntityExistenceHelpers.recordReconfigureEntity(persistor.getEntityPersistor(), entityManager, getNodeID(), getTransaction(), getOldestTransactionOnClient(), eid, version, config, null);
          break;
        case DESTROY_ENTITY:
          EntityExistenceHelpers.recordDestroyEntity(persistor.getEntityPersistor(), entityManager, getNodeID(), getTransaction(), getOldestTransactionOnClient(), eid, null);
          break;
        case FETCH_ENTITY:
        case RELEASE_ENTITY:
          break;
        default:
      }
      if (setOnce != null) {
        ActivePassiveAckWaiter waiter = setOnce.get();
        waiter.waitForCompleted();
        if (waiter.verifyLifecycleResult(true)) {
          LOGGER.warn("ZAP occurred while processing " + getAction() + " on " + this.eid);
        }
      }
      super.complete(value);
    }
  }
}
