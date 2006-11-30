/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
