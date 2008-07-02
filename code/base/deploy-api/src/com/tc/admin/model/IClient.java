/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.model;

import com.tc.management.beans.logging.InstrumentationLoggingMBean;
import com.tc.management.beans.logging.RuntimeLoggingMBean;
import com.tc.management.beans.logging.RuntimeOutputOptionsMBean;
import com.tc.stats.statistics.Statistic;

import java.util.Map;

public interface IClient extends IClusterNode {
  long getChannelID();  
  String getRemoteAddress();
  Map getL1Statistics();
  Statistic[] getDSOStatistics(String[] names); 

  InstrumentationLoggingMBean getInstrumentationLoggingBean();
  RuntimeLoggingMBean getRuntimeLoggingBean();
  RuntimeOutputOptionsMBean getRuntimeOutputOptionsBean();
  
  void killClient();
}
