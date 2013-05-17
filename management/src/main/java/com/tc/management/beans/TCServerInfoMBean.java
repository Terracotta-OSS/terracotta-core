/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;
import com.tc.management.RuntimeStatisticConstants;
import com.tc.management.TerracottaMBean;

import java.io.IOException;
import java.util.Map;

public interface TCServerInfoMBean extends TerracottaMBean, RuntimeStatisticConstants {
  public static final String STOPPED    = "jmx.terracotta.L2.stopped";
  public static final String VERBOSE_GC = "jmx.terracotta.L2.verboseGC";

  boolean isStarted();

  boolean isActive();

  boolean isPassiveUninitialized();

  boolean isPassiveStandby();

  boolean isRecovering();

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

  String getL2Identifier();

  ServerGroupInfo[] getServerGroupInfo();

  int getTSAListenPort();

  int getTSAGroupPort();

  boolean getRestartable();

  boolean isGarbageCollectionEnabled();

  int getGarbageCollectionInterval();

  Map getStatistics();

  long getUsedMemory();

  long getMaxMemory();

  byte[] takeCompressedThreadDump(long requestMillis);

  String getEnvironment();

  String getTCProperties();

  String[] getProcessArguments();

  String getConfig();

  String getState();

  void setRequestDebug(boolean requestDebug);

  boolean getRequestDebug();

  void setBroadcastDebug(boolean broadcastDebug);

  boolean getBroadcastDebug();

  void setCommitDebug(boolean commitDebug);

  boolean getCommitDebug();

  boolean isVerboseGC();

  void setVerboseGC(boolean verboseGC);

  void gc();

  boolean isEnterprise();

  boolean isSecure();

  String getSecurityServiceLocation();

  Integer getSecurityServiceTimeout();

  String getSecurityHostname();

  String getIntraL2Username();

  String getRunningBackup();

  String getBackupStatus(String name) throws IOException;

  String getBackupFailureReason(String name) throws IOException;

  Map<String, String> getBackupStatuses() throws IOException;

  void backup(String name) throws IOException;

  String getResourceState();

  boolean isLegacyProductionModeEnabled();
}
