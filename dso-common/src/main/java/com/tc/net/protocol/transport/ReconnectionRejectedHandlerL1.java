/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

public class ReconnectionRejectedHandlerL1 implements ReconnectionRejectedHandler {

  public static ReconnectionRejectedHandlerL1 SINGLETON = new ReconnectionRejectedHandlerL1();

  private ReconnectionRejectedHandlerL1() {
    // private
  }

  @Override
  public boolean isRetryOnReconnectionRejected() {
    return false;
  }

}
