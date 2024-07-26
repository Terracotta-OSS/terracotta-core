/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.management.beans;

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

  long getStartTime();

  long getActivateTime();

  void stop();

  boolean stopAndWait();

  boolean isShutdownable();

  void shutdown();

  String getVersion();

  String getBuildID();

  boolean isPatched();

  String getPatchLevel();

  String getPatchVersion();

  String getPatchBuildID();

  String getCopyright();

  String getHealthStatus();

  String getL2Identifier();

  int getTSAListenPort();

  int getTSAGroupPort();

  Map<String, Object> getStatistics();

  long getUsedMemory();

  long getMaxMemory();

  byte[] takeCompressedThreadDump(long requestMillis);

  String getEnvironment();

  String getTCProperties();

  String[] getProcessArguments();

  String getConfig();

  String getState();

  boolean isVerboseGC();
  
  boolean isReconnectWindow();

  boolean isAcceptingClients();
  
  int getReconnectWindowTimeout();

  void setVerboseGC(boolean verboseGC);

  void gc();
  
  void setPipelineMonitoring(boolean monitor);
  
  boolean disconnectClient(String id);
  
  String getClusterState(boolean shortForm);

  String getConnectedClients() throws IOException;

  String getCurrentChannelProperties() throws IOException;
}
