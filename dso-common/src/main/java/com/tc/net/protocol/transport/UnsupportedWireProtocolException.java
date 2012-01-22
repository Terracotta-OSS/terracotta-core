/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

/**
 * Thrown by wire protocol routers when a particular wire protocol sub-protocol is not supported
 * 
 * @author teck
 */
public class UnsupportedWireProtocolException extends WireProtocolException {

  public UnsupportedWireProtocolException() {
    super();
  }

  public UnsupportedWireProtocolException(String message) {
    super(message);
  }

  public UnsupportedWireProtocolException(Throwable cause) {
    super(cause);
  }

  public UnsupportedWireProtocolException(String message, Throwable cause) {
    super(message, cause);
  }

}
