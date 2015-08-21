/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.protocol.TCProtocolException;

/**
 * Thrown to indicate a byte format/validation error with TC Wire protocol header blocks
 * 
 * @author teck
 */
public class WireProtocolHeaderFormatException extends TCProtocolException {

  public WireProtocolHeaderFormatException() {
    super();
  }

  public WireProtocolHeaderFormatException(Throwable cause) {
    super(cause);
  }

  public WireProtocolHeaderFormatException(String message, Throwable cause) {
    super(message, cause);
  }

  WireProtocolHeaderFormatException(String message) {
    super(message);
  }

}
