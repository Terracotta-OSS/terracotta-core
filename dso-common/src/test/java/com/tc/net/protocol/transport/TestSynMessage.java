/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.protocol.NetworkLayer;

public class TestSynMessage extends TestTransportHandshakeMessage implements SynMessage {

  protected short flag = NetworkLayer.TYPE_TEST_MESSAGE;

  @Override
  public boolean isSyn() {
    return true;
  }

  @Override
  public boolean isSynAck() {
    return false;
  }

  @Override
  public boolean isAck() {
    return false;
  }

  @Override
  public short getStackLayerFlags() {
    return flag;
  }

  @Override
  public int getCallbackPort() {
    return TransportHandshakeMessage.NO_CALLBACK_PORT;
  }
}
