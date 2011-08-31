/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

public interface TransportHandshakeError {
  /**
   * These are the types of error that may happen at the time of handshake
   */
  public static final short ERROR_NONE                  = 0;
  public static final short ERROR_HANDSHAKE             = 1;
  public static final short ERROR_INVALID_CONNECTION_ID = 2;
  public static final short ERROR_STACK_MISMATCH        = 3;
  public static final short ERROR_GENERIC               = 4;
  public static final short ERROR_MAX_CONNECTION_EXCEED = 5;
  public static final short ERROR_RECONNECTION_REJECTED = 6;

  public String getMessage();

  public short getErrorType();
}
