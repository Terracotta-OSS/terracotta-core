package org.terracotta.passthrough;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.CommonServerEntity;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.ServerEntityService;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.passthrough.PassthroughServerMessageDecoder.MessageHandler;


/**
 * The wrapper around the thread running as the "server process", within the PassthroughServer.
 * Note that this currently handles not only message processing, but also message execution.
 * In the future, message execution will likely be split out into other threads to better support entity read-write locking
 * and also test concurrency strategy.
 */
public class PassthroughServerProcess implements MessageHandler {
  private boolean isRunning;
  private final List<ServerEntityService<?, ?>> entityServices;
  private final Thread serverThread;
  private final List<MessageContainer> messageQueue;
  // Currently, for simplicity, we will resolve entities by name.
  // Technically, these should be resolved by class+name.
  // Note that only ONE of the active or passive entities will be non-null.
  private final Map<String, ActiveServerEntity> activeEntities;
  private final Map<String, PassiveServerEntity> passiveEntities;
  private final Map<Class<?>, ServiceProvider> serviceProviderMap;
  private PassthroughServerProcess downstreamPassive;
  
  public PassthroughServerProcess(boolean isActiveMode) {
    this.isRunning = true;
    this.entityServices = new Vector<>();
    this.serverThread = new Thread(this::runServerThread);
    this.messageQueue = new Vector<>();
    this.activeEntities = (isActiveMode ? new HashMap<>() : null);
    this.passiveEntities = (isActiveMode ? null : new HashMap<>());
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
      Assert.unexpected(e);
    }
  }

  public synchronized void sendMessageToServer(PassthroughConnection sender, byte[] message) {
    MessageContainer container = new MessageContainer();
    container.sender = new IMessageSenderWrapper() {
      @Override
      public void sendAck(PassthroughMessage ack) {
        sender.sendMessageToClient(ack.asSerializedBytes());
      }
      @Override
      public void sendComplete(PassthroughMessage complete) {
        sender.sendMessageToClient(complete.asSerializedBytes());
      }
      @Override
      public PassthroughClientDescriptor clientDescriptorForID(long clientInstanceID) {
        return new PassthroughClientDescriptor(sender, clientInstanceID);
      }
    };
    container.message = message;
    this.messageQueue.add(container);
    this.notifyAll();
  }

  public synchronized void sendMessageToServerFromActive(IMessageSenderWrapper senderCallback, byte[] message) {
    MessageContainer container = new MessageContainer();
    container.sender = senderCallback;
    container.message = message;
    this.messageQueue.add(container);
    this.notifyAll();
  }

  private synchronized void runServerThread() {
    Thread.currentThread().setName("Server thread isActive: " + ((null != this.activeEntities) ? "active" : "passive"));
    while (this.isRunning) {
      if (!this.messageQueue.isEmpty()) {
        MessageContainer container = this.messageQueue.remove(0);
        IMessageSenderWrapper sender = container.sender;
        byte[] message = container.message;
        serverThreadHandleMessage(sender, message);
      } else {
        try {
          this.wait();
        } catch (InterruptedException e) {
          Assert.unexpected(e);
        }
      }
    }
  }
  
  private void serverThreadHandleMessage(IMessageSenderWrapper sender, byte[] message) {
    // Called on the server thread to handle a message.
    PassthroughMessageCodec.Decoder<Void> decoder = new PassthroughServerMessageDecoder(this, this.downstreamPassive, sender, message);
    PassthroughMessageCodec.decodeRawMessage(decoder, message);
  }

  @Override
  public byte[] invoke(PassthroughClientDescriptor clientDescriptor, String entityClassName, String entityName, byte[] payload) throws Exception {
    byte[] response = null;
    if (null != this.activeEntities) {
      // Invoke on active.
      ActiveServerEntity entity = this.activeEntities.get(entityName);
      if (null != entity) {
        response = entity.invoke(clientDescriptor, payload);
      } else {
        throw new Exception("Not fetched");
      }
    } else {
      // Invoke on passive.
      PassiveServerEntity entity = this.passiveEntities.get(entityName);
      if (null != entity) {
        // There is no return type in the passive case.
        entity.invoke(payload);
      } else {
        throw new Exception("Not fetched");
      }
    }
    return response;
  }

  @Override
  public byte[] fetch(PassthroughClientDescriptor clientDescriptor, String entityClassName, String entityName, long version) throws Exception {
    byte[] config = null;
    // Fetch should never be replicated and only handled on the active.
    Assert.assertTrue(null != this.activeEntities);
    ActiveServerEntity entity = this.activeEntities.get(entityName);
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

  @Override
  public void release(PassthroughClientDescriptor clientDescriptor, String entityClassName, String entityName) throws Exception {
    // Release should never be replicated and only handled on the active.
    Assert.assertTrue(null != this.activeEntities);
    ActiveServerEntity entity = this.activeEntities.get(entityName);
    if (null != entity) {
      entity.disconnected(clientDescriptor);
    } else {
      throw new Exception("Not found");
    }
  }

  @Override
  public void create(String entityClassName, String entityName, long version, byte[] serializedConfiguration) throws Exception {
    if (((null != this.activeEntities) && this.activeEntities.containsKey(entityName))
      || ((null != this.passiveEntities) && this.passiveEntities.containsKey(entityName))) {
      throw new Exception("Already exists");
    }
    ServerEntityService<?, ?> service = getEntityServiceForClassName(entityClassName);
    if (service.getVersion() != version) {
      throw new Exception("Version mis-match");
    }
    // TODO:  Make this ID real.
    long consumerID = 0;
    if (null != this.activeEntities) {
      PassthroughServiceRegistry registry = new PassthroughServiceRegistry(consumerID, this.serviceProviderMap);
      ActiveServerEntity entity = null;
      entity = service.createActiveEntity(registry, serializedConfiguration);
      this.activeEntities.put(entityName, entity);
    } else {
      PassthroughServiceRegistry registry = new PassthroughServiceRegistry(consumerID, this.serviceProviderMap);
      PassiveServerEntity entity = null;
      entity = service.createPassiveEntity(registry, serializedConfiguration);
      this.passiveEntities.put(entityName, entity);
    }
  }

  @Override
  public void destroy(String entityClassName, String entityName) throws Exception {
    CommonServerEntity entity = (null != this.activeEntities)
        ? this.activeEntities.remove(entityName)
        : this.passiveEntities.remove(entityName);
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

  public <T> void registerServiceProviderForType(Class<T> clazz, ServiceProvider serviceProvider) {
    this.serviceProviderMap.put(clazz, serviceProvider);
  }

  public void setDownstreamPassiveServerProcess(PassthroughServerProcess serverProcess) {
    // Make sure that we are active and they are passive.
    Assert.assertTrue(null != this.activeEntities);
    Assert.assertTrue(null != serverProcess.passiveEntities);
    this.downstreamPassive = serverProcess;
  }



  private static class MessageContainer {
    public IMessageSenderWrapper sender;
    public byte[] message;
  }
}
