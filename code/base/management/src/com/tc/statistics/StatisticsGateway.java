/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics;

import com.tc.net.protocol.tcm.ChannelID;

import javax.management.MBeanServerConnection;

public interface StatisticsGateway  {
  public void addStatisticsAgent(ChannelID channelId, MBeanServerConnection mbeanServerConnection);
  public void removeStatisticsAgent(ChannelID channelId);
  public void cleanup();
}