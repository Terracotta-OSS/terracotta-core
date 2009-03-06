/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;
import bsh.EvalError;
import bsh.Interpreter;

import com.tc.async.api.SEDA;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.handler.CallbackDumpAdapter;
import com.tc.lang.TCThreadGroup;
import com.tc.license.LicenseCheck;
import com.tc.logging.ClientIDLogger;
import com.tc.logging.ClientIDLoggerProvider;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.logging.ThreadDumpHandler;
import com.tc.management.ClientLockStatManager;
import com.tc.management.L1Management;
import com.tc.management.TCClient;
import com.tc.management.beans.sessions.SessionMonitor;
import com.tc.management.lock.stats.ClientLockStatisticsManagerImpl;
import com.tc.management.lock.stats.LockStatisticsMessage;
import com.tc.management.lock.stats.LockStatisticsResponseMessage;
import com.tc.management.remote.protocol.terracotta.JmxRemoteTunnelMessage;
import com.tc.management.remote.protocol.terracotta.L1JmxReady;
import com.tc.management.remote.protocol.terracotta.TunnelingEventHandler;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OOOEventHandler;
import com.tc.net.protocol.delivery.OOONetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.HydrateHandler;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.MessageMonitorImpl;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.HealthCheckerConfigClientImpl;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.cache.CacheConfig;
import com.tc.object.cache.CacheConfigImpl;
import com.tc.object.cache.CacheManager;
import com.tc.object.cache.ClockEvictionPolicy;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.event.DmiManager;
import com.tc.object.event.DmiManagerImpl;
import com.tc.object.field.TCFieldFactory;
import com.tc.object.gtx.ClientGlobalTransactionManager;
import com.tc.object.handler.BatchTransactionAckHandler;
import com.tc.object.handler.ClientCoordinationHandler;
import com.tc.object.handler.ClusterMetaDataHandler;
import com.tc.object.handler.DmiHandler;
import com.tc.object.handler.LockResponseHandler;
import com.tc.object.handler.LockStatisticsEnableDisableHandler;
import com.tc.object.handler.LockStatisticsResponseHandler;
import com.tc.object.handler.ReceiveObjectHandler;
import com.tc.object.handler.ReceiveRootIDHandler;
import com.tc.object.handler.ReceiveTransactionCompleteHandler;
import com.tc.object.handler.ReceiveTransactionHandler;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.handshakemanager.ClientHandshakeManagerImpl;
import com.tc.object.idprovider.api.ObjectIDProvider;
import com.tc.object.idprovider.impl.RemoteObjectIDBatchSequenceProvider;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.lockmanager.api.ClientLockManager;
import com.tc.object.lockmanager.impl.ClientLockManagerConfigImpl;
import com.tc.object.lockmanager.impl.RemoteLockManagerImpl;
import com.tc.object.lockmanager.impl.ThreadLockManagerImpl;
import com.tc.object.logging.RuntimeLogger;
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
import com.tc.object.net.DSOClientMessageChannel;
import com.tc.object.session.SessionManager;
import com.tc.object.session.SessionManagerImpl;
import com.tc.object.session.SessionProvider;
import com.tc.object.tx.ClientTransactionFactory;
import com.tc.object.tx.ClientTransactionFactoryImpl;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.object.tx.ClientTransactionManagerImpl;
import com.tc.object.tx.RemoteTransactionManager;
import com.tc.object.tx.TransactionIDGenerator;
import com.tc.object.tx.TransactionBatchWriter.FoldingConfig;
import com.tc.properties.ReconnectConfig;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.runtime.TCMemoryManagerImpl;
import com.tc.runtime.logging.LongGCLogger;
import com.tc.statistics.StatisticsAgentSubSystem;
import com.tc.statistics.StatisticsAgentSubSystemImpl;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;
import com.tc.statistics.retrieval.actions.SRACacheObjectsEvictRequest;
import com.tc.statistics.retrieval.actions.SRACacheObjectsEvicted;
import com.tc.statistics.retrieval.actions.SRAHttpSessions;
import com.tc.statistics.retrieval.actions.SRAL1OutstandingBatches;
import com.tc.statistics.retrieval.actions.SRAL1PendingBatchesSize;
import com.tc.statistics.retrieval.actions.SRAL1TransactionSize;
import com.tc.statistics.retrieval.actions.SRAL1TransactionsPerBatch;
import com.tc.statistics.retrieval.actions.SRAMemoryUsage;
import com.tc.statistics.retrieval.actions.SRAMessages;
import com.tc.statistics.retrieval.actions.SRAStageQueueDepths;
import com.tc.statistics.retrieval.actions.SRASystemProperties;
import com.tc.stats.counter.Counter;
import com.tc.stats.counter.CounterConfig;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.stats.counter.sampled.derived.SampledRateCounterConfig;
import com.tc.util.Assert;
import com.tc.util.CommonShutDownHook;
import com.tc.util.ProductInfo;
import com.tc.util.TCTimeoutException;
import com.tc.util.ToggleableReferenceManager;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.LockInfoByThreadID;
import com.tc.util.runtime.ThreadIDManager;
import com.tc.util.runtime.ThreadIDManagerImpl;
import com.tc.util.runtime.ThreadIDMap;
import com.tc.util.runtime.ThreadIDMapUtil;
import com.tc.util.sequence.BatchSequence;
import com.tc.util.sequence.BatchSequenceReceiver;
import com.tc.util.sequence.Sequence;
import com.tc.util.sequence.SimpleSequence;
import com.tcclient.cluster.DsoClusterInternal;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This is the main point of entry into the DSO client.
 */
public class DistributedObjectClient extends SEDA implements TCClient {

  protected static final TCLogger                    DSO_LOGGER                 = CustomerLogging.getDSOGenericLogger();
  private static final TCLogger                      CONSOLE_LOGGER             = CustomerLogging.getConsoleLogger();

  private final DSOClientBuilder                     dsoClientBuilder;
  private final DSOClientConfigHelper                config;
  private final ClassProvider                        classProvider;
  private final Manager                              manager;
  private final DsoClusterInternal                   dsoCluster;
  private final TCThreadGroup                        threadGroup;
  private final StatisticsAgentSubSystemImpl         statisticsAgentSubSystem;
  private final RuntimeLogger                        runtimeLogger;
  private final ThreadIDMap                          threadIDMap;

  protected final PreparedComponentsFromL2Connection connectionComponents;

  private DSOClientMessageChannel                    channel;
  private ClientLockManager                          lockManager;
  private ClientObjectManagerImpl                    objectManager;
  private ClientTransactionManager                   txManager;
  private CommunicationsManager                      communicationsManager;
  private RemoteTransactionManager                   rtxManager;
  private ClientHandshakeManagerImpl                 clientHandshakeManager;
  private ClusterMetaDataManager                     clusterMetaDataManager;
  private CacheManager                               cacheManager;
  private L1Management                               l1Management;
  private TCProperties                               l1Properties;
  private DmiManager                                 dmiManager;
  private boolean                                    createDedicatedMBeanServer = false;
  private CounterManager                             counterManager;
  private ThreadIDManager                            threadIDManager;

  public DistributedObjectClient(final DSOClientConfigHelper config, final TCThreadGroup threadGroup,
                                 final ClassProvider classProvider,
                                 final PreparedComponentsFromL2Connection connectionComponents, final Manager manager,
                                 final DsoClusterInternal dsoCluster, final RuntimeLogger runtimeLogger) {
    super(threadGroup, BoundedLinkedQueue.class.getName());
    Assert.assertNotNull(config);
    this.config = config;
    this.classProvider = classProvider;
    this.connectionComponents = connectionComponents;
    this.manager = manager;
    this.dsoCluster = dsoCluster;
    this.threadGroup = threadGroup;
    this.statisticsAgentSubSystem = new StatisticsAgentSubSystemImpl();
    this.threadIDMap = ThreadIDMapUtil.getInstance();
    this.runtimeLogger = runtimeLogger;
    this.dsoClientBuilder = createClientBuilder();
  }

  protected DSOClientBuilder createClientBuilder() {
    if (this.connectionComponents.isActiveActive()) {
      throw new AssertionError("Active Server Arrays cannot be handled by CE Client");
    } else {
      return new StandardDSOClientBuilder();
    }
  }

  public ThreadIDMap getThreadIDMap() {
    return this.threadIDMap;
  }

  public void addAllLocksTo(final LockInfoByThreadID lockInfo) {
    if (this.lockManager != null) {
      this.lockManager.addAllLocksTo(lockInfo);
    } else {
      DSO_LOGGER.error("LockManager not initialised still. LockInfo for threads cannot be updated");
    }
  }

  public void setCreateDedicatedMBeanServer(final boolean createDedicatedMBeanServer) {
    this.createDedicatedMBeanServer = createDedicatedMBeanServer;
  }

  private void populateStatisticsRetrievalRegistry(final StatisticsRetrievalRegistry registry,
                                                   final StageManager stageManager,
                                                   final MessageMonitor messageMonitor,
                                                   final Counter outstandingBatchesCounter,
                                                   final Counter pendingBatchesSize,
                                                   final SampledRateCounter transactionSizeCounter,
                                                   final SampledRateCounter transactionsPerBatchCounter) {
    registry.registerActionInstance(new SRAMemoryUsage());
    registry.registerActionInstance(new SRASystemProperties());
    registry.registerActionInstance("com.tc.statistics.retrieval.actions.SRACpu");
    registry.registerActionInstance("com.tc.statistics.retrieval.actions.SRANetworkActivity");
    registry.registerActionInstance("com.tc.statistics.retrieval.actions.SRADiskActivity");
    registry.registerActionInstance("com.tc.statistics.retrieval.actions.SRAThreadDump");
    registry.registerActionInstance(new SRAStageQueueDepths(stageManager));
    registry.registerActionInstance(new SRACacheObjectsEvictRequest());
    registry.registerActionInstance(new SRACacheObjectsEvicted());
    registry.registerActionInstance("com.tc.statistics.retrieval.actions.SRAVmGarbageCollector");
    registry.registerActionInstance(new SRAMessages(messageMonitor));
    registry.registerActionInstance(new SRAL1OutstandingBatches(outstandingBatchesCounter));
    registry.registerActionInstance(new SRAL1TransactionsPerBatch(transactionsPerBatchCounter));
    registry.registerActionInstance(new SRAL1TransactionSize(transactionSizeCounter));
    registry.registerActionInstance(new SRAL1PendingBatchesSize(pendingBatchesSize));
    registry.registerActionInstance(new SRAHttpSessions());
  }

  public synchronized void start() {
    // Check config topology
    boolean toCheckTopology = TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.L1_L2_CONFIG_VALIDATION_ENABLED);
    if (toCheckTopology) {
      try {
        this.config.validateGroupInfo();
      } catch (ConfigurationSetupException e) {
        CONSOLE_LOGGER.error(e.getMessage());
        DSO_LOGGER.error("", e);
        System.exit(1);
      }
    }

    TCProperties tcProperties = TCPropertiesImpl.getProperties();
    this.l1Properties = tcProperties.getPropertiesFor("l1");
    int maxSize = tcProperties.getInt(TCPropertiesConsts.L1_SEDA_STAGE_SINK_CAPACITY);
    int faultCount = this.config.getFaultCount();

    final SessionManager sessionManager = new SessionManagerImpl(new SessionManagerImpl.SequenceFactory() {
      public Sequence newSequence() {
        return new SimpleSequence();
      }
    });
    final SessionProvider sessionProvider = (SessionProvider) sessionManager;

    this.threadGroup.addCallbackOnExitDefaultHandler(new ThreadDumpHandler(this));
    StageManager stageManager = getStageManager();

    // stageManager.turnTracingOn();

    // //////////////////////////////////
    // create NetworkStackHarnessFactory
    ReconnectConfig l1ReconnectConfig = this.config.getL1ReconnectProperties();
    final boolean useOOOLayer = l1ReconnectConfig.getReconnectEnabled();
    final NetworkStackHarnessFactory networkStackHarnessFactory;
    if (useOOOLayer) {
      final Stage oooSendStage = stageManager.createStage(ClientConfigurationContext.OOO_NET_SEND_STAGE,
                                                          new OOOEventHandler(), 1, maxSize);
      final Stage oooReceiveStage = stageManager.createStage(ClientConfigurationContext.OOO_NET_RECEIVE_STAGE,
                                                             new OOOEventHandler(), 1, maxSize);
      networkStackHarnessFactory = new OOONetworkStackHarnessFactory(
                                                                     new OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl(),
                                                                     oooSendStage.getSink(), oooReceiveStage.getSink(),
                                                                     l1ReconnectConfig);
    } else {
      networkStackHarnessFactory = new PlainNetworkStackHarnessFactory();
    }
    // //////////////////////////////////

    this.counterManager = new CounterManagerImpl();

    MessageMonitor mm = MessageMonitorImpl.createMonitor(tcProperties, DSO_LOGGER);

    this.communicationsManager = this.dsoClientBuilder
        .createCommunicationsManager(mm, networkStackHarnessFactory, new NullConnectionPolicy(),
                                     new HealthCheckerConfigClientImpl(this.l1Properties
                                         .getPropertiesFor("healthcheck.l2"), "DSO Client"));

    DSO_LOGGER.debug("Created CommunicationsManager.");

    ConfigItem[] connectionInfoItems = this.connectionComponents.createConnectionInfoConfigItemByGroup();
    ConnectionInfo[] connectionInfo = (ConnectionInfo[]) connectionInfoItems[0].getObject();
    String serverHost = connectionInfo[0].getHostname();
    int serverPort = connectionInfo[0].getPort();

    int timeout = tcProperties.getInt(TCPropertiesConsts.L1_SOCKET_CONNECT_TIMEOUT);
    if (timeout < 0) { throw new IllegalArgumentException("invalid socket time value: " + timeout); }

    this.channel = this.dsoClientBuilder.createDSOClientMessageChannel(this.communicationsManager,
                                                                       this.connectionComponents, sessionProvider);
    ClientIDLoggerProvider cidLoggerProvider = new ClientIDLoggerProvider(this.channel.getClientIDProvider());
    stageManager.setLoggerProvider(cidLoggerProvider);

    DSO_LOGGER.debug("Created channel.");

    ClientTransactionFactory txFactory = new ClientTransactionFactoryImpl(this.runtimeLogger);

    DNAEncoding encoding = new ApplicatorDNAEncodingImpl(this.classProvider);
    SampledRateCounterConfig sampledRateCounterConfig = new SampledRateCounterConfig(1, 300, true);
    SampledRateCounter transactionSizeCounter = (SampledRateCounter) this.counterManager
        .createCounter(sampledRateCounterConfig);
    SampledRateCounter transactionsPerBatchCounter = (SampledRateCounter) this.counterManager
        .createCounter(sampledRateCounterConfig);
    Counter outstandingBatchesCounter = this.counterManager.createCounter(new CounterConfig(0));
    Counter pendingBatchesSize = this.counterManager.createCounter(new CounterConfig(0));

    this.rtxManager = this.dsoClientBuilder.createRemoteTransactionManager(this.channel.getClientIDProvider(),
                                                                           encoding, FoldingConfig
                                                                               .createFromProperties(tcProperties),
                                                                           new TransactionIDGenerator(),
                                                                           sessionManager, this.channel,
                                                                           outstandingBatchesCounter,
                                                                           pendingBatchesSize, transactionSizeCounter,
                                                                           transactionsPerBatchCounter);

    ClientGlobalTransactionManager gtxManager = this.dsoClientBuilder
        .createClientGlobalTransactionManager(this.rtxManager);

    ClientLockStatManager lockStatManager = new ClientLockStatisticsManagerImpl();

    this.lockManager = this.dsoClientBuilder
        .createLockManager(new ClientIDLogger(this.channel.getClientIDProvider(), TCLogging
            .getLogger(ClientLockManager.class)),
                           new RemoteLockManagerImpl(this.channel.getLockRequestMessageFactory(), gtxManager),
                           sessionManager, lockStatManager, new ClientLockManagerConfigImpl(this.l1Properties
                               .getPropertiesFor("lockmanager")));
    this.threadGroup.addCallbackOnExitDefaultHandler(new CallbackDumpAdapter(this.lockManager));

    RemoteObjectIDBatchSequenceProvider remoteIDProvider = new RemoteObjectIDBatchSequenceProvider(this.channel
        .getObjectIDBatchRequestMessageFactory());

    // create Sequences
    BatchSequence[] sequences = this.dsoClientBuilder.createSequences(remoteIDProvider, this.l1Properties
        .getInt("objectmanager.objectid.request.size"));
    // get Sequence Receiver -- passing in sequences
    BatchSequenceReceiver batchSequenceReceiver = this.dsoClientBuilder.getBatchReceiver(sequences);
    // create object id provider
    ObjectIDProvider idProvider = this.dsoClientBuilder.createObjectIdProvider(sequences, this.channel
        .getClientIDProvider());
    remoteIDProvider.setBatchSequenceReceiver(batchSequenceReceiver);

    TCClassFactory classFactory = new TCClassFactoryImpl(new TCFieldFactory(this.config), this.config,
                                                         this.classProvider, encoding);
    TCObjectFactory objectFactory = new TCObjectFactoryImpl(classFactory);

    ToggleableReferenceManager toggleRefMgr = new ToggleableReferenceManager();

    // setup statistics subsystem
    if (this.statisticsAgentSubSystem.setup(this.config.getNewCommonL1Config())) {
      populateStatisticsRetrievalRegistry(this.statisticsAgentSubSystem.getStatisticsRetrievalRegistry(), stageManager,
                                          mm, outstandingBatchesCounter, pendingBatchesSize, transactionSizeCounter,
                                          transactionsPerBatchCounter);
    }

    RemoteObjectManager remoteObjectManager = this.dsoClientBuilder
        .createRemoteObjectManager(new ClientIDLogger(this.channel.getClientIDProvider(), TCLogging
            .getLogger(RemoteObjectManager.class)), this.channel, new NullObjectRequestMonitor(), faultCount,
                                   sessionManager);

    this.objectManager = this.dsoClientBuilder.createObjectManager(remoteObjectManager, this.config, idProvider,
                                                                   new ClockEvictionPolicy(-1), this.runtimeLogger,
                                                                   this.channel.getClientIDProvider(),
                                                                   this.classProvider, classFactory, objectFactory,
                                                                   this.config.getPortability(), this.channel,
                                                                   toggleRefMgr);
    this.threadGroup.addCallbackOnExitDefaultHandler(new CallbackDumpAdapter(this.objectManager));
    TCProperties cacheManagerProperties = this.l1Properties.getPropertiesFor("cachemanager");
    CacheConfig cacheConfig = new CacheConfigImpl(cacheManagerProperties);
    TCMemoryManagerImpl tcMemManager = new TCMemoryManagerImpl(cacheConfig.getSleepInterval(), cacheConfig
        .getLeastCount(), cacheConfig.isOnlyOldGenMonitored(), getThreadGroup());
    long timeOut = TCPropertiesImpl.getProperties().getLong(TCPropertiesConsts.LOGGING_LONG_GC_THRESHOLD);
    LongGCLogger gcLogger = new LongGCLogger(DSO_LOGGER, timeOut);
    tcMemManager.registerForMemoryEvents(gcLogger);

    if (cacheManagerProperties.getBoolean("enabled")) {
      this.cacheManager = new CacheManager(this.objectManager, cacheConfig, getThreadGroup(),
                                           this.statisticsAgentSubSystem, tcMemManager);
      this.cacheManager.start();
      if (DSO_LOGGER.isDebugEnabled()) {
        DSO_LOGGER.debug("CacheManager Enabled : " + this.cacheManager);
      }
    } else {
      DSO_LOGGER.warn("CacheManager is Disabled");
    }

    this.threadIDManager = new ThreadIDManagerImpl(this.threadIDMap);

    // Cluster meta data
    this.clusterMetaDataManager = new ClusterMetaDataManagerImpl(encoding, this.threadIDManager, this.channel
        .getNodesWithObjectsMessageFactory(), this.channel.getKeysForOrphanedValuesMessageFactory(), this.channel
        .getNodeMetaDataMessageFactory());

    // Set up the JMX management stuff
    final TunnelingEventHandler teh = this.dsoClientBuilder.createTunnelingEventHandler(this.channel.channel());
    this.l1Management = new L1Management(teh, this.statisticsAgentSubSystem, this.runtimeLogger, this.manager
        .getInstrumentationLogger(), this.config.rawConfigText(), this);
    this.l1Management.start(this.createDedicatedMBeanServer);

    // Setup the transaction manager
    this.txManager = new ClientTransactionManagerImpl(
                                                      this.channel.getClientIDProvider(),
                                                      this.objectManager,
                                                      new ThreadLockManagerImpl(this.lockManager, this.threadIDManager),
                                                      txFactory, this.rtxManager, this.runtimeLogger, this.l1Management
                                                          .findClientTxMonitorMBean());

    this.threadGroup.addCallbackOnExitDefaultHandler(new CallbackDumpAdapter(this.txManager));

    // Create the SEDA stages
    Stage lockResponse = stageManager.createStage(ClientConfigurationContext.LOCK_RESPONSE_STAGE,
                                                  new LockResponseHandler(sessionManager), 1, maxSize);
    Stage receiveRootID = stageManager.createStage(ClientConfigurationContext.RECEIVE_ROOT_ID_STAGE,
                                                   new ReceiveRootIDHandler(), 1, maxSize);
    Stage receiveObject = stageManager.createStage(ClientConfigurationContext.RECEIVE_OBJECT_STAGE,
                                                   new ReceiveObjectHandler(), 1, maxSize);
    this.dmiManager = new DmiManagerImpl(this.classProvider, this.objectManager, this.runtimeLogger);
    Stage dmiStage = stageManager.createStage(ClientConfigurationContext.DMI_STAGE, new DmiHandler(this.dmiManager), 1,
                                              maxSize);

    Stage receiveTransaction = stageManager.createStage(ClientConfigurationContext.RECEIVE_TRANSACTION_STAGE,
                                                        new ReceiveTransactionHandler(this.channel
                                                            .getClientIDProvider(), this.channel
                                                            .getAcknowledgeTransactionMessageFactory(), gtxManager,
                                                                                      sessionManager, dmiStage
                                                                                          .getSink(), this.dmiManager),
                                                        1, maxSize);
    Stage oidRequestResponse = stageManager.createStage(ClientConfigurationContext.OBJECT_ID_REQUEST_RESPONSE_STAGE,
                                                        remoteIDProvider, 1, maxSize);
    Stage transactionResponse = stageManager.createStage(ClientConfigurationContext.RECEIVE_TRANSACTION_COMPLETE_STAGE,
                                                         new ReceiveTransactionCompleteHandler(), 1, maxSize);
    Stage hydrateStage = stageManager.createStage(ClientConfigurationContext.HYDRATE_MESSAGE_STAGE,
                                                  new HydrateHandler(), 1, maxSize);
    Stage batchTxnAckStage = stageManager.createStage(ClientConfigurationContext.BATCH_TXN_ACK_STAGE,
                                                      new BatchTransactionAckHandler(), 1, maxSize);

    // By design this stage needs to be single threaded. If it wasn't then cluster membership messages could get
    // processed before the client handshake ack, and this client would get a faulty view of the cluster at best, or
    // more likely an AssertionError
    Stage pauseStage = stageManager.createStage(ClientConfigurationContext.CLIENT_COORDINATION_STAGE,
                                                new ClientCoordinationHandler(this.dsoCluster), 1, maxSize);

    Stage clusterMetaDataStage = stageManager.createStage(ClientConfigurationContext.CLUSTER_METADATA_STAGE,
                                                          new ClusterMetaDataHandler(), 1, maxSize);

    // Lock statistics
    Stage lockStatisticsStage = stageManager.createStage(ClientConfigurationContext.LOCK_STATISTICS_RESPONSE_STAGE,
                                                         new LockStatisticsResponseHandler(), 1, 1);
    final Stage lockStatisticsEnableDisableStage = stageManager
        .createStage(ClientConfigurationContext.LOCK_STATISTICS_ENABLE_DISABLE_STAGE,
                     new LockStatisticsEnableDisableHandler(), 1, 1);
    lockStatManager.start(this.channel, lockStatisticsStage.getSink());

    final Stage jmxRemoteTunnelStage = stageManager.createStage(ClientConfigurationContext.JMXREMOTE_TUNNEL_STAGE, teh,
                                                                1, maxSize);

    List clientHandshakeCallbacks = new ArrayList();
    clientHandshakeCallbacks.add(this.lockManager);
    clientHandshakeCallbacks.add(this.objectManager);
    clientHandshakeCallbacks.add(remoteObjectManager);
    clientHandshakeCallbacks.add(this.rtxManager);
    clientHandshakeCallbacks.add(this.dsoClientBuilder.getObjectIDClientHandshakeRequester(batchSequenceReceiver));
    ProductInfo pInfo = ProductInfo.getInstance();
    this.clientHandshakeManager = new ClientHandshakeManagerImpl(
                                                                 new ClientIDLogger(
                                                                                    this.channel.getClientIDProvider(),
                                                                                    TCLogging
                                                                                        .getLogger(ClientHandshakeManagerImpl.class)),
                                                                 this.channel, this.channel
                                                                     .getClientHandshakeMessageFactory(), pauseStage
                                                                     .getSink(), sessionManager, this.dsoCluster, pInfo
                                                                     .version(), Collections
                                                                     .unmodifiableCollection(clientHandshakeCallbacks));
    this.channel.addListener(this.clientHandshakeManager);

    ClientConfigurationContext cc = new ClientConfigurationContext(stageManager, this.lockManager, remoteObjectManager,
                                                                   this.txManager, this.clientHandshakeManager,
                                                                   this.clusterMetaDataManager);
    stageManager.startAll(cc, Collections.EMPTY_LIST);

    this.channel.addClassMapping(TCMessageType.BATCH_TRANSACTION_ACK_MESSAGE,
                                 BatchTransactionAcknowledgeMessageImpl.class);
    this.channel.addClassMapping(TCMessageType.REQUEST_ROOT_MESSAGE, RequestRootMessageImpl.class);
    this.channel.addClassMapping(TCMessageType.LOCK_REQUEST_MESSAGE, LockRequestMessage.class);
    this.channel.addClassMapping(TCMessageType.LOCK_RESPONSE_MESSAGE, LockResponseMessage.class);
    this.channel.addClassMapping(TCMessageType.LOCK_RECALL_MESSAGE, LockResponseMessage.class);
    this.channel.addClassMapping(TCMessageType.LOCK_QUERY_RESPONSE_MESSAGE, LockResponseMessage.class);
    this.channel.addClassMapping(TCMessageType.LOCK_STAT_MESSAGE, LockStatisticsMessage.class);
    this.channel.addClassMapping(TCMessageType.LOCK_STATISTICS_RESPONSE_MESSAGE, LockStatisticsResponseMessage.class);
    this.channel.addClassMapping(TCMessageType.COMMIT_TRANSACTION_MESSAGE, CommitTransactionMessageImpl.class);
    this.channel.addClassMapping(TCMessageType.REQUEST_ROOT_RESPONSE_MESSAGE, RequestRootResponseMessage.class);
    this.channel.addClassMapping(TCMessageType.REQUEST_MANAGED_OBJECT_MESSAGE, RequestManagedObjectMessageImpl.class);
    this.channel.addClassMapping(TCMessageType.REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE,
                                 RequestManagedObjectResponseMessageImpl.class);
    this.channel.addClassMapping(TCMessageType.OBJECTS_NOT_FOUND_RESPONSE_MESSAGE, ObjectsNotFoundMessageImpl.class);
    this.channel.addClassMapping(TCMessageType.BROADCAST_TRANSACTION_MESSAGE, BroadcastTransactionMessageImpl.class);
    this.channel.addClassMapping(TCMessageType.OBJECT_ID_BATCH_REQUEST_MESSAGE, ObjectIDBatchRequestMessage.class);
    this.channel.addClassMapping(TCMessageType.OBJECT_ID_BATCH_REQUEST_RESPONSE_MESSAGE,
                                 ObjectIDBatchRequestResponseMessage.class);
    this.channel
        .addClassMapping(TCMessageType.ACKNOWLEDGE_TRANSACTION_MESSAGE, AcknowledgeTransactionMessageImpl.class);
    this.channel.addClassMapping(TCMessageType.CLIENT_HANDSHAKE_MESSAGE, ClientHandshakeMessageImpl.class);
    this.channel.addClassMapping(TCMessageType.CLIENT_HANDSHAKE_ACK_MESSAGE, ClientHandshakeAckMessageImpl.class);
    this.channel.addClassMapping(TCMessageType.JMX_MESSAGE, JMXMessage.class);
    this.channel.addClassMapping(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE, JmxRemoteTunnelMessage.class);
    this.channel.addClassMapping(TCMessageType.CLUSTER_MEMBERSHIP_EVENT_MESSAGE, ClusterMembershipMessage.class);
    this.channel.addClassMapping(TCMessageType.CLIENT_JMX_READY_MESSAGE, L1JmxReady.class);
    this.channel.addClassMapping(TCMessageType.COMPLETED_TRANSACTION_LOWWATERMARK_MESSAGE,
                                 CompletedTransactionLowWaterMarkMessage.class);
    this.channel.addClassMapping(TCMessageType.NODES_WITH_OBJECTS_MESSAGE, NodesWithObjectsMessageImpl.class);
    this.channel.addClassMapping(TCMessageType.NODES_WITH_OBJECTS_RESPONSE_MESSAGE,
                                 NodesWithObjectsResponseMessageImpl.class);
    this.channel
        .addClassMapping(TCMessageType.KEYS_FOR_ORPHANED_VALUES_MESSAGE, KeysForOrphanedValuesMessageImpl.class);
    this.channel.addClassMapping(TCMessageType.KEYS_FOR_ORPHANED_VALUES_RESPONSE_MESSAGE,
                                 KeysForOrphanedValuesResponseMessageImpl.class);
    this.channel.addClassMapping(TCMessageType.NODE_META_DATA_MESSAGE, NodeMetaDataMessageImpl.class);
    this.channel.addClassMapping(TCMessageType.NODE_META_DATA_RESPONSE_MESSAGE, NodeMetaDataResponseMessageImpl.class);

    DSO_LOGGER.debug("Added class mappings.");

    Sink hydrateSink = hydrateStage.getSink();
    this.channel.routeMessageType(TCMessageType.LOCK_RESPONSE_MESSAGE, lockResponse.getSink(), hydrateSink);
    this.channel.routeMessageType(TCMessageType.LOCK_QUERY_RESPONSE_MESSAGE, lockResponse.getSink(), hydrateSink);
    this.channel.routeMessageType(TCMessageType.LOCK_STAT_MESSAGE, lockStatisticsEnableDisableStage.getSink(),
                                  hydrateSink);
    this.channel.routeMessageType(TCMessageType.LOCK_RECALL_MESSAGE, lockResponse.getSink(), hydrateSink);
    this.channel.routeMessageType(TCMessageType.REQUEST_ROOT_RESPONSE_MESSAGE, receiveRootID.getSink(), hydrateSink);
    this.channel.routeMessageType(TCMessageType.REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE, receiveObject.getSink(),
                                  hydrateSink);
    this.channel.routeMessageType(TCMessageType.OBJECTS_NOT_FOUND_RESPONSE_MESSAGE, receiveObject.getSink(),
                                  hydrateSink);
    this.channel.routeMessageType(TCMessageType.BROADCAST_TRANSACTION_MESSAGE, receiveTransaction.getSink(),
                                  hydrateSink);
    this.channel.routeMessageType(TCMessageType.OBJECT_ID_BATCH_REQUEST_RESPONSE_MESSAGE, oidRequestResponse.getSink(),
                                  hydrateSink);
    this.channel.routeMessageType(TCMessageType.ACKNOWLEDGE_TRANSACTION_MESSAGE, transactionResponse.getSink(),
                                  hydrateSink);
    this.channel.routeMessageType(TCMessageType.BATCH_TRANSACTION_ACK_MESSAGE, batchTxnAckStage.getSink(), hydrateSink);
    this.channel.routeMessageType(TCMessageType.CLIENT_HANDSHAKE_ACK_MESSAGE, pauseStage.getSink(), hydrateSink);
    this.channel.routeMessageType(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE, jmxRemoteTunnelStage.getSink(),
                                  hydrateSink);
    this.channel.routeMessageType(TCMessageType.CLUSTER_MEMBERSHIP_EVENT_MESSAGE, pauseStage.getSink(), hydrateSink);
    this.channel.routeMessageType(TCMessageType.NODES_WITH_OBJECTS_RESPONSE_MESSAGE, clusterMetaDataStage.getSink(),
                                  hydrateSink);
    this.channel.routeMessageType(TCMessageType.KEYS_FOR_ORPHANED_VALUES_RESPONSE_MESSAGE, clusterMetaDataStage
        .getSink(), hydrateSink);
    this.channel.routeMessageType(TCMessageType.NODE_META_DATA_RESPONSE_MESSAGE, clusterMetaDataStage.getSink(),
                                  hydrateSink);

    final int maxConnectRetries = this.l1Properties.getInt("max.connect.retries");
    int i = 0;
    while (maxConnectRetries <= 0 || i < maxConnectRetries) {
      try {
        DSO_LOGGER.debug("Trying to open channel....");
        this.channel.open();
        DSO_LOGGER.debug("Channel open");
        break;
      } catch (TCTimeoutException tcte) {
        CONSOLE_LOGGER.warn("Timeout connecting to server: " + tcte.getMessage());
        ThreadUtil.reallySleep(5000);
      } catch (ConnectException e) {
        CONSOLE_LOGGER.warn("Connection refused from server: " + e);
        ThreadUtil.reallySleep(5000);
      } catch (MaxConnectionsExceededException e) {
        CONSOLE_LOGGER.fatal(e.getMessage());
        CONSOLE_LOGGER.fatal(LicenseCheck.EXIT_MESSAGE);
        System.exit(1);
      } catch (IOException ioe) {
        CONSOLE_LOGGER.warn("IOException connecting to server: " + serverHost + ":" + serverPort + ". "
                            + ioe.getMessage());
        ThreadUtil.reallySleep(5000);
      }
      i++;
    }
    if (i == maxConnectRetries) {
      CONSOLE_LOGGER.error("MaxConnectRetries '" + maxConnectRetries + "' attempted. Exiting.");
      System.exit(-1);
    }
    this.clientHandshakeManager.waitForHandshake();

    final TCSocketAddress remoteAddress = this.channel.channel().getRemoteAddress();
    final String infoMsg = "Connection successfully established to server at " + remoteAddress;
    CONSOLE_LOGGER.info(infoMsg);
    DSO_LOGGER.info(infoMsg);

    if (this.statisticsAgentSubSystem.isActive()) {
      this.statisticsAgentSubSystem.setDefaultAgentDifferentiator("L1/"
                                                                  + this.channel.channel().getChannelID().toLong());
    }

    if (useOOOLayer) {
      setReconnectCloseOnExit(this.channel);
    }
    setLoggerOnExit();
  }

  private void setReconnectCloseOnExit(final DSOClientMessageChannel channel) {
    CommonShutDownHook.addShutdownHook(new Runnable() {
      public void run() {
        channel.close();
      }
    });
  }

  private void setLoggerOnExit() {
    CommonShutDownHook.addShutdownHook(new Runnable() {
      public void run() {
        DSO_LOGGER.info("L1 Exiting...");
      }
    });
  }

  /**
   * Note that this method shuts down the manager that is associated with this client, this is only used in tests. To
   * properly shut down resources of this client for production, the code should be added to
   * {@link ClientShutdownManager} and not to this method.
   */
  public synchronized void stopForTests() {
    this.manager.stop();
  }

  public ClientTransactionManager getTransactionManager() {
    return this.txManager;
  }

  public ClientObjectManager getObjectManager() {
    return this.objectManager;
  }

  public RemoteTransactionManager getRemoteTransactionManager() {
    return this.rtxManager;
  }

  public CommunicationsManager getCommunicationsManager() {
    return this.communicationsManager;
  }

  public DSOClientMessageChannel getChannel() {
    return this.channel;
  }

  public ClientHandshakeManager getClientHandshakeManager() {
    return this.clientHandshakeManager;
  }

  public ClusterMetaDataManager getClusterMetaDataManager() {
    return this.clusterMetaDataManager;
  }

  public SessionMonitor getHttpSessionMonitor() {
    return this.l1Management.getHttpSessionMonitor();
  }

  public DmiManager getDmiManager() {
    return this.dmiManager;
  }

  public StatisticsAgentSubSystem getStatisticsAgentSubSystem() {
    return this.statisticsAgentSubSystem;
  }

  public void dump() {
    if (this.lockManager != null) {
      this.lockManager.dumpToLogger();
    }

    if (this.txManager != null) {
      this.txManager.dumpToLogger();
    }

    if (this.objectManager != null) {
      this.objectManager.dumpToLogger();
    }
  }

  public void startBeanShell(final int port) {
    try {
      Interpreter i = new Interpreter();
      i.set("client", this);
      i.set("objectManager", this.objectManager);
      i.set("lockmanager", this.lockManager);
      i.set("txManager", this.txManager);
      i.set("portnum", port);
      i.eval("setAccessibility(true)"); // turn off access restrictions
      i.eval("server(portnum)");
      CONSOLE_LOGGER.info("Bean shell is started on port " + port);
    } catch (EvalError e) {
      e.printStackTrace();
    }
  }
}
