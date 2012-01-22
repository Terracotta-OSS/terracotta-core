/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.statistics;

import com.tc.net.protocol.tcm.ChannelID;

import javax.management.MBeanServerConnection;

public interface StatisticsGateway  {
  public void addStatisticsAgent(ChannelID channelId, MBeanServerConnection mbeanServerConnection);
  public void removeStatisticsAgent(ChannelID channelId);
  public void cleanup();
}
