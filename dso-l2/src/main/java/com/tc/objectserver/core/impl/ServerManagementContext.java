/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.management.RemoteManagement;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.object.net.ChannelStats;
import com.tc.object.net.DSOChannelManagerMBean;
import com.tc.objectserver.api.ObjectInstanceMonitorMBean;
import com.tc.objectserver.core.api.GlobalServerStats;
import com.tc.objectserver.locks.LockManagerMBean;

public class ServerManagementContext {

  private final DSOChannelManagerMBean        channelMgr;
  private final GlobalServerStats          serverStats;
  private final ChannelStats                  channelStats;
  private final LockManagerMBean              lockMgr;
  private final ObjectInstanceMonitorMBean    instanceMonitor;
  private final ConnectionPolicy              connectionPolicy;
  private final RemoteManagement              remoteManagement;

  public ServerManagementContext(LockManagerMBean lockMgr, DSOChannelManagerMBean channelMgr,
                                 GlobalServerStats serverStats, ChannelStats channelStats,
                                 ObjectInstanceMonitorMBean instanceMonitor,
                                 ConnectionPolicy connectionPolicy,
                                 RemoteManagement remoteManagement) {
    this.lockMgr = lockMgr;
    this.channelMgr = channelMgr;
    this.serverStats = serverStats;
    this.channelStats = channelStats;
    this.instanceMonitor = instanceMonitor;
    this.connectionPolicy = connectionPolicy;
    this.remoteManagement = remoteManagement;
  }

  public DSOChannelManagerMBean getChannelManager() {
    return this.channelMgr;
  }

  public GlobalServerStats getServerStats() {
    return this.serverStats;
  }

  public ChannelStats getChannelStats() {
    return this.channelStats;
  }

  public LockManagerMBean getLockManager() {
    return this.lockMgr;
  }

  public ObjectInstanceMonitorMBean getInstanceMonitor() {
    return instanceMonitor;
  }

  public ConnectionPolicy getConnectionPolicy() {
    return this.connectionPolicy;
  }

  public RemoteManagement getRemoteManagement() {
    return remoteManagement;
  }

}
