/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.PostInit;
import com.tc.async.api.Sink;
import com.tc.async.api.StageManager;
import com.tc.config.HaConfig;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.io.TCFile;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.ha.L2HACoordinator;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.l2.objectserver.L2IndexStateManager;
import com.tc.l2.objectserver.L2ObjectStateManager;
import com.tc.l2.objectserver.L2ObjectStateManagerImpl;
import com.tc.l2.objectserver.L2PassiveSyncStateManager;
import com.tc.l2.objectserver.L2PassiveSyncStateManagerImpl;
import com.tc.l2.objectserver.NullL2IndexStateManager;
import com.tc.l2.objectserver.ServerTransactionFactory;
import com.tc.l2.state.StateSyncManager;
import com.tc.logging.DumpHandlerStore;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.L2Management;
import com.tc.management.beans.LockStatisticsMonitor;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.management.beans.object.ServerDBBackupMBean;
import com.tc.net.GroupID;
import com.tc.net.ServerID;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.SingleNodeGroupManager;
import com.tc.net.groups.StripeIDStateManager;
import com.tc.net.groups.TCGroupManagerImpl;
import com.tc.net.protocol.tcm.ChannelManager;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.object.config.schema.L2DSOConfig;
import com.tc.object.msg.MessageRecycler;
import com.tc.object.net.ChannelStatsImpl;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.persistence.api.PersistentMapStore;
import com.tc.objectserver.api.GarbageCollectionManager;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.api.ServerMapRequestManager;
import com.tc.objectserver.clustermetadata.ServerClusterMetaDataManager;
import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ServerConfigurationContextImpl;
import com.tc.objectserver.dgc.api.GarbageCollectionInfoPublisher;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.dgc.impl.DGCOperatorEventPublisher;
import com.tc.objectserver.dgc.impl.GCStatsEventPublisher;
import com.tc.objectserver.dgc.impl.MarkAndSweepGarbageCollector;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.locks.LockManager;
import com.tc.objectserver.metadata.MetaDataManager;
import com.tc.objectserver.metadata.NullMetaDataManager;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.search.IndexHACoordinator;
import com.tc.objectserver.search.IndexManager;
import com.tc.objectserver.search.NullIndexHACoordinator;
import com.tc.objectserver.search.NullSearchRequestManager;
import com.tc.objectserver.search.SearchRequestManager;
import com.tc.objectserver.storage.api.DBEnvironment;
import com.tc.objectserver.storage.api.DBFactory;
import com.tc.objectserver.tx.CommitTransactionMessageToTransactionBatchReader;
import com.tc.objectserver.tx.PassThruTransactionFilter;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionBatchManagerImpl;
import com.tc.objectserver.tx.TransactionFilter;
import com.tc.objectserver.tx.TransactionalObjectManager;
import com.tc.operatorevent.TerracottaOperatorEventCallbackLogger;
import com.tc.operatorevent.TerracottaOperatorEventHistoryProvider;
import com.tc.operatorevent.TerracottaOperatorEventLogger;
import com.tc.operatorevent.TerracottaOperatorEventLogging;
import com.tc.runtime.logging.LongGCLogger;
import com.tc.server.ServerConnectionValidator;
import com.tc.statistics.StatisticsAgentSubSystem;
import com.tc.statistics.StatisticsAgentSubSystemImpl;
import com.tc.statistics.beans.impl.StatisticsGatewayMBeanImpl;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.util.BlockingStartupLock;
import com.tc.util.NonBlockingStartupLock;
import com.tc.util.StartupLock;
import com.tc.util.runtime.ThreadDumpUtil;
import com.tc.util.sequence.DGCSequenceProvider;
import com.tc.util.sequence.SequenceGenerator;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import javax.management.MBeanServer;

public class StandardDSOServerBuilder implements DSOServerBuilder {
  private final HaConfig   haConfig;
  private final GroupID    thisGroupID;
  protected final TCLogger logger;

  public StandardDSOServerBuilder(final HaConfig haConfig, final TCLogger logger) {
    this.logger = logger;
    this.logger.info("Standard DSO Server created");
    this.haConfig = haConfig;
    this.thisGroupID = this.haConfig.getThisGroupID();
  }

  public GarbageCollector createGarbageCollector(final List<PostInit> toInit,
                                                 final ObjectManagerConfig objectManagerConfig,
                                                 final ObjectManager objectMgr, final ClientStateManager stateManager,
                                                 final StageManager stageManager, final int maxStageSize,
                                                 final GarbageCollectionInfoPublisher gcPublisher,
                                                 final ObjectManager objectManager,
                                                 final ClientStateManager clientStateManger,
                                                 final GCStatsEventPublisher gcEventListener,
                                                 final StatisticsAgentSubSystem statsAgentSubSystem,
                                                 final DGCSequenceProvider dgcSequenceProvider,
                                                 final ServerTransactionManager serverTransactionManager) {
    final MarkAndSweepGarbageCollector gc = new MarkAndSweepGarbageCollector(objectManagerConfig, objectMgr,
                                                                             stateManager, gcPublisher,
                                                                             dgcSequenceProvider);
    gc.addListener(gcEventListener);
    gc.addListener(new DGCOperatorEventPublisher());
    return gc;
  }

  public GroupManager createGroupCommManager(final boolean networkedHA,
                                             final L2ConfigurationSetupManager configManager,
                                             final StageManager stageManager, final ServerID serverNodeID,
                                             final Sink httpSink, final StripeIDStateManager stripeStateManager,
                                             final ServerGlobalTransactionManager gtxm) {
    if (networkedHA) {
      return new TCGroupManagerImpl(configManager, stageManager, serverNodeID, httpSink, this.haConfig.getNodesStore());
    } else {
      return new SingleNodeGroupManager();
    }
  }

  public MetaDataManager createMetaDataManager(Sink sink) {
    return new NullMetaDataManager();
  }

  @SuppressWarnings("unused")
  public IndexHACoordinator createIndexHACoordinator(L2ConfigurationSetupManager configSetupManager, Sink sink)
      throws IOException {
    return new NullIndexHACoordinator();
  }

  public L2IndexStateManager createL2IndexStateManager(IndexHACoordinator indexHACoordinator,
                                                       ServerTransactionManager transactionManager,
                                                       SequenceGenerator indexSequenceGenerator,
                                                       GroupManager groupManager) {
    return new NullL2IndexStateManager();
  }

  public L2ObjectStateManager createL2ObjectStateManager(ObjectManager objectManager,
                                                         ServerTransactionManager transactionManager) {
    return new L2ObjectStateManagerImpl(objectManager, transactionManager);
  }

  public L2PassiveSyncStateManager createL2PassiveSyncStateManager(L2IndexStateManager l2IndexStateManager,
                                                                   L2ObjectStateManager l2ObjectStateManager,
                                                                   StateSyncManager stateSyncManager) {
    return new L2PassiveSyncStateManagerImpl(l2IndexStateManager, l2ObjectStateManager, stateSyncManager);
  }

  public SearchRequestManager createSearchRequestManager(DSOChannelManager channelManager, Sink managedObjectRequestSink) {
    return new NullSearchRequestManager();
  }

  public ObjectRequestManager createObjectRequestManager(ObjectManager objectMgr, DSOChannelManager channelManager,
                                                         ClientStateManager clientStateMgr,
                                                         ServerTransactionManager transactionMgr,
                                                         Sink objectRequestSink, Sink respondObjectRequestSink,
                                                         ObjectStatsRecorder statsRecorder, List<PostInit> toInit,
                                                         StageManager stageManager, int maxStageSize,
                                                         DumpHandlerStore dumpHandlerStore) {
    ObjectRequestManagerImpl orm = new ObjectRequestManagerImpl(objectMgr, channelManager, clientStateMgr,
                                                                objectRequestSink, respondObjectRequestSink,
                                                                statsRecorder);
    return new ObjectRequestManagerRestartImpl(objectMgr, transactionMgr, orm);
  }

  public ServerMapRequestManager createServerMapRequestManager(final ObjectManager objectMgr,
                                                               final DSOChannelManager channelManager,
                                                               final Sink respondToServerTCMapSink,
                                                               final Sink managedObjectRequestSink) {
    return new ServerMapRequestManagerImpl(objectMgr, channelManager, respondToServerTCMapSink,
                                           managedObjectRequestSink);
  }

  public ServerConfigurationContext createServerConfigurationContext(
                                                                     StageManager stageManager,
                                                                     ObjectManager objMgr,
                                                                     ObjectRequestManager objRequestMgr,
                                                                     ServerMapRequestManager serverTCMapRequestManager,
                                                                     ManagedObjectStore objStore,
                                                                     LockManager lockMgr,
                                                                     DSOChannelManager channelManager,
                                                                     ClientStateManager clientStateMgr,
                                                                     ServerTransactionManager txnMgr,
                                                                     TransactionalObjectManager txnObjectMgr,
                                                                     ChannelStatsImpl channelStats,
                                                                     L2Coordinator coordinator,
                                                                     TransactionBatchManagerImpl transactionBatchManager,
                                                                     ServerGlobalTransactionManager gtxm,
                                                                     ServerClientHandshakeManager clientHandshakeManager,
                                                                     ServerClusterMetaDataManager clusterMetaDataManager,
                                                                     DSOGlobalServerStats serverStats,
                                                                     ConnectionIDFactory connectionIdFactory,
                                                                     int maxStageSize,
                                                                     ChannelManager genericChannelManager,
                                                                     DumpHandlerStore dumpHandlerStore,
                                                                     MetaDataManager metaDataManager,
                                                                     IndexManager indexManager,
                                                                     SearchRequestManager searchRequestManager,
                                                                     GarbageCollectionManager deleteObjectManager) {
    return new ServerConfigurationContextImpl(stageManager, objMgr, objRequestMgr, serverTCMapRequestManager, objStore,
                                              lockMgr, channelManager, clientStateMgr, txnMgr, txnObjectMgr,
                                              clientHandshakeManager, channelStats, coordinator,
                                              new CommitTransactionMessageToTransactionBatchReader(serverStats),
                                              transactionBatchManager, gtxm, clusterMetaDataManager, metaDataManager,
                                              indexManager, searchRequestManager, deleteObjectManager);
  }

  public TransactionFilter getTransactionFilter(final List<PostInit> toInit, final StageManager stageManager,
                                                final int maxStageSize) {
    final PassThruTransactionFilter txnFilter = new PassThruTransactionFilter();
    toInit.add(txnFilter);
    return txnFilter;
  }

  public void populateAdditionalStatisticsRetrivalRegistry(final StatisticsRetrievalRegistry registry) {
    // Add any additional Statistics here
  }

  public GroupManager getClusterGroupCommManager() {
    throw new AssertionError("Not supported");
  }

  public GCStatsEventPublisher getLocalDGCStatsEventPublisher() {
    throw new AssertionError("Not supported");
  }

  public void dump() {
    TCLogging.getDumpLogger().info(ThreadDumpUtil.getThreadDump());
  }

  public void initializeContext(final ConfigurationContext context) {
    // Nothing to initialize here
  }

  public L2Coordinator createL2HACoordinator(final TCLogger consoleLogger, final DistributedObjectServer server,
                                             final StageManager stageManager, final GroupManager groupCommsManager,
                                             final PersistentMapStore persistentMapStore,
                                             final L2PassiveSyncStateManager l2PassiveSyncStateManager,
                                             final L2ObjectStateManager l2ObjectStateManager,
                                             final L2IndexStateManager l2IndexStateManager,
                                             final ObjectManager objectManager,
                                             final IndexHACoordinator indexHACoordinator,
                                             final ServerTransactionManager transactionManager,
                                             final ServerGlobalTransactionManager gtxm,
                                             final WeightGeneratorFactory weightGeneratorFactory,
                                             final L2ConfigurationSetupManager configurationSetupManager,
                                             final MessageRecycler recycler,
                                             final StripeIDStateManager stripeStateManager,
                                             final ServerTransactionFactory serverTransactionFactory,
                                             final DGCSequenceProvider dgcSequenceProvider,
                                             final SequenceGenerator indexSequenceGenerator) {
    return new L2HACoordinator(consoleLogger, server, stageManager, groupCommsManager, persistentMapStore,
                               objectManager, indexHACoordinator, l2PassiveSyncStateManager, l2ObjectStateManager,
                               l2IndexStateManager, transactionManager, gtxm, weightGeneratorFactory,
                               configurationSetupManager, recycler, this.thisGroupID, stripeStateManager,
                               serverTransactionFactory, dgcSequenceProvider, indexSequenceGenerator);
  }

  public L2Management createL2Management(final TCServerInfoMBean tcServerInfoMBean,
                                         final LockStatisticsMonitor lockStatisticsMBean,
                                         final StatisticsAgentSubSystemImpl statisticsAgentSubSystem,
                                         final StatisticsGatewayMBeanImpl statisticsGateway,
                                         final L2ConfigurationSetupManager configSetupManager,
                                         final DistributedObjectServer distributedObjectServer, final InetAddress bind,
                                         final int jmxPort, final Sink remoteEventsSink,
                                         final ServerConnectionValidator serverConnectionValidator,
                                         final ServerDBBackupMBean serverDBBackupMBean) throws Exception {
    return new L2Management(tcServerInfoMBean, lockStatisticsMBean, statisticsAgentSubSystem, statisticsGateway,
                            configSetupManager, distributedObjectServer, bind, jmxPort, remoteEventsSink);
  }

  public void registerForOperatorEvents(final L2Management l2Management,
                                        final TerracottaOperatorEventHistoryProvider operatorEventHistoryProvider,
                                        final MBeanServer l2MbeanServer) {
    // register logger for OSS version
    TerracottaOperatorEventLogger tcEventLogger = TerracottaOperatorEventLogging.getEventLogger();
    tcEventLogger.registerEventCallback(new TerracottaOperatorEventCallbackLogger());
  }

  public DBEnvironment createDBEnvironment(final boolean persistent, final File dbhome, final L2DSOConfig l2DSOCofig,
                                           final DumpHandlerStore dumpHandlerStore, final StageManager stageManager,
                                           final SampledCounter l2FaultFromDisk,
                                           final SampledCounter l2FaultFromOffheap,
                                           final SampledCounter l2FlushFromOffheap, final DBFactory factory,
                                           final boolean offheapEnabled) throws IOException {
    return factory.createEnvironment(persistent, dbhome, l2FaultFromDisk, offheapEnabled);
  }

  public LongGCLogger createLongGCLogger(long gcTimeOut) {
    return new LongGCLogger(gcTimeOut);
  }

  public StartupLock createStartupLock(final TCFile location, final boolean retries) {
    if (this.haConfig.isNetworkedActivePassive()) {
      return new NonBlockingStartupLock(location, retries);
    } else if (this.haConfig.isDiskedBasedActivePassive()) {
      return new BlockingStartupLock(location, retries);
    } else {
      throw new AssertionError("Invalid HA mode");
    }
  }
}
