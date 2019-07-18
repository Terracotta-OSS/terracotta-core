/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.server;

import org.terracotta.monitoring.PlatformService.RestartMode;
import org.terracotta.monitoring.PlatformStopException;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.text.PrettyPrinter;
import com.tc.util.State;


public interface TCServer {
  String[] processArguments();

  void start() throws Exception;

  void stop();

  void stop(RestartMode restartMode);

  void stopIfPassive(RestartMode restartMode) throws PlatformStopException;

  void stopIfActive(RestartMode restartMode) throws PlatformStopException;

  boolean isStarted();

  boolean isActive();

  boolean isStopped();
  
  boolean isPassiveUnitialized();
  
  boolean isPassiveStandby();
  
  boolean isReconnectWindow();
    
  State getState();

  long getStartTime();

  void updateActivateTime();

  long getActivateTime();

  boolean canShutdown();

  void shutdown();

  String getConfig();

  String getDescriptionOfCapabilities();

  String getL2Identifier();

  int getTSAListenPort();

  int getTSAGroupPort();
  
  int getReconnectWindowTimeout();

  void waitUntilShutdown();

  void dump();

  void reloadConfiguration() throws ConfigurationSetupException;

  String getResourceState();

  String getClusterState(PrettyPrinter form);
}
