/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

public class NullMessageMonitor implements MessageMonitor {

  @Override
  public void newIncomingMessage(TCMessage message) {
    return;
  }

  @Override
  public void newOutgoingMessage(TCMessage message) {
    return;
  }

}
