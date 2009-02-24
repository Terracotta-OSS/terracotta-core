/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.PostInit;
import com.tc.async.api.Sink;
import com.tc.async.api.StageManager;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.l2.api.L2Coordinator;
import com.tc.management.beans.TCDumper;
import com.tc.net.ServerID;
import com.tc.net.groups.GroupManager;
import com.tc.object.net.ChannelStatsImpl;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.clustermetadata.ServerClusterMetaDataManager;
import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.dgc.api.GarbageCollectionInfoPublisher;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.dgc.impl.GCStatsEventPublisher;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.lockmanager.api.LockManager;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionBatchManagerImpl;
import com.tc.objectserver.tx.TransactionFilter;
import com.tc.objectserver.tx.TransactionalObjectManager;
import com.tc.statistics.StatisticsAgentSubSystem;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;

import java.util.List;

public interface DSOServerBuilder extends TCDumper, PostInit {

  TransactionFilter getTransactionFilter(List<PostInit> toInit, StageManager stageManager, int maxStageSize);

  ObjectRequestManager createObjectRequestManager(ObjectManager objectMgr, DSOChannelManager channelManager,
                                                  ClientStateManager clientStateMgr,
                                                  ServerTransactionManager transactionMgr, Sink objectRequestSink,
                                                  Sink respondObjectRequestSink, ObjectStatsRecorder statsRecorder,
                                                  List<PostInit> toInit, StageManager stageManager, int maxStageSize);

  void populateAdditionalStatisticsRetrivalRegistry(StatisticsRetrievalRegistry registry);

  GroupManager createGroupCommManager(boolean networkedHA, L2TVSConfigurationSetupManager configManager,
                                      StageManager stageManager, ServerID serverNodeID, Sink httpSink);

  GarbageCollector createGarbageCollector(List<PostInit> toInit, ObjectManagerConfig objectManagerConfig,
                                          ObjectManager objectMgr, ClientStateManager stateManager,
                                          StageManager stageManager, int maxStageSize,
                                          GarbageCollectionInfoPublisher gcPublisher, ObjectManager objectManager,
                                          ClientStateManager clientStateManger, GCStatsEventPublisher gcEventListener,
                                          StatisticsAgentSubSystem statsSubSystem);

  ServerConfigurationContext createServerConfigurationContext(StageManager stageManager, ObjectManager objMgr,
                                                              ObjectRequestManager objRequestMgr,
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
                                                              DSOGlobalServerStats serverStats);

  GroupManager getClusterGroupCommManager();

  GCStatsEventPublisher getLocalDGCStatsEventPublisher();

}
