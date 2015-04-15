/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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

  int getManagementPort();

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
