/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.ha;

import com.tc.net.GroupID;
import com.tc.net.StripeID;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.util.State;

import java.util.Map;
import java.util.Set;

public interface ClusterState {

  void setNextAvailableChannelID(long next);

  void addNewConnection(ConnectionID id);

  void addToStripeIDMap(GroupID gid, StripeID stripeID);

  void setStripeID(String clusterID);

  void removeConnection(ConnectionID id);

  long getNextAvailableChannelID();

  StripeID getStripeID();

  Set<ConnectionID> getAllConnections();

  Map<GroupID, StripeID> getStripeIDMap();

  void generateStripeIDIfNeeded();

  void syncActiveState();

  void syncSequenceState();

  void setCurrentState(State currentState);

}
