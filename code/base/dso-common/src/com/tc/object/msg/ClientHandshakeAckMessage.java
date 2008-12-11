/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.protocol.tcm.TCMessage;

import java.util.Set;

public interface ClientHandshakeAckMessage extends TCMessage {

  public boolean getPersistentServer();

  public void initialize(boolean persistent, Set allNodes, String thisNodeID, String serverVersion);

  public String[] getAllNodes();

  public String getThisNodeId();

  public String getServerVersion();

}
