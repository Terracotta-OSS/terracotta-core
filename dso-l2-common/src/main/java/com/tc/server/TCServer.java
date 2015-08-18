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
package com.tc.server;

import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;
import com.tc.config.schema.setup.ConfigurationSetupException;

import java.io.IOException;
import java.util.Map;

public interface TCServer {
  String[] processArguments();

  void start() throws Exception;

  void stop();

  boolean isStarted();

  boolean isActive();

  boolean isStopped();

  long getStartTime();

  void updateActivateTime();

  long getActivateTime();

  boolean canShutdown();

  void shutdown();

  boolean isGarbageCollectionEnabled();

  int getGarbageCollectionInterval();

  String getConfig();

  boolean getRestartable();

  String getDescriptionOfCapabilities();

  L2Info[] infoForAllL2s();

  String getL2Identifier();

  ServerGroupInfo[] serverGroups();

  int getTSAListenPort();

  int getTSAGroupPort();

  int getManagementPort();

  void waitUntilShutdown();

  void dump();

  void dumpClusterState();

  void reloadConfiguration() throws ConfigurationSetupException;

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

}
