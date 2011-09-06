/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.object.net.ChannelStats;
import com.tc.object.net.DSOChannelManagerMBean;
import com.tc.objectserver.DSOApplicationEventsMBean;
import com.tc.objectserver.api.ObjectInstanceMonitorMBean;
import com.tc.objectserver.api.ObjectManagerMBean;
import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.objectserver.locks.LockManagerMBean;
import com.tc.objectserver.search.IndexManager;
import com.tc.objectserver.tx.ServerTransactionManagerMBean;

public class ServerManagementContext {

  private final ServerTransactionManagerMBean txnMgr;
  private final ObjectManagerMBean            objMgr;
  private final DSOChannelManagerMBean        channelMgr;
  private final DSOGlobalServerStats          serverStats;
  private final ChannelStats                  channelStats;
  private final LockManagerMBean              lockMgr;
  private final ObjectInstanceMonitorMBean    instanceMonitor;
  private final DSOApplicationEventsMBean     appEvents;
  private final IndexManager                  indexManager;
  private final ConnectionPolicy              connectionPolicy;

  public ServerManagementContext(ServerTransactionManagerMBean txnMgr, ObjectManagerMBean objMgr,
                                 LockManagerMBean lockMgr, DSOChannelManagerMBean channelMgr,
                                 DSOGlobalServerStats serverStats, ChannelStats channelStats,
                                 ObjectInstanceMonitorMBean instanceMonitor, DSOApplicationEventsMBean appEvents,
                                 IndexManager indexManager, ConnectionPolicy connectionPolicy) {
    this.txnMgr = txnMgr;
    this.objMgr = objMgr;
    this.lockMgr = lockMgr;
    this.channelMgr = channelMgr;
    this.serverStats = serverStats;
    this.channelStats = channelStats;
    this.instanceMonitor = instanceMonitor;
    this.appEvents = appEvents;
    this.indexManager = indexManager;
    this.connectionPolicy = connectionPolicy;
  }

  public IndexManager getIndexManager() {
    return indexManager;
  }

  public ServerTransactionManagerMBean getTransactionManager() {
    return this.txnMgr;
  }

  public ObjectManagerMBean getObjectManager() {
    return this.objMgr;
  }

  public DSOChannelManagerMBean getChannelManager() {
    return this.channelMgr;
  }

  public DSOGlobalServerStats getServerStats() {
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

  public DSOApplicationEventsMBean getDSOAppEventsMBean() {
    return this.appEvents;
  }

  public ConnectionPolicy getConnectionPolicy() {
    return this.connectionPolicy;
  }
}
