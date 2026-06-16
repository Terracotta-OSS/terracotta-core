/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2026
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Stage;
import com.tc.bytes.TCByteBuffer;
import com.tc.tracing.Trace;
import com.tc.entity.VoltronEntityAppliedResponse;
import com.tc.entity.VoltronEntityMessage;
import com.tc.entity.VoltronEntityMultiResponse;
import com.tc.entity.VoltronEntityResponse;
import com.tc.exception.ServerException;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.NetworkRecall;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.utils.L2Utils;
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
import com.tc.objectserver.entity.ClientDisconnectMessage;
import com.tc.objectserver.entity.ReconnectListener;
import com.tc.objectserver.entity.ReferenceMessage;
import com.tc.objectserver.entity.ServerEntityRequestImpl;
import com.tc.objectserver.persistence.EntityData;
import com.tc.objectserver.persistence.Persistor;
import com.tc.services.ClientMessageSender;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.EntityMessage;
import com.tc.net.protocol.tcm.TCAction;
import com.tc.objectserver.api.ServerEntityResponse;
import com.tc.objectserver.entity.NetworkInvokeResponse;
import com.tc.objectserver.entity.WaitingResultCapture;
import com.tc.objectserver.entity.NetworkServerEntityResponse;
import com.tc.objectserver.entity.ResultCaptureImpl;
import com.tc.objectserver.entity.LifecycleResultCapture;
import com.tc.objectserver.entity.NoopResultCapture;

public class ProcessTransactionHandler implements ReconnectListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProcessTransactionHandler.class);

  private final Persistor persistor;

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
      TCAction response = context.getResponse();

      if (response instanceof VoltronEntityMultiResponse) {
        VoltronEntityMultiResponse voltronEntityMultiResponse = (VoltronEntityMultiResponse)response;
        VoltronEntityMultiResponse sub = (VoltronEntityMultiResponse)response.getChannel().createMessage(TCMessageType.VOLTRON_ENTITY_MULTI_RESPONSE);
        invokeReturn.put((ClientID)destinationID, sub);
        voltronEntityMultiResponse.stopAdding();
        if (!transactionOrderPersistenceFutures.isEmpty()) {
          waitForTransactions(voltronEntityMultiResponse);
        }
      } else if (response instanceof VoltronEntityAppliedResponse) {
        waitForTransactionOrderPersistenceFuture(((VoltronEntityAppliedResponse)response).getTransactionID());
      } else {
        // only applied messages should be sent back to the client except on resent messages
        // that path is unoptimized so regular received messages can hit this path
      }
      NetworkRecall networkMessage = response.send();
      if (networkMessage == null) {
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
        waitForTransactionOrderPersistenceFuture(tid);
      }

      @Override
      public void result(TransactionID tid, byte[] result) {
        waitForTransactionOrderPersistenceFuture(tid);
      }

      @Override
      public void message(ClientInstanceID cid, byte[] message) {
      }

      @Override
      public void message(TransactionID tid, byte[] message) {
      }

      @Override
      public void stats(TransactionID tid, long[] message) {
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
      TCByteBuffer extendedData = message.getExtendedData();

      TransactionID transactionID = message.getTransactionID();
      boolean doesRequireReplication = message.doesRequireReplication();
      TransactionID oldestTransactionOnClient = message.getOldestTransactionOnClient();
      boolean requestedReceived = message.doesRequestReceived();
      boolean requestedRetired = message.doesRequestRetired();
      boolean canBeBusy = !sourceNodeID.isNull();

      Consumer<byte[]> completion = null;
      Consumer<ServerException> exception = null;
      switch(message.getVoltronType()) {
        case DISCONNECT_CLIENT:
          //  remove any invoke returns to prevent leak if doing pre-allocation
          invokeReturn.remove(message.getSource());
          ClientDisconnectMessage disconnect = (ClientDisconnectMessage)message;
          completion = (raw)->disconnect.run();
          exception = disconnect::disconnectException;
          break;
        case FETCH_ENTITY:
// track fetch calls through the pipeline so disconnects work properly
          inflightFetch.compute(sourceNodeID, (client, count)->count == null ? 1 : count+1);
          Assert.assertNull(completion);
          Consumer<?> var = (raw)->inflightFetch.compute(sourceNodeID, (client, count)->count == 1 ? null : count - 1);
          completion = (Consumer<byte[]>)var;
          exception = (Consumer<ServerException>)var;
          break;
        case INVOKE_ACTION:
          completion = message.getCompletionHandler();
          exception = message.getExceptionHandler();
          break;
        default:
          if (message instanceof Runnable) {
            completion = (raw)->((Runnable)message).run();
          }
          break;
      }
      MessagePayload payload =  MessagePayload.commonMessagePayload(extendedData, entityMessage, doesRequireReplication, canBeBusy);
      ProcessTransactionHandler.this.addMessage(sourceNodeID, descriptor, action, payload, transactionID, oldestTransactionOnClient, completion, exception, requestedReceived, requestedRetired, !message.isServerRequest());
    }

    @Override
    protected void initialize(ConfigurationContext context) {
      super.initialize(context);
      ServerConfigurationContext server = (ServerConfigurationContext)context;

      multiSend = server.getStage(ServerConfigurationContext.RESPOND_TO_REQUEST_STAGE, ResponseMessage.class);

//  go right to active state.  this only gets initialized once ACTIVE-COORDINATOR is entered
      reconnectDone = entityManager.enterActiveState();

      server.getClientHandshakeManager().addReconnectListener(ProcessTransactionHandler.this);
    }
  };

  private final ClientMessageSender sender = new ClientMessageSender() {
    @Override
    public void send(ClientID client, ClientInstanceID clientInstance, byte[] payload) {
      ServerEntityRequest req = new ServerEntityRequestImpl(
              clientInstance,
              ServerEntityAction.INVALID, client,
              TransactionID.NULL_ID,
              TransactionID.NULL_ID, false);
      ResultCapture send = createInvokeResponse(true, req, false, false);
      send.message(payload);
    }

    @Override
    public void send(ClientID client, TransactionID transaction, byte[] payload) {
      ServerEntityRequest req = new ServerEntityRequestImpl(
              ClientInstanceID.NULL_ID,
              ServerEntityAction.INVALID, client,
              transaction,
              TransactionID.NULL_ID, false);
      ResultCapture send = createInvokeResponse(true, req, false, false);
      send.message(payload);
    }
  };

  public AbstractEventHandler<VoltronEntityMessage> getVoltronMessageHandler() {
    return this.voltronHandler;
  }

  public ClientMessageSender getClientMessageSender() {
    return sender;
  }

  public ProcessTransactionHandler(Persistor persistor, DSOChannelManager channelManager, EntityManager entityManager) {
    this.persistor = persistor;
    this.dsoChannelManager = channelManager;
    this.entityManager = entityManager;

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
  public Iterable<ManagedEntity> snapshotEntityList(Predicate<ManagedEntity> runFirst) {
    return entityManager.snapshot(runFirst);
  }

  boolean removeClient(ClientID target) {
    return !inflightFetch.containsKey(target);
  }

  private void insertMessageInStream(VoltronEntityResponse msg) {
    // cutoff multi response so that a multi-message enqueued before this message do not
    // capture new messages intended to be sent after this one.
    // only actions that were client generated need to be sent back to the client
    if (!msg.getDestinationNodeID().isNull()  && !msg.getTransactionID().isNull()) {
      invokeReturn.compute((ClientID)msg.getDestinationNodeID(), (c,v)->{
        if (v != null) {
          v.stopAdding();
        }
        multiSend.getSink().addToSink(new ResponseMessage(msg));
        return null;
      });
    }
  }

// only the process transaction thread will add messages here except for on reconnect
  private void addMessage(ClientID sourceNodeID, EntityDescriptor descriptor, ServerEntityAction action,
          MessagePayload entityMessage, TransactionID transactionID, TransactionID oldestTransactionOnClient,
          Consumer<byte[]> chaincomplete, Consumer<ServerException> chainfail, boolean requiresReceived, boolean requiresRetired, boolean sendToClient) {
    // this is capture for server sent messaging that request return.
    ResultCapture serverCapture = new ResultCaptureImpl(null, chaincomplete, null, chainfail);

    Future<Void> transactionOrderPersistenceFuture = null;
    // sendToClient flags client sent messages
    ServerEntityRequestImpl request = new ServerEntityRequestImpl(descriptor.getClientInstanceID(), action, sourceNodeID, transactionID, oldestTransactionOnClient, requiresReceived);
    if (sendToClient) {
      Assert.assertFalse(sourceNodeID.isNull());
      Assert.assertTrue(transactionID.isValid());
      // This client still needs transaction order persistence.
      transactionOrderPersistenceFuture = this.persistor.getTransactionOrderPersistor().updateWithNewMessage(sourceNodeID, transactionID, oldestTransactionOnClient);
    }
    //  this capture needs to be first.  It handles waiting for received on passives and waiting for
    //  transaction order persistence
    ResultCapture waitingCapture = createWaitingResponse(transactionOrderPersistenceFuture, request.requiresReceived());

    Trace trace = null;
    if (Trace.isTraceEnabled()) {
      trace = new Trace(request.getTraceID(), "ProcessTransactionHandler.AddMessage");
      trace.start();
      trace.log("Handling " + action);
    }

    if (ServerEntityAction.CREATE_ENTITY == action) {
      long consumerID = this.persistor.getEntityPersistor().getNextConsumerID();
      //  lifecycle capture handles entity persistence and passive verification
      ResultCapture lifecycle = createLifecycleResponse(request, descriptor, consumerID, entityMessage.getRawPayload());
      ResultCapture complete = ResultCapture.chain(waitingCapture, lifecycle, createClientResponse(sendToClient, request), serverCapture);

      try {
        EntityID entityID = descriptor.getEntityID();
        ManagedEntity temp = entityManager.createEntity(entityID, descriptor.getClientSideVersion(), consumerID);
        temp.addRequestMessage(request, entityMessage, complete);
      } catch (ServerException ee) {
        complete.failure(ee);
      }
    } else {
      // At this point, we can now look up the actual managed entity.
      Optional<ManagedEntity> optionalEntity = null;
      //  this chain handles errors due to entity lookup problems.
      try {
        optionalEntity = entityManager.getEntity(descriptor);
        if (!optionalEntity.isPresent()) {
          if (!descriptor.isIndexed()) {
            throw ServerException.createNotFoundException(descriptor.getEntityID());
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
      } catch (ServerException ee) {
        // error occured during lookup.  report the exception and return
        ResultCapture.chain(waitingCapture, createClientResponse(sendToClient, request), serverCapture)
            .failure(ee);
        return;
      }

      ManagedEntity entity = optionalEntity.get();
      // Note that it is possible to trigger an exception when decoding a message in addInvokeRequest.
      if (ServerEntityAction.INVOKE_ACTION == action) {
        boolean sendStats = safeGetChannel(sourceNodeID).map(c->{
          Object attach = c.getAttachment("SendStats");
          if (attach != null) {
            return (boolean)attach;
          } else {
            return false;
          }
        }).orElse(false);
        ResultCapture complete = ResultCapture.chain(waitingCapture, createInvokeResponse(sendToClient, request, sendStats, requiresRetired), serverCapture);

        if(transactionOrderPersistenceFuture != null) {
          transactionOrderPersistenceFutures.put(transactionID, transactionOrderPersistenceFuture);
        }

        entity.addRequestMessage(request, entityMessage, complete);
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

        ResultCapture lifecycle = createLifecycleResponse(request,
                descriptor, consumerID, entityMessage.getRawPayload());

        entity.addRequestMessage(request, entityMessage,
                ResultCapture.chain(waitingCapture, lifecycle, createClientResponse(sendToClient, request), serverCapture));
      } else if (action == ServerEntityAction.MANAGED_ENTITY_GC && entity.isRemoveable()) {
        // MANAGED_ENTITY_GC may not be removeable if the entity was immediately recreated
        // after destroy.  If this is the case, just schedule the action and it will act like a flush
        LOGGER.debug("removing " + entity.getID());
        entityManager.removeDestroyed(descriptor.getFetchID());
        //  no need to schedule for an entity that is removed
      } else {
        entity.addRequestMessage(request, entityMessage,
                ResultCapture.chain(waitingCapture, createClientResponse(sendToClient, request), serverCapture));
      }
      if (trace != null) {
        trace.end();
      }
    }
  }

  private ResultCapture createWaitingResponse(Future<Void> transactionOrderPersistenceFuture, boolean requiresReceived) {
    return  new WaitingResultCapture(transactionOrderPersistenceFuture, requiresReceived);
  }

  private ResultCapture createClientResponse(boolean sendToClient, ServerEntityRequest request) {
    if (sendToClient) {
    return new NetworkServerEntityResponse(request.getTransaction(),
            (type)->safeGetChannel(request.getNodeID()).map(c->c.createMessage(type)).orElse(null),
      this::insertMessageInStream);
    } else {
      return NoopResultCapture.noop();
    }
  }

  private ResultCapture createLifecycleResponse(ServerEntityRequest request, EntityDescriptor descriptor, long consumerID, byte[] config) {
    return new LifecycleResultCapture(
            descriptor,
            consumerID, request,
            config,
            persistor,
            entityManager,
            (node)->safeGetChannel(request.getNodeID()).orElse(null));
  }

  private ResultCapture createInvokeResponse(boolean sendToClient, ServerEntityRequest request, boolean sendStats, boolean retiredRequired) {
    if (sendToClient) {
      return new NetworkInvokeResponse(
        request.getNodeID(),
        request.getClientInstance(),
        request.getTransaction(),
        (type)->safeGetChannel(request.getNodeID()).map(c->c.createMessage(type)).orElse(null),
        invokeReturn,
        multiSend,
        request.requiresReceived(),
        sendStats,
        retiredRequired);
    } else {
      return NoopResultCapture.noop();
    }
  }

  private void waitForTransactionOrderPersistenceFuture(TransactionID transactionID) {
    if (!transactionOrderPersistenceFutures.isEmpty()) {
      Future<Void> future = transactionOrderPersistenceFutures.remove(transactionID);
      if(future != null) {
        try {
          future.get();
        } catch (InterruptedException ie) {
          L2Utils.handleInterrupted(LOGGER, ie);
        } catch (ExecutionException e) {
          throw new RuntimeException(e);
        }
      }
    }
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
      } catch (ServerException e) {
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
    ServerEntityAction cachedType = null;
    byte[] result = null;
    int index = -1;
    try {
      switch (resentMessage.getVoltronType()) {
        case CREATE_ENTITY:
          cached = this.persistor.getEntityPersistor().wasEntityCreatedInJournal(resentMessage.getEntityDescriptor().getEntityID(), resentMessage.getSource(), resentMessage.getTransactionID().toLong());
          cachedType = ServerEntityAction.CREATE_ENTITY;
          break;
        case DESTROY_ENTITY:
          cached = this.persistor.getEntityPersistor().wasEntityDestroyedInJournal(resentMessage.getEntityDescriptor().getEntityID(), resentMessage.getSource(), resentMessage.getTransactionID().toLong());
          cachedType = ServerEntityAction.DESTROY_ENTITY;
          break;
        case RECONFIGURE_ENTITY:
          result = this.persistor.getEntityPersistor().reconfiguredResultInJournal(resentMessage.getEntityDescriptor().getEntityID(), resentMessage.getSource(), resentMessage.getTransactionID().toLong());
          if (result != null) {
            cached = true;
            cachedType = ServerEntityAction.RECONFIGURE_ENTITY;
          }
          break;
        case FETCH_ENTITY:
          cached = true;
          cachedType = ServerEntityAction.FETCH_ENTITY;
          throw ServerException.createBusyException(resentMessage.getEntityDescriptor().getEntityID());
        case RELEASE_ENTITY:
          cached = true;
          cachedType = ServerEntityAction.RELEASE_ENTITY;
          throw ServerException.createBusyException(resentMessage.getEntityDescriptor().getEntityID());
        default:
          index = this.persistor.getTransactionOrderPersistor().getIndexToReplay(resentMessage.getSource(), resentMessage.getTransactionID());
          break;
      }
      if (cached) {
        ServerEntityRequest request = new ServerEntityRequestImpl(resentMessage.getEntityDescriptor().getClientInstanceID(), cachedType, resentMessage.getSource(), resentMessage.getTransactionID(), resentMessage.getOldestTransactionOnClient(), true);
        ResultCapture response = createClientResponse(!resentMessage.isServerRequest(), request);
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
    } catch (ServerException ee) {
      ServerEntityRequest request = new ServerEntityRequestImpl(resentMessage.getEntityDescriptor().getClientInstanceID(), cachedType, resentMessage.getSource(), resentMessage.getTransactionID(), resentMessage.getOldestTransactionOnClient(), true);
      ResultCapture response = createClientResponse(!resentMessage.isServerRequest(), request);
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
            L2Utils.handleInterrupted(LOGGER, ie);
          }
        }
      }
    }

    // Clear the transaction order persistor since we are starting fresh.
    this.persistor.getTransactionOrderPersistor().clearAllRecords();

    for (ReferenceMessage msg : this.references) {
      LOGGER.debug("RESENDS:" + msg);
      try {
        EntityID eid = this.entityManager.getEntity(msg.getEntityDescriptor()).get().getID();
        Assert.assertEquals(eid, msg.getEntityDescriptor().getEntityID());
      } catch (ServerException ee) {
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
    TCByteBuffer extendedData = message.getExtendedData();

    TransactionID transactionID = message.getTransactionID();
    boolean doesRequireReplication = message.doesRequireReplication();
    TransactionID oldestTransactionOnClient = message.getOldestTransactionOnClient();
    MessagePayload payload = MessagePayload.commonMessagePayloadNotBusy(extendedData, entityMessage, doesRequireReplication);
    payload.setDebugId(message.toString());

    boolean requestedReceived = message.doesRequestReceived();
    boolean requestedRetired = message.doesRequestRetired();
    Consumer<byte[]> completion = null;
    if (message instanceof Runnable) {
      completion = (r)->((Runnable)message).run();
    }
    ProcessTransactionHandler.this.addMessage(sourceNodeID, descriptor, action, payload, transactionID, oldestTransactionOnClient, completion, null, requestedReceived, requestedRetired, !message.isServerRequest());
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
}
