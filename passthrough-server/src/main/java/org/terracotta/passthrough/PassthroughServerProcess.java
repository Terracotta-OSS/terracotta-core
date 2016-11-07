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

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.CommonServerEntity;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.PassiveSynchronizationChannel;
import org.terracotta.entity.EntityServerService;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ExecutionStrategy;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.entity.SyncMessageCodec;
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
import org.terracotta.monitoring.PlatformServer;
import org.terracotta.monitoring.ServerState;
import org.terracotta.passthrough.PassthroughEmulatedStorageServiceProvider.RegistryLookup;
import org.terracotta.passthrough.PassthroughImplementationProvidedServiceProvider.DeferredEntityContainer;
import org.terracotta.passthrough.PassthroughServerMessageDecoder.LifeCycleMessageHandler;
import org.terracotta.passthrough.PassthroughServerMessageDecoder.MessageHandler;
import org.terracotta.persistence.IPlatformPersistence;


/**
 * The wrapper around the thread running as the "server process", within the PassthroughServer.
 * Note that this currently handles not only message processing, but also message execution.
 * In the future, message execution will likely be split out into other threads to better support entity read-write locking
 * and also test concurrency strategy.
 */
public class PassthroughServerProcess implements MessageHandler, PassthroughDumper {
  private static final String ENTITIES_FILE_NAME = "entities.map";
  
  private final String serverName;
  private final int bindPort;
  private final int groupPort;
  
  private static final AtomicInteger CLIENT_PORT = new AtomicInteger(49152);  //  current recommended start value of ephemeral ports
  
  private final int processID;
  private boolean isRunning;
  private final List<EntityServerService<?, ?>> entityServices;
  private Thread serverThread;
  private final List<PassthroughMessageContainer> messageQueue;
  // Currently, for simplicity, we will resolve entities by name.
  // Technically, these should be resolved by class+name.
  // Note that only ONE of the active or passive entities will be non-null.
  // WARNING:  We may be missing some synchronization around access to activeEntities and passiveEntities due to the
  //  interaction between normal message flow and passive server attachment (as they are mutually asynchronous).  This
  //  is why create/destroy/attachPassive are synchronized since they all directly interact with this entry set.
  private Map<PassthroughEntityTuple, CreationData<?, ?>> activeEntities;
  private Map<PassthroughEntityTuple, CreationData<?, ?>> passiveEntities;
  private final Map<Long, DeferredEntityContainer> consumerToLiveContainerMap;
  // The service providers offered by the user.
  private final List<ServiceProvider> serviceProviders;
  // The override service providers offered by the user.
  private final List<ServiceProvider> overrideServiceProviders;
  // The service providers offered by the server's implementation.
  private final List<PassthroughImplementationProvidedServiceProvider> implementationProvidedServiceProviders;
  // Note that we will set the service provider collections into a read-only mode as we try to create a registry over them, to catch bugs.
  private boolean serviceProvidersReadOnly;
  private Set<PassthroughServerProcess> downstreamPassives = new HashSet<PassthroughServerProcess>();
  private long nextConsumerID;
  private PassthroughServiceRegistry platformServiceRegistry;
  private IPlatformPersistence platformPersistence;
  private HashMap<Long, EntityData> persistedEntitiesByConsumerIDMap;
  private LifeCycleMessageHandler lifeCycleMessageHandler;
  private final PassthroughRetirementManager retirementManager;
  private PassthroughTransactionOrderManager transactionOrderManager;
  
  private static final AtomicInteger processIdGen = new AtomicInteger(0);
  
  // We need to hold onto any registered monitoring services to report client connection/disconnection events.
  private IMonitoringProducer serviceInterface;
  private PlatformServer serverInfo;
  
  // Special flag used to change behavior when we are receiving re-sends:  we don't want to run them until we seem them all.
  private boolean isHandlingResends;


  public PassthroughServerProcess(String serverName, int bindPort, int groupPort, boolean isActiveMode) {
    this.serverName = serverName;
    this.bindPort = bindPort;
    this.groupPort = groupPort;
    this.entityServices = new Vector<EntityServerService<?, ?>>();
    this.messageQueue = new Vector<PassthroughMessageContainer>();
    this.activeEntities = (isActiveMode ? new HashMap<PassthroughEntityTuple, CreationData<?, ?>>() : null);
    this.passiveEntities = (isActiveMode ? null : new HashMap<PassthroughEntityTuple, CreationData<?, ?>>());
    this.consumerToLiveContainerMap = new HashMap<Long, DeferredEntityContainer>();
    this.serviceProviders = new Vector<ServiceProvider>();
    this.overrideServiceProviders = new Vector<ServiceProvider>();
    this.implementationProvidedServiceProviders = new Vector<PassthroughImplementationProvidedServiceProvider>();
    // Consumer IDs start at 0 since that is the one the platform gives itself.
    this.nextConsumerID = 0;
    this.processID = processIdGen.incrementAndGet();
    this.retirementManager = new PassthroughRetirementManager();
  }

  @SuppressWarnings("unchecked")
  public void start(boolean shouldLoadStorage) {
    // Make sure that we install the in-memory registry, if needed.
    boolean isStorageInstalled = false;
    for (ServiceProvider provider : this.serviceProviders) {
      if (provider.getProvidedServiceTypes().contains(IPlatformPersistence.class)) {
        isStorageInstalled = true;
        break;
      }
    }
    if (!isStorageInstalled) {
      PassthroughNullPlatformStorageServiceProvider nullPlatformStorageServiceProvider = new PassthroughNullPlatformStorageServiceProvider();
      ServiceProviderConfiguration config = new ServiceProviderConfiguration() {
        @Override
        public Class<? extends ServiceProvider> getServiceProviderType() {
          return PassthroughNullPlatformStorageServiceProvider.class;
        }};
      nullPlatformStorageServiceProvider.initialize(config, new PassthroughPlatformConfiguration(this.serverName));
      this.serviceProviders.add(nullPlatformStorageServiceProvider);
    }
    // XXX: Temporary inclusion of the IPersistentStorage emulation on IPlatformPersistence.
    PassthroughEmulatedStorageServiceProvider legacyProvider = new PassthroughEmulatedStorageServiceProvider();
    PassthroughEmulatedStorageServiceProvider.Configuration legacyConfig = new PassthroughEmulatedStorageServiceProvider.Configuration(new RegistryLookup() {
      @Override
      public PassthroughServiceRegistry getRegistryForConsumerID(long consumerID) {
        return new PassthroughServiceRegistry(null, null, consumerID, PassthroughServerProcess.this.serviceProviders, PassthroughServerProcess.this.overrideServiceProviders, PassthroughServerProcess.this.implementationProvidedServiceProviders, null);
      }});
    legacyProvider.initialize(legacyConfig, new PassthroughPlatformConfiguration(this.serverName));
    this.serviceProviders.add(legacyProvider);
    
    // We can now get the service registry for the platform.
    this.platformServiceRegistry = getNextServiceRegistry(null, null, null);
    
    // Look up our persistence support (which might be in-memory-only).
    this.platformPersistence = this.platformServiceRegistry.getService(new BasicServiceConfiguration<IPlatformPersistence>(IPlatformPersistence.class));
    Assert.assertTrue(null != this.platformPersistence);
    // Note that we may want to persist the version, as well, but we currently have no way of exposing that difference,
    // within the passthrough system, and it would require the creation of an almost completely-redundant container class.
    try {
      this.persistedEntitiesByConsumerIDMap = (HashMap<Long, EntityData>) (shouldLoadStorage ? platformPersistence.loadDataElement(ENTITIES_FILE_NAME) : null);
    } catch (IOException e1) {
      Assert.unexpected(e1);
    }
    // This could be null if there was no file or we shouldn't load.
    if (null == this.persistedEntitiesByConsumerIDMap) {
      this.persistedEntitiesByConsumerIDMap = new HashMap<Long, EntityData>();
    }
    
    // Load the transaction order.
    this.transactionOrderManager = new PassthroughTransactionOrderManager(platformPersistence, shouldLoadStorage);
    
    // Load the entities.
    for (long consumerID : this.persistedEntitiesByConsumerIDMap.keySet()) {
      // This is an entity consumer so we use the deferred container.
      DeferredEntityContainer container = new DeferredEntityContainer();
      this.consumerToLiveContainerMap.put(consumerID, container);
      EntityData entityData = this.persistedEntitiesByConsumerIDMap.get(consumerID);
      // Create the registry for the entity.
      PassthroughServiceRegistry registry = new PassthroughServiceRegistry(entityData.className, entityData.entityName, consumerID, this.serviceProviders, this.overrideServiceProviders, this.implementationProvidedServiceProviders, container);
      // Construct the entity.
      EntityServerService<?, ?> service = null;
      try {
        service = getServerEntityServiceForVersion(entityData.className, entityData.entityName, entityData.version);
      } catch (Exception e) {
        // We don't expect a version mismatch here or other failure in this test system.
        Assert.unexpected(e);
      }
      PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityData.className, entityData.entityName);
      CommonServerEntity<?, ?> newEntity = createAndStoreEntity(entityData.className, entityData.entityName, entityData.version, entityData.configuration, entityTuple, service, registry, consumerID);
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
    
    // We want to create the tracking for life-cycle transactions, so that we correctly handle duplicated re-sends.
    this.lifeCycleMessageHandler = new PassthroughLifeCycleHandler(platformPersistence, shouldLoadStorage);
    
    // Look up the service interface the platform will use to publish events.
    this.serviceInterface = this.platformServiceRegistry.getService(new BasicServiceConfiguration<IMonitoringProducer>(IMonitoringProducer.class));
    if (null != this.serviceInterface) {
      // Create the root of the platform tree.
      this.serviceInterface.addNode(new String[0], PlatformMonitoringConstants.PLATFORM_ROOT_NAME, null);
      // Create the root of the client subtree.
      this.serviceInterface.addNode(PlatformMonitoringConstants.PLATFORM_PATH, PlatformMonitoringConstants.CLIENTS_ROOT_NAME, null);
      // Create the root of the entity subtree.
      this.serviceInterface.addNode(PlatformMonitoringConstants.PLATFORM_PATH, PlatformMonitoringConstants.ENTITIES_ROOT_NAME, null);
      // Create the root of the client-entity fetch subtree.
      this.serviceInterface.addNode(PlatformMonitoringConstants.PLATFORM_PATH, PlatformMonitoringConstants.FETCHED_ROOT_NAME, null);
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
    // We only use the retirement manager if we are the active.
    if (null != this.activeEntities) {
      this.retirementManager.setServerThread(this.serverThread);
    } else {
      // Null this out so that attempts to use it in passive mode will assert.
      this.retirementManager.setServerThread(null);
    }
    this.isRunning = true;
    // We want to now set the server info for this instance.
    this.serverInfo = new PlatformServer(
        getSafeServerName(), //  server name
        "localhost", // hostname
        "127.0.0.1", // hostAddress
        "0.0.0.0", // bindAddress
        bindPort, //  bindPort but just fake with processID
        groupPort, // groupPort
        "Version Passthrough 5.0.0-SNAPSHOT", //  version
        "Build ID - " + new Random().nextInt(), // build
        System.currentTimeMillis() // start time
    );
//  set instance
    if (null != this.serviceInterface) {
      String stateValue = (null != this.activeEntities) ? PlatformMonitoringConstants.SERVER_STATE_ACTIVE : PlatformMonitoringConstants.SERVER_STATE_UNINITIALIZED;
// Set state.
      long timestamp = System.currentTimeMillis();
      this.serviceInterface.addNode(PlatformMonitoringConstants.PLATFORM_PATH, PlatformMonitoringConstants.STATE_NODE_NAME, new ServerState(stateValue, timestamp, (this.activeEntities != null) ? timestamp : -1));
    }
    this.serverThread.start();
  }

  private String getSafeServerName() {
    return serverName == null ? "server" + processID : serverName;
  }
  
  private void setStateSynchronizing(IMonitoringProducer tracker) {
// Set state.
    if (tracker != null) {
      long timestamp = System.currentTimeMillis();
      tracker.addNode(PlatformMonitoringConstants.PLATFORM_PATH, PlatformMonitoringConstants.STATE_NODE_NAME, new ServerState(PlatformMonitoringConstants.SERVER_STATE_SYNCHRONIZING, timestamp, (this.activeEntities != null) ? timestamp : -1));
    }
  }

  /**
   * This method is only called during start-up of the PassthroughServer, since it goes directly into the instance.  Later,
   * all method calls must go through the sendMessageToServer entry-point.
   * @param service
   */
  public void registerEntityService(EntityServerService<?, ?> service) {
    this.entityServices.add(service);
  }
  
  public void stop() {
    // Shutdown can't happen while handling resends.
    Assert.assertTrue(!this.isHandlingResends);
    
    // TODO:  Find a way to cut the connections of any current task so that they can't send a response to the client.
    synchronized(this) {
      this.isRunning = false;
      // Set our state.
      if (null != this.serviceInterface) {
        this.serviceInterface.removeNode(PlatformMonitoringConstants.PLATFORM_PATH, PlatformMonitoringConstants.STATE_NODE_NAME);
      }
      this.notifyAll();
    }
    try {
// multiple paths to shutdown.  This can happen multiple times without a new thread being created
      if (this.serverThread != null) {
        this.serverThread.join();
      }
    } catch (InterruptedException e) {
      Assert.unexpected(e);
    }
    // We also want to clear the message queue, in case anything else is still sitting there.
    this.messageQueue.clear();
    this.serverThread = null;
  }

  public void shutdownServices() {
    // We now stop the services BEFORE the server has actually stopped (this is largely to address the problem where the
    //  background thread in PassthroughMessengerService might still be trying to enqueue events in a server which has
    //  already stopped).
    Assert.assertTrue(null != this.serverThread);
    
    // Finally, see if any of the ServiceProvider instances are Closeable.
    // NOTE:  This is a SPECIAL behavior exposed by the passthrough server to allow any "open state" (file descriptors,
    //  etc) to be closed by a ServiceProvider implementation.  This is ONLY called by the passthrough server since the
    //  multi-process server has no notion of clean shutdown, meaning that the analogous "clean-up" is realized through
    //  the server crashing.
    for (ServiceProvider provider : this.serviceProviders) {
      if (provider instanceof Closeable) {
        try {
          ((Closeable)provider).close();
        } catch (IOException e) {
          // We do not permit exceptions here since that would imply a bug in the service being tested.
          Assert.unexpected(e);
        }
      }
    }
  }

  public synchronized void sendMessageToServer(final PassthroughConnection sender, byte[] message) {
    // If the server shut down, throw IllegalStateException
    if (!this.isRunning) {
      throw new IllegalStateException("Connection already closed");
    }

    PassthroughMessageContainer container = new PassthroughMessageContainer();
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
      public void sendRetire(PassthroughMessage retired) {
        handleMessageRetirement(null, sender, retired);
      }
      @Override
      public PassthroughClientDescriptor clientDescriptorForID(long clientInstanceID) {
        return new PassthroughClientDescriptor(PassthroughServerProcess.this, sender, clientInstanceID);
      }
      @Override
      public long getClientOriginID() {
        return sender.getUniqueConnectionID();
      }
    };
    container.message = message;
    if (this.isHandlingResends) {
      long connectionID = sender.getNewInstanceID();
      long transactionID = PassthroughMessageCodec.decodeTransactionIDFromRawMessage(message);
      this.transactionOrderManager.handleResend(connectionID, transactionID, container);
    } else {
      this.messageQueue.add(container);
      this.notifyAll();
    }
  }

  public synchronized void sendMessageToActiveFromInsideActive(final EntityMessage newMessage, PassthroughMessage passthroughMessage) {
    // It is possible that this happens when we have already been told to shut down so we want to drop it, in that case.
    if (this.isRunning) {
      // This can only be called on the active server.
      Assert.assertTrue(null != this.activeEntities);
      // We must be given a message.
      Assert.assertTrue(null != passthroughMessage);
      // This entry-point is only used in the cases where the message already exists.
      Assert.assertTrue(null != newMessage);
      // When handling re-sends, we are effectively paused so this shouldn't happen.
      Assert.assertTrue(!this.isHandlingResends);
      
      // Defer the current message, blocking it on the new one.
      this.retirementManager.deferCurrentMessage(newMessage);
      PassthroughMessageContainer container = new PassthroughMessageContainer();
      container.sender = new IMessageSenderWrapper() {
        @Override
        public void sendAck(PassthroughMessage ack) {
          // Do nothing on ack.
        }
        @Override
        public void sendComplete(PassthroughMessage complete) {
          // Do nothing on complete.
        }
        @Override
        public void sendRetire(PassthroughMessage retired) {
          handleMessageRetirement(newMessage, null, retired);
        }
        @Override
        public PassthroughClientDescriptor clientDescriptorForID(long clientInstanceID) {
          // This can't reasonably be asked.
          return null;
        }
        @Override
        public long getClientOriginID() {
          // This can't reasonably be asked.
          return -1;
        }
      };
      container.message = passthroughMessage.asSerializedBytes();
      this.messageQueue.add(container);
      this.notifyAll();
    } else {
      System.err.println("WARNING:  Dropping internally-generated message since server is shutting down");
    }
  }

  private void handleMessageRetirement(EntityMessage messageRun, PassthroughConnection sender, PassthroughMessage retired) {
    // We only send retirement messages if we are the active.
    if (null != this.activeEntities) {
      // Ask the retirement manager what to do with this.
      PassthroughRetirementManager.RetirementTuple tuple = new PassthroughRetirementManager.RetirementTuple(sender, retired.asSerializedBytes());
      List<PassthroughRetirementManager.RetirementTuple> messagesToRetire = PassthroughServerProcess.this.retirementManager.retireableListAfterMessageDone(messageRun, tuple);
      for (PassthroughRetirementManager.RetirementTuple oneTuple : messagesToRetire) {
        if (null != oneTuple.sender) {
          oneTuple.sender.sendMessageToClient(this, oneTuple.response);
        }
      }
    }
  }

  public synchronized void sendMessageToServerFromActive(IMessageSenderWrapper senderCallback, byte[] message) {
    // Passives don't care whether a message is a re-send, or not.
    Assert.assertTrue(!this.isHandlingResends);
    
    PassthroughMessageContainer container = new PassthroughMessageContainer();
    container.sender = senderCallback;
    container.message = message;
    this.messageQueue.add(container);
    this.notifyAll();
  }

  private void runServerThread() {
    Thread.currentThread().setName("Server thread isActive: " + ((null != this.activeEntities) ? "active" : "passive"));
    PassthroughMessageContainer toRun = getNextMessage();
    while (null != toRun) {
      IMessageSenderWrapper sender = toRun.sender;
      byte[] message = toRun.message;
      serverThreadHandleMessage(sender, message);
      
      toRun = getNextMessage();
    }
  }
  
  private synchronized PassthroughMessageContainer getNextMessage() {
    PassthroughMessageContainer toRun = null;
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
    PassthroughMessageCodec.Decoder<Void> decoder = new PassthroughServerMessageDecoder(this, this.transactionOrderManager, this.lifeCycleMessageHandler, this.downstreamPassives, sender, message);
    PassthroughMessageCodec.decodeRawMessage(decoder, message);
  }

  @Override
  public byte[] invoke(IMessageSenderWrapper sender, long clientInstanceID, String entityClassName, String entityName, byte[] payload) throws EntityException {
    final PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    byte[] response = null;
    if (null != this.activeEntities) {
      // Invoke on active.
      CreationData<?, ?> data = this.activeEntities.get(entityTuple);
      if (null != data) {
        PassthroughClientDescriptor clientDescriptor = sender.clientDescriptorForID(clientInstanceID);
        response = sendActiveInvocation(entityClassName, entityName, clientDescriptor, data, payload);
      } else {
        throw new EntityNotFoundException(entityClassName, entityName);
      }
    } else {
      // Invoke on passive.
      CreationData<?, ?> data = this.passiveEntities.get(entityTuple);
      if (null != data) {
        // There is no return type in the passive case.
        sendPassiveInvocation(entityClassName, entityName, data, payload);
      } else {
        throw new EntityNotFoundException(entityClassName, entityName);
      }
    }
    return response;
  }

  private <M extends EntityMessage, R extends EntityResponse> byte[] sendActiveInvocation(String className, String entityName, ClientDescriptor clientDescriptor, CreationData<M, R> data, byte[] payload) throws EntityUserException {
    ActiveServerEntity<M, R> entity = data.getActive();
    MessageCodec<M, R> codec = data.messageCodec;
    M msg = deserialize(className, entityName, codec, payload);
    if (data.executionStrategy.getExecutionLocation(msg).runOnActive()) {
      R response = entity.invoke(clientDescriptor, msg);
      return serializeResponse(className, entityName, codec, response);
    } else {
      return new byte[0];
    }
  }

  private <M extends EntityMessage, R extends EntityResponse> void sendPassiveInvocation(String className, String entityName, CreationData<M, R> data, byte[] payload) throws EntityUserException {
    PassiveServerEntity<M, R> entity = data.getPassive();
    MessageCodec<M, R> codec = data.messageCodec;
    M msg = deserialize(className, entityName, codec, payload);
    if (data.executionStrategy.getExecutionLocation(msg).runOnPassive()) {
      entity.invoke(msg);
    }
  }

  private <M extends EntityMessage, R extends EntityResponse> void sendPassiveSyncPayload(String className, String entityName, CreationData<M, R> data, int concurrencyKey, byte[] payload) throws EntityUserException {
    PassiveServerEntity<M, R> entity = data.getPassive();
    SyncMessageCodec<M> codec = data.syncMessageCodec;
    entity.invoke(deserializeForSync(className, entityName, codec, concurrencyKey, payload));
  }
  
  private <M extends EntityMessage, R extends EntityResponse> M deserialize(String className, String entityName, final MessageCodec<M, R> codec, final byte[] payload) throws EntityUserException {
    return runWithHelper(className, entityName, new CodecHelper<M>() {
      @Override
      public M run() throws MessageCodecException {
        return codec.decodeMessage(payload);
      }
    });
  }
  
  private <M extends EntityMessage, R extends EntityResponse> M deserializeForSync(String className, String entityName, final SyncMessageCodec<M> codec, final int concurrencyKey, final byte[] payload) throws EntityUserException {
    return runWithHelper(className, entityName, new CodecHelper<M>() {
      @Override
      public M run() throws MessageCodecException {
        return codec.decode(concurrencyKey, payload);
      }
    });
  }
  
  private <M extends EntityMessage, R extends EntityResponse> byte[] serializeResponse(String className, String entityName, final MessageCodec<M, R> codec, final R response) throws EntityUserException {
    return runWithHelper(className, entityName, new CodecHelper<byte[]>() {
      @Override
      public byte[] run() throws MessageCodecException {
        return codec.encodeResponse(response);
      }
    });
  }

  @Override
  public void dump() {
    System.out.println("Existing entities:");
    if(this.persistedEntitiesByConsumerIDMap != null) {
      for(EntityData entityData : this.persistedEntitiesByConsumerIDMap.values()) {
        System.out.println("\t" + entityData.className + ":" + entityData.entityName + ":" + entityData.version);
      }
    }
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
    // Fetch the entity now that we have the read lock on the name.
    byte[] config = null;
    EntityException error = null;
    // Fetch should never be replicated and only handled on the active.
    Assert.assertTrue(null != PassthroughServerProcess.this.activeEntities);
    CreationData<?, ?> entityData = PassthroughServerProcess.this.activeEntities.get(entityTuple);
    PassthroughClientDescriptor clientDescriptor = sender.clientDescriptorForID(clientInstanceID);
    if (null != entityData && entityData.reference(clientDescriptor)) {
      ActiveServerEntity<?, ?> entity = entityData.getActive();
      EntityServerService<?, ?> service = getEntityServiceForClassName(entityClassName);
      long expectedVersion = service.getVersion();
      if (expectedVersion == version) {
        config = entityData.configuration;

        if (null != PassthroughServerProcess.this.serviceInterface) {
          // Record that this entity has been fetched by this client.
          String clientIdentifier = clientIdentifierForService(sender.getClientOriginID());
          String entityIdentifier = entityIdentifierForService(entityClassName, entityName);
          PlatformClientFetchedEntity record = new PlatformClientFetchedEntity(clientIdentifier, entityIdentifier, clientDescriptor);
          String fetchIdentifier = fetchIdentifierForService(clientIdentifier, entityIdentifier);
          PassthroughServerProcess.this.serviceInterface.addNode(PlatformMonitoringConstants.FETCHED_PATH, fetchIdentifier, record);
        }
//  connected call must happen after a possible modification to monitoring tree.  
        entity.connected(clientDescriptor);
      } else {
        error = new EntityVersionMismatchException(entityClassName, entityName, expectedVersion, version);
      }
    } else {
      error = new EntityNotFoundException(entityClassName, entityName);
    }
// Release the reference if there was a failure.
    if (entityData != null && error != null) {
      entityData.release(clientDescriptor);
    }
    
    onFetch.onFetchComplete(config, error);
  }

  @Override
  public void release(IMessageSenderWrapper sender, long clientInstanceID, String entityClassName, String entityName) throws EntityException {
    final PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    // Release should never be replicated and only handled on the active.
    Assert.assertTrue(null != this.activeEntities);
    CreationData<?, ?> data = this.activeEntities.get(entityTuple);
    if (null != data) {
      ActiveServerEntity<?, ?> entity = data.getActive();
      PassthroughClientDescriptor clientDescriptor = sender.clientDescriptorForID(clientInstanceID);
      entity.disconnected(clientDescriptor);
      data.release(clientDescriptor);
      
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
  public synchronized void create(String entityClassName, String entityName, long version, byte[] serializedConfiguration) throws EntityException {
    PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    if (this.activeEntities != null) {
      CreationData<?, ?> shell = this.activeEntities.get(entityTuple);
      if (shell != null && !shell.isDestroyed) {
        throw new EntityAlreadyExistsException(entityClassName, entityName);
      }
    } else {
      if (this.passiveEntities.containsKey(entityTuple)) {
        throw new EntityAlreadyExistsException(entityClassName, entityName);
      }
    }
    // Capture which consumerID we will use for this entity.
    long consumerID = this.nextConsumerID;
    EntityServerService<?, ?> service = getServerEntityServiceForVersion(entityClassName, entityName, version);
    // This is an entity consumer so we use the deferred container.
    DeferredEntityContainer container = new DeferredEntityContainer();
    this.consumerToLiveContainerMap.put(consumerID, container);
    PassthroughServiceRegistry registry = getNextServiceRegistry(entityClassName, entityName, container);
    // Before we create the entity, we want to store this information regarding class and name, since that might be needed by a service in its start up.
    CommonServerEntity<?, ?> newEntity = createAndStoreEntity(entityClassName, entityName, version, serializedConfiguration, entityTuple, service, registry, consumerID);
    container.entity = newEntity;
    container.codec = service.getMessageCodec();
    // Tell the entity to create itself as something new.
    newEntity.createNew();
    // If we have a persistence layer, record this.
    EntityData data = new EntityData();
    data.className = entityClassName;
    data.version = version;
    data.entityName = entityName;
    data.configuration = serializedConfiguration;
    this.persistedEntitiesByConsumerIDMap.put(consumerID, data);
    try {
      this.platformPersistence.storeDataElement(ENTITIES_FILE_NAME, this.persistedEntitiesByConsumerIDMap);
    } catch (IOException e) {
      Assert.unexpected(e);
    }
    
    if (null != this.serviceInterface) {
      // Record this new entity.
      boolean isActive = (null != this.activeEntities);
      PlatformEntity record = new PlatformEntity(entityClassName, entityName, consumerID, isActive);
      String entityIdentifier = entityIdentifierForService(entityClassName, entityName);
      this.serviceInterface.addNode(PlatformMonitoringConstants.ENTITIES_PATH, entityIdentifier, record);
    }
  }
  
  @Override
  public byte[] reconfigure(String entityClassName, String entityName, long version, byte[] serializedConfiguration) throws EntityException {
    PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    CreationData<?, ?> entityData = (this.activeEntities != null) ? this.activeEntities.get(entityTuple) : this.passiveEntities.get(entityTuple);
    return entityData.reconfigure(serializedConfiguration);
  }
  
  @Override
  public synchronized boolean destroy(String entityClassName, String entityName) throws EntityException {
    PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    boolean success = false;
// Look up the entity.
    CreationData<?, ?> entityData = null;
    if (null != this.activeEntities) {
      entityData = this.activeEntities.get(entityTuple);
    } else {
      entityData = this.passiveEntities.remove(entityTuple);
    }
    // If we found it, destroy it.  Otherwise, throw that we didn't find it.
    if (null != entityData && !entityData.isDestroyed) {
      success = entityData.destroy();
      if (success) {
        // If we found it, by any means, we expect that it still has a deferred container and we need to remove that and
        //  null the entity so that implementation-provided services know it is gone.
        Assert.assertTrue(entityData.consumerID > 0);
        DeferredEntityContainer container = this.consumerToLiveContainerMap.remove(entityData.consumerID);
        Assert.assertTrue(null != container);
        container.entity = null;
      }
      if (success && null != this.activeEntities) {
        boolean didRemove = false;
        if (entityData.equals(this.activeEntities.get(entityTuple))) {
          this.activeEntities.remove(entityTuple);
          didRemove = true;
        }
        Assert.assertTrue(didRemove);
      }
    } else {
      throw new EntityNotFoundException(entityClassName, entityName);
    }

    if (success && null != this.serviceInterface) {
      // Record that we destroyed the entity.
      String entityIdentifier = entityIdentifierForService(entityClassName, entityName);
      this.serviceInterface.removeNode(PlatformMonitoringConstants.ENTITIES_PATH, entityIdentifier);
    }
    return success;
  }

  @Override
  public void reconnect(final IMessageSenderWrapper sender, final long clientInstanceID, final String entityClassName, final String entityName, final byte[] extendedData) {
    final PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    // Fetch the entity now that we have the read lock on the name.
    // Fetch should never be replicated and only handled on the active.
    Assert.assertTrue(null != PassthroughServerProcess.this.activeEntities);
    CreationData<?, ?> entityData = PassthroughServerProcess.this.activeEntities.get(entityTuple);
    if (null != entityData) {
      PassthroughClientDescriptor clientDescriptor = sender.clientDescriptorForID(clientInstanceID);
      entityData.reconnect(clientInstanceID, clientDescriptor, extendedData);
    } else {
      Assert.unexpected(new Exception("Entity not found in reconnect"));
    }
  }

  @Override
  public void syncEntityStart(IMessageSenderWrapper sender, String entityClassName, String entityName) throws EntityException {
    // Sync only makes sense on passive.
    Assert.assertTrue(null != this.passiveEntities);
    
    final PassthroughEntityTuple entityTuple = new PassthroughEntityTuple(entityClassName, entityName);
    CreationData<?, ?> data = this.passiveEntities.get(entityTuple);
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
    CreationData<?, ?> data = this.passiveEntities.get(entityTuple);
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
    CreationData<?, ?> data = this.passiveEntities.get(entityTuple);
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
    CreationData<?, ?> data = this.passiveEntities.get(entityTuple);
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
    CreationData<?, ?> data = this.passiveEntities.get(entityTuple);
    if (null != data) {
      sendPassiveSyncPayload(entityClassName, entityName, data, concurrencyKey, payload);
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

  private EntityServerService<?, ?> getEntityServiceForClassName(String entityClassName) {
    EntityServerService<?, ?> foundService = null;
    for (EntityServerService<?, ?> service : this.entityServices) {
      if (service.handlesEntityType(entityClassName)) {
        Assert.assertTrue(null == foundService);
        foundService = service;
      }
    }
    return foundService;
  }

  public void registerImplementationProvidedServiceProvider(PassthroughImplementationProvidedServiceProvider serviceProvider, ServiceProviderConfiguration providerConfiguration) {
    Assert.assertTrue(!this.serviceProvidersReadOnly);
    this.implementationProvidedServiceProviders.add(serviceProvider);
  }

  public void registerServiceProvider(ServiceProvider serviceProvider, ServiceProviderConfiguration providerConfiguration) {
    Assert.assertTrue(!this.serviceProvidersReadOnly);
    // We run the initializer right away.
    boolean didInitialize = serviceProvider.initialize(providerConfiguration, new PassthroughPlatformConfiguration(this.serverName));
    Assert.assertTrue(didInitialize);
    this.serviceProviders.add(serviceProvider);
  }

  public void registerOverrideServiceProvider(ServiceProvider serviceProvider, ServiceProviderConfiguration providerConfiguration) {
    registerServiceProvider(serviceProvider, providerConfiguration);
    this.overrideServiceProviders.add(serviceProvider);
  }

  public synchronized void addDownstreamPassiveServerProcess(PassthroughServerProcess serverProcess) {
    // Make sure that we are active and they are passive.
    Assert.assertTrue(null != this.activeEntities);
    Assert.assertTrue(null != serverProcess.passiveEntities);
    this.downstreamPassives.add(serverProcess);
    serverProcess.setStateSynchronizing(this.serviceInterface);
    // Synchronize any entities we have.
    // NOTE:  This synchronization implementation is relatively simplistic and we may require a more substantial
    // implementation, in the future, to support concurrent replication/synchronization ordering concerns, multiple
    // concurrency queues/threads, and the ordering corner-cases which arise with those concerns.
    for (Map.Entry<PassthroughEntityTuple, CreationData<?, ?>> entry : this.activeEntities.entrySet()) {
      CreationData<?, ?> value = entry.getValue();
      final String entityClassName = value.entityClassName;
      final String entityName = value.entityName;
      // State that we will start to synchronize the entity.
      PassthroughMessage entityStart = PassthroughMessageCodec.createSyncEntityStartMessage(entityClassName, entityName, value.version, value.configuration);
      PassthroughInterserverInterlock wrapper = new PassthroughInterserverInterlock(null);
      serverProcess.sendMessageToServerFromActive(wrapper, entityStart.asSerializedBytes());
      wrapper.waitForComplete();
      // Walk all the concurrency keys for this entity.
      for (final Integer oneKey : value.getConcurrency().getKeysForSynchronization()) {
        // State that we will start to synchronize the key.
        PassthroughMessage keyStart = PassthroughMessageCodec.createSyncEntityKeyStartMessage(entityClassName, entityName, oneKey);
        wrapper = new PassthroughInterserverInterlock(null);
        serverProcess.sendMessageToServerFromActive(wrapper, keyStart.asSerializedBytes());
        wrapper.waitForComplete();
        // Send all the data.
        value.synchronizeToPassive(serverProcess, oneKey);
        // State that we are done synchronizing the key.
        PassthroughMessage keyEnd = PassthroughMessageCodec.createSyncEntityKeyEndMessage(entityClassName, entityName, oneKey);
        wrapper = new PassthroughInterserverInterlock(null);
        serverProcess.sendMessageToServerFromActive(wrapper, keyEnd.asSerializedBytes());
        wrapper.waitForComplete();
      }
      // State that we are done synchronizing the entity.
      PassthroughMessage entityEnd = PassthroughMessageCodec.createSyncEntityEndMessage(entityClassName, entityName);
      wrapper = new PassthroughInterserverInterlock(null);
      serverProcess.sendMessageToServerFromActive(wrapper, entityEnd.asSerializedBytes());
      wrapper.waitForComplete();
    }
  }

  public synchronized void removeDownstreamPassiveServerProcess(PassthroughServerProcess serverProcess) {
    boolean didRemove = this.downstreamPassives.remove(serverProcess);
    // We expect the passive to have been attached, if we are removing it.
    Assert.assertTrue(didRemove);
  }

  public void promoteToActive() {
    // Make sure that we are currently passive.
    Assert.assertTrue(null != this.passiveEntities);
    // Make us active and promote all passive entities.
    this.downstreamPassives.clear();
    this.activeEntities = new HashMap<PassthroughEntityTuple, CreationData<?, ?>>();
    
    // We need to create the entities as active but note that we would already have persisted this data so only create the
    // actual instances, don't go through the full creation path.
    for (Map.Entry<PassthroughEntityTuple, CreationData<?, ?>> entry : this.passiveEntities.entrySet()) {
      CreationData<?, ?> data = entry.getValue();
      CreationData<?, ?> newData = buildCreationDataForPromotion(data);
      newData.getActive().loadExisting();
      this.activeEntities.put(entry.getKey(), newData);
    }
//  show promotion in monitoring    
    if (this.serviceInterface != null) {
      long timestamp = System.currentTimeMillis();
      this.serviceInterface.addNode(PlatformMonitoringConstants.PLATFORM_PATH, PlatformMonitoringConstants.STATE_NODE_NAME, new ServerState(PlatformMonitoringConstants.SERVER_STATE_ACTIVE, timestamp, timestamp));
    }
    
    // Clear our passives.
    this.passiveEntities = null;
  }

  // This method exists to create the generic type context from the service for creating the CreationData for promotion to active.
  private <M extends EntityMessage, R extends EntityResponse> CreationData<M, R> buildCreationDataForPromotion(CreationData<M, R> data) {
    boolean isActive = true;
    return new CreationData<M, R>(data.entityClassName, data.entityName, data.version, data.configuration, data.registry, data.service, isActive, data.consumerID);
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
      String uuid = connection.getUUID();
      Assert.assertTrue(null != uuid);
      String connectionName = connection.getConnectionName();
      Assert.assertTrue(null != connectionName);
      PlatformConnectedClient clientDescription = new PlatformConnectedClient(uuid, connectionName, localHost, this.bindPort, localHost, CLIENT_PORT.getAndIncrement(), Long.parseLong(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]));
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

  public synchronized void beginReceivingResends() {
    // We can only enter specialized re-send processing mode if we have order persistence.
    if (null != this.transactionOrderManager) {
      Assert.assertTrue(!this.isHandlingResends);
      this.transactionOrderManager.startHandlingResends();
      this.isHandlingResends = true;
    }
  }

  public synchronized void endReceivingResends() {
    // We can only exit specialized re-send processing mode if we have order persistence.
    if (null != this.transactionOrderManager) {
      Assert.assertTrue(this.isHandlingResends);
      List<PassthroughMessageContainer> list = this.transactionOrderManager.stopHandlingResends();
      this.messageQueue.addAll(list);
      this.isHandlingResends = false;
      this.notifyAll();
    }
  }

  /**
   * This is only used to support internal service implementations which are, themselves, built on top of external service implementations.
   * The internal IMonitoringProducer implementation works this way, consuming the externally-provided IStripeMonitoring implementation.
   * 
   * @return A service registry for the described internal consumer.
   */
  public PassthroughServiceRegistry createServiceRegistryForInternalConsumer(String entityClassName, String entityName, long consumerID, DeferredEntityContainer container) {
    this.serviceProvidersReadOnly = true;
    return new PassthroughServiceRegistry(entityClassName, entityName, consumerID, this.serviceProviders, this.overrideServiceProviders, this.implementationProvidedServiceProviders, container);
  }

  public PlatformServer getServerInfo() {
    return this.serverInfo;
  }

  private PassthroughServiceRegistry getNextServiceRegistry(String entityClassName, String entityName, DeferredEntityContainer container) {
    long thisConsumerID = this.nextConsumerID;
    this.nextConsumerID += 1;
    this.serviceProvidersReadOnly = true;
    return new PassthroughServiceRegistry(entityClassName, entityName, thisConsumerID, this.serviceProviders, this.overrideServiceProviders, this.implementationProvidedServiceProviders, container);
  }

  private EntityServerService<?, ?> getServerEntityServiceForVersion(String entityClassName, String entityName, long version) throws EntityVersionMismatchException, EntityNotProvidedException {
    EntityServerService<?, ?> service = getEntityServiceForClassName(entityClassName);
    if(service == null) {
      throw new EntityNotProvidedException(entityClassName, entityName);
    }
    long expectedVersion = service.getVersion();
    if (expectedVersion != version) {
      throw new EntityVersionMismatchException(entityClassName, entityName, expectedVersion, version);
    }
    return service;
  }

  private <M extends EntityMessage, R extends EntityResponse> CommonServerEntity<M, R> createAndStoreEntity(String entityClassName, String entityName, long version, byte[] serializedConfiguration, PassthroughEntityTuple entityTuple, EntityServerService<M, R> service, PassthroughServiceRegistry registry, long consumerID) {
    CommonServerEntity<M, R> newEntity = null;
    boolean isActive = (null != this.activeEntities);
    CreationData<M, R> data = new CreationData<M, R>(entityClassName, entityName, version, serializedConfiguration, registry, service, isActive, consumerID);
    if (isActive) {
      this.activeEntities.put(entityTuple, data);
      newEntity = data.getActive();
    } else {
      this.passiveEntities.put(entityTuple, data);
      newEntity = data.getPassive();
    }
    return newEntity;
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
    public final EntityServerService<M, R> service;
    public CommonServerEntity<M, R> entityInstance;
    public final MessageCodec<M, R> messageCodec;
    public final SyncMessageCodec<M> syncMessageCodec;
    public ConcurrencyStrategy<M> concurrency; 
    public ExecutionStrategy<M> executionStrategy;
    public final boolean isActive;
    public final long consumerID;
    public boolean isDestroyed = false;
    public Map<ClientDescriptor, Integer> references = new HashMap<ClientDescriptor, Integer>();
    
    public CreationData(String entityClassName, String entityName, long version, byte[] configuration, PassthroughServiceRegistry registry, EntityServerService<M, R> service, boolean isActive, long consumerID) {
      this.entityClassName = entityClassName;
      this.entityName = entityName;
      this.version = version;
      this.configuration = configuration;
      this.registry = registry;
      this.service = service;
      this.messageCodec = service.getMessageCodec();
      this.syncMessageCodec = service.getSyncMessageCodec();
      this.entityInstance = (isActive) ? service.createActiveEntity(registry, configuration) : service.createPassiveEntity(registry, configuration);
      this.concurrency = (isActive) ? service.getConcurrencyStrategy(configuration) : null;
      this.executionStrategy = service.getExecutionStrategy(configuration); //  cheating here.  notmally onlt the active knows about execution but, passthrough is going to check on both active and passive
      this.isActive = isActive;
      this.consumerID = consumerID;
    }
    
    synchronized boolean reference(ClientDescriptor cid) {
      Assert.assertTrue(isActive);
      if (!isDestroyed) {
        Integer current = references.get(cid);
        if (null == current) {
          references.put(cid, 1);
        }
        if (current != null) {
          throw new AssertionError(current);
        }
      }
      return !isDestroyed;
    }
    
    synchronized boolean release(ClientDescriptor cid) {
      Assert.assertTrue(isActive);
      Integer current = references.remove(cid);
      if (current == null) {
        return false;
      } else if (current == 1) {
        return true;
      } else {
        throw new AssertionError("makes no sense");
      }
    }
    
    synchronized boolean destroy() {
      if (!isDestroyed && (!isActive || references.isEmpty())) {
          this.entityInstance.destroy();
        isDestroyed = true;
      }
      return isDestroyed;
    }
    
    byte[] reconfigure(byte[] data) {
      try {
        this.entityInstance = service.reconfigureEntity(registry, this.entityInstance, data);
        this.concurrency = (isActive) ? service.getConcurrencyStrategy(data) : null;
        this.executionStrategy = service.getExecutionStrategy(data);
        return this.configuration;
      } finally {
        this.configuration = data;
      }
    }

    synchronized void reconnect(long clientid, ClientDescriptor clientDescriptor, byte[] data) {
      Assert.assertTrue(isActive);
      this.reference(clientDescriptor);
      getActive().connected(clientDescriptor);
      getActive().handleReconnect(clientDescriptor, data);
    }

    @SuppressWarnings("unchecked")
    public ActiveServerEntity<M, R> getActive() {
      return ActiveServerEntity.class.cast(entityInstance);
    }

    @SuppressWarnings("unchecked")
    public PassiveServerEntity<M, R> getPassive() {
      return PassiveServerEntity.class.cast(entityInstance);
    }    
    
    public ConcurrencyStrategy<M> getConcurrency() {
      return concurrency;
    }
    
    public void synchronizeToPassive(final PassthroughServerProcess passive, final int key) {
      getActive().synchronizeKeyToPassive(new PassiveSynchronizationChannel<M>() {
          @Override
          public void synchronizeToPassive(M payload) {
            PassthroughMessage payloadMessage = PassthroughMessageCodec.createSyncPayloadMessage(entityClassName, entityName, key, serialize(key, payload));
            PassthroughInterserverInterlock wrapper = new PassthroughInterserverInterlock(null);
            passive.sendMessageToServerFromActive(wrapper, payloadMessage.asSerializedBytes());
            wrapper.waitForComplete();
          }
        }, key);
    }
    
    private byte[] serialize(int key, M message) {
      try {
        return syncMessageCodec.encode(key, message);
      } catch (MessageCodecException me) {
        throw new RuntimeException(me);
      }
    }
  }


  private static class PassthroughPlatformConfiguration implements PlatformConfiguration {
    private final String serverName;
    
    public PassthroughPlatformConfiguration(String serverName) {
      this.serverName = serverName;
    }
    
    @Override
    public String getServerName() {
      return this.serverName;
    }
  }
}
