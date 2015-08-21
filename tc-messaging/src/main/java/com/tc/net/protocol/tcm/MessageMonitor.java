/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

public interface MessageMonitor {

  public void newIncomingMessage(TCMessage message);

  public void newOutgoingMessage(TCMessage message);

}
