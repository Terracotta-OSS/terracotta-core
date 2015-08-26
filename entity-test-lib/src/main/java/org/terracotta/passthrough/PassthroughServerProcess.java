package org.terracotta.passthrough;

import java.io.DataInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ServerEntityService;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.passthrough.PassthroughMessageCodec.Type;


public class PassthroughServerProcess {
  private boolean isRunning;
  private final List<ServerEntityService<?, ?>> entityServices;
  private final Thread serverThread;
  private final List<MessageContainer> messageQueue;
  // Currently, for simplicity, we will resolve entities by name.
  // Technically, these should be resolved by class+name.
  private final Map<String, ActiveServerEntity> createdEntities;
  private final Map<Class<?>, ServiceProvider> serviceProviderMap;
  
  public PassthroughServerProcess() {
    this.isRunning = true;
    this.entityServices = new Vector<>();
    this.serverThread = new Thread(this::runServerThread);
    this.messageQueue = new Vector<>();
    this.createdEntities = new HashMap<>();
    this.serviceProviderMap = new HashMap<>();
  }
  
  public void start() {
    this.serverThread.start();
  }

  /**
   * This method is only called during start-up of the PassthroughServer, since it goes directly into the instance.  Later,
   * all method calls must go through the sendMessageToServer entry-point.
   * @param service
   */
  public void registerEntityService(ServerEntityService<?, ?> service) {
    this.entityServices.add(service);
  }
  
  public void shutdown() {
    synchronized(this) {
      this.isRunning = false;
      this.notifyAll();
    }
    try {
      this.serverThread.join();
    } catch (InterruptedException e) {
      // Not expected.
      Assert.fail(e);
    }
  }
  
  public synchronized void sendMessageToServer(PassthroughConnection sender, byte[] message) {
    MessageContainer container = new MessageContainer();
    container.sender = sender;
    container.message = message;
    this.messageQueue.add(container);
    this.notifyAll();
  }
  
  private synchronized void runServerThread() {
    while (this.isRunning) {
      if (!this.messageQueue.isEmpty()) {
        MessageContainer container = this.messageQueue.remove(0);
        PassthroughConnection sender = container.sender;
        byte[] message = container.message;
        serverThreadHandleMessage(sender, message);
      } else {
        try {
          this.wait();
        } catch (InterruptedException e) {
          // Not expected.
          Assert.fail(e);
        }
      }
    }
  }
  
  private void serverThreadHandleMessage(PassthroughConnection sender, byte[] message) {
    // Called on the server thread to handle a message.
    PassthroughMessageCodec.Decoder<Void> decoder = (DataInputStream input) -> {
      // Decode the usual, transactionID followed by type ordinal.
      long transactionID = input.readLong();
      int ordinal = input.readInt();
      Type type = PassthroughMessageCodec.Type.values()[ordinal];
      
      // First step, send the ack.
      PassthroughMessage ack = PassthroughMessageCodec.createAckMessage();
      ack.setTransactionID(transactionID);
      sender.sendMessageToClient(ack.asSerializedBytes());
      
      // Now, decode the message and interpret it.
      switch (type) {
        case CREATE_ENTITY: {
          String entityClassName = input.readUTF();
          String entityName = input.readUTF();
          long version = input.readLong();
          byte[] serializedConfiguration = new byte[input.readInt()];
          input.readFully(serializedConfiguration);
          byte[] response = null;
          Exception error = null;
          try {
            // There is no response on successful create.
            handleCreate(entityClassName, entityName, version, serializedConfiguration);
          } catch (Exception e) {
            error = e;
          }
          sendCompleteResponse(sender, transactionID, response, error);
          break;
        }
        case DESTROY_ENTITY: {
          String entityClassName = input.readUTF();
          String entityName = input.readUTF();
          byte[] response = null;
          Exception error = null;
          try {
            // There is no response on successful delete.
            handleDestroy(entityClassName, entityName);
          } catch (Exception e) {
            error = e;
          }
          sendCompleteResponse(sender, transactionID, response, error);
          break;
        }
        case DOES_ENTITY_EXIST:
          // TODO:  Implement.
          Assert.unimplemented();
          break;
        case FETCH_ENTITY: {
          String entityClassName = input.readUTF();
          String entityName = input.readUTF();
          long clientInstanceID = input.readLong();
          long version = input.readLong();
          byte[] response = null;
          Exception error = null;
          try {
            PassthroughClientDescriptor clientDescriptor = new PassthroughClientDescriptor(sender, clientInstanceID);
            // We respond with the config, if found.
            response = handleFetch(clientDescriptor, entityClassName, entityName, version);
          } catch (Exception e) {
            error = e;
          }
          sendCompleteResponse(sender, transactionID, response, error);
          break;
        }
        case RELEASE_ENTITY: {
          String entityClassName = input.readUTF();
          String entityName = input.readUTF();
          long clientInstanceID = input.readLong();
          byte[] response = null;
          Exception error = null;
          try {
            PassthroughClientDescriptor clientDescriptor = new PassthroughClientDescriptor(sender, clientInstanceID);
            // There is no response on successful delete.
            handleRelease(clientDescriptor, entityClassName, entityName);
          } catch (Exception e) {
            error = e;
          }
          sendCompleteResponse(sender, transactionID, response, error);
          break;
        }
        case INVOKE_ON_SERVER: {
          String entityClassName = input.readUTF();
          String entityName = input.readUTF();
          long clientInstanceID = input.readLong();
          byte[] payload = new byte[input.readInt()];
          input.readFully(payload);
          byte[] response = null;
          Exception error = null;
          try {
            PassthroughClientDescriptor clientDescriptor = new PassthroughClientDescriptor(sender, clientInstanceID);
            // We respond with the config, if found.
            response = handleInvoke(clientDescriptor, entityClassName, entityName, payload);
          } catch (Exception e) {
            error = e;
          }
          sendCompleteResponse(sender, transactionID, response, error);
          break;
        }
        case ACK_FROM_SERVER:
        case COMPLETE_FROM_SERVER:
        case INVOKE_ON_CLIENT:
          // Not invoked on server.
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

  private void sendCompleteResponse(PassthroughConnection sender, long transactionID, byte[] response, Exception error) {
    PassthroughMessage complete = PassthroughMessageCodec.createCompleteMessage(response, error);
    complete.setTransactionID(transactionID);
    sender.sendMessageToClient(complete.asSerializedBytes());
  }


  private byte[] handleInvoke(PassthroughClientDescriptor clientDescriptor, String entityClassName, String entityName, byte[] payload) throws Exception {
    byte[] response = null;
    ActiveServerEntity entity = this.createdEntities.get(entityName);
    if (null != entity) {
      response = entity.invoke(clientDescriptor, payload);
    } else {
      throw new Exception("Not fetched");
    }
    return response;
  }

  private byte[] handleFetch(PassthroughClientDescriptor clientDescriptor, String entityClassName, String entityName, long version) throws Exception {
    byte[] config = null;
    ActiveServerEntity entity = this.createdEntities.get(entityName);
    if (null != entity) {
      ServerEntityService<?, ?> service = getEntityServiceForClassName(entityClassName);
      if (service.getVersion() != version) {
        throw new Exception("Version mis-match");
      }
      entity.connected(clientDescriptor);
      config = entity.getConfig();
    } else {
      throw new Exception("Not found");
    }
    return config;
  }

  private void handleRelease(PassthroughClientDescriptor clientDescriptor, String entityClassName, String entityName) throws Exception {
    ActiveServerEntity entity = this.createdEntities.get(entityName);
    if (null != entity) {
      entity.disconnected(clientDescriptor);
    } else {
      throw new Exception("Not found");
    }
  }

  private void handleCreate(String entityClassName, String entityName, long version, byte[] serializedConfiguration) throws Exception {
    if (this.createdEntities.containsKey(entityName)) {
      throw new Exception("Already exists");
    }
    ActiveServerEntity entity = null;
    ServerEntityService<?, ?> service = getEntityServiceForClassName(entityClassName);
    if (service.getVersion() != version) {
      throw new Exception("Version mis-match");
    }
    // TODO:  Make this ID real.
    long consumerID = 0;
    PassthroughServiceRegistry registry = new PassthroughServiceRegistry(consumerID, this.serviceProviderMap);
    entity = service.createActiveEntity(registry, serializedConfiguration);
    this.createdEntities.put(entityName, entity);
  }

  private void handleDestroy(String entityClassName, String entityName) throws Exception {
    ActiveServerEntity entity = this.createdEntities.remove(entityName);
    if (null != entity) {
      // Success.
    } else {
      throw new Exception("Not found");
    }
  }
  
  private ServerEntityService<?, ?> getEntityServiceForClassName(String entityClassName) {
    ServerEntityService<?, ?> foundService = null;
    for (ServerEntityService<?, ?> service : this.entityServices) {
      if (service.handlesEntityType(entityClassName)) {
        Assert.assertTrue(null == foundService);
        foundService = service;
      }
    }
    return foundService;
  }


  private static class MessageContainer {
    public PassthroughConnection sender;
    public byte[] message;
  }


  public <T> void registerServiceProviderForType(Class<T> clazz, ServiceProvider serviceProvider) {
    this.serviceProviderMap.put(clazz, serviceProvider);
  }
}
