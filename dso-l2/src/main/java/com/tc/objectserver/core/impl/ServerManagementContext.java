/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.core.impl;

import com.tc.management.RemoteManagement;
import com.tc.management.RemoteManagementException;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.object.management.ManagementRequestID;
import com.tc.object.management.RemoteCallDescriptor;
import com.tc.object.management.RemoteCallHolder;
import com.tc.object.management.ResponseHolder;
import com.tc.object.management.TCManagementSerializationException;
import com.tc.object.msg.AbstractManagementMessage;
import com.tc.object.msg.InvokeRegisteredServiceMessage;
import com.tc.object.msg.InvokeRegisteredServiceResponseMessage;
import com.tc.object.msg.ListRegisteredServicesMessage;
import com.tc.object.msg.ListRegisteredServicesResponseMessage;
import com.tc.object.net.ChannelStats;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerMBean;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.api.ObjectInstanceMonitorMBean;
import com.tc.objectserver.api.ObjectManagerMBean;
import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.objectserver.handler.ServerManagementHandler;
import com.tc.objectserver.locks.LockManagerMBean;
import com.tc.objectserver.search.IndexManager;
import com.tc.objectserver.tx.ServerTransactionManagerMBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class ServerManagementContext {

  private final ServerTransactionManagerMBean txnMgr;
  private final ObjectManagerMBean            objMgr;
  private final DSOChannelManagerMBean        channelMgr;
  private final DSOGlobalServerStats          serverStats;
  private final ChannelStats                  channelStats;
  private final LockManagerMBean              lockMgr;
  private final ObjectInstanceMonitorMBean    instanceMonitor;
  private final IndexManager                  indexManager;
  private final ConnectionPolicy              connectionPolicy;
  private final RemoteManagement              remoteManagement;

  public ServerManagementContext(ServerTransactionManagerMBean txnMgr, ObjectManagerMBean objMgr,
                                 LockManagerMBean lockMgr, DSOChannelManagerMBean channelMgr,
                                 DSOGlobalServerStats serverStats, ChannelStats channelStats,
                                 ObjectInstanceMonitorMBean instanceMonitor,
                                 IndexManager indexManager, ConnectionPolicy connectionPolicy,
                                 RemoteManagement remoteManagement) {
    this.txnMgr = txnMgr;
    this.objMgr = objMgr;
    this.lockMgr = lockMgr;
    this.channelMgr = channelMgr;
    this.serverStats = serverStats;
    this.channelStats = channelStats;
    this.instanceMonitor = instanceMonitor;
    this.indexManager = indexManager;
    this.connectionPolicy = connectionPolicy;
    this.remoteManagement = remoteManagement;
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

  public ConnectionPolicy getConnectionPolicy() {
    return this.connectionPolicy;
  }

  public RemoteManagement getRemoteManagement() {
    return remoteManagement;
  }

}
