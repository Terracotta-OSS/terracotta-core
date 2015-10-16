package org.terracotta.passthrough;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.terracotta.connection.Connection;
import org.terracotta.connection.entity.Entity;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.InvokeFuture;
import org.terracotta.exception.EntityException;
import org.terracotta.passthrough.PassthroughMessage.Type;


/**
 * The pass-through component which represents a client connected to the server.
 * Internally, this runs a single thread to handle incoming ACKs, completions, and messages.
 */
public class PassthroughConnection implements Connection {
  private final List<EntityClientService<?, ?>> entityClientServices;
  private long nextTransactionID;
  private long nextClientEndpointID;
  private final Map<Long, PassthroughWait> inFlight;
  private PassthroughServerProcess serverProcess;
  private final Map<Long, PassthroughEntityClientEndpoint> localEndpoints;
  private final Runnable onClose;
  
  // ivars related to message passing and client thread.
  private boolean isRunning;
  private Thread clientThread;
  private final List<byte[]> messageQueue;
  // NOTE:  this queue exists to carry any Futures pushed in when the server-side injects a message to the client.
  // This approach is an ugly work-around for limitations imposed by running the server message processing and server
  // execution on a single thread.  Ideally, we would send another message to the server, in this case, to better emulate
  // the real implementation.
  // TODO:  Remove this in favor of splitting the server-side execution thread from its message processing thread.
  private final List<Waiter> clientResponseWaitQueue;


  public PassthroughConnection(PassthroughServerProcess serverProcess, List<EntityClientService<?, ?>> entityClientServices, Runnable onClose) {
    this.entityClientServices = entityClientServices;
    this.nextTransactionID = 1;
    this.nextClientEndpointID = 1;
    this.inFlight = new HashMap<Long, PassthroughWait>();
    this.serverProcess = serverProcess;
    this.localEndpoints = new HashMap<Long, PassthroughEntityClientEndpoint>();
    this.onClose = onClose;
    
    this.isRunning = true;
    this.clientThread = new Thread(new Runnable() {
      @Override
      public void run() {
        runClientThread();
      }
    });
    this.messageQueue = new Vector<byte[]>();
    this.clientResponseWaitQueue = new Vector<Waiter>();
    
    // Note:  This should probably not be in the constructor.
    this.clientThread.start();
  }

  /**
   * This entry-point is for any of our internal message types.  They ack and complete before returning.
   * @param message
   * @return
   */
  public PassthroughWait sendInternalMessageAfterAcks(PassthroughMessage message) {
    boolean shouldWaitForReceived = true;
    boolean shouldWaitForCompleted = true;
    return invokeAndWait(message, shouldWaitForReceived, shouldWaitForCompleted);
  }

  /**
   * This entry-point is specifically used for entity-defined action messages.
   */
  public InvokeFuture<byte[]> invokeActionAndWaitForAcks(PassthroughMessage message, boolean shouldWaitForReceived, boolean shouldWaitForCompleted) {
    return invokeAndWait(message, shouldWaitForReceived, shouldWaitForCompleted);
  }

  private PassthroughWait invokeAndWait(PassthroughMessage message, boolean shouldWaitForReceived, boolean shouldWaitForCompleted) {
    PassthroughWait waiter = new PassthroughWait(shouldWaitForReceived, shouldWaitForCompleted);
    synchronized(this) {
      long transactionID = this.nextTransactionID;
      this.nextTransactionID += 1;
      message.setTransactionID(transactionID);
      this.inFlight.put(transactionID, waiter);
    }
    byte[] raw = message.asSerializedBytes();
    waiter.saveRawMessageForResend(raw);
    this.serverProcess.sendMessageToServer(this, raw);
    waiter.waitForAck();
    return waiter;
  }

  @SuppressWarnings({ "unchecked" })
  public <T> T createEntityInstance(Class<T> cls, String name, final long clientInstanceID, long clientSideVersion, byte[] config) {
    EntityClientService<?, ?> service = getEntityClientService(cls);
    Runnable onClose = new Runnable() {

      @Override
      public void run() {
        localEndpoints.remove(clientInstanceID);
      }
    };
    PassthroughEntityClientEndpoint endpoint = new PassthroughEntityClientEndpoint(this, cls, name, clientInstanceID, config, onClose);
    this.localEndpoints.put(clientInstanceID, endpoint);
    return (T) service.create(endpoint);
  }

  public synchronized void sendMessageToClient(PassthroughServerProcess sender, byte[] payload) {
    // The sender is used to determine if this is a message coming in from a stale connection.
    if (sender == this.serverProcess) {
      this.messageQueue.add(payload);
      notifyAll();
    }
  }
  
  private synchronized void runClientThread() {
    Thread.currentThread().setName("Client thread");
    while (this.isRunning) {
      if (!this.messageQueue.isEmpty()) {
        byte[] message = this.messageQueue.remove(0);
        clientThreadHandleMessage(message);
      } else {
        try {
          this.wait();
        } catch (InterruptedException e) {
          Assert.unexpected(e);
        }
      }
    }
  }

  private void clientThreadHandleMessage(byte[] message) {
    PassthroughMessageCodec.Decoder<Void> decoder = new PassthroughMessageCodec.Decoder<Void>() {
      @Override
      public Void decode(Type type, boolean shouldReplicate, long transactionID, DataInputStream input) throws IOException {
        switch (type) {
          case ACK_FROM_SERVER:
            handleAck(transactionID);
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
            handleComplete(transactionID, result, error);
            break;
          }
          case INVOKE_ON_CLIENT: {
            long clientInstanceID = input.readLong();
            int length = input.readInt();
            byte[] result = new byte[length];
            input.readFully(result);
            // First we handle the invoke.
            handleInvokeOnClient(clientInstanceID, result);
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

  private void handleAck(long transactionID) {
    PassthroughWait waiter = this.inFlight.get(transactionID);
    Assert.assertTrue(null != waiter);
    waiter.handleAck();
  }

  private void handleComplete(long transactionID, byte[] result, EntityException error) {
    PassthroughWait waiter = this.inFlight.get(transactionID);
    Assert.assertTrue(null != waiter);
    this.inFlight.remove(transactionID);
    waiter.handleComplete(result, error);
  }

  private void handleInvokeOnClient(long clientInstanceID, byte[] result) {
    this.localEndpoints.get(clientInstanceID).handleMessageFromServer(result);
  }

  @Override
  public void close() {
    // First, call the runnable hook.
    this.onClose.run();
    // Second, walk any end-points still open and tell them they were disconnected.
    for (PassthroughEntityClientEndpoint endpoint : this.localEndpoints.values()) {
      endpoint.didCloseUnexpectedly();
    }
    this.localEndpoints.clear();
  }

  @Override
  public <T extends Entity, C> EntityRef<T, C> getEntityRef(Class<T> cls, long version, String name) {
    @SuppressWarnings("unchecked")
    EntityClientService<T, C> service = (EntityClientService<T, C>) getEntityClientService(cls);
    return new PassthroughEntityRef<T, C>(this, service, cls, version, name);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private EntityClientService<?, ?> getEntityClientService(Class rawClass) {
    EntityClientService<?, ?> selected = null;
    for (EntityClientService<?, ?> service : this.entityClientServices) {
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
    synchronized (this) {
      Assert.assertTrue(null == this.serverProcess);
      this.serverProcess = serverProcess;
    }
    
    // Tell all of our still-open end-points to reconnect to the server.
    for (PassthroughEntityClientEndpoint endpoint : this.localEndpoints.values()) {
      byte[] extendedData = endpoint.getExtendedReconnectData();
      endpoint.reconnect(extendedData);
    }
    
    // Re-send the existing in-flight messages.
    for (PassthroughWait waiter : this.inFlight.values()) {
      byte[] raw = waiter.resetAndGetMessageForResend();
      this.serverProcess.sendMessageToServer(this, raw);
      waiter.waitForAck();
    }
  }

  public synchronized void disconnect() {
    this.serverProcess = null;
  }
}
