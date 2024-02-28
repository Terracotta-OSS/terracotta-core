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
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ServiceException;
import org.terracotta.entity.ServiceRegistry;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.StageManager;
import com.tc.config.ServerConfigurationManager;
import com.tc.config.GroupConfiguration;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.ha.L2HACoordinator;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.l2.state.ConsistencyManager;
import com.tc.l2.state.StateManager;
import com.tc.net.ServerID;
import com.tc.net.core.BufferManagerFactory;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.StripeIDStateManager;
import com.tc.net.groups.TCGroupManagerImpl;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ServerConfigurationContextImpl;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.persistence.Persistor;
import com.tc.util.Assert;

import org.terracotta.persistence.IPlatformPersistence;


public class StandardServerBuilder implements ServerBuilder {
  private final GroupConfiguration groupConfiguration;

  protected final Logger logger;

  public StandardServerBuilder(GroupConfiguration groupConfiguration, Logger logger) {
    this.logger = logger;
    this.groupConfiguration = groupConfiguration;
  }

  @Override
  public GroupManager<AbstractGroupMessage> createGroupCommManager(ServerConfigurationManager configManager,
                                                                   StageManager stageManager, 
                                                                   TCConnectionManager connections,
                                                                   ServerID serverNodeID,
                                                                   StripeIDStateManager stripeStateManager, WeightGeneratorFactory weightGeneratorFactory,
                                                                   BufferManagerFactory bufferManagerFactory) {
    return new TCGroupManagerImpl(configManager, stageManager, connections, serverNodeID, this.groupConfiguration.getCurrentNode(),
                                  weightGeneratorFactory, bufferManagerFactory);
  }

  @Override
  public ServerConfigurationContext createServerConfigurationContext(String id, StageManager stageManager,
                                                                     DSOChannelManager channelManager,
                                                                     ChannelStatsImpl channelStats,
                                                                     L2Coordinator coordinator,
                                                                     ServerClientHandshakeManager clientHandshakeManager,
                                                                     ConnectionIDFactory connectionIdFactory) {
    return new ServerConfigurationContextImpl(id, stageManager,
        channelManager,
        clientHandshakeManager, channelStats, coordinator
    );
  }
  
  @Override
  public void initializeContext(ConfigurationContext context) {
    // Nothing to initialize here
  }

  @Override
  public L2Coordinator createL2HACoordinator(Logger consoleLogger, DistributedObjectServer server,
                                             StateManager stateMgr, 
                                             GroupManager<AbstractGroupMessage> groupCommsManager,
                                             Persistor persistor,
                                             WeightGeneratorFactory weightGeneratorFactory,
                                             StripeIDStateManager stripeStateManager,
                                             ConsistencyManager consistencyMgr) {
    return new L2HACoordinator(consoleLogger, server, stateMgr, 
        groupCommsManager, persistor,
        weightGeneratorFactory, stripeStateManager, consistencyMgr);
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
