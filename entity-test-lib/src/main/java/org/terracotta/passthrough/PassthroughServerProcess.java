package org.terracotta.passthrough;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.terracotta.entity.ActiveServerEntity;
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
      Assert.unexpected(e);
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
    Thread.currentThread().setName("Server thread");
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
          Assert.unexpected(e);
        }
      }
    }
  }
  
  private void serverThreadHandleMessage(PassthroughConnection sender, byte[] message) {
    // Called on the server thread to handle a message.
    PassthroughMessageCodec.Decoder<Void> decoder = new PassthroughServerMessageDecoder(this, sender);
    PassthroughMessageCodec.decodeRawMessage(decoder, message);
  }

  @Override
  public byte[] invoke(PassthroughClientDescriptor clientDescriptor, String entityClassName, String entityName, byte[] payload) throws Exception {
    byte[] response = null;
    ActiveServerEntity entity = this.createdEntities.get(entityName);
    if (null != entity) {
      response = entity.invoke(clientDescriptor, payload);
    } else {
      throw new Exception("Not fetched");
    }
    return response;
  }

  @Override
  public byte[] fetch(PassthroughClientDescriptor clientDescriptor, String entityClassName, String entityName, long version) throws Exception {
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

  @Override
  public void release(PassthroughClientDescriptor clientDescriptor, String entityClassName, String entityName) throws Exception {
    ActiveServerEntity entity = this.createdEntities.get(entityName);
    if (null != entity) {
      entity.disconnected(clientDescriptor);
    } else {
      throw new Exception("Not found");
    }
  }

  @Override
  public void create(String entityClassName, String entityName, long version, byte[] serializedConfiguration) throws Exception {
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

  @Override
  public void destroy(String entityClassName, String entityName) throws Exception {
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
