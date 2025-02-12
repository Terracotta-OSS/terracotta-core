/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
  private final List<Runnable>                 shutdownItems = Collections.synchronizedList(new LinkedList<>());

  public ServerConfigurationContextImpl(String identifier, StageManager stageManager,
                                        DSOChannelManager channelManager,
                                        ServerClientHandshakeManager clientHandshakeManager,
                                        ChannelStats channelStats, L2Coordinator l2Coordinator) {
    super(identifier, stageManager);
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
  public ServerClientHandshakeManager getClientHandshakeManager() {
    return clientHandshakeManager;
  }

  @Override
  public ChannelStats getChannelStats() {
    return this.channelStats;
  }

  @Override
  public void addShutdownItem(Runnable c) {
    shutdownItems.add(c);
  }

  @Override
  public void shutdown() {
    shutdownItems.forEach(Runnable::run);
    clientHandshakeManager.stop();
  }
}