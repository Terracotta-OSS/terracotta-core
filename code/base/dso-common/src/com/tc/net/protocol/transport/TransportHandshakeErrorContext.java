/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.transport;

public class TransportHandshakeErrorContext {
  private String    message;
  private Throwable throwable;

  public TransportHandshakeErrorContext(String message) {
    this.message = message;
  }

  public TransportHandshakeErrorContext(String message, Throwable throwable) {
    this(message);
    this.throwable = throwable;
  }

  public String getMessage() {
    return message;
  }

  public String toString() {
    StringBuffer rv = new StringBuffer(getClass().getName() + ": " + this.message);
    if (this.throwable != null) {
      rv.append(", throwable=" + throwable.getMessage());
    }
    return rv.toString();
  }
}