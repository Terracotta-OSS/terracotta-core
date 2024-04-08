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
  
  void disconnectPeer(String nodeName);
  
  void leaveGroup();
}
