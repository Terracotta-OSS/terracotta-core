/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.handshakemanager;

import com.tc.net.NodeID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.platform.rejoin.ClearableCallback;

public interface ClientHandshakeCallback extends ClearableCallback {

  public void pause(NodeID remoteNode, int disconnected);

  public void unpause(NodeID remoteNode, int disconnected);

  public void initializeHandshake(NodeID thisNode, NodeID remoteNode, ClientHandshakeMessage handshakeMessage);

  public void shutdown();

}
