/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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
  public static final short ERROR_REDIRECT_CONNECTION   = 7;

  public String getMessage();

  public short getErrorType();
}
