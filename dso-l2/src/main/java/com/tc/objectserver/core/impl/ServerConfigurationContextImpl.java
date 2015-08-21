/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.async.api.StageManager;
import com.tc.async.impl.ConfigurationContextImpl;
import com.tc.l2.api.L2Coordinator;
import com.tc.object.net.ChannelStats;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.locks.LockManager;

/**
 * App specific configuration context
 * 
 * @author steve
 */
public class ServerConfigurationContextImpl extends ConfigurationContextImpl implements ServerConfigurationContext {

  private final LockManager                    lockManager;
  private final DSOChannelManager              channelManager;
  private final ServerClientHandshakeManager   clientHandshakeManager;
  private final ChannelStats                   channelStats;
  private final L2Coordinator                  l2Coordinator;

  public ServerConfigurationContextImpl(StageManager stageManager,
                                        LockManager lockManager,
                                        DSOChannelManager channelManager,
                                        ServerClientHandshakeManager clientHandshakeManager,
                                        ChannelStats channelStats, L2Coordinator l2Coordinator) {
    super(stageManager);
    this.lockManager = lockManager;
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
  public LockManager getLockManager() {
    return lockManager;
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