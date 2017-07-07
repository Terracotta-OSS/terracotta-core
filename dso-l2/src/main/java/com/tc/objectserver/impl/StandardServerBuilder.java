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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ServiceException;
import org.terracotta.entity.ServiceRegistry;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.StageManager;
import com.tc.config.HaConfig;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.ha.L2HACoordinator;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogging;
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
import com.tc.objectserver.handler.ChannelLifeCycleHandler;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.locks.LockManager;
import com.tc.objectserver.persistence.Persistor;
import com.tc.runtime.logging.LongGCLogger;
import com.tc.util.Assert;
import com.tc.util.runtime.ThreadDumpUtil;

import org.terracotta.persistence.IPlatformPersistence;


public class StandardServerBuilder implements ServerBuilder {
  private final HaConfig            haConfig;

  protected final Logger logger;

  public StandardServerBuilder(HaConfig haConfig, Logger logger) {
    this.logger = logger;
    this.haConfig = haConfig;
  }

  @Override
  public GroupManager<AbstractGroupMessage> createGroupCommManager(L2ConfigurationSetupManager configManager,
                                             StageManager stageManager, ServerID serverNodeID,
                                             StripeIDStateManager stripeStateManager, TCSecurityManager mgr, WeightGeneratorFactory weightGeneratorFactory) {
    return new TCGroupManagerImpl(configManager, stageManager, serverNodeID, this.haConfig.getThisNode(), this.haConfig.getNodesStore(), mgr, weightGeneratorFactory);
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
                                                                     ChannelManager genericChannelManager) {
    return new ServerConfigurationContextImpl(stageManager,
        lockMgr, channelManager,
        clientHandshakeManager, channelStats, coordinator
    );
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
  public L2Coordinator createL2HACoordinator(Logger consoleLogger, DistributedObjectServer server,
                                             StageManager stageManager, StateManager stateMgr, 
                                             GroupManager<AbstractGroupMessage> groupCommsManager,
                                             Persistor persistor,
                                             WeightGeneratorFactory weightGeneratorFactory,
                                             L2ConfigurationSetupManager configurationSetupManager,
                                             StripeIDStateManager stripeStateManager, ChannelLifeCycleHandler clm) {
    return new L2HACoordinator(consoleLogger, server, stageManager, stateMgr, 
        groupCommsManager, persistor,
        weightGeneratorFactory, configurationSetupManager, stripeStateManager, clm);
  }

  @Override
  public LongGCLogger createLongGCLogger(long gcTimeOut) {
    return new LongGCLogger(gcTimeOut);
  }

  @Override
  public Persistor createPersistor(ServiceRegistry serviceRegistry) {
    IPlatformPersistence platformPersistence = null;
    try {
      platformPersistence = serviceRegistry.getService(new BasicServiceConfiguration<IPlatformPersistence>(IPlatformPersistence.class));
    } catch (ServiceException e) {
      Assert.fail("Multiple IPlatformPersistence implementations found!");
    }
    // We can't fail to look this up as the implementation will install an in-memory implementation if an on-disk on
    //  wasn't provided
    Assert.assertNotNull(platformPersistence);
    return new Persistor(platformPersistence);
  }
}
