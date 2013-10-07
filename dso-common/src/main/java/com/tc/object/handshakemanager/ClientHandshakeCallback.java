/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.handshakemanager;

import com.tc.net.NodeID;
import com.tc.object.ClearableCallback;
import com.tc.object.msg.ClientHandshakeMessage;

public interface ClientHandshakeCallback extends ClearableCallback {

  /**
   * Pauses this callback, should be UnInterruptable.
   */
  public void pause(NodeID remoteNode, int disconnected);

  public void unpause(NodeID remoteNode, int disconnected);

  public void initializeHandshake(NodeID thisNode, NodeID remoteNode, ClientHandshakeMessage handshakeMessage);

  public void shutdown(boolean fromShutdownHook);

}
