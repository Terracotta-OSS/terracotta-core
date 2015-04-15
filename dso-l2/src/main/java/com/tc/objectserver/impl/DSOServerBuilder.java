/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.impl;

import org.terracotta.corestorage.StorageManager;

import com.tc.async.api.PostInit;
import com.tc.async.api.Sink;
import com.tc.async.api.StageManager;
import com.tc.config.NodesStore;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.io.TCFile;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.l2.objectserver.L2IndexStateManager;
import com.tc.l2.objectserver.L2ObjectStateManager;
import com.tc.l2.objectserver.L2PassiveSyncStateManager;
import com.tc.l2.objectserver.ServerTransactionFactory;
import com.tc.l2.state.StateSyncManager;
import com.tc.logging.DumpHandlerStore;
import com.tc.logging.TCLogger;
import com.tc.management.L2Management;
import com.tc.management.beans.L2State;
import com.tc.management.beans.TCDumper;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.management.beans.object.ServerDBBackupMBean;
import com.tc.net.GroupID;
import com.tc.net.ServerID;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.StripeIDStateManager;
import com.tc.net.protocol.tcm.ChannelManager;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.object.msg.MessageRecycler;
import com.tc.object.net.ChannelStats;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.api.BackupManager;
import com.tc.objectserver.api.GarbageCollectionManager;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.api.ServerMapRequestManager;
import com.tc.objectserver.clustermetadata.ServerClusterMetaDataManager;
import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.locks.LockManager;
import com.tc.objectserver.metadata.MetaDataManager;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.persistence.ClusterStatePersistor;
import com.tc.objectserver.persistence.Persistor;
import com.tc.objectserver.search.IndexHACoordinator;
import com.tc.objectserver.search.IndexManager;
import com.tc.objectserver.search.SearchRequestManager;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionBatchManagerImpl;
import com.tc.objectserver.tx.TransactionFilter;
import com.tc.objectserver.tx.TransactionalObjectManager;
import com.tc.operatorevent.TerracottaOperatorEventHistoryProvider;
import com.tc.runtime.logging.LongGCLogger;
import com.tc.server.ServerConnectionValidator;
import com.tc.util.StartupLock;
import com.tc.util.concurrent.TaskRunner;
import com.tc.util.sequence.DGCSequenceProvider;
import com.tc.util.sequence.ObjectIDSequence;
import com.tc.util.sequence.SequenceGenerator;
import com.terracottatech.config.DataStorage;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import javax.management.MBeanServer;

public interface DSOServerBuilder extends TCDumper, PostInit {

  TransactionFilter getTransactionFilter(List<PostInit> toInit, StageManager stageManager, int maxStageSize);

  MetaDataManager createMetaDataManager(Sink sink);

  IndexHACoordinator createIndexHACoordinator(L2ConfigurationSetupManager configSetupManager, Sink sink, StorageManager storageManager)
      throws IOException;

  L2IndexStateManager createL2IndexStateManager(IndexHACoordinator indexHACoordinator,
                                                ServerTransactionManager transactionManager,
                                                SequenceGenerator indexSequenceGenerator, GroupManager groupManager);

  L2ObjectStateManager createL2ObjectStateManager(ObjectManager objectManager,
                                                  ServerTransactionManager transactionManager);

  L2PassiveSyncStateManager createL2PassiveSyncStateManager(L2IndexStateManager l2IndexStateManager,
                                                            L2ObjectStateManager l2ObjectStateManager,
                                                            StateSyncManager stateSyncManager);

  ServerMapRequestManager createServerMapRequestManager(ObjectManager objectMgr, DSOChannelManager channelManager,
                                                        Sink respondToServerTCMapSink, Sink prefetch,
                                                        ClientStateManager clientStateManager, ChannelStats channelStats);

  ObjectRequestManager createObjectRequestManager(ObjectManager objectMgr, DSOChannelManager channelManager,
                                                  ClientStateManager clientStateMgr,
                                                  ServerTransactionManager transactionMgr, Sink objectRequestSink,
                                                  Sink respondObjectRequestSink, ObjectStatsRecorder statsRecorder,
                                                  List<PostInit> toInit, StageManager stageManager, int maxStageSize,
                                                  DumpHandlerStore dumpHandlerStore);

  SearchRequestManager createSearchRequestManager(DSOChannelManager channelManager, Sink managedObjectRequestSink,
                                                  TaskRunner taskRunner);

  GroupManager createGroupCommManager(L2ConfigurationSetupManager configManager,
                                      StageManager stageManager, ServerID serverNodeID, Sink httpSink,
                                      StripeIDStateManager stripeStateManager, ServerGlobalTransactionManager gtxm);

  ServerConfigurationContext createServerConfigurationContext(StageManager stageManager, ObjectManager objMgr,
                                                              ObjectRequestManager objRequestMgr,
                                                              ServerMapRequestManager serverTCMapRequestManager,
                                                              PersistentManagedObjectStore objStore,
                                                              LockManager lockMgr, DSOChannelManager channelManager,
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
                                                              DumpHandlerStore dumpHandlerStore,
                                                              MetaDataManager metaDataManager,
                                                              IndexManager indexManager,
                                                              SearchRequestManager searchRequestManager,
                                                              GarbageCollectionManager deleteObjectManager);

  GroupManager getClusterGroupCommManager();

  L2Coordinator createL2HACoordinator(TCLogger consoleLogger, DistributedObjectServer server,
                                      StageManager stageManager, GroupManager groupCommsManager,
                                      ClusterStatePersistor clusterStatePersistor,
                                      L2PassiveSyncStateManager l2PassiveSyncStateManager,
                                      L2ObjectStateManager l2ObjectStateManager,
                                      L2IndexStateManager l2IndexStateManager, ObjectManager objectManager,
                                      IndexHACoordinator indexHACoordinator,
                                      ServerTransactionManager transactionManager, ServerGlobalTransactionManager gtxm,
                                      WeightGeneratorFactory weightGeneratorFactory,
                                      L2ConfigurationSetupManager configurationSetupManager, MessageRecycler recycler,
                                      StripeIDStateManager stripeStateManager,
                                      ServerTransactionFactory serverTransactionFactory,
                                      DGCSequenceProvider dgcSequenceProvider,
                                      SequenceGenerator indexSequenceGenerator, ObjectIDSequence objectIDSequence,
                                      final DataStorage datastore, int electionTimeInSecs, NodesStore nodesStore);

  L2Management createL2Management(boolean listenerEnabled, TCServerInfoMBean tcServerInfoMBean,
                                  L2ConfigurationSetupManager configSetupManager,
                                  DistributedObjectServer distributedObjectServer, InetAddress bind, int jmxPort,
                                  Sink remoteEventsSink, ServerConnectionValidator serverConnectionValidator,
                                  ServerDBBackupMBean serverDBBackupMBean) throws Exception;

  void registerForOperatorEvents(L2Management l2Management,
                                 TerracottaOperatorEventHistoryProvider operatorEventHistoryProvider,
                                 MBeanServer l2MbeanServer);

  Persistor createPersistor(final boolean persistent, final File l2DataPath, final L2State l2State) throws IOException;

  BackupManager createBackupManager(final Persistor persistor,
                                           final IndexManager indexManager,
                                           final File backupPath,
                                           final StageManager stageManager,
                                           boolean restartable,
                                           ServerTransactionManager serverTransactionManager);

  LongGCLogger createLongGCLogger(long gcTimeOut);

  StartupLock createStartupLock(TCFile location, boolean retries);

  GroupID getLocalGroupId();
}
