/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
