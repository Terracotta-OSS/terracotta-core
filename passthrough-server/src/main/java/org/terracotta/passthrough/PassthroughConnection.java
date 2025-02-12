/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.passthrough;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
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
import org.terracotta.entity.Invocation;
import org.terracotta.entity.InvocationCallback;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.exception.ConnectionClosedException;
import org.terracotta.exception.EntityException;
import org.terracotta.passthrough.PassthroughMessage.Type;

import static java.util.EnumSet.of;
import static org.terracotta.entity.InvocationCallback.Types.FAILURE;
import static org.terracotta.entity.InvocationCallback.Types.RESULT;
import static org.terracotta.entity.InvocationCallback.Types.RETIRED;


/**
 * The pass-through component which represents a client connected to the server.
 * Internally, this runs a single thread to handle incoming ACKs, completions, and messages.
 */
public class PassthroughConnection implements Connection {
  // Information that we collect and only pass through for M&M reasons.
  private final String connectionName;
  private final String uuid;

  private final PassthroughConnectionState connectionState;
  
  private final List<EntityClientService<?, ?, ? extends EntityMessage, ? extends EntityResponse, ?>> entityClientServices;
  private long nextClientEndpointID;
  private final Map<Long, PassthroughEntityClientEndpoint<?, ?>> localEndpoints;
  private final Runnable onClose;
  private final long uniqueConnectionID;
  private final PassthroughEndpointConnector endpointConnector;
  private final String readerThreadName;
  
  // ivars related to message passing and client thread.
  private volatile State state = State.INIT;
  private Thread clientThread;
  private final List<ServerToClientMessageRecord> messageQueue;
  // NOTE:  this queue exists to carry any Futures pushed in when the server-side injects a message to the client.
  // This approach is an ugly work-around for limitations imposed by running the server message processing and server
  // execution on a single thread.  Ideally, we would send another message to the server, in this case, to better emulate
  // the real implementation.
  // TODO:  Remove this in favor of splitting the server-side execution thread from its message processing thread.
  private final List<Waiter> clientResponseWaitQueue;
  
  // This is only used during reconnect.
  private Map<Long, PassthroughInvocationCallback> invocationsToResend;

  public PassthroughConnection(String connectionName, String readerThreadName, PassthroughServerProcess serverProcess, List<EntityClientService<?, ?, ? extends EntityMessage, ? extends EntityResponse, ?>> entityClientServices, Runnable onClose, long uniqueConnectionID) {
    this(connectionName, readerThreadName, serverProcess, entityClientServices, onClose, uniqueConnectionID, new PassthroughEndpointConnectorImpl());
  }

  public PassthroughConnection(String connectionName, String readerThreadName, PassthroughServerProcess serverProcess, List<EntityClientService<?, ?, ? extends EntityMessage, ? extends EntityResponse, ?>> entityClientServices, Runnable onClose, long uniqueConnectionID, PassthroughEndpointConnector endpointConnector) {
    this.connectionName = connectionName;
    this.uuid = java.util.UUID.randomUUID().toString();
    
    this.connectionState = new PassthroughConnectionState(serverProcess);
    this.entityClientServices = entityClientServices;
    this.nextClientEndpointID = 1;
    this.localEndpoints = new HashMap<Long, PassthroughEntityClientEndpoint<?, ?>>();
    this.onClose = onClose;
    this.uniqueConnectionID = uniqueConnectionID;
    this.endpointConnector = endpointConnector;
    this.readerThreadName = readerThreadName;
    this.messageQueue = new Vector<ServerToClientMessageRecord>();
    this.clientResponseWaitQueue = new Vector<Waiter>();
  }

  @Override
  public boolean isValid() {
    return state == State.RUNNING;
  }

  public void startProcessingRequests() {
    this.clientThread = new Thread(() -> runClientThread());
    this.clientThread.setName(readerThreadName);
    this.clientThread.setUncaughtExceptionHandler(PassthroughUncaughtExceptionHandler.sharedInstance);
    this.state = State.RUNNING;
    this.clientThread.start();
  }

  /**
   * @return The name, optionally set by the user via ConnectionPropertyNames.CONNECTION_NAME, in Properties.
   */
  public String getConnectionName() {
    return this.connectionName;
  }

  /**
   * @return The UUID assigned to the connection when it was first created.
   */
  public String getUUID() {
    return this.uuid;
  }

  /**
   * @return The unique connection ID of the receiver.
   */
  public long getUniqueConnectionID() {
    return this.uniqueConnectionID;
  }

  public byte[] invokeAndRetire(PassthroughMessage message) throws ExecutionException, InterruptedException {
    CompletableFuture<byte[]> future = new CompletableFuture<>();
    invoke(message, new InvocationCallback<byte[]>() {

      private volatile byte[] response;

      @Override
      public void result(byte[] response) {
        this.response = response;
      }

      @Override
      public void retired() {
        future.complete(response);
      }

      @Override
      public void failure(Throwable failure) {
        future.completeExceptionally(failure);
      }
    }, of(RESULT, FAILURE, RETIRED));

    return future.get();
  }

  public <R extends EntityResponse> Invocation.Task invoke(PassthroughMessage message, InvocationCallback<byte[]> callback, Set<InvocationCallback.Types> callbacks) {
    // If we have already disconnected, fail with IllegalStateException (this is consistent with the double-close case).
    if(state == State.INIT) {
      throw new IllegalStateException("Connection is not in " + State.RUNNING + " state");
    }
    if (state == State.CLOSED) {
      throw new ConnectionClosedException("Connection already closed");
    }
    this.connectionState.sendNormal(this, message, callback);

    //cannot cancel passthrough messages (atm)
    return () -> false;
  }

  @SuppressWarnings({ "unchecked" })
  public <T, U> T createEntityInstance(Class<?> cls, String name, final long clientInstanceID, long clientSideVersion, byte[] config, U userData) {
    EntityClientService<?, ?, ? extends EntityMessage, ? extends EntityResponse, U> service = (EntityClientService<?, ?, ? extends EntityMessage, ? extends EntityResponse, U>) getEntityClientService(cls);
    return (T) storeNewEndpointAndCreateInstance(cls, name, clientInstanceID, config, service, userData);
  }

  // Exists to create a generic type context for M and R.
  private <M extends EntityMessage, R extends EntityResponse, U> Entity storeNewEndpointAndCreateInstance(Class<?> cls, String name, final long clientInstanceID, byte[] config, EntityClientService<?, ?, M, R, U> service, U userData) {
    Runnable onClose = new Runnable() {
      @Override
      public void run() {
        localEndpoints.remove(clientInstanceID);
      }
    };
    PassthroughEntityClientEndpoint<M, R> endpoint = new PassthroughEntityClientEndpoint<M, R>(this, cls, name, clientInstanceID, config, service.getMessageCodec(), onClose);
    this.localEndpoints.put(clientInstanceID, endpoint);
    return this.endpointConnector.connect(endpoint, service, userData);
  }

  public synchronized void sendMessageToClient(PassthroughServerProcess sender, byte[] payload) {
    // The sender is used to determine if this is a message coming in from a stale connection - checked on dequeue to avoid race conditions.
    if (this.connectionState.isConnected(sender)) {
      ServerToClientMessageRecord record = new ServerToClientMessageRecord(sender, payload);
      this.messageQueue.add(record);
      notifyAll();
    }
  }
  
  private void runClientThread() {
    while (handleNextMessage()) {  
      
    }
  }
  
  private boolean handleNextMessage() {
    ServerToClientMessageRecord message = getNextClientMessage();
    if (message != null) {
      if (this.connectionState.isConnected(message.sender)) {
        clientThreadHandleMessage(message.sender, message.payload);
      }
      return true;
    } else {
      return false;
    }
  }
  
  private synchronized ServerToClientMessageRecord getNextClientMessage() {
    while (state == State.RUNNING) {
      if (!this.messageQueue.isEmpty()) {
        return this.messageQueue.remove(0);
      } else {
        try {
          this.wait();
        } catch (InterruptedException e) {
          Assert.unexpected(e);
        }
      }
    }
    return null;
  }

  private void clientThreadHandleMessage(final PassthroughServerProcess sender, byte[] message) {
    PassthroughMessageCodec.Decoder<Void> decoder = new PassthroughMessageCodec.Decoder<Void>() {
      @Override
      public Void decode(Type type, boolean shouldReplicate, long transactionID, long oldestTransactionID, DataInputStream input) throws IOException {
        switch (type) {
          case ACK_FROM_SERVER:
            handleAck(sender, transactionID);
            break;
          case MONITOR_MESSAGE: 
          case MONITOR_EXCEPTION:
          case COMPLETE_FROM_SERVER:
          case EXCEPTION_FROM_SERVER: {
            // Complete has a flag for success/failure, followed by return value and exception.
            boolean isSuccess = type != Type.EXCEPTION_FROM_SERVER && type != Type.MONITOR_EXCEPTION;
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
            if (type == Type.MONITOR_MESSAGE) {
              handleMonitor(sender, transactionID, result);
            } else {
              handleComplete(sender, transactionID, result, error);
            }
            break;
          }
          case RETIRE_FROM_SERVER:
            handleRetire(sender, transactionID);
            break;
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
          case UNEXPECTED_RELEASE:
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
    PassthroughInvocationCallback invocation = this.connectionState.getInvocationForTransaction(sender, transactionID);
    // Note that we may fail because this server may be dead.
    if (null != invocation) {
      invocation.received();
    }
  }

  private void handleComplete(PassthroughServerProcess sender, long transactionID, byte[] result, EntityException error) {
    PassthroughInvocationCallback invocation = this.connectionState.getInvocationForTransaction(sender, transactionID);
    // Note that we may fail because this server may be dead.
    if (null != invocation) {
      try {
        if (result != null) {
          invocation.result(result);
        }
        if (error != null) {
          invocation.failure(error);
        }
      } finally {
        invocation.complete();
      }
    }
  }
  
  private void handleMonitor(PassthroughServerProcess sender, long transactionID, byte[] result) {
    PassthroughInvocationCallback invocation = this.connectionState.getInvocationForTransaction(sender, transactionID);
    // Note that we may fail because this server may be dead.
    if (null != invocation) {
      invocation.result(result);
    }
  }
  
  private void handleRetire(PassthroughServerProcess sender, long transactionID) {
    PassthroughInvocationCallback invocation = this.connectionState.removeInvocationForTransaction(sender, transactionID);
    // Note that we may fail because this server may be dead.
    if (null != invocation) {
      invocation.retired();
    }
  }

  private void handleInvokeOnClient(long clientInstanceID, byte[] result) throws MessageCodecException {
    this.localEndpoints.get(clientInstanceID).handleMessageFromServer(result);
  }

  @Override
  public void close() {
    if(state == State.INIT) {
      throw new IllegalStateException("Connection is not in " + State.RUNNING + " state");
    } else if (state == State.RUNNING) {
      // It is possible that the server already shut down and will throw IllegalStateException so catch that, here.
      try {
        // Tell each our still-connected end-points to disconnect from the server.
        // (we use a message for this, since it keeps the flow obvious, but we should probably use something more jarring)
        for (PassthroughEntityClientEndpoint<?, ?> endpoint : this.localEndpoints.values()) {
          // Ask the end-point to create the message (only has it the information regarding what entity this is, etc).
          PassthroughMessage releaseMessage = endpoint.createUnexpectedReleaseMessage();
          // Send the message.
          sendUnexpectedCloseMessage(releaseMessage);
        }
      } catch (IllegalStateException e) {
        // Ignore this - it just means the server is shut down so we don't need to send them any messages.
      }
      
      // Tell the connection state that it is now invalid and must force-fail all in-flight messages.
      this.connectionState.forceClose();
      
      // We are going to stop processing messages so set us not running and stop our thread.
      synchronized (this) {
        this.state = State.CLOSED;
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
      
      // We might as well drop the references from our tracking, also, since they can't reasonably be used.
      this.localEndpoints.clear();
    } else {
      //double-close throws IllegalStateException
      throw new IllegalStateException("Connection already closed");
    }
  }

  private void sendUnexpectedCloseMessage(PassthroughMessage message) {
    try {
      invokeAndRetire(message);
    } catch (InterruptedException e) {
      Assert.unexpected(e);
    } catch (ExecutionException e) {
      Assert.unexpected(e);
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked"})
  public EntityRef<?,?,?> getEntityRef(String type, long version, String name) {
    Class<?> clazz = loadEntityType(type);
    EntityClientService service = (clazz != null) ? getEntityClientService(clazz) : null;
    return new PassthroughEntityRef<>(this, service, type, version, name);
  }

  @Override
  public <T extends Entity, C, U> EntityRef<T, C, U> getEntityRef(Class<T> cls, long version, String name) {
    @SuppressWarnings("unchecked")
    EntityClientService<T, C, ? extends EntityMessage, ? extends EntityResponse, U> service = (EntityClientService<T, C, ? extends EntityMessage, ? extends EntityResponse, U>) getEntityClientService(cls);
    return new PassthroughEntityRef<>(this, service, cls.getCanonicalName(), version, name);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private EntityClientService<?, ?, ? extends EntityMessage, ? extends EntityResponse, ?> getEntityClientService(Class type) {
    EntityClientService<?, ?, ? extends EntityMessage, ? extends EntityResponse, ?> selected = null;
    for (EntityClientService<?, ?, ? extends EntityMessage, ? extends EntityResponse, ?> service : this.entityClientServices) {
      if (service.handlesEntityType(type)) {
        Assert.assertTrue(null == selected);
        selected = service;
      }
    }
    return selected;
  }

  private Class<?> loadEntityType(String typeName) {
    try {
      return Class.forName(typeName);
    } catch (ClassNotFoundException notfound) {
      return null;
    }
  }

  public synchronized long getNewInstanceID() {
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
   * Note that reconnect (on either restart or fail-over) is a two-phase process:
   * 1) startReconnect - Reconstructs lock state and per-entity extended reconnect data
   * 2) finishReconnect - Re-sends outstanding messages
   * 
   * The reason for the two-phase approach is that the server can't process re-sends until all clients have reconnected.
   * This way, each client can startReconnect before any clients can finishReconnect.
   * 
   * Otherwise, the server-side would need substantial client-tracking logic to know when everyone had checked in.  Since
   * cases such as reconnect timeout don't happen within passthrough, explicitly ordering the connection messages from all
   * the clients, in this way, is far simpler and more obvious.
   */
  public void startReconnect(PassthroughServerProcess serverProcess) {
    Assert.assertTrue(null == this.invocationsToResend);
    // Duplicate the map to avoid ConcurrentModificationException.
    this.invocationsToResend = new HashMap<>(this.connectionState.enterReconnectState(serverProcess));
    
    // Tell all of our still-open end-points to reconnect to the server.
    for (PassthroughEntityClientEndpoint<?, ?> endpoint : this.localEndpoints.values()) {
      byte[] extendedData = endpoint.getExtendedReconnectData();
      PassthroughMessage message = endpoint.buildReconnectMessage(extendedData);
      // Send the message directly to the new process, waiting for all acks.
      CompletableFuture<byte[]> future = new CompletableFuture<>();
      this.connectionState.sendAsReconnect(this, message, new InvocationCallback<byte[]>() {

        private volatile byte[] response;

        @Override
        public void result(byte[] response) {
          this.response = response;
        }

        @Override
        public void retired() {
          future.complete(response);
        }

        @Override
        public void failure(Throwable failure) {
          future.completeExceptionally(failure);
        }
      });

      try {
        future.get();
      } catch (InterruptedException | ExecutionException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * The second phase of the reconnect - re-send in-flight messages and exit the reconnecting state.
   */
  public void finishReconnect() {
    Assert.assertTrue(null != this.invocationsToResend);
    
    // Re-send the existing in-flight messages - note that we need to take a snapshot of these instead of walking the map since it will change as the responses come back.
    for (Map.Entry<Long, PassthroughInvocationCallback> entry : this.invocationsToResend.entrySet()) {
      long transactionID = entry.getKey();
      PassthroughInvocationCallback invocation = entry.getValue();
      this.connectionState.sendAsResend(this, transactionID, invocation);
    }
    
    // Now that we send the reconnect handshake and the re-sent transactions, we can install the new serverProcess and permit the new messages to go through.
    this.connectionState.finishReconnectState();
    this.invocationsToResend = null;
  }

  public void disconnect() {
    this.connectionState.enterDisconnectedState();
  }

  private static class ServerToClientMessageRecord {
    public final PassthroughServerProcess sender;
    public final byte[] payload;
    
    public ServerToClientMessageRecord(PassthroughServerProcess sender, byte[] payload) {
      this.sender = sender;
      this.payload = payload;
    }
  }

  private enum State {
    INIT, RUNNING, CLOSED
  }
}
