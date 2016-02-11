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

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.CommonServerEntity;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.PassiveSynchronizationChannel;
import org.terracotta.entity.ServerEntityService;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.exception.EntityAlreadyExistsException;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityUserException;
import org.terracotta.exception.EntityVersionMismatchException;
import org.terracotta.monitoring.IMonitoringProducer;
import org.terracotta.monitoring.PlatformClientFetchedEntity;
import org.terracotta.monitoring.PlatformConnectedClient;
import org.terracotta.monitoring.PlatformEntity;
import org.terracotta.monitoring.PlatformMonitoringConstants;
import org.terracotta.passthrough.PassthroughBuiltInServiceProvider.DeferredEntityContainer;
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
  private Thread serverThread;
  private final List<MessageContainer> messageQueue;
  private final PassthroughLockManager lockManager;
  // Currently, for simplicity, we will resolve entities by name.
  // Technically, these should be resolved by class+name.
  // Note that only ONE of the active or passive entities will be non-null.
  private Map<PassthroughEntityTuple, CreationData> activeEntities;
  private Map<PassthroughEntityTuple, CreationData> passiveEntities;
  private final List<ServiceProvider> serviceProviders;
  private final List<PassthroughBuiltInServiceProvider> builtInServiceProviders;
  private PassthroughServerProcess downstreamPassive;
  private long nextConsumerID;
  private PassthroughServiceRegistry platformServiceRegistry;
  private KeyValueStorage<Long, EntityData> persistedEntitiesByConsumerID;
  
  // We need to hold onto any registered monitoring services to report client connection/disconnection events.
  private IMonitoringProducer serviceInterface;
  
  public PassthroughServerProcess(boolean isActiveMode) {
    this.entityServices = new Vector<ServerEntityService<?, ?>>();
    this.messageQueue = new Vector<MessageContainer>();
    this.lockManager = new PassthroughLockManager();
    this.activeEntities = (isActiveMode ? new HashMap<PassthroughEntityTuple, CreationData>() : null);
    this.passiveEntities = (isActiveMode ? null : new HashMap<PassthroughEntityTuple, CreationData>());
    this.serviceProviders = new Vector<ServiceProvider>();
    this.builtInServiceProviders = new Vector<PassthroughBuiltInServiceProvider>();
    // Consumer IDs start at 0 since that is the one the platform gives itself.
    this.nextConsumerID = 0;
  }
  
  @SuppressWarnings("deprecation")
  public void start(boolean shouldLoadStorage) {
    // We can now get the service registry for the platform.
    this.platformServiceRegistry = getNextServiceRegistry(null);
    // See if we have persistence support.
    IPersistentStorage persistentStorage = preparePersistentStorage(shouldLoadStorage);
    if (null != persistentStorage) {
      // Note that we may want to persist the version, as well, but we currently have no way of exposing that difference,
      // within the passthrough system, and it would require the creation of an almost completely-redundant container class.
      this.persistedEntitiesByConsumerID = persistentStorage.getKeyValueStorage("entities", Long.class, EntityData.class);
      
      // Load the entities.
      for (long consumerID : this.persistedEntitiesByConsumerID.keySet()) {
        // This is an entity consumer so we use the deferred container.
        DeferredEntityContainer container = new DeferredEntityContainer();
        // Create the registry for the entity.
        PassthroughServiceRegistry registry = new PassthroughServiceRegistry(consumerID, this.serviceProviders, this.builtInServiceProviders, container);
        // Construct the entity.
        EntityData entityData = this.persistedEntitiesByConsumerID.get(consumerID);        
        ServerEntityService<?, ?> service = null;
        try {
          service = getServerEntityServiceForVersion(entityData.className, entityData.entityName, entityData.version);
        } catch (Exception e) {
          // We don't expect a version mismatch here or other failure in this test system.
          Assert.unexpected(e);
        }
        PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityData.className, entityData.entityName);
        CommonServerEntity<?, ?> newEntity = createAndStoreEntity(entityData.className, entityData.entityName, entityData.version, entityData.configuration, entityTuple, service, registry);
        // We can now store the entity into the deferred container.
        container.entity = newEntity;
        container.codec = service.getMessageCodec();
        // Tell the entity to load itself from storage.
        newEntity.loadExisting();
        
        // See if we need to bump up the next consumerID for future entities.
        if (consumerID >= this.nextConsumerID) {
          this.nextConsumerID = consumerID + 1;
        }
      }
      
      // Get the persisted transaction order in case of a reconnect
      this.persistedEntitiesByConsumerID = persistentStorage.getKeyValueStorage("entities", Long.class, EntityData.class);
    }
    
    // Look up the service interface the platform will use to publish events.
    this.serviceInterface = this.platformServiceRegistry.getService(new ServiceConfiguration<IMonitoringProducer>() {
      @Override
      public Class<IMonitoringProducer> getServiceType() {
        return IMonitoringProducer.class;
      }});
    if (null != this.serviceInterface) {
      // Create the root of the platform tree.
      this.serviceInterface.addNode(new String[0], PlatformMonitoringConstants.PLATFORM_ROOT_NAME, null);
      // Create the root of the client subtree.
      this.serviceInterface.addNode(PlatformMonitoringConstants.PLATFORM_PATH, PlatformMonitoringConstants.CLIENTS_ROOT_NAME, null);
      // Create the root of the entity subtree.
      this.serviceInterface.addNode(PlatformMonitoringConstants.PLATFORM_PATH, PlatformMonitoringConstants.ENTITIES_ROOT_NAME, null);
      // Create the root of the client-entity fetch subtree.
      this.serviceInterface.addNode(PlatformMonitoringConstants.PLATFORM_PATH, PlatformMonitoringConstants.FETCHED_ROOT_NAME, null);
      // Create the initial server state.
      this.serviceInterface.addNode(PlatformMonitoringConstants.PLATFORM_PATH, PlatformMonitoringConstants.STATE_NODE_NAME, PlatformMonitoringConstants.SERVER_STATE_STOPPED);
    }
    // And start the server thread.
    startServerThreadRunning();
  }

  public void resumeMessageProcessing() {
    startServerThreadRunning();
  }

  private void startServerThreadRunning() {
    Assert.assertTrue(null == this.serverThread);
    Assert.assertTrue(!this.isRunning);
    this.serverThread = new Thread(new Runnable() {
      @Override
      public void run() {
        runServerThread();
      }
    });
    this.serverThread.setUncaughtExceptionHandler(PassthroughUncaughtExceptionHandler.sharedInstance);
    this.isRunning = true;
    // Set our state.
    if (null != this.serviceInterface) {
      String stateValue = (null != this.activeEntities) ? PlatformMonitoringConstants.SERVER_STATE_ACTIVE : PlatformMonitoringConstants.SERVER_STATE_PASSIVE;
      this.serviceInterface.removeNode(PlatformMonitoringConstants.PLATFORM_PATH, PlatformMonitoringConstants.STATE_NODE_NAME);
      this.serviceInterface.addNode(PlatformMonitoringConstants.PLATFORM_PATH, PlatformMonitoringConstants.STATE_NODE_NAME, stateValue);
    }
    this.serverThread.start();
  }

  private IPersistentStorage preparePersistentStorage(boolean shouldLoadStorage) {
    ServiceConfiguration<IPersistentStorage> persistenceConfiguration = new ServiceConfiguration<IPersistentStorage>() {
      @Override
      public Class<IPersistentStorage> getServiceType() {
        return IPersistentStorage.class;
      }
    };
    IPersistentStorage persistentStorage = this.platformServiceRegistry.getService(persistenceConfiguration);
    if (shouldLoadStorage) {
      // Note that we are told to load storage in the cases where the system is restarting.  In that case, we MUST have
      // persistent storage or else the restart doesn't even make sense:  we would have no way of reconnecting the clients.
      Assert.assertTrue(null != persistentStorage);
      try {
        persistentStorage.open();
      } catch (IOException e) {
        Assert.unexpected(e);
      }
    } else {
      if (null != persistentStorage) {
        try {
          persistentStorage.create();
        } catch (IOException e) {
          // We are not expecting both to fail.
          Assert.unexpected(e);
        }
      }
    }
    return persistentStorage;
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
      // Set our state.
      if (null != this.serviceInterface) {
        this.serviceInterface.removeNode(PlatformMonitoringConstants.PLATFORM_PATH, PlatformMonitoringConstants.STATE_NODE_NAME);
        this.serviceInterface.addNode(PlatformMonitoringConstants.PLATFORM_PATH, PlatformMonitoringConstants.STATE_NODE_NAME, PlatformMonitoringConstants.SERVER_STATE_STOPPED);
      }
      this.notifyAll();
    }
    try {
      this.serverThread.join();
    } catch (InterruptedException e) {
      Assert.unexpected(e);
    }
    // We also want to clear the message queue, in case anything else is still sitting there.
    this.messageQueue.clear();
    this.serverThread = null;
  }

  public synchronized void sendMessageToServer(final PassthroughConnection sender, byte[] message, final boolean isResend) {
    Assert.assertTrue(this.isRunning);
    MessageContainer container = new MessageContainer();
    container.sender = new IMessageSenderWrapper() {
      @Override
      public void sendAck(PassthroughMessage ack) {
        sender.sendMessageToClient(PassthroughServerProcess.this, ack.asSerializedBytes());
      }
      @Override
      public void sendComplete(PassthroughMessage complete) {
        sender.sendMessageToClient(PassthroughServerProcess.this, complete.asSerializedBytes());
      }
      @Override
      public PassthroughClientDescriptor clientDescriptorForID(long clientInstanceID) {
        return new PassthroughClientDescriptor(PassthroughServerProcess.this, sender, clientInstanceID);
      }
      @Override
      public long getClientOriginID() {
        return sender.getUniqueConnectionID();
      }
      @Override
      public boolean shouldTolerateCreateDestroyDuplication() {
        // We will handle this if the message is re-sent.
        return isResend;
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

  private void runServerThread() {
    Thread.currentThread().setName("Server thread isActive: " + ((null != this.activeEntities) ? "active" : "passive"));
    MessageContainer toRun = getNextMessage();
    while (null != toRun) {
      IMessageSenderWrapper sender = toRun.sender;
      byte[] message = toRun.message;
      serverThreadHandleMessage(sender, message);
      
      toRun = getNextMessage();
    }
  }
  
  private synchronized MessageContainer getNextMessage() {
    MessageContainer toRun = null;
    while (this.isRunning && this.messageQueue.isEmpty()) {
      try {
        this.wait();
      } catch (InterruptedException e) {
        Assert.unexpected(e);
      }
    }
    if (!this.messageQueue.isEmpty()) {
      toRun = this.messageQueue.remove(0);
    }
    return toRun;
  }
  
  private void serverThreadHandleMessage(IMessageSenderWrapper sender, byte[] message) {
    // Called on the server thread to handle a message.
    PassthroughMessageCodec.Decoder<Void> decoder = new PassthroughServerMessageDecoder(this, this.downstreamPassive, sender, message);
    PassthroughMessageCodec.decodeRawMessage(decoder, message);
  }

  @Override
  public byte[] invoke(IMessageSenderWrapper sender, long clientInstanceID, String entityClassName, String entityName, byte[] payload) throws EntityException {
    final PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    byte[] response = null;
    if (null != this.activeEntities) {
      // Invoke on active.
      CreationData data = this.activeEntities.get(entityTuple);
      if (null != data) {
        ActiveServerEntity entity = data.getActive();
        PassthroughClientDescriptor clientDescriptor = sender.clientDescriptorForID(clientInstanceID);
        response = sendActiveInvocation(entityClassName, entityName, clientDescriptor, entity, data.codec, payload);
      } else {
        throw new EntityNotFoundException(entityClassName, entityName);
      }
    } else {
      // Invoke on passive.
      CreationData data = this.passiveEntities.get(entityTuple);
      if (null != data) {
        PassiveServerEntity entity = data.getPassive();
        // There is no return type in the passive case.
        sendPassiveInvocation(entityClassName, entityName, entity, data.codec, payload);
      } else {
        throw new EntityNotFoundException(entityClassName, entityName);
      }
    }
    return response;
  }

  private <M extends EntityMessage, R extends EntityResponse> byte[] sendActiveInvocation(String className, String entityName, ClientDescriptor clientDescriptor, ActiveServerEntity<M, R> entity, MessageCodec<M, R> codec, byte[] payload) throws EntityUserException {
    R response = entity.invoke(clientDescriptor, deserialize(className, entityName, codec, payload));
    return serializeResponse(className, entityName, codec, response);
  }

  private <M extends EntityMessage, R extends EntityResponse> void sendPassiveInvocation(String className, String entityName, PassiveServerEntity<M, R> entity, MessageCodec<M, R> codec, byte[] payload) throws EntityUserException {
    entity.invoke(deserialize(className, entityName, codec, payload));
  }

  private <M extends EntityMessage, R extends EntityResponse> void sendPassiveSyncPayload(String className, String entityName, PassiveServerEntity<M, R> entity, MessageCodec<M, R> codec, int concurrencyKey, byte[] payload) throws EntityUserException {
    entity.invoke(deserializeForSync(className, entityName, codec, concurrencyKey, payload));
  }
  
  private <M extends EntityMessage, R extends EntityResponse> M deserialize(String className, String entityName, final MessageCodec<M, R> codec, final byte[] payload) throws EntityUserException {
    return runWithHelper(className, entityName, new CodecHelper<M>() {
      @Override
      public M run() throws MessageCodecException {
        return codec.deserialize(payload);
      }
    });
  }
  
  private <M extends EntityMessage, R extends EntityResponse> M deserializeForSync(String className, String entityName, final MessageCodec<M, R> codec, final int concurrencyKey, final byte[] payload) throws EntityUserException {
    return runWithHelper(className, entityName, new CodecHelper<M>() {
      @Override
      public M run() throws MessageCodecException {
        return codec.deserializeForSync(concurrencyKey, payload);
      }
    });
  }
  
  private <M extends EntityMessage, R extends EntityResponse> byte[] serializeResponse(String className, String entityName, final MessageCodec<M, R> codec, final R response) throws EntityUserException {
    return runWithHelper(className, entityName, new CodecHelper<byte[]>() {
      @Override
      public byte[] run() throws MessageCodecException {
        return codec.serialize(response);
      }
    });
  }

  private static interface CodecHelper<R> {
    public R run() throws MessageCodecException;
  }
  private <R> R runWithHelper(String className, String entityName, CodecHelper<R> helper) throws EntityUserException {
    R message = null;
    try {
      message = helper.run();
    } catch (MessageCodecException deserializationException) {
      throw new EntityUserException(className, entityName, deserializationException);
    } catch (RuntimeException e) {
      // We first want to wrap this in a codec exception to convey the meaning of where this happened.
      MessageCodecException deserializationException = new MessageCodecException("Runtime exception in deserializer", e);
      throw new EntityUserException(className, entityName, deserializationException);
    }
    return message;
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
        EntityException error = null;
        // Fetch should never be replicated and only handled on the active.
        Assert.assertTrue(null != PassthroughServerProcess.this.activeEntities);
        CreationData entityData = PassthroughServerProcess.this.activeEntities.get(entityTuple);
        if (null != entityData) {
          ActiveServerEntity<?, ?> entity = entityData.getActive();
          ServerEntityService<?, ?> service = getEntityServiceForClassName(entityClassName);
          long expectedVersion = service.getVersion();
          if (expectedVersion == version) {
            PassthroughClientDescriptor clientDescriptor = sender.clientDescriptorForID(clientInstanceID);
            entity.connected(clientDescriptor);
            config = entityData.configuration;
            
            if (null != PassthroughServerProcess.this.serviceInterface) {
              // Record that this entity has been fetched by this client.
              String clientIdentifier = clientIdentifierForService(sender.getClientOriginID());
              String entityIdentifier = entityIdentifierForService(entityClassName, entityName);
              PlatformClientFetchedEntity record = new PlatformClientFetchedEntity(clientIdentifier, entityIdentifier);
              String fetchIdentifier = fetchIdentifierForService(clientIdentifier, entityIdentifier);
              PassthroughServerProcess.this.serviceInterface.addNode(PlatformMonitoringConstants.FETCHED_PATH, fetchIdentifier, record);
            }
          } else {
            error = new EntityVersionMismatchException(entityClassName, entityName, expectedVersion, version);
          }
        } else {
          error = new EntityNotFoundException(entityClassName, entityName);
        }
        // Release the lock if there was a failure.
        if (null != error) {
          lockManager.releaseReadLock(entityTuple, sender.getClientOriginID(), clientInstanceID);
        }
        onFetch.onFetchComplete(config, error);
      }
      
    };
    // The onAcquire callback will fetch the entity asynchronously.
    this.lockManager.acquireReadLock(entityTuple, sender.getClientOriginID(), clientInstanceID, onAcquire);
  }

  @Override
  public void release(IMessageSenderWrapper sender, long clientInstanceID, String entityClassName, String entityName) throws EntityException {
    final PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    // Release should never be replicated and only handled on the active.
    Assert.assertTrue(null != this.activeEntities);
    CreationData data = this.activeEntities.get(entityTuple);
    if (null != data) {
      ActiveServerEntity entity = data.getActive();
      PassthroughClientDescriptor clientDescriptor = sender.clientDescriptorForID(clientInstanceID);
      entity.disconnected(clientDescriptor);
      this.lockManager.releaseReadLock(entityTuple, sender.getClientOriginID(), clientInstanceID);
      
      if (null != PassthroughServerProcess.this.serviceInterface) {
        // Record that this entity has been released by this client.
        String clientIdentifier = clientIdentifierForService(sender.getClientOriginID());
        String entityIdentifier = entityIdentifierForService(entityClassName, entityName);
        String fetchIdentifier = fetchIdentifierForService(clientIdentifier, entityIdentifier);
        PassthroughServerProcess.this.serviceInterface.removeNode(PlatformMonitoringConstants.FETCHED_PATH, fetchIdentifier);
      }
    } else {
      throw new EntityNotFoundException(entityClassName, entityName);
    }
  }

  @Override
  public void create(String entityClassName, String entityName, long version, byte[] serializedConfiguration) throws EntityException {
    PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    if (((null != this.activeEntities) && this.activeEntities.containsKey(entityTuple))
      || ((null != this.passiveEntities) && this.passiveEntities.containsKey(entityTuple))) {
      throw new EntityAlreadyExistsException(entityClassName, entityName);
    }
    // Capture which consumerID we will use for this entity.
    long consumerID = this.nextConsumerID;
    ServerEntityService<?, ?> service = getServerEntityServiceForVersion(entityClassName, entityName, version);
    // This is an entity consumer so we use the deferred container.
    DeferredEntityContainer container = new DeferredEntityContainer();
    PassthroughServiceRegistry registry = getNextServiceRegistry(container);
    CommonServerEntity<?, ?> newEntity = createAndStoreEntity(entityClassName, entityName, version, serializedConfiguration, entityTuple, service, registry);
    container.entity = newEntity;
    container.codec = service.getMessageCodec();
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
    
    if (null != this.serviceInterface) {
      // Record this new entity.
      boolean isActive = (null != this.activeEntities);
      PlatformEntity record = new PlatformEntity(entityClassName, entityName, isActive);
      String entityIdentifier = entityIdentifierForService(entityClassName, entityName);
      this.serviceInterface.addNode(PlatformMonitoringConstants.ENTITIES_PATH, entityIdentifier, record);
    }
  }

  @Override
  public void destroy(String entityClassName, String entityName) throws EntityException {
    PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    boolean didDestroy = false;
    if (null != this.activeEntities) {
      CreationData entityData = this.activeEntities.remove(entityTuple);
      didDestroy = (null != entityData);
    } else {
      CreationData entityData = this.passiveEntities.remove(entityTuple);
      didDestroy = (null != entityData);
    }
    if (!didDestroy) {
      throw new EntityNotFoundException(entityClassName, entityName);
    }
    
    if (null != this.serviceInterface) {
      // Record that we destroyed the entity.
      String entityIdentifier = entityIdentifierForService(entityClassName, entityName);
      this.serviceInterface.removeNode(PlatformMonitoringConstants.ENTITIES_PATH, entityIdentifier);
    }
  }

  @Override
  public void acquireWriteLock(IMessageSenderWrapper sender, String entityClassName, String entityName, Runnable onAcquire) {
    PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    this.lockManager.acquireWriteLock(entityTuple, sender.getClientOriginID(), onAcquire);
  }

  @Override
  public boolean tryAcquireWriteLock(IMessageSenderWrapper sender, String entityClassName, String entityName) {
    PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    return this.lockManager.tryAcquireWriteLock(entityTuple, sender.getClientOriginID());
  }

  @Override
  public void releaseWriteLock(IMessageSenderWrapper sender, String entityClassName, String entityName) {
    PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    this.lockManager.releaseWriteLock(entityTuple, sender.getClientOriginID());
  }

  @Override
  public void restoreWriteLock(IMessageSenderWrapper sender, String entityClassName, String entityName, Runnable onAcquire) {
    PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    this.lockManager.restoreWriteLock(entityTuple, sender.getClientOriginID(), onAcquire);
  }

  @Override
  public void reconnect(final IMessageSenderWrapper sender, final long clientInstanceID, final String entityClassName, final String entityName, final byte[] extendedData) {
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
        CreationData entityData = PassthroughServerProcess.this.activeEntities.get(entityTuple);
        if (null != entityData) {
          PassthroughClientDescriptor clientDescriptor = sender.clientDescriptorForID(clientInstanceID);
          entityData.reconnect(clientDescriptor, extendedData);
          didRun[0] = true;
        } else {
          Assert.unexpected(new Exception("Entity not found in reconnect"));
          lockManager.releaseReadLock(entityTuple, sender.getClientOriginID(), clientInstanceID);
        }
      }
    };
    // The onAcquire callback will fetch the entity asynchronously.
    this.lockManager.acquireReadLock(entityTuple, sender.getClientOriginID(), clientInstanceID, onAcquire);
    Assert.assertTrue(didRun[0]);
  }

  @Override
  public void syncEntityStart(IMessageSenderWrapper sender, String entityClassName, String entityName) throws EntityException {
    // Sync only makes sense on passive.
    Assert.assertTrue(null != this.passiveEntities);
    
    final PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    CreationData data = this.passiveEntities.get(entityTuple);
    if (null != data) {
      PassiveServerEntity<?, ?> entity = data.getPassive();
      entity.startSyncEntity();
    } else {
      throw new EntityNotFoundException(entityClassName, entityName);
    }
  }

  @Override
  public void syncEntityEnd(IMessageSenderWrapper sender, String entityClassName, String entityName) throws EntityException {
    // Sync only makes sense on passive.
    Assert.assertTrue(null != this.passiveEntities);
    
    final PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    CreationData data = this.passiveEntities.get(entityTuple);
    if (null != data) {
      PassiveServerEntity<?, ?> entity = data.getPassive();
      entity.endSyncEntity();
    } else {
      throw new EntityNotFoundException(entityClassName, entityName);
    }
  }

  @Override
  public void syncEntityKeyStart(IMessageSenderWrapper sender, String entityClassName, String entityName, int concurrencyKey) throws EntityException {
    // Sync only makes sense on passive.
    Assert.assertTrue(null != this.passiveEntities);
    
    final PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    CreationData data = this.passiveEntities.get(entityTuple);
    if (null != data) {
      PassiveServerEntity<?, ?> entity = data.getPassive();
      entity.startSyncConcurrencyKey(concurrencyKey);
    } else {
      throw new EntityNotFoundException(entityClassName, entityName);
    }
  }

  @Override
  public void syncEntityKeyEnd(IMessageSenderWrapper sender, String entityClassName, String entityName, int concurrencyKey) throws EntityException {
    // Sync only makes sense on passive.
    Assert.assertTrue(null != this.passiveEntities);
    
    final PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    CreationData data = this.passiveEntities.get(entityTuple);
    if (null != data) {
      PassiveServerEntity<?, ?> entity = data.getPassive();
      entity.endSyncConcurrencyKey(concurrencyKey);
    } else {
      throw new EntityNotFoundException(entityClassName, entityName);
    }
  }

  @Override
  public void syncPayload(IMessageSenderWrapper sender, String entityClassName, String entityName, int concurrencyKey, byte[] payload) throws EntityException {
    // Sync only makes sense on passive.
    Assert.assertTrue(null != this.passiveEntities);
    
    final PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    CreationData data = this.passiveEntities.get(entityTuple);
    if (null != data) {
      PassiveServerEntity entity = data.getPassive();
      sendPassiveSyncPayload(entityClassName, entityName, entity, data.codec, concurrencyKey, payload);
    } else {
      throw new EntityNotFoundException(entityClassName, entityName);
    }
  }


  private String clientIdentifierForService(long connectionID) {
    return "" + connectionID;
  }

  private String entityIdentifierForService(String entityClassName, String entityName) {
    return entityClassName + entityName;
  }

  private String fetchIdentifierForService(String clientIdentifier, String entityIdentifier) {
    return clientIdentifier + entityIdentifier;
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

  public void registerBuiltInServiceProvider(PassthroughBuiltInServiceProvider serviceProvider, ServiceProviderConfiguration providerConfiguration) {
    this.builtInServiceProviders.add(serviceProvider);
  }

  public void registerServiceProvider(ServiceProvider serviceProvider, ServiceProviderConfiguration providerConfiguration) {
    // We run the initializer right away.
    boolean didInitialize = serviceProvider.initialize(providerConfiguration);
    Assert.assertTrue(didInitialize);
    this.serviceProviders.add(serviceProvider);
  }

  public void setDownstreamPassiveServerProcess(PassthroughServerProcess serverProcess) {
    // Make sure that we are active and they are passive.
    Assert.assertTrue(null != this.activeEntities);
    Assert.assertTrue(null != serverProcess.passiveEntities);
    this.downstreamPassive = serverProcess;
    
    // Synchronize any entities we have.
    // NOTE:  This synchronization implementation is relatively simplistic and we may require a more substantial
    // implementation, in the future, to support concurrent replication/synchronization ordering concerns, multiple
    // concurrency queues/threads, and the ordering corner-cases which arise with those concerns.
    for (Map.Entry<PassthroughEntityTuple, CreationData> entry : this.activeEntities.entrySet()) {
      CreationData<?, ?> value = entry.getValue();
      final String entityClassName = value.entityClassName;
      final String entityName = value.entityName;
      // State that we will start to synchronize the entity.
      PassthroughMessage entityStart = PassthroughMessageCodec.createSyncEntityStartMessage(entityClassName, entityName, value.version, value.configuration);
      PassthroughInterserverInterlock wrapper = new PassthroughInterserverInterlock(null);
      this.downstreamPassive.sendMessageToServerFromActive(wrapper, entityStart.asSerializedBytes());
      wrapper.waitForComplete();
      // Walk all the concurrency keys for this entity.
      for (final Integer oneKey : value.getConcurrency().getKeysForSynchronization()) {
        // State that we will start to synchronize the key.
        PassthroughMessage keyStart = PassthroughMessageCodec.createSyncEntityKeyStartMessage(entityClassName, entityName, oneKey);
        wrapper = new PassthroughInterserverInterlock(null);
        this.downstreamPassive.sendMessageToServerFromActive(wrapper, keyStart.asSerializedBytes());
        wrapper.waitForComplete();
        // Send all the data.
        value.synchronizeToPassive(this.downstreamPassive, oneKey);
        // State that we are done synchronizing the key.
        PassthroughMessage keyEnd = PassthroughMessageCodec.createSyncEntityKeyEndMessage(entityClassName, entityName, oneKey);
        wrapper = new PassthroughInterserverInterlock(null);
        this.downstreamPassive.sendMessageToServerFromActive(wrapper, keyEnd.asSerializedBytes());
        wrapper.waitForComplete();
      }
      // State that we are done synchronizing the entity.
      PassthroughMessage entityEnd = PassthroughMessageCodec.createSyncEntityEndMessage(entityClassName, entityName);
      wrapper = new PassthroughInterserverInterlock(null);
      this.downstreamPassive.sendMessageToServerFromActive(wrapper, entityEnd.asSerializedBytes());
      wrapper.waitForComplete();
    }
  }

  public void promoteToActive() {
    // Make sure that we are currently passive.
    Assert.assertTrue(null != this.passiveEntities);
    // Make us active and promote all passive entities.
    this.downstreamPassive = null;
    this.activeEntities = new HashMap<PassthroughEntityTuple, CreationData>();
    
    // We need to create the entities as active but note that we would already have persisted this data so only create the
    // actual instances, don't go through the full creation path.
    for (Map.Entry<PassthroughEntityTuple, CreationData> entry : this.passiveEntities.entrySet()) {
      CreationData data = entry.getValue();
      CreationData newData = new CreationData(data.entityClassName, data.entityName, data.version, data.configuration, data.registry, data.service, true);
      newData.getActive().loadExisting();
      this.activeEntities.put(entry.getKey(), newData);
    }
    
    // Clear our passives.
    this.passiveEntities = null;
  }

  /**
   * Called when a new connection has been established to this server.
   * 
   * @param connection The new connection object.
   * @param connectionID A unique ID for the connection.
   */
  public void connectConnection(PassthroughConnection connection, long connectionID) {
    if (null != this.serviceInterface) {
      // We just fake up the network data.
      InetAddress localHost = null;
      try {
        localHost = InetAddress.getLocalHost();
      } catch (UnknownHostException e) {
        // We don't have handling for this.
        Assert.unexpected(e);
      }
      int serverPort = 1;
      int clientPort = 2;
      PlatformConnectedClient clientDescription = new PlatformConnectedClient(localHost, serverPort, localHost, clientPort);
      String nodeName = clientIdentifierForService(connectionID);
      this.serviceInterface.addNode(PlatformMonitoringConstants.CLIENTS_PATH, nodeName, clientDescription);
    }
  }

  /**
   * Called when a connection to the server has closed.
   * 
   * @param connection The closed connection object.
   * @param connectionID A unique ID for the connection.
   */
  public void disconnectConnection(PassthroughConnection connection, long connectionID) {
    if (null != this.serviceInterface) {
      String nodeName = clientIdentifierForService(connectionID);
      this.serviceInterface.removeNode(PlatformMonitoringConstants.CLIENTS_PATH, nodeName);
    }
  }

  private PassthroughServiceRegistry getNextServiceRegistry(DeferredEntityContainer container) {
    long thisConsumerID = this.nextConsumerID;
    this.nextConsumerID += 1;
    return new PassthroughServiceRegistry(thisConsumerID, this.serviceProviders, this.builtInServiceProviders, container);
  }

  private ServerEntityService<?, ?> getServerEntityServiceForVersion(String entityClassName, String entityName, long version) throws EntityVersionMismatchException, EntityNotProvidedException {
    ServerEntityService<?, ?> service = getEntityServiceForClassName(entityClassName);
    if(service == null) {
      throw new EntityNotProvidedException(entityClassName, entityName);
    }
    long expectedVersion = service.getVersion();
    if (expectedVersion != version) {
      throw new EntityVersionMismatchException(entityClassName, entityName, expectedVersion, version);
    }
    return service;
  }

  private CommonServerEntity<?, ?> createAndStoreEntity(String entityClassName, String entityName, long version, byte[] serializedConfiguration, PassthroughEntityTuple entityTuple, ServerEntityService<?, ?> service, PassthroughServiceRegistry registry) {
    CommonServerEntity<?, ?> newEntity = null;
    if (null != this.activeEntities) {
      CreationData data = new CreationData(entityClassName, entityName, version, serializedConfiguration, registry, service, true);
      this.activeEntities.put(entityTuple, data);
      newEntity = data.getActive();
    } else {
      CreationData data = new CreationData(entityClassName, entityName, version, serializedConfiguration, registry, service, false);
      this.passiveEntities.put(entityTuple, data);
      newEntity = data.getPassive();
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

  private static class CreationData<M extends EntityMessage, R extends EntityResponse> {
    public final String entityClassName;
    public final String entityName;
    public final long version;
    public byte[] configuration;
    public final PassthroughServiceRegistry registry;
    public final ServerEntityService<M, R> service;
    public CommonServerEntity<M, R> entityInstance;
    public final MessageCodec<M, R> codec;
    public ConcurrencyStrategy<M> concurrency; 
    public boolean isActive;
    
    public CreationData(String entityClassName, String entityName, long version, byte[] configuration, PassthroughServiceRegistry registry, ServerEntityService<M, R> service, boolean isActive) {
      this.entityClassName = entityClassName;
      this.entityName = entityName;
      this.version = version;
      this.configuration = configuration;
      this.registry = registry;
      this.service = service;
      this.codec = service.getMessageCodec();
      this.entityInstance = (isActive) ? service.createActiveEntity(registry, configuration) : service.createPassiveEntity(registry, configuration);
      this.concurrency = (isActive) ? service.getConcurrencyStrategy(configuration) : null;
      this.isActive = isActive;
    }
      
    M deserialize(byte[] data) {
      try {
        return this.codec.deserialize(data);
      } catch (MessageCodecException ce) {
        throw new RuntimeException(ce);
      }
    }
    
    void reconnect(ClientDescriptor clientDescriptor, byte[] data) {
      getActive().connected(clientDescriptor);
      getActive().handleReconnect(clientDescriptor, data);
    }
      
    public ActiveServerEntity<M, R> getActive() {
      return ActiveServerEntity.class.cast(entityInstance);
    }
    
    public PassiveServerEntity<M, R> getPassive() {
      return PassiveServerEntity.class.cast(entityInstance);
    }    
    
    public ConcurrencyStrategy<M> getConcurrency() {
      return concurrency;
    }
    
    public void synchronizeToPassive(final PassthroughServerProcess passive, final int key) {
      getActive().synchronizeKeyToPassive(new PassiveSynchronizationChannel<R>() {
          @Override
          public void synchronizeToPassive(R payload) {
            PassthroughMessage payloadMessage = PassthroughMessageCodec.createSyncPayloadMessage(entityClassName, entityName, key, serialize(key, payload));
            PassthroughInterserverInterlock wrapper = new PassthroughInterserverInterlock(null);
            passive.sendMessageToServerFromActive(wrapper, payloadMessage.asSerializedBytes());
            wrapper.waitForComplete();
          }
        }, key);
    }
    
    private byte[] serialize(int key, R response) {
      try {
        return codec.serializeForSync(key, response);
      } catch (MessageCodecException me) {
        throw new RuntimeException(me);
      }
    }
  }  
}
