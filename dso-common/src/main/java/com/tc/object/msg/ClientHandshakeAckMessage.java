/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.net.StripeID;
import com.tc.net.protocol.tcm.TCMessage;

import java.util.Map;
import java.util.Set;

public interface ClientHandshakeAckMessage extends TCMessage {

  public boolean getPersistentServer();

  public void initialize(boolean persistent, Set<ClientID> allNodes, ClientID thisNodeID, String serverVersion,
                         GroupID thisGroup, StripeID stripeID, Map<GroupID, StripeID> stripeIDMap);

  public ClientID[] getAllNodes();

  public ClientID getThisNodeId();

  public String getServerVersion();

  public GroupID getGroupID();

  public StripeID getStripeID();
  
  public Map<GroupID, StripeID> getStripeIDMap();

}
