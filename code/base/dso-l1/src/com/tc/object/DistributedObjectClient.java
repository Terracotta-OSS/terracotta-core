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
import com.tc.cluster.Cluster;
import com.tc.cluster.DsoClusterInternal;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.handler.CallbackDumpAdapter;
import com.tc.lang.TCThreadGroup;
import com.tc.license.AbstractLicenseResolverFactory;
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
import com.tc.net.GroupID;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OOOEventHandler;
import com.tc.net.protocol.delivery.OOONetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.HydrateHandler;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.MessageMonitorImpl;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.HealthCheckerConfig;
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
import com.tc.object.gtx.ClientGlobalTransactionManagerImpl;
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
import com.tc.object.idprovider.impl.ObjectIDClientHandshakeRequester;
import com.tc.object.idprovider.impl.ObjectIDProviderImpl;
import com.tc.object.idprovider.impl.RemoteObjectIDBatchSequenceProvider;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.lockmanager.api.ClientLockManager;
import com.tc.object.lockmanager.impl.ClientLockManagerConfigImpl;
import com.tc.object.lockmanager.impl.RemoteLockManagerImpl;
import com.tc.object.lockmanager.impl.StripedClientLockManagerImpl;
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
import com.tc.object.msg.LockRequestMessage;
import com.tc.object.msg.LockResponseMessage;
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
import com.tc.object.tx.RemoteTransactionManagerImpl;
import com.tc.object.tx.TransactionBatchFactory;
import com.tc.object.tx.TransactionBatchWriterFactory;
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
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterConfig;
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
import com.tc.util.sequence.Sequence;
import com.tc.util.sequence.SimpleSequence;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This is the main point of entry into the DSO client.
 */
public class DistributedObjectClient extends SEDA implements TCClient {

  private static final TCLogger                    DSO_LOGGER                 = CustomerLogging.getDSOGenericLogger();
  private static final TCLogger                    CONSOLE_LOGGER             = CustomerLogging.getConsoleLogger();

  private final DSOClientConfigHelper              config;
  private final ClassProvider                      classProvider;
  private final PreparedComponentsFromL2Connection connectionComponents;
  private final Manager                            manager;
  private final Cluster                            cluster;
  private final DsoClusterInternal                 dsoCluster;
  private final TCThreadGroup                      threadGroup;
  private final StatisticsAgentSubSystemImpl       statisticsAgentSubSystem;

  private final RuntimeLogger                      runtimeLogger;
  private final ThreadIDMap                        threadIDMap;

  private DSOClientMessageChannel                  channel;
  private ClientLockManager                        lockManager;
  private ClientObjectManagerImpl                  objectManager;
  private ClientTransactionManager                 txManager;
  private CommunicationsManager                    communicationsManager;
  private RemoteTransactionManager                 rtxManager;
  private ClientHandshakeManagerImpl               clientHandshakeManager;
  private ClusterMetaDataManager                   clusterMetaDataManager;
  private CacheManager                             cacheManager;
  private L1Management                             l1Management;
  private TCProperties                             l1Properties;
  private DmiManager                               dmiManager;
  private boolean                                  createDedicatedMBeanServer = false;
  private CounterManager                           counterManager;
  private ThreadIDManager                          threadIDManager;

  public DistributedObjectClient(final DSOClientConfigHelper config, final TCThreadGroup threadGroup,
                                 final ClassProvider classProvider,
                                 final PreparedComponentsFromL2Connection connectionComponents, final Manager manager,
                                 final Cluster cluster, final DsoClusterInternal dsoCluster,
                                 final RuntimeLogger runtimeLogger) {
    super(threadGroup, BoundedLinkedQueue.class.getName());
    Assert.assertNotNull(config);
    this.config = config;
    this.classProvider = classProvider;
    this.connectionComponents = connectionComponents;
    this.manager = manager;
    this.cluster = cluster;
    this.dsoCluster = dsoCluster;
    this.threadGroup = threadGroup;
    this.statisticsAgentSubSystem = new StatisticsAgentSubSystemImpl();
    this.threadIDMap = ThreadIDMapUtil.getInstance();
    this.runtimeLogger = runtimeLogger;
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

  /*
   * Overwrite this routine to do active-active channel
   */
  protected DSOClientMessageChannel createDSOClientMessageChannel(final CommunicationsManager commMgr,
                                                                  final PreparedComponentsFromL2Connection connComp,
                                                                  final SessionProvider sessionProvider) {
    ClientMessageChannel cmc;
    ConfigItem connectionInfoItem = connComp.createConnectionInfoConfigItem();
    ConnectionInfo[] connectionInfo = (ConnectionInfo[]) connectionInfoItem.getObject();
    ConnectionAddressProvider cap = new ConnectionAddressProvider(connectionInfo);
    cmc = commMgr.createClientChannel(sessionProvider, -1, null, 0, 10000, cap);
    return new DSOClientMessageChannelImpl(cmc, new GroupID[] { new GroupID(cap.getGroupId()) });
  }

  /*
   * Overwrite this routine to do active-active channel
   */
  protected CommunicationsManager createCommunicationsManager(final MessageMonitor monitor,
                                                              final NetworkStackHarnessFactory stackHarnessFactory,
                                                              final ConnectionPolicy connectionPolicy,
                                                              final HealthCheckerConfig aConfig) {
    return new CommunicationsManagerImpl(monitor, stackHarnessFactory, connectionPolicy, aConfig);
  }

  private void populateStatisticsRetrievalRegistry(final StatisticsRetrievalRegistry registry,
                                                   final StageManager stageManager,
                                                   final MessageMonitor messageMonitor,
                                                   final Counter outstandingBatchesCounter,
                                                   final SampledCounter numTransactionCounter,
                                                   final SampledCounter numBatchesCounter,
                                                   final SampledCounter batchSizeCounter,
                                                   final Counter pendingBatchesSize) {
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
    registry.registerActionInstance(new SRAL1TransactionsPerBatch(numTransactionCounter, numBatchesCounter));
    registry.registerActionInstance(new SRAL1TransactionSize(batchSizeCounter, numTransactionCounter));
    registry.registerActionInstance(new SRAL1PendingBatchesSize(pendingBatchesSize));
    registry.registerActionInstance(new SRAHttpSessions());
  }

  protected TunnelingEventHandler createTunnelingEventHandler(final ClientMessageChannel ch) {
    return new TunnelingEventHandler(ch);
  }

  public synchronized void start() {
    TCProperties tcProperties = TCPropertiesImpl.getProperties();
    l1Properties = tcProperties.getPropertiesFor("l1");
    int maxSize = tcProperties.getInt(TCPropertiesConsts.L1_SEDA_STAGE_SINK_CAPACITY);
    int faultCount = config.getFaultCount();

    final SessionManager sessionManager = new SessionManagerImpl(new SessionManagerImpl.SequenceFactory() {
      public Sequence newSequence() {
        return new SimpleSequence();
      }
    });
    final SessionProvider sessionProvider = (SessionProvider) sessionManager;

    threadGroup.addCallbackOnExitDefaultHandler(new ThreadDumpHandler(this));
    StageManager stageManager = getStageManager();

    // stageManager.turnTracingOn();

    // //////////////////////////////////
    // create NetworkStackHarnessFactory
    ReconnectConfig l1ReconnectConfig = config.getL1ReconnectProperties();
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

    counterManager = new CounterManagerImpl();

    MessageMonitor mm = MessageMonitorImpl.createMonitor(tcProperties, DSO_LOGGER);

    communicationsManager = createCommunicationsManager(mm, networkStackHarnessFactory, new NullConnectionPolicy(),
                                                        new HealthCheckerConfigClientImpl(l1Properties
                                                            .getPropertiesFor("healthcheck.l2"), "DSO Client"));

    DSO_LOGGER.debug("Created CommunicationsManager.");

    ConfigItem[] connectionInfoItems = this.connectionComponents.createConnectionInfoConfigItemByGroup();
    ConnectionInfo[] connectionInfo = (ConnectionInfo[]) connectionInfoItems[0].getObject();
    String serverHost = connectionInfo[0].getHostname();
    int serverPort = connectionInfo[0].getPort();

    int timeout = tcProperties.getInt(TCPropertiesConsts.L1_SOCKET_CONNECT_TIMEOUT);
    if (timeout < 0) { throw new IllegalArgumentException("invalid socket time value: " + timeout); }

    channel = createDSOClientMessageChannel(communicationsManager, connectionComponents, sessionProvider);
    ClientIDLoggerProvider cidLoggerProvider = new ClientIDLoggerProvider(channel.getClientIDProvider());
    stageManager.setLoggerProvider(cidLoggerProvider);

    DSO_LOGGER.debug("Created channel.");

    ClientTransactionFactory txFactory = new ClientTransactionFactoryImpl(runtimeLogger);

    DNAEncoding encoding = new ApplicatorDNAEncodingImpl(classProvider);

    SampledCounter numTransactionCounter = (SampledCounter) counterManager.createCounter(new SampledCounterConfig(1,
                                                                                                                  900,
                                                                                                                  true,
                                                                                                                  0L));
    SampledCounter numBatchesCounter = (SampledCounter) counterManager
        .createCounter(new SampledCounterConfig(1, 900, true, 0L));
    SampledCounter batchSizeCounter = (SampledCounter) counterManager.createCounter(new SampledCounterConfig(1, 900,
                                                                                                             true, 0L));
    Counter outstandingBatchesCounter = counterManager.createCounter(new CounterConfig(0));
    Counter pendingBatchesSize = counterManager.createCounter(new CounterConfig(0));

    rtxManager = createRemoteTransactionManager(channel.getClientIDProvider(), encoding, FoldingConfig
        .createFromProperties(tcProperties), new TransactionIDGenerator(), sessionManager, channel,
                                                outstandingBatchesCounter, numTransactionCounter, numBatchesCounter,
                                                batchSizeCounter, pendingBatchesSize);

    ClientGlobalTransactionManager gtxManager = createClientGlobalTransactionManager(rtxManager);

    ClientLockStatManager lockStatManager = new ClientLockStatisticsManagerImpl();

    lockManager = createLockManager(new ClientIDLogger(channel.getClientIDProvider(), TCLogging
        .getLogger(ClientLockManager.class)), new RemoteLockManagerImpl(channel.getLockRequestMessageFactory(),
                                                                        gtxManager), sessionManager, lockStatManager,
                                    new ClientLockManagerConfigImpl(l1Properties.getPropertiesFor("lockmanager")));
    threadGroup.addCallbackOnExitDefaultHandler(new CallbackDumpAdapter(lockManager));

    RemoteObjectIDBatchSequenceProvider remoteIDProvider = new RemoteObjectIDBatchSequenceProvider(channel
        .getObjectIDBatchRequestMessageFactory());
    BatchSequence sequence = new BatchSequence(remoteIDProvider, l1Properties
        .getInt("objectmanager.objectid.request.size"));
    ObjectIDProvider idProvider = new ObjectIDProviderImpl(sequence);
    remoteIDProvider.setBatchSequenceReceiver(sequence);

    TCClassFactory classFactory = new TCClassFactoryImpl(new TCFieldFactory(config), config, classProvider, encoding);
    TCObjectFactory objectFactory = new TCObjectFactoryImpl(classFactory);

    ToggleableReferenceManager toggleRefMgr = new ToggleableReferenceManager();

    // setup statistics subsystem
    if (statisticsAgentSubSystem.setup(config.getNewCommonL1Config())) {
      populateStatisticsRetrievalRegistry(statisticsAgentSubSystem.getStatisticsRetrievalRegistry(), stageManager, mm,
                                          outstandingBatchesCounter, numTransactionCounter, numBatchesCounter,
                                          batchSizeCounter, pendingBatchesSize);
    }

    RemoteObjectManager remoteObjectManager = createRemoteObjectManager(new ClientIDLogger(channel
        .getClientIDProvider(), TCLogging.getLogger(RemoteObjectManager.class)), channel,
                                                                        new NullObjectRequestMonitor(), faultCount,
                                                                        sessionManager);

    objectManager = createObjectManager(remoteObjectManager, config, idProvider, new ClockEvictionPolicy(-1),
                                        runtimeLogger, channel.getClientIDProvider(), classProvider, classFactory,
                                        objectFactory, config.getPortability(), channel, toggleRefMgr);
    threadGroup.addCallbackOnExitDefaultHandler(new CallbackDumpAdapter(objectManager));
    TCProperties cacheManagerProperties = l1Properties.getPropertiesFor("cachemanager");
    CacheConfig cacheConfig = new CacheConfigImpl(cacheManagerProperties);
    TCMemoryManagerImpl tcMemManager = new TCMemoryManagerImpl(cacheConfig.getSleepInterval(), cacheConfig
        .getLeastCount(), cacheConfig.isOnlyOldGenMonitored(), getThreadGroup());
    long timeOut = TCPropertiesImpl.getProperties().getLong(TCPropertiesConsts.LOGGING_LONG_GC_THRESHOLD);
    LongGCLogger gcLogger = new LongGCLogger(DSO_LOGGER, timeOut);
    tcMemManager.registerForMemoryEvents(gcLogger);

    if (cacheManagerProperties.getBoolean("enabled")) {
      this.cacheManager = new CacheManager(objectManager, cacheConfig, getThreadGroup(), statisticsAgentSubSystem,
                                           tcMemManager);
      this.cacheManager.start();
      if (DSO_LOGGER.isDebugEnabled()) {
        DSO_LOGGER.debug("CacheManager Enabled : " + cacheManager);
      }
    } else {
      DSO_LOGGER.warn("CacheManager is Disabled");
    }

    threadIDManager = new ThreadIDManagerImpl(threadIDMap);

    // Cluster meta data
    clusterMetaDataManager = new ClusterMetaDataManagerImpl(threadIDManager, channel.getClusterMetaDataMessageFactory());

    // Set up the JMX management stuff
    final TunnelingEventHandler teh = createTunnelingEventHandler(channel.channel());
    l1Management = new L1Management(teh, statisticsAgentSubSystem, runtimeLogger, manager.getInstrumentationLogger(),
                                    config.rawConfigText(), this);
    l1Management.start(createDedicatedMBeanServer);

    // Setup the transaction manager
    txManager = new ClientTransactionManagerImpl(channel.getClientIDProvider(), objectManager,
                                                 new ThreadLockManagerImpl(lockManager, threadIDManager), txFactory,
                                                 rtxManager, runtimeLogger, l1Management.findClientTxMonitorMBean());

    threadGroup.addCallbackOnExitDefaultHandler(new CallbackDumpAdapter(txManager));

    // Create the SEDA stages
    Stage lockResponse = stageManager.createStage(ClientConfigurationContext.LOCK_RESPONSE_STAGE,
                                                  new LockResponseHandler(sessionManager), 1, maxSize);
    Stage receiveRootID = stageManager.createStage(ClientConfigurationContext.RECEIVE_ROOT_ID_STAGE,
                                                   new ReceiveRootIDHandler(), 1, maxSize);
    Stage receiveObject = stageManager.createStage(ClientConfigurationContext.RECEIVE_OBJECT_STAGE,
                                                   new ReceiveObjectHandler(), 1, maxSize);
    this.dmiManager = new DmiManagerImpl(classProvider, objectManager, runtimeLogger);
    Stage dmiStage = stageManager.createStage(ClientConfigurationContext.DMI_STAGE, new DmiHandler(dmiManager), 1,
                                              maxSize);

    Stage receiveTransaction = stageManager
        .createStage(ClientConfigurationContext.RECEIVE_TRANSACTION_STAGE,
                     new ReceiveTransactionHandler(channel.getClientIDProvider(), channel
                         .getAcknowledgeTransactionMessageFactory(), gtxManager, sessionManager, dmiStage.getSink(),
                                                   dmiManager), 1, maxSize);
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
                                                new ClientCoordinationHandler(cluster, dsoCluster), 1, maxSize);

    // Stage clusterEventsStage = stageManager.createStage(ClientConfigurationContext.CLUSTER_EVENTS_STAGE,
    // new ClusterEventsHandler(dsoCluster), 1, maxSize);

    Stage clusterMetaDataStage = stageManager.createStage(ClientConfigurationContext.CLUSTER_METADATA_STAGE,
                                                          new ClusterMetaDataHandler(), 1, maxSize);

    // Lock statistics
    Stage lockStatisticsStage = stageManager.createStage(ClientConfigurationContext.LOCK_STATISTICS_RESPONSE_STAGE,
                                                         new LockStatisticsResponseHandler(), 1, 1);
    final Stage lockStatisticsEnableDisableStage = stageManager
        .createStage(ClientConfigurationContext.LOCK_STATISTICS_ENABLE_DISABLE_STAGE,
                     new LockStatisticsEnableDisableHandler(), 1, 1);
    lockStatManager.start(channel, lockStatisticsStage.getSink());

    final Stage jmxRemoteTunnelStage = stageManager.createStage(ClientConfigurationContext.JMXREMOTE_TUNNEL_STAGE, teh,
                                                                1, maxSize);

    List clientHandshakeCallbacks = new ArrayList();
    clientHandshakeCallbacks.add(lockManager);
    clientHandshakeCallbacks.add(objectManager);
    clientHandshakeCallbacks.add(remoteObjectManager);
    clientHandshakeCallbacks.add(rtxManager);
    clientHandshakeCallbacks.add(getObjectIDClientHandshakeRequester(sequence));
    ProductInfo pInfo = ProductInfo.getInstance();
    clientHandshakeManager = new ClientHandshakeManagerImpl(new ClientIDLogger(channel.getClientIDProvider(), TCLogging
        .getLogger(ClientHandshakeManagerImpl.class)), channel, channel.getClientHandshakeMessageFactory(), pauseStage
        .getSink(), sessionManager, cluster, dsoCluster, pInfo.version(), Collections
        .unmodifiableCollection(clientHandshakeCallbacks));
    channel.addListener(clientHandshakeManager);

    ClientConfigurationContext cc = new ClientConfigurationContext(stageManager, lockManager, remoteObjectManager,
                                                                   txManager, clientHandshakeManager,
                                                                   clusterMetaDataManager);
    stageManager.startAll(cc, Collections.EMPTY_LIST);

    channel.addClassMapping(TCMessageType.BATCH_TRANSACTION_ACK_MESSAGE, BatchTransactionAcknowledgeMessageImpl.class);
    channel.addClassMapping(TCMessageType.REQUEST_ROOT_MESSAGE, RequestRootMessageImpl.class);
    channel.addClassMapping(TCMessageType.LOCK_REQUEST_MESSAGE, LockRequestMessage.class);
    channel.addClassMapping(TCMessageType.LOCK_RESPONSE_MESSAGE, LockResponseMessage.class);
    channel.addClassMapping(TCMessageType.LOCK_RECALL_MESSAGE, LockResponseMessage.class);
    channel.addClassMapping(TCMessageType.LOCK_QUERY_RESPONSE_MESSAGE, LockResponseMessage.class);
    channel.addClassMapping(TCMessageType.LOCK_STAT_MESSAGE, LockStatisticsMessage.class);
    channel.addClassMapping(TCMessageType.LOCK_STATISTICS_RESPONSE_MESSAGE, LockStatisticsResponseMessage.class);
    channel.addClassMapping(TCMessageType.COMMIT_TRANSACTION_MESSAGE, CommitTransactionMessageImpl.class);
    channel.addClassMapping(TCMessageType.REQUEST_ROOT_RESPONSE_MESSAGE, RequestRootResponseMessage.class);
    channel.addClassMapping(TCMessageType.REQUEST_MANAGED_OBJECT_MESSAGE, RequestManagedObjectMessageImpl.class);
    channel.addClassMapping(TCMessageType.REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE,
                            RequestManagedObjectResponseMessageImpl.class);
    channel.addClassMapping(TCMessageType.OBJECTS_NOT_FOUND_RESPONSE_MESSAGE, ObjectsNotFoundMessageImpl.class);
    channel.addClassMapping(TCMessageType.BROADCAST_TRANSACTION_MESSAGE, BroadcastTransactionMessageImpl.class);
    channel.addClassMapping(TCMessageType.OBJECT_ID_BATCH_REQUEST_MESSAGE, ObjectIDBatchRequestMessage.class);
    channel.addClassMapping(TCMessageType.OBJECT_ID_BATCH_REQUEST_RESPONSE_MESSAGE,
                            ObjectIDBatchRequestResponseMessage.class);
    channel.addClassMapping(TCMessageType.ACKNOWLEDGE_TRANSACTION_MESSAGE, AcknowledgeTransactionMessageImpl.class);
    channel.addClassMapping(TCMessageType.CLIENT_HANDSHAKE_MESSAGE, ClientHandshakeMessageImpl.class);
    channel.addClassMapping(TCMessageType.CLIENT_HANDSHAKE_ACK_MESSAGE, ClientHandshakeAckMessageImpl.class);
    channel.addClassMapping(TCMessageType.JMX_MESSAGE, JMXMessage.class);
    channel.addClassMapping(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE, JmxRemoteTunnelMessage.class);
    channel.addClassMapping(TCMessageType.CLUSTER_MEMBERSHIP_EVENT_MESSAGE, ClusterMembershipMessage.class);
    channel.addClassMapping(TCMessageType.CLIENT_JMX_READY_MESSAGE, L1JmxReady.class);
    channel.addClassMapping(TCMessageType.COMPLETED_TRANSACTION_LOWWATERMARK_MESSAGE,
                            CompletedTransactionLowWaterMarkMessage.class);
    channel.addClassMapping(TCMessageType.NODES_WITH_OBJECTS_MESSAGE, NodesWithObjectsMessageImpl.class);
    channel.addClassMapping(TCMessageType.NODES_WITH_OBJECTS_RESPONSE_MESSAGE,
                            NodesWithObjectsResponseMessageImpl.class);

    DSO_LOGGER.debug("Added class mappings.");

    Sink hydrateSink = hydrateStage.getSink();
    channel.routeMessageType(TCMessageType.LOCK_RESPONSE_MESSAGE, lockResponse.getSink(), hydrateSink);
    channel.routeMessageType(TCMessageType.LOCK_QUERY_RESPONSE_MESSAGE, lockResponse.getSink(), hydrateSink);
    channel.routeMessageType(TCMessageType.LOCK_STAT_MESSAGE, lockStatisticsEnableDisableStage.getSink(), hydrateSink);
    channel.routeMessageType(TCMessageType.LOCK_RECALL_MESSAGE, lockResponse.getSink(), hydrateSink);
    channel.routeMessageType(TCMessageType.REQUEST_ROOT_RESPONSE_MESSAGE, receiveRootID.getSink(), hydrateSink);
    channel.routeMessageType(TCMessageType.REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE, receiveObject.getSink(),
                             hydrateSink);
    channel.routeMessageType(TCMessageType.OBJECTS_NOT_FOUND_RESPONSE_MESSAGE, receiveObject.getSink(), hydrateSink);
    channel.routeMessageType(TCMessageType.BROADCAST_TRANSACTION_MESSAGE, receiveTransaction.getSink(), hydrateSink);
    channel.routeMessageType(TCMessageType.OBJECT_ID_BATCH_REQUEST_RESPONSE_MESSAGE, oidRequestResponse.getSink(),
                             hydrateSink);
    channel.routeMessageType(TCMessageType.ACKNOWLEDGE_TRANSACTION_MESSAGE, transactionResponse.getSink(), hydrateSink);
    channel.routeMessageType(TCMessageType.BATCH_TRANSACTION_ACK_MESSAGE, batchTxnAckStage.getSink(), hydrateSink);
    channel.routeMessageType(TCMessageType.CLIENT_HANDSHAKE_ACK_MESSAGE, pauseStage.getSink(), hydrateSink);
    channel.routeMessageType(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE, jmxRemoteTunnelStage.getSink(),
                             hydrateSink);
    channel.routeMessageType(TCMessageType.CLUSTER_MEMBERSHIP_EVENT_MESSAGE, pauseStage.getSink(), hydrateSink);
    channel.routeMessageType(TCMessageType.NODES_WITH_OBJECTS_RESPONSE_MESSAGE, clusterMetaDataStage.getSink(),
                             hydrateSink);
    // channel.routeMessageType(TCMessageType.CLUSTER_MEMBERSHIP_EVENT_MESSAGE, clusterEventsStage.getSink(),
    // hydrateSink);

    final int maxConnectRetries = l1Properties.getInt("max.connect.retries");
    int i = 0;
    while (maxConnectRetries <= 0 || i < maxConnectRetries) {
      try {
        DSO_LOGGER.debug("Trying to open channel....");
        channel.open();
        DSO_LOGGER.debug("Channel open");
        break;
      } catch (TCTimeoutException tcte) {
        CONSOLE_LOGGER.warn("Timeout connecting to server: " + tcte.getMessage());
        ThreadUtil.reallySleep(5000);
      } catch (ConnectException e) {
        CONSOLE_LOGGER.warn("Connection refused from server: " + e);
        ThreadUtil.reallySleep(5000);
      } catch (MaxConnectionsExceededException e) {
        int maxClients = AbstractLicenseResolverFactory.getLicense().maxClients();
        CONSOLE_LOGGER.fatal("Your product key only allows maximum " + maxClients
                             + " clients to connect. This client is now shutdown.");
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
    clientHandshakeManager.waitForHandshake();

    final TCSocketAddress remoteAddress = channel.channel().getRemoteAddress();
    final String infoMsg = "Connection successfully established to server at " + remoteAddress;
    CONSOLE_LOGGER.info(infoMsg);
    DSO_LOGGER.info(infoMsg);

    if (statisticsAgentSubSystem.isActive()) {
      statisticsAgentSubSystem.setDefaultAgentDifferentiator("L1/" + channel.channel().getChannelID().toLong());
    }

    cluster.addClusterEventListener(l1Management.getTerracottaCluster());
    if (useOOOLayer) {
      setReconnectCloseOnExit(channel);
    }
    setLoggerOnExit();
  }

  /*
   * Overwrite this routine to do active-active, TODO:: These should go into some interface
   */
  protected ClientGlobalTransactionManager createClientGlobalTransactionManager(
                                                                                final RemoteTransactionManager remoteTxnMgr) {
    return new ClientGlobalTransactionManagerImpl(remoteTxnMgr);
  }

  /*
   * Overwrite this routine to do active-active, TODO:: These should go into some interface
   */
  protected ObjectIDClientHandshakeRequester getObjectIDClientHandshakeRequester(final BatchSequence sequence) {
    return new ObjectIDClientHandshakeRequester(sequence);
  }

  /*
   * Overwrite this routine to do active-active, TODO:: These should go into some interface
   */
  protected RemoteObjectManager createRemoteObjectManager(final TCLogger logger,
                                                          final DSOClientMessageChannel dsoChannel,
                                                          final ObjectRequestMonitor objectRequestMonitor,
                                                          final int faultCount, final SessionManager sessionManager) {
    GroupID defaultGroups[] = dsoChannel.getGroupIDs();
    assert defaultGroups != null && defaultGroups.length == 1;
    return new RemoteObjectManagerImpl(defaultGroups[0], logger, dsoChannel.getClientIDProvider(), dsoChannel
        .getRequestRootMessageFactory(), dsoChannel.getRequestManagedObjectMessageFactory(), objectRequestMonitor,
                                       faultCount, sessionManager);
  }

  /*
   * Overwrite this routine to do active-active, TODO:: These should go into some interface
   */
  protected ClientObjectManagerImpl createObjectManager(final RemoteObjectManager remoteObjectManager,
                                                        final DSOClientConfigHelper dsoConfig,
                                                        final ObjectIDProvider idProvider,
                                                        final ClockEvictionPolicy clockEvictionPolicy,
                                                        final RuntimeLogger rtLogger,
                                                        final ClientIDProvider clientIDProvider,
                                                        final ClassProvider classProviderLocal,
                                                        final TCClassFactory classFactory,
                                                        final TCObjectFactory objectFactory,
                                                        final Portability portability,
                                                        final DSOClientMessageChannel dsoChannel,
                                                        final ToggleableReferenceManager toggleRefMgr) {
    return new ClientObjectManagerImpl(remoteObjectManager, dsoConfig, idProvider, clockEvictionPolicy, rtLogger,
                                       clientIDProvider, classProviderLocal, classFactory, objectFactory, portability,
                                       dsoChannel, toggleRefMgr);
  }

  /*
   * Overwrite this routine to do active-active, TODO:: These should go into some interface
   */
  protected ClientLockManager createLockManager(final ClientIDLogger clientIDLogger,
                                                final RemoteLockManagerImpl remoteLockManagerImpl,
                                                final SessionManager sessionManager,
                                                final ClientLockStatManager lockStatManager,
                                                final ClientLockManagerConfigImpl clientLockManagerConfigImpl) {
    return new StripedClientLockManagerImpl(clientIDLogger, remoteLockManagerImpl, sessionManager, lockStatManager,
                                            clientLockManagerConfigImpl);
  }

  /*
   * Overwrite this routine to do active-active
   */
  protected RemoteTransactionManager createRemoteTransactionManager(
                                                                    final ClientIDProvider cidProvider,
                                                                    final DNAEncoding encoding,
                                                                    final FoldingConfig foldingConfig,
                                                                    final TransactionIDGenerator transactionIDGenerator,
                                                                    final SessionManager sessionManager,
                                                                    final DSOClientMessageChannel dsoChannel,
                                                                    final Counter outstandingBatchesCounter,
                                                                    final SampledCounter numTransactionCounter,
                                                                    final SampledCounter numBatchesCounter,
                                                                    final SampledCounter batchSizeCounter,
                                                                    final Counter pendingBatchesSize) {
    GroupID defaultGroups[] = dsoChannel.getGroupIDs();
    assert defaultGroups != null && defaultGroups.length == 1;
    TransactionBatchFactory txBatchFactory = new TransactionBatchWriterFactory(channel
        .getCommitTransactionMessageFactory(), encoding, foldingConfig);
    return new RemoteTransactionManagerImpl(defaultGroups[0], new ClientIDLogger(cidProvider, TCLogging
        .getLogger(RemoteTransactionManagerImpl.class)), txBatchFactory, transactionIDGenerator, sessionManager,
                                            dsoChannel, outstandingBatchesCounter, numTransactionCounter,
                                            numBatchesCounter, batchSizeCounter, pendingBatchesSize);
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
    manager.stop();
  }

  public ClientTransactionManager getTransactionManager() {
    return txManager;
  }

  public ClientObjectManager getObjectManager() {
    return objectManager;
  }

  public RemoteTransactionManager getRemoteTransactionManager() {
    return rtxManager;
  }

  public CommunicationsManager getCommunicationsManager() {
    return communicationsManager;
  }

  public DSOClientMessageChannel getChannel() {
    return channel;
  }

  public ClientHandshakeManager getClientHandshakeManager() {
    return clientHandshakeManager;
  }

  public ClusterMetaDataManager getClusterMetaDataManager() {
    return clusterMetaDataManager;
  }

  public SessionMonitor getHttpSessionMonitor() {
    return l1Management.getHttpSessionMonitor();
  }

  public DmiManager getDmiManager() {
    return dmiManager;
  }

  public StatisticsAgentSubSystem getStatisticsAgentSubSystem() {
    return statisticsAgentSubSystem;
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
      i.set("objectManager", objectManager);
      i.set("lockmanager", lockManager);
      i.set("txManager", txManager);
      i.set("portnum", port);
      i.eval("setAccessibility(true)"); // turn off access restrictions
      i.eval("server(portnum)");
      CONSOLE_LOGGER.info("Bean shell is started on port " + port);
    } catch (EvalError e) {
      e.printStackTrace();
    }
  }
}
