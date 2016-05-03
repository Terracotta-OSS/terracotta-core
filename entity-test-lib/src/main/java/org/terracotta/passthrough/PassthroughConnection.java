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
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.terracotta.connection.Connection;
import org.terracotta.connection.entity.Entity;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.InvokeFuture;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.exception.EntityException;
import org.terracotta.passthrough.PassthroughMessage.Type;

import com.google.common.base.Throwables;


/**
 * The pass-through component which represents a client connected to the server.
 * Internally, this runs a single thread to handle incoming ACKs, completions, and messages.
 */
public class PassthroughConnection implements Connection {
  private final PassthroughConnectionState connectionState;
  
  private final List<EntityClientService<?, ?, ? extends EntityMessage, ? extends EntityResponse>> entityClientServices;
  private long nextClientEndpointID;
  private final Map<Long, PassthroughEntityClientEndpoint<?, ?>> localEndpoints;
  private final Set<PassthroughEntityTuple> writeLockedEntities;
  private final Runnable onClose;
  private final long uniqueConnectionID;
  
  // ivars related to message passing and client thread.
  private boolean isRunning;
  private Thread clientThread;
  private final List<ServerToClientMessageRecord> messageQueue;
  // NOTE:  this queue exists to carry any Futures pushed in when the server-side injects a message to the client.
  // This approach is an ugly work-around for limitations imposed by running the server message processing and server
  // execution on a single thread.  Ideally, we would send another message to the server, in this case, to better emulate
  // the real implementation.
  // TODO:  Remove this in favor of splitting the server-side execution thread from its message processing thread.
  private final List<Waiter> clientResponseWaitQueue;


  public PassthroughConnection(PassthroughServerProcess serverProcess, List<EntityClientService<?, ?, ? extends EntityMessage, ? extends EntityResponse>> entityClientServices, Runnable onClose, long uniqueConnectionID) {
    this.connectionState = new PassthroughConnectionState(serverProcess);
    this.entityClientServices = entityClientServices;
    this.nextClientEndpointID = 1;
    this.localEndpoints = new HashMap<Long, PassthroughEntityClientEndpoint<?, ?>>();
    this.writeLockedEntities = new HashSet<PassthroughEntityTuple>();
    this.onClose = onClose;
    this.uniqueConnectionID = uniqueConnectionID;
    
    this.isRunning = true;
    this.clientThread = new Thread(new Runnable() {
      @Override
      public void run() {
        runClientThread();
      }
    });
    this.clientThread.setUncaughtExceptionHandler(PassthroughUncaughtExceptionHandler.sharedInstance);
    this.messageQueue = new Vector<ServerToClientMessageRecord>();
    this.clientResponseWaitQueue = new Vector<Waiter>();
    
    // Note:  This should probably not be in the constructor.
    this.clientThread.start();
  }

  /**
   * @return The unique connection ID of the receiver.
   */
  public long getUniqueConnectionID() {
    return this.uniqueConnectionID;
  }

  /**
   * This entry-point is for any of our internal message types.  They ack and complete before returning.
   * @param message
   * @return
   */
  public PassthroughWait sendInternalMessageAfterAcks(PassthroughMessage message) {
    boolean shouldWaitForSent = true;
    boolean shouldWaitForReceived = true;
    boolean shouldWaitForCompleted = true;
    return invokeAndWait(message, shouldWaitForSent, shouldWaitForReceived, shouldWaitForCompleted);
  }

  /**
   * This entry-point is specifically used for entity-defined action messages.
   */
  public InvokeFuture<byte[]> invokeActionAndWaitForAcks(PassthroughMessage message, boolean shouldWaitForSent, boolean shouldWaitForReceived, boolean shouldWaitForCompleted) {
    return invokeAndWait(message, shouldWaitForSent, shouldWaitForReceived, shouldWaitForCompleted);
  }

  private PassthroughWait invokeAndWait(PassthroughMessage message, boolean shouldWaitForSent, boolean shouldWaitForReceived, boolean shouldWaitForCompleted) {
    PassthroughWait waiter = this.connectionState.sendNormal(this, message, shouldWaitForSent, shouldWaitForReceived, shouldWaitForCompleted);
    waiter.waitForAck();
    return waiter;
  }

  @SuppressWarnings({ "unchecked" })
  public <T> T createEntityInstance(Class<T> cls, String name, final long clientInstanceID, long clientSideVersion, byte[] config) {
    EntityClientService<?, ?, ? extends EntityMessage, ? extends EntityResponse> service = getEntityClientService(cls);
    return (T) storeNewEndpointAndCreateInstance(cls, name, clientInstanceID, config, service);
  }

  // Exists to create a generic type context for M and R.
  private <M extends EntityMessage, R extends EntityResponse> Entity storeNewEndpointAndCreateInstance(Class<?> cls, String name, final long clientInstanceID, byte[] config, EntityClientService<?, ?, M, R> service) {
    Runnable onClose = new Runnable() {
      @Override
      public void run() {
        localEndpoints.remove(clientInstanceID);
      }
    };
    PassthroughEntityClientEndpoint<M, R> endpoint = new PassthroughEntityClientEndpoint<M, R>(this, cls, name, clientInstanceID, config, service.getMessageCodec(), onClose);
    this.localEndpoints.put(clientInstanceID, endpoint);
    return service.create(endpoint);
  }

  public synchronized void sendMessageToClient(PassthroughServerProcess sender, byte[] payload) {
    // The sender is used to determine if this is a message coming in from a stale connection - checked on dequeue to avoid race conditions.
    if (this.connectionState.isConnected(sender)) {
      ServerToClientMessageRecord record = new ServerToClientMessageRecord(sender, payload);
      this.messageQueue.add(record);
      notifyAll();
    }
  }
  
  private synchronized void runClientThread() {
    Thread.currentThread().setName("Client thread");
    while (this.isRunning) {
      if (!this.messageQueue.isEmpty()) {
        ServerToClientMessageRecord message = this.messageQueue.remove(0);
        // We will do a quick pre-check that this is from the sender we are currently using.
        if (this.connectionState.isConnected(message.sender)) {
          clientThreadHandleMessage(message.sender, message.payload);
        }
      } else {
        try {
          this.wait();
        } catch (InterruptedException e) {
          Assert.unexpected(e);
        }
      }
    }
  }

  // Note that this method is called under the monitor.
  private void clientThreadHandleMessage(final PassthroughServerProcess sender, byte[] message) {
    PassthroughMessageCodec.Decoder<Void> decoder = new PassthroughMessageCodec.Decoder<Void>() {
      @Override
      public Void decode(Type type, boolean shouldReplicate, long transactionID, long oldestTransactionID, DataInputStream input) throws IOException {
        switch (type) {
          case ACK_FROM_SERVER:
            handleAck(sender, transactionID);
            break;
          case COMPLETE_FROM_SERVER: {
            // Complete has a flag for success/failure, followed by return value and exception.
            boolean isSuccess = input.readBoolean();
            int length = input.readInt();
            byte[] bytes = null;
            if (-1 == length) {
              // This is the case of a null result.
            } else {
              bytes = new byte[length];
              input.readFully(bytes);
            }
            byte[] result = null;
            EntityException error = null;
            if (isSuccess) {
              result = bytes;
            } else {
              error = PassthroughMessageCodec.deserializeExceptionFromArray(bytes);
            }
            handleComplete(sender, transactionID, result, error);
            break;
          }
          case INVOKE_ON_CLIENT: {
            long clientInstanceID = input.readLong();
            int length = input.readInt();
            byte[] result = new byte[length];
            input.readFully(result);
            // First we handle the invoke.
            try {
              handleInvokeOnClient(clientInstanceID, result);
            } catch (MessageCodecException e) {
              // Not expected (implies there is a serious bug in the entity being tested).
              Assert.unexpected(e);
            }
            // Now, we need to send this response as a sort of ack, to the server.  They typically don't wait for it but
            // they can.
            // TODO:  Remove this in favor of splitting the server-side execution thread from its message processing thread.
            PassthroughConnection.this.clientResponseWaitQueue.remove(0).finish();
            break;
          }
          case CREATE_ENTITY:
          case DESTROY_ENTITY:
          case DOES_ENTITY_EXIST:
          case FETCH_ENTITY:
          case RELEASE_ENTITY:
          case INVOKE_ON_SERVER:
          case RECONNECT:
            // Not handled on client.
            Assert.unreachable();
            break;
          default:
            Assert.unreachable();
            break;
        }
        return null;
      }
    };
    PassthroughMessageCodec.decodeRawMessage(decoder, message);
  }

  private void handleAck(PassthroughServerProcess sender, long transactionID) {
    PassthroughWait waiter = this.connectionState.getWaiterForTransaction(sender, transactionID);
    // Note that we may fail because this server may be dead.
    if (null != waiter) {
      waiter.handleAck();
    }
  }

  private void handleComplete(PassthroughServerProcess sender, long transactionID, byte[] result, EntityException error) {
    PassthroughWait waiter = this.connectionState.removeWaiterForTransaction(sender, transactionID);
    // Note that we may fail because this server may be dead.
    if (null != waiter) {
      waiter.handleComplete(result, error);
    }
  }

  private void handleInvokeOnClient(long clientInstanceID, byte[] result) throws MessageCodecException {
    this.localEndpoints.get(clientInstanceID).handleMessageFromServer(result);
  }

  @Override
  public void close() {
    // Note that the caller may not know if we were already closed by another test trying to simulate unexpected disconnect situations so check this.
    if (this.isRunning) {
      // We are going to stop processing messages so set us not running and stop our thread.
      synchronized (this) {
        this.isRunning = false;
        this.notifyAll();
      }
      try {
        this.clientThread.join();
      } catch (InterruptedException e) {
        // This is not expected.
        Assert.unexpected(e);
      }
      // Continue with the shutdown.
      // First, call the runnable hook.
      this.onClose.run();
      // Second, walk any end-points still open and tell them they were disconnected.
      for (PassthroughEntityClientEndpoint<?, ?> endpoint : this.localEndpoints.values()) {
        endpoint.didCloseUnexpectedly();
      }
      this.localEndpoints.clear();
    }
  }

  @Override
  public <T extends Entity, C> EntityRef<T, C> getEntityRef(Class<T> cls, long version, String name) {
    @SuppressWarnings("unchecked")
    EntityClientService<T, C, ? extends EntityMessage, ? extends EntityResponse> service = (EntityClientService<T, C, ? extends EntityMessage, ? extends EntityResponse>) getEntityClientService(cls);
    return new PassthroughEntityRef<T, C>(this, service, cls, version, name);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private EntityClientService<?, ?, ? extends EntityMessage, ? extends EntityResponse> getEntityClientService(Class rawClass) {
    EntityClientService<?, ?, ? extends EntityMessage, ? extends EntityResponse> selected = null;
    for (EntityClientService<?, ?, ? extends EntityMessage, ? extends EntityResponse> service : this.entityClientServices) {
      if (service.handlesEntityType(rawClass)) {
        Assert.assertTrue(null == selected);
        selected = service;
      }
    }
    return selected;
  }

  public long getNewInstanceID() {
    long thisClientEndpointID = this.nextClientEndpointID;
    this.nextClientEndpointID += 1;
    return thisClientEndpointID;
  }

  public synchronized Future<Void> createClientResponseFuture() {
    Waiter waiter = new Waiter();
    this.clientResponseWaitQueue.add(waiter);
    return waiter;
  }


  // TODO:  Remove this in favor of splitting the server-side execution thread from its message processing thread and using
  // a real message, instead of this shared Future.
  private static class Waiter implements Future<Void> {
    private boolean isDone = false;
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      Assert.unreachable();
      return false;
    }
    @Override
    public synchronized Void get() throws InterruptedException, ExecutionException {
      while (!this.isDone) {
        wait();
      }
      return null;
    }
    @Override
    public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      Assert.unreachable();
      return null;
    }
    @Override
    public boolean isCancelled() {
      Assert.unreachable();
      return false;
    }
    @Override
    public synchronized boolean isDone() {
      return this.isDone;
    }
    public synchronized void finish() {
      this.isDone = true;
      notifyAll();
    }
  }


  /**
   * Called after the server restarts to reconnect us to the new instance.
   */
  public void reconnect(PassthroughServerProcess serverProcess) {
    Map<Long, PassthroughWait> waitersToResend = this.connectionState.enterReconnectState(serverProcess);
    
    // Tell the server about our exclusive lock states (since this isn't replicated or persisted).
    for (PassthroughEntityTuple lockedEntity : this.writeLockedEntities) {
      PassthroughMessage message = PassthroughMessageCodec.createWriteLockRestoreMessage(lockedEntity.entityClassName, lockedEntity.entityName);
      // Send the message directly to the new process, waiting for all acks.
      boolean shouldWaitForSent = true;
      boolean shouldWaitForReceived = true;
      boolean shouldWaitForCompleted = true;
      PassthroughWait waiter = this.connectionState.sendAsReconnect(this, message, shouldWaitForSent, shouldWaitForReceived, shouldWaitForCompleted);
      waiter.waitForAck();
    }
    
    // Tell all of our still-open end-points to reconnect to the server.
    for (PassthroughEntityClientEndpoint<?, ?> endpoint : this.localEndpoints.values()) {
      byte[] extendedData = endpoint.getExtendedReconnectData();
      PassthroughMessage message = endpoint.buildReconnectMessage(extendedData);
      // Send the message directly to the new process, waiting for all acks.
      boolean shouldWaitForSent = true;
      boolean shouldWaitForReceived = true;
      boolean shouldWaitForCompleted = true;
      PassthroughWait waiter = this.connectionState.sendAsReconnect(this, message, shouldWaitForSent, shouldWaitForReceived, shouldWaitForCompleted);
      waiter.waitForAck();
    }
    
    // Re-send the existing in-flight messages - note that we need to take a snapshot of these instead of walking the map since it will change as the responses come back.
    for (Map.Entry<Long, PassthroughWait> entry : waitersToResend.entrySet()) {
      long transactionID = entry.getKey();
      PassthroughWait waiter = entry.getValue();
      this.connectionState.sendAsResend(this, transactionID, waiter);
      // This is a little heavy-handed but it gives us a clean state for leaving reconnect.
      // We don't really care what the get does, just that it blocks until complete.
      try {
        waiter.get();
      } catch (InterruptedException e) {
        // Unexpected.
        Throwables.propagate(e);
      } catch (EntityException e) {
        // We ignore this since someone will call the get(), later, in a more appropriate place.
      }
    }
    
    // Now that we send the reconnect handshake and the re-sent transactions, we can install the new serverProcess and permit the new messages to go through.
    this.connectionState.finishReconnectState();
  }

  public void disconnect() {
    this.connectionState.enterDisconnectedState();
  }

  public void didAcquireWriteLock(PassthroughEntityTuple entityTuple) {
    boolean isNew = this.writeLockedEntities.add(entityTuple);
    // We can't have duplicate entries.
    Assert.assertTrue(isNew);
  }

  public void didReleaseWriteLock(PassthroughEntityTuple entityTuple) {
    boolean wasRemoved = this.writeLockedEntities.remove(entityTuple);
    // We can't fail to remove.
    Assert.assertTrue(wasRemoved);
  }

  private static class ServerToClientMessageRecord {
    public final PassthroughServerProcess sender;
    public final byte[] payload;
    
    public ServerToClientMessageRecord(PassthroughServerProcess sender, byte[] payload) {
      this.sender = sender;
      this.payload = payload;
    }
  }
}
