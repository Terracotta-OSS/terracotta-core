/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.objectserver.impl;

import com.tc.net.core.security.TCSecurityManager;

import org.terracotta.entity.ServiceRegistry;

import com.tc.async.api.PostInit;
import com.tc.async.api.Sink;
import com.tc.async.api.StageManager;
import com.tc.config.NodesStore;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.io.TCFile;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.logging.DumpHandlerStore;
import com.tc.logging.TCLogger;
import com.tc.management.L2Management;
import com.tc.management.beans.TCDumper;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.management.beans.object.ServerDBBackupMBean;
import com.tc.net.GroupID;
import com.tc.net.ServerID;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.StripeIDStateManager;
import com.tc.net.protocol.tcm.ChannelManager;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.core.api.GlobalServerStats;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.locks.LockManager;
import com.tc.objectserver.persistence.ClusterStatePersistor;
import com.tc.objectserver.persistence.Persistor;
import com.tc.operatorevent.TerracottaOperatorEventHistoryProvider;
import com.tc.runtime.logging.LongGCLogger;
import com.tc.server.ServerConnectionValidator;
import com.tc.util.StartupLock;
import com.tc.util.sequence.DGCSequenceProvider;

import java.io.IOException;
import java.net.InetAddress;

import javax.management.MBeanServer;

public interface ServerBuilder extends TCDumper, PostInit {

  GroupManager createGroupCommManager(L2ConfigurationSetupManager configManager,
                                      StageManager stageManager, ServerID serverNodeID,
                                      StripeIDStateManager stripeStateManager);

  ServerConfigurationContext createServerConfigurationContext(StageManager stageManager,
                                                              LockManager lockMgr, DSOChannelManager channelManager,
                                                              ChannelStatsImpl channelStats,
                                                              L2Coordinator l2HACoordinator,
                                                              ServerClientHandshakeManager clientHandshakeManager,
                                                              GlobalServerStats serverStats,
                                                              ConnectionIDFactory connectionIdFactory,
                                                              int maxStageSize, ChannelManager genericChannelManager,
                                                              DumpHandlerStore dumpHandlerStore);

  GroupManager getClusterGroupCommManager();

  L2Coordinator createL2HACoordinator(TCLogger consoleLogger, DistributedObjectServer server,
                                      StageManager stageManager, GroupManager groupCommsManager,
                                      ClusterStatePersistor clusterStatePersistor,
                                      WeightGeneratorFactory weightGeneratorFactory,
                                      L2ConfigurationSetupManager configurationSetupManager,
                                      StripeIDStateManager stripeStateManager,
                                      DGCSequenceProvider dgcSequenceProvider,
                                      int electionTimeInSecs, NodesStore nodesStore);

  L2Management createL2Management(boolean listenerEnabled, TCServerInfoMBean tcServerInfoMBean,
                                  L2ConfigurationSetupManager configSetupManager,
                                  DistributedObjectServer distributedObjectServer, InetAddress bind, int jmxPort,
                                  Sink remoteEventsSink, ServerConnectionValidator serverConnectionValidator,
                                  ServerDBBackupMBean serverDBBackupMBean, TCSecurityManager manager) throws Exception;

  void registerForOperatorEvents(L2Management l2Management,
                                 TerracottaOperatorEventHistoryProvider operatorEventHistoryProvider,
                                 MBeanServer l2MbeanServer);

  Persistor createPersistor(ServiceRegistry serviceRegistry) throws IOException;

  LongGCLogger createLongGCLogger(long gcTimeOut);

  StartupLock createStartupLock(TCFile location, boolean retries);

  GroupID getLocalGroupId();
}
