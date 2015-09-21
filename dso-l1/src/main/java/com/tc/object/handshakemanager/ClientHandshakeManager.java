/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handshakemanager;

import com.tc.object.msg.ClientHandshakeAckMessage;


public interface ClientHandshakeManager {
  /**
   * Called when the connection drops to notify the receiver that it should go into a paused state, unless this was expected
   * due to an actual shutdown operation.
   */
  public void disconnected();

  /**
   * Called when the connection is established to notify the receiver that it should now send a handshake over the wire and
   * can now start sending/receiving messages.  Note that the handshake will NOT be sent if the receiver is already in a
   * shutdown operation.
   */
  public void connected();

  /**
   * Called when an attempt to reconnect to the server fails.
   */
  public void fireNodeError();

  /**
   * Called with the ACK from a formerly sent handshake message.
   * This includes information describing the capabilities of the server, such as whether or not it is persistent.
   * 
   * @param handshakeAck
   */
  public void acknowledgeHandshake(ClientHandshakeAckMessage handshakeAck);

  /**
   * @return True if the remote server is running in a persistent mode.
   */
  public boolean serverIsPersistent();

  /**
   * Blocks the caller until an acknowledgement of the handshake has been received, changing the receiver into a running
   * state.
   */
  public void waitForHandshake();

  /**
   * Called to start a shutdown operation.
   * 
   * @param fromShutdownHook True if the request was from a VM shutdown hook, false if it was from an internal code path.
   */
  public void shutdown(boolean fromShutdownHook);

  /**
   * @return True if shutdown(boolean) has been called.  False, otherwise.
   */
  public boolean isShutdown();
}
