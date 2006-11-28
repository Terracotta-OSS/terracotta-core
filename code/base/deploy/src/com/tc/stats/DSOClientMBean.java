package com.tc.stats;

import com.tc.management.TerracottaMBean;
import com.tc.net.protocol.tcm.ChannelID;

import javax.management.j2ee.statistics.CountStatistic;
import javax.management.j2ee.statistics.Statistic;

public interface DSOClientMBean extends TerracottaMBean {

  ChannelID getChannelID();

  String getRemoteAddress();

  CountStatistic getTransactionRate();

  CountStatistic getObjectFaultRate();

  CountStatistic getObjectFlushRate();

  Statistic[] getStatistics(String[] names);

}
