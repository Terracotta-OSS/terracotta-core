/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;
import com.tc.management.RuntimeStatisticConstants;
import com.tc.management.TerracottaMBean;
import com.tc.statistics.StatisticData;

import java.util.Map;

public interface TCServerInfoMBean extends TerracottaMBean, RuntimeStatisticConstants {
  public static final String STOPPED    = "jmx.terracotta.L2.stopped";
  public static final String VERBOSE_GC = "jmx.terracotta.L2.verboseGC";

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

  String getMavenArtifactsVersion();

  String getBuildID();

  boolean isPatched();

  String getPatchLevel();

  String getPatchVersion();

  String getPatchBuildID();

  String getCopyright();

  String getHealthStatus();

  String getDescriptionOfCapabilities();

  L2Info[] getL2Info();

  ServerGroupInfo[] getServerGroupInfo();

  int getDSOListenPort();

  int getDSOGroupPort();

  String getPersistenceMode();

  String getFailoverMode();

  boolean isGarbageCollectionEnabled();

  int getGarbageCollectionInterval();

  String[] getCpuStatNames();

  Map getStatistics();

  long getUsedMemory();

  long getMaxMemory();

  StatisticData[] getCpuUsage();

  StatisticData getCpuLoad();

  byte[] takeCompressedThreadDump(long requestMillis);

  String getEnvironment();

  String getTCProperties();

  String[] getProcessArguments();

  String getConfig();

  String getState();

  void setFaultDebug(boolean faultDebug);

  boolean getFaultDebug();

  void setRequestDebug(boolean requestDebug);

  boolean getRequestDebug();

  void setFlushDebug(boolean flushDebug);

  boolean getFlushDebug();

  void setBroadcastDebug(boolean broadcastDebug);

  boolean getBroadcastDebug();

  void setCommitDebug(boolean commitDebug);

  boolean getCommitDebug();

  boolean isVerboseGC();

  void setVerboseGC(boolean verboseGC);

  void gc();

  boolean isEnterprise();
}
