package org.terracotta.passthrough;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.CommonServerEntity;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.ServerEntityService;
import org.terracotta.entity.Service;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.passthrough.PassthroughServerMessageDecoder.MessageHandler;
import org.terracotta.persistence.IPersistentStorage;
import org.terracotta.persistence.KeyValueStorage;


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
  private long nextConsumerID;
  private PassthroughServiceRegistry platformServiceRegistry;
  private KeyValueStorage<Long, EntityData> persistedEntitiesByConsumerID;
  
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
    // Consumer IDs start at 0 since that is the one the platform gives itself.
    this.nextConsumerID = 0;
  }
  
  public void start() {
    // We can now get the service registry for the platform.
    this.platformServiceRegistry = getNextServiceRegistry();
    // See if we have persistence support.
    ServiceConfiguration<IPersistentStorage> persistenceConfiguration = new ServiceConfiguration<IPersistentStorage>() {
      @Override
      public Class<IPersistentStorage> getServiceType() {
        return IPersistentStorage.class;
      }
    };
    Service<IPersistentStorage> persistentStorageService = this.platformServiceRegistry.getService(persistenceConfiguration);
    if (null != persistentStorageService) {
      IPersistentStorage persistentStorage = persistentStorageService.get();
      try {
        persistentStorage.open();
      } catch (IOException e) {
        // Fall back to creating a new one since this probably means it doesn't exist and the Persitor has no notion of which
        // mode (open/create) it should prefer.
        try {
          persistentStorage.create();
        } catch (IOException e1) {
          // We are not expecting both to fail.
          Assert.unexpected(e1);
        }
      }
      // Note that we may want to persist the version, as well, but we currently have no way of exposing that difference, within the passthrough system, and it would require the creation of an almost completely-redundant container class.
      this.persistedEntitiesByConsumerID = persistentStorage.getKeyValueStorage("entities", Long.class, EntityData.class);
    }
    // And start the server thread.
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
    // TODO:  Find a way to cut the connections of any current task so that they can't send a response to the client.
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
    // Capture which consumerID we will use for this entity.
    long consumerID = this.nextConsumerID;
    ServerEntityService<?, ?> service = getServerEntityServiceForVersion(entityClassName, version);
    PassthroughServiceRegistry registry = getNextServiceRegistry();
    CommonServerEntity newEntity = createAndStoreEntity(serializedConfiguration, entityTuple, service, registry);
    // Tell the entity to create itself as something new.
    newEntity.createNew();
    // If we have a persistence layer, record this.
    if (null != this.persistedEntitiesByConsumerID) {
      EntityData data = new EntityData();
      data.className = entityClassName;
      data.version = version;
      data.entityName = entityName;
      data.configuration = serializedConfiguration;
      this.persistedEntitiesByConsumerID.put(consumerID, data);
    }
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

  @Override
  public void reconnect(final IMessageSenderWrapper sender, final long clientInstanceID, final String entityClassName, final String entityName) {
    final PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    
    // We need to get the lock, but we can't fail or wait, during the reconnect, so we handle that internally.
    // NOTE:  This use of "didRun" is a somewhat ugly way to avoid creating an entirely new Runnable class so we could ask
    // for the result but we are only using this in an assert, within this method, so it does maintain clarity.
    final boolean[] didRun = new boolean[1];
    Runnable onAcquire = new Runnable() {
      @Override
      public void run() {
        // Fetch the entity now that we have the read lock on the name.
        // Fetch should never be replicated and only handled on the active.
        Assert.assertTrue(null != PassthroughServerProcess.this.activeEntities);
        ActiveServerEntity entity = PassthroughServerProcess.this.activeEntities.get(entityTuple);
        if (null != entity) {
          PassthroughClientDescriptor clientDescriptor = sender.clientDescriptorForID(clientInstanceID);
          entity.connected(clientDescriptor);
          didRun[0] = true;
        } else {
          Assert.unexpected(new Exception("Entity not found in reconnect"));
          lockManager.releaseReadLock(entityTuple, sender.getClientOrigin(), clientInstanceID);
        }
      }
    };
    // The onAcquire callback will fetch the entity asynchronously.
    this.lockManager.acquireReadLock(entityTuple, sender.getClientOrigin(), clientInstanceID, onAcquire);
    Assert.assertTrue(didRun[0]);
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

  /**
   * Called upon restart to reload our entities from disk.
   * Note that this will do nothing if the server was not persistent.
   */
  @SuppressWarnings("deprecation")
  public void reloadEntities() {
    if (null != this.persistedEntitiesByConsumerID) {
      for (long consumerID : this.persistedEntitiesByConsumerID.keySet()) {
        // Create the registry for the entity.
        PassthroughServiceRegistry registry = new PassthroughServiceRegistry(consumerID, this.serviceProviderMap);
        // Construct the entity.
        EntityData entityData = this.persistedEntitiesByConsumerID.get(consumerID);
        ServerEntityService<?, ?> service = null;
        try {
          service = getServerEntityServiceForVersion(entityData.className, entityData.version);
        } catch (Exception e) {
          // We don't expect a version mismatch here or other failure in this test system.
          Assert.unexpected(e);
        }
        PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityData.className, entityData.entityName);
        CommonServerEntity newEntity = createAndStoreEntity(entityData.configuration, entityTuple, service, registry);
        // Tell the entity to load itself from storage.
        newEntity.loadExisting();
        
        // See if we need to bump up the next consumerID for future entities.
        if (consumerID >= this.nextConsumerID) {
          this.nextConsumerID = consumerID + 1;
        }
      }
    }
  }

  private PassthroughServiceRegistry getNextServiceRegistry() {
    long thisConsumerID = this.nextConsumerID;
    this.nextConsumerID += 1;
    return new PassthroughServiceRegistry(thisConsumerID, this.serviceProviderMap);
  }

  private ServerEntityService<?, ?> getServerEntityServiceForVersion(String entityClassName, long version) throws Exception {
    ServerEntityService<?, ?> service = getEntityServiceForClassName(entityClassName);
    if (service.getVersion() != version) {
      throw new Exception("Version mis-match");
    }
    return service;
  }

  private CommonServerEntity createAndStoreEntity(byte[] serializedConfiguration, PassthroughEntityTuple entityTuple, ServerEntityService<?, ?> service, PassthroughServiceRegistry registry) {
    CommonServerEntity newEntity = null;
    if (null != this.activeEntities) {
      ActiveServerEntity entity = service.createActiveEntity(registry, serializedConfiguration);
      this.activeEntities.put(entityTuple, entity);
      newEntity = entity;
    } else {
      PassiveServerEntity entity = service.createPassiveEntity(registry, serializedConfiguration);
      this.passiveEntities.put(entityTuple, entity);
      newEntity = entity;
    }
    return newEntity;
  }


  private static class MessageContainer {
    public IMessageSenderWrapper sender;
    public byte[] message;
  }

  private static class EntityData implements Serializable {
    private static final long serialVersionUID = 1L;
    public String className;
    public long version;
    public String entityName;
    public byte[] configuration;
  }
}
