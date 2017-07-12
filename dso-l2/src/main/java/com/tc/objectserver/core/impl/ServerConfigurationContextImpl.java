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
package com.tc.objectserver.core.impl;

import com.tc.async.api.StageManager;
import com.tc.async.impl.ConfigurationContextImpl;
import com.tc.l2.api.L2Coordinator;
import com.tc.object.net.ChannelStats;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;

/**
 * App specific configuration context
 * 
 * @author steve
 */
public class ServerConfigurationContextImpl extends ConfigurationContextImpl implements ServerConfigurationContext {

  private final DSOChannelManager              channelManager;
  private final ServerClientHandshakeManager   clientHandshakeManager;
  private final ChannelStats                   channelStats;
  private final L2Coordinator                  l2Coordinator;

  public ServerConfigurationContextImpl(StageManager stageManager,
                                        DSOChannelManager channelManager,
                                        ServerClientHandshakeManager clientHandshakeManager,
                                        ChannelStats channelStats, L2Coordinator l2Coordinator) {
    super(stageManager);
    this.channelManager = channelManager;
    this.clientHandshakeManager = clientHandshakeManager;
    this.channelStats = channelStats;
    this.l2Coordinator = l2Coordinator;
  }

  @Override
  public L2Coordinator getL2Coordinator() {
    return l2Coordinator;
  }
  
  @Override
  public DSOChannelManager getChannelManager() {
    return channelManager;
  }

  @Override
  public ServerClientHandshakeManager getClientHandshakeManager() {
    return clientHandshakeManager;
  }

  @Override
  public ChannelStats getChannelStats() {
    return this.channelStats;
  }
}