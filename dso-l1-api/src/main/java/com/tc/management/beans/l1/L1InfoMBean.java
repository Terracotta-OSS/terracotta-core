/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans.l1;

import com.tc.management.RuntimeStatisticConstants;
import com.tc.management.TerracottaMBean;
import com.tc.statistics.StatisticData;

import java.util.Map;

import javax.management.NotificationEmitter;

public interface L1InfoMBean extends TerracottaMBean, NotificationEmitter, RuntimeStatisticConstants {
  public static final String VERBOSE_GC = "jmx.terracotta.L1.verboseGC";

  String getVersion();

  String getMavenArtifactsVersion();

  String getBuildID();

  boolean isPatched();

  String getPatchLevel();

  String getPatchVersion();

  String getPatchBuildID();

  String getCopyright();

  String takeThreadDump(long requestMillis);

  byte[] takeCompressedThreadDump(long requestMillis);

  void startBeanShell(int port);

  String getEnvironment();

  String getConfig();

  String[] getCpuStatNames();

  Map getStatistics();

  long getUsedMemory();

  long getMaxMemory();

  StatisticData getCpuLoad();

  StatisticData[] getCpuUsage();

  boolean isVerboseGC();

  void setVerboseGC(boolean verboseGC);

  void gc();

  String getTCProperties();

  String[] getProcessArguments();
}
