/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import bsh.EvalError;
import bsh.Interpreter;

import com.tc.aspectwerkz.reflect.ClassInfoRepository;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfoRepository;
import com.tc.aspectwerkz.reflect.impl.java.JavaClassInfoRepository;
import com.tc.async.api.SEDA;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.bytes.TCByteBuffer;
import com.tc.client.ClientMode;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.handler.CallbackDumpAdapter;
import com.tc.handler.CallbackDumpHandler;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.lang.TCThreadGroup;
import com.tc.license.LicenseManager;
import com.tc.logging.ClientIDLogger;
import com.tc.logging.ClientIDLoggerProvider;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.logging.ThreadDumpHandler;
import com.tc.management.ClientLockStatManager;
import com.tc.management.L1Management;
import com.tc.management.TCClient;
import com.tc.management.lock.stats.LockStatisticsMessage;
import com.tc.management.lock.stats.LockStatisticsResponseMessageImpl;
import com.tc.management.remote.protocol.terracotta.JmxRemoteTunnelMessage;
import com.tc.management.remote.protocol.terracotta.L1JmxReady;
import com.tc.management.remote.protocol.terracotta.TunneledDomainManager;
import com.tc.management.remote.protocol.terracotta.TunneledDomainsChanged;
import com.tc.management.remote.protocol.terracotta.TunnelingEventHandler;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ClusterTopologyChangedListener;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OOONetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.GeneratedMessageFactory;
import com.tc.net.protocol.tcm.HydrateHandler;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.MessageMonitorImpl;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageRouter;
import com.tc.net.protocol.tcm.TCMessageRouterImpl;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.HealthCheckerConfigClientImpl;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.net.protocol.transport.ReconnectionRejectedCallback;
import com.tc.net.protocol.transport.ReconnectionRejectedDefaultHandler;
import com.tc.net.protocol.transport.ReconnectionRejectedExpressRejoinClientBehaviour;
import com.tc.net.protocol.transport.ReconnectionRejectedHandler;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.cache.CacheConfig;
import com.tc.object.cache.CacheConfigImpl;
import com.tc.object.cache.CacheManager;
import com.tc.object.config.ConnectionInfoConfig;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAEncodingInternal;
import com.tc.object.event.DmiManager;
import com.tc.object.event.DmiManagerImpl;
import com.tc.object.gtx.ClientGlobalTransactionManager;
import com.tc.object.handler.BatchTransactionAckHandler;
import com.tc.object.handler.ClientCoordinationHandler;
import com.tc.object.handler.ClusterInternalEventsHandler;
import com.tc.object.handler.ClusterMemberShipEventsHandler;
import com.tc.object.handler.ClusterMetaDataHandler;
import com.tc.object.handler.DmiHandler;
import com.tc.object.handler.LockRecallHandler;
import com.tc.object.handler.LockResponseHandler;
import com.tc.object.handler.LockStatisticsEnableDisableHandler;
import com.tc.object.handler.LockStatisticsResponseHandler;
import com.tc.object.handler.ReceiveInvalidationHandler;
import com.tc.object.handler.ReceiveObjectHandler;
import com.tc.object.handler.ReceiveRootIDHandler;
import com.tc.object.handler.ReceiveSearchQueryResponseHandler;
import com.tc.object.handler.ReceiveServerMapEvictionBroadcastHandler;
import com.tc.object.handler.ReceiveServerMapResponseHandler;
import com.tc.object.handler.ReceiveSyncWriteTransactionAckHandler;
import com.tc.object.handler.ReceiveTransactionCompleteHandler;
import com.tc.object.handler.ReceiveTransactionHandler;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.handshakemanager.ClientHandshakeManagerImpl;
import com.tc.object.idprovider.api.ObjectIDProvider;
import com.tc.object.idprovider.impl.RemoteObjectIDBatchSequenceProvider;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.locks.ClientLockManager;
import com.tc.object.locks.ClientLockManagerConfigImpl;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.locks.LocksRecallService;
import com.tc.object.locks.LocksRecallServiceImpl;
import com.tc.object.logging.RuntimeLogger;
import com.tc.object.msg.AcknowledgeTransactionMessageImpl;
import com.tc.object.msg.BatchTransactionAcknowledgeMessageImpl;
import com.tc.object.msg.BroadcastTransactionMessageImpl;
import com.tc.object.msg.ClientHandshakeAckMessageImpl;
import com.tc.object.msg.ClientHandshakeMessageImpl;
import com.tc.object.msg.ClientHandshakeRefusedMessageImpl;
import com.tc.object.msg.ClusterMembershipMessage;
import com.tc.object.msg.CommitTransactionMessageImpl;
import com.tc.object.msg.CompletedTransactionLowWaterMarkMessage;
import com.tc.object.msg.GetAllKeysServerMapRequestMessageImpl;
import com.tc.object.msg.GetAllKeysServerMapResponseMessageImpl;
import com.tc.object.msg.GetAllSizeServerMapRequestMessageImpl;
import com.tc.object.msg.GetAllSizeServerMapResponseMessageImpl;
import com.tc.object.msg.GetValueServerMapRequestMessageImpl;
import com.tc.object.msg.GetValueServerMapResponseMessageImpl;
import com.tc.object.msg.InvalidateObjectsMessage;
import com.tc.object.msg.JMXMessage;
import com.tc.object.msg.KeysForOrphanedValuesMessageImpl;
import com.tc.object.msg.KeysForOrphanedValuesResponseMessageImpl;
import com.tc.object.msg.LockRequestMessage;
import com.tc.object.msg.LockResponseMessage;
import com.tc.object.msg.NodeMetaDataMessageImpl;
import com.tc.object.msg.NodeMetaDataResponseMessageImpl;
import com.tc.object.msg.NodesWithKeysMessageImpl;
import com.tc.object.msg.NodesWithKeysResponseMessageImpl;
import com.tc.object.msg.NodesWithObjectsMessageImpl;
import com.tc.object.msg.NodesWithObjectsResponseMessageImpl;
import com.tc.object.msg.ObjectIDBatchRequestMessage;
import com.tc.object.msg.ObjectIDBatchRequestResponseMessage;
import com.tc.object.msg.ObjectNotFoundServerMapResponseMessageImpl;
import com.tc.object.msg.ObjectsNotFoundMessageImpl;
import com.tc.object.msg.RequestManagedObjectMessageImpl;
import com.tc.object.msg.RequestManagedObjectResponseMessageImpl;
import com.tc.object.msg.RequestRootMessageImpl;
import com.tc.object.msg.RequestRootResponseMessage;
import com.tc.object.msg.SearchQueryRequestMessageImpl;
import com.tc.object.msg.SearchQueryResponseMessageImpl;
import com.tc.object.msg.ServerMapEvictionBroadcastMessageImpl;
import com.tc.object.msg.SyncWriteTransactionReceivedMessage;
import com.tc.object.net.DSOClientMessageChannel;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;
import com.tc.object.servermap.localcache.impl.L1ServerMapCapacityEvictionHandler;
import com.tc.object.servermap.localcache.impl.L1ServerMapLocalCacheManagerImpl;
import com.tc.object.servermap.localcache.impl.L1ServerMapTransactionCompletionHandler;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.object.session.SessionManagerImpl;
import com.tc.object.session.SessionProvider;
import com.tc.object.tx.ClientTransactionBatchWriter.FoldingConfig;
import com.tc.object.tx.ClientTransactionFactory;
import com.tc.object.tx.ClientTransactionFactoryImpl;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.object.tx.ClientTransactionManagerImpl;
import com.tc.object.tx.RemoteTransactionManager;
import com.tc.object.tx.TransactionIDGenerator;
import com.tc.operatorevent.TerracottaOperatorEventLogging;
import com.tc.properties.ReconnectConfig;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.runtime.TCMemoryManagerImpl;
import com.tc.runtime.logging.LongGCLogger;
import com.tc.runtime.logging.MemoryOperatorEventListener;
import com.tc.statistics.StatisticsAgentSubSystem;
import com.tc.statistics.StatisticsAgentSubSystemCallback;
import com.tc.statistics.StatisticsSystemType;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;
import com.tc.statistics.retrieval.actions.SRACacheObjectsEvictRequest;
import com.tc.statistics.retrieval.actions.SRACacheObjectsEvicted;
import com.tc.statistics.retrieval.actions.SRAL1OutstandingBatches;
import com.tc.statistics.retrieval.actions.SRAL1PendingBatchesSize;
import com.tc.statistics.retrieval.actions.SRAL1TransactionCount;
import com.tc.statistics.retrieval.actions.SRAL1TransactionSize;
import com.tc.statistics.retrieval.actions.SRAL1TransactionsPerBatch;
import com.tc.statistics.retrieval.actions.SRAMemoryUsage;
import com.tc.statistics.retrieval.actions.SRAMessages;
import com.tc.statistics.retrieval.actions.SRAStageQueueDepths;
import com.tc.statistics.retrieval.actions.SRASystemProperties;
import com.tc.statistics.retrieval.actions.SRAVmGarbageCollector;
import com.tc.statistics.retrieval.actions.SRAVmGarbageCollector.SRAVmGarbageCollectorType;
import com.tc.stats.counter.Counter;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.stats.counter.SimpleCounterConfig;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.stats.counter.sampled.derived.SampledRateCounterConfig;
import com.tc.util.Assert;
import com.tc.util.CommonShutDownHook;
import com.tc.util.ProductInfo;
import com.tc.util.TCTimeoutException;
import com.tc.util.TCTimerService;
import com.tc.util.ToggleableReferenceManager;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.LockInfoByThreadID;
import com.tc.util.runtime.LockState;
import com.tc.util.runtime.ThreadIDManager;
import com.tc.util.runtime.ThreadIDManagerImpl;
import com.tc.util.runtime.ThreadIDMap;
import com.tc.util.runtime.ThreadIDMapImpl;
import com.tc.util.sequence.BatchSequence;
import com.tc.util.sequence.BatchSequenceReceiver;
import com.tc.util.sequence.Sequence;
import com.tc.util.sequence.SimpleSequence;
import com.tcclient.cluster.DsoClusterInternal;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the main point of entry into the DSO client.
 */
public class DistributedObjectClient extends SEDA implements TCClient {

  public final static String                         DEFAULT_AGENT_DIFFERENTIATOR_PREFIX = "L1/";

  protected static final TCLogger                    DSO_LOGGER                          = CustomerLogging
                                                                                             .getDSOGenericLogger();
  private static final TCLogger                      CONSOLE_LOGGER                      = CustomerLogging
                                                                                             .getConsoleLogger();

  private final DSOClientBuilder                     dsoClientBuilder;
  private final ClientMode                           clientMode;
  private final DSOClientConfigHelper                config;
  private final ClassProvider                        classProvider;
  private final Manager                              manager;
  private final DsoClusterInternal                   dsoCluster;
  private final TCThreadGroup                        threadGroup;
  private final StatisticsAgentSubSystem             statisticsAgentSubSystem;
  private final RuntimeLogger                        runtimeLogger;
  private final ThreadIDMap                          threadIDMap;

  protected final PreparedComponentsFromL2Connection connectionComponents;

  private DSOClientMessageChannel                    channel;
  private ClientLockManager                          lockManager;
  private ClientObjectManagerImpl                    objectManager;
  private RemoteSearchRequestManager                 searchRequestManager;
  private ClientTransactionManagerImpl               txManager;
  private CommunicationsManager                      communicationsManager;
  private RemoteTransactionManager                   rtxManager;
  private ClientHandshakeManager                     clientHandshakeManager;
  private ClusterMetaDataManager                     clusterMetaDataManager;
  private CacheManager                               cacheManager;
  private L1Management                               l1Management;
  private TCProperties                               l1Properties;
  private DmiManager                                 dmiManager;
  private boolean                                    createDedicatedMBeanServer          = false;
  private CounterManager                             counterManager;
  private ThreadIDManager                            threadIDManager;
  private final CallbackDumpHandler                  dumpHandler                         = new CallbackDumpHandler();
  private TunneledDomainManager                      tunneledDomainManager;
  private TCMemoryManagerImpl                        tcMemManager;
  private ReconnectionRejectedListener               reconnectionRejectedListener;

  private Stage                                      clusterEventsStage;

  private L1ServerMapLocalCacheManager               globalLocalCacheManager;

  public DistributedObjectClient(final DSOClientConfigHelper config, final TCThreadGroup threadGroup,
                                 final ClassProvider classProvider,
                                 final PreparedComponentsFromL2Connection connectionComponents, final Manager manager,
                                 final StatisticsAgentSubSystem statisticsAgentSubSystem,
                                 final DsoClusterInternal dsoCluster, final RuntimeLogger runtimeLogger) {
    this(config, threadGroup, classProvider, connectionComponents, manager, statisticsAgentSubSystem, dsoCluster,
         runtimeLogger, ClientMode.DSO_MODE);
  }

  public DistributedObjectClient(final DSOClientConfigHelper config, final TCThreadGroup threadGroup,
                                 final ClassProvider classProvider,
                                 final PreparedComponentsFromL2Connection connectionComponents, final Manager manager,
                                 final StatisticsAgentSubSystem statisticsAgentSubSystem,
                                 final DsoClusterInternal dsoCluster, final RuntimeLogger runtimeLogger,
                                 final ClientMode clientMode) {
    super(threadGroup);
    Assert.assertNotNull(config);
    this.clientMode = clientMode;
    this.config = config;
    this.classProvider = classProvider;
    this.connectionComponents = connectionComponents;
    this.manager = manager;
    this.dsoCluster = dsoCluster;
    this.threadGroup = threadGroup;
    this.statisticsAgentSubSystem = statisticsAgentSubSystem;
    this.threadIDMap = new ThreadIDMapImpl();
    this.runtimeLogger = runtimeLogger;
    this.dsoClientBuilder = createClientBuilder();
  }

  protected DSOClientBuilder createClientBuilder() {
    if (this.connectionComponents.isActiveActive()) {
      CONSOLE_LOGGER
          .fatal("An attempt to start a Terracotta server array with more than one active server failed. "
                 + "This feature is not available in the currently installed Terracotta platform. For more information on "
                 + "supported features for Terracotta platforms, please see this link http://www.terracotta.org/sadne");
      System.exit(3);
    }
    return new StandardDSOClientBuilder();
  }

  public ThreadIDMap getThreadIDMap() {
    return this.threadIDMap;
  }

  public void addAllLocksTo(final LockInfoByThreadID lockInfo) {
    if (this.lockManager != null) {
      for (final ClientServerExchangeLockContext c : this.lockManager.getAllLockContexts()) {
        switch (c.getState().getType()) {
          case GREEDY_HOLDER:
          case HOLDER:
            lockInfo.addLock(LockState.HOLDING, c.getThreadID(), c.getLockID().toString());
            break;
          case WAITER:
            lockInfo.addLock(LockState.WAITING_ON, c.getThreadID(), c.getLockID().toString());
            break;
          case TRY_PENDING:
          case PENDING:
            lockInfo.addLock(LockState.WAITING_TO, c.getThreadID(), c.getLockID().toString());
            break;
        }
      }
    } else {
      DSO_LOGGER.error("LockManager not initialised still. LockInfo for threads cannot be updated");
    }
  }

  public void setCreateDedicatedMBeanServer(final boolean createDedicatedMBeanServer) {
    this.createDedicatedMBeanServer = createDedicatedMBeanServer;
  }

  private class StatisticsSetupCallback implements StatisticsAgentSubSystemCallback {
    final StageManager       stageManager;
    final MessageMonitor     messageMonitor;
    final Counter            outstandingBatchesCounter;
    final Counter            pendingBatchesSize;
    final SampledRateCounter transactionSizeCounter;
    final SampledRateCounter transactionsPerBatchCounter;
    final SampledCounter     txCounter;

    public StatisticsSetupCallback(final StageManager stageManager, final MessageMonitor messageMonitor,
                                   final Counter outstandingBatchesCounter, final Counter pendingBatchesSize,
                                   final SampledRateCounter transactionSizeCounter,
                                   final SampledRateCounter transactionsPerBatchCounter, final SampledCounter txCounter) {
      this.stageManager = stageManager;
      this.messageMonitor = messageMonitor;
      this.outstandingBatchesCounter = outstandingBatchesCounter;
      this.pendingBatchesSize = pendingBatchesSize;
      this.transactionSizeCounter = transactionSizeCounter;
      this.transactionsPerBatchCounter = transactionsPerBatchCounter;
      this.txCounter = txCounter;
    }

    public void setupComplete(final StatisticsAgentSubSystem subsystem) {
      final StatisticsRetrievalRegistry registry = subsystem.getStatisticsRetrievalRegistry();

      registry.registerActionInstance(new SRAMemoryUsage());
      registry.registerActionInstance(new SRASystemProperties());
      registry.registerActionInstance("com.tc.statistics.retrieval.actions.SRAThreadDump");
      registry.registerActionInstance(new SRAStageQueueDepths(this.stageManager));
      registry.registerActionInstance(new SRACacheObjectsEvictRequest());
      registry.registerActionInstance(new SRACacheObjectsEvicted());
      registry.registerActionInstance(new SRAMessages(this.messageMonitor));
      registry.registerActionInstance(new SRAL1OutstandingBatches(this.outstandingBatchesCounter));
      registry.registerActionInstance(new SRAL1TransactionsPerBatch(this.transactionsPerBatchCounter));
      registry.registerActionInstance(new SRAL1TransactionSize(this.transactionSizeCounter));
      registry.registerActionInstance(new SRAL1PendingBatchesSize(this.pendingBatchesSize));
      registry.registerActionInstance(new SRAL1TransactionCount(this.txCounter));
      registry.registerActionInstance(new SRAVmGarbageCollector(SRAVmGarbageCollectorType.L1_VM_GARBAGE_COLLECTOR));
    }
  }

  private void validateGroupConfigOrExit() {
    final boolean toCheckTopology = TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.L1_L2_CONFIG_VALIDATION_ENABLED);
    if (toCheckTopology) {
      try {
        this.config.validateGroupInfo();
      } catch (final ConfigurationSetupException e) {
        CONSOLE_LOGGER.error(e.getMessage());
        System.exit(1);
      }
    }
  }

  private ReconnectConfig getReconnectPropertiesFromServerOrExit() {
    ReconnectConfig reconnectConfig = null;
    try {
      reconnectConfig = this.config.getL1ReconnectProperties();
    } catch (ConfigurationSetupException e) {
      CONSOLE_LOGGER.error(e.getMessage());
      System.exit(1);
    }
    return reconnectConfig;
  }

  private NetworkStackHarnessFactory getNetworkStackHarnessFactory(boolean useOOOLayer,
                                                                   ReconnectConfig l1ReconnectConfig) {
    if (useOOOLayer) {
      return new OOONetworkStackHarnessFactory(new OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl(), l1ReconnectConfig);
    } else {
      return new PlainNetworkStackHarnessFactory();
    }
  }

  public Stage getClusterEventsStage() {
    return clusterEventsStage;
  }

  public void start() {
    start(null);
  }

  public synchronized void start(CountDownLatch testStartLatch) {

    validateGroupConfigOrExit();

    final TCProperties tcProperties = TCPropertiesImpl.getProperties();
    this.l1Properties = tcProperties.getPropertiesFor("l1");
    final int maxSize = tcProperties.getInt(TCPropertiesConsts.L1_SEDA_STAGE_SINK_CAPACITY);
    final int faultCount = this.config.getFaultCount();

    final SessionManager sessionManager = new SessionManagerImpl(new SessionManagerImpl.SequenceFactory() {
      public Sequence newSequence() {
        return new SimpleSequence();
      }
    });
    final SessionProvider sessionProvider = sessionManager;

    this.threadGroup.addCallbackOnExitDefaultHandler(new ThreadDumpHandler(this));
    final StageManager stageManager = getStageManager();
    this.dumpHandler.registerForDump(new CallbackDumpAdapter(stageManager));

    final ReconnectConfig l1ReconnectConfig = getReconnectPropertiesFromServerOrExit();

    final boolean useOOOLayer = l1ReconnectConfig.getReconnectEnabled();
    final NetworkStackHarnessFactory networkStackHarnessFactory = getNetworkStackHarnessFactory(useOOOLayer,
                                                                                                l1ReconnectConfig);

    this.counterManager = new CounterManagerImpl();
    final MessageMonitor mm = MessageMonitorImpl.createMonitor(tcProperties, DSO_LOGGER);
    final DNAEncodingInternal encoding = new ApplicatorDNAEncodingImpl(this.classProvider);
    final TCMessageRouter messageRouter = new TCMessageRouterImpl();

    ReconnectionRejectedHandler reconnectionRejectedHandler;
    if (clientMode.isExpressRejoinClient()) {
      reconnectionRejectedHandler = new ReconnectionRejectedExpressRejoinClientBehaviour();
    } else {
      reconnectionRejectedHandler = new ReconnectionRejectedDefaultHandler();
    }

    this.communicationsManager = this.dsoClientBuilder
        .createCommunicationsManager(mm,
                                     messageRouter,
                                     networkStackHarnessFactory,
                                     new NullConnectionPolicy(),
                                     this.connectionComponents.createConnectionInfoConfigItemByGroup().length,
                                     new HealthCheckerConfigClientImpl(this.l1Properties
                                         .getPropertiesFor("healthcheck.l2"), "DSO Client"),
                                     getMessageTypeClassMapping(), getMessageTypeFactoryMApping(encoding),
                                     reconnectionRejectedHandler);

    DSO_LOGGER.debug("Created CommunicationsManager.");

    final ConnectionInfoConfig[] connectionInfoItems = this.connectionComponents
        .createConnectionInfoConfigItemByGroup();
    final ConnectionInfo[] connectionInfo = connectionInfoItems[0].getConnectionInfos();
    final String serverHost = connectionInfo[0].getHostname();
    final int serverPort = connectionInfo[0].getPort();

    clusterEventsStage = stageManager.createStage(ClientConfigurationContext.CLUSTER_EVENTS_STAGE,
                                                  new ClusterInternalEventsHandler(dsoCluster), 1, maxSize);

    final int socketConnectTimeout = tcProperties.getInt(TCPropertiesConsts.L1_SOCKET_CONNECT_TIMEOUT);
    final int maxConnectRetries = tcProperties.getInt(TCPropertiesConsts.L1_MAX_CONNECT_RETRIES);
    if (socketConnectTimeout < 0) { throw new IllegalArgumentException("invalid socket time value: "
                                                                       + socketConnectTimeout); }
    this.channel = this.dsoClientBuilder.createDSOClientMessageChannel(this.communicationsManager,
                                                                       this.connectionComponents, sessionProvider,
                                                                       maxConnectRetries, socketConnectTimeout, this);
    final ClientIDLoggerProvider cidLoggerProvider = new ClientIDLoggerProvider(this.channel.getClientIDProvider());
    stageManager.setLoggerProvider(cidLoggerProvider);

    DSO_LOGGER.debug("Created channel.");

    TerracottaOperatorEventLogging.setNodeNameProvider(new ClientNameProvider(this.dsoCluster));

    final ClientTransactionFactory txFactory = new ClientTransactionFactoryImpl(this.runtimeLogger);

    final SampledRateCounterConfig sampledRateCounterConfig = new SampledRateCounterConfig(1, 300, true);
    final SampledRateCounter transactionSizeCounter = (SampledRateCounter) this.counterManager
        .createCounter(sampledRateCounterConfig);
    final SampledRateCounter transactionsPerBatchCounter = (SampledRateCounter) this.counterManager
        .createCounter(sampledRateCounterConfig);
    final Counter outstandingBatchesCounter = this.counterManager.createCounter(new SimpleCounterConfig(0));
    final Counter pendingBatchesSize = this.counterManager.createCounter(new SimpleCounterConfig(0));

    this.rtxManager = this.dsoClientBuilder.createRemoteTransactionManager(this.channel.getClientIDProvider(),
                                                                           encoding, FoldingConfig
                                                                               .createFromProperties(tcProperties),
                                                                           new TransactionIDGenerator(),
                                                                           sessionManager, this.channel,
                                                                           outstandingBatchesCounter,
                                                                           pendingBatchesSize, transactionSizeCounter,
                                                                           transactionsPerBatchCounter);

    this.dumpHandler.registerForDump(new CallbackDumpAdapter(this.rtxManager));
    final RemoteObjectIDBatchSequenceProvider remoteIDProvider = new RemoteObjectIDBatchSequenceProvider(
                                                                                                         this.channel
                                                                                                             .getObjectIDBatchRequestMessageFactory());

    // create Sequences
    final BatchSequence[] sequences = this.dsoClientBuilder.createSequences(remoteIDProvider, this.l1Properties
        .getInt("objectmanager.objectid.request.size"));
    // get Sequence Receiver -- passing in sequences
    final BatchSequenceReceiver batchSequenceReceiver = this.dsoClientBuilder.getBatchReceiver(sequences);
    // create object id provider
    final ObjectIDProvider idProvider = this.dsoClientBuilder
        .createObjectIdProvider(sequences, this.channel.getClientIDProvider());
    remoteIDProvider.setBatchSequenceReceiver(batchSequenceReceiver);

    final ToggleableReferenceManager toggleRefMgr = new ToggleableReferenceManager();

    // for SRA L1 Tx count
    final SampledCounterConfig sampledCounterConfig = new SampledCounterConfig(1, 300, true, 0L);
    final SampledCounter txnCounter = (SampledCounter) this.counterManager.createCounter(sampledCounterConfig);

    // setup statistics subsystem
    this.statisticsAgentSubSystem.addCallback(new StatisticsSetupCallback(stageManager, mm, outstandingBatchesCounter,
                                                                          pendingBatchesSize, transactionSizeCounter,
                                                                          transactionsPerBatchCounter, txnCounter));
    this.statisticsAgentSubSystem.setup(StatisticsSystemType.CLIENT, null);

    final RemoteObjectManager remoteObjectManager = this.dsoClientBuilder
        .createRemoteObjectManager(new ClientIDLogger(this.channel.getClientIDProvider(), TCLogging
                                       .getLogger(RemoteObjectManager.class)), this.channel, faultCount, sessionManager);
    this.dumpHandler.registerForDump(new CallbackDumpAdapter(remoteObjectManager));

    LockRecallHandler lockRecallHandler = new LockRecallHandler();
    final Stage lockRecallStage = stageManager.createStage(ClientConfigurationContext.LOCK_RECALL_STAGE,
                                                           lockRecallHandler, 8, maxSize);
    LocksRecallService locksRecallHelper = new LocksRecallServiceImpl(lockRecallHandler, lockRecallStage);

    searchRequestManager = this.dsoClientBuilder
        .createRemoteSearchRequestManager(new ClientIDLogger(this.channel.getClientIDProvider(), TCLogging
                                              .getLogger(RemoteObjectManager.class)), this.channel, sessionManager);

    final L1ServerMapCapacityEvictionHandler l1ServerMapCapacityEvictionHandler = new L1ServerMapCapacityEvictionHandler();
    final Stage capacityEvictionStage = stageManager.createStage(ClientConfigurationContext.CAPACITY_EVICTION_STAGE,
                                                                 l1ServerMapCapacityEvictionHandler, 8, maxSize);

    L1ServerMapTransactionCompletionHandler completionHandler = new L1ServerMapTransactionCompletionHandler();
    final Stage txnCompleteStage = stageManager
        .createStage(ClientConfigurationContext.LOCAL_CACHE_TXN_COMPLETE_STAGE, completionHandler, TCPropertiesImpl
            .getProperties().getInt(TCPropertiesConsts.L2_LOCAL_CACHE_TXN_COMPLETE_THREADS), TCPropertiesImpl
            .getProperties().getInt(TCPropertiesConsts.L2_LOCAL_CACHE_TXN_COMPLETE_SINK_CAPACITY));
    globalLocalCacheManager = new L1ServerMapLocalCacheManagerImpl(locksRecallHelper, capacityEvictionStage.getSink(),
                                                                   txnCompleteStage.getSink());
    l1ServerMapCapacityEvictionHandler.initialize(globalLocalCacheManager);

    final RemoteServerMapManager remoteServerMapManager = this.dsoClientBuilder
        .createRemoteServerMapManager(new ClientIDLogger(this.channel.getClientIDProvider(), TCLogging
                                          .getLogger(RemoteObjectManager.class)), this.channel, sessionManager,
                                      globalLocalCacheManager);

    final ClientGlobalTransactionManager gtxManager = this.dsoClientBuilder
        .createClientGlobalTransactionManager(this.rtxManager, remoteServerMapManager);

    final TCClassFactory classFactory = this.dsoClientBuilder.createTCClassFactory(this.config, this.classProvider,
                                                                                   encoding, this.manager,
                                                                                   globalLocalCacheManager,
                                                                                   remoteServerMapManager);
    final TCObjectFactory objectFactory = new TCObjectFactoryImpl(classFactory);

    this.objectManager = this.dsoClientBuilder.createObjectManager(remoteObjectManager, this.config, idProvider,
                                                                   this.runtimeLogger,
                                                                   this.channel.getClientIDProvider(),
                                                                   this.classProvider, classFactory, objectFactory,
                                                                   this.config.getPortability(), this.channel,
                                                                   toggleRefMgr, globalLocalCacheManager);
    this.globalLocalCacheManager.initializeTCObjectSelfStore(objectManager);

    this.threadGroup.addCallbackOnExitDefaultHandler(new CallbackDumpAdapter(this.objectManager));
    this.dumpHandler.registerForDump(new CallbackDumpAdapter(this.objectManager));

    final TCProperties cacheManagerProperties = this.l1Properties.getPropertiesFor("cachemanager");
    final CacheConfig cacheConfig = new CacheConfigImpl(cacheManagerProperties);
    this.tcMemManager = new TCMemoryManagerImpl(cacheConfig.getSleepInterval(), cacheConfig.getLeastCount(),
                                                cacheConfig.isOnlyOldGenMonitored(), getThreadGroup(), false);
    final long timeOut = TCPropertiesImpl.getProperties().getLong(TCPropertiesConsts.LOGGING_LONG_GC_THRESHOLD);
    final LongGCLogger gcLogger = this.dsoClientBuilder.createLongGCLogger(timeOut);
    this.tcMemManager.registerForMemoryEvents(gcLogger);
    // CDV-1181 warn if using CMS
    this.tcMemManager.checkGarbageCollectors();

    if (cacheManagerProperties.getBoolean("enabled")) {
      this.cacheManager = new CacheManager(this.objectManager, cacheConfig, getThreadGroup(),
                                           this.statisticsAgentSubSystem, this.tcMemManager);
      this.cacheManager.start();
      if (DSO_LOGGER.isDebugEnabled()) {
        DSO_LOGGER.debug("CacheManager Enabled : " + this.cacheManager);
      }
    } else {
      DSO_LOGGER.warn("CacheManager is Disabled");
    }

    this.threadIDManager = new ThreadIDManagerImpl(this.threadIDMap);

    // Cluster meta data
    this.clusterMetaDataManager = this.dsoClientBuilder
        .createClusterMetaDataManager(this.channel, encoding, this.threadIDManager,
                                      this.channel.getNodesWithObjectsMessageFactory(),
                                      this.channel.getKeysForOrphanedValuesMessageFactory(),
                                      this.channel.getNodeMetaDataMessageFactory(),
                                      this.channel.getNodesWithKeysMessageFactory());

    // Set up the JMX management stuff
    final TunnelingEventHandler teh = this.dsoClientBuilder.createTunnelingEventHandler(this.channel.channel(),
                                                                                        this.config);
    this.tunneledDomainManager = this.dsoClientBuilder.createTunneledDomainManager(this.channel.channel(), this.config,
                                                                                   teh);

    this.l1Management = this.dsoClientBuilder.createL1Management(teh, this.statisticsAgentSubSystem,
                                                                 this.runtimeLogger,
                                                                 this.manager.getInstrumentationLogger(),
                                                                 this.config.rawConfigText(), this);
    this.l1Management.start(this.createDedicatedMBeanServer);

    // register the terracotta operator event logger
    this.dsoClientBuilder.registerForOperatorEvents(this.l1Management);

    // Setup the lock manager
    final ClientLockStatManager lockStatManager = this.dsoClientBuilder.createLockStatsManager();
    this.lockManager = this.dsoClientBuilder
        .createLockManager(this.channel,
                           new ClientIDLogger(this.channel.getClientIDProvider(), TCLogging
                               .getLogger(ClientLockManager.class)), sessionManager, lockStatManager, this.channel
                               .getLockRequestMessageFactory(), this.threadIDManager, gtxManager,
                           new ClientLockManagerConfigImpl(this.l1Properties.getPropertiesFor("lockmanager")));
    this.globalLocalCacheManager.setLockManager(this.lockManager);
    final CallbackDumpAdapter lockDumpAdapter = new CallbackDumpAdapter(this.lockManager);
    this.threadGroup.addCallbackOnExitDefaultHandler(lockDumpAdapter);
    this.dumpHandler.registerForDump(lockDumpAdapter);

    // Setup the transaction manager
    this.txManager = new ClientTransactionManagerImpl(this.channel.getClientIDProvider(), this.objectManager,
                                                      txFactory, this.lockManager, this.rtxManager, this.runtimeLogger,
                                                      txnCounter, globalLocalCacheManager);

    final CallbackDumpAdapter txnMgrDumpAdapter = new CallbackDumpAdapter(this.txManager);
    this.threadGroup.addCallbackOnExitDefaultHandler(txnMgrDumpAdapter);
    this.dumpHandler.registerForDump(txnMgrDumpAdapter);

    // Create the SEDA stages
    final Stage lockResponse = stageManager.createStage(ClientConfigurationContext.LOCK_RESPONSE_STAGE,
                                                        new LockResponseHandler(sessionManager),
                                                        this.channel.getGroupIDs().length, 1, maxSize);
    final Stage receiveRootID = stageManager.createStage(ClientConfigurationContext.RECEIVE_ROOT_ID_STAGE,
                                                         new ReceiveRootIDHandler(), 1, maxSize);
    final Stage receiveObject = stageManager.createStage(ClientConfigurationContext.RECEIVE_OBJECT_STAGE,
                                                         new ReceiveObjectHandler(), 1, maxSize);
    this.dmiManager = new DmiManagerImpl(this.classProvider, this.objectManager, this.runtimeLogger);
    final Stage dmiStage = stageManager.createStage(ClientConfigurationContext.DMI_STAGE,
                                                    new DmiHandler(this.dmiManager), 1, maxSize);

    final Stage receiveTransaction = stageManager
        .createStage(ClientConfigurationContext.RECEIVE_TRANSACTION_STAGE,
                     new ReceiveTransactionHandler(this.channel.getClientIDProvider(), this.channel
                         .getAcknowledgeTransactionMessageFactory(), gtxManager, sessionManager, dmiStage.getSink(),
                                                   this.dmiManager, testStartLatch), 1, maxSize);
    final Stage oidRequestResponse = stageManager
        .createStage(ClientConfigurationContext.OBJECT_ID_REQUEST_RESPONSE_STAGE, remoteIDProvider, 1, maxSize);
    final Stage transactionResponse = stageManager
        .createStage(ClientConfigurationContext.RECEIVE_TRANSACTION_COMPLETE_STAGE,
                     new ReceiveTransactionCompleteHandler(), 1, maxSize);
    final Stage hydrateStage = stageManager.createStage(ClientConfigurationContext.HYDRATE_MESSAGE_STAGE,
                                                        new HydrateHandler(), this.channel.getGroupIDs().length, 1,
                                                        maxSize);
    final Stage batchTxnAckStage = stageManager.createStage(ClientConfigurationContext.BATCH_TXN_ACK_STAGE,
                                                            new BatchTransactionAckHandler(), 1, maxSize);
    final Stage receiveServerMapStage = stageManager
        .createStage(ClientConfigurationContext.RECEIVE_SERVER_MAP_RESPONSE_STAGE,
                     new ReceiveServerMapResponseHandler(remoteServerMapManager), 1, maxSize);
    final Stage receiveSearchQueryStage = stageManager
        .createStage(ClientConfigurationContext.RECEIVE_SEARCH_QUERY_RESPONSE_STAGE,
                     new ReceiveSearchQueryResponseHandler(searchRequestManager), 1, maxSize);

    final Stage receiveServerMapEvictionBroadcastStage = stageManager
        .createStage(ClientConfigurationContext.RECEIVE_SERVER_MAP_EVICTION_BROADCAST_STAGE,
                     new ReceiveServerMapEvictionBroadcastHandler(this.objectManager), 1, maxSize);

    // By design this stage needs to be single threaded. If it wasn't then cluster membership messages could get
    // processed before the client handshake ack, and this client would get a faulty view of the cluster at best, or
    // more likely an AssertionError
    final Stage pauseStage = stageManager.createStage(ClientConfigurationContext.CLIENT_COORDINATION_STAGE,
                                                      new ClientCoordinationHandler(), 1, maxSize);

    final Stage clusterMembershipEventStage = stageManager
        .createStage(ClientConfigurationContext.CLUSTER_MEMBERSHIP_EVENT_STAGE,
                     new ClusterMemberShipEventsHandler(dsoCluster), 1, maxSize);

    final Stage clusterMetaDataStage = stageManager.createStage(ClientConfigurationContext.CLUSTER_METADATA_STAGE,
                                                                new ClusterMetaDataHandler(), 1, maxSize);

    // Lock statistics
    final Stage lockStatisticsStage = stageManager
        .createStage(ClientConfigurationContext.LOCK_STATISTICS_RESPONSE_STAGE, new LockStatisticsResponseHandler(), 1,
                     1);
    final Stage lockStatisticsEnableDisableStage = stageManager
        .createStage(ClientConfigurationContext.LOCK_STATISTICS_ENABLE_DISABLE_STAGE,
                     new LockStatisticsEnableDisableHandler(lockStatManager), 1, 1);
    lockStatManager.start(this.channel, lockStatisticsStage.getSink());

    final Stage syncWriteBatchRecvdHandler = stageManager
        .createStage(ClientConfigurationContext.RECEIVED_SYNC_WRITE_TRANSACTION_ACK_STAGE,
                     new ReceiveSyncWriteTransactionAckHandler(this.rtxManager), this.channel.getGroupIDs().length,
                     maxSize);

    final Stage jmxRemoteTunnelStage = stageManager.createStage(ClientConfigurationContext.JMXREMOTE_TUNNEL_STAGE, teh,
                                                                1, maxSize);
    final Stage receiveInvalidationStage = stageManager
        .createStage(ClientConfigurationContext.RECEIVE_INVALIDATE_OBJECTS_STAGE,
                     new ReceiveInvalidationHandler(remoteServerMapManager), 1, TCPropertiesImpl.getProperties()
                         .getInt(TCPropertiesConsts.L2_LOCAL_CACHE_INVALIDATIONS_SINK_CAPACITY));

    final List<ClientHandshakeCallback> clientHandshakeCallbacks = new ArrayList<ClientHandshakeCallback>();
    clientHandshakeCallbacks.add(this.lockManager);
    clientHandshakeCallbacks.add(this.objectManager);
    clientHandshakeCallbacks.add(remoteObjectManager);
    clientHandshakeCallbacks.add(remoteServerMapManager);
    clientHandshakeCallbacks.add(searchRequestManager);
    clientHandshakeCallbacks.add(this.rtxManager);
    clientHandshakeCallbacks.add(this.dsoClientBuilder.getObjectIDClientHandshakeRequester(batchSequenceReceiver));
    clientHandshakeCallbacks.add(this.clusterMetaDataManager);
    clientHandshakeCallbacks.add(teh);
    final ProductInfo pInfo = ProductInfo.getInstance();
    this.clientHandshakeManager = this.dsoClientBuilder
        .createClientHandshakeManager(new ClientIDLogger(this.channel.getClientIDProvider(), TCLogging
                                          .getLogger(ClientHandshakeManagerImpl.class)), this.channel, this.channel
                                          .getClientHandshakeMessageFactory(), pauseStage.getSink(), sessionManager,
                                      dsoCluster, pInfo.version(), Collections
                                          .unmodifiableCollection(clientHandshakeCallbacks));
    this.channel.addListener(this.clientHandshakeManager);

    final List<ReconnectionRejectedCallback> reconnectionRejectedCallbacks = new ArrayList<ReconnectionRejectedCallback>();
    reconnectionRejectedCallbacks.add(tcMemManager);

    if (clientMode.isExpressRejoinClient()) {
      reconnectionRejectedListener = new ReconnectionRejectedListenerImpl(dsoCluster, clientHandshakeManager,
                                                                          reconnectionRejectedCallbacks);
      this.channel.addListener(reconnectionRejectedListener);
    }

    final ClientConfigurationContext cc = new ClientConfigurationContext(stageManager, this.lockManager,
                                                                         remoteObjectManager, this.txManager,
                                                                         this.clientHandshakeManager,
                                                                         this.clusterMetaDataManager);
    stageManager.startAll(cc, Collections.EMPTY_LIST);

    initChannelMessageRouter(messageRouter, hydrateStage, lockResponse, lockStatisticsEnableDisableStage,
                             receiveRootID, receiveObject, receiveTransaction, oidRequestResponse, transactionResponse,
                             batchTxnAckStage, pauseStage, jmxRemoteTunnelStage, clusterMembershipEventStage,
                             clusterMetaDataStage, syncWriteBatchRecvdHandler, receiveServerMapStage,
                             receiveServerMapEvictionBroadcastStage, receiveSearchQueryStage, receiveInvalidationStage);

    openChannelOrExit(serverHost, serverPort, maxConnectRetries);
    waitForHandshake();

    // register for memory events for operator console register for it after the handshake happens see MNK-1684
    this.tcMemManager.registerForMemoryEvents(new MemoryOperatorEventListener(cacheConfig.getUsedCriticalThreshold()));

    if (this.statisticsAgentSubSystem.isActive()) {
      this.statisticsAgentSubSystem.setDefaultAgentDifferentiator(DEFAULT_AGENT_DIFFERENTIATOR_PREFIX
                                                                  + this.channel.channel().getChannelID().toLong());
    }

    setLoggerOnExit();
  }

  private void openChannelOrExit(final String serverHost, final int serverPort, final int maxConnectRetries) {
    int i = 0;
    while (maxConnectRetries <= 0 || i < maxConnectRetries) {
      try {
        DSO_LOGGER.debug("Trying to open channel....");
        this.channel.open();
        DSO_LOGGER.debug("Channel open");
        break;
      } catch (final TCTimeoutException tcte) {
        CONSOLE_LOGGER.warn("Timeout connecting to server: " + tcte.getMessage());
        ThreadUtil.reallySleep(5000);
      } catch (final ConnectException e) {
        CONSOLE_LOGGER.warn("Connection refused from server: " + e);
        ThreadUtil.reallySleep(5000);
      } catch (final MaxConnectionsExceededException e) {
        DSO_LOGGER.fatal(e.getMessage());
        CONSOLE_LOGGER.fatal(e.getMessage());
        CONSOLE_LOGGER.fatal(LicenseManager.EXIT_MESSAGE);
        System.exit(1);
      } catch (final CommStackMismatchException e) {
        DSO_LOGGER.fatal(e.getMessage());
        CONSOLE_LOGGER.fatal(e.getMessage());
        System.exit(1);
      } catch (final IOException ioe) {
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

  }

  private void waitForHandshake() {
    this.clientHandshakeManager.waitForHandshake();
    final TCSocketAddress remoteAddress = this.channel.channel().getRemoteAddress();
    final String infoMsg = "Connection successfully established to server at " + remoteAddress;
    CONSOLE_LOGGER.info(infoMsg);
    DSO_LOGGER.info(infoMsg);
  }

  private Map<TCMessageType, GeneratedMessageFactory> getMessageTypeFactoryMApping(final DNAEncoding encoding) {
    final Map<TCMessageType, GeneratedMessageFactory> messageTypeFactoryMapping = new HashMap<TCMessageType, GeneratedMessageFactory>();

    messageTypeFactoryMapping.put(TCMessageType.GET_VALUE_SERVER_MAP_RESPONSE_MESSAGE,
    // Special handling to get the applicator encoding
                                  new GeneratedMessageFactory() {

                                    public TCMessage createMessage(final SessionID sid, final MessageMonitor monitor,
                                                                   final MessageChannel mChannel,
                                                                   final TCMessageHeader msgHeader,
                                                                   final TCByteBuffer[] data) {
                                      return new GetValueServerMapResponseMessageImpl(sid, monitor, mChannel,
                                                                                      msgHeader, data, encoding);
                                    }

                                    public TCMessage createMessage(final SessionID sid, final MessageMonitor monitor,
                                                                   final TCByteBufferOutputStream output,
                                                                   final MessageChannel mChannel,
                                                                   final TCMessageType type) {
                                      throw new AssertionError(
                                                               GetValueServerMapRequestMessageImpl.class.getName()
                                                                   + " shouldn't be created using this constructor at the client.");
                                    }
                                  });
    messageTypeFactoryMapping.put(TCMessageType.GET_ALL_KEYS_SERVER_MAP_RESPONSE_MESSAGE,
    // Special handling to get the applicator encoding
                                  new GeneratedMessageFactory() {

                                    public TCMessage createMessage(final SessionID sid, final MessageMonitor monitor,
                                                                   final MessageChannel mChannel,
                                                                   final TCMessageHeader msgHeader,
                                                                   final TCByteBuffer[] data) {
                                      return new GetAllKeysServerMapResponseMessageImpl(sid, monitor, mChannel,
                                                                                        msgHeader, data, encoding);
                                    }

                                    public TCMessage createMessage(final SessionID sid, final MessageMonitor monitor,
                                                                   final TCByteBufferOutputStream output,
                                                                   final MessageChannel mChannel,
                                                                   final TCMessageType type) {
                                      throw new AssertionError(
                                                               GetAllKeysServerMapRequestMessageImpl.class.getName()
                                                                   + " shouldn't be created using this constructor at the client.");
                                    }
                                  });
    messageTypeFactoryMapping.put(TCMessageType.EVICTION_SERVER_MAP_BROADCAST_MESSAGE,
    // Special handling to get the applicator encoding
                                  new GeneratedMessageFactory() {

                                    public TCMessage createMessage(final SessionID sid, final MessageMonitor monitor,
                                                                   final MessageChannel mChannel,
                                                                   final TCMessageHeader msgHeader,
                                                                   final TCByteBuffer[] data) {
                                      return new ServerMapEvictionBroadcastMessageImpl(sid, monitor, mChannel,
                                                                                       msgHeader, data, encoding);
                                    }

                                    public TCMessage createMessage(final SessionID sid, final MessageMonitor monitor,
                                                                   final TCByteBufferOutputStream output,
                                                                   final MessageChannel mChannel,
                                                                   final TCMessageType type) {
                                      throw new AssertionError(
                                                               ServerMapEvictionBroadcastMessageImpl.class.getName()
                                                                   + " shouldn't be created using this constructor at the client.");
                                    }
                                  });

    return messageTypeFactoryMapping;
  }

  private Map<TCMessageType, Class> getMessageTypeClassMapping() {
    final Map<TCMessageType, Class> messageTypeClassMapping = new HashMap<TCMessageType, Class>();

    messageTypeClassMapping.put(TCMessageType.BATCH_TRANSACTION_ACK_MESSAGE,
                                BatchTransactionAcknowledgeMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.REQUEST_ROOT_MESSAGE, RequestRootMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.LOCK_REQUEST_MESSAGE, LockRequestMessage.class);
    messageTypeClassMapping.put(TCMessageType.LOCK_RESPONSE_MESSAGE, LockResponseMessage.class);
    messageTypeClassMapping.put(TCMessageType.LOCK_RECALL_MESSAGE, LockResponseMessage.class);
    messageTypeClassMapping.put(TCMessageType.LOCK_QUERY_RESPONSE_MESSAGE, LockResponseMessage.class);
    messageTypeClassMapping.put(TCMessageType.LOCK_STAT_MESSAGE, LockStatisticsMessage.class);
    messageTypeClassMapping
        .put(TCMessageType.LOCK_STATISTICS_RESPONSE_MESSAGE, LockStatisticsResponseMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.COMMIT_TRANSACTION_MESSAGE, CommitTransactionMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.REQUEST_ROOT_RESPONSE_MESSAGE, RequestRootResponseMessage.class);
    messageTypeClassMapping.put(TCMessageType.REQUEST_MANAGED_OBJECT_MESSAGE, RequestManagedObjectMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE,
                                RequestManagedObjectResponseMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.OBJECTS_NOT_FOUND_RESPONSE_MESSAGE, ObjectsNotFoundMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.BROADCAST_TRANSACTION_MESSAGE, BroadcastTransactionMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.OBJECT_ID_BATCH_REQUEST_MESSAGE, ObjectIDBatchRequestMessage.class);
    messageTypeClassMapping.put(TCMessageType.OBJECT_ID_BATCH_REQUEST_RESPONSE_MESSAGE,
                                ObjectIDBatchRequestResponseMessage.class);
    messageTypeClassMapping.put(TCMessageType.ACKNOWLEDGE_TRANSACTION_MESSAGE, AcknowledgeTransactionMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.CLIENT_HANDSHAKE_MESSAGE, ClientHandshakeMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.CLIENT_HANDSHAKE_ACK_MESSAGE, ClientHandshakeAckMessageImpl.class);
    messageTypeClassMapping
        .put(TCMessageType.CLIENT_HANDSHAKE_REFUSED_MESSAGE, ClientHandshakeRefusedMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.JMX_MESSAGE, JMXMessage.class);
    messageTypeClassMapping.put(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE, JmxRemoteTunnelMessage.class);
    messageTypeClassMapping.put(TCMessageType.CLUSTER_MEMBERSHIP_EVENT_MESSAGE, ClusterMembershipMessage.class);
    messageTypeClassMapping.put(TCMessageType.CLIENT_JMX_READY_MESSAGE, L1JmxReady.class);
    messageTypeClassMapping.put(TCMessageType.COMPLETED_TRANSACTION_LOWWATERMARK_MESSAGE,
                                CompletedTransactionLowWaterMarkMessage.class);
    messageTypeClassMapping.put(TCMessageType.NODES_WITH_OBJECTS_MESSAGE, NodesWithObjectsMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.NODES_WITH_KEYS_MESSAGE, NodesWithKeysMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.NODES_WITH_KEYS_RESPONSE_MESSAGE, NodesWithKeysResponseMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.NODES_WITH_OBJECTS_RESPONSE_MESSAGE,
                                NodesWithObjectsResponseMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.KEYS_FOR_ORPHANED_VALUES_MESSAGE, KeysForOrphanedValuesMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.KEYS_FOR_ORPHANED_VALUES_RESPONSE_MESSAGE,
                                KeysForOrphanedValuesResponseMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.NODE_META_DATA_MESSAGE, NodeMetaDataMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.NODE_META_DATA_RESPONSE_MESSAGE, NodeMetaDataResponseMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.SYNC_WRITE_TRANSACTION_RECEIVED_MESSAGE,
                                SyncWriteTransactionReceivedMessage.class);
    messageTypeClassMapping.put(TCMessageType.GET_ALL_SIZE_SERVER_MAP_REQUEST_MESSAGE,
                                GetAllSizeServerMapRequestMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.GET_ALL_SIZE_SERVER_MAP_RESPONSE_MESSAGE,
                                GetAllSizeServerMapResponseMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.SEARCH_QUERY_REQUEST_MESSAGE, SearchQueryRequestMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.SEARCH_QUERY_RESPONSE_MESSAGE, SearchQueryResponseMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.GET_VALUE_SERVER_MAP_REQUEST_MESSAGE,
                                GetValueServerMapRequestMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.OBJECT_NOT_FOUND_SERVER_MAP_RESPONSE_MESSAGE,
                                ObjectNotFoundServerMapResponseMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.TUNNELED_DOMAINS_CHANGED_MESSAGE, TunneledDomainsChanged.class);
    messageTypeClassMapping.put(TCMessageType.INVALIDATE_OBJECTS_MESSAGE, InvalidateObjectsMessage.class);
    messageTypeClassMapping.put(TCMessageType.GET_ALL_KEYS_SERVER_MAP_REQUEST_MESSAGE,
                                GetAllKeysServerMapRequestMessageImpl.class);

    return messageTypeClassMapping;
  }

  private void initChannelMessageRouter(TCMessageRouter messageRouter, Stage hydrateStage, Stage lockResponse,
                                        Stage lockStatisticsEnableDisableStage, Stage receiveRootID,
                                        Stage receiveObject, Stage receiveTransaction, Stage oidRequestResponse,
                                        Stage transactionResponse, Stage batchTxnAckStage, Stage pauseStage,
                                        Stage jmxRemoteTunnelStage, Stage clusterMembershipEventStage,
                                        Stage clusterMetaDataStage, Stage syncWriteBatchRecvdHandler,
                                        Stage receiveServerMapStage, Stage receiveServerMapEvictionBroadcastStage,
                                        Stage receiveSearchQueryStage, Stage receiveInvalidationStage) {
    final Sink hydrateSink = hydrateStage.getSink();
    messageRouter.routeMessageType(TCMessageType.LOCK_RESPONSE_MESSAGE, lockResponse.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.LOCK_QUERY_RESPONSE_MESSAGE, lockResponse.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.LOCK_STAT_MESSAGE, lockStatisticsEnableDisableStage.getSink(),
                                   hydrateSink);
    messageRouter.routeMessageType(TCMessageType.LOCK_RECALL_MESSAGE, lockResponse.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.REQUEST_ROOT_RESPONSE_MESSAGE, receiveRootID.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE, receiveObject.getSink(),
                                   hydrateSink);
    messageRouter.routeMessageType(TCMessageType.OBJECTS_NOT_FOUND_RESPONSE_MESSAGE, receiveObject.getSink(),
                                   hydrateSink);
    messageRouter.routeMessageType(TCMessageType.BROADCAST_TRANSACTION_MESSAGE, receiveTransaction.getSink(),
                                   hydrateSink);
    messageRouter.routeMessageType(TCMessageType.OBJECT_ID_BATCH_REQUEST_RESPONSE_MESSAGE,
                                   oidRequestResponse.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.ACKNOWLEDGE_TRANSACTION_MESSAGE, transactionResponse.getSink(),
                                   hydrateSink);
    messageRouter
        .routeMessageType(TCMessageType.BATCH_TRANSACTION_ACK_MESSAGE, batchTxnAckStage.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.CLIENT_HANDSHAKE_ACK_MESSAGE, pauseStage.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.CLIENT_HANDSHAKE_REFUSED_MESSAGE, pauseStage.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE, jmxRemoteTunnelStage.getSink(),
                                   hydrateSink);
    messageRouter.routeMessageType(TCMessageType.CLUSTER_MEMBERSHIP_EVENT_MESSAGE,
                                   clusterMembershipEventStage.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.NODES_WITH_OBJECTS_RESPONSE_MESSAGE, clusterMetaDataStage.getSink(),
                                   hydrateSink);
    messageRouter.routeMessageType(TCMessageType.NODES_WITH_KEYS_RESPONSE_MESSAGE, clusterMetaDataStage.getSink(),
                                   hydrateSink);
    messageRouter.routeMessageType(TCMessageType.KEYS_FOR_ORPHANED_VALUES_RESPONSE_MESSAGE,
                                   clusterMetaDataStage.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.NODE_META_DATA_RESPONSE_MESSAGE, clusterMetaDataStage.getSink(),
                                   hydrateSink);
    messageRouter.routeMessageType(TCMessageType.SYNC_WRITE_TRANSACTION_RECEIVED_MESSAGE,
                                   syncWriteBatchRecvdHandler.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.GET_VALUE_SERVER_MAP_RESPONSE_MESSAGE,
                                   receiveServerMapStage.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.GET_ALL_SIZE_SERVER_MAP_RESPONSE_MESSAGE,
                                   receiveServerMapStage.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.GET_ALL_KEYS_SERVER_MAP_RESPONSE_MESSAGE,
                                   receiveServerMapStage.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.OBJECT_NOT_FOUND_SERVER_MAP_RESPONSE_MESSAGE,
                                   receiveServerMapStage.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.EVICTION_SERVER_MAP_BROADCAST_MESSAGE,
                                   receiveServerMapEvictionBroadcastStage.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.SEARCH_QUERY_RESPONSE_MESSAGE, receiveSearchQueryStage.getSink(),
                                   hydrateSink);
    messageRouter.routeMessageType(TCMessageType.INVALIDATE_OBJECTS_MESSAGE, receiveInvalidationStage.getSink(),
                                   hydrateSink);
    DSO_LOGGER.debug("Added message routing types.");
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

  public ClientLockManager getLockManager() {
    return this.lockManager;
  }

  public ClientObjectManager getObjectManager() {
    return this.objectManager;
  }

  public RemoteSearchRequestManager getSearchRequestManager() {
    return this.searchRequestManager;
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

  public L1Management getL1Management() {
    return this.l1Management;
  }

  public DmiManager getDmiManager() {
    return this.dmiManager;
  }

  public StatisticsAgentSubSystem getStatisticsAgentSubSystem() {
    return this.statisticsAgentSubSystem;
  }

  public TunneledDomainManager getTunneledDomainManager() {
    return this.tunneledDomainManager;
  }

  public void dump() {
    this.dumpHandler.dump();
  }

  public void startBeanShell(final int port) {
    try {
      final Interpreter i = new Interpreter();
      i.set("client", this);
      i.set("objectManager", this.objectManager);
      i.set("lockmanager", this.lockManager);
      i.set("txManager", this.txManager);
      i.set("portnum", port);
      i.eval("setAccessibility(true)"); // turn off access restrictions
      i.eval("server(portnum)");
      CONSOLE_LOGGER.info("Bean shell is started on port " + port);
    } catch (final EvalError e) {
      e.printStackTrace();
    }
  }

  public void reloadConfiguration() throws ConfigurationSetupException {
    if (false) { throw new ConfigurationSetupException(); }
    throw new UnsupportedOperationException();
  }

  public void addServerConfigurationChangedListeners(final ClusterTopologyChangedListener listener) {
    throw new UnsupportedOperationException();
  }

  protected DSOClientConfigHelper getClientConfigHelper() {
    return this.config;
  }

  public void shutdown() {
    final TCLogger logger = DSO_LOGGER;

    // This seems to help perm gen GC
    ClassInfoRepository.clear();
    AsmClassInfoRepository.clear();
    JavaClassInfoRepository.clear();

    TCTimerService.getInstance().shutdown();

    if (globalLocalCacheManager != null) {
      globalLocalCacheManager.shutdown();
    }

    if (this.counterManager != null) {
      try {
        this.counterManager.shutdown();
      } catch (final Throwable t) {
        logger.error("error shutting down counter manager", t);
      } finally {
        this.counterManager = null;
      }
    }

    if (this.l1Management != null) {
      try {
        this.l1Management.stop();
      } catch (final Throwable t) {
        logger.error("error shutting down JMX connector", t);
      } finally {
        this.l1Management = null;
      }
    }

    if (this.tcMemManager != null) {
      try {
        this.tcMemManager.shutdown();
      } catch (final Throwable t) {
        logger.error("Error stopping memory manager", t);
      } finally {
        this.tcMemManager = null;
      }
    }

    if (this.lockManager != null) {
      try {
        this.lockManager.shutdown();
      } catch (final Throwable t) {
        logger.error("Error stopping lock manager", t);
      } finally {
        this.lockManager = null;
      }
    }

    try {
      getStageManager().stopAll();
    } catch (final Throwable t) {
      logger.error("Error stopping stage manager", t);
    }

    if (this.objectManager != null) {
      try {
        this.objectManager.shutdown();
      } catch (final Throwable t) {
        logger.error("Error shutting down client object manager", t);
      } finally {
        this.objectManager = null;
      }
    }

    if (this.reconnectionRejectedListener != null) {
      this.reconnectionRejectedListener.shutDown();
    }

    if (this.channel != null) {
      try {
        this.channel.close();
      } catch (final Throwable t) {
        logger.error("Error closing channel", t);
      } finally {
        this.channel = null;
      }
    }

    if (this.communicationsManager != null) {
      try {
        this.communicationsManager.shutdown();
      } catch (final Throwable t) {
        logger.error("Error shutting down communications manager", t);
      } finally {
        this.communicationsManager = null;
      }
    }

    CommonShutDownHook.shutdown();
    this.dsoCluster.shutdown();

    if (this.threadGroup != null) {
      boolean interrupted = false;

      try {
        final long end = System.currentTimeMillis()
                         + TCPropertiesImpl.getProperties()
                             .getLong(TCPropertiesConsts.L1_SHUTDOWN_THREADGROUP_GRACETIME);

        while (this.threadGroup.activeCount() > 0 && System.currentTimeMillis() < end) {
          try {
            Thread.sleep(1000);
          } catch (final InterruptedException e) {
            interrupted = true;
          }
        }
        if (this.threadGroup.activeCount() > 0) {
          logger.warn("Timed out waiting for TC thread group threads to die - probable shutdown memory leak\n"
                      + "Live threads: " + getLiveThreads(this.threadGroup));

          Thread threadGroupCleanerThread = new Thread(this.threadGroup.getParent(),
                                                       new TCThreadGroupCleanerRunnable(threadGroup),
                                                       "TCThreadGroup last chance cleaner thread");
          threadGroupCleanerThread.setDaemon(true);
          threadGroupCleanerThread.start();
          logger.warn("Spawning TCThreadGroup last chance cleaner thread");
        } else {
          logger.info("Destroying TC thread group");
          this.threadGroup.destroy();
        }
      } catch (final Throwable t) {
        logger.error("Error destroying TC thread group", t);
      } finally {
        if (interrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }

    try {
      TCLogging.closeFileAppender();
      TCLogging.disableLocking();
    } catch (final Throwable t) {
      Logger.getAnonymousLogger().log(Level.WARNING, "Error shutting down TC logging system", t);
    }
  }

  private static List<Thread> getLiveThreads(final ThreadGroup group) {
    final int estimate = group.activeCount();

    Thread[] threads = new Thread[estimate + 1];

    while (true) {
      final int count = group.enumerate(threads);

      if (count < threads.length) {
        final List<Thread> l = new ArrayList<Thread>(count);
        for (final Thread t : threads) {
          if (t != null) {
            l.add(t);
          }
        }
        return l;
      } else {
        threads = new Thread[threads.length * 2];
      }
    }
  }

  public String[] processArguments() {
    return null;
  }

  private static class TCThreadGroupCleanerRunnable implements Runnable {
    private final TCThreadGroup threadGroup;

    public TCThreadGroupCleanerRunnable(TCThreadGroup threadGroup) {
      this.threadGroup = threadGroup;
    }

    public void run() {
      while (threadGroup.activeCount() > 0) {
        for (Thread liveThread : getLiveThreads(threadGroup)) {
          liveThread.interrupt();
        }
        try {
          Thread.sleep(1000);
        } catch (final InterruptedException e) {
          // ignore
        }
      }
      try {
        threadGroup.destroy();
      } catch (Exception e) {
        // the logger is closed by now so we can't even log that
      }
    }
  }
}
