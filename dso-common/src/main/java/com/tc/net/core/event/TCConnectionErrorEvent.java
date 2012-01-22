/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.core.event;

import com.tc.net.core.TCConnection;
import com.tc.net.protocol.TCNetworkMessage;

/**
 * A special flavor of TCConnectionEvent indicating an error on a specific connection
 * 
 * @author teck
 */
public class TCConnectionErrorEvent extends TCConnectionEvent {

  private final Exception        exception;
  private final TCNetworkMessage context;

  public TCConnectionErrorEvent(TCConnection connection, final Exception exception, final TCNetworkMessage context) {
    super(connection);
    this.exception = exception;
    this.context = context;
  }

  /**
   * The exception thrown by an IO operation on this connection
   */
  public Exception getException() {
    return exception;
  }

  /**
   * If relevant, the message instance that was being used for the IO operation. Can be null
   */
  public TCNetworkMessage getMessageContext() {
    return context;
  }

  public String toString() {
    return getSource() + ", exception: " + ((exception != null) ? exception.toString() : "[null exception]")
           + ", message context: " + ((context != null) ? context.toString() : "[no message context]");
  }
}
