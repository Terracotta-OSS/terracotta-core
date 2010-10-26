/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.PostInit;
import com.tc.async.api.Sink;
import com.tc.async.api.StageManager;
import com.tc.config.HaConfig;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.ha.L2HACoordinator;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.l2.objectserver.ServerTransactionFactory;
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
import com.tc.object.config.schema.NewL2DSOConfig;
import com.tc.object.msg.MessageRecycler;
import com.tc.object.net.ChannelStatsImpl;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.persistence.api.PersistentMapStore;
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
import com.tc.objectserver.dgc.impl.DGCEventStatsProvider;
import com.tc.objectserver.dgc.impl.MarkAndSweepGarbageCollector;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.locks.LockManager;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.storage.api.DBEnvironment;
import com.tc.objectserver.storage.api.DBFactory;
import com.tc.objectserver.tx.CommitTransactionMessageToTransactionBatchReader;
import com.tc.objectserver.tx.PassThruTransactionFilter;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionBatchManagerImpl;
import com.tc.objectserver.tx.TransactionFilter;
import com.tc.objectserver.tx.TransactionalObjectManager;
import com.tc.operatorevent.TerracottaOperatorEventHistoryProvider;
import com.tc.server.ServerConnectionValidator;
import com.tc.statistics.StatisticsAgentSubSystem;
import com.tc.statistics.StatisticsAgentSubSystemImpl;
import com.tc.statistics.beans.impl.StatisticsGatewayMBeanImpl;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.util.runtime.ThreadDumpUtil;

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
                                                 final DGCEventStatsProvider gcEventStatsProvider,
                                                 final StatisticsAgentSubSystem statsAgentSubSystem) {
    final MarkAndSweepGarbageCollector gc = new MarkAndSweepGarbageCollector(objectManagerConfig, objectMgr,
                                                                             stateManager, gcPublisher,
                                                                             gcEventStatsProvider);
    gc.addListener(gcEventStatsProvider);
    gc.addListener(new DGCOperatorEventPublisher());
    return gc;
  }

  public GroupManager createGroupCommManager(final boolean networkedHA,
                                             final L2TVSConfigurationSetupManager configManager,
                                             final StageManager stageManager, final ServerID serverNodeID,
                                             final Sink httpSink, final StripeIDStateManager stripeStateManager,
                                             final ServerGlobalTransactionManager gtxm) {
    if (networkedHA) {
      return new TCGroupManagerImpl(configManager, stageManager, serverNodeID, httpSink, this.haConfig.getNodesStore());
    } else {
      return new SingleNodeGroupManager();
    }
  }

  public ObjectRequestManager createObjectRequestManager(final ObjectManager objectMgr,
                                                         final DSOChannelManager channelManager,
                                                         final ClientStateManager clientStateMgr,
                                                         final ServerTransactionManager transactionMgr,
                                                         final Sink objectRequestSink,
                                                         final Sink respondObjectRequestSink,
                                                         final ObjectStatsRecorder statsRecorder,
                                                         final List<PostInit> toInit, final StageManager stageManager,
                                                         final int maxStageSize, final DumpHandlerStore dumpHandlerStore) {
    final ObjectRequestManagerImpl orm = new ObjectRequestManagerImpl(objectMgr, channelManager, clientStateMgr,
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
                                                                     final StageManager stageManager,
                                                                     final ObjectManager objMgr,
                                                                     final ObjectRequestManager objRequestMgr,
                                                                     final ServerMapRequestManager serverTCMapRequestManager,
                                                                     final ManagedObjectStore objStore,
                                                                     final LockManager lockMgr,
                                                                     final DSOChannelManager channelManager,
                                                                     final ClientStateManager clientStateMgr,
                                                                     final ServerTransactionManager txnMgr,
                                                                     final TransactionalObjectManager txnObjectMgr,
                                                                     final ChannelStatsImpl channelStats,
                                                                     final L2Coordinator coordinator,
                                                                     final TransactionBatchManagerImpl transactionBatchManager,
                                                                     final ServerGlobalTransactionManager gtxm,
                                                                     final ServerClientHandshakeManager clientHandshakeManager,
                                                                     final ServerClusterMetaDataManager clusterMetaDataManager,
                                                                     final DSOGlobalServerStats serverStats,
                                                                     final ConnectionIDFactory connectionIdFactory,
                                                                     final int maxStageSize,
                                                                     final ChannelManager genericChannelManager,
                                                                     final DumpHandlerStore dumpHandlerStore) {
    return new ServerConfigurationContextImpl(stageManager, objMgr, objRequestMgr, serverTCMapRequestManager, objStore,
                                              lockMgr, channelManager, clientStateMgr, txnMgr, txnObjectMgr,
                                              clientHandshakeManager, channelStats, coordinator,
                                              new CommitTransactionMessageToTransactionBatchReader(serverStats),
                                              transactionBatchManager, gtxm, clusterMetaDataManager);
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

  public DGCEventStatsProvider getLocalDGCStatsEventPublisher() {
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
                                             final ObjectManager objectManager,
                                             final ServerTransactionManager transactionManager,
                                             final ServerGlobalTransactionManager gtxm,
                                             final WeightGeneratorFactory weightGeneratorFactory,
                                             final L2TVSConfigurationSetupManager configurationSetupManager,
                                             final MessageRecycler recycler,
                                             final StripeIDStateManager stripeStateManager,
                                             final ServerTransactionFactory serverTransactionFactory) {
    return new L2HACoordinator(consoleLogger, server, stageManager, groupCommsManager, persistentMapStore,
                               objectManager, transactionManager, gtxm, weightGeneratorFactory,
                               configurationSetupManager, recycler, this.thisGroupID, stripeStateManager,
                               serverTransactionFactory);
  }

  public L2Management createL2Management(final TCServerInfoMBean tcServerInfoMBean,
                                         final LockStatisticsMonitor lockStatisticsMBean,
                                         final StatisticsAgentSubSystemImpl statisticsAgentSubSystem,
                                         final StatisticsGatewayMBeanImpl statisticsGateway,
                                         final L2TVSConfigurationSetupManager configSetupManager,
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
    // NOP
  }

  public DBEnvironment createDBEnvironment(final boolean persistent, final File dbhome,
                                           final NewL2DSOConfig l2DSOCofig, final DumpHandlerStore dumpHandlerStore,
                                           final StageManager stageManager, final SampledCounter l2FaultFromDisk,
                                           final SampledCounter l2FaultFromOffheap,
                                           final SampledCounter l2FlushFromOffheap, final DBFactory factory)
      throws IOException {
    return factory.createEnvironment(persistent, dbhome, l2FaultFromDisk);
  }
}
