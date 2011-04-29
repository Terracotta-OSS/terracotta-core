/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import org.apache.commons.io.FileUtils;

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
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.exception.CleanDirtyDatabaseException;
import com.tc.exception.TCRuntimeException;
import com.tc.exception.ZapDirtyDbServerNodeException;
import com.tc.exception.ZapServerNodeException;
import com.tc.handler.CallbackDatabaseDirtyAlertAdapter;
import com.tc.handler.CallbackDirtyDatabaseExceptionAdapter;
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
import com.tc.l2.ha.L2HADisabledCooridinator;
import com.tc.l2.ha.StripeIDStateManagerImpl;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.l2.ha.ZapNodeProcessorWeightGeneratorFactory;
import com.tc.l2.objectserver.L2IndexStateManager;
import com.tc.l2.objectserver.L2ObjectStateManager;
import com.tc.l2.objectserver.L2PassiveSyncStateManager;
import com.tc.l2.objectserver.ServerTransactionFactory;
import com.tc.l2.state.StateManager;
import com.tc.l2.state.StateSyncManager;
import com.tc.l2.state.StateSyncManagerImpl;
import com.tc.lang.TCThreadGroup;
import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CustomerLogging;
import com.tc.logging.DumpHandlerStore;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.logging.TerracottaOperatorEventLogging;
import com.tc.logging.ThreadDumpHandler;
import com.tc.management.L2LockStatsManager;
import com.tc.management.L2Management;
import com.tc.management.RemoteJMXProcessor;
import com.tc.management.beans.L2State;
import com.tc.management.beans.LockStatisticsMonitor;
import com.tc.management.beans.TCDumper;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.management.beans.object.ObjectManagementMonitor.ObjectIdsFetcher;
import com.tc.management.beans.object.ServerDBBackupMBean;
import com.tc.management.lock.stats.L2LockStatisticsManagerImpl;
import com.tc.management.lock.stats.LockStatisticsMessage;
import com.tc.management.lock.stats.LockStatisticsResponseMessageImpl;
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
import com.tc.object.cache.CacheConfig;
import com.tc.object.cache.CacheConfigImpl;
import com.tc.object.cache.CacheManager;
import com.tc.object.cache.EvictionPolicy;
import com.tc.object.cache.LFUConfigImpl;
import com.tc.object.cache.LFUEvictionPolicy;
import com.tc.object.cache.LRUEvictionPolicy;
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
import com.tc.object.net.ChannelStatsImpl;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerImpl;
import com.tc.object.net.DSOChannelManagerMBean;
import com.tc.object.session.NullSessionManager;
import com.tc.object.session.SessionManager;
import com.tc.objectserver.DSOApplicationEvents;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.api.ObjectStatsManager;
import com.tc.objectserver.api.ObjectStatsManagerImpl;
import com.tc.objectserver.api.ServerMapEvictionManager;
import com.tc.objectserver.api.ServerMapRequestManager;
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
import com.tc.objectserver.handler.ClientChannelOperatorEventlistener;
import com.tc.objectserver.handler.ClientHandshakeHandler;
import com.tc.objectserver.handler.ClientLockStatisticsHandler;
import com.tc.objectserver.handler.CommitTransactionChangeHandler;
import com.tc.objectserver.handler.GarbageDisposeHandler;
import com.tc.objectserver.handler.GlobalTransactionIDBatchRequestHandler;
import com.tc.objectserver.handler.InvalidateObjectsHandler;
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
import com.tc.objectserver.handler.RespondToServerMapRequestHandler;
import com.tc.objectserver.handler.ServerClusterMetaDataHandler;
import com.tc.objectserver.handler.ServerMapCapacityEvictionHandler;
import com.tc.objectserver.handler.ServerMapEvictionBroadcastHandler;
import com.tc.objectserver.handler.ServerMapEvictionHandler;
import com.tc.objectserver.handler.ServerMapRequestHandler;
import com.tc.objectserver.handler.SyncWriteTransactionReceivedHandler;
import com.tc.objectserver.handler.TransactionAcknowledgementHandler;
import com.tc.objectserver.handler.TransactionLookupHandler;
import com.tc.objectserver.handler.TransactionLowWaterMarkHandler;
import com.tc.objectserver.handler.ValidateObjectsHandler;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.l1.impl.ClientStateManagerImpl;
import com.tc.objectserver.l1.impl.InvalidateObjectManagerImpl;
import com.tc.objectserver.l1.impl.TransactionAcknowledgeAction;
import com.tc.objectserver.l1.impl.TransactionAcknowledgeActionImpl;
import com.tc.objectserver.locks.LockManagerImpl;
import com.tc.objectserver.locks.LockManagerMBean;
import com.tc.objectserver.managedobject.ManagedObjectChangeListenerProviderImpl;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.metadata.MetaDataManager;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.persistence.api.ClientStatePersistor;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.persistence.api.Persistor;
import com.tc.objectserver.persistence.api.TransactionPersistor;
import com.tc.objectserver.persistence.api.TransactionStore;
import com.tc.objectserver.persistence.db.ConnectionIDFactoryImpl;
import com.tc.objectserver.persistence.db.CustomSerializationAdapterFactory;
import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.persistence.db.DBPersistorImpl;
import com.tc.objectserver.persistence.db.DBSequenceKeys;
import com.tc.objectserver.persistence.db.DatabaseDirtyException;
import com.tc.objectserver.persistence.db.SerializationAdapterFactory;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.persistence.db.TempSwapDBPersistorImpl;
import com.tc.objectserver.persistence.db.TransactionStoreImpl;
import com.tc.objectserver.search.IndexHACoordinator;
import com.tc.objectserver.search.SearchEventHandler;
import com.tc.objectserver.search.SearchQueryRequestMessageHandler;
import com.tc.objectserver.search.SearchRequestManager;
import com.tc.objectserver.storage.api.DBEnvironment;
import com.tc.objectserver.storage.api.DBFactory;
import com.tc.objectserver.storage.api.OffheapStats;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.tx.CommitTransactionMessageRecycler;
import com.tc.objectserver.tx.ServerTransactionManagerConfig;
import com.tc.objectserver.tx.ServerTransactionManagerImpl;
import com.tc.objectserver.tx.ServerTransactionSequencerImpl;
import com.tc.objectserver.tx.ServerTransactionSequencerStats;
import com.tc.objectserver.tx.TransactionBatchManagerImpl;
import com.tc.objectserver.tx.TransactionFilter;
import com.tc.objectserver.tx.TransactionalObjectManagerImpl;
import com.tc.objectserver.tx.TransactionalStagesCoordinatorImpl;
import com.tc.operatorevent.DsoOperatorEventHistoryProvider;
import com.tc.operatorevent.TerracottaOperatorEventHistoryProvider;
import com.tc.properties.L1ReconnectConfigImpl;
import com.tc.properties.ReconnectConfig;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.runtime.TCMemoryManagerImpl;
import com.tc.runtime.logging.LongGCLogger;
import com.tc.runtime.logging.MemoryOperatorEventListener;
import com.tc.server.ServerConnectionValidator;
import com.tc.server.TCServer;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticsAgentSubSystem;
import com.tc.statistics.StatisticsAgentSubSystemImpl;
import com.tc.statistics.StatisticsGathererSubSystem;
import com.tc.statistics.StatisticsSystemType;
import com.tc.statistics.beans.impl.StatisticsGatewayMBeanImpl;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;
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
import com.tc.statistics.retrieval.actions.SRAL2ServerMapGetSizeRequestsCount;
import com.tc.statistics.retrieval.actions.SRAL2ServerMapGetSizeRequestsRate;
import com.tc.statistics.retrieval.actions.SRAL2ServerMapGetValueRequestsCount;
import com.tc.statistics.retrieval.actions.SRAL2ServerMapGetValueRequestsRate;
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
import com.tc.stats.counter.sampled.SampledCumulativeCounter;
import com.tc.stats.counter.sampled.SampledCumulativeCounterConfig;
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
import com.tc.util.runtime.LockInfoByThreadID;
import com.tc.util.runtime.NullThreadIDMapImpl;
import com.tc.util.runtime.ThreadIDMap;
import com.tc.util.sequence.BatchSequence;
import com.tc.util.sequence.DGCSequenceProvider;
import com.tc.util.sequence.MutableSequence;
import com.tc.util.sequence.Sequence;
import com.tc.util.sequence.SequenceGenerator;
import com.tc.util.startuplock.FileNotCreatedException;
import com.tc.util.startuplock.LocationNotCreatedException;
import com.terracottatech.config.Offheap;
import com.terracottatech.config.PersistenceMode;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Timer;

import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
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
  private ManagedObjectStore                     objectStore;
  private Persistor                              persistor;
  private ServerTransactionManagerImpl           transactionManager;

  private CacheManager                           cacheManager;

  private L2Management                           l2Management;
  private L2Coordinator                          l2Coordinator;

  private TCProperties                           l2Properties;

  private ConnectionIDFactoryImpl                connectionIdFactory;

  private LockStatisticsMonitor                  lockStatisticsMBean;

  private StatisticsAgentSubSystemImpl           statisticsAgentSubSystem;
  private StatisticsGatewayMBeanImpl             statisticsGateway;
  private StatisticsGathererSubSystem            statisticsGathererSubSystem;

  private final TCThreadGroup                    threadGroup;
  private final SEDA                             seda;

  private ReconnectConfig                        l1ReconnectConfig;

  private GroupManager                           groupCommManager;
  private Stage                                  hydrateStage;
  private StripeIDStateManagerImpl               stripeIDStateManager;
  private DBEnvironment                          dbenv;
  private IndexHACoordinator                     indexHACoordinator;
  private MetaDataManager                        metaDataManager;
  private SearchRequestManager                   searchRequestManager;

  private final CallbackDumpHandler              dumpHandler      = new CallbackDumpHandler();

  // used by a test
  public DistributedObjectServer(final L2ConfigurationSetupManager configSetupManager, final TCThreadGroup threadGroup,
                                 final ConnectionPolicy connectionPolicy, final TCServerInfoMBean tcServerInfoMBean,
                                 final ObjectStatsRecorder objectStatsRecorder) {
    this(configSetupManager, threadGroup, connectionPolicy, new NullSink(), tcServerInfoMBean, objectStatsRecorder,
         new L2State(), new SEDA(threadGroup), null);

  }

  public DistributedObjectServer(final L2ConfigurationSetupManager configSetupManager, final TCThreadGroup threadGroup,
                                 final ConnectionPolicy connectionPolicy, final Sink httpSink,
                                 final TCServerInfoMBean tcServerInfoMBean,
                                 final ObjectStatsRecorder objectStatsRecorder, final L2State l2State, final SEDA seda,
                                 final TCServer server) {
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
    this.serverBuilder = createServerBuilder(this.haConfig, logger, server);
  }

  protected DSOServerBuilder createServerBuilder(final HaConfig config, final TCLogger tcLogger, final TCServer server) {
    Assert.assertEquals(config.isActiveActive(), false);
    return new StandardDSOServerBuilder(config, tcLogger);
  }

  protected DSOServerBuilder getServerBuilder() {
    return this.serverBuilder;
  }

  public void dump() {
    this.dumpHandler.dump();
    this.serverBuilder.dump();
  }

  public synchronized void start() throws IOException, TCDatabaseException, LocationNotCreatedException,
      FileNotCreatedException {

    this.threadGroup.addCallbackOnExitDefaultHandler(new ThreadDumpHandler(this));
    this.threadGroup.addCallbackOnExitDefaultHandler(this.dumpHandler);

    this.thisServerNodeID = makeServerNodeID(this.configSetupManager.dsoL2Config());

    TerracottaOperatorEventLogging.setNodeNameProvider(new ServerNameProvider(this.configSetupManager.dsoL2Config()
        .serverName()));

    final L2LockStatsManager lockStatsManager = new L2LockStatisticsManagerImpl();

    final List<PostInit> toInit = new ArrayList<PostInit>();

    try {
      this.lockStatisticsMBean = new LockStatisticsMonitor(lockStatsManager);
    } catch (final NotCompliantMBeanException ncmbe) {
      throw new TCRuntimeException("Unable to construct the " + LockStatisticsMonitor.class.getName()
                                   + " MBean; this is a programming error. Please go fix that class.", ncmbe);
    }

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

    // setup the statistics subsystem
    this.statisticsAgentSubSystem = new StatisticsAgentSubSystemImpl();
    if (!this.statisticsAgentSubSystem.setup(StatisticsSystemType.SERVER, this.configSetupManager.commonl2Config())) {
      System.exit(-1);
    }
    if (TCSocketAddress.WILDCARD_IP.equals(bindAddress)) {
      this.statisticsAgentSubSystem.setDefaultAgentIp(InetAddress.getLocalHost().getHostAddress());
    } else {
      this.statisticsAgentSubSystem.setDefaultAgentIp(jmxBind.getHostAddress());
    }
    try {
      this.statisticsGateway = new StatisticsGatewayMBeanImpl();
    } catch (final NotCompliantMBeanException e) {
      throw new TCRuntimeException("Unable to construct the " + StatisticsGatewayMBeanImpl.class.getName()
                                   + " MBean; this is a programming error. Please go fix that class.", e);
    }
    this.statisticsGathererSubSystem = new StatisticsGathererSubSystem();
    if (!this.statisticsGathererSubSystem.setup(this.configSetupManager.commonl2Config())) {
      System.exit(1);
    }

    NIOWorkarounds.solaris10Workaround();
    this.l2Properties = TCPropertiesImpl.getProperties().getPropertiesFor("l2");
    final PersistenceMode.Enum persistenceMode = l2DSOConfig.getPersistence().getMode();
    final TCProperties objManagerProperties = this.l2Properties.getPropertiesFor("objectmanager");
    this.l1ReconnectConfig = new L1ReconnectConfigImpl();
    final boolean persistent = (persistenceMode == PersistenceMode.PERMANENT_STORE);

    final Offheap offHeapConfig = l2DSOConfig.offHeapConfig();

    final DBFactory dbFactory = getDBFactory();

    final TCFile location = new TCFileImpl(this.configSetupManager.commonl2Config().dataPath());
    this.startupLock = new StartupLock(location, this.l2Properties.getBoolean("startuplock.retries.enabled"));

    if (!this.startupLock.canProceed(new TCRandomFileAccessImpl(), persistent)) {
      consoleLogger.error("Another L2 process is using the directory " + location + " as data directory.");
      if (!persistent) {
        consoleLogger.error("This is not allowed with persistence mode set to temporary-swap-only.");
      }
      consoleLogger.error("Exiting...");
      System.exit(1);
    }

    final int maxStageSize = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_SEDA_STAGE_SINK_CAPACITY);
    final StageManager stageManager = this.seda.getStageManager();
    final SessionManager sessionManager = new NullSessionManager();

    this.dumpHandler.registerForDump(new CallbackDumpAdapter(stageManager));

    EvictionPolicy swapCache;
    final ClientStatePersistor clientStateStore;
    final TransactionPersistor transactionPersistor;
    final Sequence globalTransactionIDSequence;
    final GarbageCollectionInfoPublisher gcPublisher = new GarbageCollectionInfoPublisherImpl();
    final ManagedObjectChangeListenerProviderImpl managedObjectChangeListenerProvider = new ManagedObjectChangeListenerProviderImpl();
    StatisticRetrievalAction[] sraForDbEnv = null;

    this.sampledCounterManager = new CounterManagerImpl();
    final SampledCounterConfig sampledCounterConfig = new SampledCounterConfig(1, 300, true, 0L);
    final SampledCounter l2FaultFromDisk = (SampledCounter) this.sampledCounterManager
        .createCounter(sampledCounterConfig);
    final SampledCounter l2FaultFromOffheap = (SampledCounter) this.sampledCounterManager
        .createCounter(sampledCounterConfig);
    final SampledCounter l2FlushFromOffheap = (SampledCounter) this.sampledCounterManager
        .createCounter(sampledCounterConfig);

    final File dbhome = new File(this.configSetupManager.commonl2Config().dataPath(), L2DSOConfig.OBJECTDB_DIRNAME);
    logger.debug("persistent: " + persistent);
    if (!persistent) {
      if (dbhome.exists()) {
        logger.info("deleting persistence database: " + dbhome.getAbsolutePath());
        FileUtils.cleanDirectory(dbhome);
      }

      File indexRoot = configSetupManager.commonl2Config().indexPath();
      if (indexRoot.exists()) {
        logger.info("deleting index directory: " + indexRoot.getAbsolutePath());
        FileUtils.cleanDirectory(indexRoot);
      }
    }
    logger.debug("persistence database home: " + dbhome);

    final CallbackOnExitHandler dirtydbHandler = new CallbackDatabaseDirtyAlertAdapter(logger, consoleLogger);
    this.threadGroup.addCallbackOnExitExceptionHandler(DatabaseDirtyException.class, dirtydbHandler);

    this.dbenv = this.serverBuilder.createDBEnvironment(persistent, dbhome, l2DSOConfig, this, stageManager,
                                                        l2FaultFromDisk, l2FaultFromOffheap, l2FlushFromOffheap,
                                                        dbFactory, offHeapConfig.getEnabled());
    final SerializationAdapterFactory serializationAdapterFactory = new CustomSerializationAdapterFactory();

    sraForDbEnv = this.dbenv.getSRAs();

    // init the JMX server
    try {
      setupL2Management(jmxBind, this.configSetupManager.commonl2Config().jmxPort().getIntValue(),
                        new RemoteJMXProcessor(), dbFactory.getServerDBBackupMBean(this.configSetupManager));
    } catch (final Exception e) {
      final String msg = "Unable to setup JMX server. Do you have another Terracotta Server instance running?";
      consoleLogger.error(msg);
      logger.error(msg, e);
      System.exit(-1);
    }

    // Setting the DB environment for the bean which takes backup of the active server
    if (persistent) {
      this.persistor = new DBPersistorImpl(TCLogging.getLogger(DBPersistorImpl.class), this.dbenv,
                                           serializationAdapterFactory, this.configSetupManager.commonl2Config()
                                               .dataPath(), this.objectStatsRecorder);
      this.l2Management.initBackupMbean(this.dbenv);
    } else {
      this.persistor = new TempSwapDBPersistorImpl(TCLogging.getLogger(DBPersistorImpl.class), this.dbenv,
                                                   serializationAdapterFactory, this.configSetupManager
                                                       .commonl2Config().dataPath(), this.objectStatsRecorder);
    }

    // register the terracotta operator event logger
    this.operatorEventHistoryProvider = new DsoOperatorEventHistoryProvider();
    this.serverBuilder
        .registerForOperatorEvents(this.l2Management, this.operatorEventHistoryProvider, getMBeanServer());

    final String cachePolicy = this.l2Properties.getProperty("objectmanager.cachePolicy").toUpperCase();
    if (cachePolicy.equals("LRU")) {
      swapCache = new LRUEvictionPolicy(-1);
    } else if (cachePolicy.equals("LFU")) {
      swapCache = new LFUEvictionPolicy(-1, new LFUConfigImpl(this.l2Properties.getPropertiesFor("lfu")));
    } else {
      throw new AssertionError("Unknown Cache Policy : " + cachePolicy
                               + " Accepted Values are : <LRU>/<LFU> Please check tc.properties");
    }
    final int gcDeleteThreads = this.l2Properties.getInt("seda.gcdeletestage.threads");
    final Sink gcDisposerSink = stageManager.createStage(ServerConfigurationContext.GC_DELETE_FROM_DISK_STAGE,
                                                         new GarbageDisposeHandler(gcPublisher), gcDeleteThreads,
                                                         maxStageSize).getSink();

    this.objectStore = new PersistentManagedObjectStore(this.persistor.getManagedObjectPersistor(), gcDisposerSink);

    /**
     * CachedDBEnvironment passes the object store stats to Offheap Event Manager which triggers Operator Event based on
     * object cached % in offheap
     */
    this.dbenv.initObjectStoreStats(this.objectStore);

    this.threadGroup
        .addCallbackOnExitExceptionHandler(CleanDirtyDatabaseException.class,
                                           new CallbackDirtyDatabaseExceptionAdapter(logger, consoleLogger,
                                                                                     this.persistor
                                                                                         .getPersistentStateStore()));
    this.threadGroup
        .addCallbackOnExitExceptionHandler(ZapDirtyDbServerNodeException.class,
                                           new CallbackZapDirtyDbExceptionAdapter(logger, consoleLogger, this.persistor
                                               .getPersistentStateStore()));
    this.threadGroup
        .addCallbackOnExitExceptionHandler(ZapServerNodeException.class,
                                           new CallbackZapServerNodeExceptionAdapter(logger, consoleLogger,
                                                                                     this.persistor
                                                                                         .getPersistentStateStore()));

    PersistenceTransactionProvider transactionStorePTP = this.persistor.getPersistenceTransactionProvider();
    MutableSequence gidSequence;
    transactionPersistor = this.persistor.getTransactionPersistor();
    gidSequence = this.persistor.getGlobalTransactionIDSequence();

    final GlobalTransactionIDBatchRequestHandler gidSequenceProvider = new GlobalTransactionIDBatchRequestHandler(
                                                                                                                  gidSequence);
    final Stage requestBatchStage = stageManager
        .createStage(ServerConfigurationContext.REQUEST_BATCH_GLOBAL_TRANSACTION_ID_SEQUENCE_STAGE,
                     gidSequenceProvider, 1, maxStageSize);
    gidSequenceProvider.setRequestBatchSink(requestBatchStage.getSink());
    globalTransactionIDSequence = new BatchSequence(gidSequenceProvider, 10000);

    clientStateStore = this.persistor.getClientStatePersistor();

    ManagedObjectStateFactory.createInstance(managedObjectChangeListenerProvider, this.persistor);

    final int commWorkerThreadCount = L2Utils.getOptimalCommWorkerThreads();
    final int stageWorkerThreadCount = L2Utils.getOptimalStageWorkerThreads();

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

    final TCMessageRouter messageRouter = new TCMessageRouterImpl();
    this.communicationsManager = new CommunicationsManagerImpl(CommunicationsManager.COMMSMGR_SERVER, mm,
                                                               messageRouter, networkStackHarnessFactory,
                                                               this.connectionPolicy, commWorkerThreadCount,
                                                               new HealthCheckerConfigImpl(this.l2Properties
                                                                   .getPropertiesFor("healthcheck.l1"), "DSO Server"),
                                                               this.thisServerNodeID,
                                                               new TransportHandshakeErrorNullHandler(),
                                                               getMessageTypeClassMappings(), Collections.EMPTY_MAP);

    final DSOApplicationEvents appEvents;
    try {
      appEvents = new DSOApplicationEvents();
    } catch (final NotCompliantMBeanException ncmbe) {
      throw new TCRuntimeException("Unable to construct the " + DSOApplicationEvents.class.getName()
                                   + " MBean; this is a programming error. Please go fix that class.", ncmbe);
    }

    this.clientStateManager = new ClientStateManagerImpl(TCLogging.getLogger(ClientStateManager.class));

    final boolean gcEnabled = l2DSOConfig.garbageCollection().getEnabled();

    final long gcInterval = l2DSOConfig.garbageCollection().getInterval();

    final boolean verboseGC = l2DSOConfig.garbageCollection().getVerbose();
    final SampledCumulativeCounterConfig sampledCumulativeCounterConfig = new SampledCumulativeCounterConfig(1, 300,
                                                                                                             true, 0L);
    final SampledCounter objectCreationRate = (SampledCounter) this.sampledCounterManager
        .createCounter(sampledCounterConfig);
    final SampledCounter objectFaultRate = (SampledCounter) this.sampledCounterManager
        .createCounter(sampledCounterConfig);
    final SampledCounter objectFlushedRate = (SampledCounter) this.sampledCounterManager
        .createCounter(sampledCounterConfig);
    final ObjectManagerStatsImpl objMgrStats = new ObjectManagerStatsImpl(objectCreationRate, objectFaultRate,
                                                                          objectFlushedRate);
    final SampledCounter time2FaultFromDiskOrOffheap = (SampledCounter) this.sampledCounterManager
        .createCounter(sampledCounterConfig);
    final SampledCounter time2Add2ObjMgr = (SampledCounter) this.sampledCounterManager
        .createCounter(sampledCounterConfig);

    final SequenceValidator sequenceValidator = new SequenceValidator(0);
    // Server initiated request processing queues shouldn't have any max queue size.

    final ManagedObjectFaultHandler managedObjectFaultHandler = new ManagedObjectFaultHandler(
                                                                                              time2FaultFromDiskOrOffheap,
                                                                                              time2Add2ObjMgr,
                                                                                              this.objectStatsRecorder);
    final Stage faultManagedObjectStage = stageManager
        .createStage(ServerConfigurationContext.MANAGED_OBJECT_FAULT_STAGE, managedObjectFaultHandler,
                     this.l2Properties.getInt("seda.faultstage.threads", stageWorkerThreadCount), -1);
    final ManagedObjectFlushHandler managedObjectFlushHandler = new ManagedObjectFlushHandler(this.objectStatsRecorder);
    final Stage flushManagedObjectStage = stageManager
        .createStage(ServerConfigurationContext.MANAGED_OBJECT_FLUSH_STAGE, managedObjectFlushHandler, (persistent ? 1
            : this.l2Properties.getInt("seda.flushstage.threads", stageWorkerThreadCount)), -1);
    final long enterpriseMarkStageInterval = objManagerProperties.getPropertiesFor("dgc")
        .getLong("enterpriseMarkStageInterval");
    final TCProperties youngDGCProperties = objManagerProperties.getPropertiesFor("dgc").getPropertiesFor("young");
    final boolean enableYoungGenDGC = youngDGCProperties.getBoolean("enabled");
    final long youngGenDGCFrequency = youngDGCProperties.getLong("frequencyInMillis");

    final ObjectManagerConfig objectManagerConfig = new ObjectManagerConfig(gcInterval * 1000, gcEnabled, verboseGC,
                                                                            persistent, enableYoungGenDGC,
                                                                            youngGenDGCFrequency,
                                                                            enterpriseMarkStageInterval);

    this.objectManager = new ObjectManagerImpl(objectManagerConfig, this.clientStateManager, this.objectStore,
                                               swapCache, dbenv.getPersistenceTransactionProvider(),
                                               faultManagedObjectStage.getSink(), flushManagedObjectStage.getSink(),
                                               this.objectStatsRecorder);

    this.objectManager.setStatsListener(objMgrStats);
    this.gcStatsEventPublisher = new GCStatsEventPublisher();
    managedObjectChangeListenerProvider.setListener(this.objectManager);
    final CallbackDumpAdapter objMgrDumpAdapter = new CallbackDumpAdapter(this.objectManager);
    this.dumpHandler.registerForDump(objMgrDumpAdapter);

    final TCProperties cacheManagerProperties = this.l2Properties.getPropertiesFor("cachemanager");
    final CacheConfig cacheConfig = new CacheConfigImpl(cacheManagerProperties, persistent, offHeapConfig.getEnabled());
    final TCMemoryManagerImpl tcMemManager = new TCMemoryManagerImpl(cacheConfig.getSleepInterval(),
                                                                     cacheConfig.getLeastCount(),
                                                                     cacheConfig.isOnlyOldGenMonitored(),
                                                                     this.threadGroup, !offHeapConfig.getEnabled());
    final long timeOut = TCPropertiesImpl.getProperties().getLong(TCPropertiesConsts.LOGGING_LONG_GC_THRESHOLD);
    final LongGCLogger gcLogger = this.serverBuilder.createLongGCLogger(timeOut);
    tcMemManager.registerForMemoryEvents(gcLogger);
    tcMemManager.registerForMemoryEvents(new MemoryOperatorEventListener(cacheConfig.getUsedCriticalThreshold()));
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

    final int serverPort = l2DSOConfig.dsoPort().getIntValue();

    this.statisticsAgentSubSystem.setDefaultAgentDifferentiator("L2/" + serverPort);

    final String dsoBind = l2DSOConfig.dsoPort().getBind();
    this.l1Listener = this.communicationsManager.createListener(sessionManager,
                                                                new TCSocketAddress(dsoBind, serverPort), true,
                                                                this.connectionIdFactory, this.httpSink);

    final ClientTunnelingEventHandler cteh = new ClientTunnelingEventHandler();
    this.stripeIDStateManager = new StripeIDStateManagerImpl(this.haConfig, this.persistor.getPersistentStateStore());

    this.dumpHandler.registerForDump(new CallbackDumpAdapter(this.stripeIDStateManager));

    final ProductInfo pInfo = ProductInfo.getInstance();
    final DSOChannelManager channelManager = new DSOChannelManagerImpl(this.haConfig.getThisGroupID(),
                                                                       this.l1Listener.getChannelManager(),
                                                                       this.communicationsManager
                                                                           .getConnectionManager(), pInfo.version(),
                                                                       this.stripeIDStateManager);
    channelManager.addEventListener(cteh);
    channelManager.addEventListener(this.connectionIdFactory);

    final ChannelStatsImpl channelStats = new ChannelStatsImpl(this.sampledCounterManager, channelManager);
    channelManager.addEventListener(channelStats);

    final CommitTransactionMessageRecycler recycler = new CommitTransactionMessageRecycler();
    toInit.add(recycler);

    // Creating a stage here so that the sink can be passed
    final Stage respondToLockStage = stageManager.createStage(ServerConfigurationContext.RESPOND_TO_LOCK_REQUEST_STAGE,
                                                              new RespondToRequestLockHandler(),
                                                              stageWorkerThreadCount, 1, maxStageSize);
    this.lockManager = new LockManagerImpl(respondToLockStage.getSink(), channelManager);
    this.lockStatisticsMBean.addL2LockStatisticsEnableDisableListener(this.lockManager);

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
                     queryThreads, 1, maxStageSize);

    final Sink searchEventSink = searchEventStage.getSink();

    final TransactionBatchManagerImpl transactionBatchManager = new TransactionBatchManagerImpl(sequenceValidator,
                                                                                                recycler, txnFilter,
                                                                                                syncWriteTxnRecvdAckStage
                                                                                                    .getSink());
    toInit.add(transactionBatchManager);
    this.dumpHandler.registerForDump(new CallbackDumpAdapter(transactionBatchManager));

    final TransactionAcknowledgeAction taa = new TransactionAcknowledgeActionImpl(channelManager,
                                                                                  transactionBatchManager);
    final SampledCounter globalTxnCounter = (SampledCounter) this.sampledCounterManager
        .createCounter(sampledCounterConfig);

    final SampledCounter broadcastCounter = (SampledCounter) this.sampledCounterManager
        .createCounter(sampledCounterConfig);

    final SampledCounter globalObjectFaultCounter = (SampledCounter) this.sampledCounterManager
        .createCounter(sampledCounterConfig);
    final SampledCounter globalObjectFlushCounter = (SampledCounter) this.sampledCounterManager
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

    final DSOGlobalServerStatsImpl serverStats = new DSOGlobalServerStatsImpl(globalObjectFlushCounter,
                                                                              globalObjectFaultCounter,
                                                                              globalTxnCounter, objMgrStats,
                                                                              broadcastCounter, l2FaultFromDisk,
                                                                              time2FaultFromDiskOrOffheap,
                                                                              time2Add2ObjMgr, globalLockRecallCounter,
                                                                              changesPerBroadcast,
                                                                              transactionSizeCounter, globalLockCount);
    serverStats.serverMapGetSizeRequestsCounter(globalServerMapGetSizeRequestsCounter)
        .serverMapGetValueRequestsCounter(globalServerMapGetValueRequestsCounter)
        .serverMapGetSnapshotRequestsCounter(globalServerMapGetSnapshotRequestsCounter);

    final TransactionStore transactionStore = new TransactionStoreImpl(transactionPersistor,
                                                                       globalTransactionIDSequence);
    final ServerGlobalTransactionManager gtxm = new ServerGlobalTransactionManagerImpl(sequenceValidator,
                                                                                       transactionStore,
                                                                                       transactionStorePTP,
                                                                                       gidSequenceProvider,
                                                                                       globalTransactionIDSequence);

    final TransactionalStagesCoordinatorImpl txnStageCoordinator = new TransactionalStagesCoordinatorImpl(stageManager);
    final ServerTransactionSequencerImpl serverTransactionSequencerImpl = new ServerTransactionSequencerImpl();
    this.txnObjectManager = new TransactionalObjectManagerImpl(this.objectManager, serverTransactionSequencerImpl,
                                                               gtxm, txnStageCoordinator);

    final CallbackDumpAdapter txnObjMgrDumpAdapter = new CallbackDumpAdapter(this.txnObjectManager);
    this.dumpHandler.registerForDump(txnObjMgrDumpAdapter);
    this.objectManager.setTransactionalObjectManager(this.txnObjectManager);

    this.metaDataManager = this.serverBuilder.createMetaDataManager(searchEventSink);

    this.transactionManager = new ServerTransactionManagerImpl(gtxm, transactionStore, this.lockManager,
                                                               this.clientStateManager, this.objectManager,
                                                               this.txnObjectManager, taa, globalTxnCounter,
                                                               channelStats,
                                                               new ServerTransactionManagerConfig(this.l2Properties
                                                                   .getPropertiesFor("transactionmanager")),
                                                               this.objectStatsRecorder, this.metaDataManager);

    this.metaDataManager.setTransactionManager(transactionManager);

    final CallbackDumpAdapter txnMgrDumpAdapter = new CallbackDumpAdapter(this.transactionManager);
    this.dumpHandler.registerForDump(txnMgrDumpAdapter);

    final ServerClusterMetaDataManager clusterMetaDataManager = new ServerClusterMetaDataManagerImpl(
                                                                                                     logger,
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

    final int commitThreads = (persistent ? this.l2Properties.getInt("seda.commitstage.threads") : 1);
    stageManager.createStage(ServerConfigurationContext.COMMIT_CHANGES_STAGE,
                             new CommitTransactionChangeHandler(transactionStorePTP), commitThreads, maxStageSize);

    txnStageCoordinator.lookUpSinks();

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
                     new ManagedObjectRequestHandler(globalObjectFaultCounter, globalObjectFlushCounter),
                     this.l2Properties.getInt("seda.managedobjectrequeststage.threads", stageWorkerThreadCount), 1,
                     maxStageSize);
    final Stage respondToObjectRequestStage = stageManager
        .createStage(ServerConfigurationContext.RESPOND_TO_OBJECT_REQUEST_STAGE, new RespondToObjectRequestHandler(),
                     this.l2Properties.getInt("seda.managedobjectresponsestage.threads", stageWorkerThreadCount),
                     maxStageSize);

    final Stage serverMapRequestStage = stageManager
        .createStage(ServerConfigurationContext.SERVER_MAP_REQUEST_STAGE,
                     new ServerMapRequestHandler(globalServerMapGetSizeRequestsCounter,
                                                 globalServerMapGetValueRequestsCounter,
                                                 globalServerMapGetSnapshotRequestsCounter), 8, maxStageSize);
    final Stage respondToServerTCMapStage = stageManager
        .createStage(ServerConfigurationContext.SERVER_MAP_RESPOND_STAGE, new RespondToServerMapRequestHandler(), 8,
                     maxStageSize);

    this.searchRequestManager = this.serverBuilder.createSearchRequestManager(channelManager,
                                                                              objectRequestStage.getSink());
    toInit.add(this.searchRequestManager);

    this.serverMapRequestManager = this.serverBuilder
        .createServerMapRequestManager(this.objectManager, channelManager, respondToServerTCMapStage.getSink(),
                                       objectRequestStage.getSink());
    this.dumpHandler.registerForDump(new CallbackDumpAdapter(this.serverMapRequestManager));

    final ServerTransactionFactory serverTransactionFactory = new ServerTransactionFactory();
    this.serverMapEvictor = new ServerMapEvictionManagerImpl(
                                                             this.objectManager,
                                                             this.objectStore,
                                                             this.clientStateManager,
                                                             serverTransactionFactory,
                                                             objectManagerConfig.gcThreadSleepTime() > 0 ? ((objectManagerConfig
                                                                 .gcThreadSleepTime() + 1) / 2)
                                                                 : ServerMapEvictionManagerImpl.DEFAULT_SLEEP_TIME);
    toInit.add(this.serverMapEvictor);
    this.dumpHandler.registerForDump(new CallbackDumpAdapter(this.serverMapEvictor));
    stageManager.createStage(ServerConfigurationContext.SERVER_MAP_EVICTION_BROADCAST_STAGE,
                             new ServerMapEvictionBroadcastHandler(broadcastCounter), 1, maxStageSize);
    stageManager.createStage(ServerConfigurationContext.SERVER_MAP_EVICTION_PROCESSOR_STAGE,
                             new ServerMapEvictionHandler(this.serverMapEvictor), 8, TCPropertiesImpl.getProperties()
                                 .getInt(TCPropertiesConsts.L2_SEDA_EVICTION_PROCESSORSTAGE_SINK_SIZE));
    stageManager.createStage(ServerConfigurationContext.SERVER_MAP_CAPACITY_EVICTION_STAGE,
                             new ServerMapCapacityEvictionHandler(this.serverMapEvictor),
                             this.l2Properties.getInt("servermap.capacityEvictor.threads", 1), maxStageSize);

    this.objectRequestManager = this.serverBuilder.createObjectRequestManager(this.objectManager, channelManager,
                                                                              this.clientStateManager,
                                                                              this.transactionManager,
                                                                              objectRequestStage.getSink(),
                                                                              respondToObjectRequestStage.getSink(),
                                                                              this.objectStatsRecorder, toInit,
                                                                              stageManager, maxStageSize, this);
    this.dumpHandler.registerForDump(new CallbackDumpAdapter(this.objectRequestManager));
    final Stage oidRequest = stageManager.createStage(ServerConfigurationContext.OBJECT_ID_BATCH_REQUEST_STAGE,
                                                      new RequestObjectIDBatchHandler(this.objectStore), 1,
                                                      maxStageSize);
    final Stage transactionAck = stageManager.createStage(ServerConfigurationContext.TRANSACTION_ACKNOWLEDGEMENT_STAGE,
                                                          new TransactionAcknowledgementHandler(), 1, maxStageSize);
    final Stage clientHandshake = stageManager.createStage(ServerConfigurationContext.CLIENT_HANDSHAKE_STAGE,
                                                           createHandShakeHandler(), 1, maxStageSize);
    this.hydrateStage = stageManager.createStage(ServerConfigurationContext.HYDRATE_MESSAGE_SINK, new HydrateHandler(),
                                                 stageWorkerThreadCount, 1, maxStageSize);
    final Stage txnLwmStage = stageManager.createStage(ServerConfigurationContext.TRANSACTION_LOWWATERMARK_STAGE,
                                                       new TransactionLowWaterMarkHandler(gtxm), 1, maxStageSize);

    final Stage jmxEventsStage = stageManager.createStage(ServerConfigurationContext.JMX_EVENTS_STAGE,
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

    final Stage clusterMetaDataStage = stageManager.createStage(ServerConfigurationContext.CLUSTER_METADATA_STAGE,
                                                                new ServerClusterMetaDataHandler(), 1, maxStageSize);

    initRouteMessages(messageRouter, processTx, rootRequest, requestLock, objectRequestStage, oidRequest,
                      transactionAck, clientHandshake, txnLwmStage, jmxEventsStage, jmxRemoteTunnelStage,
                      clientLockStatisticsRespondStage, clusterMetaDataStage, serverMapRequestStage,
                      searchQueryRequestStage);

    long reconnectTimeout = l2DSOConfig.clientReconnectWindow();
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
                                                                                                     .getStage(ServerConfigurationContext.RESPOND_TO_LOCK_REQUEST_STAGE)
                                                                                                     .getSink(),
                                                                                                 stageManager
                                                                                                     .getStage(ServerConfigurationContext.OBJECT_ID_BATCH_REQUEST_STAGE)
                                                                                                     .getSink(),
                                                                                                 new Timer(
                                                                                                           "Reconnect timer",
                                                                                                           true),
                                                                                                 reconnectTimeout,
                                                                                                 persistent,
                                                                                                 consoleLogger);

    final boolean networkedHA = this.haConfig.isNetworkedActivePassive();
    this.groupCommManager = this.serverBuilder.createGroupCommManager(networkedHA, this.configSetupManager,
                                                                      stageManager, this.thisServerNodeID,
                                                                      this.httpSink, this.stripeIDStateManager, gtxm);

    this.dumpHandler.registerForDump(new CallbackDumpAdapter(this.groupCommManager));
    // initialize the garbage collector
    final MutableSequence dgcSequence = this.dbenv.getSequence(this.dbenv.getPersistenceTransactionProvider(), logger,
                                                               DBSequenceKeys.DGC_SEQUENCE_NAME, 1);
    final DGCSequenceProvider dgcSequenceProvider = new DGCSequenceProvider(dgcSequence);
    final GarbageCollector gc = this.serverBuilder.createGarbageCollector(toInit, objectManagerConfig,
                                                                          this.objectManager, this.clientStateManager,
                                                                          stageManager, maxStageSize, gcPublisher,
                                                                          this.objectManager, this.clientStateManager,
                                                                          getGcStatsEventPublisher(),
                                                                          getStatisticsAgentSubSystem(),
                                                                          dgcSequenceProvider, this.transactionManager);
    gc.addListener(new GCStatisticsAgentSubSystemEventListener(getStatisticsAgentSubSystem()));
    this.objectManager.setGarbageCollector(gc);
    if (objectManagerConfig.startGCThread()) {
      final StoppableThread st = new GarbageCollectorThread(this.threadGroup, "DGC-Thread", gc, objectManagerConfig);
      st.setDaemon(true);
      gc.setState(st);
    }
    this.l2Management.findObjectManagementMonitorMBean().registerGCController(new GCComptrollerImpl(this.objectManager
                                                                                  .getGarbageCollector()));
    this.l2Management.findObjectManagementMonitorMBean().registerObjectIdFetcher(new ObjectIdsFetcher() {
      public Set getAllObjectIds() {
        return DistributedObjectServer.this.objectManager.getAllObjectIDs();
      }
    });

    // TODO: currently making all with L2hacoordinator which should probably the case after this feature
    if (networkedHA) {
      final WeightGeneratorFactory weightGeneratorFactory = new ZapNodeProcessorWeightGeneratorFactory(
                                                                                                       channelManager,
                                                                                                       transactionBatchManager,
                                                                                                       this.transactionManager,
                                                                                                       host, serverPort);
      logger.info("L2 Networked HA Enabled ");
      this.indexHACoordinator = this.serverBuilder.createIndexHACoordinator(this.configSetupManager, searchEventSink);

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
                                                                    this.groupCommManager,
                                                                    this.persistor.getPersistentStateStore(),
                                                                    l2PassiveSyncStateManager, l2ObjectStateManager,
                                                                    l2IndexStateManager, this.objectManager,
                                                                    this.indexHACoordinator, this.transactionManager,
                                                                    gtxm, weightGeneratorFactory,
                                                                    this.configSetupManager, recycler,
                                                                    this.stripeIDStateManager,
                                                                    serverTransactionFactory, dgcSequenceProvider,
                                                                    indexSequenceGenerator);
      this.l2Coordinator.getStateManager().registerForStateChangeEvents(this.l2State);
      this.l2Coordinator.getStateManager().registerForStateChangeEvents(this.indexHACoordinator);
      this.l2Coordinator.getStateManager().registerForStateChangeEvents(this.l2Coordinator);

      dgcSequenceProvider.registerSequecePublisher(this.l2Coordinator.getReplicatedClusterStateManager());
    } else {
      this.l2State.setState(StateManager.ACTIVE_COORDINATOR);
      this.l2Coordinator = new L2HADisabledCooridinator(this.groupCommManager);
    }

    this.dumpHandler.registerForDump(new CallbackDumpAdapter(this.l2Coordinator));
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
                                                                       searchRequestManager);
    toInit.add(this.serverBuilder);

    stageManager.startAll(this.context, toInit);

    // populate the statistics retrieval register
    populateStatisticsRetrievalRegistry(serverStats, this.seda.getStageManager(), mm, this.transactionManager,
                                        serverTransactionSequencerImpl, sraForDbEnv);

    // XXX: yucky casts
    this.managementContext = new ServerManagementContext(this.transactionManager, this.objectRequestManager,
                                                         (LockManagerMBean) this.lockManager,
                                                         (DSOChannelManagerMBean) channelManager, serverStats,
                                                         channelStats, instanceMonitor, appEvents, indexHACoordinator);

    try {
      this.l2Management.start();
    } catch (final Exception e) {
      final String msg = "Unable to start JMX server. Do you have another Terracotta Server instance running?";
      consoleLogger.error(msg);
      logger.error(msg, e);
      System.exit(-1);
    }

    if (this.l2Properties.getBoolean("beanshell.enabled")) {
      startBeanShell(this.l2Properties.getInt("beanshell.port"));
    }

    final ObjectStatsManager objStatsManager = new ObjectStatsManagerImpl(this.objectManager,
                                                                          this.objectManager.getObjectStore());
    // Start lock statistics manager.
    lockStatsManager.start(channelManager, serverStats, objStatsManager);
    if (lockStatsManager.isLockStatisticsEnabled()) {
      this.lockManager.setLockStatisticsEnabled(true, lockStatsManager);
    } else {
      L2LockStatsManager.UNSYNCHRONIZED_LOCK_STATS_MANAGER.start(channelManager, serverStats, objStatsManager);
    }

    final CallbackOnExitHandler handler = new CallbackGroupExceptionHandler(logger, consoleLogger);
    this.threadGroup.addCallbackOnExitExceptionHandler(GroupException.class, handler);

    startGroupManagers();
    this.l2Coordinator.start();
    if (!networkedHA) {
      // In non-network enabled HA, Only active server reached here.
      startActiveMode();
      startL1Listener();
    }
    setLoggerOnExit();
  }

  private DBFactory getDBFactory() {
    String factoryName = TCPropertiesImpl.getProperties().getProperty(TCPropertiesConsts.L2_DB_FACTORY_NAME);
    DBFactory dbFactory = null;
    try {
      Class dbClass = Class.forName(factoryName);
      Constructor<DBFactory> constructor = dbClass.getConstructor(TCProperties.class);
      dbFactory = constructor.newInstance(this.l2Properties);
    } catch (Exception e) {
      consoleLogger.warn("Unable to create db class:" + factoryName, e);
      System.exit(1);
    }
    return dbFactory;
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
                                   final Stage jmxEventsStage, final Stage jmxRemoteTunnelStage,
                                   final Stage clientLockStatisticsRespondStage, final Stage clusterMetaDataStage,
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
    messageRouter.routeMessageType(TCMessageType.JMX_MESSAGE, jmxEventsStage.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE, jmxRemoteTunnelStage.getSink(),
                                   hydrateSink);
    messageRouter.routeMessageType(TCMessageType.CLIENT_JMX_READY_MESSAGE, jmxRemoteTunnelStage.getSink(), hydrateSink);
    messageRouter.routeMessageType(TCMessageType.TUNNELED_DOMAINS_CHANGED_MESSAGE, jmxRemoteTunnelStage.getSink(),
                                   hydrateSink);
    messageRouter.routeMessageType(TCMessageType.LOCK_STATISTICS_RESPONSE_MESSAGE,
                                   clientLockStatisticsRespondStage.getSink(), hydrateSink);
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
    messageTypeClassMapping.put(TCMessageType.EVICTION_SERVER_MAP_BROADCAST_MESSAGE,
                                ServerMapEvictionBroadcastMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.SEARCH_QUERY_REQUEST_MESSAGE, SearchQueryRequestMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.SEARCH_QUERY_RESPONSE_MESSAGE, SearchQueryResponseMessageImpl.class);
    messageTypeClassMapping.put(TCMessageType.INVALIDATE_OBJECTS_MESSAGE, InvalidateObjectsMessage.class);
    return messageTypeClassMapping;
  }

  protected TCLogger getLogger() {
    return logger;
  }

  private ServerID makeServerNodeID(final L2DSOConfig l2DSOConfig) {
    String host = l2DSOConfig.l2GroupPort().getBind();
    if (TCSocketAddress.WILDCARD_IP.equals(host)) {
      host = l2DSOConfig.host();
    }
    final Node node = new Node(host, l2DSOConfig.dsoPort().getIntValue());
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
      public void run() {
        logger.info("L2 Exiting...");
      }
    });
  }

  private void populateStatisticsRetrievalRegistry(final DSOGlobalServerStats serverStats,
                                                   final StageManager stageManager,
                                                   final MessageMonitor messageMonitor,
                                                   final ServerTransactionManagerImpl txnManager,
                                                   final ServerTransactionSequencerStats serverTransactionSequencerStats,
                                                   final StatisticRetrievalAction[] srasForDbEnv) {
    if (this.statisticsAgentSubSystem.isActive()) {
      final StatisticsRetrievalRegistry registry = this.statisticsAgentSubSystem.getStatisticsRetrievalRegistry();
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
      registry.registerActionInstance(new SRAL2ServerMapGetSizeRequestsCount(serverStats));
      registry.registerActionInstance(new SRAL2ServerMapGetSizeRequestsRate(serverStats));
      registry.registerActionInstance(new SRAL2ServerMapGetValueRequestsCount(serverStats));
      registry.registerActionInstance(new SRAL2ServerMapGetValueRequestsRate(serverStats));
      for (final StatisticRetrievalAction sraForDbEnv : srasForDbEnv) {
        registry.registerActionInstance(sraForDbEnv);
      }

      this.serverBuilder.populateAdditionalStatisticsRetrivalRegistry(registry);
    }
  }

  public boolean isBlocking() {
    return this.startupLock != null && this.startupLock.isBlocking();
  }

  public void startActiveMode() {
    this.transactionManager.goToActiveMode();
  }

  public void startL1Listener() throws IOException {
    final Set existingConnections = Collections.unmodifiableSet(this.connectionIdFactory.loadConnectionIDs());
    this.context.getClientHandshakeManager().setStarting(existingConnections);
    this.l1Listener.start(existingConnections);
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
    final int configValue = l2DSOConfig.dsoPort().getIntValue();
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
    final int configValue = l2DSOConfig.l2GroupPort().getIntValue();
    if (configValue != 0) { return configValue; }
    return -1;
  }

  public synchronized void stop() {

    try {
      this.indexHACoordinator.shutdown();
    } catch (Throwable t) {
      logger.warn(t);
    }

    try {
      this.statisticsAgentSubSystem.cleanup();
    } catch (final Throwable e) {
      logger.warn(e);
    }

    try {
      this.statisticsGateway.cleanup();
    } catch (final Throwable e) {
      logger.warn(e);
    }

    try {
      this.statisticsGathererSubSystem.cleanup();
    } catch (Exception e) {
      logger.error("Error shutting down statistics gatherer", e);
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
    } catch (final DBException e) {
      logger.warn(e);
    }

    if (this.sampledCounterManager != null) {
      try {
        this.sampledCounterManager.shutdown();
      } catch (final Exception e) {
        logger.error(e);
      }
    }

    try {
      stopJMXServer();
    } catch (final Throwable t) {
      logger.error("Error shutting down jmx server", t);
    }

    basicStop();
  }

  public void quickStop() {
    try {
      stopJMXServer();
    } catch (final Throwable t) {
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

  public StatisticsGathererSubSystem getStatisticsGathererSubsystem() {
    return this.statisticsGathererSubSystem;
  }

  public GCStatsEventPublisher getGcStatsEventPublisher() {
    return this.gcStatsEventPublisher;
  }

  public TerracottaOperatorEventHistoryProvider getOperatorEventsHistoryProvider() {
    return this.operatorEventHistoryProvider;
  }

  private void setupL2Management(final InetAddress bind, int jmxPort, final Sink remoteEventsSink,
                                 final ServerDBBackupMBean serverDBBackupMBean) throws Exception {
    if (jmxPort == 0) {
      jmxPort = new PortChooser().chooseRandomPort();
    }

    this.l2Management = this.serverBuilder.createL2Management(this.tcServerInfoMBean, this.lockStatisticsMBean,
                                                              this.statisticsAgentSubSystem, this.statisticsGateway,
                                                              this.statisticsGathererSubSystem,
                                                              this.configSetupManager, this, bind, jmxPort,
                                                              remoteEventsSink, this, serverDBBackupMBean);
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

  public OffheapStats getOffheapStats() {
    return this.dbenv.getOffheapStats();
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

  protected GroupManager getGroupManager() {
    return this.groupCommManager;
  }

  public void registerForDump(final CallbackDumpAdapter dumpAdapter) {
    this.dumpHandler.registerForDump(dumpAdapter);
  }

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
}
