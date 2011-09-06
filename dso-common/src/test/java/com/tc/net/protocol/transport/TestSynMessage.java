/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.protocol.NetworkLayer;

public class TestSynMessage extends TestTransportHandshakeMessage implements SynMessage {

  protected short flag = NetworkLayer.TYPE_TEST_MESSAGE;

  public boolean isSyn() {
    return true;
  }

  public boolean isSynAck() {
    return false;
  }

  public boolean isAck() {
    return false;
  }

  public short getStackLayerFlags() {
    return flag;
  }

  public int getCallbackPort() {
    return TransportHandshakeMessage.NO_CALLBACK_PORT;
  }
}
