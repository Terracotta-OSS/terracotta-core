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
package com.tc.l2.ha;

import com.tc.net.StripeID;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.util.State;
import java.util.Map;

import java.util.Set;

public interface ClusterState {

  void setNextAvailableChannelID(long next);

  void setStartGlobalMessageID(long next);

  void addNewConnection(ConnectionID id);

  void setStripeID(String clusterID);

  void removeConnection(ConnectionID id);

  long getNextAvailableChannelID();

  long getStartGlobalMessageID();

  StripeID getStripeID();

  Set<ConnectionID> getAllConnections();

  void generateStripeIDIfNeeded();

  void syncActiveState();

  void syncSequenceState();

  void setCurrentState(State currentState);
  
  void reportStateToMap(Map<String, Object> state);

  byte[] getConfigSyncData();

  void setConfigSyncData(byte[] configSyncData);

}
