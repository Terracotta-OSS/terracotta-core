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

import com.tc.exception.EntityReferencedException;
import com.tc.exception.WrappedEntityException;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.bytes.TCByteBufferFactory;
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
import com.tc.text.MapListPrettyPrint;
import com.tc.text.PrettyPrintable;
import com.tc.util.Assert;
import com.tc.util.Util;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityServerUncaughtException;


public class ClientEntityManagerImpl implements ClientEntityManager {
  private final Logger logger;

  private final ClientMessageChannel channel;
  private final ConcurrentMap<TransactionID, InFlightMessage> inFlightMessages;
  private final StoppableSemaphore requestTickets = new StoppableSemaphore(ClientConfigurationContext.MAX_PENDING_REQUESTS);
  private final TransactionSource transactionSource;

  private final ClientEntityStateManager stateManager;
  private final ConcurrentMap<ClientInstanceID, EntityClientEndpointImpl<?, ?>> objectStoreMap;
    
  private final StageManager stages;
  
  private final ExecutorService endpointCloser = Executors.newWorkStealingPool();
//  for testing
  private boolean wasBusy = false;
  
  private final LongAdder msgCount = new LongAdder();
  private final LongAdder inflights = new LongAdder();
  private final LongAdder addWindow = new LongAdder();
  
  public ClientEntityManagerImpl(ClientMessageChannel channel, StageManager mgr) {
    this.logger = new ClientIDLogger(channel::getClientID, LoggerFactory.getLogger(ClientEntityManager.class));
    
    this.channel = channel;

    this.inFlightMessages = new ConcurrentHashMap<>();
    this.transactionSource = new TransactionSource();
    this.stateManager = new ClientEntityStateManager();
    this.objectStoreMap = new ConcurrentHashMap<>(10240, 0.75f, 128);
    this.stages = mgr;
  }

  @Override
  public synchronized boolean isValid() {
    return !stateManager.isShutdown() && channel.isOpen();
  }

  private synchronized boolean waitUntilRunning(long timeout, long end) {
    boolean interrupted = Thread.interrupted();
    while (!stateManager.isRunning()) {
      try {
        if (stateManager.isShutdown()) {
          break;
        } else if (timeout == 0) {
          wait();
        } else {
          long timing = end - System.nanoTime();
          wait(timing / TimeUnit.MILLISECONDS.toNanos(1), (int) (timing % TimeUnit.MILLISECONDS.toNanos(1)));
        }
      } catch (InterruptedException e) {
        interrupted = true;
      }
    }
    return interrupted;
  }

  private boolean enqueueMessage(InFlightMessage msg) throws RejectedExecutionException {
    if (!this.stateManager.isRunning()) {
      return false;
    }
    if (requestTickets.tryAcquire()) {
      inFlightMessages.put(msg.getTransactionID(), msg);
      return true;
    } else {
      throw new RejectedExecutionException("Output queue is full");
    }
  }

  private boolean enqueueMessage(InFlightMessage msg, long timeout, TimeUnit unit, boolean waitUntilRunning) throws TimeoutException {
    boolean interrupted = Thread.interrupted();
    try {
      long end = 0;
      if (timeout < 0) {
        throw new IllegalArgumentException("timeout must be >= 0");
      } else if (timeout > 0) {
        end = System.nanoTime() + unit.toNanos(timeout);
      }

      if (waitUntilRunning) {
        interrupted |= waitUntilRunning(timeout, end);
      }

      // stop drains the permits so even if asked to not waitUntilRunning, stop is still checked

      while (!isShutdown()) {
        try {
          if (timeout == 0) {
            requestTickets.acquire();
          } else if (!requestTickets.tryAcquire(end - System.nanoTime(), TimeUnit.NANOSECONDS)) {
            throw new TimeoutException();
          }
          inFlightMessages.put(msg.getTransactionID(), msg);
          return true;
        } catch (InterruptedException e) {
          interrupted = true;
        }
      }
      return false;
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private synchronized boolean isRunning() {
    return stateManager.isRunning();
  }

  private synchronized boolean isShutdown() {
    return stateManager.isShutdown();
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
      logger.info("transaction " + tid + " not found. Ignoring message.");
    }
  }

  @Override
  public void handleStatistics(TransactionID tid, long[] message) {
    InFlightMessage msg = this.inFlightMessages.get(tid);
    if (msg != null) {
      msg.addServerStatistics(message);
    } else {
      addWindow.add(message[0]);
      if (message[0] > TimeUnit.MILLISECONDS.toNanos(500)) {
        logger.debug("add window " + message[0]);
      }
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

    return sendMessageWhileBusy(entityID, ()->createMessageWithoutClientInstance(entityID, version, requiresReplication, config, VoltronEntityMessage.Type.CREATE_ENTITY, lifecycleAcks()),
            lifecycleAcks(), "ClientEntityManagerImpl.createEntity");
  }

  @Override
  public byte[] reconfigureEntity(EntityID entityID, long version, byte[] config) throws EntityException {
    // A create needs to be replicated.
    boolean requiresReplication = true;
    return sendMessageWhileBusy(entityID, ()->createMessageWithoutClientInstance(entityID, version, requiresReplication, config, VoltronEntityMessage.Type.RECONFIGURE_ENTITY, lifecycleAcks()), lifecycleAcks(), "ClientEntityManagerImpl.reconfigureEntity");
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
    try {
      //  don't care about the return
      sendMessageWhileBusy(entityID, ()->createMessageWithoutClientInstance(entityID, version, requiresReplication, emtpyExtendedData, VoltronEntityMessage.Type.DESTROY_ENTITY, lifecycleAcks()), lifecycleAcks(), "ClientEntityManagerImpl.destroyEntity");
    } catch (EntityReferencedException r) {
      return false;
    }
    return true;
  }

  @Override
  public InFlightMessage invokeActionWithTimeout(EntityID eid, EntityDescriptor entityDescriptor,
          Set<VoltronEntityMessage.Acks> acks, InFlightMonitor monitor, boolean requiresReplication,
          boolean shouldBlockGetOnRetire, long invokeTimeout, TimeUnit units, byte[] payload) throws InterruptedException, TimeoutException {
    long start = System.nanoTime();
    InFlightMessage inFlightMessage = queueInFlightMessage(eid, ()->createMessageWithDescriptor(eid, entityDescriptor, requiresReplication, payload, VoltronEntityMessage.Type.INVOKE_ACTION, makeServerAcks(shouldBlockGetOnRetire, acks)),
            acks, monitor, invokeTimeout, units, shouldBlockGetOnRetire);
    long timeLeft = units.toNanos(invokeTimeout) - (System.nanoTime() - start);
    if (invokeTimeout == 0) {
      inFlightMessage.waitForAcks();
    } else if (timeLeft > 0) {
      inFlightMessage.waitForAcks(timeLeft, TimeUnit.NANOSECONDS);
    } else {
      throw new TimeoutException(invokeTimeout + " " + units);
    }
    return inFlightMessage;
  }
  // add the retired ack to the message if block on retired is requested, this will increase
  // message efficiency by grouping the retired message with the result.
  private Set<VoltronEntityMessage.Acks> makeServerAcks(boolean blockOnRetire, Set<VoltronEntityMessage.Acks> requestedAcks) {
    Set<VoltronEntityMessage.Acks> serverAcks = requestedAcks;
    if (blockOnRetire) {
      serverAcks = EnumSet.copyOf(requestedAcks);
      serverAcks.add(VoltronEntityMessage.Acks.RETIRED);
    }
    return serverAcks;
  }

  @Override
  public InFlightMessage invokeAction(EntityID eid, EntityDescriptor entityDescriptor, Set<VoltronEntityMessage.Acks> requestedAcks,
          InFlightMonitor monitor, boolean requiresReplication, boolean shouldBlockGetOnRetire, byte[] payload) {
    try {
    InFlightMessage inFlightMessage = queueInFlightMessage(eid,
            ()->createMessageWithDescriptor(eid, entityDescriptor, requiresReplication, payload, VoltronEntityMessage.Type.INVOKE_ACTION, makeServerAcks(shouldBlockGetOnRetire, requestedAcks)),
            requestedAcks, monitor, 0L, TimeUnit.MILLISECONDS, shouldBlockGetOnRetire);
      inFlightMessage.waitForAcks();
      return inFlightMessage;
    } catch (TimeoutException to) {
      throw new RuntimeException(to);
    }
  }

  @Override
  public void asyncInvokeAction(EntityID eid, EntityDescriptor entityDescriptor, Set<VoltronEntityMessage.Acks> requestedAcks, InFlightMonitor monitor, boolean requiresReplication, byte[] payload, long timeout, TimeUnit unit) throws RejectedExecutionException {
    if (unit == null) {
      asyncQueueInFlightMessage(eid,
          () -> createMessageWithDescriptor(eid, entityDescriptor, requiresReplication, payload, VoltronEntityMessage.Type.INVOKE_ACTION, requestedAcks),
          requestedAcks, monitor);
    } else {
      try {
        queueInFlightMessage(eid,
            ()->createMessageWithDescriptor(eid, entityDescriptor, requiresReplication, payload, VoltronEntityMessage.Type.INVOKE_ACTION, requestedAcks),
            requestedAcks, monitor, timeout, unit, false, true);
      } catch (TimeoutException te) {
        throw new RejectedExecutionException(te);
      }
    }
  }

  private void asyncQueueInFlightMessage(EntityID eid, Supplier<NetworkVoltronEntityMessage> message, Set<VoltronEntityMessage.Acks> requestedAcks, InFlightMonitor monitor) throws RejectedExecutionException {
    boolean queued;
    InFlightMessage inFlight = new InFlightMessage(eid, message, requestedAcks, monitor, false, true);
    try {
      msgCount.increment();
      inflights.add(ClientConfigurationContext.MAX_PENDING_REQUESTS - requestTickets.availablePermits());
      queued = enqueueMessage(inFlight);
    } catch (Throwable t) {
      transactionSource.retire(inFlight.getTransactionID());
      throw t;
    }

    if (queued && !stateManager.isShutdown()) {
      inFlight.sent();
      if (!inFlight.send()) {
        logger.debug("message not sent.  Make sure resend happens : {}", inFlight);
        if (!this.channel.getProductID().isReconnectEnabled()) {
          throwClosedExceptionOnMessage(inFlight, "connection closed");
        }
      }
    } else {
      throwClosedExceptionOnMessage(inFlight, "Connection closed before sending message");
    }
  }

  @Override
  public Map<String, ?> getStateMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    for (EntityClientEndpointImpl<?,?> s : objectStoreMap.values()) {
      map.put(s.getEntityID().toString(), s.getStatistics().getStateMap());
    }
    map.put("messagesOut", msgCount.sum());
    if (msgCount.sum() > 0) {
      map.put("averagePending", inflights.sum()/msgCount.sum());
      map.put("averageServerWindow", addWindow.sum()/msgCount.sum());
    }
    Object stats = this.channel.getAttachment("ChannelStats");
    Map<String, Object> sub = new LinkedHashMap<>();
    sub.put("connection", channel.getConnectionID());
    sub.put("local", channel.getLocalAddress());
    sub.put("remote", channel.getRemoteAddress());
    sub.put("product", channel.getProductID());
    sub.put("client", channel.getClientID());
    if (stateManager.isShutdown()) {
      sub.put("pendingMessages", "<shutdown>");
    } else {
      sub.put("pendingMessages", ClientConfigurationContext.MAX_PENDING_REQUESTS - this.requestTickets.availablePermits());
    }
    map.put("channel", sub);
    if (stats instanceof PrettyPrintable) {
      sub.put("stats", ((PrettyPrintable)stats).getStateMap());
    }
    return map;
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
  public void failed(TransactionID id, Exception error) {
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
    try {
      InFlightMessage inFlight = inFlightMessages.remove(id);
      if (inFlight != null) {
        inFlight.retired();
        requestTickets.release();
      } else {
        // resend result or stop
      }
    } finally {
      transactionSource.retire(id);
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
    
    Stage<VoltronEntityMultiResponse> responderMulti = stages.getStage(ClientConfigurationContext.VOLTRON_ENTITY_MULTI_RESPONSE_STAGE, VoltronEntityMultiResponse.class);
    if (!responderMulti.isEmpty()) {
      FlushResponse flush = new FlushResponse();
      responderMulti.getSink().addToSink(flush);
      flush.waitForAccess();
    }
    // Walk the inFlightMessages, adding them all to the handshake, since we need them to be replayed.
    for (InFlightMessage inFlight : this.inFlightMessages.values()) {
      VoltronEntityMessage message = inFlight.getMessage();
//  validate the locking on release and destroy on resends

      ResendVoltronEntityMessage packaged = new ResendVoltronEntityMessage(message.getSource(), message.getTransactionID(),
          message.getEntityDescriptor(), message.getVoltronType(), message.doesRequireReplication(), message.getExtendedData());
      handshakeMessage.addResendMessage(packaged);
    }
  }

  @Override
  public void shutdown() {
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
    if (logger.isDebugEnabled()) {
      MapListPrettyPrint print = new MapListPrettyPrint();
      this.prettyPrint(print);
      logger.debug(print.toString());
    }
    this.objectStoreMap.clear();
  }
  
  private void throwClosedExceptionOnMessage(InFlightMessage msg, String description) {
    msg.received();
    // Synthesize the disconnect runtime exception for this message.
    ConnectionClosedException closed = new ConnectionClosedException(msg.getEntityID().getClassName(), msg.getEntityID().getEntityName(), description, false, null);
    msg.setResult(null, closed);
    msg.retired();
    // may or may not be there.
    inFlightMessages.remove(msg.getTransactionID());
    transactionSource.retire(msg.getTransactionID());
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
    sendMessageWhileBusy(eid, ()->createMessageWithDescriptor(eid, entityDescriptor, requiresReplication, payload, VoltronEntityMessage.Type.RELEASE_ENTITY, requestedAcks),
            requestedAcks, "ClientEntityManagerImpl.internalRelease");
    
    // Note that we remove the entity from the local object store only after this release call returns in order to avoid
    // the case where a reconnect might happen before the message completes, thus causing a re-send.  If we don't include
    // this reference in the reconnect handshake, the re-sent release will try to release a non-fetched entity.
    EntityClientEndpointImpl<?, ?> ref = this.objectStoreMap.remove(entityDescriptor.getClientInstanceID());
    if (ref != null && logger.isDebugEnabled()) {
      MapListPrettyPrint print = new MapListPrettyPrint();
      ref.getStatistics().prettyPrint(print);
      logger.debug("Releasing " + ref.getEntityID() + "=" + print.toString());
    }
    if (closeHook != null) {
      closeHook.run();
    }
  }
  
  private byte[] sendMessageWhileBusy(EntityID eid, Supplier<NetworkVoltronEntityMessage> msg, Set<VoltronEntityMessage.Acks> requestedAcks, final String traceComponentName) throws EntityException {
    while (true) {
      try {
        InFlightMessage inflight = queueInFlightMessage(eid, msg, requestedAcks, null, 0L, TimeUnit.MILLISECONDS, false);
        inflight.waitForAcks();
        return inflight.get();
      } catch (EntityBusyException busy) {
  //  server was busy, try again in 2 seconds
        wasBusy = true;
        logger.info("Cluster is busy. Requested operation will be retried in 2 seconds");
        try {
          TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException in) {
          throw new WrappedEntityException(new EntityServerUncaughtException(eid.getClassName(), eid.getEntityName(), "", in));
        }
      } catch (InterruptedException | TimeoutException ie) {
        throw new WrappedEntityException(new EntityServerUncaughtException(eid.getClassName(), eid.getEntityName(), "", ie));
      } 
    }
  }
  
  private byte[] internalRetrieve(EntityDescriptor entityDescriptor) throws EntityException {
    // We need to provide fully blocking semantics with this call so we will wait for the "COMPLETED" ack.
    Set<VoltronEntityMessage.Acks> requestedAcks = EnumSet.of(VoltronEntityMessage.Acks.COMPLETED);

    boolean requiresReplication = true;
    byte[] payload = new byte[0];
    return sendMessageWhileBusy(entityDescriptor.getEntityID(), ()->createMessageWithDescriptor(entityDescriptor.getEntityID(), entityDescriptor, requiresReplication, payload, VoltronEntityMessage.Type.FETCH_ENTITY, requestedAcks),
            requestedAcks, "ClientEntityManagerImpl.internalRetrieve");
  }

  private InFlightMessage queueInFlightMessage(EntityID eid, Supplier<NetworkVoltronEntityMessage> message, Set<VoltronEntityMessage.Acks> requestedAcks, InFlightMonitor monitor, long timeout, TimeUnit units, boolean shouldBlockGetOnRetire) throws TimeoutException {
    return queueInFlightMessage(eid, message, requestedAcks, monitor, timeout, units, shouldBlockGetOnRetire, false);
  }

  private InFlightMessage queueInFlightMessage(EntityID eid, Supplier<NetworkVoltronEntityMessage> message, Set<VoltronEntityMessage.Acks> requestedAcks, InFlightMonitor monitor, long timeout, TimeUnit units, boolean shouldBlockGetOnRetire, boolean asyncMode) throws TimeoutException {
    boolean queued;
    InFlightMessage inFlight = new InFlightMessage(eid, message, requestedAcks, monitor, shouldBlockGetOnRetire, asyncMode);
    try {
      msgCount.increment();
      inflights.add(ClientConfigurationContext.MAX_PENDING_REQUESTS - requestTickets.availablePermits());
      // NOTE:  If we are already stop, the handler in outbound will fail this message for us.
      queued = enqueueMessage(inFlight, timeout, units, inFlight.getMessage().getVoltronType() != VoltronEntityMessage.Type.INVOKE_ACTION);
    } catch (Throwable t) {
      transactionSource.retire(inFlight.getTransactionID());
      throw t;
    }

    if (queued && !stateManager.isShutdown()) {
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
        if (!this.channel.getProductID().isReconnectEnabled()) {
          throwClosedExceptionOnMessage(inFlight, "connection not capable of resend");
        }
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
    NetworkVoltronEntityMessage message = (NetworkVoltronEntityMessage) channel.createMessage(TCMessageType.VOLTRON_ENTITY_MESSAGE);
    ClientID clientID = this.channel.getClientID();
    TransactionID transactionID = transactionSource.create();
    TransactionID oldestTransactionPending = transactionSource.oldest();//either premature retirement or late (or missing) removal from inflight
    message.setContents(clientID, transactionID, entityID, entityDescriptor, type, requiresReplication, TCByteBufferFactory.wrap(config), oldestTransactionPending, acks);
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
    public boolean addStats(TransactionID cid, long[] timings) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void stopAdding() {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean startAdding() {
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
  
  private static class StoppableSemaphore extends Semaphore {

    private final int permitCount;

    public StoppableSemaphore(int permitCount) {
      super(permitCount);
      this.permitCount = permitCount;
    }

    private void stop() {
      reducePermits(permitCount);
    }
  }
}
