/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import bsh.EvalError;
import bsh.Interpreter;

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
import com.tc.logging.CallbackDumpAdapter;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.L2LockStatsManager;
import com.tc.management.L2Management;
import com.tc.management.beans.L2State;
import com.tc.management.beans.LockStatisticsMonitor;
import com.tc.management.beans.LockStatisticsMonitorMBean;
import com.tc.management.beans.TCDumper;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.management.beans.object.ServerDBBackup;
import com.tc.management.lock.stats.L2LockStatisticsManagerImpl;
import com.tc.management.lock.stats.LockStatisticsMessage;
import com.tc.management.lock.stats.LockStatisticsResponseMessage;
import com.tc.management.remote.connect.ClientConnectEventHandler;
import com.tc.management.remote.protocol.terracotta.ClientTunnelingEventHandler;
import com.tc.management.remote.protocol.terracotta.JmxRemoteTunnelMessage;
import com.tc.management.remote.protocol.terracotta.L1JmxReady;
import com.tc.net.AddressChecker;
import com.tc.net.NIOWorkarounds;
import com.tc.net.TCSocketAddress;
import com.tc.net.groups.Node;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OOOEventHandler;
import com.tc.net.protocol.delivery.OOONetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.HydrateHandler;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.MessageMonitorImpl;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.HealthCheckerConfigImpl;
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
import com.tc.object.msg.CompletedTransactionLowWaterMarkMessage;
import com.tc.object.msg.JMXMessage;
import com.tc.object.msg.LockRequestMessage;
import com.tc.object.msg.LockResponseMessage;
import com.tc.object.msg.MessageRecycler;
import com.tc.object.msg.ObjectIDBatchRequestMessage;
import com.tc.object.msg.ObjectIDBatchRequestResponseMessage;
import com.tc.object.msg.ObjectsNotFoundMessage;
import com.tc.object.msg.RequestManagedObjectMessageImpl;
import com.tc.object.msg.RequestManagedObjectResponseMessage;
import com.tc.object.msg.RequestRootMessageImpl;
import com.tc.object.msg.RequestRootResponseMessage;
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
import com.tc.objectserver.handler.ClientLockStatisticsHandler;
import com.tc.objectserver.handler.CommitTransactionChangeHandler;
import com.tc.objectserver.handler.GarbageDisposeHandler;
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
import com.tc.objectserver.handler.TransactionLowWaterMarkHandler;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.l1.impl.ClientStateManagerImpl;
import com.tc.objectserver.l1.impl.TransactionAcknowledgeAction;
import com.tc.objectserver.l1.impl.TransactionAcknowledgeActionImpl;
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
import com.tc.objectserver.persistence.sleepycat.TCDatabaseException;
import com.tc.objectserver.tx.CommitTransactionMessageRecycler;
import com.tc.objectserver.tx.CommitTransactionMessageToTransactionBatchReader;
import com.tc.objectserver.tx.ServerTransactionManagerConfig;
import com.tc.objectserver.tx.ServerTransactionManagerImpl;
import com.tc.objectserver.tx.ServerTransactionSequencerImpl;
import com.tc.objectserver.tx.ServerTransactionSequencerStats;
import com.tc.objectserver.tx.TransactionBatchManager;
import com.tc.objectserver.tx.TransactionBatchManagerImpl;
import com.tc.objectserver.tx.TransactionalObjectManager;
import com.tc.objectserver.tx.TransactionalObjectManagerImpl;
import com.tc.objectserver.tx.TransactionalStagesCoordinatorImpl;
import com.tc.properties.L1ReconnectConfigImpl;
import com.tc.properties.ReconnectConfig;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.statistics.StatisticsAgentSubSystem;
import com.tc.statistics.StatisticsAgentSubSystemImpl;
import com.tc.statistics.beans.impl.StatisticsGatewayMBeanImpl;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;
import com.tc.statistics.retrieval.actions.SRACacheObjectsEvictRequest;
import com.tc.statistics.retrieval.actions.SRACacheObjectsEvicted;
import com.tc.statistics.retrieval.actions.SRADistributedGC;
import com.tc.statistics.retrieval.actions.SRAL1ToL2FlushRate;
import com.tc.statistics.retrieval.actions.SRAL2BroadcastCount;
import com.tc.statistics.retrieval.actions.SRAL2BroadcastPerTransaction;
import com.tc.statistics.retrieval.actions.SRAL2ChangesPerBroadcast;
import com.tc.statistics.retrieval.actions.SRAL2FaultsFromDisk;
import com.tc.statistics.retrieval.actions.SRAL2PendingTransactions;
import com.tc.statistics.retrieval.actions.SRAL2ToL1FaultRate;
import com.tc.statistics.retrieval.actions.SRAL2TransactionCount;
import com.tc.statistics.retrieval.actions.SRAMemoryUsage;
import com.tc.statistics.retrieval.actions.SRAMessages;
import com.tc.statistics.retrieval.actions.SRAServerTransactionSequencer;
import com.tc.statistics.retrieval.actions.SRAStageQueueDepths;
import com.tc.statistics.retrieval.actions.SRASystemProperties;
import com.tc.statistics.retrieval.actions.SRAVmGarbageCollector;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.util.Assert;
import com.tc.util.CommonShutDownHook;
import com.tc.util.PortChooser;
import com.tc.util.ProductInfo;
import com.tc.util.SequenceValidator;
import com.tc.util.StartupLock;
import com.tc.util.TCTimeoutException;
import com.tc.util.TCTimerImpl;
import com.tc.util.io.TCFileUtils;
import com.tc.util.sequence.BatchSequence;
import com.tc.util.sequence.MutableSequence;
import com.tc.util.sequence.Sequence;
import com.tc.util.startuplock.FileNotCreatedException;
import com.tc.util.startuplock.LocationNotCreatedException;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.remote.JMXConnectorServer;

/**
 * Startup and shutdown point. Builds and starts the server
 * 
 * @author steve
 */
public class DistributedObjectServer implements TCDumper {
  private final ConnectionPolicy               connectionPolicy;

  private static final TCLogger                logger                   = CustomerLogging.getDSOGenericLogger();
  private static final TCLogger                consoleLogger            = CustomerLogging.getConsoleLogger();

  private static final int                     MAX_DEFAULT_COMM_THREADS = 16;

  private final L2TVSConfigurationSetupManager configSetupManager;
  private final Sink                           httpSink;
  private NetworkListener                      l1Listener;
  private CommunicationsManager                communicationsManager;
  private ServerConfigurationContext           context;
  private ObjectManagerImpl                    objectManager;
  private TransactionalObjectManager           txnObjectManager;
  private CounterManager                       sampledCounterManager;
  private LockManagerImpl                      lockManager;
  private ServerManagementContext              managementContext;
  private StartupLock                          startupLock;

  private ClientStateManager                   clientStateManager;

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

  private LockStatisticsMonitorMBean           lockStatisticsMBean;

  private StatisticsAgentSubSystemImpl         statisticsAgentSubSystem;
  private StatisticsGatewayMBeanImpl           statisticsGateway;

  private final TCThreadGroup                  threadGroup;

  private final SEDA                           seda;

  private ReconnectConfig                      l1ReconnectConfig;

  // used by a test
  public DistributedObjectServer(L2TVSConfigurationSetupManager configSetupManager, TCThreadGroup threadGroup,
                                 ConnectionPolicy connectionPolicy, TCServerInfoMBean tcServerInfoMBean) {
    this(configSetupManager, threadGroup, connectionPolicy, new NullSink(), tcServerInfoMBean, new L2State(),
         new SEDA(threadGroup));

  }

  public DistributedObjectServer(L2TVSConfigurationSetupManager configSetupManager, TCThreadGroup threadGroup,
                                 ConnectionPolicy connectionPolicy, Sink httpSink, TCServerInfoMBean tcServerInfoMBean,
                                 L2State l2State, SEDA seda) {
    // This assertion is here because we want to assume that all threads spawned by the server (including any created in
    // 3rd party libs) inherit their thread group from the current thread . Consider this before removing the assertion.
    // Even in tests, we probably don't want different thread group configurations
    Assert.assertEquals(threadGroup, Thread.currentThread().getThreadGroup());

    this.configSetupManager = configSetupManager;
    this.connectionPolicy = connectionPolicy;
    this.httpSink = httpSink;
    this.tcServerInfoMBean = tcServerInfoMBean;
    this.l2State = l2State;
    this.threadGroup = threadGroup;
    this.seda = seda;
  }

  public void dump() {
    if (this.lockManager != null) {
      this.lockManager.dumpToLogger();
    }

    if (this.objectManager != null) {
      this.objectManager.dumpToLogger();
    }

    if (this.txnObjectManager != null) {
      this.objectManager.dumpToLogger();
    }

    if (this.transactionManager != null) {
      this.objectManager.dumpToLogger();
    }
  }

  public synchronized void start() throws IOException, TCDatabaseException, LocationNotCreatedException,
      FileNotCreatedException {

    L2LockStatsManager lockStatsManager = new L2LockStatisticsManagerImpl();
    try {
      this.lockStatisticsMBean = new LockStatisticsMonitor(lockStatsManager);
    } catch (NotCompliantMBeanException ncmbe) {
      throw new TCRuntimeException("Unable to construct the " + LockStatisticsMonitor.class.getName()
                                   + " MBean; this is a programming error. Please go fix that class.", ncmbe);
    }

    // perform the DSO network config verification
    NewL2DSOConfig l2DSOConfig = configSetupManager.dsoL2Config();

    String bindAddress = l2DSOConfig.bind().getString();
    if (bindAddress == null) {
      // workaround for CDV-584
      bindAddress = TCSocketAddress.WILDCARD_IP;
    }

    InetAddress bind = InetAddress.getByName(bindAddress);

    AddressChecker addressChecker = new AddressChecker();
    if (!addressChecker.isLegalBindAddress(bind)) { throw new IOException("Invalid bind address [" + bind
                                                                          + "]. Local addresses are "
                                                                          + addressChecker.getAllLocalAddresses()); }

    // setup the statistics subsystem
    statisticsAgentSubSystem = new StatisticsAgentSubSystemImpl();
    statisticsAgentSubSystem.setup(configSetupManager.commonl2Config());
    if (TCSocketAddress.WILDCARD_IP.equals(bindAddress) || TCSocketAddress.LOOPBACK_IP.equals(bindAddress)) {
      statisticsAgentSubSystem.setDefaultAgentIp(InetAddress.getLocalHost().getHostAddress());
    } else {
      statisticsAgentSubSystem.setDefaultAgentIp(bind.getHostAddress());
    }
    try {
      statisticsGateway = new StatisticsGatewayMBeanImpl();
    } catch (NotCompliantMBeanException e) {
      throw new TCRuntimeException("Unable to construct the " + StatisticsGatewayMBeanImpl.class.getName()
                                   + " MBean; this is a programming error. Please go fix that class.", e);
    }

    // start the JMX server
    try {
      startJMXServer(bind, configSetupManager.commonl2Config().jmxPort().getInt());
    } catch (Exception e) {
      String msg = "Unable to start the JMX server. Do you have another Terracotta Server running?";
      consoleLogger.error(msg);
      logger.error(msg, e);
      System.exit(-1);
    }

    NIOWorkarounds.solaris10Workaround();

    configSetupManager.commonl2Config().changesInItemIgnored(configSetupManager.commonl2Config().dataPath());
    l2DSOConfig.changesInItemIgnored(l2DSOConfig.persistenceMode());
    PersistenceMode persistenceMode = (PersistenceMode) l2DSOConfig.persistenceMode().getObject();

    l2Properties = TCPropertiesImpl.getProperties().getPropertiesFor("l2");
    TCProperties objManagerProperties = l2Properties.getPropertiesFor("objectmanager");

    l1ReconnectConfig = new L1ReconnectConfigImpl();

    final boolean swapEnabled = true; // 2006-01-31 andrew -- no longer possible to use in-memory only; DSO folks say
    // it's broken
    final boolean persistent = persistenceMode.equals(PersistenceMode.PERMANENT_STORE);

    TCFile location = new TCFileImpl(this.configSetupManager.commonl2Config().dataPath().getFile());
    startupLock = new StartupLock(location, l2Properties.getBoolean("startuplock.retries.enabled"));

    if (!startupLock.canProceed(new TCRandomFileAccessImpl(), persistent)) {
      consoleLogger.error("Another L2 process is using the directory " + location + " as data directory.");
      if (!persistent) {
        consoleLogger.error("This is not allowed with persistence mode set to temporary-swap-only.");
      }
      consoleLogger.error("Exiting...");
      System.exit(1);
    }

    int maxStageSize = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_SEDA_STAGE_SINK_CAPACITY);

    StageManager stageManager = seda.getStageManager();
    SessionManager sessionManager = new NullSessionManager();
    SessionProvider sessionProvider = (SessionProvider) sessionManager;

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
          TCFileUtils.forceDelete(dbhome, "jdb");
          logger.debug("persistence database deleted.");
        }
      }
      logger.debug("persistence database home: " + dbhome);

      DBEnvironment dbenv = new DBEnvironment(persistent, dbhome, l2Properties.getPropertiesFor("berkeleydb")
          .addAllPropertiesTo(new Properties()));
      SerializationAdapterFactory serializationAdapterFactory = new CustomSerializationAdapterFactory();
      persistor = new SleepycatPersistor(TCLogging.getLogger(SleepycatPersistor.class), dbenv,
                                         serializationAdapterFactory);
      // Setting the DB environment for the bean which takes backup of the active server
      if (persistent) {
        ServerDBBackup mbean = l2Management.findServerDbBackupMBean();
        mbean.setDbEnvironment(dbenv.getEnvironment(), dbenv.getEnvironmentHome());
      }
      // DONT DELETE ::This commented code is for replacing SleepyCat with MemoryDataStore as an in-memory DB for
      // testing purpose. You need to include MemoryDataStore in tc.jar and enable with tc.properties
      // l2.memorystore.enabled=true.
      // boolean useMemoryStore = false;
      // if (l2Properties.getProperty("memorystore.enabled", false) != null) {
      // useMemoryStore = l2Properties.getBoolean("memorystore.enabled");
      // }
      // if (useMemoryStore) {
      // persistor = new MemoryStorePersistor(TCLogging.getLogger(MemoryStorePersistor.class));
      // }

      String cachePolicy = l2Properties.getProperty("objectmanager.cachePolicy").toUpperCase();
      if (cachePolicy.equals("LRU")) {
        swapCache = new LRUEvictionPolicy(-1);
      } else if (cachePolicy.equals("LFU")) {
        swapCache = new LFUEvictionPolicy(-1, new LFUConfigImpl(l2Properties.getPropertiesFor("lfu")));
      } else {
        throw new AssertionError("Unknown Cache Policy : " + cachePolicy
                                 + " Accepted Values are : <LRU>/<LFU> Please check tc.properties");
      }
      Sink gcDisposerSink = stageManager
          .createStage(
                       ServerConfigurationContext.GC_DELETE_FROM_DISK_STAGE,
                       new GarbageDisposeHandler(persistor.getManagedObjectPersistor(), persistor
                           .getPersistenceTransactionProvider(), objManagerProperties.getInt("deleteBatchSize")), 1,
                       maxStageSize).getSink();

      objectStore = new PersistentManagedObjectStore(persistor.getManagedObjectPersistor(), gcDisposerSink);
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

    final NetworkStackHarnessFactory networkStackHarnessFactory;
    final boolean useOOOLayer = l1ReconnectConfig.getReconnectEnabled();
    if (useOOOLayer) {
      final Stage oooStage = stageManager.createStage("OOONetStage", new OOOEventHandler(), 1, maxStageSize);
      final int sendQueueCap = l1ReconnectConfig.getSendQueueCapacity();
      networkStackHarnessFactory = new OOONetworkStackHarnessFactory(
                                                                     new OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl(),
                                                                     oooStage.getSink(), l1ReconnectConfig,
                                                                     sendQueueCap);
    } else {
      networkStackHarnessFactory = new PlainNetworkStackHarnessFactory();
    }

    int numCommWorkers = getCommWorkerCount(l2Properties);

    MessageMonitor mm = MessageMonitorImpl.createMonitor(TCPropertiesImpl.getProperties(), logger);

    communicationsManager = new CommunicationsManagerImpl(mm, networkStackHarnessFactory, connectionPolicy,
                                                          numCommWorkers, new HealthCheckerConfigImpl(l2Properties
                                                              .getPropertiesFor("healthcheck.l1"), "DSO Server"));

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
    sampledCounterManager = new CounterManagerImpl();
    SampledCounter objectCreationRate = (SampledCounter) sampledCounterManager
        .createCounter(new SampledCounterConfig(1, 900, true, 0L));
    SampledCounter objectFaultRate = (SampledCounter) sampledCounterManager
        .createCounter(new SampledCounterConfig(1, 900, true, 0L));
    ObjectManagerStatsImpl objMgrStats = new ObjectManagerStatsImpl(objectCreationRate, objectFaultRate);

    SequenceValidator sequenceValidator = new SequenceValidator(0);
    ManagedObjectFaultHandler managedObjectFaultHandler = new ManagedObjectFaultHandler();
    // Server initiated request processing queues shouldn't have any max queue size.
    Stage faultManagedObjectStage = stageManager.createStage(ServerConfigurationContext.MANAGED_OBJECT_FAULT_STAGE,
                                                             managedObjectFaultHandler, l2Properties
                                                                 .getInt("seda.faultstage.threads"), -1);
    ManagedObjectFlushHandler managedObjectFlushHandler = new ManagedObjectFlushHandler();
    Stage flushManagedObjectStage = stageManager.createStage(ServerConfigurationContext.MANAGED_OBJECT_FLUSH_STAGE,
                                                             managedObjectFlushHandler, (persistent ? 1 : l2Properties
                                                                 .getInt("seda.flushstage.threads")), -1);

    ObjectManagerConfig objectManagerConfig = new ObjectManagerConfig(gcInterval * 1000, gcEnabled, verboseGC,
                                                                      persistent);
    objectManager = new ObjectManagerImpl(objectManagerConfig, threadGroup, clientStateManager, objectStore, swapCache,
                                          persistenceTransactionProvider, faultManagedObjectStage.getSink(),
                                          flushManagedObjectStage.getSink());
    objectManager.setStatsListener(objMgrStats);
    objectManager.setGarbageCollector(new MarkAndSweepGarbageCollector(objectManager, clientStateManager, verboseGC,
                                                                       statisticsAgentSubSystem));
    managedObjectChangeListenerProvider.setListener(objectManager);

    l2Management.findObjectManagementMonitorMBean()
        .registerGCController(new GCComptrollerImpl(objectManagerConfig, objectManager.getGarbageCollector()));

    TCProperties cacheManagerProperties = l2Properties.getPropertiesFor("cachemanager");
    if (cacheManagerProperties.getBoolean("enabled")) {
      cacheManager = new CacheManager(objectManager, new CacheConfigImpl(cacheManagerProperties), threadGroup,
                                      statisticsAgentSubSystem);
      if (logger.isDebugEnabled()) {
        logger.debug("CacheManager Enabled : " + cacheManager);
      }
    } else {
      logger.warn("CacheManager is Disabled");
    }

    connectionIdFactory = new ConnectionIDFactoryImpl(clientStateStore);

    l2DSOConfig.changesInItemIgnored(l2DSOConfig.listenPort());
    int serverPort = l2DSOConfig.listenPort().getInt();

    statisticsAgentSubSystem.setDefaultAgentDifferentiator("L2/" + serverPort);

    l1Listener = communicationsManager.createListener(sessionProvider, new TCSocketAddress(bind, serverPort), true,
                                                      connectionIdFactory, httpSink);

    ClientTunnelingEventHandler cteh = new ClientTunnelingEventHandler();

    ProductInfo pInfo = ProductInfo.getInstance();
    DSOChannelManager channelManager = new DSOChannelManagerImpl(l1Listener.getChannelManager(), pInfo.version());
    channelManager.addEventListener(cteh);
    channelManager.addEventListener(connectionIdFactory);

    ChannelStatsImpl channelStats = new ChannelStatsImpl(sampledCounterManager, channelManager);
    channelManager.addEventListener(channelStats);

    lockManager = new LockManagerImpl(channelManager, lockStatsManager);
    threadGroup.addCallbackOnExitHandler(new CallbackDumpAdapter(lockManager));
    ObjectInstanceMonitorImpl instanceMonitor = new ObjectInstanceMonitorImpl();
    TransactionBatchManager transactionBatchManager = new TransactionBatchManagerImpl();
    TransactionAcknowledgeAction taa = new TransactionAcknowledgeActionImpl(channelManager, transactionBatchManager);
    SampledCounter globalTxnCounter = (SampledCounter) sampledCounterManager
        .createCounter(new SampledCounterConfig(1, 300, true, 0L));

    SampledCounter broadcastCounter = (SampledCounter) sampledCounterManager
        .createCounter(new SampledCounterConfig(1, 300, true, 0L));
    SampledCounter changeCounter = (SampledCounter) sampledCounterManager.createCounter(new SampledCounterConfig(1,
                                                                                                                 300,
                                                                                                                 true,
                                                                                                                 0L));

    SampledCounter globalObjectFaultCounter = (SampledCounter) sampledCounterManager
        .createCounter(new SampledCounterConfig(1, 300, true, 0L));
    SampledCounter globalObjectFlushCounter = (SampledCounter) sampledCounterManager
        .createCounter(new SampledCounterConfig(1, 300, true, 0L));

    DSOGlobalServerStats serverStats = new DSOGlobalServerStatsImpl(globalObjectFlushCounter, globalObjectFaultCounter,
                                                                    globalTxnCounter, objMgrStats, broadcastCounter,
                                                                    changeCounter);

    final TransactionStore transactionStore = new TransactionStoreImpl(transactionPersistor,
                                                                       globalTransactionIDSequence);
    ServerGlobalTransactionManager gtxm = new ServerGlobalTransactionManagerImpl(sequenceValidator, transactionStore,
                                                                                 transactionStorePTP,
                                                                                 gidSequenceProvider,
                                                                                 globalTransactionIDSequence);

    TransactionalStagesCoordinatorImpl txnStageCoordinator = new TransactionalStagesCoordinatorImpl(stageManager);
    ServerTransactionSequencerImpl serverTransactionSequencerImpl = new ServerTransactionSequencerImpl();
    txnObjectManager = new TransactionalObjectManagerImpl(objectManager, serverTransactionSequencerImpl, gtxm,
                                                          txnStageCoordinator);
    threadGroup.addCallbackOnExitHandler(new CallbackDumpAdapter(txnObjectManager));
    objectManager.setTransactionalObjectManager(txnObjectManager);
    threadGroup.addCallbackOnExitHandler(new CallbackDumpAdapter(objectManager));
    transactionManager = new ServerTransactionManagerImpl(gtxm, transactionStore, lockManager, clientStateManager,
                                                          objectManager, txnObjectManager, taa, globalTxnCounter,
                                                          channelStats, new ServerTransactionManagerConfig(l2Properties
                                                              .getPropertiesFor("transactionmanager")));
    threadGroup.addCallbackOnExitHandler(new CallbackDumpAdapter(transactionManager));

    MessageRecycler recycler = new CommitTransactionMessageRecycler(transactionManager);
    ObjectRequestManager objectRequestManager = new ObjectRequestManagerImpl(objectManager, transactionManager);

    stageManager.createStage(ServerConfigurationContext.TRANSACTION_LOOKUP_STAGE, new TransactionLookupHandler(), 1,
                             maxStageSize);

    // Lookup stage should never be blocked trying to add to apply stage
    stageManager.createStage(ServerConfigurationContext.APPLY_CHANGES_STAGE,
                             new ApplyTransactionChangeHandler(instanceMonitor, transactionManager), 1, -1);

    stageManager.createStage(ServerConfigurationContext.APPLY_COMPLETE_STAGE, new ApplyCompleteTransactionHandler(), 1,
                             maxStageSize);

    // Server initiated request processing stages should not be bounded
    stageManager.createStage(ServerConfigurationContext.RECALL_OBJECTS_STAGE, new RecallObjectsHandler(), 1, -1);

    int commitThreads = (persistent ? l2Properties.getInt("seda.commitstage.threads") : 1);
    stageManager.createStage(ServerConfigurationContext.COMMIT_CHANGES_STAGE,
                             new CommitTransactionChangeHandler(transactionStorePTP), commitThreads, maxStageSize);

    txnStageCoordinator.lookUpSinks();

    Stage processTx = stageManager.createStage(ServerConfigurationContext.PROCESS_TRANSACTION_STAGE,
                                               new ProcessTransactionHandler(transactionBatchManager,
                                                                             sequenceValidator, recycler), 1,
                                               maxStageSize);

    Stage rootRequest = stageManager.createStage(ServerConfigurationContext.MANAGED_ROOT_REQUEST_STAGE,
                                                 new RequestRootHandler(), 1, maxStageSize);

    BroadcastChangeHandler broadcastChangeHandler = new BroadcastChangeHandler(broadcastCounter, changeCounter);
    stageManager.createStage(ServerConfigurationContext.BROADCAST_CHANGES_STAGE, broadcastChangeHandler, 1,
                             maxStageSize);
    stageManager.createStage(ServerConfigurationContext.RESPOND_TO_LOCK_REQUEST_STAGE,
                             new RespondToRequestLockHandler(), 1, maxStageSize);
    Stage requestLock = stageManager.createStage(ServerConfigurationContext.REQUEST_LOCK_STAGE,
                                                 new RequestLockUnLockHandler(), 1, maxStageSize);
    ChannelLifeCycleHandler channelLifeCycleHandler = new ChannelLifeCycleHandler(communicationsManager,
                                                                                  transactionManager,
                                                                                  transactionBatchManager,
                                                                                  channelManager);
    stageManager.createStage(ServerConfigurationContext.CHANNEL_LIFE_CYCLE_STAGE, channelLifeCycleHandler, 1,
                             maxStageSize);
    channelManager.addEventListener(channelLifeCycleHandler);

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
    final Stage txnLwmStage = stageManager.createStage(ServerConfigurationContext.TRANSACTION_LOWWATERMARK_STAGE,
                                                       new TransactionLowWaterMarkHandler(gtxm), 1, maxStageSize);

    Stage jmxEventsStage = stageManager.createStage(ServerConfigurationContext.JMX_EVENTS_STAGE,
                                                    new JMXEventsHandler(appEvents), 1, maxStageSize);

    final Stage jmxRemoteConnectStage = stageManager.createStage(ServerConfigurationContext.JMXREMOTE_CONNECT_STAGE,
                                                                 new ClientConnectEventHandler(statisticsGateway), 1,
                                                                 maxStageSize);

    final Stage jmxRemoteDisconnectStage = stageManager
        .createStage(ServerConfigurationContext.JMXREMOTE_DISCONNECT_STAGE,
                     new ClientConnectEventHandler(statisticsGateway), 1, maxStageSize);

    cteh.setStages(jmxRemoteConnectStage.getSink(), jmxRemoteDisconnectStage.getSink());
    final Stage jmxRemoteTunnelStage = stageManager.createStage(ServerConfigurationContext.JMXREMOTE_TUNNEL_STAGE,
                                                                cteh, 1, maxStageSize);

    final Stage clientLockStatisticsRespondStage = stageManager
        .createStage(ServerConfigurationContext.CLIENT_LOCK_STATISTICS_RESPOND_STAGE,
                     new ClientLockStatisticsHandler(lockStatsManager), 1, 1);

    l1Listener.addClassMapping(TCMessageType.BATCH_TRANSACTION_ACK_MESSAGE,
                               BatchTransactionAcknowledgeMessageImpl.class);
    l1Listener.addClassMapping(TCMessageType.REQUEST_ROOT_MESSAGE, RequestRootMessageImpl.class);
    l1Listener.addClassMapping(TCMessageType.LOCK_REQUEST_MESSAGE, LockRequestMessage.class);
    l1Listener.addClassMapping(TCMessageType.LOCK_RESPONSE_MESSAGE, LockResponseMessage.class);
    l1Listener.addClassMapping(TCMessageType.LOCK_RECALL_MESSAGE, LockResponseMessage.class);
    l1Listener.addClassMapping(TCMessageType.LOCK_QUERY_RESPONSE_MESSAGE, LockResponseMessage.class);
    l1Listener.addClassMapping(TCMessageType.LOCK_STAT_MESSAGE, LockStatisticsMessage.class);
    l1Listener.addClassMapping(TCMessageType.LOCK_STATISTICS_RESPONSE_MESSAGE, LockStatisticsResponseMessage.class);
    l1Listener.addClassMapping(TCMessageType.COMMIT_TRANSACTION_MESSAGE, CommitTransactionMessageImpl.class);
    l1Listener.addClassMapping(TCMessageType.REQUEST_ROOT_RESPONSE_MESSAGE, RequestRootResponseMessage.class);
    l1Listener.addClassMapping(TCMessageType.REQUEST_MANAGED_OBJECT_MESSAGE, RequestManagedObjectMessageImpl.class);
    l1Listener.addClassMapping(TCMessageType.REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE,
                               RequestManagedObjectResponseMessage.class);
    l1Listener.addClassMapping(TCMessageType.OBJECTS_NOT_FOUND_RESPONSE_MESSAGE, ObjectsNotFoundMessage.class);
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
    l1Listener.addClassMapping(TCMessageType.COMPLETED_TRANSACTION_LOWWATERMARK_MESSAGE,
                               CompletedTransactionLowWaterMarkMessage.class);

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
    l1Listener.routeMessageType(TCMessageType.LOCK_STATISTICS_RESPONSE_MESSAGE, clientLockStatisticsRespondStage
        .getSink(), hydrateSink);
    l1Listener.routeMessageType(TCMessageType.COMPLETED_TRANSACTION_LOWWATERMARK_MESSAGE, txnLwmStage.getSink(),
                                hydrateSink);

    l2DSOConfig.changesInItemIgnored(l2DSOConfig.clientReconnectWindow());
    long reconnectTimeout = l2DSOConfig.clientReconnectWindow().getInt();
    logger.debug("Client Reconnect Window: " + reconnectTimeout + " seconds");
    reconnectTimeout *= 1000;
    ServerClientHandshakeManager clientHandshakeManager = new ServerClientHandshakeManager(
                                                                                           TCLogging
                                                                                               .getLogger(ServerClientHandshakeManager.class),
                                                                                           channelManager,
                                                                                           transactionManager,
                                                                                           sequenceValidator,
                                                                                           clientStateManager,
                                                                                           lockManager,
                                                                                           stageManager
                                                                                               .getStage(
                                                                                                         ServerConfigurationContext.RESPOND_TO_LOCK_REQUEST_STAGE)
                                                                                               .getSink(),
                                                                                           stageManager
                                                                                               .getStage(
                                                                                                         ServerConfigurationContext.OBJECT_ID_BATCH_REQUEST_STAGE)
                                                                                               .getSink(),
                                                                                           new TCTimerImpl(
                                                                                                           "Reconnect timer",
                                                                                                           true),
                                                                                           reconnectTimeout,
                                                                                           persistent, consoleLogger);

    boolean networkedHA = configSetupManager.haConfig().isNetworkedActivePassive();
    if (networkedHA) {
      logger.info("L2 Networked HA Enabled ");
      l2Coordinator = new L2HACoordinator(configSetupManager, consoleLogger, this, stageManager, persistor
          .getClusterStateStore(), objectManager, transactionManager, gtxm, channelManager, configSetupManager
          .haConfig(), recycler);
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

    // populate the statistics retrieval registry
    populateStatisticsRetrievalRegistry(serverStats, seda.getStageManager(), mm, managedObjectFaultHandler,
                                        transactionManager, serverTransactionSequencerImpl);

    // XXX: yucky casts
    managementContext = new ServerManagementContext(transactionManager, (ObjectManagerMBean) objectManager,
                                                    (LockManagerMBean) lockManager,
                                                    (DSOChannelManagerMBean) channelManager, serverStats, channelStats,
                                                    instanceMonitor, appEvents);

    if (l2Properties.getBoolean("beanshell.enabled")) startBeanShell(l2Properties.getInt("beanshell.port"));

    lockStatsManager.start(channelManager);

    if (networkedHA) {
      final Node thisNode = makeThisNode(bind);
      final Node[] allNodes = makeAllNodes();
      l2Coordinator.start(thisNode, allNodes);
    } else {
      // In non-network enabled HA, Only active server reached here.
      startActiveMode();
    }
    setLoggerOnExit();
  }

  private void setLoggerOnExit() {
    CommonShutDownHook.addShutdownHook(new Runnable() {
      public void run() {
        logger.info("L2 Exiting...");
      }
    });
  }

  private void populateStatisticsRetrievalRegistry(final DSOGlobalServerStats serverStats,
                                                   final StageManager stageManager,
                                                   final MessageMonitor messageMonitor,
                                                   final ManagedObjectFaultHandler managedObjectFaultHandler,
                                                   final ServerTransactionManagerImpl txnManager,
                                                   final ServerTransactionSequencerStats serverTransactionSequencerStats) {
    if (statisticsAgentSubSystem.isActive()) {
      StatisticsRetrievalRegistry registry = statisticsAgentSubSystem.getStatisticsRetrievalRegistry();
      registry.registerActionInstance(new SRAL2ToL1FaultRate(serverStats));
      registry.registerActionInstance(new SRAMemoryUsage());
      registry.registerActionInstance(new SRASystemProperties());
      registry.registerActionInstance("com.tc.statistics.retrieval.actions.SRACpu");
      registry.registerActionInstance("com.tc.statistics.retrieval.actions.SRANetworkActivity");
      registry.registerActionInstance("com.tc.statistics.retrieval.actions.SRADiskActivity");
      registry.registerActionInstance("com.tc.statistics.retrieval.actions.SRAThreadDump");
      registry.registerActionInstance(new SRAL2TransactionCount(serverStats));
      registry.registerActionInstance(new SRAL2BroadcastCount(serverStats));
      registry.registerActionInstance(new SRAL2ChangesPerBroadcast(serverStats));
      registry.registerActionInstance(new SRAL2BroadcastPerTransaction(serverStats));
      registry.registerActionInstance(new SRAStageQueueDepths(stageManager));
      registry.registerActionInstance(new SRACacheObjectsEvictRequest());
      registry.registerActionInstance(new SRACacheObjectsEvicted());
      registry.registerActionInstance(new SRADistributedGC());
      registry.registerActionInstance(new SRAVmGarbageCollector());
      registry.registerActionInstance(new SRAMessages(messageMonitor));
      registry.registerActionInstance(new SRAL2FaultsFromDisk(managedObjectFaultHandler));
      registry.registerActionInstance(new SRAL1ToL2FlushRate(serverStats));
      registry.registerActionInstance(new SRAL2PendingTransactions(txnManager));
      registry.registerActionInstance(new SRAServerTransactionSequencer(serverTransactionSequencerStats));
    }
  }

  private int getCommWorkerCount(TCProperties props) {
    int def = Math.min(Runtime.getRuntime().availableProcessors(), MAX_DEFAULT_COMM_THREADS);
    return props.getInt("tccom.workerthreads", def);
  }

  public boolean isBlocking() {
    return startupLock != null && startupLock.isBlocking();
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
      rv[i] = makeNode(l2, null);
    }
    return rv;
  }

  private static Node makeNode(NewL2DSOConfig l2, String bind) {
    // NOTE: until we resolve Tribes stepping on TCComm's port
    // we'll use TCComm.port + 1 in Tribes
    int dsoPort = l2.listenPort().getInt();
    if (dsoPort == 0) {
      return new Node(l2.host().getString(), dsoPort, bind);
    } else {
      return new Node(l2.host().getString(), l2.l2GroupPort().getInt(), bind);
    }
  }

  private Node makeThisNode(InetAddress bind) {
    NewL2DSOConfig l2 = configSetupManager.dsoL2Config();
    return makeNode(l2, bind.getHostAddress());
  }

  public boolean startActiveMode() throws IOException {
    transactionManager.goToActiveMode();
    Set existingConnections = Collections.unmodifiableSet(connectionIdFactory.loadConnectionIDs());
    context.getClientHandshakeManager().setStarting(existingConnections);
    l1Listener.start(existingConnections);
    consoleLogger.info("Terracotta Server has started up as ACTIVE node on " + format(l1Listener)
                       + " successfully, and is now ready for work.");
    return true;
  }

  private static String format(NetworkListener listener) {
    StringBuilder sb = new StringBuilder(listener.getBindAddress().getHostAddress());
    sb.append(':');
    sb.append(listener.getBindPort());
    return sb.toString();
  }

  public boolean stopActiveMode() throws TCTimeoutException {
    // TODO:: Make this not take timeout and force stop
    consoleLogger.info("Stopping ACTIVE Terracotta Server on " + format(l1Listener) + ".");
    l1Listener.stop(10000);
    l1Listener.getChannelManager().closeAllChannels();
    return true;
  }

  public void startBeanShell(int port) {
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

  /**
   * Since this is accessed via JMX and l1Listener isn't initialed when a secondary is waiting on the lock file, use the
   * config value unless the special value 0 is specified for use in the tests to get a random port.
   */
  public int getListenPort() {
    NewL2DSOConfig l2DSOConfig = configSetupManager.dsoL2Config();
    int configValue = l2DSOConfig.listenPort().getInt();
    if (configValue != 0) { return configValue; }
    if (this.l1Listener != null) {
      try {
        return this.l1Listener.getBindPort();
      } catch (IllegalStateException ise) {/**/
      }
    }
    return -1;
  }

  public InetAddress getListenAddr() {
    return this.l1Listener.getBindAddress();
  }

  public synchronized void stop() {
    try {
      statisticsAgentSubSystem.cleanup();
    } catch (Throwable e) {
      logger.warn(e);
    }

    try {
      statisticsGateway.cleanup();
    } catch (Throwable e) {
      logger.warn(e);
    }

    try {
      if (lockManager != null) lockManager.stop();
    } catch (InterruptedException e) {
      logger.error(e);
    }

    seda.getStageManager().stopAll();

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

  public JMXConnectorServer getJMXConnServer() {
    return l2Management.getJMXConnServer();
  }

  public StatisticsAgentSubSystem getStatisticsAgentSubSystem() {
    return statisticsAgentSubSystem;
  }

  public StatisticsGatewayMBeanImpl getStatisticsGateway() {
    return statisticsGateway;
  }

  private void startJMXServer(InetAddress bind, int jmxPort) throws Exception {
    if (jmxPort == 0) {
      jmxPort = new PortChooser().chooseRandomPort();
    }

    l2Management = new L2Management(tcServerInfoMBean, lockStatisticsMBean, statisticsAgentSubSystem,
                                    statisticsGateway, configSetupManager, this, bind, jmxPort);

    /*
     * Some tests use this if they run with jdk1.4 and start multiple in-process DistributedObjectServers. When we no
     * longer support 1.4, this can be removed. See com.tctest.LockManagerSystemTest.
     */
    if (!Boolean.getBoolean("org.terracotta.server.disableJmxConnector")) {
      l2Management.start();
    }
  }

  private void stopJMXServer() throws Exception {
    statisticsAgentSubSystem.disableJMX();

    try {
      if (l2Management != null) {
        l2Management.stop();
      }
    } finally {
      l2Management = null;
    }
  }

  public ReconnectConfig getL1ReconnectProperties() {
    return l1ReconnectConfig;
  }
}