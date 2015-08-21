/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.handshakemanager;

import com.tc.object.ClearableCallback;
import com.tc.object.msg.ClientHandshakeMessage;

public interface ClientHandshakeCallback extends ClearableCallback {

  /**
   * Pauses this callback, should be UnInterruptable.
   */
  public void pause();

  public void unpause();

  public void initializeHandshake(ClientHandshakeMessage handshakeMessage);

  public void shutdown(boolean fromShutdownHook);

}
