/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import org.apache.commons.io.FileUtils;
import org.terracotta.corestorage.monitoring.MonitoredResource;

import bsh.EvalError;
import bsh.Interpreter;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.tc.async.api.PostInit;
import com.tc.async.api.SEDA;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.async.impl.NullSink;
import com.tc.config.HaConfig;
import com.tc.config.HaConfigImpl;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.exception.TCRuntimeException;
import com.tc.exception.TCServerRestartException;
import com.tc.exception.TCShutdownServerException;
import com.tc.exception.ZapDirtyDbServerNodeException;
import com.tc.exception.ZapServerNodeException;
import com.tc.handler.CallbackDumpAdapter;
import com.tc.handler.CallbackDumpHandler;
import com.tc.handler.CallbackGroupExceptionHandler;
import com.tc.handler.CallbackZapDirtyDbExceptionAdapter;
import com.tc.handler.CallbackZapServerNodeExceptionAdapter;
import com.tc.handler.LockInfoDumpHandler;
import com.tc.io.TCFile;
import com.tc.io.TCFileImpl;
import com.tc.io.TCRandomFileAccessImpl;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.ha.HASettingsChecker;
import com.tc.l2.ha.StripeIDStateManagerImpl;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.l2.ha.ZapNodeProcessorWeightGeneratorFactory;
import com.tc.l2.objectserver.L2IndexStateManager;
import com.tc.l2.objectserver.L2ObjectStateManager;
import com.tc.l2.objectserver.L2PassiveSyncStateManager;
import com.tc.l2.objectserver.ServerTransactionFactory;
import com.tc.l2.state.StateSyncManager;
import com.tc.l2.state.StateSyncManagerImpl;
import com.tc.lang.TCThreadGroup;
import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;
import com.tc.logging.CustomerLogging;
import com.tc.logging.DumpHandlerStore;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.logging.ThreadDumpHandler;
import com.tc.management.L2Management;
import com.tc.management.RemoteJMXProcessor;
import com.tc.management.RemoteManagement;
import com.tc.management.RemoteManagementImpl;
import com.tc.management.TSAManagementEventPayload;
import com.tc.management.TerracottaRemoteManagement;
import com.tc.management.beans.L2DumperMBean;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.L2State;
import com.tc.management.beans.TCDumper;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.management.beans.object.ObjectManagementMonitor.ObjectIdsFetcher;
import com.tc.management.beans.object.ServerDBBackupMBean;
import com.tc.management.remote.connect.ClientConnectEventHandler;
import com.tc.management.remote.protocol.terracotta.ClientTunnelingEventHandler;
import com.tc.management.remote.protocol.terracotta.JmxRemoteTunnelMessage;
import com.tc.management.remote.protocol.terracotta.L1JmxReady;
import com.tc.management.remote.protocol.terracotta.TunneledDomainsChanged;
import com.tc.net.AddressChecker;
import com.tc.net.NIOWorkarounds;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.Node;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OOONetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl;
import com.tc.net.protocol.tcm.ChannelManager;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.HydrateHandler;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.MessageMonitorImpl;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.TCMessageRouter;
import com.tc.net.protocol.tcm.TCMessageRouterImpl;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.HealthCheckerConfigImpl;
import com.tc.net.protocol.transport.TransportHandshakeErrorNullHandler;
import com.tc.net.utils.L2Utils;
import com.tc.object.config.schema.L2DSOConfig;
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
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerImpl;
import com.tc.object.net.DSOChannelManagerMBean;
import com.tc.object.session.NullSessionManager;
import com.tc.object.session.SessionManager;
import com.tc.objectserver.api.BackupManager;
import com.tc.objectserver.api.GarbageCollectionManager;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.api.ResourceManager;
import com.tc.objectserver.api.SequenceNames;
import com.tc.objectserver.api.ServerMapEvictionManager;
import com.tc.objectserver.api.ServerMapRequestManager;
import com.tc.objectserver.api.TransactionStore;
import com.tc.objectserver.clustermetadata.ServerClusterMetaDataManager;
import com.tc.objectserver.clustermetadata.ServerClusterMetaDataManagerImpl;
import com.tc.objectserver.core.api.DSOGlobalServerStatsImpl;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ServerManagementContext;
import com.tc.objectserver.dgc.api.GarbageCollectionInfoPublisher;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.dgc.impl.DGCOperatorEventPublisher;
import com.tc.objectserver.dgc.impl.GCControllerImpl;
import com.tc.objectserver.dgc.impl.GCStatsEventPublisher;
import com.tc.objectserver.dgc.impl.GarbageCollectionInfoPublisherImpl;
import com.tc.objectserver.dgc.impl.MarkAndSweepGarbageCollector;
import com.tc.objectserver.event.ClientChannelMonitorImpl;
import com.tc.objectserver.event.InClusterServerEventBuffer;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.objectserver.gtx.ServerGlobalTransactionManagerImpl;
import com.tc.objectserver.handler.ApplyTransactionChangeHandler;
import com.tc.objectserver.handler.BroadcastChangeHandler;
import com.tc.objectserver.handler.ChannelLifeCycleHandler;
import com.tc.objectserver.handler.ClientChannelOperatorEventlistener;
import com.tc.objectserver.handler.ClientHandshakeHandler;
import com.tc.objectserver.handler.GarbageCollectHandler;
import com.tc.objectserver.handler.GlobalTransactionIDBatchRequestHandler;
import com.tc.objectserver.handler.InvalidateObjectsHandler;
import com.tc.objectserver.handler.LowWaterMarkCallbackHandler;
import com.tc.objectserver.handler.ManagedObjectRequestHandler;
import com.tc.objectserver.handler.ProcessTransactionHandler;
import com.tc.objectserver.handler.RequestLockUnLockHandler;
import com.tc.objectserver.handler.RequestObjectIDBatchHandler;
import com.tc.objectserver.handler.RequestRootHandler;
import com.tc.objectserver.handler.RespondToObjectRequestHandler;
import com.tc.objectserver.handler.RespondToRequestLockHandler;
import com.tc.objectserver.handler.RespondToServerMapRequestHandler;
import com.tc.objectserver.handler.ServerClusterMetaDataHandler;
import com.tc.objectserver.handler.ServerManagementHandler;
import com.tc.objectserver.handler.ServerMapEvictionHandler;
import com.tc.objectserver.handler.ServerMapPrefetchObjectHandler;
import com.tc.objectserver.handler.ServerMapRequestHandler;
import com.tc.objectserver.handler.SyncWriteTransactionReceivedHandler;
import com.tc.objectserver.handler.TransactionAcknowledgementHandler;
import com.tc.objectserver.handler.TransactionLookupHandler;
import com.tc.objectserver.handler.TransactionLowWaterMarkHandler;
import com.tc.objectserver.handler.ValidateObjectsHandler;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;
import com.tc.objectserver.l1.impl.ClientStateManagerImpl;
import com.tc.objectserver.l1.impl.InvalidateObjectManagerImpl;
import com.tc.objectserver.l1.impl.TransactionAcknowledgeAction;
import com.tc.objectserver.l1.impl.TransactionAcknowledgeActionImpl;
import com.tc.objectserver.locks.LockManagerImpl;
import com.tc.objectserver.managedobject.ConcurrentDistributedServerMapManagedObjectState;
import com.tc.objectserver.managedobject.ManagedObjectChangeListenerProviderImpl;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.metadata.MetaDataManager;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.persistence.ClientStatePersistor;
import com.tc.objectserver.persistence.EvictionTransactionPersistor;
import com.tc.objectserver.persistence.OffheapStatsImpl;
import com.tc.objectserver.persistence.Persistor;
import com.tc.objectserver.persistence.StorageDataStatsImpl;
import com.tc.objectserver.persistence.TransactionPersistor;
import com.tc.objectserver.search.IndexHACoordinator;
import com.tc.objectserver.search.SearchEventHandler;
import com.tc.objectserver.search.SearchQueryRequestMessageHandler;
import com.tc.objectserver.search.SearchRequestManager;
import com.tc.objectserver.storage.api.OffheapStats;
import com.tc.objectserver.storage.api.StorageDataStats;
import com.tc.objectserver.tx.CommitTransactionMessageRecycler;
import com.tc.objectserver.tx.ResentTransactionSequencer;
import com.tc.objectserver.tx.ServerTransactionManagerConfig;
import com.tc.objectserver.tx.ServerTransactionManagerImpl;
import com.tc.objectserver.tx.TransactionBatchManagerImpl;
import com.tc.objectserver.tx.TransactionFilter;
import com.tc.objectserver.tx.TransactionalObjectManagerImpl;
import com.tc.objectserver.tx.TransactionalStagesCoordinatorImpl;
import com.tc.operatorevent.DsoOperatorEventHistoryProvider;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEventCallback;
import com.tc.operatorevent.TerracottaOperatorEventHistoryProvider;
import com.tc.operatorevent.TerracottaOperatorEventLogging;
import com.tc.properties.L1ReconnectConfigImpl;
import com.tc.properties.ReconnectConfig;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.runtime.TCMemoryManagerImpl;
import com.tc.runtime.logging.LongGCLogger;
import com.tc.server.ServerConnectionValidator;
import com.tc.server.TCServer;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.stats.counter.sampled.SampledCumulativeCounter;
import com.tc.stats.counter.sampled.SampledCumulativeCounterConfig;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.stats.counter.sampled.derived.SampledRateCounterConfig;
import com.tc.util.Assert;
import com.tc.util.CommonShutDownHook;
import com.tc.util.Events;
import com.tc.util.PortChooser;
import com.tc.util.ProductInfo;
import com.tc.util.SequenceValidator;
import com.tc.util.StartupLock;
import com.tc.util.TCTimeoutException;
import com.tc.util.UUID;
import com.tc.util.concurrent.Runners;
import com.tc.util.concurrent.TaskRunner;
import com.tc.util.runtime.LockInfoByThreadID;
import com.tc.util.runtime.NullThreadIDMapImpl;
import com.tc.util.runtime.ThreadIDMap;
import com.tc.util.sequence.BatchSequence;
import com.tc.util.sequence.DGCSequenceProvider;
import com.tc.util.sequence.MutableSequence;
import com.tc.util.sequence.ObjectIDSequence;
import com.tc.util.sequence.Sequence;
import com.tc.util.sequence.SequenceGenerator;
import com.tc.util.startuplock.FileNotCreatedException;
import com.tc.util.startuplock.LocationNotCreatedException;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Timer;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;

/**
 * Startup and shutdown point. Builds and starts the server
 */
public class DistributedObjectServer implements TCDumper, LockInfoDumpHandler, ServerConnectionValidator,
    DumpHandlerStore {
  private final ConnectionPolicy                 connectionPolicy;
  private final TCServerInfoMBean                tcServerInfoMBean;
  private final ObjectStatsRecorder              objectStatsRecorder;
  private final L2State                          l2State;
  private final DSOServerBuilder                 serverBuilder;
  protected final L2ConfigurationSetupManager    configSetupManager;
  private final Sink                             httpSink;
  protected final HaConfigImpl                   haConfig;

  private static final TCLogger                  logger           = CustomerLogging.getDSOGenericLogger();
  private static final TCLogger                  consoleLogger    = CustomerLogging.getConsoleLogger();

  private ServerID                               thisServerNodeID = ServerID.NULL_ID;
  protected NetworkListener                      l1Listener;
  protected GCStatsEventPublisher                gcStatsEventPublisher;
  private TerracottaOperatorEventHistoryProvider operatorEventHistoryProvider;
  private CommunicationsManager                  communicationsManager;
  private ServerConfigurationContext             context;
  private ObjectManagerImpl                      objectManager;
  private ObjectRequestManager                   objectRequestManager;
  private ServerMapRequestManager                serverMapRequestManager;
  private ServerMapEvictionManager               serverMapEvictor;
  private TransactionalObjectManagerImpl         txnObjectManager;
  private CounterManager                         sampledCounterManager;
  private LockManagerImpl                        lockManager;
  private ServerManagementContext                managementContext;
  private StartupLock                            startupLock;
  private ClientStateManager                     clientStateManager;
  private PersistentManagedObjectStore           objectStore;
  private GarbageCollectionManager               garbageCollectionManager;
  private Persistor                              persistor;
  private BackupManager                          backupManager;
  private ResourceManager                        resourceManager;
  private ServerTransactionManagerImpl           transactionManager;

  private L2Management                           l2Management;
  private L2Coordinator                          l2Coordinator;

  private TCProperties                           tcProperties;

  private ConnectionIDFactoryImpl                connectionIdFactory;

  private final TCThreadGroup                    threadGroup;
  private final SEDA                             seda;

  private ReconnectConfig                        l1ReconnectConfig;

  private GroupManager                           groupCommManager;
  private Stage                                  hydrateStage;
  private StripeIDStateManagerImpl               stripeIDStateManager;
  private IndexHACoordinator                     indexHACoordinator;
  private MetaDataManager                        metaDataManager;
  private SearchRequestManager                   searchRequestManager;

  private EvictionTransactionPersistor           evictionTransactionPersistor;

  private final CallbackDumpHandler              dumpHandler      = new CallbackDumpHandler();

  protected final TCSecurityManager              tcSecurityManager;

  private final TaskRunner                       taskRunner;

  // used by a test
  public DistributedObjectServer(final L2ConfigurationSetupManager configSetupManager, final TCThreadGroup threadGroup,
                                 final ConnectionPolicy connectionPolicy, final TCServerInfoMBean tcServerInfoMBean,
                                 final ObjectStatsRecorder objectStatsRecorder) {
    this(configSetupManager, threadGroup, connectionPolicy, new NullSink(), tcServerInfoMBean, objectStatsRecorder,
         new L2State(), new SEDA(threadGroup), null, null);

  }

  public DistributedObjectServer(final L2ConfigurationSetupManager configSetupManager, final TCThreadGroup threadGroup,
                                 final ConnectionPolicy connectionPolicy, final Sink httpSink,
                                 final TCServerInfoMBean tcServerInfoMBean,
                                 final ObjectStatsRecorder objectStatsRecorder, final L2State l2State, final SEDA seda,
                                 final TCServer server, final TCSecurityManager securityManager) {
    // This assertion is here because we want to assume that all threads spawned by the server (including any created in
    // 3rd party libs) inherit their thread group from the current thread . Consider this before removing the assertion.
    // Even in tests, we probably don't want different thread group configurations
    Assert.assertEquals(threadGroup, Thread.currentThread().getThreadGroup());

    this.tcSecurityManager = securityManager;
    if (configSetupManager.isSecure()) {
      Assert.assertNotNull("Security is turned on, but TCSecurityManager", this.tcSecurityManager);
      consoleLogger.info("Security enabled, turning on SSL");
    }

    this.configSetupManager = configSetupManager;
    this.haConfig = new HaConfigImpl(this.configSetupManager);
    this.connectionPolicy = connectionPolicy;
    this.httpSink = httpSink;
    this.tcServerInfoMBean = tcServerInfoMBean;
    this.objectStatsRecorder = objectStatsRecorder;
    this.l2State = l2State;
    this.threadGroup = threadGroup;
    this.seda = seda;
    this.serverBuilder = createServerBuilder(this.haConfig, logger, server, configSetupManager.dsoL2Config());
    this.taskRunner = Runners.newDefaultCachedScheduledTaskRunner(this.threadGroup);
  }

  protected DSOServerBuilder createServerBuilder(final HaConfig config, final TCLogger tcLogger, final TCServer server,
                                                 L2DSOConfig l2dsoConfig) {
    Assert.assertEquals(config.isActiveActive(), false);
    return new StandardDSOServerBuilder(config, tcLogger, tcSecurityManager);
  }

  protected DSOServerBuilder getServerBuilder() {
    return this.serverBuilder;
  }

  public TaskRunner getTaskRunner() {
    return taskRunner;
  }

  @Override
  public void dump() {
    this.dumpHandler.dump();
    this.serverBuilder.dump();
  }

  public synchronized void start() throws IOException, LocationNotCreatedException, FileNotCreatedException {

    threadGroup.addCallbackOnExitDefaultHandler(new ThreadDumpHandler(this));
    threadGroup.addCallbackOnExitDefaultHandler(this.dumpHandler);
    threadGroup.addCallbackOnExitExceptionHandler(TCServerRestartException.class, new CallbackOnExitHandler() {
      @Override
      public void callbackOnExit(final CallbackOnExitState state) {
        state.setRestartNeeded();
      }
    });
    threadGroup.addCallbackOnExitExceptionHandler(TCShutdownServerException.class, new CallbackOnExitHandler() {
      @Override
      public void callbackOnExit(final CallbackOnExitState state) {
        Throwable t = state.getThrowable();
        while (t.getCause() != null) {
          t = t.getCause();
        }
        consoleLogger.error("Server exiting: " + t.getMessage());
      }
    });

    this.thisServerNodeID = makeServerNodeID(this.configSetupManager.dsoL2Config());
    ThisServerNodeId.setThisServerNodeId(thisServerNodeID);

    TerracottaOperatorEventLogging.setNodeNameProvider(new ServerNameProvider(this.configSetupManager.dsoL2Config()
        .serverName()));


    final List<PostInit> toInit = new ArrayList<PostInit>();

    // perform the DSO network config verification
    final L2DSOConfig l2DSOConfig = this.configSetupManager.dsoL2Config();

    // verify user input host name, DEV-2293
    final String host = l2DSOConfig.host();
    final InetAddress ip = InetAddress.getByName(host);
    if (!ip.isLoopbackAddress() && (NetworkInterface.getByInetAddress(ip) == null)) {
      final String msg = "Unable to find local network interface for " + host;
      consoleLogger.error(msg);
      logger.error(msg, new TCRuntimeException(msg));
      System.exit(-1);
    }

    String bindAddress = this.configSetupManager.commonl2Config().jmxPort().getBind();
    if (bindAddress == null) {
      // workaround for CDV-584
      bindAddress = TCSocketAddress.WILDCARD_IP;
    }

    final InetAddress jmxBind = InetAddress.getByName(bindAddress);
    final AddressChecker addressChecker = new AddressChecker();
    if (!addressChecker.isLegalBindAddress(jmxBind)) { throw new IOException("Invalid bind address [" + jmxBind
                                                                             + "]. Local addresses are "
                                                                             + addressChecker.getAllLocalAddresses()); }

    NIOWorkarounds.solaris10Workaround();
    ConcurrentDistributedServerMapManagedObjectState.init();
    this.tcProperties = TCPropertiesImpl.getProperties();
    this.l1ReconnectConfig = new L1ReconnectConfigImpl();
    final boolean restartable = l2DSOConfig.getRestartable().getEnabled();
    final boolean hybrid = l2DSOConfig.getDataStorage().isSetHybrid();

    // start the JMX server
    try {
      startJMXServer(l2DSOConfig.isJmxEnabled(), jmxBind, this.configSetupManager.commonl2Config().jmxPort()
          .getIntValue(),
                     new RemoteJMXProcessor(), null);
    } catch (final Exception e) {
      final String msg = "Unable to start the JMX server. Do you have another Terracotta Server instance running?";
      consoleLogger.error(msg);
      logger.error(msg, e);
      System.exit(-1);
    }


    final TCFile location = new TCFileImpl(this.configSetupManager.commonl2Config().dataPath());
    boolean retries = tcProperties.getBoolean(TCPropertiesConsts.L2_STARTUPLOCK_RETRIES_ENABLED);
    this.startupLock = this.serverBuilder.createStartupLock(location, retries);

    if (!this.startupLock.canProceed(new TCRandomFileAccessImpl())) {
      consoleLogger.error("Another L2 process is using the directory " + location + " as data directory.");
      if (!restartable) {
        consoleLogger.error("This is not allowed with persistence mode set to temporary-swap-only.");
      }
      consoleLogger.error("Exiting...");
      System.exit(1);
    }

    final int maxStageSize = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_SEDA_STAGE_SINK_CAPACITY);
    final StageManager stageManager = this.seda.getStageManager();
    final SessionManager sessionManager = new NullSessionManager();

    this.dumpHandler.registerForDump(new CallbackDumpAdapter(stageManager));

    final Sequence globalTransactionIDSequence;
    final GarbageCollectionInfoPublisher gcPublisher = new GarbageCollectionInfoPublisherImpl();
    final ManagedObjectChangeListenerProviderImpl managedObjectChangeListenerProvider = new ManagedObjectChangeListenerProviderImpl();

    this.sampledCounterManager = new CounterManagerImpl();
    final SampledCounterConfig sampledCounterConfig = new SampledCounterConfig(1, 300, true, 0L);

    logger.debug("persistent: " + restartable);
    if (!restartable) {
      File indexRoot = configSetupManager.commonl2Config().indexPath();
      if (indexRoot.exists()) {
        logger.info("deleting index directory: " + indexRoot.getAbsolutePath());
        FileUtils.cleanDirectory(indexRoot);
      }
    }

    persistor = serverBuilder.createPersistor(restartable, configSetupManager.commonl2Config().dataPath(), l2State);
    dumpHandler.registerForDump(new CallbackDumpAdapter(persistor));
    new ServerPersistenceVersionChecker(persistor.getClusterStatePersistor()).checkAndSetVersion();
    persistor.start();

    // register the terracotta operator event logger
    this.operatorEventHistoryProvider = new DsoOperatorEventHistoryProvider();
    this.serverBuilder
        .registerForOperatorEvents(this.l2Management, this.operatorEventHistoryProvider, getMBeanServer());

    this.objectStore = new PersistentManagedObjectStore(this.persistor.getManagedObjectPersistor());

    this.threadGroup
        .addCallbackOnExitExceptionHandler(ZapDirtyDbServerNodeException.class,
                                           new CallbackZapDirtyDbExceptionAdapter(logger, consoleLogger, this.persistor
                                               .getClusterStatePersistor()));
    this.threadGroup
        .addCallbackOnExitExceptionHandler(ZapServerNodeException.class,
                                           new CallbackZapServerNodeExceptionAdapter(logger, consoleLogger,
                                                                                     this.persistor
                                                                                         .getClusterStatePersistor()));

    MutableSequence gidSequence;
    TransactionPersistor transactionPersistor = this.persistor.getTransactionPersistor();
    this.evictionTransactionPersistor = persistor.getEvictionTransactionPersistor();

    gidSequence = this.persistor.getGlobalTransactionIDSequence();

    final GlobalTransactionIDBatchRequestHandler gidSequenceProvider = new GlobalTransactionIDBatchRequestHandler(
                                                                                                                  gidSequence);
    final Stage requestBatchStage = stageManager
        .createStage(ServerConfigurationContext.REQUEST_BATCH_GLOBAL_TRANSACTION_ID_SEQUENCE_STAGE,
                     gidSequenceProvider, 1, maxStageSize);
    gidSequenceProvider.setRequestBatchSink(requestBatchStage.getSink());
    globalTransactionIDSequence = new BatchSequence(gidSequenceProvider, 10000);

    ClientStatePersistor clientStateStore = this.persistor.getClientStatePersistor();

    ManagedObjectStateFactory.createInstance(managedObjectChangeListenerProvider, persistor);

    final int stageWorkerThreadCount = L2Utils.getOptimalStageWorkerThreads();

    final TCMessageRouter messageRouter = new TCMessageRouterImpl();
    this.communicationsManager = createCommunicationsManager(messageRouter);


    this.clientStateManager = new ClientStateManagerImpl();
    final ClientObjectReferenceSet clientObjectReferenceSet = new ClientObjectReferenceSet(this.clientStateManager);

    final boolean gcEnabled = l2DSOConfig.garbageCollection().getEnabled();

    final long gcInterval = l2DSOConfig.garbageCollection().getInterval();

    final boolean verboseGC = l2DSOConfig.garbageCollection().getVerbose();
    final SampledCumulativeCounterConfig sampledCumulativeCounterConfig = new SampledCumulativeCounterConfig(1, 300,
                                                                                                             true, 0L);
    final SampledCounter objectCreationRate = (SampledCounter) this.sampledCounterManager
        .createCounter(sampledCounterConfig);
    final ObjectManagerStatsImpl objMgrStats = new ObjectManagerStatsImpl(objectCreationRate);

    final SequenceValidator sequenceValidator = new SequenceValidator(0);

    final ObjectManagerConfig objectManagerConfig = new ObjectManagerConfig(gcInterval * 1000, gcEnabled, verboseGC,
            restartable);

    final Stage garbageCollectStage = stageManager.createStage(ServerConfigurationContext.GARBAGE_COLLECT_STAGE,
                                                               new GarbageCollectHandler(objectManagerConfig),
                                                               1, -1);

    this.garbageCollectionManager = new ActiveGarbageCollectionManager(garbageCollectStage.getSink(), persistor);
    toInit.add(this.garbageCollectionManager);

    this.objectManager = new ObjectManagerImpl(objectManagerConfig, this.clientStateManager, this.objectStore,
        objMgrStats, persistor.getPersistenceTransactionProvider());

    this.gcStatsEventPublisher = new GCStatsEventPublisher();
    managedObjectChangeListenerProvider.setListener(this.objectManager);
    final CallbackDumpAdapter objMgrDumpAdapter = new CallbackDumpAdapter(this.objectManager);
    this.dumpHandler.registerForDump(objMgrDumpAdapter);

    final TCMemoryManagerImpl tcMemManager = new TCMemoryManagerImpl(this.threadGroup);
    final long timeOut = TCPropertiesImpl.getProperties().getLong(TCPropertiesConsts.LOGGING_LONG_GC_THRESHOLD);
    final LongGCLogger gcLogger = this.serverBuilder.createLongGCLogger(timeOut);

    tcMemManager.registerForMemoryEvents(gcLogger);
    // CDV-1181 warn if using CMS
    tcMemManager.checkGarbageCollectors();

    this.connectionIdFactory = new ConnectionIDFactoryImpl(clientStateStore);

    final int serverPort = l2DSOConfig.tsaPort().getIntValue();

    final String dsoBind = l2DSOConfig.tsaPort().getBind();
    this.l1Listener = this.communicationsManager.createListener(sessionManager,
                                                                new TCSocketAddress(dsoBind, serverPort), true,
                                                                this.connectionIdFactory, this.httpSink);

    final ClientTunnelingEventHandler cteh = new ClientTunnelingEventHandler();
    this.stripeIDStateManager = new StripeIDStateManagerImpl(this.haConfig, this.persistor.getClusterStatePersistor());

    this.dumpHandler.registerForDump(new CallbackDumpAdapter(this.stripeIDStateManager));

    final ProductInfo pInfo = ProductInfo.getInstance();
    final DSOChannelManager channelManager = new DSOChannelManagerImpl(this.haConfig.getThisGroupID(),
                                                                       this.l1Listener.getChannelManager(),
                                                                       this.communicationsManager
                                                                           .getConnectionManager(), pInfo.version(),
                                                                       this.stripeIDStateManager);
    channelManager.addEventListener(cteh);
    channelManager.addEventListener(this.connectionIdFactory);

    final ChannelStatsImpl channelStats = new ChannelStatsImpl(sampledCounterManager, channelManager);
    ManagedObjectStateFactory.getInstance().getOperationEventBus().register(channelStats);
    channelManager.addEventListener(channelStats);

    final CommitTransactionMessageRecycler recycler = new CommitTransactionMessageRecycler();
    toInit.add(recycler);

    // Creating a stage here so that the sink can be passed
    final Stage respondToLockStage = stageManager.createStage(ServerConfigurationContext.RESPOND_TO_LOCK_REQUEST_STAGE,
                                                              new RespondToRequestLockHandler(),
                                                              stageWorkerThreadCount, 1, maxStageSize);
    this.lockManager = new LockManagerImpl(respondToLockStage.getSink(), channelManager);

    final CallbackDumpAdapter lockDumpAdapter = new CallbackDumpAdapter(this.lockManager);
    this.dumpHandler.registerForDump(lockDumpAdapter);
    final ObjectInstanceMonitorImpl instanceMonitor = new ObjectInstanceMonitorImpl();

    final TransactionFilter txnFilter = this.serverBuilder.getTransactionFilter(toInit, stageManager, maxStageSize);

    // create a stage which will send an ack to the clients that they have received a particular batch
    final Stage syncWriteTxnRecvdAckStage = stageManager
        .createStage(ServerConfigurationContext.SYNC_WRITE_TXN_RECVD_STAGE,
                     new SyncWriteTransactionReceivedHandler(channelManager), 4, maxStageSize);

    int searchThreads = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_SEDA_SEARCH_THREADS);

    final Stage searchEventStage = stageManager.createStage(ServerConfigurationContext.SEARCH_EVENT_STAGE,
                                                            new SearchEventHandler(), searchThreads, 1, maxStageSize);

    int queryThreads = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_SEDA_QUERY_THREADS);
    final Stage searchQueryRequestStage = stageManager
        .createStage(ServerConfigurationContext.SEARCH_QUERY_REQUEST_STAGE, new SearchQueryRequestMessageHandler(),
                     queryThreads, maxStageSize);

    final Sink searchEventSink = searchEventStage.getSink();

    ResentTransactionSequencer resentTransactionSequencer = new ResentTransactionSequencer();
    toInit.add(resentTransactionSequencer);
    final TransactionBatchManagerImpl transactionBatchManager = new TransactionBatchManagerImpl(sequenceValidator,
                                                                                                recycler, txnFilter,
                                                                                                syncWriteTxnRecvdAckStage
                                                                                                    .getSink(), resentTransactionSequencer);
    toInit.add(transactionBatchManager);
    this.dumpHandler.registerForDump(new CallbackDumpAdapter(transactionBatchManager));

    final TransactionAcknowledgeAction taa = new TransactionAcknowledgeActionImpl(channelManager,
                                                                                  transactionBatchManager);
    final SampledCounter globalTxnCounter = (SampledCounter) this.sampledCounterManager
        .createCounter(sampledCounterConfig);

    // DEV-8737. Count map mutation operations
    final SampledCounter globalOperationCounter = (SampledCounter) this.sampledCounterManager
        .createCounter(sampledCounterConfig);
    final EventBus operationEventBus = ManagedObjectStateFactory.getInstance().getOperationEventBus();
    operationEventBus.register(new OperationCountIncrementEventListener(globalOperationCounter));

    final SampledCounter broadcastCounter = (SampledCounter) this.sampledCounterManager
        .createCounter(sampledCounterConfig);

    final SampledCounter globalObjectFaultCounter = (SampledCounter) this.sampledCounterManager
        .createCounter(sampledCounterConfig);
    final SampledCounter globalLockRecallCounter = (SampledCounter) this.sampledCounterManager
        .createCounter(sampledCounterConfig);
    final SampledRateCounterConfig sampledRateCounterConfig = new SampledRateCounterConfig(1, 300, true);
    final SampledRateCounter changesPerBroadcast = (SampledRateCounter) this.sampledCounterManager
        .createCounter(sampledRateCounterConfig);
    final SampledRateCounter transactionSizeCounter = (SampledRateCounter) this.sampledCounterManager
        .createCounter(sampledRateCounterConfig);
    final SampledCounter globalLockCount = (SampledCounter) this.sampledCounterManager
        .createCounter(sampledCounterConfig);
    final SampledCumulativeCounter globalServerMapGetSizeRequestsCounter = (SampledCumulativeCounter) this.sampledCounterManager
        .createCounter(sampledCumulativeCounterConfig);
    final SampledCumulativeCounter globalServerMapGetValueRequestsCounter = (SampledCumulativeCounter) this.sampledCounterManager
        .createCounter(sampledCumulativeCounterConfig);
    final SampledCumulativeCounter globalServerMapGetSnapshotRequestsCounter = (SampledCumulativeCounter) this.sampledCounterManager
        .createCounter(sampledCumulativeCounterConfig);

    final ServerTransactionFactory serverTransactionFactory = new ServerTransactionFactory(thisServerNodeID);
    toInit.add(serverTransactionFactory);

    // cache server event related objects
    final InClusterServerEventBuffer serverEventbuffer = new InClusterServerEventBuffer();
    final ClientChannelMonitorImpl clientChannelMonitorImpl = new ClientChannelMonitorImpl(channelManager,
                                                                                           serverTransactionFactory);
    toInit.add(clientChannelMonitorImpl);

    final TransactionStore transactionStore = new TransactionStoreImpl(transactionPersistor,
                                                                       globalTransactionIDSequence);
    final Stage lwmCallbackStage = stageManager.createStage(ServerConfigurationContext.LOW_WATERMARK_CALLBACK_STAGE,
                                                            new LowWaterMarkCallbackHandler(), 1, maxStageSize);
    final ServerGlobalTransactionManager gtxm = new ServerGlobalTransactionManagerImpl(sequenceValidator, transactionStore,
                                                                                       gidSequenceProvider, globalTransactionIDSequence,
                                                                                       lwmCallbackStage.getSink(),
                                                                                       persistor.getPersistenceTransactionProvider(), serverEventbuffer);

    registerForDump(new CallbackDumpAdapter(gtxm));

    final TransactionalStagesCoordinatorImpl txnStageCoordinator = new TransactionalStagesCoordinatorImpl(stageManager);
    this.txnObjectManager = new TransactionalObjectManagerImpl(this.objectManager, gtxm, txnStageCoordinator);

    final CallbackDumpAdapter txnObjMgrDumpAdapter = new CallbackDumpAdapter(this.txnObjectManager);
    this.dumpHandler.registerForDump(txnObjMgrDumpAdapter);

    this.metaDataManager = this.serverBuilder.createMetaDataManager(searchEventSink);

    this.transactionManager = new ServerTransactionManagerImpl(gtxm, this.lockManager, this.clientStateManager,
                                                               this.objectManager, this.txnObjectManager, taa,
                                                               globalTxnCounter, channelStats,
                                                               new ServerTransactionManagerConfig(TCPropertiesImpl.getProperties()),
                                                               this.objectStatsRecorder, this.metaDataManager,
                                                               resentTransactionSequencer);

    this.metaDataManager.setTransactionManager(transactionManager);
    transactionManager.addTransactionListener(serverTransactionFactory);

    final CallbackDumpAdapter txnMgrDumpAdapter = new CallbackDumpAdapter(this.transactionManager);
    this.dumpHandler.registerForDump(txnMgrDumpAdapter);

    final ServerClusterMetaDataManager clusterMetaDataManager = new ServerClusterMetaDataManagerImpl(
                                                                                                     logger,
                                                                                                     this.clientStateManager,
                                                                                                     this.objectManager,
                                                                                                     channelManager);

    stageManager.createStage(ServerConfigurationContext.TRANSACTION_LOOKUP_STAGE, new TransactionLookupHandler(), 1,
        maxStageSize);

    final Stage processTx = stageManager.createStage(ServerConfigurationContext.PROCESS_TRANSACTION_STAGE,
                                                     new ProcessTransactionHandler(transactionBatchManager), 1,
                                                     maxStageSize);

    final Stage rootRequest = stageManager.createStage(ServerConfigurationContext.MANAGED_ROOT_REQUEST_STAGE,
                                                       new RequestRootHandler(), 1, maxStageSize);

    final InvalidateObjectManagerImpl invalidateObjMgr = new InvalidateObjectManagerImpl(transactionManager);
    toInit.add(invalidateObjMgr);
    stageManager.createStage(ServerConfigurationContext.INVALIDATE_OBJECTS_STAGE,
                             new InvalidateObjectsHandler(invalidateObjMgr, channelManager), 8, maxStageSize);
    stageManager.createStage(ServerConfigurationContext.VALIDATE_OBJECTS_STAGE,
                             new ValidateObjectsHandler(invalidateObjMgr, objectManager, objectStore), 1, maxStageSize);

    final BroadcastChangeHandler broadcastChangeHandler = new BroadcastChangeHandler(broadcastCounter,
                                                                                     this.objectStatsRecorder,
                                                                                     changesPerBroadcast,
                                                                                     invalidateObjMgr);
    stageManager.createStage(ServerConfigurationContext.BROADCAST_CHANGES_STAGE, broadcastChangeHandler, 1,
        maxStageSize);
    final Stage requestLock = stageManager.createStage(ServerConfigurationContext.REQUEST_LOCK_STAGE,
                                                       new RequestLockUnLockHandler(), stageWorkerThreadCount, 1,
                                                       maxStageSize);
    final ChannelLifeCycleHandler channelLifeCycleHandler = new ChannelLifeCycleHandler(this.communicationsManager,
                                                                                        transactionBatchManager,
                                                                                        channelManager, this.haConfig);
    stageManager.createStage(ServerConfigurationContext.CHANNEL_LIFE_CYCLE_STAGE, channelLifeCycleHandler, 1,
        maxStageSize);
    channelManager.addEventListener(channelLifeCycleHandler);
    channelManager.addEventListener(new ClientChannelOperatorEventlistener());

    final Stage objectRequestStage = stageManager
        .createStage(ServerConfigurationContext.MANAGED_OBJECT_REQUEST_STAGE,
            new ManagedObjectRequestHandler(globalObjectFaultCounter),
            tcProperties.getInt(TCPropertiesConsts.L2_SEDA_MANAGEDOBJECTREQUESTSTAGE_THREADS,
                stageWorkerThreadCount), 1, maxStageSize);
    final Stage respondToObjectRequestStage = stageManager
        .createStage(ServerConfigurationContext.RESPOND_TO_OBJECT_REQUEST_STAGE, new RespondToObjectRequestHandler(),
            tcProperties.getInt(TCPropertiesConsts.L2_SEDA_MANAGEDOBJECTRESPONSESTAGE_THREADS,
                stageWorkerThreadCount), maxStageSize);

    final Stage serverMapRequestStage = stageManager
        .createStage(ServerConfigurationContext.SERVER_MAP_REQUEST_STAGE,
                     new ServerMapRequestHandler(globalServerMapGetSizeRequestsCounter,
                                                 globalServerMapGetValueRequestsCounter,
                                                 globalServerMapGetSnapshotRequestsCounter), 8, maxStageSize);
    final Stage respondToServerTCMapStage = stageManager
        .createStage(ServerConfigurationContext.SERVER_MAP_RESPOND_STAGE, new RespondToServerMapRequestHandler(), 32,
                     maxStageSize);
    
    final Stage prefetchStage = stageManager
        .createStage(ServerConfigurationContext.SERVER_MAP_PREFETCH_STAGE, new ServerMapPrefetchObjectHandler(globalObjectFaultCounter), 8,
                     maxStageSize);

    this.searchRequestManager = this.serverBuilder.createSearchRequestManager(channelManager,
                                                                              objectRequestStage.getSink(), taskRunner);
    toInit.add(this.searchRequestManager);

    this.serverMapRequestManager = this.serverBuilder
        .createServerMapRequestManager(this.objectManager, channelManager, respondToServerTCMapStage.getSink(),prefetchStage.getSink(),
                                        this.clientStateManager, channelStats);
    this.dumpHandler.registerForDump(new CallbackDumpAdapter(this.serverMapRequestManager));

    resourceManager = new ResourceManagerImpl(channelManager, haConfig.getThisGroupID());
    dumpHandler.registerForDump(new CallbackDumpAdapter(resourceManager));
    channelManager.addEventListener(resourceManager);

    this.serverMapEvictor = new ProgressiveEvictionManager(objectManager, persistor.getMonitoredResources(),
                                                           objectStore, clientObjectReferenceSet,
                                                           serverTransactionFactory, threadGroup, resourceManager,
                                                           sampledCounterManager, evictionTransactionPersistor, hybrid, restartable);

    toInit.add(this.serverMapEvictor);
    this.dumpHandler.registerForDump(new CallbackDumpAdapter(this.serverMapEvictor));
    stageManager.createStage(ServerConfigurationContext.SERVER_MAP_EVICTION_PROCESSOR_STAGE,
                             new ServerMapEvictionHandler(this.serverMapEvictor), 8, TCPropertiesImpl.getProperties()
                                 .getInt(TCPropertiesConsts.L2_SEDA_EVICTION_PROCESSORSTAGE_SINK_SIZE));
    
    // Lookup stage should never be blocked trying to add to apply stage
    int applyStageThreads = L2Utils.getOptimalApplyStageWorkerThreads(restartable || hybrid);
    stageManager.createStage(ServerConfigurationContext.APPLY_CHANGES_STAGE,
                             new ApplyTransactionChangeHandler(instanceMonitor, this.transactionManager, this.serverMapEvictor,
                             persistor.getPersistenceTransactionProvider(), taskRunner, serverEventbuffer, clientChannelMonitorImpl),
                             applyStageThreads, 1, -1);

    txnStageCoordinator.lookUpSinks();
    
    this.objectRequestManager = this.serverBuilder.createObjectRequestManager(this.objectManager, channelManager,
                                                                              this.clientStateManager,
                                                                              this.transactionManager,
                                                                              objectRequestStage.getSink(),
                                                                              respondToObjectRequestStage.getSink(),
                                                                              this.objectStatsRecorder, toInit,
                                                                              stageManager, maxStageSize, this);
    this.dumpHandler.registerForDump(new CallbackDumpAdapter(this.objectRequestManager));

    final ObjectIDSequence objectIDSequence = persistor.getManagedObjectPersistor().getObjectIDSequence();
    final Stage oidRequest = stageManager.createStage(ServerConfigurationContext.OBJECT_ID_BATCH_REQUEST_STAGE,
                                                      new RequestObjectIDBatchHandler(objectIDSequence), 1,
                                                      maxStageSize);
    final Stage transactionAck = stageManager.createStage(ServerConfigurationContext.TRANSACTION_ACKNOWLEDGEMENT_STAGE,
                                                          new TransactionAcknowledgementHandler(), 1, maxStageSize);
    final Stage clientHandshake = stageManager.createStage(ServerConfigurationContext.CLIENT_HANDSHAKE_STAGE,
                                                           createHandShakeHandler(), 1, maxStageSize);
    this.hydrateStage = stageManager.createStage(ServerConfigurationContext.HYDRATE_MESSAGE_SINK, new HydrateHandler(),
                                                 stageWorkerThreadCount, 1, maxStageSize);
    final Stage txnLwmStage = stageManager.createStage(ServerConfigurationContext.TRANSACTION_LOWWATERMARK_STAGE,
                                                       new TransactionLowWaterMarkHandler(gtxm), 1, maxStageSize);

    ClientConnectEventHandler clientConnectEventHandler = new ClientConnectEventHandler();
    final Stage jmxRemoteConnectStage = stageManager.createStage(ServerConfigurationContext.JMXREMOTE_CONNECT_STAGE,
                                                                 clientConnectEventHandler, 1, maxStageSize);
    final Stage jmxRemoteDisconnectStage = stageManager
        .createStage(ServerConfigurationContext.JMXREMOTE_DISCONNECT_STAGE, clientConnectEventHandler, 1, maxStageSize);

    cteh.setStages(jmxRemoteConnectStage.getSink(), jmxRemoteDisconnectStage.getSink());
    final Stage jmxRemoteTunnelStage = stageManager.createStage(ServerConfigurationContext.JMXREMOTE_TUNNEL_STAGE,
        cteh, 1, maxStageSize);

    final Stage clusterMetaDataStage = stageManager.createStage(ServerConfigurationContext.CLUSTER_METADATA_STAGE,
        new ServerClusterMetaDataHandler(), 1, maxStageSize);

    ServerManagementHandler serverManagementHandler = new ServerManagementHandler();
    final Stage managementStage = stageManager.createStage(ServerConfigurationContext.MANAGEMENT_STAGE,
                                                           serverManagementHandler, TCPropertiesImpl.getProperties()
                                                               .getInt(TCPropertiesConsts.L2_REMOTEJMX_MAXTHREADS) / 2,
                                                           maxStageSize);

    initRouteMessages(messageRouter, processTx, rootRequest, requestLock, objectRequestStage, oidRequest,
                      transactionAck, clientHandshake, txnLwmStage, jmxRemoteTunnelStage, clusterMetaDataStage,
                      managementStage, serverMapRequestStage, searchQueryRequestStage);

    long reconnectTimeout = l2DSOConfig.clientReconnectWindow();

    HASettingsChecker haChecker = new HASettingsChecker(configSetupManager, TCPropertiesImpl.getProperties());
    haChecker.validateHealthCheckSettingsForHighAvailability();

    logger.debug("Client Reconnect Window: " + reconnectTimeout + " seconds");
    reconnectTimeout *= 1000;
    final ServerClientHandshakeManager clientHandshakeManager = new ServerClientHandshakeManager(
                                                                                                 TCLogging
                                                                                                     .getLogger(ServerClientHandshakeManager.class),
                                                                                                 channelManager,
                                                                                                 this.transactionManager,
                                                                                                 transactionBatchManager,
                                                                                                 sequenceValidator,
                                                                                                 this.clientStateManager,
                                                                                                 invalidateObjMgr,
                                                                                                 this.lockManager,
                                                                                                 this.serverMapEvictor,
                                                                                                 stageManager
                                                                                                     .getStage(ServerConfigurationContext.OBJECT_ID_BATCH_REQUEST_STAGE)
                                                                                                     .getSink(),
                                                                                                 new Timer(
                                                                                                           "Reconnect timer",
                                                                                                           true),
                                                                                                 reconnectTimeout,
                                                                                                 restartable,
                                                                                                 consoleLogger);

    this.groupCommManager = this.serverBuilder.createGroupCommManager(this.configSetupManager, stageManager,
                                                                      this.thisServerNodeID, this.httpSink,
                                                                      this.stripeIDStateManager, gtxm);

    this.dumpHandler.registerForDump(new CallbackDumpAdapter(this.groupCommManager));
    // initialize the garbage collector
    final MutableSequence dgcSequence = persistor.getSequenceManager()
        .getSequence(SequenceNames.DGC_SEQUENCE_NAME.getName(), 1L);
    final DGCSequenceProvider dgcSequenceProvider = new DGCSequenceProvider(dgcSequence);
    final GarbageCollector gc = new MarkAndSweepGarbageCollector(objectManagerConfig, this.objectManager,
            this.clientStateManager, gcPublisher, dgcSequenceProvider);
    gc.addListener(getGcStatsEventPublisher());
    gc.addListener(new DGCOperatorEventPublisher());

    this.objectManager.setGarbageCollector(gc);
    this.l2Management.findObjectManagementMonitorMBean().registerGCController(new GCControllerImpl(this.objectManager
                                                                                  .getGarbageCollector()));
    this.l2Management.findObjectManagementMonitorMBean().registerObjectIdFetcher(new ObjectIdsFetcher() {
      @Override
      public Set getAllObjectIds() {
        return DistributedObjectServer.this.objectManager.getAllObjectIDs();
      }
    });

    final WeightGeneratorFactory weightGeneratorFactory = new ZapNodeProcessorWeightGeneratorFactory(
                                                                                                     channelManager,
                                                                                                     transactionBatchManager,
                                                                                                     this.transactionManager,
                                                                                                     host, serverPort);
    this.indexHACoordinator = this.serverBuilder.createIndexHACoordinator(this.configSetupManager, searchEventSink,
        persistor.getStorageManager());

    SequenceGenerator indexSequenceGenerator = new SequenceGenerator();

    L2IndexStateManager l2IndexStateManager = this.serverBuilder.createL2IndexStateManager(this.indexHACoordinator,
                                                                                           this.transactionManager,
                                                                                           indexSequenceGenerator,
                                                                                           groupCommManager);

    L2ObjectStateManager l2ObjectStateManager = this.serverBuilder.createL2ObjectStateManager(objectManager,
                                                                                              transactionManager);

    L2PassiveSyncStateManager l2PassiveSyncStateManager = this.serverBuilder
        .createL2PassiveSyncStateManager(l2IndexStateManager, l2ObjectStateManager,
                                         createStateSyncManager(this.indexHACoordinator));

    this.l2Coordinator = this.serverBuilder.createL2HACoordinator(consoleLogger, this, stageManager,
                                                                  this.groupCommManager, this.persistor
                                                                      .getClusterStatePersistor(),
                                                                  l2PassiveSyncStateManager, l2ObjectStateManager,
                                                                  l2IndexStateManager, this.objectManager,
                                                                  this.indexHACoordinator, this.transactionManager,
                                                                  gtxm, weightGeneratorFactory,
                                                                  this.configSetupManager, recycler,
                                                                  this.stripeIDStateManager, serverTransactionFactory,
                                                                  dgcSequenceProvider, indexSequenceGenerator,
                                                                  objectIDSequence, l2DSOConfig.getDataStorage(),
                                                                  configSetupManager.getActiveServerGroupForThisL2()
                                                                      .getElectionTimeInSecs(), haConfig
                                                                      .getNodesStore());
    this.l2Coordinator.getStateManager().registerForStateChangeEvents(this.l2State);
    this.l2Coordinator.getStateManager().registerForStateChangeEvents(this.indexHACoordinator);
    this.l2Coordinator.getStateManager().registerForStateChangeEvents(this.l2Coordinator);
    this.l2Coordinator.getStateManager().registerForStateChangeEvents(this.garbageCollectionManager);
    dgcSequenceProvider.registerSequecePublisher(this.l2Coordinator.getReplicatedClusterStateManager());
    this.dumpHandler.registerForDump(new CallbackDumpAdapter(this.l2Coordinator));

    backupManager = serverBuilder.createBackupManager(persistor, indexHACoordinator, configSetupManager.commonl2Config()
          .serverDbBackupPath(), stageManager, restartable, transactionManager);

    final DSOGlobalServerStatsImpl serverStats = new DSOGlobalServerStatsImpl(globalObjectFaultCounter,
                                                                              globalTxnCounter, objMgrStats,
                                                                              broadcastCounter,
                                                                              globalLockRecallCounter,
                                                                              changesPerBroadcast,
                                                                              transactionSizeCounter, globalLockCount,
                                                                              serverMapEvictor.getEvictionStatistics(),
                                                                              serverMapEvictor
                                                                                  .getExpirationStatistics(),
                                                                              globalOperationCounter);

    serverStats.serverMapGetSizeRequestsCounter(globalServerMapGetSizeRequestsCounter)
        .serverMapGetValueRequestsCounter(globalServerMapGetValueRequestsCounter)
        .serverMapGetSnapshotRequestsCounter(globalServerMapGetSnapshotRequestsCounter);

    this.context = this.serverBuilder.createServerConfigurationContext(stageManager, this.objectManager,
                                                                       this.objectRequestManager,
                                                                       this.serverMapRequestManager, this.objectStore,
                                                                       this.lockManager, channelManager,
                                                                       this.clientStateManager,
                                                                       this.transactionManager, this.txnObjectManager,
                                                                       channelStats, this.l2Coordinator,
                                                                       transactionBatchManager, gtxm,
                                                                       clientHandshakeManager, clusterMetaDataManager,
                                                                       serverStats, this.connectionIdFactory,
                                                                       maxStageSize,
                                                                       this.l1Listener.getChannelManager(), this,
                                                                       metaDataManager, indexHACoordinator,
                                                                       searchRequestManager, garbageCollectionManager);
    toInit.add(this.serverBuilder);

    stageManager.startAll(this.context, toInit);

    final RemoteManagement remoteManagement = new RemoteManagementImpl(channelManager, serverManagementHandler, haConfig.getNodesStore().getServerNameFromNodeName(thisServerNodeID.getName()));
    TerracottaRemoteManagement.setRemoteManagementInstance(remoteManagement);
    TerracottaOperatorEventLogging.getEventLogger().registerEventCallback(new TerracottaOperatorEventCallback() {
      @Override
      public void logOperatorEvent(TerracottaOperatorEvent event) {
        TSAManagementEventPayload payload = new TSAManagementEventPayload("TSA.OPERATOR_EVENT." + event.getEventTypeAsString());

        payload.getAttributes().put("OperatorEvent.CollapseString", event.getCollapseString());
        payload.getAttributes().put("OperatorEvent.EventLevel", event.getEventLevelAsString());
        payload.getAttributes().put("OperatorEvent.EventMessage", event.getEventMessage());
        payload.getAttributes().put("OperatorEvent.EventSubsystem", event.getEventSubsystemAsString());
        payload.getAttributes().put("OperatorEvent.EventType", event.getEventTypeAsString());
        payload.getAttributes().put("OperatorEvent.EventTime", event.getEventTime().getTime());
        payload.getAttributes().put("OperatorEvent.NodeName", event.getNodeName());

        remoteManagement.sendEvent(payload.toManagementEvent());
      }
    });

    // XXX: yucky casts
    this.managementContext = new ServerManagementContext(this.transactionManager, this.objectRequestManager,
                                                         this.lockManager, (DSOChannelManagerMBean) channelManager,
                                                         serverStats, channelStats, instanceMonitor,
                                                         indexHACoordinator, connectionPolicy, remoteManagement);
    if (tcProperties.getBoolean(TCPropertiesConsts.L2_BEANSHELL_ENABLED)) {
      startBeanShell(tcProperties.getInt(TCPropertiesConsts.L2_BEANSHELL_PORT));
    }

    final CallbackOnExitHandler handler = new CallbackGroupExceptionHandler(logger, consoleLogger);
    this.threadGroup.addCallbackOnExitExceptionHandler(GroupException.class, handler);

    startGroupManagers();
    this.l2Coordinator.start();
    setLoggerOnExit();
  }

  protected CommunicationsManagerImpl createCommunicationsManager(TCMessageRouter messageRouter) {
    final int commWorkerThreadCount = L2Utils.getOptimalCommWorkerThreads();

    final NetworkStackHarnessFactory networkStackHarnessFactory;
    final boolean useOOOLayer = this.l1ReconnectConfig.getReconnectEnabled();
    if (useOOOLayer) {
      networkStackHarnessFactory = new OOONetworkStackHarnessFactory(
          new OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl(),
          this.l1ReconnectConfig);
    } else {
      networkStackHarnessFactory = new PlainNetworkStackHarnessFactory();
    }

    final MessageMonitor mm = MessageMonitorImpl.createMonitor(TCPropertiesImpl.getProperties(), logger);

    return new CommunicationsManagerImpl(CommunicationsManager.COMMSMGR_SERVER, mm,
                                                               messageRouter, networkStackHarnessFactory,
                                                               this.connectionPolicy, commWorkerThreadCount,
                                                               new HealthCheckerConfigImpl(tcProperties
                                                                   .getPropertiesFor("l2.healthcheck.l1"), "TSA Server"),
                                                               this.thisServerNodeID,
                                                               new TransportHandshakeErrorNullHandler(),
                                                               getMessageTypeClassMappings(), Collections.EMPTY_MAP,
                                                               tcSecurityManager);
  }

  /**
   * Counts map operations using events from {@link ConcurrentDistributedServerMapManagedObjectState}.
   * 
   * @see EventBus
   */
  public static final class OperationCountIncrementEventListener {
    private final SampledCounter counter;

    private OperationCountIncrementEventListener(final SampledCounter counter) {
      this.counter = counter;
    }

    @Subscribe
    public void writeOperationCountEvent(final Events.WriteOperationCountChangeEvent event) {
      if (event.getSource().getNodeType() == NodeID.CLIENT_NODE_TYPE) {
        // Only count client events
        this.counter.increment(event.getDelta());
      }
    }
  }

  protected StateSyncManager createStateSyncManager(IndexHACoordinator coordinator) {
    return new StateSyncManagerImpl();
  }

  public void startGroupManagers() {
    try {

      final NodeID myNodeId = this.groupCommManager.join(this.haConfig.getThisNode(), this.haConfig.getNodesStore());
      logger.info("This L2 Node ID = " + myNodeId);
    } catch (final GroupException e) {
      logger.error("Caught Exception :", e);
      throw new RuntimeException(e);
    }
  }

  public void reloadConfiguration() throws ConfigurationSetupException {
    if (false) { throw new ConfigurationSetupException(); }
    throw new UnsupportedOperationException();
  }

  protected void initRouteMessages(TCMessageRouter messageRouter, final Stage processTx, final Stage rootRequest,
                                   final Stage requestLock, final Stage objectRequestStage, final Stage oidRequest,
                                   final Stage transactionAck, final Stage clientHandshake, final Stage txnLwmStage,
                                   final Stage jmxRemoteTunnelStage, final Stage clusterMetaDataStage, final Stage managementStage,
                                   final Stage serverMapRequestStage, final Stage searchQueryRequestStage) {
    final Sink hydrateSink = this.hydrateStage.getSink();
    messageRouter.routeMessageType(TCMessageType.COMMIT_TRANSACTION_MESSAGE, processTx.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.LOCK_REQUEST_MESSAGE, requestLock.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.REQUEST_ROOT_MESSAGE, rootRequest.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.REQUEST_MANAGED_OBJECT_MESSAGE, objectRequestStage.getSink(),
                                   hydrateSink);
    messageRouter.routeMessageType(TCMessageType.OBJECT_ID_BATCH_REQUEST_MESSAGE, oidRequest.getSink(), hydrateSink);
    messageRouter
        .routeMessageType(TCMessageType.ACKNOWLEDGE_TRANSACTION_MESSAGE, transactionAck.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.CLIENT_HANDSHAKE_MESSAGE, clientHandshake.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE, jmxRemoteTunnelStage.getSink(),
        hydrateSink);
    messageRouter.routeMessageType(TCMessageType.CLIENT_JMX_READY_MESSAGE, jmxRemoteTunnelStage.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.TUNNELED_DOMAINS_CHANGED_MESSAGE, jmxRemoteTunnelStage.getSink(),
                                   hydrateSink);
    messageRouter.routeMessageType(TCMessageType.COMPLETED_TRANSACTION_LOWWATERMARK_MESSAGE, txnLwmStage.getSink(),
                                   hydrateSink);
    messageRouter.routeMessageType(TCMessageType.NODES_WITH_OBJECTS_MESSAGE, clusterMetaDataStage.getSink(),
                                   hydrateSink);
    messageRouter.routeMessageType(TCMessageType.NODES_WITH_KEYS_MESSAGE, clusterMetaDataStage.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.KEYS_FOR_ORPHANED_VALUES_MESSAGE, clusterMetaDataStage.getSink(),
                                   hydrateSink);
    messageRouter.routeMessageType(TCMessageType.NODE_META_DATA_MESSAGE, clusterMetaDataStage.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.GET_VALUE_SERVER_MAP_REQUEST_MESSAGE, serverMapRequestStage.getSink(),
                                   hydrateSink);
    messageRouter.routeMessageType(TCMessageType.GET_ALL_SIZE_SERVER_MAP_REQUEST_MESSAGE,
                                   serverMapRequestStage.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.GET_ALL_KEYS_SERVER_MAP_REQUEST_MESSAGE,
                                   serverMapRequestStage.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.SEARCH_QUERY_REQUEST_MESSAGE, searchQueryRequestStage.getSink(),
                                   hydrateSink);
    messageRouter.routeMessageType(TCMessageType.SEARCH_RESULTS_REQUEST_MESSAGE, searchQueryRequestStage.getSink(),
                                   hydrateSink);
    messageRouter.routeMessageType(TCMessageType.LIST_REGISTERED_SERVICES_RESPONSE_MESSAGE, managementStage.getSink(),
                                   hydrateSink);
    messageRouter.routeMessageType(TCMessageType.INVOKE_REGISTERED_SERVICE_RESPONSE_MESSAGE, managementStage.getSink(),
                                   hydrateSink);
  }

  private HashMap<TCMessageType, Class> getMessageTypeClassMappings() {
    HashMap<TCMessageType, Class> messageTypeClassMapping = new HashMap<TCMessageType, Class>();
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
    messageTypeClassMapping.put(TCMessageType.NODES_WITH_OBJECTS_RESPONSE_MESSAGE,
                                NodesWithObjectsResponseMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.NODES_WITH_KEYS_MESSAGE, NodesWithKeysMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.NODES_WITH_KEYS_RESPONSE_MESSAGE, NodesWithKeysResponseMessageImpl.class);
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
    messageTypeClassMapping.put(TCMessageType.GET_ALL_KEYS_SERVER_MAP_REQUEST_MESSAGE,
                                GetAllKeysServerMapRequestMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.GET_ALL_KEYS_SERVER_MAP_RESPONSE_MESSAGE,
                                GetAllKeysServerMapResponseMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.GET_VALUE_SERVER_MAP_REQUEST_MESSAGE,
                                GetValueServerMapRequestMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.GET_VALUE_SERVER_MAP_RESPONSE_MESSAGE,
                                GetValueServerMapResponseMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.OBJECT_NOT_FOUND_SERVER_MAP_RESPONSE_MESSAGE,
                                ObjectNotFoundServerMapResponseMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.TUNNELED_DOMAINS_CHANGED_MESSAGE, TunneledDomainsChanged.class);
    messageTypeClassMapping.put(TCMessageType.SEARCH_QUERY_REQUEST_MESSAGE, SearchQueryRequestMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.SEARCH_QUERY_RESPONSE_MESSAGE, SearchQueryResponseMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.SEARCH_RESULTS_REQUEST_MESSAGE, SearchResultsRequestMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.SEARCH_RESULTS_RESPONSE_MESSAGE, SearchResultsResponseMessageImpl.class);

    messageTypeClassMapping.put(TCMessageType.INVALIDATE_OBJECTS_MESSAGE, InvalidateObjectsMessage.class);
    messageTypeClassMapping.put(TCMessageType.RESOURCE_MANAGER_THROTTLE_STATE_MESSAGE,
                                ResourceManagerThrottleMessage.class);

    messageTypeClassMapping.put(TCMessageType.LIST_REGISTERED_SERVICES_MESSAGE, ListRegisteredServicesMessage.class);
    messageTypeClassMapping.put(TCMessageType.LIST_REGISTERED_SERVICES_RESPONSE_MESSAGE, ListRegisteredServicesResponseMessage.class);
    messageTypeClassMapping.put(TCMessageType.INVOKE_REGISTERED_SERVICE_MESSAGE, InvokeRegisteredServiceMessage.class);
    messageTypeClassMapping.put(TCMessageType.INVOKE_REGISTERED_SERVICE_RESPONSE_MESSAGE, InvokeRegisteredServiceResponseMessage.class);

    return messageTypeClassMapping;
  }

  protected TCLogger getLogger() {
    return logger;
  }

  private ServerID makeServerNodeID(final L2DSOConfig l2DSOConfig) {
    String host = l2DSOConfig.tsaGroupPort().getBind();
    if (TCSocketAddress.WILDCARD_IP.equals(host)) {
      host = l2DSOConfig.host();
    }
    final Node node = new Node(host, l2DSOConfig.tsaPort().getIntValue());
    final ServerID aNodeID = new ServerID(node.getServerNodeName(), UUID.getUUID().toString().getBytes());
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

  private void setLoggerOnExit() {
    CommonShutDownHook.addShutdownHook(new Runnable() {
      @Override
      public void run() {
        logger.info("L2 Exiting...");
      }
    });
  }

  public boolean isBlocking() {
    return this.startupLock != null && this.startupLock.isBlocked();
  }

  public void startActiveMode() {
    this.transactionManager.goToActiveMode();
  }

  public void startL1Listener() throws IOException {
    final Set existingConnections = Collections.unmodifiableSet(this.connectionIdFactory.loadConnectionIDs());
    this.context.getClientHandshakeManager().setStarting(existingConnections);
    this.l1Listener.start(existingConnections);
    if (!existingConnections.isEmpty()) {
      this.context.getClientHandshakeManager().startReconnectWindow();
    }
    consoleLogger.info("Terracotta Server instance has started up as ACTIVE node on " + format(this.l1Listener)
                       + " successfully, and is now ready for work.");
  }

  private static String format(final NetworkListener listener) {
    final StringBuilder sb = new StringBuilder(listener.getBindAddress().getHostAddress());
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
      final Interpreter i = new Interpreter();
      i.set("dsoServer", this);
      i.set("objectManager", this.objectManager);
      i.set("txnObjectManager", this.txnObjectManager);
      i.set("portnum", port);
      i.eval("setAccessibility(true)"); // turn off access restrictions
      i.eval("server(portnum)");
      consoleLogger.info("Bean shell is started on port " + port);
    } catch (final EvalError e) {
      e.printStackTrace();
    }
  }

  /**
   * Since this is accessed via JMX and l1Listener isn't initialed when a secondary is waiting on the lock file, use the
   * config value unless the special value 0 is specified for use in the tests to get a random port.
   */
  public int getListenPort() {
    final L2DSOConfig l2DSOConfig = this.configSetupManager.dsoL2Config();
    final int configValue = l2DSOConfig.tsaPort().getIntValue();
    if (configValue != 0) { return configValue; }
    if (this.l1Listener != null) {
      try {
        return this.l1Listener.getBindPort();
      } catch (final IllegalStateException ise) {/**/
      }
    }
    return -1;
  }

  public InetAddress getListenAddr() {
    return this.l1Listener.getBindAddress();
  }

  public int getGroupPort() {
    final L2DSOConfig l2DSOConfig = this.configSetupManager.dsoL2Config();
    final int configValue = l2DSOConfig.tsaGroupPort().getIntValue();
    if (configValue != 0) { return configValue; }
    return -1;
  }

  public int getManagementPort() {
    final L2DSOConfig l2DSOConfig = this.configSetupManager.dsoL2Config();
    final int configValue = l2DSOConfig.managementPort().getIntValue();
    if (configValue != 0) { return configValue; }
    return -1;
  }

  public synchronized void stop() {
    try {
      if (this.indexHACoordinator != null) {
        this.indexHACoordinator.shutdown();
      }
    } catch (Throwable t) {
      logger.warn(t);
    }

    this.seda.getStageManager().stopAll();

    if (this.l1Listener != null) {
      try {
        this.l1Listener.stop(5000);
      } catch (final TCTimeoutException e) {
        logger.warn("timeout trying to stop listener: " + e.getMessage());
      }
    }

    if ((this.communicationsManager != null)) {
      this.communicationsManager.shutdown();
    }

    if (this.objectManager != null) {
      try {
        this.objectManager.stop();
      } catch (final Throwable e) {
        logger.error(e);
      }
    }

    try {
      this.objectStore.shutdown();
    } catch (final Throwable e) {
      logger.warn(e);
    }

    try {
      this.persistor.close();
    } catch (final Exception e) {
      logger.warn(e);
    }

    if (this.sampledCounterManager != null) {
      try {
        this.sampledCounterManager.shutdown();
      } catch (final Exception e) {
        logger.error(e);
      }
    }

    basicStop();
  }

  public void quickStop() {
    basicStop();
  }

  private void basicStop() {
    try {
      stopJMXServer();
    } catch (final Throwable t) {
      logger.error("Error shutting down jmx server", t);
    }

    TerracottaRemoteManagement.setRemoteManagementInstance(null);

    if (this.startupLock != null) {
      this.startupLock.release();
    }
  }

  public ConnectionIDFactory getConnectionIdFactory() {
    return this.connectionIdFactory;
  }

  public PersistentManagedObjectStore getManagedObjectStore() {
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

  public GCStatsEventPublisher getGcStatsEventPublisher() {
    return this.gcStatsEventPublisher;
  }

  public TerracottaOperatorEventHistoryProvider getOperatorEventsHistoryProvider() {
    return this.operatorEventHistoryProvider;
  }

  private void startJMXServer(boolean listenerEnabled, final InetAddress bind, int jmxPort,
                              final Sink remoteEventsSink,
                              final ServerDBBackupMBean serverDBBackupMBean) throws Exception {
    if (jmxPort == 0) {
      jmxPort = new PortChooser().chooseRandomPort();
    }

    this.l2Management = this.serverBuilder.createL2Management(listenerEnabled, this.tcServerInfoMBean,
                                                              this.configSetupManager, this,
                                                              bind, jmxPort, remoteEventsSink, this,
                                                              serverDBBackupMBean);

    this.l2Management.start();
  }

  private void stopJMXServer() throws Exception {
    try {
      if (this.l2Management != null) {
        this.l2Management.stop();
      }
    } finally {
      this.l2Management = null;
    }
  }

  public OffheapStats getOffheapStats() {
    Collection<MonitoredResource> list = persistor.getMonitoredResources();
    for (MonitoredResource rsrc : list) {
      if (rsrc.getType() == MonitoredResource.Type.OFFHEAP) { return new OffheapStatsImpl(rsrc); }
    }
    return null;
  }

  public StorageDataStats getStorageStats() {
    return new StorageDataStatsImpl(persistor.getMonitoredResources());
  }

  public ReconnectConfig getL1ReconnectProperties() {
    return this.l1ReconnectConfig;
  }

  @Override
  public void addAllLocksTo(final LockInfoByThreadID lockInfo) {
    // this feature not implemented for server. DEV-1949
  }

  @Override
  public ThreadIDMap getThreadIDMap() {
    return new NullThreadIDMapImpl();
  }

  protected GroupManager getGroupManager() {
    return this.groupCommManager;
  }

  @Override
  public void registerForDump(final CallbackDumpAdapter dumpAdapter) {
    this.dumpHandler.registerForDump(dumpAdapter);
  }

  @Override
  public boolean isAlive(final String name) {
    throw new UnsupportedOperationException();
  }

  protected ClientHandshakeHandler createHandShakeHandler() {
    return new ClientHandshakeHandler(this.configSetupManager.dsoL2Config().serverName());
  }

  // for tests only
  public CommunicationsManager getCommunicationsManager() {
    return communicationsManager;
  }

  public void dumpClusterState() {
    try {
      L2DumperMBean mbean = (L2DumperMBean) l2Management.findMBean(L2MBeanNames.DUMPER, L2DumperMBean.class);
      mbean.dumpClusterState();
    } catch (Exception e) {
      logger.warn("Could not take Cluster dump, hence taking server dump only");
      dump();
    }
  }

  public BackupManager getBackupManager() {
    return backupManager;
  }

  public ResourceManager getResourceManager() {
    return resourceManager;
  }
}
