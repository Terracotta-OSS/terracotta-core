/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.PostInit;
import com.tc.async.api.Sink;
import com.tc.async.api.StageManager;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.l2.api.L2Coordinator;
import com.tc.logging.TCLogger;
import com.tc.net.ServerID;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.SingleNodeGroupManager;
import com.tc.net.groups.TCGroupManagerImpl;
import com.tc.object.net.ChannelStatsImpl;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.clustermetadata.ServerClusterMetaDataManager;
import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ServerConfigurationContextImpl;
import com.tc.objectserver.dgc.api.GarbageCollectionInfoPublisher;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.dgc.impl.GCStatsEventPublisher;
import com.tc.objectserver.dgc.impl.MarkAndSweepGarbageCollector;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.lockmanager.api.LockManager;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.tx.CommitTransactionMessageToTransactionBatchReader;
import com.tc.objectserver.tx.PassThruTransactionFilter;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionBatchManagerImpl;
import com.tc.objectserver.tx.TransactionFilter;
import com.tc.objectserver.tx.TransactionalObjectManager;
import com.tc.statistics.StatisticsAgentSubSystem;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;

import java.util.List;

public class StandardDSOServerBuilder implements DSOServerBuilder {

  public StandardDSOServerBuilder(TCLogger logger) {
    logger.info("Standard DSO Server created");
  }

  public GarbageCollector createGarbageCollector(List<PostInit> toInit, ObjectManagerConfig objectManagerConfig,
                                                 ObjectManager objectMgr, ClientStateManager stateManager,
                                                 StageManager stageManager, int maxStageSize,
                                                 GarbageCollectionInfoPublisher gcPublisher,
                                                 ObjectManager objectManager, ClientStateManager clientStateManger,
                                                 GCStatsEventPublisher gcEventListener,
                                                 StatisticsAgentSubSystem statsAgentSubSystem) {
    MarkAndSweepGarbageCollector gc = new MarkAndSweepGarbageCollector(objectManagerConfig, objectMgr, stateManager,
                                                                       gcPublisher);
    gc.addListener(gcEventListener);
    return gc;
  }

  public GroupManager createGroupCommManager(boolean networkedHA, L2TVSConfigurationSetupManager configManager,
                                             StageManager stageManager, ServerID serverNodeID, Sink httpSink) {
    if (networkedHA) {
      return new TCGroupManagerImpl(configManager, stageManager, serverNodeID, httpSink);
    } else {
      return new SingleNodeGroupManager();
    }
  }

  public ObjectRequestManager createObjectRequestManager(ObjectManager objectMgr, DSOChannelManager channelManager,
                                                         ClientStateManager clientStateMgr,
                                                         ServerTransactionManager transactionMgr,
                                                         Sink objectRequestSink, Sink respondObjectRequestSink,
                                                         ObjectStatsRecorder statsRecorder, List<PostInit> toInit,
                                                         StageManager stageManager, int maxStageSize) {
    ObjectRequestManagerImpl orm = new ObjectRequestManagerImpl(objectMgr, channelManager, clientStateMgr,
                                                                objectRequestSink, respondObjectRequestSink,
                                                                statsRecorder);
    return new ObjectRequestManagerRestartImpl(objectMgr, transactionMgr, orm);
  }

  public ServerConfigurationContext createServerConfigurationContext(
                                                                     StageManager stageManager,
                                                                     ObjectManager objMgr,
                                                                     ObjectRequestManager objRequestMgr,
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
                                                                     DSOGlobalServerStats serverStats) {
    return new ServerConfigurationContextImpl(stageManager, objMgr, objRequestMgr, objStore, lockMgr, channelManager,
                                              clientStateMgr, txnMgr, txnObjectMgr, clientHandshakeManager,
                                              channelStats, coordinator,
                                              new CommitTransactionMessageToTransactionBatchReader(serverStats),
                                              transactionBatchManager, gtxm, clusterMetaDataManager);
  }

  public TransactionFilter getTransactionFilter(List<PostInit> toInit, StageManager stageManager, int maxStageSize) {
    PassThruTransactionFilter txnFilter = new PassThruTransactionFilter();
    toInit.add(txnFilter);
    return txnFilter;
  }

  public void populateAdditionalStatisticsRetrivalRegistry(StatisticsRetrievalRegistry registry) {
    // Add any additional Statistics here
  }

  public GroupManager getClusterGroupCommManager() {
    throw new AssertionError("Not supported");
  }

  public GCStatsEventPublisher getLocalDGCStatsEventPublisher() {
    throw new AssertionError("Not supported");
  }

  public void dump() {
    // Nothing to dump
  }

  public void initializeContext(ConfigurationContext context) {
    // Nothing to initialize here
  }

}
