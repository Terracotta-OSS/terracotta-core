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

import com.tc.bytes.TCByteBufferFactory;
import com.tc.exception.EntityBusyException;
import com.tc.exception.EntityReferencedException;
import com.tc.exception.WrappedEntityException;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.util.Throwables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.Invocation;
import org.terracotta.entity.InvocationCallback;
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

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityServerUncaughtException;

import static com.tc.object.EntityDescriptor.createDescriptorForLifecycle;
import static com.tc.object.SafeInvocationCallback.safe;
import static java.util.stream.Collectors.toCollection;
import static org.terracotta.entity.Invocation.uninterruptiblyGet;


public class ClientEntityManagerImpl implements ClientEntityManager {
  private final Logger logger;

  private final ClientMessageChannel channel;
  private final ConcurrentMap<TransactionID, InFlightMessage> inFlightMessages;
  private final TransactionSource transactionSource;

  private final ClientEntityStateManager stateManager;
  private final ConcurrentMap<ClientInstanceID, EntityClientEndpointImpl<?, ?>> objectStoreMap;

  private final ExecutorService endpointCloser = Executors.newWorkStealingPool();

  private final LongAdder msgCount = new LongAdder();
  private final LongAdder inflights = new LongAdder();
  private final LongAdder addWindow = new LongAdder();

  public ClientEntityManagerImpl(ClientMessageChannel channel) {
    this.channel = channel;
    this.logger = new ClientIDLogger(() -> channel.getClientID(), LoggerFactory.getLogger(ClientEntityManager.class));
    this.inFlightMessages = new ConcurrentHashMap<>();
    this.transactionSource = new TransactionSource();
    this.stateManager = new ClientEntityStateManager();
    this.objectStoreMap = new ConcurrentHashMap<>(10240, 0.75f, 128);
  }

  @Override
  public synchronized boolean isValid() {
    return !stateManager.isShutdown() && channel.isOpen();
  }

  private boolean enqueueMessage(InFlightMessage msg) throws RejectedExecutionException {
    if (this.stateManager.isShutdown()) {
      return false;
    } else {
      inFlightMessages.put(msg.getTransactionID(), msg);
      return true;
    }
  }

  @SuppressWarnings("rawtypes")
  @Override
  public EntityClientEndpoint fetchEntity(EntityID entity, long version, ClientInstanceID instance, MessageCodec<? extends EntityMessage, ? extends EntityResponse> codec) throws EntityException {
    return internalLookup(entity, version, instance, codec);
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
    return lifecycleAndRetire(entityID, version, VoltronEntityMessage.Type.CREATE_ENTITY, config);
  }

  @Override
  public byte[] reconfigureEntity(EntityID entityID, long version, byte[] config) throws EntityException {
    return lifecycleAndRetire(entityID, version, VoltronEntityMessage.Type.RECONFIGURE_ENTITY, config);
  }

  private byte[] lifecycleAndComplete(EntityID entityId, EntityDescriptor entityDescriptor, VoltronEntityMessage.Type type) throws EntityException {
    return retryingWhileBusy(entityId, () -> uninterruptiblyGet(
            lifecycle(entityId, entityDescriptor, type, new byte[0]).invoke(),
            EntityException.class));
  }

  private byte[] lifecycleAndRetire(EntityID entityId, long version, VoltronEntityMessage.Type type, byte[] message) throws EntityException {
    return retryingWhileBusy(entityId, () -> {
      Invocation<byte[]> builder = lifecycle(entityId, createDescriptorForLifecycle(entityId, version), type, message);
      return uninterruptiblyGet(builder.invokeAndRetire(), EntityException.class);
    });
  }

  private Invocation<byte[]> lifecycle(EntityID entityID, EntityDescriptor entityDescriptor, VoltronEntityMessage.Type type, byte[] message) {
    return (callback, callbacks) -> ClientEntityManagerImpl.this.invoke(entityID, entityDescriptor, callbacks, safe(callback), true, type, message);
  }

  @Override
  public boolean destroyEntity(EntityID entityID, long version) throws EntityException {
    try {
      lifecycleAndRetire(entityID, version, VoltronEntityMessage.Type.DESTROY_ENTITY, new byte[0]);
      return true;
    } catch (EntityReferencedException r) {
      return false;
    }
  }

  private Set<VoltronEntityMessage.Acks> makeServerAcks(Set<InvocationCallback.Types> requestedCallbacks) {
    return requestedCallbacks.stream().map(callback -> {
      switch (callback) {
        case SENT:
          return VoltronEntityMessage.Acks.SENT;
        case RECEIVED:
          return VoltronEntityMessage.Acks.RECEIVED;
        case COMPLETE:
          return VoltronEntityMessage.Acks.COMPLETED;
        case RETIRED:
          return VoltronEntityMessage.Acks.RETIRED;
        default:
          return null;
      }
    }).filter(Objects::nonNull).collect(toCollection(() -> EnumSet.noneOf(VoltronEntityMessage.Acks.class)));
  }

  @Override
  public Invocation.Task invokeAction(EntityID eid, EntityDescriptor entityDescriptor, Set<InvocationCallback.Types> requestedCallbacks,
                                      SafeInvocationCallback<byte[]> callback, boolean requiresReplication, byte[] payload) {
    return invoke(eid, entityDescriptor, requestedCallbacks, callback, requiresReplication, VoltronEntityMessage.Type.INVOKE_ACTION, payload);
  }

  private Invocation.Task invoke(EntityID eid, EntityDescriptor entityDescriptor, Set<InvocationCallback.Types> requestedCallbacks,
                                 SafeInvocationCallback<byte[]> callback, boolean requiresReplication, VoltronEntityMessage.Type type, byte[] payload) {
    Set<VoltronEntityMessage.Acks> requestedAcks = makeServerAcks(requestedCallbacks);
    return queueInFlightMessage(eid, () -> createMessageWithDescriptor(eid, entityDescriptor, requiresReplication, payload, type, requestedAcks), callback);
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

    Object stats = channel.getAttachment("ChannelStats");
    Map<String, Object> sub = new LinkedHashMap<>();
    sub.put("connection", channel.getConnectionID());
    sub.put("local", channel.getLocalAddress());
    sub.put("remote", channel.getRemoteAddress());
    sub.put("product", channel.getProductID());
    sub.put("client", channel.getClientID());
    if (stateManager.isShutdown()) {
      sub.put("pendingMessages", "<shutdown>");
    } else {
      sub.put("pendingMessages", inFlightMessages.size());
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
    
    // Walk the inFlightMessages, adding them all to the handshake, since we need them to be replayed.
    for (InFlightMessage inFlight : this.inFlightMessages.values()) {
      if (inFlight.commit()) {
        VoltronEntityMessage message = inFlight.getMessage();
        //  validate the locking on release and destroy on resends

        ResendVoltronEntityMessage packaged = new ResendVoltronEntityMessage(message.getSource(), message.getTransactionID(),
                message.getEntityDescriptor(), message.getVoltronType(), message.doesRequireReplication(), message.getExtendedData());
        handshakeMessage.addResendMessage(packaged);
      }
    }
  }

  @Override
  public void shutdown() {
    synchronized (this) {
      if (this.stateManager.isShutdown()) {
        return;
      } else {
        // not sending anymore, drain the permits
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

  private <M extends EntityMessage, R extends EntityResponse> EntityClientEndpoint<M, R> internalLookup(final EntityID entity, long version, final ClientInstanceID instance, final MessageCodec<M, R> codec) throws EntityException {
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
            internalRelease(entity, fetchDescriptor);
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
      internalRelease(entity, fetchDescriptor);
      // NOTE:  Since we are throwing, we are not responsible for calling the given closeHook.
      throw e;
    } catch (Throwable t) {
      // This is the unexpected case so clean up and re-throw as a RuntimeException
      // Clean up any client-side or server-side state regarding this failed connection.
      internalRelease(entity, fetchDescriptor);
      // NOTE:  Since we are throwing, we are not responsible for calling the given closeHook.
      throw Throwables.propagate(t);
    }

    return resolvedEndpoint;
  }

  private void internalRelease(EntityID entityId, EntityDescriptor entityDescriptor) throws EntityException {
    // We need to provide fully blocking semantics with this call so we will wait for the "COMPLETED" ack.
    lifecycleAndComplete(entityId, entityDescriptor, VoltronEntityMessage.Type.RELEASE_ENTITY);

    // Note that we remove the entity from the local object store only after this release call returns in order to avoid
    // the case where a reconnect might happen before the message completes, thus causing a re-send.  If we don't include
    // this reference in the reconnect handshake, the re-sent release will try to release a non-fetched entity.
    EntityClientEndpointImpl<?, ?> ref = this.objectStoreMap.remove(entityDescriptor.getClientInstanceID());
    if (ref != null && logger.isDebugEnabled()) {
      MapListPrettyPrint print = new MapListPrettyPrint();
      ref.getStatistics().prettyPrint(print);
      logger.debug("Releasing " + ref.getEntityID() + "=" + print.toString());
    }
  }
  
  private byte[] internalRetrieve(EntityDescriptor entityDescriptor) throws EntityException {
    // We need to provide fully blocking semantics with this call so we will wait for the "COMPLETED" ack.
    return lifecycleAndComplete(entityDescriptor.getEntityID(), entityDescriptor, VoltronEntityMessage.Type.FETCH_ENTITY);
  }

  private Invocation.Task queueInFlightMessage(EntityID eid, Supplier<NetworkVoltronEntityMessage> message, SafeInvocationCallback<byte[]> callback) {
    boolean queued;
    try {
      InFlightMessage inFlight = new InFlightMessage(eid, message, callback);
      try {
        msgCount.increment();
        inflights.add(inFlightMessages.size());
        // NOTE:  If we are already stop, the handler in outbound will fail this message for us.
        queued = enqueueMessage(inFlight);
      } catch (Throwable t) {
        transactionSource.retire(inFlight.getTransactionID());
        throw t;
      }

      if (queued && !stateManager.isShutdown()) {
        inFlight.sent();
        if (!inFlight.send()) {
          logger.debug("message not sent.  Make sure resend happens " + inFlight);
          if (!channel.getProductID().isReconnectEnabled()) {
            throwClosedExceptionOnMessage(inFlight, "connection not capable of resend");
          }
        }
      } else {
        throwClosedExceptionOnMessage(inFlight, "Connection closed before sending message");
      }
      return () -> {
        if (inFlight.cancel()) {
          inFlightMessages.remove(inFlight.getTransactionID(), inFlight);
          return true;
        } else {
          return false;
        }
      };
    } catch (ConnectionClosedException e) {
      callback.sent();
      callback.failure(e);
      callback.complete();
      callback.retired();
      return () -> false;
    }
  }

  private NetworkVoltronEntityMessage createMessageWithoutClientInstance(EntityID entityID, long version, boolean requiresReplication, byte[] config, VoltronEntityMessage.Type type, Set<VoltronEntityMessage.Acks> acks) {
    // We have no client instance for a create but the request currently requires a full descriptor.
    EntityDescriptor entityDescriptor = createDescriptorForLifecycle(entityID, version);
    return createMessageWithDescriptor(entityID, entityDescriptor, requiresReplication, config, type, acks);
  }

  private NetworkVoltronEntityMessage createMessageWithDescriptor(EntityID entityID, EntityDescriptor entityDescriptor, boolean requiresReplication, byte[] config, VoltronEntityMessage.Type type, Set<VoltronEntityMessage.Acks> acks) {
    NetworkVoltronEntityMessage message = (NetworkVoltronEntityMessage) channel.createMessage(TCMessageType.VOLTRON_ENTITY_MESSAGE);
    ClientID clientID = channel.getClientID();
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
    public synchronized int replay(ReplayReceiver receiver) {
      notifyAll();
      accessed = true;
      return 0;
    }
    
    @Override
    public TCNetworkMessage send() {
      return null;
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
    public int getMessageLength() {
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
      boolean interrupted = Thread.interrupted();
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
  private <T> T retryingWhileBusy(EntityID entityID, EntityLifecycleTask<T> task) throws EntityException {
    while (true) {
      try {
        return task.run();
      } catch (EntityBusyException busy) {
        logger.info("Cluster is busy. Requested operation will be retried in 2 seconds");
        try {
          TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException in) {
          throw new WrappedEntityException(new EntityServerUncaughtException(entityID.getClassName(), entityID.getEntityName(), "", in));
        }
      }
    }
  }

  private interface EntityLifecycleTask<T> {

    T run() throws EntityException;
  }
}
