/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import org.terracotta.entity.EntityClientEndpoint;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Futures;
import com.tc.entity.NetworkVoltronEntityMessage;
import com.tc.entity.ResendVoltronEntityMessage;
import com.tc.entity.VoltronEntityMessage;
import com.tc.exception.PlatformRejoinException;
import com.tc.exception.TCNotRunningException;
import com.tc.exception.TCObjectNotFoundException;
import com.tc.logging.ClientIDLogger;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.ClientEntityReferenceContext;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.tx.TransactionID;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.Util;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;


public class ClientEntityManagerImpl implements ClientEntityManager {
  private static final int MAX_PENDING_REQUESTS = 5000;
  private static final int MAX_QUEUED_REQUESTS = 100;
  
  private static enum State {
    PAUSED, RUNNING, REJOIN_IN_PROGRESS, STARTING, STOPPED
  }
  
  private final TCLogger logger;
  
  private final ClientMessageChannel channel;
  private final Thread sender;
  private final ConcurrentMap<TransactionID, InFlightMessage> inFlightMessages;
  private final BlockingQueue<InFlightMessage> outbound;
  private final Semaphore requestTickets;
  private final AtomicLong currentTransactionID;
  
  private volatile State state;
  private final ConcurrentMap<EntityDescriptor, EntityClientEndpoint> objectStoreMap;

  
  public ClientEntityManagerImpl(ClientMessageChannel channel) {
    this.logger = new ClientIDLogger(channel, TCLogging.getLogger(ClientEntityManager.class));
    
    this.channel = channel;
    this.sender = new Thread(this::sendLoop);
    this.sender.setDaemon(true);
    this.inFlightMessages = new ConcurrentHashMap<>();
    this.outbound = new LinkedBlockingQueue<>(MAX_QUEUED_REQUESTS);
    this.requestTickets = new Semaphore(MAX_PENDING_REQUESTS);
    this.currentTransactionID = new AtomicLong();
    
    this.state = State.RUNNING;
    this.objectStoreMap = new ConcurrentHashMap<>(10240, 0.75f, 128);
    
    // TODO:  This constructor should not be starting a thread so we probably want some external methods to manage the
    //  life-cycle of this internal thread.
    this.sender.start();
  }

  @Override
  public synchronized boolean doesEntityExist(EntityID entityID, long version) {
    // We will synthesize a descriptor for this lookup.
    EntityDescriptor lookupDescriptor = new EntityDescriptor(entityID, ClientInstanceID.NULL_ID, version);
    Set<VoltronEntityMessage.Acks> requestedAcks = EnumSet.of(VoltronEntityMessage.Acks.APPLIED);
    // A "DOES_EXIST" isn't replicated.
    boolean requiresReplication = false;
    byte[] payload = new byte[0];
    NetworkVoltronEntityMessage message = createMessageWithDescriptor(lookupDescriptor, requiresReplication, payload, VoltronEntityMessage.Type.DOES_EXIST);
    boolean doesExist = false;
    try {
      synchronousWaitForResponse(message, requestedAcks);
      // If we don't throw an exception, it means the entity exists.
      doesExist = true;
    } catch (ExecutionException e) {
      // We get a not found exception if the entity didn't exist.
    }
    return doesExist;
  }

  @Override
  public synchronized EntityClientEndpoint fetchEntity(EntityDescriptor entityDescriptor, Runnable closeHook) {
    return internalLookup(entityDescriptor, closeHook);
  }

  @Override
  public void releaseEntity(EntityDescriptor entityDescriptor) {
    internalRelease(entityDescriptor);
  }

  @Override
  public void handleMessage(EntityDescriptor entityDescriptor, byte[] message) {
    @SuppressWarnings("resource")
    EntityClientEndpoint endpoint = this.objectStoreMap.get(entityDescriptor);
    if (endpoint != null) {
      EntityClientEndpointImpl endpointImpl = (EntityClientEndpointImpl) endpoint;
      endpointImpl.handleMessage(message);
    } else {
      logger.info("Entity " + entityDescriptor + " not found. Ignoring message.");
    }
  }

  @Override
  public Future<Void> createEntity(EntityID entityID, long version, Set<VoltronEntityMessage.Acks> requestedAcks, byte[] config) {
    // A create needs to be replicated.
    boolean requiresReplication = true;
    NetworkVoltronEntityMessage message = createMessageWithoutClientInstance(entityID, version, requestedAcks, requiresReplication, config, VoltronEntityMessage.Type.CREATE_ENTITY);
    return Futures.lazyTransform(createInFlightMessageAfterAcks(message, requestedAcks), ignore -> null);
  }

  @Override
  public Future<Void> destroyEntity(EntityID entityID, long version, Set<VoltronEntityMessage.Acks> requestedAcks) {
    // A destroy needs to be replicated.
    boolean requiresReplication = true;
    // A destroy call has no extended data.
    byte[] emtpyExtendedData = new byte[0];
    NetworkVoltronEntityMessage message = createMessageWithoutClientInstance(entityID, version, requestedAcks, requiresReplication, emtpyExtendedData, VoltronEntityMessage.Type.DESTROY_ENTITY);
    return Futures.lazyTransform(createInFlightMessageAfterAcks(message, requestedAcks), ignore -> null);
  }

  @Override
  public Future<byte[]> invokeAction(EntityDescriptor entityDescriptor, Set<VoltronEntityMessage.Acks> requestedAcks, boolean requiresReplication, byte[] payload) {
    NetworkVoltronEntityMessage message = createMessageWithDescriptor(entityDescriptor, requiresReplication, payload, VoltronEntityMessage.Type.INVOKE_ACTION);
    return createInFlightMessageAfterAcks(message, requestedAcks);
  }

  @Override
  public synchronized PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.print(this.getClass().getName()).flush();
    out.duplicateAndIndent().indent().print(this.state).flush();
    out.duplicateAndIndent().indent().print("inFlightMessages size: ").print(Integer.valueOf(this.inFlightMessages.size())).flush();
    out.duplicateAndIndent().indent().print("outbound size: ").print(Integer.valueOf(this.outbound.size())).flush();
    out.duplicateAndIndent().indent().print("objectStoreMap size: ").print(Integer.valueOf(this.objectStoreMap.size())).flush();
    return out;
  }

  @Override
  public void received(TransactionID id) {
    // Note that this call comes the platform, potentially concurrently with complete()/failure().
    InFlightMessage inFlight = inFlightMessages.get(id);
    if (inFlight == null) {
      throw new RuntimeException("Got an ack for an unknown transaction id " + id);
    }
    inFlight.received();
  }

  @Override
  public void complete(TransactionID id) {
    // Note that this call comes the platform, potentially concurrently with received().
    complete(id, null);
  }

  @Override
  public void complete(TransactionID id, byte[] value) {
    // Note that this call comes the platform, potentially concurrently with received().
    InFlightMessage inFlight = inFlightMessages.remove(id);
    if (inFlight != null) {
      requestTickets.release();
      inFlight.setResult(value, null);
    } else {
      throw new IllegalArgumentException("Got an unknown transaction id ack " + id);
    }
  }

  @Override
  public void failed(TransactionID id, Exception e) {
    // Note that this call comes the platform, potentially concurrently with received().
    InFlightMessage inFlight = inFlightMessages.remove(id);
    if (inFlight == null) {
      throw new IllegalArgumentException("Got an unknown transaction id that failed with error.", e);
    }
    requestTickets.release();
    inFlight.setResult(null, e);
  }

  @Override
  public synchronized void pause() {
    Assert.assertTrue("Attempt to pause while PAUSED: " + this.state, State.PAUSED != this.state);
    this.state = State.PAUSED;
    // Notify anyone waiting for this state.
    notifyAll();
  }

  @Override
  public synchronized void unpause() {
    Assert.assertTrue("Attempt to unpause while RUNNING: " + this.state, State.RUNNING != this.state);
    this.state = State.RUNNING;
    // Notify anyone waiting for this state.
    notifyAll();
  }

  @Override
  public synchronized void initializeHandshake(ClientHandshakeMessage handshakeMessage) {
    if (this.state != State.STOPPED) {
      Assert.assertTrue("Attempt to initiateHandshake: " + this.state, this.state == State.PAUSED || this.state == State.REJOIN_IN_PROGRESS);
      this.state = State.STARTING;
    }
    // Walk the objectStoreMap and add reconnect references for any objects found there.
    for (EntityDescriptor descriptor : this.objectStoreMap.keySet()) {
      EntityID entityID = descriptor.getEntityID();
      long entityVersion = descriptor.getClientSideVersion();
      ClientInstanceID clientInstanceID = descriptor.getClientInstanceID();
      ClientEntityReferenceContext context = new ClientEntityReferenceContext(entityID, entityVersion, clientInstanceID);
      handshakeMessage.addReconnectReference(context);
    }
    // Walk the inFlightMessages, adding them all to the handshake, since we need them to be replayed.
    for (InFlightMessage inFlight : this.inFlightMessages.values()) {
      NetworkVoltronEntityMessage message = inFlight.getMessage();
      ResendVoltronEntityMessage packaged = new ResendVoltronEntityMessage(message.getSource(), message.getTransactionID(), message.getEntityDescriptor(), message.getType(), message.doesRequireReplication(), message.getExtendedData(), message.getOldestTransactionOnClient());
      handshakeMessage.addResendMessage(packaged);
    }
  }

  @Override
  public synchronized void shutdown(boolean fromShutdownHook) {
    this.state = State.STOPPED;
    // Notify anyone waiting for this state.
    notifyAll();
  }

  @Override
  public synchronized void cleanup() {
    if (state != State.PAUSED) {
      String message = "cleanup unexpected state: expected " + State.PAUSED + " but found " + state;
      throw new IllegalStateException(message);
    }
    state = State.REJOIN_IN_PROGRESS;
    objectStoreMap.clear();
    // Notify anyone waiting for this state.
    notifyAll();
  }

  @Override
  public byte[] retrieve(EntityDescriptor entityDescriptor) {
    return internalRetrieve(entityDescriptor);
  }

  @Override
  public void release(EntityDescriptor entityDescriptor) {
    internalRelease(entityDescriptor);
  }


  private EntityClientEndpoint internalLookup(EntityDescriptor entityDescriptor, Runnable closeHook) {
    Assert.assertNotNull("Can't lookup null entity descriptor", entityDescriptor);
    if (State.REJOIN_IN_PROGRESS == this.state) {
      throw new PlatformRejoinException("Unable to start lookup for EntityDescriptor " + entityDescriptor
                                        + " due to rejoin in progress state");
    }
    
    EntityClientEndpoint resolvedEndpoint = null;
    try {
      byte[] config = internalRetrieve(entityDescriptor);
      // We can only fail to get the config if we threw an exception.
      Assert.assertTrue(null != config);
      // We managed to retrieve the config so create the end-point.
      // Note that we will need to call release on this descriptor, when it is closed, prior to running the closeHook we
      //  were given so combine these ideas to pass in one runnable.
      Runnable compoundRunnable = new Runnable() {
        @Override
        public void run() {
          internalRelease(entityDescriptor);
          closeHook.run();
        }
      };
      resolvedEndpoint = new EntityClientEndpointImpl(entityDescriptor, this, config, compoundRunnable);
      
      if (null != this.objectStoreMap.get(entityDescriptor)) {
        throw Assert.failure("Attempt to add an object that already exists: Object of class " + resolvedEndpoint.getClass()
                             + " [Identity Hashcode : 0x" + Integer.toHexString(System.identityHashCode(resolvedEndpoint)) + "] ");
      }
      this.objectStoreMap.put(entityDescriptor, resolvedEndpoint);
    } catch (TCObjectNotFoundException e) {
      // In this case, we failed to find the object in a well-defined way so we will return that we didn't find it.
      resolvedEndpoint = null;
    } catch (Throwable t) {
      // This is the unexpected case so clean up and re-throw as a RuntimeException
      logger.warn("Exception retrieving entity descriptor " + entityDescriptor, t);
      // Clean up any client-side or server-side state regarding this failed connection.
      internalRelease(entityDescriptor);
      throw Throwables.propagate(t);
    }

    // If we created the end-point, it will handle the closeHook.  If we failed to look it up, we need to call the closeHook, ourselves.
    if (null == resolvedEndpoint) {
      closeHook.run();
    }
    return resolvedEndpoint;
  }

  private void internalRelease(EntityDescriptor entityDescriptor) {
    waitUntilRunning();
    
    // We will immediately remove this from our map so that we don't try to service new calls _from_ the server while we
    //  are trying to release.
    this.objectStoreMap.remove(entityDescriptor);
    
    // We need to provide fully blocking semantics with this call so we will wait for the "APPLIED" ack.
    Set<VoltronEntityMessage.Acks> requestedAcks = EnumSet.of(VoltronEntityMessage.Acks.APPLIED);
    // A "RELEASE" doesn't matter to the passive.
    boolean requiresReplication = false;
    byte[] payload = new byte[0];
    NetworkVoltronEntityMessage message = createMessageWithDescriptor(entityDescriptor, requiresReplication, payload, VoltronEntityMessage.Type.RELEASE_ENTITY);
    try {
      synchronousWaitForResponse(message, requestedAcks);
    } catch (ExecutionException e) {
      // Currently, we will interpret this as being unable to find the entity to release but we may want to change this, in the future.
      // TODO:  Rationalize these exceptions so we can throw back exactly what we got from the server.
      throw new TCObjectNotFoundException(entityDescriptor.toString());
    }
  }

  /**
   * Sends the message, waiting for requestedAcks, then waits for the response to come back.
   * @return The returned byte[], on success, or throws the ExecutionException representing the failure.
   */
  private byte[] synchronousWaitForResponse(NetworkVoltronEntityMessage message, Set<VoltronEntityMessage.Acks> requestedAcks) throws ExecutionException {
    InFlightMessage inFlight = createInFlightMessageAfterAcks(message, requestedAcks);
    // Just wait for it.
    byte[] result = null;
    try {
      result = inFlight.get();
    } catch (InterruptedException e) {
      // For now, we will just rethrow this until we determine how else to handle it.
      throw new RuntimeException(e);
    }
    return result;
  }

  /**
   * TODO:  Which operations need to wait on this?  Is it both outgoing calls _and_ incoming acks?
   */
  private void waitUntilRunning() {
    boolean isInterrupted = false;
    try {
      while (State.RUNNING != this.state) {
        if (State.STOPPED == this.state) {
          throw new TCNotRunningException();
        }
        if (State.REJOIN_IN_PROGRESS == this.state) {
          throw new PlatformRejoinException();
        }
        try {
          wait();
        } catch (final InterruptedException e) {
          isInterrupted = true;
        }
      }
    } finally {
      Util.selfInterruptIfNeeded(isInterrupted);
    }
  }

  private synchronized byte[] internalRetrieve(EntityDescriptor entityDescriptor) {
    waitUntilRunning();
    
    // We need to provide fully blocking semantics with this call so we will wait for the "APPLIED" ack.
    Set<VoltronEntityMessage.Acks> requestedAcks = EnumSet.of(VoltronEntityMessage.Acks.APPLIED);
    // We don't care about whether a "FETCH" is replicated.
    boolean requiresReplication = false;
    byte[] payload = new byte[0];
    NetworkVoltronEntityMessage message = createMessageWithDescriptor(entityDescriptor, requiresReplication, payload, VoltronEntityMessage.Type.FETCH_ENTITY);
    byte[] result = null;
    try {
      result = synchronousWaitForResponse(message, requestedAcks);
    } catch (ExecutionException e) {
      // There was a failure in the GET.
      // Currently, we only communicate this back via a TCObjectNotFoundException.
      // TODO:  Rationalize these exceptions so we can throw back exactly what we got from the server.
      throw new TCObjectNotFoundException(entityDescriptor.toString());
    }
    return result;
  }

  private void sendLoop() {
    boolean interrupted = false;
    while (!interrupted) {
      try {
        InFlightMessage first = outbound.take();
        inFlightMessages.put(first.getTransactionID(), first);
        first.send();
      } catch (InterruptedException e) {
        logger.info("ClientRequestManager interrupted! bailing out.");
        // We now want to fall out of the loop.
        interrupted = true;
      }
    }
  }

  private InFlightMessage createInFlightMessageAfterAcks(NetworkVoltronEntityMessage message, Set<VoltronEntityMessage.Acks> requestedAcks) {
    InFlightMessage inFlight = new InFlightMessage(message, requestedAcks);
    boolean interrupted = false;
    while (true) {
      try {
        outbound.put(inFlight);
        break;
      } catch (InterruptedException e1) {
        interrupted = true;
      }
    }
    inFlight.waitForAcks();
    Util.selfInterruptIfNeeded(interrupted);
    return inFlight;
  }

  private NetworkVoltronEntityMessage createMessageWithoutClientInstance(EntityID entityID, long version, Set<VoltronEntityMessage.Acks> requestedAcks, boolean requiresReplication, byte[] config, VoltronEntityMessage.Type type) {
    // We have no client instance for a create but the request currently requires a full descriptor.
    EntityDescriptor entityDescriptor = new EntityDescriptor(entityID, ClientInstanceID.NULL_ID, version);
    return createMessageWithDescriptor(entityDescriptor, requiresReplication, config, type);
  }

  private NetworkVoltronEntityMessage createMessageWithDescriptor(EntityDescriptor entityDescriptor, boolean requiresReplication, byte[] config, VoltronEntityMessage.Type type) {
    // Get the clientID for our channel.
    ClientID clientID = this.channel.getClientID();
    // Get the next transaction ID.
    TransactionID transactionID = new TransactionID(currentTransactionID.incrementAndGet());
    // Figure out the "trailing edge" of the current progress through the transaction stream.
    TransactionID oldestTransactionPending = transactionID;
    for (TransactionID pendingID : this.inFlightMessages.keySet()) {
      if (oldestTransactionPending.compareTo(pendingID) > 0) {
        // peindingID is earlier than oldestTransactionPending.
        oldestTransactionPending = pendingID;
      }
    }
    // Create the message and populate it.
    NetworkVoltronEntityMessage message = (NetworkVoltronEntityMessage) channel.createMessage(TCMessageType.VOLTRON_ENTITY_MESSAGE);
    message.setContents(clientID, transactionID, entityDescriptor, type, requiresReplication, config, oldestTransactionPending);
    return message;
  }


  /**
   * This is essentially a wrapper over an in-flight VoltronEntityMessage, used for tracking its response.
   * The message is stored here, since it is sent asynchronously, along with storage for the return value.
   */
  private class InFlightMessage implements Future<byte[]> {
    private final NetworkVoltronEntityMessage message;
    /**
     * The set of pending ACKs determines when the caller returns from the send, in order to preserve ordering in the
     * client code.  This is different from being "done" which specifically means that the APPLIED has happened,
     * potentially returning a value or exception.
     * ACKs are removed from this pending set, as they arrive.
     */
    private final Set<VoltronEntityMessage.Acks> pendingAcks;

    private boolean isSent;
    private Exception exception;
    private byte[] value;
    private boolean done;

    private InFlightMessage(NetworkVoltronEntityMessage message, Set<VoltronEntityMessage.Acks> acks) {
      this.message = message;
      this.pendingAcks = EnumSet.noneOf(VoltronEntityMessage.Acks.class);
      this.pendingAcks.addAll(acks);
    }

    /**
     * Used when populating the reconnect handshake.
     */
    public NetworkVoltronEntityMessage getMessage() {
      return this.message;
    }

    public TransactionID getTransactionID() {
      return this.message.getTransactionID();
    }

    public void send() {
      Assert.assertFalse(this.isSent);
      this.isSent = true;
      this.message.send();
    }

    public synchronized void waitForAcks() {
      boolean interrupted = false;
      while (!this.pendingAcks.isEmpty()) {
        try {
          wait();
        } catch (InterruptedException e) {
          interrupted = true;
        }
      }
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }

    public synchronized void received() {
      if (this.pendingAcks.remove(VoltronEntityMessage.Acks.RECEIVED)) {
        if (this.pendingAcks.isEmpty()) {
          notifyAll();
        }
      }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public boolean isCancelled() {
      return false;
    }

    @Override
    public synchronized boolean isDone() {
      return done;
    }

    @Override
    public synchronized byte[] get() throws InterruptedException, ExecutionException {
      while (!done) {
        wait();
      }
      if (exception != null) {
        throw new ExecutionException(exception);
      } else {
        return value;
      }
    }

    @Override
    public synchronized byte[] get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      long end = System.nanoTime() + unit.toNanos(timeout);
      while (!done) {
        long remainingNanos = end - System.nanoTime();
        wait(TimeUnit.NANOSECONDS.toMillis(remainingNanos)); // only millisecond resolution. Close enough...?
      }
      if (exception != null) {
        throw new ExecutionException(exception);
      } else {
        return value;
      }
    }

    synchronized void setResult(byte[] value, Exception e) {
      this.pendingAcks.remove(VoltronEntityMessage.Acks.APPLIED);
      this.exception = e;
      this.value = value;
      this.done = true;
      notifyAll();
    }
  }
}
