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
import org.terracotta.entity.ServiceRegistry;

import com.tc.async.api.PostInit;
import com.tc.async.api.StageManager;
import com.tc.config.ServerConfigurationManager;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.l2.state.ConsistencyManager;
import com.tc.l2.state.StateManager;
import com.tc.net.ServerID;
import com.tc.net.core.BufferManagerFactory;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.StripeIDStateManager;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.persistence.Persistor;

import java.io.IOException;


public interface ServerBuilder extends PostInit {
  GroupManager<AbstractGroupMessage> createGroupCommManager(ServerConfigurationManager configManager,
                                                            StageManager stageManager, 
                                                            TCConnectionManager connectionManager,
                                                            ServerID serverNodeID,
                                                            StripeIDStateManager stripeStateManager, WeightGeneratorFactory weightGeneratorFactory,
                                                            BufferManagerFactory bufferManagerFactory);

  ServerConfigurationContext createServerConfigurationContext(String id, StageManager stageManager, DSOChannelManager channelManager,
                                                              ChannelStatsImpl channelStats,
                                                              L2Coordinator l2HACoordinator,
                                                              ServerClientHandshakeManager clientHandshakeManager,
                                                              ConnectionIDFactory connectionIdFactory);

  L2Coordinator createL2HACoordinator(Logger consoleLogger, DistributedObjectServer server,
                                      StateManager stateMgr,
                                      GroupManager<AbstractGroupMessage> groupCommsManager,
                                      Persistor clusterStatePersistor,
                                      WeightGeneratorFactory weightGeneratorFactory,
                                      StripeIDStateManager stripeStateManager,
                                      ConsistencyManager consistency);

  Persistor createPersistor(ServiceRegistry serviceRegistry) throws IOException;
}
