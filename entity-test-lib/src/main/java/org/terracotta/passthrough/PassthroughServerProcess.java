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
  private final PassthroughLockManager lockManager;
  // Currently, for simplicity, we will resolve entities by name.
  // Technically, these should be resolved by class+name.
  // Note that only ONE of the active or passive entities will be non-null.
  private final Map<PassthroughEntityTuple, ActiveServerEntity> activeEntities;
  private final Map<PassthroughEntityTuple, PassiveServerEntity> passiveEntities;
  private final Map<Class<?>, ServiceProvider> serviceProviderMap;
  private PassthroughServerProcess downstreamPassive;
  
  public PassthroughServerProcess(boolean isActiveMode) {
    this.isRunning = true;
    this.entityServices = new Vector<ServerEntityService<?, ?>>();
    this.serverThread = new Thread(new Runnable() {
      @Override
      public void run() {
        runServerThread();
      }
    });
    this.messageQueue = new Vector<MessageContainer>();
    this.lockManager = new PassthroughLockManager();
    this.activeEntities = (isActiveMode ? new HashMap<PassthroughEntityTuple, ActiveServerEntity>() : null);
    this.passiveEntities = (isActiveMode ? null : new HashMap<PassthroughEntityTuple, PassiveServerEntity>());
    this.serviceProviderMap = new HashMap<Class<?>, ServiceProvider>();
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

  public synchronized void sendMessageToServer(final PassthroughConnection sender, byte[] message) {
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
      @Override
      public PassthroughConnection getClientOrigin() {
        return sender;
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
  public byte[] invoke(IMessageSenderWrapper sender, long clientInstanceID, String entityClassName, String entityName, byte[] payload) throws Exception {
    final PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    byte[] response = null;
    if (null != this.activeEntities) {
      // Invoke on active.
      ActiveServerEntity entity = this.activeEntities.get(entityTuple);
      if (null != entity) {
        PassthroughClientDescriptor clientDescriptor = sender.clientDescriptorForID(clientInstanceID);
        response = entity.invoke(clientDescriptor, payload);
      } else {
        throw new Exception("Not fetched");
      }
    } else {
      // Invoke on passive.
      PassiveServerEntity entity = this.passiveEntities.get(entityTuple);
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
  public void fetch(final IMessageSenderWrapper sender, final long clientInstanceID, final String entityClassName, final String entityName, final long version, final IFetchResult onFetch) {
    final PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    // We need to get the read lock before the fetch so we wrap the actual fetch in the onAcquire.
    // Note that this could technically be a little simpler by getting the lock after the fetch but this keeps the semantic
    // ordering we would need if the process were to be made multi-threaded or re-factored.
    Runnable onAcquire = new Runnable() {
      @Override
      public void run() {
        // Fetch the entity now that we have the read lock on the name.
        byte[] config = null;
        Exception error = null;
        // Fetch should never be replicated and only handled on the active.
        Assert.assertTrue(null != PassthroughServerProcess.this.activeEntities);
        ActiveServerEntity entity = PassthroughServerProcess.this.activeEntities.get(entityTuple);
        if (null != entity) {
          ServerEntityService<?, ?> service = getEntityServiceForClassName(entityClassName);
          if (service.getVersion() == version) {
            PassthroughClientDescriptor clientDescriptor = sender.clientDescriptorForID(clientInstanceID);
            entity.connected(clientDescriptor);
            config = entity.getConfig();
          } else {
            error = new Exception("Version mis-match");
          }
        } else {
          error = new Exception("Not found");
        }
        // Release the lock if there was a failure.
        if (null != error) {
          lockManager.releaseReadLock(entityTuple, sender.getClientOrigin(), clientInstanceID);
        }
        onFetch.onFetchComplete(config, error);
      }
      
    };
    // The onAcquire callback will fetch the entity asynchronously.
    this.lockManager.acquireReadLock(entityTuple, sender.getClientOrigin(), clientInstanceID, onAcquire);
  }

  @Override
  public void release(IMessageSenderWrapper sender, long clientInstanceID, String entityClassName, String entityName) throws Exception {
    final PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    // Release should never be replicated and only handled on the active.
    Assert.assertTrue(null != this.activeEntities);
    ActiveServerEntity entity = this.activeEntities.get(entityTuple);
    if (null != entity) {
      PassthroughClientDescriptor clientDescriptor = sender.clientDescriptorForID(clientInstanceID);
      entity.disconnected(clientDescriptor);
      this.lockManager.releaseReadLock(entityTuple, sender.getClientOrigin(), clientInstanceID);
    } else {
      throw new Exception("Not found");
    }
  }

  @Override
  public void create(String entityClassName, String entityName, long version, byte[] serializedConfiguration) throws Exception {
    PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    if (((null != this.activeEntities) && this.activeEntities.containsKey(entityTuple))
      || ((null != this.passiveEntities) && this.passiveEntities.containsKey(entityTuple))) {
      throw new Exception("Already exists");
    }
    ServerEntityService<?, ?> service = getEntityServiceForClassName(entityClassName);
    if (service.getVersion() != version) {
      throw new Exception("Version mis-match");
    }
    // TODO:  Make this ID real.
    long consumerID = 0;
    CommonServerEntity newEntity = null;
    if (null != this.activeEntities) {
      PassthroughServiceRegistry registry = new PassthroughServiceRegistry(consumerID, this.serviceProviderMap);
      ActiveServerEntity entity = service.createActiveEntity(registry, serializedConfiguration);
      this.activeEntities.put(entityTuple, entity);
      newEntity = entity;
    } else {
      PassthroughServiceRegistry registry = new PassthroughServiceRegistry(consumerID, this.serviceProviderMap);
      PassiveServerEntity entity = service.createPassiveEntity(registry, serializedConfiguration);
      this.passiveEntities.put(entityTuple, entity);
      newEntity = entity;
    }
    // Tell the entity to create itself as something new.
    newEntity.createNew();
  }

  @Override
  public void destroy(String entityClassName, String entityName) throws Exception {
    PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    CommonServerEntity entity = (null != this.activeEntities)
        ? this.activeEntities.remove(entityTuple)
        : this.passiveEntities.remove(entityTuple);
    if (null != entity) {
      // Success.
    } else {
      throw new Exception("Not found");
    }
  }

  @Override
  public void acquireWriteLock(IMessageSenderWrapper sender, String entityClassName, String entityName, Runnable onAcquire) {
    PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    this.lockManager.acquireWriteLock(entityTuple, sender.getClientOrigin(), onAcquire);
  }

  @Override
  public void releaseWriteLock(IMessageSenderWrapper sender, String entityClassName, String entityName) {
    PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    this.lockManager.releaseWriteLock(entityTuple, sender.getClientOrigin());
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
