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
package com.tc.server;

import com.tc.stats.Client;
import org.terracotta.monitoring.PlatformStopException;

import com.tc.objectserver.impl.JMXSubsystem;
import com.tc.productinfo.ProductInfo;
import com.tc.spi.Pauseable;
import com.tc.text.PrettyPrinter;
import com.tc.util.State;
import org.terracotta.server.StopAction;

import java.util.List;

public interface TCServer extends Pauseable {
  String[] processArguments();

  void start() throws Exception;

  void stop(StopAction...restartMode);

  void stopIfPassive(StopAction...restartMode) throws PlatformStopException;

  void stopIfActive(StopAction...restartMode) throws PlatformStopException;

  boolean isStarted();

  boolean isActive();

  boolean isStopped();
  
  boolean isPassiveUnitialized();
  
  boolean isPassiveStandby();
  
  boolean isReconnectWindow();

  boolean isAcceptingClients();
    
  State getState();

  long getStartTime();

  void updateActivateTime();

  long getActivateTime();

  boolean canShutdown();

  void shutdown();

  String getConfig();

  String getL2Identifier();

  int getTSAListenPort();

  int getTSAGroupPort();
  
  int getReconnectWindowTimeout();

  boolean waitUntilShutdown();

  void dump();

  JMXSubsystem getJMX();

  String getClusterState(PrettyPrinter form);

  ProductInfo productInfo();

  List<Client> getConnectedClients();
}
