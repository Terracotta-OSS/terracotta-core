/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

public class TransportHandshakeErrorContext implements TransportHandshakeError {
  private String    message;
  private short     errorType;
  private Throwable throwable;

  public TransportHandshakeErrorContext(String message) {
    this.message = message;
    this.errorType = ERROR_GENERIC;
  }

  public TransportHandshakeErrorContext(String message, Throwable throwable) {
    this(message);
    this.errorType = ERROR_GENERIC;
    this.throwable = throwable;
  }

  public TransportHandshakeErrorContext(String message, short errorType) {
    this(message);
    this.errorType = errorType;
  }

  public String getMessage() {
    return message;
  }

  public short getErrorType() {
    return errorType;
  }

  public String toString() {
    StringBuffer rv = new StringBuffer(getClass().getName() + ": " + this.message);
    if (this.throwable != null) {
      rv.append(", throwable=" + throwable.getMessage());
    }
    return rv.toString();
  }
}