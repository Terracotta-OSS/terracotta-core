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
import com.tc.objectserver.entity.VoltronMessageSink;
import com.tc.objectserver.handler.VoltronMessageHandler;
import com.tc.spi.Guardian;
import java.util.LinkedHashMap;
import java.util.Map;
import com.tc.text.PrettyPrintable;

public class ServerManagementContext implements PrettyPrintable {

  private final DSOChannelManagerMBean        channelMgr;
  private final ChannelStats                  channelStats;
  private final ConnectionPolicy              connectionPolicy;
  private final Guardian           guardian;
  private final VoltronMessageHandler handler;
  private final VoltronMessageSink msgSink;

  public ServerManagementContext(DSOChannelManagerMBean channelMgr,ChannelStats channelStats,
                                 ConnectionPolicy connectionPolicy, Guardian guard, VoltronMessageHandler handler,
                                 VoltronMessageSink msgs) {
    this.channelMgr = channelMgr;
    this.channelStats = channelStats;
    this.connectionPolicy = connectionPolicy;
    this.guardian = guard;
    this.handler = handler;
    this.msgSink = msgs;
  }

  public DSOChannelManagerMBean getChannelManager() {
    return this.channelMgr;
  }

  public ChannelStats getChannelStats() {
    return this.channelStats;
  }
  
  public ConnectionPolicy getConnectionPolicy() {
    return this.connectionPolicy;
  }
  
  public Guardian getOperationGuardian() {
    return this.guardian;
  }
  
  public VoltronMessageHandler getVoltronMessageHandler() {
    return this.handler;
  }
  
  public VoltronMessageSink getVoltronMessageSink() {
    return this.msgSink;
  }

  @Override
  public Map<String, ?> getStateMap() {
    LinkedHashMap<String, Object> map = new LinkedHashMap<>();
    map.put("messageHandler", this.handler.getStateMap());
    return map;
  }
}
