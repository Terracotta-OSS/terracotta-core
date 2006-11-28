/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.transport;

public class TestSynMessage extends TestTransportHandshakeMessage implements SynMessage {

  public boolean isSyn() {
    return true;
  }

  public boolean isSynAck() {
    return false;
  }

  public boolean isAck() {
    return false;
  }
}
