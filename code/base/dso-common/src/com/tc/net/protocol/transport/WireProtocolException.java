/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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