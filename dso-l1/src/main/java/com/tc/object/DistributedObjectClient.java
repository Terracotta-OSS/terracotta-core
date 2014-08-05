/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import bsh.EvalError;
import bsh.Interpreter;

import com.tc.abortable.AbortableOperationManager;
import com.tc.async.api.PostInit;
import com.tc.async.api.SEDA;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.bytes.TCByteBuffer;
import com.tc.cluster.DsoCluster;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.exception.TCRuntimeException;
import com.tc.handler.CallbackDumpAdapter;
import com.tc.handler.CallbackDumpHandler;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.lang.TCThreadGroup;
import com.tc.license.LicenseManager;
import com.tc.license.ProductID;
import com.tc.logging.ClientIDLogger;
import com.tc.logging.ClientIDLoggerProvider;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.logging.ThreadDumpHandler;
import com.tc.management.L1Management;
import com.tc.management.ManagementServicesManager;
import com.tc.management.ManagementServicesManagerImpl;
import com.tc.management.TCClient;
import com.tc.management.remote.protocol.terracotta.JmxRemoteTunnelMessage;
import com.tc.management.remote.protocol.terracotta.L1JmxReady;
import com.tc.management.remote.protocol.terracotta.TunneledDomainManager;
import com.tc.management.remote.protocol.terracotta.TunneledDomainsChanged;
import com.tc.management.remote.protocol.terracotta.TunnelingEventHandler;
import com.tc.net.CommStackMismatchException;
import com.tc.net.GroupID;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ClusterTopologyChangedListener;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.core.security.TCSecurityManager;
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
import com.tc.net.protocol.transport.ReconnectionRejectedHandlerL1;
import com.tc.object.config.ConnectionInfoConfig;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.PreparedComponentsFromL2Connection;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAEncodingInternal;
import com.tc.object.gtx.ClientGlobalTransactionManager;
import com.tc.object.handler.BatchTransactionAckHandler;
import com.tc.object.handler.ClientCoordinationHandler;
import com.tc.object.handler.ClientManagementHandler;
import com.tc.object.handler.ClusterInternalEventsHandler;
import com.tc.object.handler.ClusterMemberShipEventsHandler;
import com.tc.object.handler.ClusterMetaDataHandler;
import com.tc.object.handler.LockRecallHandler;
import com.tc.object.handler.LockResponseHandler;
import com.tc.object.handler.ReceiveInvalidationHandler;
import com.tc.object.handler.ReceiveObjectHandler;
import com.tc.object.handler.ReceiveRootIDHandler;
import com.tc.object.handler.ReceiveSearchQueryResponseHandler;
import com.tc.object.handler.ReceiveServerMapResponseHandler;
import com.tc.object.handler.ReceiveSyncWriteTransactionAckHandler;
import com.tc.object.handler.ReceiveTransactionCompleteHandler;
import com.tc.object.handler.ReceiveTransactionHandler;
import com.tc.object.handler.ResourceManagerMessageHandler;
import com.tc.object.handler.ServerEventDeliveryHandler;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.handshakemanager.ClientHandshakeManagerImpl;
import com.tc.object.idprovider.api.ObjectIDProvider;
import com.tc.object.idprovider.impl.RemoteObjectIDBatchSequenceProvider;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.locks.ClientLockManager;
import com.tc.object.locks.ClientLockManagerConfigImpl;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.locks.LockIdFactory;
import com.tc.object.locks.LocksRecallService;
import com.tc.object.locks.LocksRecallServiceImpl;
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
import com.tc.object.msg.InvokeRegisteredServiceMessage;
import com.tc.object.msg.InvokeRegisteredServiceResponseMessage;
import com.tc.object.msg.KeysForOrphanedValuesMessageImpl;
import com.tc.object.msg.KeysForOrphanedValuesResponseMessageImpl;
import com.tc.object.msg.ListRegisteredServicesMessage;
import com.tc.object.msg.ListRegisteredServicesResponseMessage;
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
import com.tc.object.msg.ResourceManagerThrottleMessage;
import com.tc.object.msg.SearchQueryRequestMessageImpl;
import com.tc.object.msg.SearchQueryResponseMessageImpl;
import com.tc.object.msg.SearchResultsRequestMessageImpl;
import com.tc.object.msg.SearchResultsResponseMessageImpl;
import com.tc.object.msg.SyncWriteTransactionReceivedMessage;
import com.tc.object.net.DSOClientMessageChannel;
import com.tc.object.search.SearchResultManager;
import com.tc.object.search.SearchResultReplyHandler;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;
import com.tc.object.servermap.localcache.impl.L1ServerMapCapacityEvictionHandler;
import com.tc.object.servermap.localcache.impl.L1ServerMapLocalCacheManagerImpl;
import com.tc.object.servermap.localcache.impl.L1ServerMapTransactionCompletionHandler;
import com.tc.object.servermap.localcache.impl.PinnedEntryFaultHandler;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.object.session.SessionManagerImpl;
import com.tc.object.session.SessionProvider;
import com.tc.object.tx.ClientTransactionFactory;
import com.tc.object.tx.ClientTransactionFactoryImpl;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.object.tx.ClientTransactionManagerImpl;
import com.tc.object.tx.FoldingConfigHelper;
import com.tc.object.tx.RemoteTransactionManager;
import com.tc.object.tx.TransactionIDGenerator;
import com.tc.operatorevent.TerracottaOperatorEventLogging;
import com.tc.platform.PlatformService;
import com.tc.platform.PlatformServiceImpl;
import com.tc.platform.rejoin.ClientChannelEventController;
import com.tc.platform.rejoin.RejoinAwarePlatformService;
import com.tc.platform.rejoin.RejoinManagerInternal;
import com.tc.properties.ReconnectConfig;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.runtime.TCMemoryManagerImpl;
import com.tc.runtime.logging.LongGCLogger;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.stats.counter.sampled.derived.SampledRateCounterConfig;
import com.tc.util.Assert;
import com.tc.util.CommonShutDownHook;
import com.tc.util.ProductInfo;
import com.tc.util.TCTimeoutException;
import com.tc.util.UUID;
import com.tc.util.concurrent.Runners;
import com.tc.util.concurrent.SetOnceFlag;
import com.tc.util.concurrent.TaskRunner;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  private static final int                           MAX_CONNECT_TRIES                   = -1;

  private static final String                        L1VMShutdownHookName                = "L1 VM Shutdown Hook";
  private final DSOClientBuilder                     dsoClientBuilder;
  private final DSOClientConfigHelper                config;
  private final ClassProvider                        classProvider;
  private final DsoClusterInternal                   dsoCluster;
  private final TCThreadGroup                        threadGroup;
  private final ThreadIDMap                          threadIDMap;

  protected final PreparedComponentsFromL2Connection connectionComponents;
  private final ProductID                            productId;

  private DSOClientMessageChannel                    channel;
  private ClientLockManager                          lockManager;
  private ClientObjectManagerImpl                    objectManager;
  private RemoteSearchRequestManager                 searchRequestManager;
  private ClientTransactionManagerImpl               clientTxnManager;
  private CommunicationsManager                      communicationsManager;
  private RemoteTransactionManager                   remoteTxnManager;
  private ClientHandshakeManager                     clientHandshakeManager;
  private ClusterMetaDataManager                     clusterMetaDataManager;
  private L1Management                               l1Management;
  private TCProperties                               l1Properties;
  private boolean                                    createDedicatedMBeanServer          = false;
  private CounterManager                             counterManager;
  private ThreadIDManager                            threadIDManager;
  private final CallbackDumpHandler                  dumpHandler                         = new CallbackDumpHandler();
  private TunneledDomainManager                      tunneledDomainManager;
  private TCMemoryManagerImpl                        tcMemManager;
  private ClientChannelEventController               clientChannelEventController;
  private RemoteResourceManager                      remoteResourceManager;
  private ServerEventListenerManager                 serverEventListenerManager;
  private ManagementServicesManager                  managementServicesManager;

  private Stage                                      clusterEventsStage;

  private L1ServerMapLocalCacheManager               globalLocalCacheManager;
  private final TCSecurityManager                    securityManager;

  private final AbortableOperationManager            abortableOperationManager;

  private final RejoinManagerInternal                rejoinManager;

  private final UUID                                 uuid;

  private final TaskRunner                           taskRunner;

  private PlatformService                            platformService;

  private ClientShutdownManager                      shutdownManager;

  private final Thread                               shutdownAction;

  private final SetOnceFlag                          clientStopped                       = new SetOnceFlag();

  
  public DistributedObjectClient(final DSOClientConfigHelper config, final TCThreadGroup threadGroup,
                                 final ClassProvider classProvider,
                                 final PreparedComponentsFromL2Connection connectionComponents,
                                 final DsoClusterInternal dsoCluster,
                                 final AbortableOperationManager abortableOperationManager,
                                 final RejoinManagerInternal rejoinManager) {
    this(config, threadGroup, classProvider, connectionComponents, dsoCluster, null, abortableOperationManager,
         rejoinManager, UUID.NULL_ID, null);
  }

  public DistributedObjectClient(final DSOClientConfigHelper config, final TCThreadGroup threadGroup,
                                 final ClassProvider classProvider,
                                 final PreparedComponentsFromL2Connection connectionComponents,
                                 final DsoClusterInternal dsoCluster, final TCSecurityManager securityManager,
                                 final AbortableOperationManager abortableOperationManager,
                                 final RejoinManagerInternal rejoinManager, UUID uuid, final ProductID productId) {
    super(threadGroup);
    this.productId = productId;
    Assert.assertNotNull(config);
    this.abortableOperationManager = abortableOperationManager;
    this.config = config;
    this.securityManager = securityManager;
    this.classProvider = classProvider;
    this.connectionComponents = connectionComponents;
    this.dsoCluster = dsoCluster;
    this.threadGroup = threadGroup;
    this.threadIDMap = new ThreadIDMapImpl();
    this.dsoClientBuilder = createClientBuilder();
    this.rejoinManager = rejoinManager;
    this.uuid = uuid;
    this.taskRunner = Runners.newDefaultCachedScheduledTaskRunner(threadGroup);
    this.shutdownAction = new Thread(new ShutdownAction(), L1VMShutdownHookName);
    Runtime.getRuntime().addShutdownHook(this.shutdownAction);
  }

  protected DSOClientBuilder createClientBuilder() {
    if (this.connectionComponents.isActiveActive()) {
      String msg = "An attempt to start a Terracotta server array with more than one active server failed. "
                   + "This feature is not available in the currently installed Terracotta platform. For more information on "
                   + "supported features for Terracotta platforms, please see this link http://www.terracotta.org/sadne";
      CONSOLE_LOGGER.fatal(msg);
      throw new IllegalStateException(msg);
    }
    return new StandardDSOClientBuilder();
  }

  @Override
  public ThreadIDMap getThreadIDMap() {
    return this.threadIDMap;
  }

  @Override
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

  private void validateSecurityConfig() {
    if (config.getSecurityInfo().isSecure() && securityManager == null) { throw new TCRuntimeException(
                                                                                                       "client configured as secure but was constructed without securityManager"); }
    if (!config.getSecurityInfo().isSecure() && securityManager != null) { throw new TCRuntimeException(
                                                                                                        "client not configured as secure but was constructed with securityManager"); }
  }

  private void validateGroupConfig() {
    final boolean toCheckTopology = TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.L1_L2_CONFIG_VALIDATION_ENABLED);
    if (toCheckTopology) {
      try {
        this.config.validateGroupInfo(securityManager);
      } catch (final ConfigurationSetupException e) {
        CONSOLE_LOGGER.error(e.getMessage());
        throw new IllegalStateException(e.getMessage(), e);
      }
    }
  }

  private ReconnectConfig getReconnectPropertiesFromServer() {
    ReconnectConfig reconnectConfig = null;
    try {
      reconnectConfig = this.config.getL1ReconnectProperties(securityManager);
    } catch (ConfigurationSetupException e) {
      CONSOLE_LOGGER.error(e.getMessage());
      throw new IllegalStateException(e.getMessage(), e);
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

  private void validateClientServerCompatibility() {
    try {
      this.config.validateClientServerCompatibility(securityManager, config.getSecurityInfo());
    } catch (final ConfigurationSetupException e) {
      CONSOLE_LOGGER.error(e.getMessage());
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  public synchronized void start() {
    rejoinManager.start();
    validateSecurityConfig();
    validateGroupConfig();

    final TCProperties tcProperties = TCPropertiesImpl.getProperties();
    final boolean checkClientServerVersions = tcProperties.getBoolean(TCPropertiesConsts.VERSION_COMPATIBILITY_CHECK);
    if (checkClientServerVersions) {
      validateClientServerCompatibility();
    }
    this.l1Properties = tcProperties.getPropertiesFor("l1");
    final int maxSize = tcProperties.getInt(TCPropertiesConsts.L1_SEDA_STAGE_SINK_CAPACITY);
    final int faultCount = this.config.getFaultCount();

    final SessionManager sessionManager = new SessionManagerImpl(new SessionManagerImpl.SequenceFactory() {
      @Override
      public Sequence newSequence() {
        return new SimpleSequence();
      }
    });
    final SessionProvider sessionProvider = sessionManager;

    this.threadGroup.addCallbackOnExitDefaultHandler(new ThreadDumpHandler(this));
    final StageManager stageManager = getStageManager();
    this.dumpHandler.registerForDump(new CallbackDumpAdapter(stageManager));

    final ReconnectConfig l1ReconnectConfig = getReconnectPropertiesFromServer();

    final boolean useOOOLayer = l1ReconnectConfig.getReconnectEnabled();
    final NetworkStackHarnessFactory networkStackHarnessFactory = getNetworkStackHarnessFactory(useOOOLayer,
                                                                                                l1ReconnectConfig);

    this.counterManager = new CounterManagerImpl();
    final MessageMonitor mm = MessageMonitorImpl.createMonitor(tcProperties, DSO_LOGGER);
    final DNAEncodingInternal encoding = new ApplicatorDNAEncodingImpl(this.classProvider);
    final TCMessageRouter messageRouter = new TCMessageRouterImpl();

    this.communicationsManager = this.dsoClientBuilder
        .createCommunicationsManager(mm,
                                     messageRouter,
                                     networkStackHarnessFactory,
                                     new NullConnectionPolicy(),
                                     this.connectionComponents.createConnectionInfoConfigItemByGroup().length,
                                     new HealthCheckerConfigClientImpl(this.l1Properties
                                         .getPropertiesFor("healthcheck.l2"), "DSO Client"),
                                     getMessageTypeClassMapping(), getMessageTypeFactoryMapping(encoding),
                                     ReconnectionRejectedHandlerL1.SINGLETON, securityManager, productId);

    DSO_LOGGER.debug("Created CommunicationsManager.");

    final ConnectionInfoConfig[] connectionInfoItems = this.connectionComponents
        .createConnectionInfoConfigItemByGroup();
    final ConnectionInfo[] connectionInfo = connectionInfoItems[0].getConnectionInfos();
    final String serverHost = connectionInfo[0].getHostname();
    final int serverPort = connectionInfo[0].getPort();

    clusterEventsStage = stageManager.createStage(ClientConfigurationContext.CLUSTER_EVENTS_STAGE,
                                                  new ClusterInternalEventsHandler(dsoCluster), 1, maxSize);

    final int socketConnectTimeout = tcProperties.getInt(TCPropertiesConsts.L1_SOCKET_CONNECT_TIMEOUT);

    if (socketConnectTimeout < 0) { throw new IllegalArgumentException("invalid socket time value: "
                                                                       + socketConnectTimeout); }
    this.channel = this.dsoClientBuilder.createDSOClientMessageChannel(this.communicationsManager,
                                                                       this.connectionComponents, sessionProvider,
                                                                       MAX_CONNECT_TRIES, socketConnectTimeout, this);

    final ClientIDLoggerProvider cidLoggerProvider = new ClientIDLoggerProvider(this.channel.getClientIDProvider());
    stageManager.setLoggerProvider(cidLoggerProvider);

    DSO_LOGGER.debug("Created channel.");

    TerracottaOperatorEventLogging.setNodeNameProvider(new ClientNameProvider(this.dsoCluster));

    final ClientTransactionFactory txFactory = new ClientTransactionFactoryImpl();

    final SampledRateCounterConfig sampledRateCounterConfig = new SampledRateCounterConfig(1, 300, true);
    final SampledRateCounter transactionSizeCounter = (SampledRateCounter) this.counterManager
        .createCounter(sampledRateCounterConfig);
    final SampledRateCounter transactionsPerBatchCounter = (SampledRateCounter) this.counterManager
        .createCounter(sampledRateCounterConfig);

    this.remoteTxnManager = this.dsoClientBuilder
        .createRemoteTransactionManager(this.channel.getClientIDProvider(), encoding,
                                        FoldingConfigHelper.createFromProperties(tcProperties),
                                        new TransactionIDGenerator(), sessionManager, this.channel,
                                        transactionSizeCounter, transactionsPerBatchCounter, abortableOperationManager,
                                        taskRunner);

    this.dumpHandler.registerForDump(new CallbackDumpAdapter(this.remoteTxnManager));
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

    // for SRA L1 Tx count
    final SampledCounterConfig sampledCounterConfig = new SampledCounterConfig(1, 300, true, 0L);
    final SampledCounter txnCounter = (SampledCounter) this.counterManager.createCounter(sampledCounterConfig);

    final RemoteObjectManager remoteObjectManager = this.dsoClientBuilder
        .createRemoteObjectManager(new ClientIDLogger(this.channel.getClientIDProvider(), TCLogging
                                       .getLogger(RemoteObjectManager.class)), this.channel, faultCount,
                                   sessionManager, abortableOperationManager, this.taskRunner);
    this.dumpHandler.registerForDump(new CallbackDumpAdapter(remoteObjectManager));

    LockRecallHandler lockRecallHandler = new LockRecallHandler();
    final Stage lockRecallStage = stageManager.createStage(ClientConfigurationContext.LOCK_RECALL_STAGE,
                                                           lockRecallHandler, 8, maxSize);
    LocksRecallService locksRecallHelper = new LocksRecallServiceImpl(lockRecallHandler, lockRecallStage);

    SearchResultManager searchResultMgr = this.dsoClientBuilder
        .createSearchResultManager(new ClientIDLogger(this.channel.getClientIDProvider(), TCLogging
                                       .getLogger(SearchResultManager.class)), this.channel, sessionManager,
                                   abortableOperationManager);
    searchRequestManager = this.dsoClientBuilder
        .createRemoteSearchRequestManager(new ClientIDLogger(this.channel.getClientIDProvider(), TCLogging
                                              .getLogger(RemoteSearchRequestManager.class)), this.channel,
                                          sessionManager, searchResultMgr, abortableOperationManager);

    final L1ServerMapCapacityEvictionHandler l1ServerMapCapacityEvictionHandler = new L1ServerMapCapacityEvictionHandler();
    final Stage capacityEvictionStage = stageManager.createStage(ClientConfigurationContext.CAPACITY_EVICTION_STAGE,
                                                                 l1ServerMapCapacityEvictionHandler, 8, maxSize);

    L1ServerMapTransactionCompletionHandler completionHandler = new L1ServerMapTransactionCompletionHandler();
    final Stage txnCompleteStage = stageManager
        .createStage(ClientConfigurationContext.LOCAL_CACHE_TXN_COMPLETE_STAGE, completionHandler, TCPropertiesImpl
            .getProperties().getInt(TCPropertiesConsts.L2_LOCAL_CACHE_TXN_COMPLETE_THREADS), TCPropertiesImpl
            .getProperties().getInt(TCPropertiesConsts.L2_LOCAL_CACHE_TXN_COMPLETE_SINK_CAPACITY));

    int pinnedEntryFaultStageThreads = l1Properties.getInt(TCPropertiesConsts.L1_SEDA_PINNED_ENTRY_FAULT_STAGE_THREADS,
                                                           8);
    final Stage pinnedEntryFaultStage = stageManager.createStage(ClientConfigurationContext.PINNED_ENTRY_FAULT_STAGE,
                                                                 new PinnedEntryFaultHandler(),
                                                                 pinnedEntryFaultStageThreads, maxSize);

    globalLocalCacheManager = new L1ServerMapLocalCacheManagerImpl(locksRecallHelper, capacityEvictionStage.getSink(),
                                                                   txnCompleteStage.getSink(),
                                                                   pinnedEntryFaultStage.getSink());
    l1ServerMapCapacityEvictionHandler.initialize(globalLocalCacheManager);

    final RemoteServerMapManager remoteServerMapManager = this.dsoClientBuilder
        .createRemoteServerMapManager(new ClientIDLogger(this.channel.getClientIDProvider(), TCLogging
                                          .getLogger(RemoteServerMapManager.class)), remoteObjectManager, this.channel,
                                      sessionManager, globalLocalCacheManager, abortableOperationManager,
                                      this.taskRunner);
    final CallbackDumpAdapter remoteServerMgrDumpAdapter = new CallbackDumpAdapter(remoteServerMapManager);
    this.threadGroup.addCallbackOnExitDefaultHandler(remoteServerMgrDumpAdapter);
    this.dumpHandler.registerForDump(remoteServerMgrDumpAdapter);

    final ClientGlobalTransactionManager gtxManager = this.dsoClientBuilder
        .createClientGlobalTransactionManager(this.remoteTxnManager);

    final TCClassFactory classFactory = this.dsoClientBuilder.createTCClassFactory(this.config, this.classProvider,
                                                                                   encoding, globalLocalCacheManager,
                                                                                   remoteServerMapManager);
    final TCObjectFactory objectFactory = new TCObjectFactoryImpl(classFactory);

    this.objectManager = this.dsoClientBuilder.createObjectManager(remoteObjectManager, idProvider,
                                                                   this.channel.getClientIDProvider(),
                                                                   this.classProvider, classFactory, objectFactory,
                                                                   this.config.getPortability(),
                                                                   globalLocalCacheManager, abortableOperationManager);
    this.globalLocalCacheManager.initializeTCObjectSelfStore(objectManager);

    this.threadGroup.addCallbackOnExitDefaultHandler(new CallbackDumpAdapter(this.objectManager));
    this.dumpHandler.registerForDump(new CallbackDumpAdapter(this.objectManager));

    this.tcMemManager = new TCMemoryManagerImpl(getThreadGroup());
    final long timeOut = TCPropertiesImpl.getProperties().getLong(TCPropertiesConsts.LOGGING_LONG_GC_THRESHOLD);
    final LongGCLogger gcLogger = this.dsoClientBuilder.createLongGCLogger(timeOut);
    this.tcMemManager.registerForMemoryEvents(gcLogger);
    // CDV-1181 warn if using CMS
    this.tcMemManager.checkGarbageCollectors();

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
                                                                                        this.config, uuid);
    this.tunneledDomainManager = this.dsoClientBuilder.createTunneledDomainManager(this.channel.channel(), this.config,
                                                                                   teh);

    this.l1Management = this.dsoClientBuilder.createL1Management(teh, this.config.rawConfigText(), this);
    this.l1Management.start(createDedicatedMBeanServer);

    // register the terracotta operator event logger
    this.dsoClientBuilder.registerForOperatorEvents(this.l1Management);

    // Setup the lock manager
    this.lockManager = this.dsoClientBuilder
        .createLockManager(this.channel,
                           new ClientIDLogger(this.channel.getClientIDProvider(), TCLogging
                               .getLogger(ClientLockManager.class)), sessionManager, this.channel
                               .getLockRequestMessageFactory(), this.threadIDManager, gtxManager,
                           new ClientLockManagerConfigImpl(this.l1Properties.getPropertiesFor("lockmanager")),
                           abortableOperationManager, this.taskRunner);
    final CallbackDumpAdapter lockDumpAdapter = new CallbackDumpAdapter(this.lockManager);
    this.threadGroup.addCallbackOnExitDefaultHandler(lockDumpAdapter);
    this.dumpHandler.registerForDump(lockDumpAdapter);

    // Setup the transaction manager
    this.clientTxnManager = new ClientTransactionManagerImpl(this.channel.getClientIDProvider(), this.objectManager,
                                                             txFactory, this.lockManager, this.remoteTxnManager,
                                                             txnCounter, globalLocalCacheManager,
                                                             abortableOperationManager);

    final CallbackDumpAdapter txnMgrDumpAdapter = new CallbackDumpAdapter(this.clientTxnManager);
    this.threadGroup.addCallbackOnExitDefaultHandler(txnMgrDumpAdapter);
    this.dumpHandler.registerForDump(txnMgrDumpAdapter);

    // Setup Remote Resource Manager
    remoteResourceManager = dsoClientBuilder.createRemoteResourceManager(remoteTxnManager, channel,
                                                                         abortableOperationManager);
    final Stage resourceManagerStage = stageManager
        .createStage(ClientConfigurationContext.RESOURCE_MANAGER_STAGE,
                     new ResourceManagerMessageHandler(remoteResourceManager), 1, maxSize);

    // Create the SEDA stages
    final Stage lockResponse = stageManager.createStage(ClientConfigurationContext.LOCK_RESPONSE_STAGE,
                                                        new LockResponseHandler(sessionManager),
                                                        this.channel.getGroupIDs().length, 1, maxSize);
    final Stage receiveRootID = stageManager.createStage(ClientConfigurationContext.RECEIVE_ROOT_ID_STAGE,
                                                         new ReceiveRootIDHandler(), 1, maxSize);
    final Stage receiveObject = stageManager.createStage(ClientConfigurationContext.RECEIVE_OBJECT_STAGE,
                                                         new ReceiveObjectHandler(), 1, maxSize);

    serverEventListenerManager = dsoClientBuilder.createServerEventListenerManager(channel);

    final Stage serverEventDeliveryStage = createServerEventDeliveryStage(stageManager);

    final Stage receiveTransaction = stageManager
        .createStage(ClientConfigurationContext.RECEIVE_TRANSACTION_STAGE,
                     new ReceiveTransactionHandler(this.channel.getAcknowledgeTransactionMessageFactory(), gtxManager,
                                                   sessionManager, serverEventDeliveryStage.getSink()), 1, maxSize);
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

    final Stage receiveSearchResultStage = stageManager
        .createStage(ClientConfigurationContext.RECEIVE_SEARCH_RESULT_RESPONSE_STAGE,
                     new SearchResultReplyHandler(searchResultMgr), 1, maxSize);

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

    final Stage syncWriteBatchRecvdHandler = stageManager
        .createStage(ClientConfigurationContext.RECEIVED_SYNC_WRITE_TRANSACTION_ACK_STAGE,
                     new ReceiveSyncWriteTransactionAckHandler(this.remoteTxnManager),
                     this.channel.getGroupIDs().length, maxSize);

    final Stage jmxRemoteTunnelStage = stageManager.createStage(ClientConfigurationContext.JMXREMOTE_TUNNEL_STAGE, teh,
                                                                1, maxSize);

    this.managementServicesManager = new ManagementServicesManagerImpl(Collections.<MessageChannel> singleton(channel
        .channel()), channel.getClientIDProvider());

    final Stage managementStage = stageManager.createStage(ClientConfigurationContext.MANAGEMENT_STAGE,
                                                           new ClientManagementHandler(managementServicesManager), 1,
                                                           maxSize);

    final Stage receiveInvalidationStage = stageManager
        .createStage(ClientConfigurationContext.RECEIVE_INVALIDATE_OBJECTS_STAGE,
                     new ReceiveInvalidationHandler(remoteServerMapManager), 1, TCPropertiesImpl.getProperties()
                         .getInt(TCPropertiesConsts.L2_LOCAL_CACHE_INVALIDATIONS_SINK_CAPACITY));

    final List<ClientHandshakeCallback> clientHandshakeCallbacks = new ArrayList<ClientHandshakeCallback>();
    clientHandshakeCallbacks.add(this.lockManager);
    clientHandshakeCallbacks.add(remoteObjectManager);
    clientHandshakeCallbacks.add(remoteServerMapManager);
    clientHandshakeCallbacks.add(searchRequestManager);
    clientHandshakeCallbacks.add(searchResultMgr);
    clientHandshakeCallbacks.add(this.remoteTxnManager);
    clientHandshakeCallbacks.add(this.dsoClientBuilder.getObjectIDClientHandshakeRequester(batchSequenceReceiver));
    clientHandshakeCallbacks.add(this.clusterMetaDataManager);
    clientHandshakeCallbacks.add(teh);
    clientHandshakeCallbacks.add(serverEventListenerManager);
    // ClientObjectManager should be the last one sothat isRejoinInProgress flag of TCObjectSelfStoreImpl has been reset
    // in RemoteServerMapManager.initializeHandshake()
    clientHandshakeCallbacks.add(this.objectManager);
    final ProductInfo pInfo = ProductInfo.getInstance();
    final Collection<ClearableCallback> clearCallbacks = new ArrayList<ClearableCallback>();
    clearCallbacks.add(stageManager);
    clearCallbacks.add(dsoCluster);
    this.clientHandshakeManager = this.dsoClientBuilder
        .createClientHandshakeManager(new ClientIDLogger(this.channel.getClientIDProvider(), TCLogging
                                          .getLogger(ClientHandshakeManagerImpl.class)), this.channel, this.channel
                                          .getClientHandshakeMessageFactory(), pauseStage.getSink(), sessionManager,
                                      dsoCluster, pInfo.version(), Collections
                                          .unmodifiableCollection(clientHandshakeCallbacks), Collections
                                          .unmodifiableCollection(clearCallbacks));

    this.clientChannelEventController = new ClientChannelEventController(channel, pauseStage.getSink(),
                                                                         clientHandshakeManager, this.rejoinManager);

    this.shutdownManager = new ClientShutdownManager(objectManager, this, connectionComponents, rejoinManager);

    this.platformService = new PlatformServiceImpl(objectManager, clientTxnManager, shutdownManager, lockManager,
                                                   searchRequestManager, this, new LockIdFactory(objectManager),
                                                   dsoCluster, abortableOperationManager, uuid,
                                                   serverEventListenerManager, rejoinManager, taskRunner,
                                                   clientHandshakeManager);

    if (rejoinManager.isRejoinEnabled()) {
      platformService = new RejoinAwarePlatformService(platformService);
    }

    classFactory.setPlatformService(platformService);
    objectManager.setPlatformService(platformService);

    final ClientConfigurationContext cc = new ClientConfigurationContext(stageManager, this.lockManager,
                                                                         remoteObjectManager, this.clientTxnManager,
                                                                         this.clientHandshakeManager,
                                                                         this.clusterMetaDataManager,
                                                                         this.rejoinManager,
                                                                         this.managementServicesManager);
    // DO NOT create any stages after this call
    stageManager.startAll(cc, Collections.<PostInit> emptyList());

    initChannelMessageRouter(messageRouter, hydrateStage, lockResponse, receiveRootID, receiveObject,
                             receiveTransaction, oidRequestResponse, transactionResponse, batchTxnAckStage, pauseStage,
                             jmxRemoteTunnelStage, managementStage, clusterMembershipEventStage, clusterMetaDataStage,
                             syncWriteBatchRecvdHandler, receiveServerMapStage, receiveSearchQueryStage,
                             receiveSearchResultStage, receiveInvalidationStage, resourceManagerStage);

    openChannel(serverHost, serverPort);
    waitForHandshake();

    setLoggerOnExit();
  }

  private Stage createServerEventDeliveryStage(final StageManager stageManager) {
    final int threadsCount = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L1_SERVER_EVENT_DELIVERY_THREADS, 4);
    final int queueSize = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L1_SERVER_EVENT_DELIVERY_QUEUE_SIZE, 16 * 1024);
    return stageManager.createStage(ClientConfigurationContext.SERVER_EVENT_DELIVERY_STAGE,
                                    new ServerEventDeliveryHandler(serverEventListenerManager), threadsCount, 1,
                                    queueSize);
  }

  private void openChannel(final String serverHost, final int serverPort) {
    while (true) {
      try {
        DSO_LOGGER.debug("Trying to open channel....");
        final char[] pw;
        if (config.getSecurityInfo().hasCredentials()) {
          Assert.assertNotNull(securityManager);
          pw = securityManager.getPasswordForTC(config.getSecurityInfo().getUsername(), serverHost, serverPort);
        } else {
          pw = null;
        }
        this.channel.open(pw);
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
        CONSOLE_LOGGER.fatal(LicenseManager.ERROR_MESSAGE);
        throw new IllegalStateException(e.getMessage(), e);
      } catch (final CommStackMismatchException e) {
        DSO_LOGGER.fatal(e.getMessage());
        CONSOLE_LOGGER.fatal(e.getMessage());
        throw new IllegalStateException(e.getMessage(), e);
      } catch (final IOException ioe) {
        CONSOLE_LOGGER.warn("IOException connecting to server: " + serverHost + ":" + serverPort + ". "
                            + ioe.getMessage());
        ThreadUtil.reallySleep(5000);
      }
    }

  }

  private void waitForHandshake() {
    this.clientHandshakeManager.waitForHandshake();
    final TCSocketAddress remoteAddress = this.channel.channel().getRemoteAddress();
    final String infoMsg = "Connection successfully established to server at " + remoteAddress;
    CONSOLE_LOGGER.info(infoMsg);
    DSO_LOGGER.info(infoMsg);
  }

  private Map<TCMessageType, GeneratedMessageFactory> getMessageTypeFactoryMapping(final DNAEncoding encoding) {
    final Map<TCMessageType, GeneratedMessageFactory> messageTypeFactoryMapping = new HashMap<TCMessageType, GeneratedMessageFactory>();

    messageTypeFactoryMapping.put(TCMessageType.GET_VALUE_SERVER_MAP_RESPONSE_MESSAGE,
    // Special handling to get the applicator encoding
                                  new GeneratedMessageFactory() {

                                    @Override
                                    public TCMessage createMessage(final SessionID sid, final MessageMonitor monitor,
                                                                   final MessageChannel mChannel,
                                                                   final TCMessageHeader msgHeader,
                                                                   final TCByteBuffer[] data) {
                                      return new GetValueServerMapResponseMessageImpl(sid, monitor, mChannel,
                                                                                      msgHeader, data, encoding);
                                    }

                                    @Override
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

                                    @Override
                                    public TCMessage createMessage(final SessionID sid, final MessageMonitor monitor,
                                                                   final MessageChannel mChannel,
                                                                   final TCMessageHeader msgHeader,
                                                                   final TCByteBuffer[] data) {
                                      return new GetAllKeysServerMapResponseMessageImpl(sid, monitor, mChannel,
                                                                                        msgHeader, data, encoding);
                                    }

                                    @Override
                                    public TCMessage createMessage(final SessionID sid, final MessageMonitor monitor,
                                                                   final TCByteBufferOutputStream output,
                                                                   final MessageChannel mChannel,
                                                                   final TCMessageType type) {
                                      throw new AssertionError(
                                                               GetAllKeysServerMapRequestMessageImpl.class.getName()
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
    messageTypeClassMapping.put(TCMessageType.SEARCH_RESULTS_REQUEST_MESSAGE, SearchResultsRequestMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.SEARCH_RESULTS_RESPONSE_MESSAGE, SearchResultsResponseMessageImpl.class);

    messageTypeClassMapping.put(TCMessageType.GET_VALUE_SERVER_MAP_REQUEST_MESSAGE,
                                GetValueServerMapRequestMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.OBJECT_NOT_FOUND_SERVER_MAP_RESPONSE_MESSAGE,
                                ObjectNotFoundServerMapResponseMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.TUNNELED_DOMAINS_CHANGED_MESSAGE, TunneledDomainsChanged.class);
    messageTypeClassMapping.put(TCMessageType.INVALIDATE_OBJECTS_MESSAGE, InvalidateObjectsMessage.class);
    messageTypeClassMapping.put(TCMessageType.GET_ALL_KEYS_SERVER_MAP_REQUEST_MESSAGE,
                                GetAllKeysServerMapRequestMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.RESOURCE_MANAGER_THROTTLE_STATE_MESSAGE,
                                ResourceManagerThrottleMessage.class);
    messageTypeClassMapping.put(TCMessageType.LIST_REGISTERED_SERVICES_MESSAGE, ListRegisteredServicesMessage.class);
    messageTypeClassMapping.put(TCMessageType.LIST_REGISTERED_SERVICES_RESPONSE_MESSAGE,
                                ListRegisteredServicesResponseMessage.class);
    messageTypeClassMapping.put(TCMessageType.INVOKE_REGISTERED_SERVICE_MESSAGE, InvokeRegisteredServiceMessage.class);
    messageTypeClassMapping.put(TCMessageType.INVOKE_REGISTERED_SERVICE_RESPONSE_MESSAGE,
                                InvokeRegisteredServiceResponseMessage.class);
    return messageTypeClassMapping;
  }

  private void initChannelMessageRouter(TCMessageRouter messageRouter, Stage hydrateStage, Stage lockResponse,
                                        Stage receiveRootID, Stage receiveObject, Stage receiveTransaction,
                                        Stage oidRequestResponse, Stage transactionResponse, Stage batchTxnAckStage,
                                        Stage pauseStage, Stage jmxRemoteTunnelStage, Stage managementStage,
                                        Stage clusterMembershipEventStage, Stage clusterMetaDataStage,
                                        Stage syncWriteBatchRecvdHandler, Stage receiveServerMapStage,
                                        Stage receiveSearchQueryStage, Stage searchResultLoadStage,
                                        Stage receiveInvalidationStage, Stage resourceManagerStage) {
    final Sink hydrateSink = hydrateStage.getSink();
    messageRouter.routeMessageType(TCMessageType.LOCK_RESPONSE_MESSAGE, lockResponse.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.LOCK_QUERY_RESPONSE_MESSAGE, lockResponse.getSink(), hydrateSink);
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
    messageRouter.routeMessageType(TCMessageType.LIST_REGISTERED_SERVICES_MESSAGE, managementStage.getSink(),
                                   hydrateSink);
    messageRouter.routeMessageType(TCMessageType.INVOKE_REGISTERED_SERVICE_MESSAGE, managementStage.getSink(),
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
    messageRouter.routeMessageType(TCMessageType.SEARCH_QUERY_RESPONSE_MESSAGE, receiveSearchQueryStage.getSink(),
                                   hydrateSink);
    messageRouter.routeMessageType(TCMessageType.SEARCH_RESULTS_RESPONSE_MESSAGE, searchResultLoadStage.getSink(),
                                   hydrateSink);
    messageRouter.routeMessageType(TCMessageType.INVALIDATE_OBJECTS_MESSAGE, receiveInvalidationStage.getSink(),
                                   hydrateSink);
    messageRouter.routeMessageType(TCMessageType.RESOURCE_MANAGER_THROTTLE_STATE_MESSAGE,
                                   resourceManagerStage.getSink(), hydrateSink);
    DSO_LOGGER.debug("Added message routing types.");
  }

  private void setLoggerOnExit() {
    CommonShutDownHook.addShutdownHook(new Runnable() {
      @Override
      public void run() {
        DSO_LOGGER.info("L1 Exiting...");
      }
    });
  }

  public ClientTransactionManager getTransactionManager() {
    return this.clientTxnManager;
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
    return this.remoteTxnManager;
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

  public TunneledDomainManager getTunneledDomainManager() {
    return this.tunneledDomainManager;
  }

  @Override
  public void dump() {
    this.dumpHandler.dump();
  }

  @Override
  public void startBeanShell(final int port) {
    try {
      final Interpreter i = new Interpreter();
      i.set("client", this);
      i.set("objectManager", this.objectManager);
      i.set("lockmanager", this.lockManager);
      i.set("txManager", this.clientTxnManager);
      i.set("portnum", port);
      i.eval("setAccessibility(true)"); // turn off access restrictions
      i.eval("server(portnum)");
      CONSOLE_LOGGER.info("Bean shell is started on port " + port);
    } catch (final EvalError e) {
      e.printStackTrace();
    }
  }

  @Override
  public void reloadConfiguration() throws ConfigurationSetupException {
    if (false) throw new ConfigurationSetupException(); // to avoid warning
    throw new UnsupportedOperationException();
  }

  @Override
  public void addServerConfigurationChangedListeners(final ClusterTopologyChangedListener listener) {
    throw new UnsupportedOperationException();
  }

  protected DSOClientConfigHelper getClientConfigHelper() {
    return this.config;
  }

  public void shutdown() {
    shutdownClient(false, false);
  }

  void shutdownResources() {
    final TCLogger logger = DSO_LOGGER;

    if (this.rejoinManager != null) {
      this.rejoinManager.shutdown();
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
        this.lockManager.shutdown(false);
      } catch (final Throwable t) {
        logger.error("Error stopping lock manager", t);
      } finally {
        this.lockManager = null;
      }
    }

    if (serverEventListenerManager != null) {
      try {
        serverEventListenerManager.shutdown(false);
      } catch (final Throwable t) {
        logger.error("Error stopping server event listener manager", t);
      } finally {
        serverEventListenerManager = null;
      }
    }

    try {
      getStageManager().stopAll();
    } catch (final Throwable t) {
      logger.error("Error stopping stage manager", t);
    }

    if (globalLocalCacheManager != null) {
      globalLocalCacheManager.shutdown(false);
    }

    if (this.objectManager != null) {
      try {
        this.objectManager.shutdown(false);
      } catch (final Throwable t) {
        logger.error("Error shutting down client object manager", t);
      } finally {
        this.objectManager = null;
      }
    }
    
    if (this.remoteTxnManager != null) {
      try {
        this.remoteTxnManager.stop();
      } catch (Throwable t) {
        logger.error("Error shutting down remote txn mgr", t);
      } finally {
        this.remoteTxnManager = null;
      }
    }

    clientChannelEventController.shutdown();

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

    if (taskRunner != null) {
      logger.info("Shutting down TaskRunner");
      taskRunner.shutdown();
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

    if (TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.L1_SHUTDOWN_FORCE_FINALIZATION)) System
        .runFinalization();
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

  @Override
  public String[] processArguments() {
    return null;
  }

  @Override
  public String getUUID() {
    return uuid.toString();
  }

  public ManagementServicesManager getManagementServicesManager() {
    return managementServicesManager;
  }

  private static class TCThreadGroupCleanerRunnable implements Runnable {
    private final TCThreadGroup threadGroup;

    public TCThreadGroupCleanerRunnable(TCThreadGroup threadGroup) {
      this.threadGroup = threadGroup;
    }

    @Override
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

  public GroupID[] getGroupIDs() {
    return this.connectionComponents.getGroupIDs();
  }

  public RemoteResourceManager getRemoteResourceManager() {
    return remoteResourceManager;
  }

  public ServerEventListenerManager getServerEventListenerManager() {
    return serverEventListenerManager;
  }

  public PlatformService getPlatformService() {
    return this.platformService;
  }

  public DsoCluster getDsoCluster() {
    return this.dsoCluster;
  }

  private void shutdownClient(boolean fromShutdownHook, boolean forceImmediate) {
    if (this.shutdownManager != null) {
      try {
        this.shutdownManager.execute(fromShutdownHook, forceImmediate);
      } finally {
        // If we're not being called as a result of the shutdown hook, de-register the hook
        if (Thread.currentThread() != this.shutdownAction) {
          try {
            Runtime.getRuntime().removeShutdownHook(this.shutdownAction);
          } catch (final Exception e) {
            // ignore
          }
        }
      }
    }
  }

  private void shutdown(boolean fromShutdownHook, boolean forceImmediate) {
    if (clientStopped.attemptSet()) {
      DSO_LOGGER.info("shuting down Terracotta Client hook=" + fromShutdownHook + " force=" + forceImmediate);
      shutdownClient(fromShutdownHook, forceImmediate);
    } else {
      DSO_LOGGER.info("Client already shutdown.");
    }
  }

  private class ShutdownAction implements Runnable {
    @Override
    public void run() {
      DSO_LOGGER.info("Running L1 VM shutdown hook");
      shutdown(true, false);
    }
  }

  public void addTunneledMBeanDomain(String mbeanDomain) {
    this.config.addTunneledMBeanDomain(mbeanDomain);
  }
}
