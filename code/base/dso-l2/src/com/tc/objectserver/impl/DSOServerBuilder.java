/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.PostInit;
import com.tc.async.api.Sink;
import com.tc.async.api.StageManager;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.l2.objectserver.ServerTransactionFactory;
import com.tc.logging.DumpHandlerStore;
import com.tc.logging.TCLogger;
import com.tc.management.L2Management;
import com.tc.management.beans.LockStatisticsMonitor;
import com.tc.management.beans.TCDumper;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.management.beans.object.ServerDBBackupMBean;
import com.tc.net.ServerID;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.StripeIDStateManager;
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
import com.tc.objectserver.dgc.api.GarbageCollectionInfoPublisher;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.dgc.impl.GCStatsEventPublisher;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.locks.LockManager;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.storage.api.DBEnvironment;
import com.tc.objectserver.storage.api.DBFactory;
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

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import javax.management.MBeanServer;

public interface DSOServerBuilder extends TCDumper, PostInit {

  TransactionFilter getTransactionFilter(List<PostInit> toInit, StageManager stageManager, int maxStageSize);

  ServerMapRequestManager createServerMapRequestManager(ObjectManager objectMgr, DSOChannelManager channelManager,
                                                        Sink respondToServerTCMapSink, Sink managedObjectRequestSink);

  ObjectRequestManager createObjectRequestManager(ObjectManager objectMgr, DSOChannelManager channelManager,
                                                  ClientStateManager clientStateMgr,
                                                  ServerTransactionManager transactionMgr, Sink objectRequestSink,
                                                  Sink respondObjectRequestSink, ObjectStatsRecorder statsRecorder,
                                                  List<PostInit> toInit, StageManager stageManager, int maxStageSize,
                                                  DumpHandlerStore dumpHandlerStore);

  void populateAdditionalStatisticsRetrivalRegistry(StatisticsRetrievalRegistry registry);

  GroupManager createGroupCommManager(boolean networkedHA, L2TVSConfigurationSetupManager configManager,
                                      StageManager stageManager, ServerID serverNodeID, Sink httpSink,
                                      StripeIDStateManager stripeStateManager, ServerGlobalTransactionManager gtxm);

  GarbageCollector createGarbageCollector(List<PostInit> toInit, ObjectManagerConfig objectManagerConfig,
                                          ObjectManager objectMgr, ClientStateManager stateManager,
                                          StageManager stageManager, int maxStageSize,
                                          GarbageCollectionInfoPublisher gcPublisher, ObjectManager objectManager,
                                          ClientStateManager clientStateManger, GCStatsEventPublisher gcEventListener,
                                          StatisticsAgentSubSystem statsSubSystem);

  ServerConfigurationContext createServerConfigurationContext(StageManager stageManager, ObjectManager objMgr,
                                                              ObjectRequestManager objRequestMgr,
                                                              ServerMapRequestManager serverTCMapRequestManager,
                                                              ManagedObjectStore objStore, LockManager lockMgr,
                                                              DSOChannelManager channelManager,
                                                              ClientStateManager clientStateMgr,
                                                              ServerTransactionManager txnMgr,
                                                              TransactionalObjectManager txnObjectMgr,
                                                              ChannelStatsImpl channelStats,
                                                              L2Coordinator l2HACoordinator,
                                                              TransactionBatchManagerImpl transactionBatchManager,
                                                              ServerGlobalTransactionManager gtxm,
                                                              ServerClientHandshakeManager clientHandshakeManager,
                                                              ServerClusterMetaDataManager clusterMetaDataManager,
                                                              DSOGlobalServerStats serverStats,
                                                              ConnectionIDFactory connectionIdFactory,
                                                              int maxStageSize, ChannelManager genericChannelManager,
                                                              DumpHandlerStore dumpHandlerStore);

  GroupManager getClusterGroupCommManager();

  GCStatsEventPublisher getLocalDGCStatsEventPublisher();

  L2Coordinator createL2HACoordinator(TCLogger consoleLogger, DistributedObjectServer server,
                                      StageManager stageManager, GroupManager groupCommsManager,
                                      PersistentMapStore persistentMapStore, ObjectManager objectManager,
                                      ServerTransactionManager transactionManager, ServerGlobalTransactionManager gtxm,
                                      WeightGeneratorFactory weightGeneratorFactory,
                                      L2TVSConfigurationSetupManager configurationSetupManager,
                                      MessageRecycler recycler, StripeIDStateManager stripeStateManager,
                                      ServerTransactionFactory serverTransactionFactory);

  L2Management createL2Management(TCServerInfoMBean tcServerInfoMBean, LockStatisticsMonitor lockStatisticsMBean,
                                  StatisticsAgentSubSystemImpl statisticsAgentSubSystem,
                                  StatisticsGatewayMBeanImpl statisticsGateway,
                                  L2TVSConfigurationSetupManager configSetupManager,
                                  DistributedObjectServer distributedObjectServer, InetAddress bind, int jmxPort,
                                  Sink remoteEventsSink, ServerConnectionValidator serverConnectionValidator,
                                  ServerDBBackupMBean serverDBBackupMBean) throws Exception;

  void registerForOperatorEvents(L2Management l2Management,
                                 TerracottaOperatorEventHistoryProvider operatorEventHistoryProvider,
                                 MBeanServer l2MbeanServer);

  DBEnvironment createDBEnvironment(final boolean persistent, final File dbhome, final NewL2DSOConfig l2DSOCofig,
                                    DumpHandlerStore dumpHandlerStore, final StageManager stageManager,
                                    SampledCounter l2FaultFromDisk, SampledCounter l2FaultFromOffheap,
                                    SampledCounter l2FlushFromOffheap, DBFactory factory) throws IOException,
      TCDatabaseException;
}
