/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import java.util.Map;

public interface TCServerInfoMBean {

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

  boolean isProduction();
}
