package org.terracotta.passthrough;

import java.io.DataInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Future;

import org.terracotta.connection.Connection;
import org.terracotta.connection.entity.Entity;
import org.terracotta.connection.entity.EntityMaintenanceRef;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.entity.EntityClientService;
import org.terracotta.passthrough.PassthroughMessageCodec.Type;


/**
 * The pass-through component which represents a client connected to the server.
 * Internally, this runs a single thread to handle incoming ACKs, completions, and messages.
 */
public class PassthroughConnection implements Connection {
  private final List<EntityClientService<?, ?>> entityClientServices;
  private long nextTransactionID;
  private long nextClientEndpointID;
  private final Map<Long, PassthroughWait> inFlight;
  private final PassthroughServerProcess serverProcess;
  private final Map<Long, PassthroughEntityClientEndpoint> localEndpoints;
  
  // ivars related to message passing and client thread.
  private boolean isRunning;
  private Thread clientThread;
  private List<byte[]> messageQueue;


  public PassthroughConnection(PassthroughServerProcess serverProcess, List<EntityClientService<?, ?>> entityClientServices) {
    this.entityClientServices = entityClientServices;
    this.nextTransactionID = 1;
    this.nextClientEndpointID = 1;
    this.inFlight = new HashMap<>();
    this.serverProcess = serverProcess;
    this.localEndpoints = new HashMap<>();
    
    this.isRunning = true;
    this.clientThread = new Thread(this::runClientThread);
    this.messageQueue = new Vector<>();
    
    // Note:  This should probably not be in the constructor.
    this.clientThread.start();
  }

  /**
   * This entry-point is for any of our internal message types.  They ack and complete before returning.
   * @param message
   * @return
   */
  public Future<byte[]> sendInternalMessageAfterAcks(PassthroughMessage message) {
    boolean shouldWaitForReceived = true;
    boolean shouldWaitForCompleted = true;
    return invokeAndWait(message, shouldWaitForReceived, shouldWaitForCompleted);
  }

  /**
   * This entry-point is specifically used for entity-defined action messages.
   */
  public Future<byte[]> invokeActionAndWaitForAcks(PassthroughMessage message, boolean shouldWaitForReceived, boolean shouldWaitForCompleted) {
    return invokeAndWait(message, shouldWaitForReceived, shouldWaitForCompleted);
  }

  private Future<byte[]> invokeAndWait(PassthroughMessage message, boolean shouldWaitForReceived, boolean shouldWaitForCompleted) {
    PassthroughWait waiter = new PassthroughWait(shouldWaitForReceived, shouldWaitForCompleted);
    synchronized(this) {
      long transactionID = this.nextTransactionID;
      this.nextTransactionID += 1;
      message.setTransactionID(transactionID);
      this.inFlight.put(transactionID, waiter);
    }
    byte[] raw = message.asSerializedBytes();
    this.serverProcess.sendMessageToServer(this, raw);
    waiter.waitForAck();
    return waiter;
  }

  @SuppressWarnings({ "unchecked" })
  public <T> T createEntityInstance(Class<T> cls, String name, long clientInstanceID, long clientSideVersion, byte[] config) {
    EntityClientService<?, ?> service = getEntityClientService(cls);
    Runnable onClose = () -> {
      this.localEndpoints.remove(clientInstanceID);
    };
    PassthroughEntityClientEndpoint endpoint = new PassthroughEntityClientEndpoint(this, cls, name, clientInstanceID, config, onClose);
    this.localEndpoints.put(clientInstanceID, endpoint);
    return (T) service.create(endpoint);
  }

  public synchronized void sendMessageToClient(byte[] payload) {
    this.messageQueue.add(payload);
    notifyAll();
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
    PassthroughMessageCodec.Decoder<Void> decoder = (DataInputStream input) -> {
      // Decode the usual, transactionID followed by type ordinal.
      long transactionID = input.readLong();
      int ordinal = input.readInt();
      Type type = PassthroughMessageCodec.Type.values()[ordinal];
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
          Exception error = null;
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
          handleInvokeOnClient(clientInstanceID, result);
          break;
        }
        case CREATE_ENTITY:
        case DESTROY_ENTITY:
        case DOES_ENTITY_EXIST:
        case FETCH_ENTITY:
        case RELEASE_ENTITY:
        case INVOKE_ON_SERVER:
          // Not handled on client.
          Assert.unreachable();
          break;
        default:
          Assert.unreachable();
          break;
      }
      return null;
    };
    PassthroughMessageCodec.decodeRawMessage(decoder, message);
  }

  private void handleAck(long transactionID) {
    PassthroughWait waiter = this.inFlight.get(transactionID);
    Assert.assertTrue(null != waiter);
    waiter.handleAck();
  }

  private void handleComplete(long transactionID, byte[] result, Exception error) {
    PassthroughWait waiter = this.inFlight.get(transactionID);
    Assert.assertTrue(null != waiter);
    this.inFlight.remove(transactionID);
    waiter.handleComplete(result, error);
  }

  private void handleInvokeOnClient(long clientInstanceID, byte[] result) {
    this.localEndpoints.get(clientInstanceID).handleMessageFromServer(result);
  }

  @Override
  public void close() throws Exception {
  }

  @Override
  public <T extends Entity, C> EntityRef<T, C> getEntityRef(Class<T> cls, long version, String name) {
    return new PassthroughEntityRef<>(this, cls, version, name);
  }

  @Override
  public <T extends Entity, C> EntityMaintenanceRef<T, C> acquireMaintenanceModeRef(Class<T> cls, long version, String name) {
    @SuppressWarnings("unchecked")
    EntityClientService<T, C> service = (EntityClientService<T, C>) getEntityClientService(cls);
    return new PassthroughMaintenanceRef<>(this, service, cls, version, name);
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
}
