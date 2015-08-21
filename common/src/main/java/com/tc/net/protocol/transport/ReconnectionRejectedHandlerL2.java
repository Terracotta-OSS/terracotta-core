/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

public class ReconnectionRejectedHandlerL2 implements ReconnectionRejectedHandler {

  public static ReconnectionRejectedHandlerL2 SINGLETON = new ReconnectionRejectedHandlerL2();

  private ReconnectionRejectedHandlerL2() {
    // private
  }

  @Override
  public boolean isRetryOnReconnectionRejected() {
    return true;
  }

}
