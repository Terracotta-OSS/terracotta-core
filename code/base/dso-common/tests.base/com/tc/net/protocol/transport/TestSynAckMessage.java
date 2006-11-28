/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.exception.ImplementMe;

public class TestSynAckMessage extends TestTransportHandshakeMessage implements SynAckMessage {

  public String getErrorContext() {
    throw new ImplementMe();
  }

  public boolean hasErrorContext() {
    throw new ImplementMe();
  }

  public boolean isSyn() {
    return false;
  }

  public boolean isSynAck() {
    return true;
  }

  public boolean isAck() {
    return false;
  }

  public void recycle() {
    return;
  }

}
