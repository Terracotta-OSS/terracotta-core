/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import bsh.EvalError;
import bsh.Interpreter;

import com.sleepycat.je.DatabaseException;
import com.tc.async.api.SEDA;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.async.impl.NullSink;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.exception.TCRuntimeException;
import com.tc.io.TCFile;
import com.tc.io.TCFileImpl;
import com.tc.io.TCRandomFileAccessImpl;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.ha.L2HACoordinator;
import com.tc.l2.ha.L2HADisabledCooridinator;
import com.tc.l2.state.StateManager;
import com.tc.lang.TCThreadGroup;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.L2Management;
import com.tc.management.beans.L2State;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.management.remote.connect.ClientConnectEventHandler;
import com.tc.management.remote.protocol.terracotta.ClientTunnelingEventHandler;
import com.tc.management.remote.protocol.terracotta.JmxRemoteTunnelMessage;
import com.tc.management.remote.protocol.terracotta.L1JmxReady;
import com.tc.net.NIOWorkarounds;
import com.tc.net.TCSocketAddress;
import com.tc.net.groups.Node;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.HydrateHandler;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.object.cache.CacheConfigImpl;
import com.tc.object.cache.CacheManager;
import com.tc.object.cache.EvictionPolicy;
import com.tc.object.cache.LFUConfigImpl;
import com.tc.object.cache.LFUEvictionPolicy;
import com.tc.object.cache.LRUEvictionPolicy;
import com.tc.object.cache.NullCache;
import com.tc.object.config.schema.NewL2DSOConfig;
import com.tc.object.config.schema.PersistenceMode;
import com.tc.object.msg.AcknowledgeTransactionMessageImpl;
import com.tc.object.msg.BatchTransactionAcknowledgeMessageImpl;
import com.tc.object.msg.BroadcastTransactionMessageImpl;
import com.tc.object.msg.ClientHandshakeAckMessageImpl;
import com.tc.object.msg.ClientHandshakeMessageImpl;
import com.tc.object.msg.ClusterMembershipMessage;
import com.tc.object.msg.CommitTransactionMessageImpl;
import com.tc.object.msg.JMXMessage;
import com.tc.object.msg.LockRequestMessage;
import com.tc.object.msg.LockResponseMessage;
import com.tc.object.msg.MessageRecycler;
import com.tc.object.msg.ObjectIDBatchRequestMessage;
import com.tc.object.msg.ObjectIDBatchRequestResponseMessage;
import com.tc.object.msg.RequestManagedObjectMessageImpl;
import com.tc.object.msg.RequestManagedObjectResponseMessage;
import com.tc.object.msg.RequestRootMessageImpl;
import com.tc.object.msg.RequestRootResponseMessage;
import com.tc.object.net.ChannelStats;
import com.tc.object.net.ChannelStatsImpl;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerImpl;
import com.tc.object.net.DSOChannelManagerMBean;
import com.tc.object.session.NullSessionManager;
import com.tc.object.session.SessionManager;
import com.tc.object.session.SessionProvider;
import com.tc.objectserver.DSOApplicationEvents;
import com.tc.objectserver.api.ObjectManagerMBean;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.objectserver.core.api.DSOGlobalServerStatsImpl;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.MarkAndSweepGarbageCollector;
import com.tc.objectserver.core.impl.ServerConfigurationContextImpl;
import com.tc.objectserver.core.impl.ServerManagementContext;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.objectserver.gtx.ServerGlobalTransactionManagerImpl;
import com.tc.objectserver.handler.ApplyCompleteTransactionHandler;
import com.tc.objectserver.handler.ApplyTransactionChangeHandler;
import com.tc.objectserver.handler.BroadcastChangeHandler;
import com.tc.objectserver.handler.ChannelLifeCycleHandler;
import com.tc.objectserver.handler.ClientHandshakeHandler;
import com.tc.objectserver.handler.CommitTransactionChangeHandler;
import com.tc.objectserver.handler.GlobalTransactionIDBatchRequestHandler;
import com.tc.objectserver.handler.JMXEventsHandler;
import com.tc.objectserver.handler.ManagedObjectFaultHandler;
import com.tc.objectserver.handler.ManagedObjectFlushHandler;
import com.tc.objectserver.handler.ManagedObjectRequestHandler;
import com.tc.objectserver.handler.ProcessTransactionHandler;
import com.tc.objectserver.handler.RecallObjectsHandler;
import com.tc.objectserver.handler.RequestLockUnLockHandler;
import com.tc.objectserver.handler.RequestObjectIDBatchHandler;
import com.tc.objectserver.handler.RequestRootHandler;
import com.tc.objectserver.handler.RespondToObjectRequestHandler;
import com.tc.objectserver.handler.RespondToRequestLockHandler;
import com.tc.objectserver.handler.TransactionAcknowledgementHandler;
import com.tc.objectserver.handler.TransactionLookupHandler;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.l1.impl.ClientStateManagerImpl;
import com.tc.objectserver.l1.impl.TransactionAcknowledgeAction;
import com.tc.objectserver.l1.impl.TransactionAcknowledgeActionImpl;
import com.tc.objectserver.lockmanager.api.LockManager;
import com.tc.objectserver.lockmanager.api.LockManagerMBean;
import com.tc.objectserver.lockmanager.impl.LockManagerImpl;
import com.tc.objectserver.managedobject.ManagedObjectChangeListenerProviderImpl;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.persistence.api.ClientStatePersistor;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.api.Persistor;
import com.tc.objectserver.persistence.api.TransactionPersistor;
import com.tc.objectserver.persistence.api.TransactionStore;
import com.tc.objectserver.persistence.impl.InMemoryPersistor;
import com.tc.objectserver.persistence.impl.InMemorySequenceProvider;
import com.tc.objectserver.persistence.impl.NullPersistenceTransactionProvider;
import com.tc.objectserver.persistence.impl.NullTransactionPersistor;
import com.tc.objectserver.persistence.impl.TransactionStoreImpl;
import com.tc.objectserver.persistence.sleepycat.ConnectionIDFactoryImpl;
import com.tc.objectserver.persistence.sleepycat.CustomSerializationAdapterFactory;
import com.tc.objectserver.persistence.sleepycat.DBEnvironment;
import com.tc.objectserver.persistence.sleepycat.DBException;
import com.tc.objectserver.persistence.sleepycat.SerializationAdapterFactory;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor;
import com.tc.objectserver.tx.CommitTransactionMessageRecycler;
import com.tc.objectserver.tx.CommitTransactionMessageToTransactionBatchReader;
import com.tc.objectserver.tx.ServerTransactionManagerImpl;
import com.tc.objectserver.tx.TransactionBatchManager;
import com.tc.objectserver.tx.TransactionBatchManagerImpl;
import com.tc.objectserver.tx.TransactionSequencer;
import com.tc.objectserver.tx.TransactionalObjectManagerImpl;
import com.tc.objectserver.tx.TransactionalStageCoordinator;
import com.tc.objectserver.tx.TransactionalStagesCoordinatorImpl;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.stats.counter.sampled.SampledCounterManager;
import com.tc.stats.counter.sampled.SampledCounterManagerImpl;
import com.tc.util.Assert;
import com.tc.util.SequenceValidator;
import com.tc.util.StartupLock;
import com.tc.util.TCTimeoutException;
import com.tc.util.TCTimerImpl;
import com.tc.util.io.FileUtils;
import com.tc.util.sequence.BatchSequence;
import com.tc.util.sequence.MutableSequence;
import com.tc.util.sequence.Sequence;
import com.tc.util.startuplock.FileNotCreatedException;
import com.tc.util.startuplock.LocationNotCreatedException;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;

/**
 * Startup and shutdown point. Builds and starts the server
 *
 * @author steve
 */
public class DistributedObjectServer extends SEDA {
  private final ConnectionPolicy               connectionPolicy;

  private static final TCLogger                logger        = CustomerLogging.getDSOGenericLogger();
  private static final TCLogger                consoleLogger = CustomerLogging.getConsoleLogger();

  private final L2TVSConfigurationSetupManager configSetupManager;
  private final Sink                           httpSink;
  private NetworkListener                      l1Listener;
  private CommunicationsManager                communicationsManager;
  private ServerConfigurationContext           context;
  private ObjectManagerImpl                    objectManager;
  private TransactionalObjectManagerImpl       txnObjectManager;
  private SampledCounterManager                sampledCounterManager;
  private LockManager                          lockManager;
  private ServerManagementContext              managementContext;
  private StartupLock                          startupLock;

  private ClientStateManagerImpl               clientStateManager;

  private ManagedObjectStore                   objectStore;
  private Persistor                            persistor;
  private ServerTransactionManagerImpl         transactionManager;

  private CacheManager                         cacheManager;

  private final TCServerInfoMBean              tcServerInfoMBean;
  private final L2State                        l2State;
  private L2Management                         l2Management;
  private L2Coordinator                        l2Coordinator;

  private TCProperties                         l2Properties;

  private ConnectionIDFactoryImpl              connectionIdFactory;

  // used by a test
  public DistributedObjectServer(L2TVSConfigurationSetupManager configSetupManager, TCThreadGroup threadGroup,
                                 ConnectionPolicy connectionPolicy, TCServerInfoMBean tcServerInfoMBean) {
    this(configSetupManager, threadGroup, connectionPolicy, new NullSink(), tcServerInfoMBean, new L2State());
  }

  public DistributedObjectServer(L2TVSConfigurationSetupManager configSetupManager, TCThreadGroup threadGroup,
                                 ConnectionPolicy connectionPolicy, Sink httpSink, TCServerInfoMBean tcServerInfoMBean,
                                 L2State l2State) {
    super(threadGroup);

    // This assertion is here because we want to assume that all threads spawned by the server (including any created in
    // 3rd party libs) inherit their thread group from the current thread . Consider this before removing the assertion.
    // Even in tests, we probably don't want different thread group configurations
    Assert.assertEquals(threadGroup, Thread.currentThread().getThreadGroup());

    this.configSetupManager = configSetupManager;
    this.connectionPolicy = connectionPolicy;
    this.httpSink = httpSink;
    this.tcServerInfoMBean = tcServerInfoMBean;
    this.l2State = l2State;
  }

  public void dump() {
    if (this.lockManager != null) {
      this.lockManager.dump();
    }

    if (this.objectManager != null) {
      this.objectManager.dump();
    }

    if (this.txnObjectManager != null) {
      this.txnObjectManager.dump();
    }

    if (this.transactionManager != null) {
      this.transactionManager.dump();
    }
  }

  public synchronized void start() throws IOException, DatabaseException, LocationNotCreatedException,
      FileNotCreatedException {

    try {
      startJMXServer();
    } catch (Exception e) {
      String msg = "Unable to start the JMX server. Do you have another Terracotta Server running?";
      consoleLogger.error(msg);
      logger.error(msg, e);
      System.exit(-1);
    }

    NIOWorkarounds.solaris10Workaround();

    configSetupManager.commonl2Config().changesInItemIgnored(configSetupManager.commonl2Config().dataPath());
    NewL2DSOConfig l2DSOConfig = configSetupManager.dsoL2Config();
    l2DSOConfig.changesInItemIgnored(l2DSOConfig.persistenceMode());
    PersistenceMode persistenceMode = (PersistenceMode) l2DSOConfig.persistenceMode().getObject();

    final boolean swapEnabled = true; // 2006-01-31 andrew -- no longer possible to use in-memory only; DSO folks say
    // it's broken
    final boolean persistent = persistenceMode.equals(PersistenceMode.PERMANENT_STORE);

    TCFile location = new TCFileImpl(this.configSetupManager.commonl2Config().dataPath().getFile());
    startupLock = new StartupLock(location);

    if (!startupLock.canProceed(new TCRandomFileAccessImpl(), persistent)) {
      consoleLogger.error("Another L2 process is using the directory " + location + " as data directory.");
      if (!persistent) {
        consoleLogger.error("This is not allowed with persistence mode set to temporary-swap-only.");
      }
      consoleLogger.error("Exiting...");
      System.exit(1);
    }

    int maxStageSize = 5000;

    StageManager stageManager = getStageManager();
    SessionManager sessionManager = new NullSessionManager();
    SessionProvider sessionProvider = (SessionProvider) sessionManager;
    l2Properties = TCPropertiesImpl.getProperties().getPropertiesFor("l2");

    EvictionPolicy swapCache;
    final ClientStatePersistor clientStateStore;
    final PersistenceTransactionProvider persistenceTransactionProvider;
    final TransactionPersistor transactionPersistor;
    final Sequence globalTransactionIDSequence;
    logger.debug("server swap enabled: " + swapEnabled);
    final ManagedObjectChangeListenerProviderImpl managedObjectChangeListenerProvider = new ManagedObjectChangeListenerProviderImpl();
    if (swapEnabled) {
      File dbhome = new File(configSetupManager.commonl2Config().dataPath().getFile(), "objectdb");
      logger.debug("persistent: " + persistent);
      if (!persistent) {
        if (dbhome.exists()) {
          logger.debug("deleting persistence database...");
          FileUtils.forceDelete(dbhome, "jdb");
          logger.debug("persistence database deleted.");
        }
      }
      logger.debug("persistence database home: " + dbhome);

      DBEnvironment dbenv = new DBEnvironment(persistent, dbhome, l2Properties.getPropertiesFor("berkeleydb")
          .addAllPropertiesTo(new Properties()));
      SerializationAdapterFactory serializationAdapterFactory = new CustomSerializationAdapterFactory();
      persistor = new SleepycatPersistor(TCLogging.getLogger(SleepycatPersistor.class), dbenv,
                                         serializationAdapterFactory);

      String cachePolicy = l2Properties.getProperty("objectmanager.cachePolicy").toUpperCase();
      if (cachePolicy.equals("LRU")) {
        swapCache = new LRUEvictionPolicy(-1);
      } else if (cachePolicy.equals("LFU")) {
        swapCache = new LFUEvictionPolicy(-1, new LFUConfigImpl(l2Properties.getPropertiesFor("lfu")));
      } else {
        throw new AssertionError("Unknown Cache Policy : " + cachePolicy
                                 + " Accepted Values are : <LRU>/<LFU> Please check tc.properties");
      }
      objectStore = new PersistentManagedObjectStore(persistor.getManagedObjectPersistor());
    } else {
      persistor = new InMemoryPersistor();
      swapCache = new NullCache();
      objectStore = new InMemoryManagedObjectStore(new HashMap());
    }

    persistenceTransactionProvider = persistor.getPersistenceTransactionProvider();
    PersistenceTransactionProvider transactionStorePTP;
    MutableSequence gidSequence;
    if (persistent) {
      gidSequence = persistor.getGlobalTransactionIDSequence();

      transactionPersistor = persistor.getTransactionPersistor();
      transactionStorePTP = persistenceTransactionProvider;
    } else {
      gidSequence = new InMemorySequenceProvider();

      transactionPersistor = new NullTransactionPersistor();
      transactionStorePTP = new NullPersistenceTransactionProvider();
    }

    GlobalTransactionIDBatchRequestHandler gidSequenceProvider = new GlobalTransactionIDBatchRequestHandler(gidSequence);
    Stage requestBatchStage = stageManager
        .createStage(ServerConfigurationContext.REQUEST_BATCH_GLOBAL_TRANSACTION_ID_SEQUENCE_STAGE,
                     gidSequenceProvider, 1, maxStageSize);
    gidSequenceProvider.setRequestBatchSink(requestBatchStage.getSink());
    globalTransactionIDSequence = new BatchSequence(gidSequenceProvider, 10000);

    clientStateStore = persistor.getClientStatePersistor();

    ManagedObjectStateFactory.createInstance(managedObjectChangeListenerProvider, persistor);

    communicationsManager = new CommunicationsManagerImpl(new NullMessageMonitor(),
                                                          new PlainNetworkStackHarnessFactory(), connectionPolicy);

    final DSOApplicationEvents appEvents;
    try {
      appEvents = new DSOApplicationEvents();
    } catch (NotCompliantMBeanException ncmbe) {
      throw new TCRuntimeException("Unable to construct the " + DSOApplicationEvents.class.getName()
                                   + " MBean; this is a programming error. Please go fix that class.", ncmbe);
    }

    clientStateManager = new ClientStateManagerImpl(TCLogging.getLogger(ClientStateManager.class));

    l2DSOConfig.changesInItemIgnored(l2DSOConfig.garbageCollectionEnabled());
    boolean gcEnabled = l2DSOConfig.garbageCollectionEnabled().getBoolean();
    logger.debug("GC enabled: " + gcEnabled);

    l2DSOConfig.changesInItemIgnored(l2DSOConfig.garbageCollectionInterval());
    long gcInterval = l2DSOConfig.garbageCollectionInterval().getInt();
    if (gcEnabled) logger.debug("GC interval: " + gcInterval + " seconds");

    l2DSOConfig.changesInItemIgnored(l2DSOConfig.garbageCollectionVerbose());
    boolean verboseGC = l2DSOConfig.garbageCollectionVerbose().getBoolean();
    if (gcEnabled) logger.debug("Verbose GC enabled: " + verboseGC);
    sampledCounterManager = new SampledCounterManagerImpl();
    SampledCounter objectCreationRate = sampledCounterManager.createCounter(new SampledCounterConfig(1, 900, true, 0L));
    SampledCounter objectFaultRate = sampledCounterManager.createCounter(new SampledCounterConfig(1, 900, true, 0L));
    ObjectManagerStatsImpl objMgrStats = new ObjectManagerStatsImpl(objectCreationRate, objectFaultRate);

    SequenceValidator sequenceValidator = new SequenceValidator(0);
    ManagedObjectFaultHandler managedObjectFaultHandler = new ManagedObjectFaultHandler();
    // Server initiated request processing queues shouldn't have any max queue size.
    Stage faultManagedObjectStage = stageManager.createStage(ServerConfigurationContext.MANAGED_OBJECT_FAULT_STAGE,
                                                             managedObjectFaultHandler, 4, -1);
    ManagedObjectFlushHandler managedObjectFlushHandler = new ManagedObjectFlushHandler();
    Stage flushManagedObjectStage = stageManager.createStage(ServerConfigurationContext.MANAGED_OBJECT_FLUSH_STAGE,
                                                             managedObjectFlushHandler, (persistent ? 1 : 4), -1);

    TCProperties objManagerProperties = l2Properties.getPropertiesFor("objectmanager");

    objectManager = new ObjectManagerImpl(new ObjectManagerConfig(gcInterval * 1000, gcEnabled, verboseGC, persistent,
                                                                  objManagerProperties.getInt("deleteBatchSize")),
                                          getThreadGroup(), clientStateManager, objectStore, swapCache,
                                          persistenceTransactionProvider, faultManagedObjectStage.getSink(),
                                          flushManagedObjectStage.getSink(), l2Management
                                              .findObjectManagementMonitorMBean());
    objectManager.setStatsListener(objMgrStats);
    objectManager.setGarbageCollector(new MarkAndSweepGarbageCollector(objectManager, clientStateManager, verboseGC));
    managedObjectChangeListenerProvider.setListener(objectManager);

    TCProperties cacheManagerProperties = l2Properties.getPropertiesFor("cachemanager");
    if (cacheManagerProperties.getBoolean("enabled")) {
      cacheManager = new CacheManager(objectManager, new CacheConfigImpl(cacheManagerProperties));
      if (logger.isDebugEnabled()) {
        logger.debug("CacheManager Enabled : " + cacheManager);
      }
    } else {
      logger.warn("CacheManager is Disabled");
    }

    connectionIdFactory = new ConnectionIDFactoryImpl(clientStateStore);

    l2DSOConfig.changesInItemIgnored(l2DSOConfig.listenPort());
    int serverPort = l2DSOConfig.listenPort().getInt();
    l1Listener = communicationsManager.createListener(sessionProvider,
                                                      new TCSocketAddress(TCSocketAddress.WILDCARD_ADDR, serverPort),
                                                      true, connectionIdFactory, httpSink);

    ClientTunnelingEventHandler cteh = new ClientTunnelingEventHandler();

    DSOChannelManager channelManager = new DSOChannelManagerImpl(l1Listener.getChannelManager());
    channelManager.addEventListener(cteh);
    channelManager.addEventListener(connectionIdFactory);

    ChannelStats channelStats = new ChannelStatsImpl(sampledCounterManager, channelManager);

    lockManager = new LockManagerImpl(channelManager);
    TransactionAcknowledgeAction taa = new TransactionAcknowledgeActionImpl(channelManager);
    ObjectInstanceMonitorImpl instanceMonitor = new ObjectInstanceMonitorImpl();
    TransactionBatchManager transactionBatchManager = new TransactionBatchManagerImpl();
    SampledCounter globalTxnCounter = sampledCounterManager.createCounter(new SampledCounterConfig(1, 300, true, 0L));

    final TransactionStore transactionStore = new TransactionStoreImpl(transactionPersistor,
                                                                       globalTransactionIDSequence);
    ServerGlobalTransactionManager gtxm = new ServerGlobalTransactionManagerImpl(sequenceValidator, transactionStore,
                                                                                 transactionStorePTP);
    transactionManager = new ServerTransactionManagerImpl(gtxm, transactionStore, lockManager, clientStateManager,
                                                          objectManager, taa, globalTxnCounter, channelStats);

    if (l2Properties.getBoolean("transactionmanager.logging.enabled")) {
      transactionManager.enableTransactionLogger();
    }

    MessageRecycler recycler = new CommitTransactionMessageRecycler(transactionManager);
    ObjectRequestManager objectRequestManager = new ObjectRequestManagerImpl(objectManager, transactionManager);

    stageManager.createStage(ServerConfigurationContext.TRANSACTION_LOOKUP_STAGE, new TransactionLookupHandler(), 1,
                             maxStageSize);

    // Lookup stage should never be blocked trying to add to apply stage
    stageManager.createStage(ServerConfigurationContext.APPLY_CHANGES_STAGE,
                             new ApplyTransactionChangeHandler(instanceMonitor, gtxm), 1, -1);

    stageManager.createStage(ServerConfigurationContext.APPLY_COMPLETE_STAGE, new ApplyCompleteTransactionHandler(), 1,
                             maxStageSize);

    // Server initiated request processing stages should not be bounded
    stageManager.createStage(ServerConfigurationContext.RECALL_OBJECTS_STAGE, new RecallObjectsHandler(), 1, -1);

    int commitThreads = (persistent ? 4 : 1);
    stageManager.createStage(ServerConfigurationContext.COMMIT_CHANGES_STAGE,
                             new CommitTransactionChangeHandler(transactionStorePTP), commitThreads, maxStageSize);

    TransactionalStageCoordinator txnStageCoordinator = new TransactionalStagesCoordinatorImpl(stageManager);
    txnObjectManager = new TransactionalObjectManagerImpl(objectManager, new TransactionSequencer(), gtxm,
                                                          txnStageCoordinator);
    objectManager.setTransactionalObjectManager(txnObjectManager);
    Stage processTx = stageManager.createStage(ServerConfigurationContext.PROCESS_TRANSACTION_STAGE,
                                               new ProcessTransactionHandler(transactionBatchManager, txnObjectManager,
                                                                             sequenceValidator, recycler), 1,
                                               maxStageSize);

    Stage rootRequest = stageManager.createStage(ServerConfigurationContext.MANAGED_ROOT_REQUEST_STAGE,
                                                 new RequestRootHandler(), 1, maxStageSize);

    stageManager.createStage(ServerConfigurationContext.BROADCAST_CHANGES_STAGE,
                             new BroadcastChangeHandler(transactionBatchManager), 1, maxStageSize);
    stageManager.createStage(ServerConfigurationContext.RESPOND_TO_LOCK_REQUEST_STAGE,
                             new RespondToRequestLockHandler(), 1, maxStageSize);
    Stage requestLock = stageManager.createStage(ServerConfigurationContext.REQUEST_LOCK_STAGE,
                                                 new RequestLockUnLockHandler(), 1, maxStageSize);
    Stage channelLifecycleStage = stageManager
        .createStage(ServerConfigurationContext.CHANNEL_LIFE_CYCLE_STAGE,
                     new ChannelLifeCycleHandler(communicationsManager, transactionManager, transactionBatchManager,
                                                 channelManager), 1, maxStageSize);
    channelManager.addEventListener(new ChannelLifeCycleHandler.EventListener(channelLifecycleStage.getSink()));

    SampledCounter globalObjectFaultCounter = sampledCounterManager.createCounter(new SampledCounterConfig(1, 300,
                                                                                                           true, 0L));
    SampledCounter globalObjectFlushCounter = sampledCounterManager.createCounter(new SampledCounterConfig(1, 300,
                                                                                                           true, 0L));
    Stage objectRequest = stageManager.createStage(ServerConfigurationContext.MANAGED_OBJECT_REQUEST_STAGE,
                                                   new ManagedObjectRequestHandler(globalObjectFaultCounter,
                                                                                   globalObjectFlushCounter,
                                                                                   objectRequestManager), 1,
                                                   maxStageSize);
    stageManager.createStage(ServerConfigurationContext.RESPOND_TO_OBJECT_REQUEST_STAGE,
                             new RespondToObjectRequestHandler(), 4, maxStageSize);
    Stage oidRequest = stageManager.createStage(ServerConfigurationContext.OBJECT_ID_BATCH_REQUEST_STAGE,
                                                new RequestObjectIDBatchHandler(objectStore), 1, maxStageSize);
    Stage transactionAck = stageManager.createStage(ServerConfigurationContext.TRANSACTION_ACKNOWLEDGEMENT_STAGE,
                                                    new TransactionAcknowledgementHandler(), 1, maxStageSize);
    Stage clientHandshake = stageManager.createStage(ServerConfigurationContext.CLIENT_HANDSHAKE_STAGE,
                                                     new ClientHandshakeHandler(), 1, maxStageSize);
    Stage hydrateStage = stageManager.createStage(ServerConfigurationContext.HYDRATE_MESSAGE_SINK,
                                                  new HydrateHandler(), 1, maxStageSize);

    Stage jmxEventsStage = stageManager.createStage(ServerConfigurationContext.JMX_EVENTS_STAGE,
                                                    new JMXEventsHandler(appEvents), 1, maxStageSize);

    final Stage jmxRemoteConnectStage = stageManager.createStage(ServerConfigurationContext.JMXREMOTE_CONNECT_STAGE,
                                                                 new ClientConnectEventHandler(), 1, maxStageSize);
    cteh.setConnectStageSink(jmxRemoteConnectStage.getSink());
    final Stage jmxRemoteTunnelStage = stageManager.createStage(ServerConfigurationContext.JMXREMOTE_TUNNEL_STAGE,
                                                                cteh, 1, maxStageSize);

    l1Listener.addClassMapping(TCMessageType.BATCH_TRANSACTION_ACK_MESSAGE,
                               BatchTransactionAcknowledgeMessageImpl.class);
    l1Listener.addClassMapping(TCMessageType.REQUEST_ROOT_MESSAGE, RequestRootMessageImpl.class);
    l1Listener.addClassMapping(TCMessageType.LOCK_REQUEST_MESSAGE, LockRequestMessage.class);
    l1Listener.addClassMapping(TCMessageType.LOCK_RESPONSE_MESSAGE, LockResponseMessage.class);
    l1Listener.addClassMapping(TCMessageType.LOCK_RECALL_MESSAGE, LockResponseMessage.class);
    l1Listener.addClassMapping(TCMessageType.LOCK_QUERY_RESPONSE_MESSAGE, LockResponseMessage.class);
    l1Listener.addClassMapping(TCMessageType.COMMIT_TRANSACTION_MESSAGE, CommitTransactionMessageImpl.class);
    l1Listener.addClassMapping(TCMessageType.REQUEST_ROOT_RESPONSE_MESSAGE, RequestRootResponseMessage.class);
    l1Listener.addClassMapping(TCMessageType.REQUEST_MANAGED_OBJECT_MESSAGE, RequestManagedObjectMessageImpl.class);
    l1Listener.addClassMapping(TCMessageType.REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE,
                               RequestManagedObjectResponseMessage.class);
    l1Listener.addClassMapping(TCMessageType.BROADCAST_TRANSACTION_MESSAGE, BroadcastTransactionMessageImpl.class);
    l1Listener.addClassMapping(TCMessageType.OBJECT_ID_BATCH_REQUEST_MESSAGE, ObjectIDBatchRequestMessage.class);
    l1Listener.addClassMapping(TCMessageType.OBJECT_ID_BATCH_REQUEST_RESPONSE_MESSAGE,
                               ObjectIDBatchRequestResponseMessage.class);
    l1Listener.addClassMapping(TCMessageType.ACKNOWLEDGE_TRANSACTION_MESSAGE, AcknowledgeTransactionMessageImpl.class);
    l1Listener.addClassMapping(TCMessageType.CLIENT_HANDSHAKE_MESSAGE, ClientHandshakeMessageImpl.class);
    l1Listener.addClassMapping(TCMessageType.CLIENT_HANDSHAKE_ACK_MESSAGE, ClientHandshakeAckMessageImpl.class);
    l1Listener.addClassMapping(TCMessageType.JMX_MESSAGE, JMXMessage.class);
    l1Listener.addClassMapping(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE, JmxRemoteTunnelMessage.class);
    l1Listener.addClassMapping(TCMessageType.CLUSTER_MEMBERSHIP_EVENT_MESSAGE, ClusterMembershipMessage.class);
    l1Listener.addClassMapping(TCMessageType.CLIENT_JMX_READY_MESSAGE, L1JmxReady.class);

    Sink hydrateSink = hydrateStage.getSink();
    l1Listener.routeMessageType(TCMessageType.COMMIT_TRANSACTION_MESSAGE, processTx.getSink(), hydrateSink);
    l1Listener.routeMessageType(TCMessageType.LOCK_REQUEST_MESSAGE, requestLock.getSink(), hydrateSink);
    l1Listener.routeMessageType(TCMessageType.REQUEST_ROOT_MESSAGE, rootRequest.getSink(), hydrateSink);
    l1Listener.routeMessageType(TCMessageType.REQUEST_MANAGED_OBJECT_MESSAGE, objectRequest.getSink(), hydrateSink);
    l1Listener.routeMessageType(TCMessageType.OBJECT_ID_BATCH_REQUEST_MESSAGE, oidRequest.getSink(), hydrateSink);
    l1Listener.routeMessageType(TCMessageType.ACKNOWLEDGE_TRANSACTION_MESSAGE, transactionAck.getSink(), hydrateSink);
    l1Listener.routeMessageType(TCMessageType.CLIENT_HANDSHAKE_MESSAGE, clientHandshake.getSink(), hydrateSink);
    l1Listener.routeMessageType(TCMessageType.JMX_MESSAGE, jmxEventsStage.getSink(), hydrateSink);
    l1Listener.routeMessageType(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE, jmxRemoteTunnelStage.getSink(),
                                hydrateSink);
    l1Listener.routeMessageType(TCMessageType.CLIENT_JMX_READY_MESSAGE, jmxRemoteTunnelStage.getSink(), hydrateSink);

    l2DSOConfig.changesInItemIgnored(l2DSOConfig.clientReconnectWindow());
    long reconnectTimeout = l2DSOConfig.clientReconnectWindow().getInt();
    logger.debug("Client Reconnect Window: " + reconnectTimeout + " seconds");
    reconnectTimeout *= 1000;
    ServerClientHandshakeManager clientHandshakeManager = new ServerClientHandshakeManager(
                                                                                           TCLogging
                                                                                               .getLogger(ServerClientHandshakeManager.class),
                                                                                           channelManager,
                                                                                           objectRequestManager,
                                                                                           transactionManager,
                                                                                           sequenceValidator,
                                                                                           clientStateManager,
                                                                                           lockManager,
                                                                                           stageManager
                                                                                               .getStage(
                                                                                                         ServerConfigurationContext.RESPOND_TO_LOCK_REQUEST_STAGE)
                                                                                               .getSink(),
                                                                                           objectStore,
                                                                                           new TCTimerImpl(
                                                                                                           "Reconnect timer",
                                                                                                           true),
                                                                                           reconnectTimeout, persistent);

    boolean networkedHA = l2Properties.getBoolean("ha.network.enabled");
    if (networkedHA) {
      logger.info("L2 Networked HA Enabled ");
      l2Coordinator = new L2HACoordinator(consoleLogger, this, stageManager, persistor.getClusterStateStore(),
                                          objectManager, transactionManager, gidSequenceProvider);
      l2Coordinator.getStateManager().registerForStateChangeEvents(l2State);
    } else {
      l2State.setState(StateManager.ACTIVE_COORDINATOR);
      l2Coordinator = new L2HADisabledCooridinator();
    }

    context = new ServerConfigurationContextImpl(stageManager, objectManager, objectStore, lockManager, channelManager,
                                                 clientStateManager, transactionManager, txnObjectManager,
                                                 clientHandshakeManager, channelStats, l2Coordinator,
                                                 new CommitTransactionMessageToTransactionBatchReader(gtxm));

    stageManager.startAll(context);

    DSOGlobalServerStats serverStats = new DSOGlobalServerStatsImpl(globalObjectFlushCounter, globalObjectFaultCounter,
                                                                    globalTxnCounter, objMgrStats);

    // XXX: yucky casts
    managementContext = new ServerManagementContext(transactionManager, (ObjectManagerMBean) objectManager,
                                                    (LockManagerMBean) lockManager,
                                                    (DSOChannelManagerMBean) channelManager, serverStats, channelStats,
                                                    instanceMonitor, appEvents);

    if (l2Properties.getBoolean("beanshell.enabled")) startBeanShell(l2Properties.getInt("beanshell.port"));

    if (networkedHA) {
      final Node thisNode = makeThisNode();
      final Node[] allNodes = makeAllNodes();
      l2Coordinator.start(thisNode, allNodes);
    } else {
      // In non-network enabled HA, Only active server reached here.
      startActiveMode();
    }
  }

  private Node[] makeAllNodes() {
    String[] l2s = configSetupManager.allCurrentlyKnownServers();
    Node[] rv = new Node[l2s.length];
    for (int i = 0; i < l2s.length; i++) {
      NewL2DSOConfig l2;
      try {
        l2 = configSetupManager.dsoL2ConfigFor(l2s[i]);
      } catch (ConfigurationSetupException e) {
        throw new RuntimeException("Error getting l2 config for: " + l2s[i], e);
      }
      rv[i] = makeNode(l2);
    }
    return rv;
  }

  private static Node makeNode(NewL2DSOConfig l2) {
    // NOTE: until we resolve Tribes stepping on TCComm's port
    // we'll use TCComm.port + 1 in Tribes
    int dsoPort = l2.listenPort().getInt();
    if (dsoPort == 0) {
      return new Node(l2.host().getString(), dsoPort);
    } else {
      return new Node(l2.host().getString(), dsoPort + 1);
    }
  }

  private Node makeThisNode() {
    NewL2DSOConfig l2 = configSetupManager.dsoL2Config();
    return makeNode(l2);
  }

  public boolean startActiveMode() throws IOException {
    transactionManager.goToActiveMode();
    Set existingConnections = Collections.unmodifiableSet(connectionIdFactory.loadConnectionIDs());
    context.getClientHandshakeManager().setStarting(existingConnections);
    l1Listener.start(existingConnections);
    consoleLogger.info("Terracotta Server has started up as ACTIVE node on port " + l1Listener.getBindPort()
                       + " successfully, and is now ready for work.");
    return true;
  }

  public boolean stopActiveMode() throws TCTimeoutException {
    // TODO:: Make this not take timeout and force stop
    consoleLogger.info("Stopping ACTIVE Terracotta Server on port " + l1Listener.getBindPort() + ".");
    l1Listener.stop(10000);
    l1Listener.getChannelManager().closeAllChannels();
    return true;
  }

  private void startBeanShell(int port) {
    try {
      Interpreter i = new Interpreter();
      i.set("dsoServer", this);
      i.set("objectManager", objectManager);
      i.set("txnObjectManager", txnObjectManager);
      i.set("portnum", port);
      i.eval("setAccessibility(true)"); // turn off access restrictions
      i.eval("server(portnum)");
      consoleLogger.info("Bean shell is started on port " + port);
    } catch (EvalError e) {
      e.printStackTrace();
    }
  }

  public int getListenPort() {
    return this.l1Listener.getBindPort();
  }

  public synchronized void stop() {
    try {
      if (lockManager != null) lockManager.stop();
    } catch (InterruptedException e) {
      logger.error(e);
    }

    getStageManager().stopAll();

    if (l1Listener != null) {
      try {
        l1Listener.stop(5000);
      } catch (TCTimeoutException e) {
        logger.warn("timeout trying to stop listener: " + e.getMessage());
      }
    }

    if ((communicationsManager != null)) {
      communicationsManager.shutdown();
    }

    if (objectManager != null) {
      try {
        objectManager.stop();
      } catch (Throwable e) {
        logger.error(e);
      }
    }

    clientStateManager.stop();

    try {
      objectStore.shutdown();
    } catch (Throwable e) {
      logger.warn(e);
    }

    try {
      persistor.close();
    } catch (DBException e) {
      logger.warn(e);
    }

    if (sampledCounterManager != null) {
      try {
        sampledCounterManager.shutdown();
      } catch (Exception e) {
        logger.error(e);
      }
    }

    try {
      stopJMXServer();
    } catch (Throwable t) {
      logger.error("Error shutting down jmx server", t);
    }

    basicStop();
  }

  public void quickStop() {
    try {
      stopJMXServer();
    } catch (Throwable t) {
      logger.error("Error shutting down jmx server", t);
    }

    // XXX: not calling basicStop() here, it creates a race condition with the Sleepycat's own writer lock (see
    // LKC-3239) Provided we ever fix graceful server shutdown, we'll want to uncommnet this at that time and/or get rid
    // of this method completely

    // basicStop();
  }

  private void basicStop() {
    if (startupLock != null) {
      startupLock.release();
    }
  }

  public ConnectionIDFactory getConnectionIdFactory() {
    return connectionIdFactory;
  }

  public ManagedObjectStore getManagedObjectStore() {
    return objectStore;
  }

  public ServerConfigurationContext getContext() {
    return context;
  }

  public ServerManagementContext getManagementContext() {
    return managementContext;
  }

  public MBeanServer getMBeanServer() {
    return l2Management.getMBeanServer();
  }

  private void startJMXServer() throws Exception {
    l2Management = new L2Management(tcServerInfoMBean, configSetupManager);

    /*
     * Some tests use this if they run with jdk1.4 and start multiple in-process DistributedObjectServers. When we no
     * longer support 1.4, this can be removed. See com.tctest.LockManagerSystemTest.
     */
    if (!Boolean.getBoolean("org.terracotta.server.disableJmxConnector")) {
      l2Management.start();
    }
  }

  private void stopJMXServer() throws Exception {
    try {
      if (l2Management != null) {
        l2Management.stop();
      }
    } finally {
      l2Management = null;
    }
  }
}
