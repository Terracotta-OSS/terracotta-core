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

import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.object.net.ChannelStats;
import com.tc.object.net.DSOChannelManagerMBean;
import com.tc.objectserver.core.api.GlobalServerStats;

public class ServerManagementContext {

  private final DSOChannelManagerMBean        channelMgr;
  private final GlobalServerStats          serverStats;
  private final ChannelStats                  channelStats;
  private final ConnectionPolicy              connectionPolicy;

  public ServerManagementContext(DSOChannelManagerMBean channelMgr,
                                 GlobalServerStats serverStats, ChannelStats channelStats,
                                 ConnectionPolicy connectionPolicy) {
    this.channelMgr = channelMgr;
    this.serverStats = serverStats;
    this.channelStats = channelStats;
    this.connectionPolicy = connectionPolicy;
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
  
  public ConnectionPolicy getConnectionPolicy() {
    return this.connectionPolicy;
  }
}
