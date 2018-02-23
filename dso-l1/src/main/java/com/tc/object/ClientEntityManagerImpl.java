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
package com.tc.object;

import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.tracing.Trace;
import com.tc.util.Throwables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.exception.ConnectionClosedException;
import org.terracotta.exception.EntityException;

import com.tc.entity.NetworkVoltronEntityMessage;
import com.tc.entity.ResendVoltronEntityMessage;
import com.tc.entity.VoltronEntityMessage;
import com.tc.entity.VoltronEntityMultiResponse;
import com.tc.entity.VoltronEntityResponse;
import com.tc.exception.EntityBusyException;
import com.tc.exception.EntityReferencedException;
import com.tc.exception.VoltronWrapperException;
import com.tc.logging.ClientIDLogger;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.UnknownNameException;
import com.tc.object.msg.ClientEntityReferenceContext;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;
import com.tc.util.Util;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityServerUncaughtException;


public class ClientEntityManagerImpl implements ClientEntityManager {
  private final Logger logger;
  
  private final ClientMessageChannel channel;
  private final ConcurrentMap<TransactionID, InFlightMessage> inFlightMessages;
  private final MessagePendingCount requestTickets = new MessagePendingCount();
  private final AtomicLong currentTransactionID;

  private final ClientEntityStateManager stateManager;
  private final ConcurrentMap<ClientInstanceID, EntityClientEndpointImpl<?, ?>> objectStoreMap;
    
  private final StageManager stages;
  
  private final ExecutorService endpointCloser = Executors.newWorkStealingPool();
//  for testing
  private boolean wasBusy = false;
  
  public ClientEntityManagerImpl(ClientMessageChannel channel, StageManager mgr) {
    this.logger = new ClientIDLogger(channel, LoggerFactory.getLogger(ClientEntityManager.class));
    
    this.channel = channel;

    this.inFlightMessages = new ConcurrentHashMap<>();
    this.currentTransactionID = new AtomicLong();
    this.stateManager = new ClientEntityStateManager();
    this.objectStoreMap = new ConcurrentHashMap<>(10240, 0.75f, 128);
    this.stages = mgr;      
  }
  
  public boolean checkBusy() {
    try {
      return wasBusy;
    } finally {
      wasBusy = false;
    }
  } 
  
  private synchronized boolean enqueueMessage(InFlightMessage msg, long timeout, TimeUnit unit, boolean waitUntilRunning) throws TimeoutException {
    boolean enqueued = true;
    boolean interrupted = false;
    long end = (timeout > 0) ? System.nanoTime() + unit.toNanos(timeout) : 0;
 //  stop drains the permits so even if asked to not waitUntilRunning, stop is still checked
    while ((waitUntilRunning && !this.stateManager.isRunning()) || !requestTickets.messagePendingSlotAvailable()) {
      try {
        if (!this.stateManager.isShutdown()) {
          long timing = (end > 0) ? end - System.nanoTime() : 0;
          if (timing < 0) {
            throw new TimeoutException();
          } else {
            wait(timing / TimeUnit.MILLISECONDS.toNanos(1), (int)(timing % TimeUnit.MILLISECONDS.toNanos(1))); 
          }
        } else {
          enqueued = false;
          break;
        }
      } catch (InterruptedException ie) {
        interrupted = true;
      }
    }
    if (interrupted) {
      Thread.currentThread().interrupt();
    }
    if (enqueued) {
      inFlightMessages.put(msg.getTransactionID(), msg);
      requestTickets.messagePending();
    }
    return enqueued;
  }
  
  @SuppressWarnings("rawtypes")
  @Override
  public EntityClientEndpoint fetchEntity(EntityID entity, long version, ClientInstanceID instance, MessageCodec<? extends EntityMessage, ? extends EntityResponse> codec, Runnable closeHook) throws EntityException {
    return internalLookup(entity, version, instance, codec, closeHook);
  }

  @Override
  public void handleMessage(TransactionID tid, byte[] message) {
    InFlightMessage msg = this.inFlightMessages.get(tid);
    if (msg != null) {
      msg.handleMessage(message);
    } else {
      Assert.fail("transaction " + tid + " not found. Ignoring message.");
    }
  }
  
  @Override
  public void handleMessage(ClientInstanceID clientInstance, byte[] message) {
    EntityClientEndpoint<?, ?> endpoint = this.objectStoreMap.get(clientInstance);
    if (endpoint != null) {
      deliverInboundMessage(endpoint, message);
    } else {
      logger.info("Instance " + clientInstance + " not found. Ignoring message.");
    }
  }
  
  private void deliverInboundMessage(EntityClientEndpoint endpoint, byte[] msg) {
    EntityClientEndpointImpl<?, ?> endpointImpl = (EntityClientEndpointImpl<?, ?>) endpoint;
    try {
        endpointImpl.handleMessage(msg);
    } catch (MessageCodecException e) {
      // For now (at least), we will fail on this codec exception since it indicates a serious bug in the entity
      // implementation.
      Assert.fail(e.getLocalizedMessage());
    }
  }

  @Override
  public byte[] createEntity(EntityID entityID, long version, byte[] config) throws EntityException {
    // A create needs to be replicated.
    boolean requiresReplication = true;
    final NetworkVoltronEntityMessage message = createMessageWithoutClientInstance(entityID, version, requiresReplication, config, VoltronEntityMessage.Type.CREATE_ENTITY, lifecycleAcks());

    return sendMessageWhileBusy(message, lifecycleAcks(), "ClientEntityManagerImpl.createEntity");
  }

  @Override
  public byte[] reconfigureEntity(EntityID entityID, long version, byte[] config) throws EntityException {
    // A create needs to be replicated.
    boolean requiresReplication = true;
    NetworkVoltronEntityMessage message = createMessageWithoutClientInstance(entityID, version, requiresReplication, config, VoltronEntityMessage.Type.RECONFIGURE_ENTITY, lifecycleAcks());
    return sendMessageWhileBusy(message, lifecycleAcks(), "ClientEntityManagerImpl.reconfigureEntity");
  }
  
  private Set<VoltronEntityMessage.Acks> lifecycleAcks() {
    return Collections.singleton(VoltronEntityMessage.Acks.RETIRED);
  }
  
  @Override
  public boolean destroyEntity(EntityID entityID, long version) throws EntityException {
    // A destroy needs to be replicated.
    boolean requiresReplication = true;
    // A destroy call has no extended data.
    byte[] emtpyExtendedData = new byte[0];
    NetworkVoltronEntityMessage message = createMessageWithoutClientInstance(entityID, version, requiresReplication, emtpyExtendedData, VoltronEntityMessage.Type.DESTROY_ENTITY, lifecycleAcks());
    try {
      //  don't care about the return
      sendMessageWhileBusy(message, lifecycleAcks(), "ClientEntityManagerImpl.destroyEntity");
    } catch (EntityReferencedException r) {
      return false;
    }
    return true;
  }

  @Override
  public InFlightMessage invokeActionWithTimeout(EntityID eid, EntityDescriptor entityDescriptor, Set<VoltronEntityMessage.Acks> acks, InFlightMonitor monitor, boolean requiresReplication, boolean shouldBlockGetOnRetire, boolean deferred, long invokeTimeout, TimeUnit units, byte[] payload) throws InterruptedException, TimeoutException {
    NetworkVoltronEntityMessage message = createMessageWithDescriptor(eid, entityDescriptor, requiresReplication, payload, VoltronEntityMessage.Type.INVOKE_ACTION, acks);
    Trace trace = Trace.newTrace(message, "ClientEntityManagerImpl.invokeActionWithTimeout");
    trace.start();
    long start = System.nanoTime();
    InFlightMessage inFlightMessage = queueInFlightMessage(message, acks, monitor, invokeTimeout, units, shouldBlockGetOnRetire, deferred);
    long timeLeft = units.toNanos(invokeTimeout) - (System.nanoTime() - start);
    if (invokeTimeout == 0) {
      inFlightMessage.waitForAcks();
    } else if (timeLeft > 0) {
      inFlightMessage.waitForAcks(timeLeft, TimeUnit.NANOSECONDS);
    } else {
      throw new TimeoutException(invokeTimeout + " " + units);
    }
    trace.end();
    return inFlightMessage;
  }

  @Override
  public InFlightMessage invokeAction(EntityID eid, EntityDescriptor entityDescriptor, Set<VoltronEntityMessage.Acks> requestedAcks, InFlightMonitor monitor, boolean requiresReplication, boolean shouldBlockGetOnRetire, boolean deferred, byte[] payload) {
    try {
      NetworkVoltronEntityMessage message = createMessageWithDescriptor(eid, entityDescriptor, requiresReplication, payload, VoltronEntityMessage.Type.INVOKE_ACTION, requestedAcks);
      Trace trace = Trace.newTrace(message, "ClientEntityManagerImpl.invokeAction");
      trace.start();
      InFlightMessage inFlightMessage = queueInFlightMessage(message, requestedAcks, monitor, 0L, TimeUnit.MILLISECONDS, shouldBlockGetOnRetire, deferred);
      inFlightMessage.waitForAcks();
      trace.end();
      return inFlightMessage;
    } catch (TimeoutException to) {
      throw new RuntimeException(to);
    }
  }

  @Override
  public Map<String, ?> getStateMap() {
    return Collections.emptyMap();
  }

  @Override
  public void received(TransactionID id) {
    // Note that this call comes the platform, potentially concurrently with complete()/failure().
    InFlightMessage inFlight = inFlightMessages.get(id);
    if (inFlight != null) {
      inFlight.received();
    } else {
   // resend result or stop
    }
  }

  @Override
  public void complete(TransactionID id) {
    // Note that this call comes the platform, potentially concurrently with received().
    complete(id, null);
  }

  @Override
  public void complete(TransactionID id, byte[] value) {
    // Note that this call comes the platform, potentially concurrently with received().
    InFlightMessage inFlight = inFlightMessages.get(id);
    if (inFlight != null) {
      inFlight.setResult(value, null);
    } else {
   // resend result or stop
    }
  }

  @Override
  public void failed(TransactionID id, EntityException error) {
    // Note that this call comes the platform, potentially concurrently with received().
    InFlightMessage inFlight = inFlightMessages.get(id);
    if (inFlight != null) {
      inFlight.setResult(null, error);
    } else {
   // resend result or stop
    }
  }

  @Override
  public void retired(TransactionID id) {
    // We only retire the InFlightMessage from our mapping and release the request ticket once we get the retired ACK.
    InFlightMessage inFlight = inFlightMessages.remove(id);
    if (inFlight != null) {
      inFlight.retired();
      synchronized (this) {
        requestTickets.messageRetired();
        notify();
      }
    } else {
   // resend result or stop
    }
  }

  @Override
  public synchronized void pause() {
    stateManager.pause();
  }

  @Override
  public synchronized void unpause() {
    stateManager.running();
    notifyAll();
  }

  @Override
  public synchronized void initializeHandshake(ClientHandshakeMessage handshakeMessage) {
    // Walk the objectStoreMap and add reconnect references for any objects found there.
    for (EntityClientEndpointImpl<?, ?> endpoint : this.objectStoreMap.values()) {
      EntityDescriptor descriptor = endpoint.getEntityDescriptor();
      EntityID entityID = endpoint.getEntityID();
      long entityVersion = endpoint.getVersion();
      byte[] extendedReconnectData = endpoint.getExtendedReconnectData();
      ClientEntityReferenceContext context = new ClientEntityReferenceContext(entityID, entityVersion, descriptor.getClientInstanceID(), extendedReconnectData);
      handshakeMessage.addReconnectReference(context);
    }
    
    Stage<VoltronEntityResponse> responder = stages.getStage(ClientConfigurationContext.VOLTRON_ENTITY_RESPONSE_STAGE, VoltronEntityResponse.class);
    Stage<VoltronEntityMultiResponse> responderMulti = stages.getStage(ClientConfigurationContext.VOLTRON_ENTITY_MULTI_RESPONSE_STAGE, VoltronEntityMultiResponse.class);
    FlushResponse flush = new FlushResponse();
    responder.getSink().addToSink(flush);
    flush.waitForAccess();
    flush = new FlushResponse();
    responderMulti.getSink().addToSink(flush);
    flush.waitForAccess();
    // Walk the inFlightMessages, adding them all to the handshake, since we need them to be replayed.
    for (InFlightMessage inFlight : this.inFlightMessages.values()) {
      VoltronEntityMessage message = inFlight.getMessage();
//  validate the locking on release and destroy on resends

      ResendVoltronEntityMessage packaged = new ResendVoltronEntityMessage(message.getSource(), message.getTransactionID(), 
          message.getEntityDescriptor(), message.getVoltronType(), message.doesRequireReplication(), message.getExtendedData(), 
          message.getOldestTransactionOnClient());
      handshakeMessage.addResendMessage(packaged);
    }
  }

  @Override
  public void shutdown(boolean fromShutdownHook) {
    synchronized (this) {
      if (this.stateManager.isShutdown()) {
        return;
      } else {
        // not sending anymore, drain the permits
        requestTickets.stop();
        stateManager.stop();
        notifyAll();
      }
    }
    for (InFlightMessage msg : inFlightMessages.values()) {
      throwClosedExceptionOnMessage(msg, "Connection closed under in-flight message");
    }
    // We also want to notify any end-points that they have been disconnected.
    for(EntityClientEndpointImpl<?, ?> endpoint : this.objectStoreMap.values()) {
      try {
        endpoint.didCloseUnexpectedly();
      } catch (Throwable t) {
//  something happened in cleanup.  log and continue
        logger.error("error in shutdown", t);
      }
    }
    this.endpointCloser.shutdownNow(); // ignore the return.  nothing we can do
    // And then drop them.
    this.objectStoreMap.clear();
  }
  
  private void throwClosedExceptionOnMessage(InFlightMessage msg, String description) {
    msg.received();
    // Synthesize the disconnect runtime exception for this message.
    msg.setResult(null, new VoltronWrapperException(new ConnectionClosedException(description)));
    msg.retired();
  }

  private <M extends EntityMessage, R extends EntityResponse> EntityClientEndpoint<M, R> internalLookup(final EntityID entity, long version, final ClientInstanceID instance, final MessageCodec<M, R> codec, final Runnable closeHook) throws EntityException {
    Assert.assertNotNull("Can't lookup null entity descriptor", instance);
    final EntityDescriptor fetchDescriptor = EntityDescriptor.createDescriptorForFetch(entity, version, instance);
    EntityClientEndpointImpl<M, R> resolvedEndpoint = null;
    try {
      byte[] raw = internalRetrieve(fetchDescriptor);
      ByteBuffer br = ByteBuffer.wrap(raw);
      final long fetchID = br.getLong();
      FetchID fetch = new FetchID(fetchID);
      byte[] config = new byte[br.remaining()];
      br.get(config);
      // We can only fail to get the config if we threw an exception.
      Assert.assertTrue(null != raw);
      // We managed to retrieve the config so create the end-point.
      // Note that we will need to call release on this descriptor, when it is closed, prior to running the closeHook we
      //  were given so combine these ideas to pass in one runnable.
      Runnable compoundRunnable = new Runnable() {
        @Override
        public void run() {
          try {
            internalRelease(entity, fetchDescriptor, closeHook);
          } catch (EntityException e) {
            // We aren't expecting there to be any problems releasing an entity in the close hook so we will just log and re-throw.
            Util.printLogAndRethrowError(e, logger);
          }
        }
      };
      resolvedEndpoint = new EntityClientEndpointImpl<M, R>(entity, version, EntityDescriptor.createDescriptorForInvoke(fetch, instance), this, config, codec, compoundRunnable, this.endpointCloser);
      
      if (null != this.objectStoreMap.get(instance)) {
        throw Assert.failure("Attempt to add an object that already exists: Object of class " + resolvedEndpoint.getClass()
                             + " [Identity Hashcode : 0x" + Integer.toHexString(System.identityHashCode(resolvedEndpoint)) + "] ");
      }
      this.objectStoreMap.put(instance, resolvedEndpoint);
    } catch (EntityNotFoundException notfound) {
      throw notfound;
    } catch (EntityException e) {
      // Release the entity and re-throw to the higher level.
      internalRelease(entity, fetchDescriptor, null);
      // NOTE:  Since we are throwing, we are not responsible for calling the given closeHook.
      throw e;
    } catch (Throwable t) {
      // This is the unexpected case so clean up and re-throw as a RuntimeException
      // Clean up any client-side or server-side state regarding this failed connection.
      internalRelease(entity, fetchDescriptor, null);
      // NOTE:  Since we are throwing, we are not responsible for calling the given closeHook.
      throw Throwables.propagate(t);
    }

    return resolvedEndpoint;
  }

  private void internalRelease(EntityID eid, EntityDescriptor entityDescriptor, Runnable closeHook) throws EntityException {
    // We need to provide fully blocking semantics with this call so we will wait for the "COMPLETED" ack.
    Set<VoltronEntityMessage.Acks> requestedAcks = EnumSet.of(VoltronEntityMessage.Acks.COMPLETED);
    // A "RELEASE" doesn't matter to the passive.
    boolean requiresReplication = true;
    byte[] payload = new byte[0];
    NetworkVoltronEntityMessage message = createMessageWithDescriptor(eid, entityDescriptor, requiresReplication, payload, VoltronEntityMessage.Type.RELEASE_ENTITY, requestedAcks);
    sendMessageWhileBusy(message, requestedAcks, "ClientEntityManagerImpl.internalRelease");
    
    // Note that we remove the entity from the local object store only after this release call returns in order to avoid
    // the case where a reconnect might happen before the message completes, thus causing a re-send.  If we don't include
    // this reference in the reconnect handshake, the re-sent release will try to release a non-fetched entity.
    this.objectStoreMap.remove(entityDescriptor.getClientInstanceID());
    
    if (closeHook != null) {
      closeHook.run();
    }
  }
  
  private byte[] sendMessageWhileBusy(NetworkVoltronEntityMessage msg, Set<VoltronEntityMessage.Acks> requestedAcks, final String traceComponentName) throws EntityException {
    Trace trace = Trace.newTrace(msg, traceComponentName);
    trace.start();
    EntityID eid = msg.getEntityDescriptor().getEntityID();
    while (true) {
      try {
        InFlightMessage inflight = queueInFlightMessage(msg, requestedAcks, null, 0L, TimeUnit.MILLISECONDS, false, false);
        inflight.waitForAcks();
        return inflight.get();
      } catch (EntityBusyException busy) {
  //  server was busy, try again in 2 seconds
        wasBusy = true;
        try {
          TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException in) {
          throw new VoltronWrapperException(new EntityServerUncaughtException(eid.getClassName(), eid.getEntityName(), "", in));
        }
        logger.info("Operation delayed:" + msg.getVoltronType() + ", busy wait");
        msg = createMessageWithDescriptor(eid, msg.getEntityDescriptor(), msg.doesRequireReplication(), msg.getExtendedData(), msg.getVoltronType(), requestedAcks);
      } catch (InterruptedException | TimeoutException ie) {
        throw new VoltronWrapperException(new EntityServerUncaughtException(eid.getClassName(), eid.getEntityName(), "", ie));
      } finally {
        trace.end();
      }
    }
  }
  
  private byte[] internalRetrieve(EntityDescriptor entityDescriptor) throws EntityException {
    // We need to provide fully blocking semantics with this call so we will wait for the "COMPLETED" ack.
    Set<VoltronEntityMessage.Acks> requestedAcks = EnumSet.of(VoltronEntityMessage.Acks.COMPLETED);

    boolean requiresReplication = true;
    byte[] payload = new byte[0];
    NetworkVoltronEntityMessage message = createMessageWithDescriptor(entityDescriptor.getEntityID(), entityDescriptor, requiresReplication, payload, VoltronEntityMessage.Type.FETCH_ENTITY, requestedAcks);
    return sendMessageWhileBusy(message, requestedAcks, "ClientEntityManagerImpl.internalRetrieve");
  }

  private InFlightMessage queueInFlightMessage(NetworkVoltronEntityMessage message, Set<VoltronEntityMessage.Acks> requestedAcks, InFlightMonitor monitor, long timeout, TimeUnit units, boolean shouldBlockGetOnRetire, boolean isDeferred) throws TimeoutException {
    InFlightMessage inFlight = new InFlightMessage(message.getEntityID(), message, requestedAcks, monitor, shouldBlockGetOnRetire, isDeferred);
    
    // NOTE:  If we are already stop, the handler in outbound will fail this message for us.
        if(enqueueMessage(inFlight, timeout, units, inFlight.getMessage().getVoltronType() != VoltronEntityMessage.Type.INVOKE_ACTION)) {
          inFlight.sent();
          if (inFlight.send()) {
//  when encountering a send for anything other than an invoke, wait here before sending anything else
//  this is a bit paranoid but it is to prevent too many resends of lifecycle operations.  Just
//  make sure those complete before sending any new invokes or lifecycle messages
            if (inFlight.getMessage().getVoltronType() != VoltronEntityMessage.Type.INVOKE_ACTION) {
              inFlight.waitForAcks();
            }
          } else {
            logger.debug("message not sent.  Make sure resend happens " + inFlight);
          }
        } else {
          throwClosedExceptionOnMessage(inFlight, "Connection closed before sending message");
        }
        return inFlight;
  }

  private NetworkVoltronEntityMessage createMessageWithoutClientInstance(EntityID entityID, long version, boolean requiresReplication, byte[] config, VoltronEntityMessage.Type type, Set<VoltronEntityMessage.Acks> acks) {
    // We have no client instance for a create but the request currently requires a full descriptor.
    EntityDescriptor entityDescriptor = EntityDescriptor.createDescriptorForLifecycle(entityID, version);
    return createMessageWithDescriptor(entityID, entityDescriptor, requiresReplication, config, type, acks);
  }

  private NetworkVoltronEntityMessage createMessageWithDescriptor(EntityID entityID, EntityDescriptor entityDescriptor, boolean requiresReplication, byte[] config, VoltronEntityMessage.Type type, Set<VoltronEntityMessage.Acks> acks) {
    // Get the clientID for our channel.
    ClientID clientID = this.channel.getClientID();
    // Get the next transaction ID.
    TransactionID transactionID = new TransactionID(currentTransactionID.incrementAndGet());
    // Figure out the "trailing edge" of the current progress through the transaction stream.
    TransactionID oldestTransactionPending = transactionID;
    // if reconnectable, discover the oldest transaction still being waited for
    if (this.channel.getProductId().isReconnectEnabled()) {
      for (TransactionID pendingID : this.inFlightMessages.keySet()) {
        if (oldestTransactionPending.compareTo(pendingID) > 0) {
          // pendingID is earlier than oldestTransactionPending.
          oldestTransactionPending = pendingID;
        }
      }
    }
    // Create the message and populate it.
    NetworkVoltronEntityMessage message = (NetworkVoltronEntityMessage) channel.createMessage(TCMessageType.VOLTRON_ENTITY_MESSAGE);
    message.setContents(clientID, transactionID, entityID, entityDescriptor, type, requiresReplication, config, oldestTransactionPending, acks);
    return message;
  }
  
  private static class FlushResponse implements VoltronEntityResponse, VoltronEntityMultiResponse {
    private boolean accessed = false;
    
    @Override
    public synchronized TransactionID getTransactionID() {
      notifyAll();
      accessed = true;
      return TransactionID.NULL_ID;
    }

    @Override
    public VoltronEntityMessage.Acks getAckType() {
      return VoltronEntityMessage.Acks.RECEIVED;
    }

    @Override
    public TCMessageType getMessageType() {
      return TCMessageType.VOLTRON_ENTITY_RECEIVED_RESPONSE;
    }

    @Override
    public void hydrate() throws IOException, UnknownNameException {

    }

    @Override
    public void dehydrate() {

    }

    @Override
    public synchronized int replay(ReplayReceiver receiver) {
      notifyAll();
      accessed = true;
      return 0;
    }
    
    @Override
    public boolean send() {
      return true;
    }

    @Override
    public MessageChannel getChannel() {
      return null;
    }

    @Override
    public NodeID getSourceNodeID() {
      return ServerID.NULL_ID;
    }

    @Override
    public NodeID getDestinationNodeID() {
      return ClientID.NULL_ID;
    }

    @Override
    public SessionID getLocalSessionID() {
      return SessionID.NULL_ID;
    }

    @Override
    public int getTotalLength() {
      return 0;
    }

    @Override
    public boolean addReceived(TransactionID tid) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean addRetired(TransactionID tid) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean addResult(TransactionID tid, byte[] result) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean addResultAndRetire(TransactionID tid, byte[] result) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean addServerMessage(TransactionID cid, byte[] message) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean addServerMessage(ClientInstanceID cid, byte[] message) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void stopAdding() {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    public synchronized void waitForAccess() {
      boolean interrupted = false;
      while (!accessed) {
        try {
          this.wait();
        } catch (InterruptedException ie) {
          interrupted = true;
        }
      }
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }
  
  private static class MessagePendingCount {
    private int messagesPending = ClientConfigurationContext.MAX_PENDING_REQUESTS;
  
    // synchronized by caller
    private int messagePending() {
      Assert.assertTrue(messagesPending > 0);
      return --messagesPending;
    }

    // synchronized by caller
    private int messageRetired() {
      Assert.assertTrue(messagesPending < ClientConfigurationContext.MAX_PENDING_REQUESTS);
      return ++messagesPending;
    }
    
    private boolean messagePendingSlotAvailable() {
      return messagesPending > 0;
    }
    
    private void stop() {
      messagesPending = 0;
    }
  }
}
