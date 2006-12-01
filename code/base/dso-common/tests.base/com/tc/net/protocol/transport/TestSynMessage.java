/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
