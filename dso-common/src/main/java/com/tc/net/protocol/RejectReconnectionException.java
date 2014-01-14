/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.transport.ConnectionID;

/**
 * Thrown by Stack providers when reconnection attempt is rejected (meaning subsequent reconnects will also be rejected)
 */
public class RejectReconnectionException extends Exception {

  public RejectReconnectionException(String reason, TCSocketAddress socketAddress) {
    super("Connection attempts from the Terracotta node at " + socketAddress
          + " are being rejected by the Terracotta server array. Reason: " + reason);
  }
}
