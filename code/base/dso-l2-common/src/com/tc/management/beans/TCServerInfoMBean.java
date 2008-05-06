/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.config.schema.L2Info;
import com.tc.management.RuntimeStatisticConstants;
import com.tc.management.TerracottaMBean;
import com.tc.statistics.StatisticData;

import java.util.Map;

public interface TCServerInfoMBean extends TerracottaMBean, RuntimeStatisticConstants {

  boolean isStarted();

  boolean isActive();

  boolean isPassiveUninitialized();

  boolean isPassiveStandby();

  long getStartTime();

  long getActivateTime();

  void stop();

  boolean isShutdownable();
  
  void shutdown();

  void startBeanShell(int port);

  String getVersion();

  String getBuildID();

  String getCopyright();

  String getHealthStatus();

  String getDescriptionOfCapabilities();

  L2Info[] getL2Info();

  int getDSOListenPort();
  
  String[] getCpuStatNames();
  
  Map getStatistics();
  
  StatisticData[] getCpuUsage();

  String takeThreadDump(long requestMillis);
  
  String getEnvironment();
  
  String getConfig();
}
