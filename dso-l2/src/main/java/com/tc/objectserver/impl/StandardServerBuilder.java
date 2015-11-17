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

import org.terracotta.entity.ServiceRegistry;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.Sink;
import com.tc.async.api.StageManager;
import com.tc.config.HaConfig;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.io.TCFile;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.ha.L2HACoordinator;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.l2.state.StateManager;
import com.tc.logging.DumpHandlerStore;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.L2Management;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.management.beans.object.ServerDBBackupMBean;
import com.tc.net.GroupID;
import com.tc.net.ServerID;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.StripeIDStateManager;
import com.tc.net.groups.TCGroupManagerImpl;
import com.tc.net.protocol.tcm.ChannelManager;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.core.api.GlobalServerStats;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ServerConfigurationContextImpl;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.locks.LockManager;
import com.tc.objectserver.persistence.ClusterStatePersistor;
import com.tc.objectserver.persistence.PersistentStorageServiceConfiguration;
import com.tc.objectserver.persistence.Persistor;
import com.tc.operatorevent.TerracottaOperatorEventCallbackLogger;
import com.tc.operatorevent.TerracottaOperatorEventHistoryProvider;
import com.tc.operatorevent.TerracottaOperatorEventLogger;
import com.tc.operatorevent.TerracottaOperatorEventLogging;
import com.tc.runtime.logging.LongGCLogger;
import com.tc.server.ServerConnectionValidator;
import com.tc.util.Assert;
import com.tc.util.NonBlockingStartupLock;
import com.tc.util.StartupLock;
import com.tc.util.runtime.ThreadDumpUtil;

import java.net.InetAddress;

import javax.management.MBeanServer;
import org.terracotta.persistence.IPersistentStorage;


public class StandardServerBuilder implements ServerBuilder {
  private final HaConfig            haConfig;

  protected final TCSecurityManager securityManager;
  protected final TCLogger          logger;

  public StandardServerBuilder(HaConfig haConfig, TCLogger logger,
                                  TCSecurityManager securityManager) {
    this.logger = logger;
    this.securityManager = securityManager;
    this.logger.info("Standard TSA Server created");
    this.haConfig = haConfig;
  }

  @Override
  public GroupManager<AbstractGroupMessage> createGroupCommManager(L2ConfigurationSetupManager configManager,
                                             StageManager stageManager, ServerID serverNodeID,
                                             StripeIDStateManager stripeStateManager, WeightGeneratorFactory weightGeneratorFactory) {
    return new TCGroupManagerImpl(configManager, stageManager, serverNodeID, this.haConfig.getNodesStore(), securityManager, weightGeneratorFactory);
  }

  @Override
  public ServerConfigurationContext createServerConfigurationContext(StageManager stageManager,
                                                                     LockManager lockMgr,
                                                                     DSOChannelManager channelManager,
                                                                     ChannelStatsImpl channelStats,
                                                                     L2Coordinator coordinator,
                                                                     ServerClientHandshakeManager clientHandshakeManager,
                                                                     GlobalServerStats serverStats,
                                                                     ConnectionIDFactory connectionIdFactory,
                                                                     int maxStageSize,
                                                                     ChannelManager genericChannelManager,
                                                                     DumpHandlerStore dumpHandlerStore) {
    return new ServerConfigurationContextImpl(stageManager,
        lockMgr, channelManager,
        clientHandshakeManager, channelStats, coordinator
    );
  }
  
  @Override
  public GroupManager getClusterGroupCommManager() {
    throw new AssertionError("Not supported");
  }

  @Override
  public void dump() {
    TCLogging.getDumpLogger().info(ThreadDumpUtil.getThreadDump());
  }

  @Override
  public void initializeContext(ConfigurationContext context) {
    // Nothing to initialize here
  }

  @Override
  public L2Coordinator createL2HACoordinator(TCLogger consoleLogger, DistributedObjectServer server,
                                             StageManager stageManager, StateManager stateMgr, 
                                             GroupManager groupCommsManager,
                                             ClusterStatePersistor clusterStatePersistor,
                                             WeightGeneratorFactory weightGeneratorFactory,
                                             L2ConfigurationSetupManager configurationSetupManager,
                                             StripeIDStateManager stripeStateManager) {
    return new L2HACoordinator(consoleLogger, server, stageManager, stateMgr, 
        groupCommsManager, clusterStatePersistor,
        weightGeneratorFactory, configurationSetupManager,
        haConfig.getThisGroupID(), stripeStateManager);
  }

  @Override
  public L2Management createL2Management(boolean listenerEnabled, TCServerInfoMBean tcServerInfoMBean,
                                         L2ConfigurationSetupManager configSetupManager,
                                         DistributedObjectServer distributedObjectServer, InetAddress bind,
                                         int jmxPort, Sink remoteEventsSink,
                                         ServerConnectionValidator serverConnectionValidator,
                                         ServerDBBackupMBean serverDBBackupMBean, TCSecurityManager securityManager) throws Exception {
    return new L2Management(tcServerInfoMBean, configSetupManager, distributedObjectServer, listenerEnabled, bind,
                            jmxPort, remoteEventsSink, securityManager);
  }

  @Override
  public void registerForOperatorEvents(L2Management l2Management,
                                        TerracottaOperatorEventHistoryProvider operatorEventHistoryProvider,
                                        MBeanServer l2MbeanServer) {
    // register logger for OSS version
    TerracottaOperatorEventLogger tcEventLogger = TerracottaOperatorEventLogging.getEventLogger();
    tcEventLogger.registerEventCallback(new TerracottaOperatorEventCallbackLogger());
  }

  @Override
  public LongGCLogger createLongGCLogger(long gcTimeOut) {
    return new LongGCLogger(gcTimeOut);
  }

  @Override
  public StartupLock createStartupLock(TCFile location, boolean retries) {
    return new NonBlockingStartupLock(location, retries);
  }

  @Override
  public Persistor createPersistor(ServiceRegistry serviceRegistry) {
    IPersistentStorage persistentStorage = serviceRegistry.getService(new PersistentStorageServiceConfiguration());
    // We can't fail to find persistence support (at least not in our current environment).
    Assert.assertNotNull(persistentStorage);
    return new Persistor(persistentStorage);
  }

  @Override
  public GroupID getLocalGroupId() {
    return haConfig.getThisGroupID();
  }
}
