/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

public class NullMessageMonitor implements MessageMonitor {

  public void newIncomingMessage(TCMessage message) {
    return;
  }

  public void newOutgoingMessage(TCMessage message) {
    return;
  }

}
