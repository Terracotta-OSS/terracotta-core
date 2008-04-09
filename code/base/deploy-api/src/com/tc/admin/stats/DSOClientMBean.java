/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.stats;

import com.tc.management.TerracottaMBean;
import com.tc.management.beans.l1.L1InfoMBean;
import com.tc.management.beans.logging.InstrumentationLoggingMBean;
import com.tc.management.beans.logging.RuntimeLoggingMBean;
import com.tc.management.beans.logging.RuntimeOutputOptionsMBean;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.stats.statistics.CountStatistic;
import com.tc.stats.statistics.Statistic;

import javax.management.ObjectName;

public interface DSOClientMBean extends TerracottaMBean {
  public static final String TUNNELED_BEANS_REGISTERED = "tunneled.beans.registered";

  boolean isTunneledBeansRegistered();

  ObjectName getL1InfoBeanName();

  L1InfoMBean getL1InfoBean();

  ObjectName getInstrumentationLoggingBeanName();

  InstrumentationLoggingMBean getInstrumentationLoggingBean();

  ObjectName getRuntimeLoggingBeanName();

  RuntimeLoggingMBean getRuntimeLoggingBean();

  ObjectName getRuntimeOutputOptionsBeanName();

  RuntimeOutputOptionsMBean getRuntimeOutputOptionsBean();

  ChannelID getChannelID();

  String getRemoteAddress();

  CountStatistic getTransactionRate();

  CountStatistic getObjectFaultRate();

  CountStatistic getObjectFlushRate();

  CountStatistic getPendingTransactionsCount();

  Statistic[] getStatistics(String[] names);

  void killClient();
}
