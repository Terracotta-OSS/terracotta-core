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

import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.InvokeFuture;
import org.terracotta.exception.EntityException;

import com.google.common.base.Throwables;
import com.tc.entity.NetworkVoltronEntityMessage;
import com.tc.entity.ResendVoltronEntityMessage;
import com.tc.entity.VoltronEntityMessage;
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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;


public class ClientEntityManagerImpl implements ClientEntityManager {
  private static final int MAX_PENDING_REQUESTS = 5000;
  private static final int MAX_QUEUED_REQUESTS = 100;
  private final TCLogger logger;
  
  private final ClientMessageChannel channel;
  private final Thread sender;
  private final ConcurrentMap<TransactionID, InFlightMessage> inFlightMessages;
  private final BlockingQueue<InFlightMessage> outbound;
  private final Semaphore requestTickets;
  private final AtomicLong currentTransactionID;

  private final ClientEntityStateManager stateManager;
  private final ConcurrentMap<EntityDescriptor, EntityClientEndpoint> objectStoreMap;

  
  public ClientEntityManagerImpl(ClientMessageChannel channel) {
    this.logger = new ClientIDLogger(channel, TCLogging.getLogger(ClientEntityManager.class));
    
    this.channel = channel;
    this.sender = new Thread(new Runnable() {
      @Override
      public void run() {
        sendLoop();
      }
    });
    this.sender.setDaemon(true);
    this.inFlightMessages = new ConcurrentHashMap<TransactionID, InFlightMessage>();
    this.outbound = new LinkedBlockingQueue<InFlightMessage>(MAX_QUEUED_REQUESTS);
    this.requestTickets = new Semaphore(MAX_PENDING_REQUESTS);
    this.currentTransactionID = new AtomicLong();
    this.stateManager = new ClientEntityStateManager();
    this.objectStoreMap = new ConcurrentHashMap<EntityDescriptor, EntityClientEndpoint>(10240, 0.75f, 128);
    
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
    } catch (EntityException e) {
      // We will handle any of the exception types as meaning that it doesn't exist.
    }
    return doesExist;
  }

  @Override
  public EntityClientEndpoint fetchEntity(EntityDescriptor entityDescriptor, Runnable closeHook) throws EntityException {
    return internalLookup(entityDescriptor, closeHook);
  }

  @Override
  public void releaseEntity(EntityDescriptor entityDescriptor) throws EntityException {
    internalRelease(entityDescriptor);
  }

  @Override
  public void handleMessage(EntityDescriptor entityDescriptor, byte[] message) {
    EntityClientEndpoint endpoint = this.objectStoreMap.get(entityDescriptor);
    if (endpoint != null) {
      EntityClientEndpointImpl endpointImpl = (EntityClientEndpointImpl) endpoint;
      endpointImpl.handleMessage(message);
    } else {
      logger.info("Entity " + entityDescriptor + " not found. Ignoring message.");
    }
  }

  @Override
  public InvokeFuture<byte[]> createEntity(EntityID entityID, long version, Set<VoltronEntityMessage.Acks> requestedAcks, byte[] config) {
    // A create needs to be replicated.
    boolean requiresReplication = true;
    NetworkVoltronEntityMessage message = createMessageWithoutClientInstance(entityID, version, requestedAcks, requiresReplication, config, VoltronEntityMessage.Type.CREATE_ENTITY);
    return createInFlightMessageAfterAcks(message, requestedAcks);
  }

  @Override
  public InvokeFuture<byte[]> destroyEntity(EntityID entityID, long version, Set<VoltronEntityMessage.Acks> requestedAcks) {
    // A destroy needs to be replicated.
    boolean requiresReplication = true;
    // A destroy call has no extended data.
    byte[] emtpyExtendedData = new byte[0];
    NetworkVoltronEntityMessage message = createMessageWithoutClientInstance(entityID, version, requestedAcks, requiresReplication, emtpyExtendedData, VoltronEntityMessage.Type.DESTROY_ENTITY);
    return createInFlightMessageAfterAcks(message, requestedAcks);
  }

  @Override
  public InvokeFuture<byte[]> invokeAction(EntityDescriptor entityDescriptor, Set<VoltronEntityMessage.Acks> requestedAcks, boolean requiresReplication, byte[] payload) {
    NetworkVoltronEntityMessage message = createMessageWithDescriptor(entityDescriptor, requiresReplication, payload, VoltronEntityMessage.Type.INVOKE_ACTION);
    return createInFlightMessageAfterAcks(message, requestedAcks);
  }

  @Override
  public synchronized PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.print(this.getClass().getName()).flush();
    out.duplicateAndIndent().indent().print(this.stateManager.getCurrentState()).flush();
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
  public void failed(TransactionID id, EntityException e) {
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
    stateManager.pause();
  }

  @Override
  public synchronized void unpause() {
    stateManager.running();
  }

  @Override
  public synchronized void initializeHandshake(ClientHandshakeMessage handshakeMessage) {
    stateManager.start();
    // Walk the objectStoreMap and add reconnect references for any objects found there.
    for (EntityDescriptor descriptor : this.objectStoreMap.keySet()) {
      EntityID entityID = descriptor.getEntityID();
      long entityVersion = descriptor.getClientSideVersion();
      ClientInstanceID clientInstanceID = descriptor.getClientInstanceID();
      byte[] extendedReconnectData = this.objectStoreMap.get(descriptor).getExtendedReconnectData();
      ClientEntityReferenceContext context = new ClientEntityReferenceContext(entityID, entityVersion, clientInstanceID, extendedReconnectData);
      handshakeMessage.addReconnectReference(context);
    }
    // Walk the inFlightMessages, adding them all to the handshake, since we need them to be replayed.
    for (InFlightMessage inFlight : this.inFlightMessages.values()) {
      NetworkVoltronEntityMessage message = inFlight.getMessage();
      ResendVoltronEntityMessage packaged = new ResendVoltronEntityMessage(message.getSource(), message.getTransactionID(), 
          message.getEntityDescriptor(), message.getVoltronType(), message.doesRequireReplication(), message.getExtendedData(), 
          message.getOldestTransactionOnClient());
      handshakeMessage.addResendMessage(packaged);
    }
  }

  @Override
  public synchronized void shutdown(boolean fromShutdownHook) {
    stateManager.stop();
    // We also want to notify any end-points that they have been disconnected.
    for(EntityClientEndpoint endpoint : this.objectStoreMap.values()) {
      endpoint.didCloseUnexpectedly();
    }
    // And then drop them.
    this.objectStoreMap.clear();
  }

  @Override
  public byte[] retrieve(EntityDescriptor entityDescriptor) throws EntityException {
    return internalRetrieve(entityDescriptor);
  }

  @Override
  public void release(EntityDescriptor entityDescriptor) throws EntityException {
    internalRelease(entityDescriptor);
  }


  private EntityClientEndpoint internalLookup(final EntityDescriptor entityDescriptor, final Runnable closeHook) throws EntityException {
    Assert.assertNotNull("Can't lookup null entity descriptor", entityDescriptor);

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
          try {
            internalRelease(entityDescriptor);
          } catch (EntityException e) {
            // We aren't expecting there to be any problems releasing an entity in the close hook so we will just log and re-throw.
            Util.printLogAndRethrowError(e, logger);
          }
          closeHook.run();
        }
      };
      resolvedEndpoint = new EntityClientEndpointImpl(entityDescriptor, this, config, compoundRunnable);
      
      if (null != this.objectStoreMap.get(entityDescriptor)) {
        throw Assert.failure("Attempt to add an object that already exists: Object of class " + resolvedEndpoint.getClass()
                             + " [Identity Hashcode : 0x" + Integer.toHexString(System.identityHashCode(resolvedEndpoint)) + "] ");
      }
      this.objectStoreMap.put(entityDescriptor, resolvedEndpoint);
    } catch (EntityException e) {
      // Release the entity and re-throw to the higher level.
      internalRelease(entityDescriptor);
      // NOTE:  Since we are throwing, we are not responsible for calling the given closeHook.
      throw e;
    } catch (Throwable t) {
      // This is the unexpected case so clean up and re-throw as a RuntimeException
      logger.warn("Exception retrieving entity descriptor " + entityDescriptor, t);
      // Clean up any client-side or server-side state regarding this failed connection.
      internalRelease(entityDescriptor);
      // NOTE:  Since we are throwing, we are not responsible for calling the given closeHook.
      throw Throwables.propagate(t);
    }

    return resolvedEndpoint;
  }

  private void internalRelease(EntityDescriptor entityDescriptor) throws EntityException {
    stateManager.waitUntilRunning();

    // We will immediately remove this from our map so that we don't try to service new calls _from_ the server while we
    //  are trying to release.
    this.objectStoreMap.remove(entityDescriptor);
    
    // We need to provide fully blocking semantics with this call so we will wait for the "APPLIED" ack.
    Set<VoltronEntityMessage.Acks> requestedAcks = EnumSet.of(VoltronEntityMessage.Acks.APPLIED);
    // A "RELEASE" doesn't matter to the passive.
    boolean requiresReplication = false;
    byte[] payload = new byte[0];
    NetworkVoltronEntityMessage message = createMessageWithDescriptor(entityDescriptor, requiresReplication, payload, VoltronEntityMessage.Type.RELEASE_ENTITY);
    synchronousWaitForResponse(message, requestedAcks);
  }

  /**
   * Sends the message, waiting for requestedAcks, then waits for the response to come back.
   * @return The returned byte[], on success, or throws the ExecutionException representing the failure.
   */
  private byte[] synchronousWaitForResponse(NetworkVoltronEntityMessage message, Set<VoltronEntityMessage.Acks> requestedAcks) throws EntityException {
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

  private byte[] internalRetrieve(EntityDescriptor entityDescriptor) throws EntityException {
    stateManager.waitUntilRunning();

    // We need to provide fully blocking semantics with this call so we will wait for the "APPLIED" ack.
    Set<VoltronEntityMessage.Acks> requestedAcks = EnumSet.of(VoltronEntityMessage.Acks.APPLIED);
    // We don't care about whether a "FETCH" is replicated.
    boolean requiresReplication = false;
    byte[] payload = new byte[0];
    NetworkVoltronEntityMessage message = createMessageWithDescriptor(entityDescriptor, requiresReplication, payload, VoltronEntityMessage.Type.FETCH_ENTITY);
    return synchronousWaitForResponse(message, requestedAcks);
  }

  private void sendLoop() {
    boolean interrupted = false;
    while (!interrupted) {
      try {
        InFlightMessage first = outbound.take();
        synchronized (this) {
          inFlightMessages.put(first.getTransactionID(), first);
          first.sent();
        }
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
}
