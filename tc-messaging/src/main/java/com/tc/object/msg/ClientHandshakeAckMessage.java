/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.TCMessage;

import java.util.Set;

public interface ClientHandshakeAckMessage extends TCMessage {

  public boolean getPersistentServer();

  public void initialize(boolean persistent, Set<? extends NodeID> allNodes, ClientID thisNodeID, String serverVersion);

  public ClientID[] getAllNodes();

  public ClientID getThisNodeId();

  public String getServerVersion();

}
