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

import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.l2.state.StateChangeListener;
import com.tc.util.State;

import java.nio.charset.Charset;


public interface TCServer extends StateChangeListener {
  String[] processArguments();

  void start() throws Exception;

  void stop();

  boolean isStarted();

  boolean isActive();

  boolean isStopped();
  
  boolean isPassiveUnitialized();
  
  boolean isPassiveStandby();
  
  boolean isReconnectWindow();

  State getState();

  byte[] getClusterState(Charset charset);

  long getStartTime();

  void updateActivateTime();

  long getActivateTime();

  boolean canShutdown();

  void shutdown();

  String getConfig();

  String getDescriptionOfCapabilities();

  L2Info[] infoForAllL2s();

  String getL2Identifier();

  ServerGroupInfo getStripeInfo();

  int getTSAListenPort();

  int getTSAGroupPort();
  
  int getReconnectWindowTimeout();

  void waitUntilShutdown();

  void dump();

  void dumpClusterState();

  void reloadConfiguration() throws ConfigurationSetupException;

  String getResourceState();

}
