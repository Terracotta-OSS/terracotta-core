/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handshakemanager;

import com.tc.net.NodeID;
import com.tc.object.msg.ClientHandshakeAckMessage;

public interface ClientHandshakeManager {

  public void initiateHandshake(NodeID remoteNode);

  public void disconnected(NodeID remoteNode);

  public void connected(NodeID remoteNode);

  public void fireNodeErrorIfNecessary(boolean isRejoinEnabled);

  public void acknowledgeHandshake(ClientHandshakeAckMessage handshakeAck);

  public boolean serverIsPersistent();

  public void waitForHandshake();

  public void reset();

  public void shutdown(boolean fromShutdownHook);

  public boolean isShutdown();
}
