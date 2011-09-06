/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handshakemanager;

public class ServerClientModeInCompatibleException extends Exception {

  public ServerClientModeInCompatibleException(final String message) {
    super(message);
  }
}
