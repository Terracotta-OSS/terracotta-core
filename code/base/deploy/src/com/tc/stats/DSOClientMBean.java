/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.stats;

import com.tc.management.TerracottaMBean;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.stats.statistics.CountStatistic;
import com.tc.stats.statistics.Statistic;

public interface DSOClientMBean extends TerracottaMBean {

  ChannelID getChannelID();

  String getRemoteAddress();

  CountStatistic getTransactionRate();

  CountStatistic getObjectFaultRate();

  CountStatistic getObjectFlushRate();

  Statistic[] getStatistics(String[] names);

}
