/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import bsh.EvalError;
import bsh.Interpreter;

import com.tc.async.api.PostInit;
import com.tc.async.api.SEDA;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.async.impl.NullSink;
import com.tc.config.HaConfig;
import com.tc.config.HaConfigImpl;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.exception.CleanDirtyDatabaseException;
import com.tc.exception.TCRuntimeException;
import com.tc.exception.ZapDirtyDbServerNodeException;
import com.tc.exception.ZapServerNodeException;
import com.tc.handler.CallbackDatabaseDirtyAlertAdapter;
import com.tc.handler.CallbackDirtyDatabaseExceptionAdapter;
import com.tc.handler.CallbackDumpAdapter;
import com.tc.handler.CallbackGroupExceptionHandler;
import com.tc.handler.CallbackZapDirtyDbExceptionAdapter;
import com.tc.handler.CallbackZapServerNodeExceptionAdapter;
import com.tc.handler.LockInfoDumpHandler;
import com.tc.io.TCFile;
import com.tc.io.TCFileImpl;
import com.tc.io.TCRandomFileAccessImpl;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.ha.L2HACoordinator;
import com.tc.l2.ha.L2HADisabledCooridinator;
import com.tc.l2.state.StateManager;
import com.tc.lang.TCThreadGroup;
import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.logging.ThreadDumpHandler;
import com.tc.management.L2LockStatsManager;
import com.tc.management.L2Management;
import com.tc.management.RemoteJMXProcessor;
import com.tc.management.beans.L2State;
import com.tc.management.beans.LockStatisticsMonitor;
import com.tc.management.beans.LockStatisticsMonitorMBean;
import com.tc.management.beans.TCDumper;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.management.beans.object.ServerDBBackup;
import com.tc.management.beans.object.ObjectManagementMonitor.ObjectIdsFetcher;
import com.tc.management.lock.stats.L2LockStatisticsManagerImpl;
import com.tc.management.lock.stats.LockStatisticsMessage;
import com.tc.management.lock.stats.LockStatisticsResponseMessageImpl;
import com.tc.management.remote.connect.ClientConnectEventHandler;
import com.tc.management.remote.protocol.terracotta.ClientTunnelingEventHandler;
import com.tc.management.remote.protocol.terracotta.JmxRemoteTunnelMessage;
import com.tc.management.remote.protocol.terracotta.L1JmxReady;
import com.tc.net.AddressChecker;
import com.tc.net.NIOWorkarounds;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.TCSocketAddress;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.Node;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OOOEventHandler;
import com.tc.net.protocol.delivery.OOONetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl;
import com.tc.net.protocol.tcm.ChannelManager;
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
import com.tc.object.cache.CacheConfig;
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
import com.tc.object.msg.KeysForOrphanedValuesMessageImpl;
import com.tc.object.msg.KeysForOrphanedValuesResponseMessageImpl;
import com.tc.object.msg.LockRequestMessage;
import com.tc.object.msg.LockResponseMessage;
import com.tc.object.msg.NodeMetaDataMessageImpl;
import com.tc.object.msg.NodeMetaDataResponseMessageImpl;
import com.tc.object.msg.NodesWithObjectsMessageImpl;
import com.tc.object.msg.NodesWithObjectsResponseMessageImpl;
import com.tc.object.msg.ObjectIDBatchRequestMessage;
import com.tc.object.msg.ObjectIDBatchRequestResponseMessage;
import com.tc.object.msg.ObjectsNotFoundMessageImpl;
import com.tc.object.msg.RequestManagedObjectMessageImpl;
import com.tc.object.msg.RequestManagedObjectResponseMessageImpl;
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
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectManagerMBean;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.clustermetadata.ServerClusterMetaDataManager;
import com.tc.objectserver.clustermetadata.ServerClusterMetaDataManagerImpl;
import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.objectserver.core.api.DSOGlobalServerStatsImpl;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ServerManagementContext;
import com.tc.objectserver.dgc.api.GarbageCollectionInfoPublisher;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.dgc.impl.GCComptrollerImpl;
import com.tc.objectserver.dgc.impl.GCStatisticsAgentSubSystemEventListener;
import com.tc.objectserver.dgc.impl.GCStatsEventPublisher;
import com.tc.objectserver.dgc.impl.GarbageCollectionInfoPublisherImpl;
import com.tc.objectserver.dgc.impl.GarbageCollectorThread;
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
import com.tc.objectserver.handler.ServerClusterMetaDataHandler;
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
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
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
import com.tc.objectserver.persistence.sleepycat.DatabaseDirtyException;
import com.tc.objectserver.persistence.sleepycat.SerializationAdapterFactory;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor;
import com.tc.objectserver.persistence.sleepycat.TCDatabaseException;
import com.tc.objectserver.tx.CommitTransactionMessageRecycler;
import com.tc.objectserver.tx.ServerTransactionManagerConfig;
import com.tc.objectserver.tx.ServerTransactionManagerImpl;
import com.tc.objectserver.tx.ServerTransactionSequencerImpl;
import com.tc.objectserver.tx.ServerTransactionSequencerStats;
import com.tc.objectserver.tx.TransactionBatchManagerImpl;
import com.tc.objectserver.tx.TransactionFilter;
import com.tc.objectserver.tx.TransactionalObjectManager;
import com.tc.objectserver.tx.TransactionalObjectManagerImpl;
import com.tc.objectserver.tx.TransactionalStagesCoordinatorImpl;
import com.tc.properties.L1ReconnectConfigImpl;
import com.tc.properties.ReconnectConfig;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.runtime.TCMemoryManagerImpl;
import com.tc.runtime.logging.LongGCLogger;
import com.tc.statistics.StatisticsAgentSubSystem;
import com.tc.statistics.StatisticsAgentSubSystemImpl;
import com.tc.statistics.beans.impl.StatisticsGatewayMBeanImpl;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;
import com.tc.statistics.retrieval.actions.SRABerkeleyDB;
import com.tc.statistics.retrieval.actions.SRACacheObjectsEvictRequest;
import com.tc.statistics.retrieval.actions.SRACacheObjectsEvicted;
import com.tc.statistics.retrieval.actions.SRADistributedGC;
import com.tc.statistics.retrieval.actions.SRAGlobalLockRecallCount;
import com.tc.statistics.retrieval.actions.SRAL1ReferenceCount;
import com.tc.statistics.retrieval.actions.SRAL1ToL2FlushRate;
import com.tc.statistics.retrieval.actions.SRAL2BroadcastCount;
import com.tc.statistics.retrieval.actions.SRAL2BroadcastPerTransaction;
import com.tc.statistics.retrieval.actions.SRAL2ChangesPerBroadcast;
import com.tc.statistics.retrieval.actions.SRAL2FaultsFromDisk;
import com.tc.statistics.retrieval.actions.SRAL2GlobalLockCount;
import com.tc.statistics.retrieval.actions.SRAL2PendingTransactions;
import com.tc.statistics.retrieval.actions.SRAL2ToL1FaultRate;
import com.tc.statistics.retrieval.actions.SRAL2TransactionCount;
import com.tc.statistics.retrieval.actions.SRAMemoryUsage;
import com.tc.statistics.retrieval.actions.SRAMessages;
import com.tc.statistics.retrieval.actions.SRAServerTransactionSequencer;
import com.tc.statistics.retrieval.actions.SRAStageQueueDepths;
import com.tc.statistics.retrieval.actions.SRASystemProperties;
import com.tc.statistics.retrieval.actions.SRAVmGarbageCollector;
import com.tc.statistics.retrieval.actions.SRAVmGarbageCollector.SRAVmGarbageCollectorType;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.stats.counter.sampled.derived.SampledRateCounterConfig;
import com.tc.util.Assert;
import com.tc.util.CommonShutDownHook;
import com.tc.util.PortChooser;
import com.tc.util.ProductInfo;
import com.tc.util.SequenceValidator;
import com.tc.util.StartupLock;
import com.tc.util.TCTimeoutException;
import com.tc.util.UUID;
import com.tc.util.concurrent.StoppableThread;
import com.tc.util.io.TCFileUtils;
import com.tc.util.runtime.LockInfoByThreadID;
import com.tc.util.runtime.NullThreadIDMapImpl;
import com.tc.util.runtime.ThreadIDMap;
import com.tc.util.sequence.BatchSequence;
import com.tc.util.sequence.MutableSequence;
import com.tc.util.sequence.Sequence;
import com.tc.util.startuplock.FileNotCreatedException;
import com.tc.util.startuplock.LocationNotCreatedException;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;

import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.remote.JMXConnectorServer;

/**
 * Startup and shutdown point. Builds and starts the server
 */
public class DistributedObjectServer implements TCDumper, LockInfoDumpHandler {
  private final ConnectionPolicy                 connectionPolicy;
  private final TCServerInfoMBean                tcServerInfoMBean;
  private final ObjectStatsRecorder              objectStatsRecorder;
  private final L2State                          l2State;
  private final DSOServerBuilder                 serverBuilder;
  protected final L2TVSConfigurationSetupManager configSetupManager;
  private final Sink                             httpSink;
  protected final HaConfig                       haConfig;

  private static final int                       MAX_DEFAULT_COMM_THREADS = 16;
  private static final TCLogger                  logger                   = CustomerLogging.getDSOGenericLogger();
  private static final TCLogger                  consoleLogger            = CustomerLogging.getConsoleLogger();

  private ServerID                               thisServerNodeID         = ServerID.NULL_ID;
  protected NetworkListener                      l1Listener;
  protected GCStatsEventPublisher                gcStatsEventPublisher;
  private CommunicationsManager                  communicationsManager;
  private ServerConfigurationContext             context;
  private ObjectManagerImpl                      objectManager;
  private ObjectRequestManager                   objectRequestManager;
  private TransactionalObjectManager             txnObjectManager;
  private CounterManager                         sampledCounterManager;
  private LockManagerImpl                        lockManager;
  private ServerManagementContext                managementContext;
  private StartupLock                            startupLock;
  private ClientStateManager                     clientStateManager;
  private ManagedObjectStore                     objectStore;
  private Persistor                              persistor;
  private ServerTransactionManagerImpl           transactionManager;

  private CacheManager                           cacheManager;

  private L2Management                           l2Management;
  private L2Coordinator                          l2Coordinator;

  private TCProperties                           l2Properties;

  private ConnectionIDFactoryImpl                connectionIdFactory;

  private LockStatisticsMonitorMBean             lockStatisticsMBean;

  private StatisticsAgentSubSystemImpl           statisticsAgentSubSystem;
  private StatisticsGatewayMBeanImpl             statisticsGateway;

  private final TCThreadGroup                    threadGroup;
  private final SEDA                             seda;

  private ReconnectConfig                        l1ReconnectConfig;

  private GroupManager                           groupCommManager;
  private Stage                                  hydrateStage;

  // used by a test
  public DistributedObjectServer(final L2TVSConfigurationSetupManager configSetupManager,
                                 final TCThreadGroup threadGroup, final ConnectionPolicy connectionPolicy,
                                 final TCServerInfoMBean tcServerInfoMBean,
                                 final ObjectStatsRecorder objectStatsRecorder) {
    this(configSetupManager, threadGroup, connectionPolicy, new NullSink(), tcServerInfoMBean, objectStatsRecorder,
         new L2State(), new SEDA(threadGroup));

  }

  public DistributedObjectServer(final L2TVSConfigurationSetupManager configSetupManager,
                                 final TCThreadGroup threadGroup, final ConnectionPolicy connectionPolicy,
                                 final Sink httpSink, final TCServerInfoMBean tcServerInfoMBean,
                                 final ObjectStatsRecorder objectStatsRecorder, final L2State l2State, final SEDA seda) {
    // This assertion is here because we want to assume that all threads spawned by the server (including any created in
    // 3rd party libs) inherit their thread group from the current thread . Consider this before removing the assertion.
    // Even in tests, we probably don't want different thread group configurations
    Assert.assertEquals(threadGroup, Thread.currentThread().getThreadGroup());

    this.configSetupManager = configSetupManager;
    this.haConfig = new HaConfigImpl(this.configSetupManager);
    this.connectionPolicy = connectionPolicy;
    this.httpSink = httpSink;
    this.tcServerInfoMBean = tcServerInfoMBean;
    this.objectStatsRecorder = objectStatsRecorder;
    this.l2State = l2State;
    this.threadGroup = threadGroup;
    this.seda = seda;
    this.serverBuilder = createServerBuilder(this.haConfig, logger);
  }

  protected DSOServerBuilder createServerBuilder(final HaConfig config, final TCLogger tcLogger) {
    Assert.assertEquals(config.isActiveActive(), false);
    return new StandardDSOServerBuilder(tcLogger);
  }

  protected DSOServerBuilder getServerBuilder() {
    return this.serverBuilder;
  }

  public void dump() {
    if (this.lockManager != null) {
      this.lockManager.dumpToLogger();
    }

    if (this.objectManager != null) {
      this.objectManager.dumpToLogger();
    }

    if (this.txnObjectManager != null) {
      this.txnObjectManager.dumpToLogger();
    }

    if (this.transactionManager != null) {
      this.transactionManager.dumpToLogger();
    }
    this.serverBuilder.dump();
  }

  public synchronized void start() throws IOException, TCDatabaseException, LocationNotCreatedException,
      FileNotCreatedException {

    this.threadGroup.addCallbackOnExitDefaultHandler(new ThreadDumpHandler(this));
    this.thisServerNodeID = makeServerNodeID(this.configSetupManager.dsoL2Config());
    L2LockStatsManager lockStatsManager = new L2LockStatisticsManagerImpl();

    List<PostInit> toInit = new ArrayList<PostInit>();

    try {
      this.lockStatisticsMBean = new LockStatisticsMonitor(lockStatsManager);
    } catch (NotCompliantMBeanException ncmbe) {
      throw new TCRuntimeException("Unable to construct the " + LockStatisticsMonitor.class.getName()
                                   + " MBean; this is a programming error. Please go fix that class.", ncmbe);
    }

    // perform the DSO network config verification
    NewL2DSOConfig l2DSOConfig = this.configSetupManager.dsoL2Config();

    // verify user input host name, DEV-2293
    String host = l2DSOConfig.host().getString();
    InetAddress ip = InetAddress.getByName(host);
    if (!ip.isLoopbackAddress() && (NetworkInterface.getByInetAddress(ip) == null)) {
      String msg = "Unable to find local network interface for " + host;
      consoleLogger.error(msg);
      logger.error(msg, new TCRuntimeException(msg));
      System.exit(-1);
    }

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
    this.statisticsAgentSubSystem = new StatisticsAgentSubSystemImpl();
    if (!this.statisticsAgentSubSystem.setup(this.configSetupManager.commonl2Config())) {
      System.exit(-1);
    }
    if (TCSocketAddress.WILDCARD_IP.equals(bindAddress) || TCSocketAddress.LOOPBACK_IP.equals(bindAddress)) {
      this.statisticsAgentSubSystem.setDefaultAgentIp(InetAddress.getLocalHost().getHostAddress());
    } else {
      this.statisticsAgentSubSystem.setDefaultAgentIp(bind.getHostAddress());
    }
    try {
      this.statisticsGateway = new StatisticsGatewayMBeanImpl();
    } catch (NotCompliantMBeanException e) {
      throw new TCRuntimeException("Unable to construct the " + StatisticsGatewayMBeanImpl.class.getName()
                                   + " MBean; this is a programming error. Please go fix that class.", e);
    }

    // start the JMX server
    try {
      startJMXServer(bind, this.configSetupManager.commonl2Config().jmxPort().getInt(), new RemoteJMXProcessor());
    } catch (Exception e) {
      String msg = "Unable to start the JMX server. Do you have another Terracotta Server instance running?";
      consoleLogger.error(msg);
      logger.error(msg, e);
      System.exit(-1);
    }

    NIOWorkarounds.solaris10Workaround();

    this.configSetupManager.commonl2Config().changesInItemIgnored(this.configSetupManager.commonl2Config().dataPath());
    l2DSOConfig.changesInItemIgnored(l2DSOConfig.persistenceMode());
    PersistenceMode persistenceMode = (PersistenceMode) l2DSOConfig.persistenceMode().getObject();

    this.l2Properties = TCPropertiesImpl.getProperties().getPropertiesFor("l2");
    TCProperties objManagerProperties = this.l2Properties.getPropertiesFor("objectmanager");

    this.l1ReconnectConfig = new L1ReconnectConfigImpl();

    final boolean swapEnabled = true;
    final boolean persistent = persistenceMode.equals(PersistenceMode.PERMANENT_STORE);

    TCFile location = new TCFileImpl(this.configSetupManager.commonl2Config().dataPath().getFile());
    this.startupLock = new StartupLock(location, this.l2Properties.getBoolean("startuplock.retries.enabled"));

    if (!this.startupLock.canProceed(new TCRandomFileAccessImpl(), persistent)) {
      consoleLogger.error("Another L2 process is using the directory " + location + " as data directory.");
      if (!persistent) {
        consoleLogger.error("This is not allowed with persistence mode set to temporary-swap-only.");
      }
      consoleLogger.error("Exiting...");
      System.exit(1);
    }

    int maxStageSize = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_SEDA_STAGE_SINK_CAPACITY);

    StageManager stageManager = this.seda.getStageManager();
    SessionManager sessionManager = new NullSessionManager();
    SessionProvider sessionProvider = (SessionProvider) sessionManager;

    EvictionPolicy swapCache;
    final ClientStatePersistor clientStateStore;
    final PersistenceTransactionProvider persistenceTransactionProvider;
    final TransactionPersistor transactionPersistor;
    final Sequence globalTransactionIDSequence;
    final GarbageCollectionInfoPublisher gcPublisher = new GarbageCollectionInfoPublisherImpl();
    logger.debug("server swap enabled: " + swapEnabled);
    final ManagedObjectChangeListenerProviderImpl managedObjectChangeListenerProvider = new ManagedObjectChangeListenerProviderImpl();
    SRABerkeleyDB sraBdb = null;

    if (swapEnabled) {
      File dbhome = new File(this.configSetupManager.commonl2Config().dataPath().getFile(),
                             NewL2DSOConfig.OBJECTDB_DIRNAME);
      logger.debug("persistent: " + persistent);
      if (!persistent) {
        if (dbhome.exists()) {
          logger.debug("deleting persistence database...");
          TCFileUtils.forceDelete(dbhome, "jdb");
          logger.debug("persistence database deleted.");
        }
      }
      logger.debug("persistence database home: " + dbhome);

      CallbackOnExitHandler dirtydbHandler = new CallbackDatabaseDirtyAlertAdapter(logger, consoleLogger);
      this.threadGroup.addCallbackOnExitExceptionHandler(DatabaseDirtyException.class, dirtydbHandler);

      DBEnvironment dbenv = new DBEnvironment(persistent, dbhome, this.l2Properties.getPropertiesFor("berkeleydb")
          .addAllPropertiesTo(new Properties()));
      SerializationAdapterFactory serializationAdapterFactory = new CustomSerializationAdapterFactory();

      this.persistor = new SleepycatPersistor(TCLogging.getLogger(SleepycatPersistor.class), dbenv,
                                              serializationAdapterFactory, this.configSetupManager.commonl2Config()
                                                  .dataPath().getFile(), this.objectStatsRecorder);
      sraBdb = new SRABerkeleyDB((SleepycatPersistor) this.persistor);
      // Setting the DB environment for the bean which takes backup of the active server
      if (persistent) {
        ServerDBBackup mbean = this.l2Management.findServerDbBackupMBean();
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

      String cachePolicy = this.l2Properties.getProperty("objectmanager.cachePolicy").toUpperCase();
      if (cachePolicy.equals("LRU")) {
        swapCache = new LRUEvictionPolicy(-1);
      } else if (cachePolicy.equals("LFU")) {
        swapCache = new LFUEvictionPolicy(-1, new LFUConfigImpl(this.l2Properties.getPropertiesFor("lfu")));
      } else {
        throw new AssertionError("Unknown Cache Policy : " + cachePolicy
                                 + " Accepted Values are : <LRU>/<LFU> Please check tc.properties");
      }
      int gcDeleteThreads = this.l2Properties.getInt("seda.gcdeletestage.threads");
      Sink gcDisposerSink = stageManager.createStage(
                                                     ServerConfigurationContext.GC_DELETE_FROM_DISK_STAGE,
                                                     new GarbageDisposeHandler(gcPublisher, this.persistor
                                                         .getManagedObjectPersistor(), this.persistor
                                                         .getPersistenceTransactionProvider(), objManagerProperties
                                                         .getInt("deleteBatchSize")), gcDeleteThreads, maxStageSize)
          .getSink();

      this.objectStore = new PersistentManagedObjectStore(this.persistor.getManagedObjectPersistor(), gcDisposerSink);
    } else {
      this.persistor = new InMemoryPersistor();
      swapCache = new NullCache();
      this.objectStore = new InMemoryManagedObjectStore(new HashMap());
    }

    this.threadGroup
        .addCallbackOnExitExceptionHandler(CleanDirtyDatabaseException.class,
                                           new CallbackDirtyDatabaseExceptionAdapter(logger, consoleLogger,
                                                                                     this.persistor
                                                                                         .getClusterStateStore()));
    this.threadGroup
        .addCallbackOnExitExceptionHandler(ZapDirtyDbServerNodeException.class,
                                           new CallbackZapDirtyDbExceptionAdapter(logger, consoleLogger, this.persistor
                                               .getClusterStateStore()));
    this.threadGroup
        .addCallbackOnExitExceptionHandler(ZapServerNodeException.class,
                                           new CallbackZapServerNodeExceptionAdapter(logger, consoleLogger,
                                                                                     this.persistor
                                                                                         .getClusterStateStore()));

    persistenceTransactionProvider = this.persistor.getPersistenceTransactionProvider();
    PersistenceTransactionProvider transactionStorePTP;
    MutableSequence gidSequence;
    if (persistent) {
      gidSequence = this.persistor.getGlobalTransactionIDSequence();

      transactionPersistor = this.persistor.getTransactionPersistor();
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

    clientStateStore = this.persistor.getClientStatePersistor();

    ManagedObjectStateFactory.createInstance(managedObjectChangeListenerProvider, this.persistor);

    int numCommWorkers = getCommWorkerCount(this.l2Properties);

    final NetworkStackHarnessFactory networkStackHarnessFactory;
    final boolean useOOOLayer = this.l1ReconnectConfig.getReconnectEnabled();
    if (useOOOLayer) {
      final Stage oooSendStage = stageManager.createStage(ServerConfigurationContext.OOO_NET_SEND_STAGE,
                                                          new OOOEventHandler(), numCommWorkers, maxStageSize);
      final Stage oooReceiveStage = stageManager.createStage(ServerConfigurationContext.OOO_NET_RECEIVE_STAGE,
                                                             new OOOEventHandler(), numCommWorkers, maxStageSize);
      networkStackHarnessFactory = new OOONetworkStackHarnessFactory(
                                                                     new OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl(),
                                                                     oooSendStage.getSink(), oooReceiveStage.getSink(),
                                                                     this.l1ReconnectConfig);
    } else {
      networkStackHarnessFactory = new PlainNetworkStackHarnessFactory();
    }

    MessageMonitor mm = MessageMonitorImpl.createMonitor(TCPropertiesImpl.getProperties(), logger);

    this.communicationsManager = new CommunicationsManagerImpl(mm, networkStackHarnessFactory, this.connectionPolicy,
                                                               numCommWorkers,
                                                               new HealthCheckerConfigImpl(this.l2Properties
                                                                   .getPropertiesFor("healthcheck.l1"), "DSO Server"),
                                                               this.thisServerNodeID);

    final DSOApplicationEvents appEvents;
    try {
      appEvents = new DSOApplicationEvents();
    } catch (NotCompliantMBeanException ncmbe) {
      throw new TCRuntimeException("Unable to construct the " + DSOApplicationEvents.class.getName()
                                   + " MBean; this is a programming error. Please go fix that class.", ncmbe);
    }

    this.clientStateManager = new ClientStateManagerImpl(TCLogging.getLogger(ClientStateManager.class));

    l2DSOConfig.changesInItemIgnored(l2DSOConfig.garbageCollectionEnabled());
    boolean gcEnabled = l2DSOConfig.garbageCollectionEnabled().getBoolean();

    l2DSOConfig.changesInItemIgnored(l2DSOConfig.garbageCollectionInterval());
    long gcInterval = l2DSOConfig.garbageCollectionInterval().getInt();

    l2DSOConfig.changesInItemIgnored(l2DSOConfig.garbageCollectionVerbose());
    boolean verboseGC = l2DSOConfig.garbageCollectionVerbose().getBoolean();
    this.sampledCounterManager = new CounterManagerImpl();
    SampledCounterConfig sampledCounterConfig = new SampledCounterConfig(1, 300, true, 0L);
    SampledCounter objectCreationRate = (SampledCounter) this.sampledCounterManager.createCounter(sampledCounterConfig);
    SampledCounter objectFaultRate = (SampledCounter) this.sampledCounterManager.createCounter(sampledCounterConfig);
    ObjectManagerStatsImpl objMgrStats = new ObjectManagerStatsImpl(objectCreationRate, objectFaultRate);
    SampledCounter l2FaultFromDisk = (SampledCounter) this.sampledCounterManager.createCounter(sampledCounterConfig);
    SampledCounter time2FaultFromDisk = (SampledCounter) this.sampledCounterManager.createCounter(sampledCounterConfig);
    SampledCounter time2Add2ObjMgr = (SampledCounter) this.sampledCounterManager.createCounter(sampledCounterConfig);

    SequenceValidator sequenceValidator = new SequenceValidator(0);
    // Server initiated request processing queues shouldn't have any max queue size.
    ManagedObjectFaultHandler managedObjectFaultHandler = new ManagedObjectFaultHandler(l2FaultFromDisk,
                                                                                        time2FaultFromDisk,
                                                                                        time2Add2ObjMgr,
                                                                                        this.objectStatsRecorder);
    Stage faultManagedObjectStage = stageManager.createStage(ServerConfigurationContext.MANAGED_OBJECT_FAULT_STAGE,
                                                             managedObjectFaultHandler, this.l2Properties
                                                                 .getInt("seda.faultstage.threads"), -1);
    ManagedObjectFlushHandler managedObjectFlushHandler = new ManagedObjectFlushHandler(this.objectStatsRecorder);
    Stage flushManagedObjectStage = stageManager.createStage(ServerConfigurationContext.MANAGED_OBJECT_FLUSH_STAGE,
                                                             managedObjectFlushHandler,
                                                             (persistent ? 1 : this.l2Properties
                                                                 .getInt("seda.flushstage.threads")), -1);
    TCProperties youngDGCProperties = objManagerProperties.getPropertiesFor("dgc").getPropertiesFor("young");
    boolean enableYoungGenDGC = youngDGCProperties.getBoolean("enabled");
    long youngGenDGCFrequency = youngDGCProperties.getLong("frequencyInMillis");

    ObjectManagerConfig objectManagerConfig = new ObjectManagerConfig(gcInterval * 1000, gcEnabled, verboseGC,
                                                                      persistent, enableYoungGenDGC,
                                                                      youngGenDGCFrequency);

    this.objectManager = new ObjectManagerImpl(objectManagerConfig, this.clientStateManager, this.objectStore,
                                               swapCache, persistenceTransactionProvider, faultManagedObjectStage
                                                   .getSink(), flushManagedObjectStage.getSink(),
                                               this.objectStatsRecorder);

    this.objectManager.setStatsListener(objMgrStats);
    this.gcStatsEventPublisher = new GCStatsEventPublisher();
    managedObjectChangeListenerProvider.setListener(this.objectManager);

    TCProperties cacheManagerProperties = this.l2Properties.getPropertiesFor("cachemanager");
    CacheConfig cacheConfig = new CacheConfigImpl(cacheManagerProperties);
    TCMemoryManagerImpl tcMemManager = new TCMemoryManagerImpl(cacheConfig.getSleepInterval(), cacheConfig
        .getLeastCount(), cacheConfig.isOnlyOldGenMonitored(), this.threadGroup);
    long timeOut = TCPropertiesImpl.getProperties().getLong(TCPropertiesConsts.LOGGING_LONG_GC_THRESHOLD);
    LongGCLogger gcLogger = new LongGCLogger(logger, timeOut);
    tcMemManager.registerForMemoryEvents(gcLogger);
    // CDV-1181 warn if using CMS
    tcMemManager.checkGarbageCollectors();

    if (cacheManagerProperties.getBoolean("enabled")) {
      this.cacheManager = new CacheManager(this.objectManager, cacheConfig, this.threadGroup,
                                           this.statisticsAgentSubSystem, tcMemManager);
      this.cacheManager.start();
      if (logger.isDebugEnabled()) {
        logger.debug("CacheManager Enabled : " + this.cacheManager);
      }
    } else {
      logger.warn("CacheManager is Disabled");
    }

    this.connectionIdFactory = new ConnectionIDFactoryImpl(clientStateStore);

    l2DSOConfig.changesInItemIgnored(l2DSOConfig.listenPort());
    int serverPort = l2DSOConfig.listenPort().getInt();

    this.statisticsAgentSubSystem.setDefaultAgentDifferentiator("L2/" + serverPort);

    this.l1Listener = this.communicationsManager.createListener(sessionProvider, new TCSocketAddress(bind, serverPort),
                                                                true, this.connectionIdFactory, this.httpSink);

    ClientTunnelingEventHandler cteh = new ClientTunnelingEventHandler();

    ProductInfo pInfo = ProductInfo.getInstance();
    DSOChannelManager channelManager = new DSOChannelManagerImpl(this.l1Listener.getChannelManager(),
                                                                 this.communicationsManager.getConnectionManager(),
                                                                 pInfo.version());
    channelManager.addEventListener(cteh);
    channelManager.addEventListener(this.connectionIdFactory);

    ChannelStatsImpl channelStats = new ChannelStatsImpl(this.sampledCounterManager, channelManager);
    channelManager.addEventListener(channelStats);

    CommitTransactionMessageRecycler recycler = new CommitTransactionMessageRecycler();
    toInit.add(recycler);

    this.lockManager = new LockManagerImpl(channelManager, lockStatsManager);
    this.threadGroup.addCallbackOnExitDefaultHandler(new CallbackDumpAdapter(this.lockManager));
    ObjectInstanceMonitorImpl instanceMonitor = new ObjectInstanceMonitorImpl();

    TransactionFilter txnFilter = this.serverBuilder.getTransactionFilter(toInit, stageManager, maxStageSize);
    TransactionBatchManagerImpl transactionBatchManager = new TransactionBatchManagerImpl(sequenceValidator, recycler,
                                                                                          txnFilter);
    toInit.add(transactionBatchManager);

    TransactionAcknowledgeAction taa = new TransactionAcknowledgeActionImpl(channelManager, transactionBatchManager);
    SampledCounter globalTxnCounter = (SampledCounter) this.sampledCounterManager.createCounter(sampledCounterConfig);

    SampledCounter broadcastCounter = (SampledCounter) this.sampledCounterManager.createCounter(sampledCounterConfig);

    SampledCounter globalObjectFaultCounter = (SampledCounter) this.sampledCounterManager
        .createCounter(sampledCounterConfig);
    SampledCounter globalObjectFlushCounter = (SampledCounter) this.sampledCounterManager
        .createCounter(sampledCounterConfig);
    SampledCounter globalLockRecallCounter = (SampledCounter) this.sampledCounterManager
        .createCounter(sampledCounterConfig);
    SampledRateCounterConfig sampledRateCounterConfig = new SampledRateCounterConfig(1, 300, true);
    SampledRateCounter changesPerBroadcast = (SampledRateCounter) this.sampledCounterManager
        .createCounter(sampledRateCounterConfig);
    SampledRateCounter transactionSizeCounter = (SampledRateCounter) this.sampledCounterManager
        .createCounter(sampledRateCounterConfig);
    SampledCounter globalLockCount = (SampledCounter) this.sampledCounterManager.createCounter(sampledCounterConfig);

    DSOGlobalServerStats serverStats = new DSOGlobalServerStatsImpl(globalObjectFlushCounter, globalObjectFaultCounter,
                                                                    globalTxnCounter, objMgrStats, broadcastCounter,
                                                                    l2FaultFromDisk, time2FaultFromDisk,
                                                                    time2Add2ObjMgr, globalLockRecallCounter,
                                                                    changesPerBroadcast, transactionSizeCounter,
                                                                    globalLockCount);

    final TransactionStore transactionStore = new TransactionStoreImpl(transactionPersistor,
                                                                       globalTransactionIDSequence);
    ServerGlobalTransactionManager gtxm = new ServerGlobalTransactionManagerImpl(sequenceValidator, transactionStore,
                                                                                 transactionStorePTP,
                                                                                 gidSequenceProvider,
                                                                                 globalTransactionIDSequence);

    TransactionalStagesCoordinatorImpl txnStageCoordinator = new TransactionalStagesCoordinatorImpl(stageManager);
    ServerTransactionSequencerImpl serverTransactionSequencerImpl = new ServerTransactionSequencerImpl();
    this.txnObjectManager = new TransactionalObjectManagerImpl(this.objectManager, serverTransactionSequencerImpl,
                                                               gtxm, txnStageCoordinator);
    this.threadGroup.addCallbackOnExitDefaultHandler(new CallbackDumpAdapter(this.txnObjectManager));
    this.objectManager.setTransactionalObjectManager(this.txnObjectManager);
    this.threadGroup.addCallbackOnExitDefaultHandler(new CallbackDumpAdapter(this.objectManager));
    this.transactionManager = new ServerTransactionManagerImpl(gtxm, transactionStore, this.lockManager,
                                                               this.clientStateManager, this.objectManager,
                                                               this.txnObjectManager, taa, globalTxnCounter,
                                                               channelStats,
                                                               new ServerTransactionManagerConfig(this.l2Properties
                                                                   .getPropertiesFor("transactionmanager")),
                                                               this.objectStatsRecorder);
    this.threadGroup.addCallbackOnExitDefaultHandler(new CallbackDumpAdapter(this.transactionManager));

    ServerClusterMetaDataManager clusterMetaDataManager = new ServerClusterMetaDataManagerImpl(logger,
                                                                                               this.clientStateManager,
                                                                                               this.objectManager,
                                                                                               channelManager);

    stageManager.createStage(ServerConfigurationContext.TRANSACTION_LOOKUP_STAGE, new TransactionLookupHandler(), 1,
                             maxStageSize);

    // Lookup stage should never be blocked trying to add to apply stage
    stageManager.createStage(ServerConfigurationContext.APPLY_CHANGES_STAGE,
                             new ApplyTransactionChangeHandler(instanceMonitor, this.transactionManager), 1, -1);

    stageManager.createStage(ServerConfigurationContext.APPLY_COMPLETE_STAGE, new ApplyCompleteTransactionHandler(), 1,
                             maxStageSize);

    // Server initiated request processing stages should not be bounded
    stageManager.createStage(ServerConfigurationContext.RECALL_OBJECTS_STAGE, new RecallObjectsHandler(), 1, -1);

    int commitThreads = (persistent ? this.l2Properties.getInt("seda.commitstage.threads") : 1);
    stageManager.createStage(ServerConfigurationContext.COMMIT_CHANGES_STAGE,
                             new CommitTransactionChangeHandler(transactionStorePTP), commitThreads, maxStageSize);

    txnStageCoordinator.lookUpSinks();

    Stage processTx = stageManager.createStage(ServerConfigurationContext.PROCESS_TRANSACTION_STAGE,
                                               new ProcessTransactionHandler(transactionBatchManager), 1, maxStageSize);

    Stage rootRequest = stageManager.createStage(ServerConfigurationContext.MANAGED_ROOT_REQUEST_STAGE,
                                                 new RequestRootHandler(), 1, maxStageSize);

    BroadcastChangeHandler broadcastChangeHandler = new BroadcastChangeHandler(broadcastCounter,
                                                                               this.objectStatsRecorder,
                                                                               changesPerBroadcast);
    stageManager.createStage(ServerConfigurationContext.BROADCAST_CHANGES_STAGE, broadcastChangeHandler, 1,
                             maxStageSize);
    stageManager.createStage(ServerConfigurationContext.RESPOND_TO_LOCK_REQUEST_STAGE,
                             new RespondToRequestLockHandler(), 1, maxStageSize);
    Stage requestLock = stageManager.createStage(ServerConfigurationContext.REQUEST_LOCK_STAGE,
                                                 new RequestLockUnLockHandler(), 1, maxStageSize);
    ChannelLifeCycleHandler channelLifeCycleHandler = new ChannelLifeCycleHandler(this.communicationsManager,
                                                                                  transactionBatchManager,
                                                                                  channelManager, this.haConfig);
    stageManager.createStage(ServerConfigurationContext.CHANNEL_LIFE_CYCLE_STAGE, channelLifeCycleHandler, 1,
                             maxStageSize);
    channelManager.addEventListener(channelLifeCycleHandler);

    Stage objectRequestStage = stageManager.createStage(ServerConfigurationContext.MANAGED_OBJECT_REQUEST_STAGE,
                                                        new ManagedObjectRequestHandler(globalObjectFaultCounter,
                                                                                        globalObjectFlushCounter), 1,
                                                        maxStageSize);
    Stage respondToObjectRequestStage = stageManager
        .createStage(ServerConfigurationContext.RESPOND_TO_OBJECT_REQUEST_STAGE, new RespondToObjectRequestHandler(),
                     4, maxStageSize);

    this.objectRequestManager = this.serverBuilder.createObjectRequestManager(this.objectManager, channelManager,
                                                                              this.clientStateManager,
                                                                              this.transactionManager,
                                                                              objectRequestStage.getSink(),
                                                                              respondToObjectRequestStage.getSink(),
                                                                              this.objectStatsRecorder, toInit,
                                                                              stageManager, maxStageSize);
    Stage oidRequest = stageManager.createStage(ServerConfigurationContext.OBJECT_ID_BATCH_REQUEST_STAGE,
                                                new RequestObjectIDBatchHandler(this.objectStore), 1, maxStageSize);
    Stage transactionAck = stageManager.createStage(ServerConfigurationContext.TRANSACTION_ACKNOWLEDGEMENT_STAGE,
                                                    new TransactionAcknowledgementHandler(), 1, maxStageSize);
    Stage clientHandshake = stageManager.createStage(ServerConfigurationContext.CLIENT_HANDSHAKE_STAGE,
                                                     new ClientHandshakeHandler(), 1, maxStageSize);
    this.hydrateStage = stageManager.createStage(ServerConfigurationContext.HYDRATE_MESSAGE_SINK, new HydrateHandler(),
                                                 1, maxStageSize);
    final Stage txnLwmStage = stageManager.createStage(ServerConfigurationContext.TRANSACTION_LOWWATERMARK_STAGE,
                                                       new TransactionLowWaterMarkHandler(gtxm), 1, maxStageSize);

    Stage jmxEventsStage = stageManager.createStage(ServerConfigurationContext.JMX_EVENTS_STAGE,
                                                    new JMXEventsHandler(appEvents), 1, maxStageSize);

    final Stage jmxRemoteConnectStage = stageManager.createStage(ServerConfigurationContext.JMXREMOTE_CONNECT_STAGE,
                                                                 new ClientConnectEventHandler(this.statisticsGateway),
                                                                 1, maxStageSize);

    final Stage jmxRemoteDisconnectStage = stageManager
        .createStage(ServerConfigurationContext.JMXREMOTE_DISCONNECT_STAGE,
                     new ClientConnectEventHandler(this.statisticsGateway), 1, maxStageSize);

    cteh.setStages(jmxRemoteConnectStage.getSink(), jmxRemoteDisconnectStage.getSink());
    final Stage jmxRemoteTunnelStage = stageManager.createStage(ServerConfigurationContext.JMXREMOTE_TUNNEL_STAGE,
                                                                cteh, 1, maxStageSize);

    final Stage clientLockStatisticsRespondStage = stageManager
        .createStage(ServerConfigurationContext.CLIENT_LOCK_STATISTICS_RESPOND_STAGE,
                     new ClientLockStatisticsHandler(lockStatsManager), 1, 1);

    Stage clusterMetaDataStage = stageManager.createStage(ServerConfigurationContext.CLUSTER_METADATA_STAGE,
                                                          new ServerClusterMetaDataHandler(), 1, maxStageSize);

    initClassMappings();
    initRouteMessages(processTx, rootRequest, requestLock, objectRequestStage, oidRequest, transactionAck,
                      clientHandshake, txnLwmStage, jmxEventsStage, jmxRemoteTunnelStage,
                      clientLockStatisticsRespondStage, clusterMetaDataStage);

    l2DSOConfig.changesInItemIgnored(l2DSOConfig.clientReconnectWindow());
    long reconnectTimeout = l2DSOConfig.clientReconnectWindow().getInt();
    logger.debug("Client Reconnect Window: " + reconnectTimeout + " seconds");
    reconnectTimeout *= 1000;
    ServerClientHandshakeManager clientHandshakeManager = new ServerClientHandshakeManager(
                                                                                           TCLogging
                                                                                               .getLogger(ServerClientHandshakeManager.class),
                                                                                           channelManager,
                                                                                           this.transactionManager,
                                                                                           transactionBatchManager,
                                                                                           sequenceValidator,
                                                                                           this.clientStateManager,
                                                                                           this.lockManager,
                                                                                           stageManager
                                                                                               .getStage(
                                                                                                         ServerConfigurationContext.RESPOND_TO_LOCK_REQUEST_STAGE)
                                                                                               .getSink(),
                                                                                           stageManager
                                                                                               .getStage(
                                                                                                         ServerConfigurationContext.OBJECT_ID_BATCH_REQUEST_STAGE)
                                                                                               .getSink(),
                                                                                           new Timer("Reconnect timer",
                                                                                                     true),
                                                                                           reconnectTimeout,
                                                                                           persistent, consoleLogger);

    boolean networkedHA = this.haConfig.isNetworkedActivePassive();
    this.groupCommManager = this.serverBuilder.createGroupCommManager(networkedHA, this.configSetupManager,
                                                                      stageManager, this.thisServerNodeID,
                                                                      this.httpSink);

    // initialize the garbage collector
    GarbageCollector gc = this.serverBuilder.createGarbageCollector(toInit, objectManagerConfig, this.objectManager,
                                                                    this.clientStateManager, stageManager,
                                                                    maxStageSize, gcPublisher, this.objectManager,
                                                                    this.clientStateManager,
                                                                    getGcStatsEventPublisher(),
                                                                    getStatisticsAgentSubSystem());
    gc.addListener(new GCStatisticsAgentSubSystemEventListener(getStatisticsAgentSubSystem()));
    this.objectManager.setGarbageCollector(gc);
    if (objectManagerConfig.startGCThread()) {
      StoppableThread st = new GarbageCollectorThread(this.threadGroup, "DGC-Thread", gc, objectManagerConfig);
      st.setDaemon(true);
      gc.setState(st);
    }
    this.l2Management.findObjectManagementMonitorMBean().registerGCController(
                                                                              new GCComptrollerImpl(this.objectManager
                                                                                  .getGarbageCollector()));
    this.l2Management.findObjectManagementMonitorMBean().registerObjectIdFetcher(new ObjectIdsFetcher() {
      public Set getAllObjectIds() {
        return DistributedObjectServer.this.objectManager.getAllObjectIDs();
      }
    });

    if (networkedHA) {

      logger.info("L2 Networked HA Enabled ");
      this.l2Coordinator = new L2HACoordinator(consoleLogger, this, stageManager, this.groupCommManager, this.persistor
          .getClusterStateStore(), this.objectManager, this.transactionManager, gtxm, channelManager,
                                               this.configSetupManager.haConfig(), recycler);
      this.l2Coordinator.getStateManager().registerForStateChangeEvents(this.l2State);
    } else {
      this.l2State.setState(StateManager.ACTIVE_COORDINATOR);
      this.l2Coordinator = new L2HADisabledCooridinator(this.groupCommManager);
    }

    this.context = this.serverBuilder.createServerConfigurationContext(stageManager, this.objectManager,
                                                                       this.objectRequestManager, this.objectStore,
                                                                       this.lockManager, channelManager,
                                                                       this.clientStateManager,
                                                                       this.transactionManager, this.txnObjectManager,
                                                                       channelStats, this.l2Coordinator,
                                                                       transactionBatchManager, gtxm,
                                                                       clientHandshakeManager, clusterMetaDataManager,
                                                                       serverStats);
    toInit.add(this.serverBuilder);

    stageManager.startAll(this.context, toInit);

    // populate the statistics retrieval register
    populateStatisticsRetrievalRegistry(serverStats, this.seda.getStageManager(), mm, this.transactionManager,
                                        serverTransactionSequencerImpl, sraBdb);

    // XXX: yucky casts
    this.managementContext = new ServerManagementContext(this.transactionManager,
                                                         (ObjectManagerMBean) this.objectManager,
                                                         (LockManagerMBean) this.lockManager,
                                                         (DSOChannelManagerMBean) channelManager, serverStats,
                                                         channelStats, instanceMonitor, appEvents);
    if (this.l2Properties.getBoolean("beanshell.enabled")) {
      startBeanShell(this.l2Properties.getInt("beanshell.port"));
    }

    lockStatsManager.start(channelManager, serverStats);

    CallbackOnExitHandler handler = new CallbackGroupExceptionHandler(logger, consoleLogger);
    this.threadGroup.addCallbackOnExitExceptionHandler(GroupException.class, handler);

    startGroupManagers();
    this.l2Coordinator.start();
    if (!networkedHA) {
      // In non-network enabled HA, Only active server reached here.
      startActiveMode();
    }
    setLoggerOnExit();
  }

  public void startGroupManagers() {
    try {
      NodeID myNodeId = this.groupCommManager.join(this.haConfig.getThisNode(), this.haConfig.getThisGroupNodes());
      logger.info("This L2 Node ID = " + myNodeId);
    } catch (GroupException e) {
      logger.error("Caught Exception :", e);
      throw new RuntimeException(e);
    }
  }

  protected void initRouteMessages(final Stage processTx, final Stage rootRequest, final Stage requestLock,
                                   final Stage objectRequestStage, final Stage oidRequest, final Stage transactionAck,
                                   final Stage clientHandshake, final Stage txnLwmStage, final Stage jmxEventsStage,
                                   final Stage jmxRemoteTunnelStage, final Stage clientLockStatisticsRespondStage,
                                   final Stage clusterMetaDataStage) {
    Sink hydrateSink = this.hydrateStage.getSink();
    this.l1Listener.routeMessageType(TCMessageType.COMMIT_TRANSACTION_MESSAGE, processTx.getSink(), hydrateSink);
    this.l1Listener.routeMessageType(TCMessageType.LOCK_REQUEST_MESSAGE, requestLock.getSink(), hydrateSink);
    this.l1Listener.routeMessageType(TCMessageType.REQUEST_ROOT_MESSAGE, rootRequest.getSink(), hydrateSink);
    this.l1Listener.routeMessageType(TCMessageType.REQUEST_MANAGED_OBJECT_MESSAGE, objectRequestStage.getSink(),
                                     hydrateSink);
    this.l1Listener.routeMessageType(TCMessageType.OBJECT_ID_BATCH_REQUEST_MESSAGE, oidRequest.getSink(), hydrateSink);
    this.l1Listener.routeMessageType(TCMessageType.ACKNOWLEDGE_TRANSACTION_MESSAGE, transactionAck.getSink(),
                                     hydrateSink);
    this.l1Listener.routeMessageType(TCMessageType.CLIENT_HANDSHAKE_MESSAGE, clientHandshake.getSink(), hydrateSink);
    this.l1Listener.routeMessageType(TCMessageType.JMX_MESSAGE, jmxEventsStage.getSink(), hydrateSink);
    this.l1Listener.routeMessageType(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE,
                                     jmxRemoteTunnelStage.getSink(), hydrateSink);
    this.l1Listener.routeMessageType(TCMessageType.CLIENT_JMX_READY_MESSAGE, jmxRemoteTunnelStage.getSink(),
                                     hydrateSink);
    this.l1Listener.routeMessageType(TCMessageType.LOCK_STATISTICS_RESPONSE_MESSAGE, clientLockStatisticsRespondStage
        .getSink(), hydrateSink);
    this.l1Listener.routeMessageType(TCMessageType.COMPLETED_TRANSACTION_LOWWATERMARK_MESSAGE, txnLwmStage.getSink(),
                                     hydrateSink);
    this.l1Listener.routeMessageType(TCMessageType.NODES_WITH_OBJECTS_MESSAGE, clusterMetaDataStage.getSink(),
                                     hydrateSink);
    this.l1Listener.routeMessageType(TCMessageType.KEYS_FOR_ORPHANED_VALUES_MESSAGE, clusterMetaDataStage.getSink(),
                                     hydrateSink);
    this.l1Listener.routeMessageType(TCMessageType.NODE_META_DATA_MESSAGE, clusterMetaDataStage.getSink(), hydrateSink);

  }

  protected void initClassMappings() {
    this.l1Listener.addClassMapping(TCMessageType.BATCH_TRANSACTION_ACK_MESSAGE,
                                    BatchTransactionAcknowledgeMessageImpl.class);
    this.l1Listener.addClassMapping(TCMessageType.REQUEST_ROOT_MESSAGE, RequestRootMessageImpl.class);
    this.l1Listener.addClassMapping(TCMessageType.LOCK_REQUEST_MESSAGE, LockRequestMessage.class);
    this.l1Listener.addClassMapping(TCMessageType.LOCK_RESPONSE_MESSAGE, LockResponseMessage.class);
    this.l1Listener.addClassMapping(TCMessageType.LOCK_RECALL_MESSAGE, LockResponseMessage.class);
    this.l1Listener.addClassMapping(TCMessageType.LOCK_QUERY_RESPONSE_MESSAGE, LockResponseMessage.class);
    this.l1Listener.addClassMapping(TCMessageType.LOCK_STAT_MESSAGE, LockStatisticsMessage.class);
    this.l1Listener
        .addClassMapping(TCMessageType.LOCK_STATISTICS_RESPONSE_MESSAGE, LockStatisticsResponseMessageImpl.class);
    this.l1Listener.addClassMapping(TCMessageType.COMMIT_TRANSACTION_MESSAGE, CommitTransactionMessageImpl.class);
    this.l1Listener.addClassMapping(TCMessageType.REQUEST_ROOT_RESPONSE_MESSAGE, RequestRootResponseMessage.class);
    this.l1Listener
        .addClassMapping(TCMessageType.REQUEST_MANAGED_OBJECT_MESSAGE, RequestManagedObjectMessageImpl.class);
    this.l1Listener.addClassMapping(TCMessageType.REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE,
                                    RequestManagedObjectResponseMessageImpl.class);
    this.l1Listener.addClassMapping(TCMessageType.OBJECTS_NOT_FOUND_RESPONSE_MESSAGE, ObjectsNotFoundMessageImpl.class);
    this.l1Listener.addClassMapping(TCMessageType.BROADCAST_TRANSACTION_MESSAGE, BroadcastTransactionMessageImpl.class);
    this.l1Listener.addClassMapping(TCMessageType.OBJECT_ID_BATCH_REQUEST_MESSAGE, ObjectIDBatchRequestMessage.class);
    this.l1Listener.addClassMapping(TCMessageType.OBJECT_ID_BATCH_REQUEST_RESPONSE_MESSAGE,
                                    ObjectIDBatchRequestResponseMessage.class);
    this.l1Listener.addClassMapping(TCMessageType.ACKNOWLEDGE_TRANSACTION_MESSAGE,
                                    AcknowledgeTransactionMessageImpl.class);
    this.l1Listener.addClassMapping(TCMessageType.CLIENT_HANDSHAKE_MESSAGE, ClientHandshakeMessageImpl.class);
    this.l1Listener.addClassMapping(TCMessageType.CLIENT_HANDSHAKE_ACK_MESSAGE, ClientHandshakeAckMessageImpl.class);
    this.l1Listener.addClassMapping(TCMessageType.JMX_MESSAGE, JMXMessage.class);
    this.l1Listener.addClassMapping(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE, JmxRemoteTunnelMessage.class);
    this.l1Listener.addClassMapping(TCMessageType.CLUSTER_MEMBERSHIP_EVENT_MESSAGE, ClusterMembershipMessage.class);
    this.l1Listener.addClassMapping(TCMessageType.CLIENT_JMX_READY_MESSAGE, L1JmxReady.class);
    this.l1Listener.addClassMapping(TCMessageType.COMPLETED_TRANSACTION_LOWWATERMARK_MESSAGE,
                                    CompletedTransactionLowWaterMarkMessage.class);
    this.l1Listener.addClassMapping(TCMessageType.NODES_WITH_OBJECTS_MESSAGE, NodesWithObjectsMessageImpl.class);
    this.l1Listener.addClassMapping(TCMessageType.NODES_WITH_OBJECTS_RESPONSE_MESSAGE,
                                    NodesWithObjectsResponseMessageImpl.class);
    this.l1Listener.addClassMapping(TCMessageType.KEYS_FOR_ORPHANED_VALUES_MESSAGE,
                                    KeysForOrphanedValuesMessageImpl.class);
    this.l1Listener.addClassMapping(TCMessageType.KEYS_FOR_ORPHANED_VALUES_RESPONSE_MESSAGE,
                                    KeysForOrphanedValuesResponseMessageImpl.class);
    this.l1Listener.addClassMapping(TCMessageType.NODE_META_DATA_MESSAGE, NodeMetaDataMessageImpl.class);
    this.l1Listener.addClassMapping(TCMessageType.NODE_META_DATA_RESPONSE_MESSAGE,
                                    NodeMetaDataResponseMessageImpl.class);
  }

  protected TCLogger getLogger() {
    return logger;
  }

  private ServerID makeServerNodeID(final NewL2DSOConfig l2DSOConfig) {
    String nodeName = new Node(l2DSOConfig.host().getString(), l2DSOConfig.listenPort().getInt()).getServerNodeName();
    ServerID aNodeID = new ServerID(nodeName, UUID.getUUID().toString().getBytes());
    logger.info("Creating server nodeID: " + aNodeID);
    return aNodeID;
  }

  public ServerID getServerNodeID() {
    return this.thisServerNodeID;
  }

  // for testing purpose only
  public ChannelManager getChannelManager() {
    return this.l1Listener.getChannelManager();
  }

  // for testing purpose only
  public void addClassMapping(final TCMessageType type, final Class msgClass) {
    this.l1Listener.addClassMapping(type, msgClass);
  }

  private void setLoggerOnExit() {
    CommonShutDownHook.addShutdownHook(new Runnable() {
      public void run() {
        logger.info("L2 Exiting...");
      }
    });
  }

  private void populateStatisticsRetrievalRegistry(
                                                   final DSOGlobalServerStats serverStats,
                                                   final StageManager stageManager,
                                                   final MessageMonitor messageMonitor,
                                                   final ServerTransactionManagerImpl txnManager,
                                                   final ServerTransactionSequencerStats serverTransactionSequencerStats,
                                                   final SRABerkeleyDB sraBdb) {
    if (this.statisticsAgentSubSystem.isActive()) {
      StatisticsRetrievalRegistry registry = this.statisticsAgentSubSystem.getStatisticsRetrievalRegistry();
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
      registry.registerActionInstance(new SRAVmGarbageCollector(SRAVmGarbageCollectorType.L2_VM_GARBAGE_COLLECTOR));
      registry.registerActionInstance(new SRAMessages(messageMonitor));
      registry.registerActionInstance(new SRAL2FaultsFromDisk(serverStats));
      registry.registerActionInstance(new SRAL1ToL2FlushRate(serverStats));
      registry.registerActionInstance(new SRAL2PendingTransactions(txnManager));
      registry.registerActionInstance(new SRAServerTransactionSequencer(serverTransactionSequencerStats));
      registry.registerActionInstance(new SRAL1ReferenceCount(this.clientStateManager));
      registry.registerActionInstance(new SRAGlobalLockRecallCount(serverStats));
      registry.registerActionInstance(new SRAL2GlobalLockCount(serverStats));
      if (sraBdb != null) {
        registry.registerActionInstance(sraBdb);
      }

      this.serverBuilder.populateAdditionalStatisticsRetrivalRegistry(registry);
    }
  }

  private int getCommWorkerCount(final TCProperties props) {
    int def = Math.min(Runtime.getRuntime().availableProcessors(), MAX_DEFAULT_COMM_THREADS);
    return props.getInt("tccom.workerthreads", def);
  }

  public boolean isBlocking() {
    return this.startupLock != null && this.startupLock.isBlocking();
  }

  public boolean startActiveMode() throws IOException {
    this.transactionManager.goToActiveMode();
    Set existingConnections = Collections.unmodifiableSet(this.connectionIdFactory.loadConnectionIDs());
    this.context.getClientHandshakeManager().setStarting(existingConnections);
    this.l1Listener.start(existingConnections);
    consoleLogger.info("Terracotta Server instance has started up as ACTIVE node on " + format(this.l1Listener)
                       + " successfully, and is now ready for work.");
    return true;
  }

  private static String format(final NetworkListener listener) {
    StringBuilder sb = new StringBuilder(listener.getBindAddress().getHostAddress());
    sb.append(':');
    sb.append(listener.getBindPort());
    return sb.toString();
  }

  public boolean stopActiveMode() throws TCTimeoutException {
    // TODO:: Make this not take timeout and force stop
    consoleLogger.info("Stopping ACTIVE Terracotta Server instance on " + format(this.l1Listener) + ".");
    this.l1Listener.stop(10000);
    this.l1Listener.getChannelManager().closeAllChannels();
    return true;
  }

  public void startBeanShell(final int port) {
    try {
      Interpreter i = new Interpreter();
      i.set("dsoServer", this);
      i.set("objectManager", this.objectManager);
      i.set("txnObjectManager", this.txnObjectManager);
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
    NewL2DSOConfig l2DSOConfig = this.configSetupManager.dsoL2Config();
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

  public int getGroupPort() {
    NewL2DSOConfig l2DSOConfig = this.configSetupManager.dsoL2Config();
    int configValue = l2DSOConfig.l2GroupPort().getInt();
    if (configValue != 0) { return configValue; }
    return -1;
  }

  public synchronized void stop() {
    try {
      this.statisticsAgentSubSystem.cleanup();
    } catch (Throwable e) {
      logger.warn(e);
    }

    try {
      this.statisticsGateway.cleanup();
    } catch (Throwable e) {
      logger.warn(e);
    }

    try {
      if (this.lockManager != null) {
        this.lockManager.stop();
      }
    } catch (InterruptedException e) {
      logger.error(e);
    }

    this.seda.getStageManager().stopAll();

    if (this.l1Listener != null) {
      try {
        this.l1Listener.stop(5000);
      } catch (TCTimeoutException e) {
        logger.warn("timeout trying to stop listener: " + e.getMessage());
      }
    }

    if ((this.communicationsManager != null)) {
      this.communicationsManager.shutdown();
    }

    if (this.objectManager != null) {
      try {
        this.objectManager.stop();
      } catch (Throwable e) {
        logger.error(e);
      }
    }

    this.clientStateManager.stop();

    try {
      this.objectStore.shutdown();
    } catch (Throwable e) {
      logger.warn(e);
    }

    try {
      this.persistor.close();
    } catch (DBException e) {
      logger.warn(e);
    }

    if (this.sampledCounterManager != null) {
      try {
        this.sampledCounterManager.shutdown();
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
    if (this.startupLock != null) {
      this.startupLock.release();
    }
  }

  public ConnectionIDFactory getConnectionIdFactory() {
    return this.connectionIdFactory;
  }

  public ManagedObjectStore getManagedObjectStore() {
    return this.objectStore;
  }

  public ServerConfigurationContext getContext() {
    return this.context;
  }

  public ObjectManager getObjectManager() {
    return this.objectManager;
  }

  public ClientStateManager getClientStateManager() {
    return this.clientStateManager;
  }

  public ServerManagementContext getManagementContext() {
    return this.managementContext;
  }

  public MBeanServer getMBeanServer() {
    return this.l2Management.getMBeanServer();
  }

  public JMXConnectorServer getJMXConnServer() {
    return this.l2Management.getJMXConnServer();
  }

  public StatisticsAgentSubSystem getStatisticsAgentSubSystem() {
    return this.statisticsAgentSubSystem;
  }

  public StatisticsGatewayMBeanImpl getStatisticsGateway() {
    return this.statisticsGateway;
  }

  public GCStatsEventPublisher getGcStatsEventPublisher() {
    return this.gcStatsEventPublisher;
  }

  private void startJMXServer(final InetAddress bind, int jmxPort, final Sink remoteEventsSink) throws Exception {
    if (jmxPort == 0) {
      jmxPort = new PortChooser().chooseRandomPort();
    }

    this.l2Management = new L2Management(this.tcServerInfoMBean, this.lockStatisticsMBean,
                                         this.statisticsAgentSubSystem, this.statisticsGateway,
                                         this.configSetupManager, this, bind, jmxPort, remoteEventsSink);
    this.l2Management.start();
  }

  private void stopJMXServer() throws Exception {
    this.statisticsAgentSubSystem.disableJMX();

    try {
      if (this.l2Management != null) {
        this.l2Management.stop();
      }
    } finally {
      this.l2Management = null;
    }
  }

  public ReconnectConfig getL1ReconnectProperties() {
    return this.l1ReconnectConfig;
  }

  public void addAllLocksTo(final LockInfoByThreadID lockInfo) {
    // this feature not implemented for server. DEV-1949
  }

  public ThreadIDMap getThreadIDMap() {
    return new NullThreadIDMapImpl();
  }

}
