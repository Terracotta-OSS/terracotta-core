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
package com.tc.l2.ha;

import com.tc.net.StripeID;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.util.State;
import java.util.Map;

import java.util.Set;

public interface ClusterState {

  void setNextAvailableChannelID(long next);

  void addNewConnection(ConnectionID id);

  void setStripeID(String clusterID);

  void removeConnection(ConnectionID id);

  long getNextAvailableChannelID();

  StripeID getStripeID();

  Set<ConnectionID> getAllConnections();

  void generateStripeIDIfNeeded();

  void syncActiveState();

  void syncSequenceState();

  void setCurrentState(State currentState);
  
  void reportStateToMap(Map<String, Object> state);

}
