/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.protocol.TCProtocolException;

/**
 * Generic wire protocol exception
 * 
 * @author teck
 */
public class WireProtocolException extends TCProtocolException {

  public WireProtocolException() {
    super();
  }

  public WireProtocolException(String message) {
    super(message);
  }

  public WireProtocolException(Throwable cause) {
    super(cause);
  }

  public WireProtocolException(String message, Throwable cause) {
    super(message, cause);
  }

}
