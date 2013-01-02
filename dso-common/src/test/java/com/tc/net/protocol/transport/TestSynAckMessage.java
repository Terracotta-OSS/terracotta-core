/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.exception.ImplementMe;
import com.tc.net.protocol.NetworkLayer;

public class TestSynAckMessage extends TestTransportHandshakeMessage implements SynAckMessage {

  @Override
  public String getErrorContext() {
    throw new ImplementMe();
  }

  @Override
  public short getErrorType() {
    throw new ImplementMe();
  }

  @Override
  public boolean hasErrorContext() {
    throw new ImplementMe();
  }

  @Override
  public boolean isSyn() {
    return false;
  }

  @Override
  public boolean isSynAck() {
    return true;
  }

  @Override
  public boolean isAck() {
    return false;
  }

  @Override
  public void recycle() {
    return;
  }

  @Override
  public short getStackLayerFlags() {
    return NetworkLayer.TYPE_TEST_MESSAGE;
  }

  @Override
  public int getCallbackPort() {
    throw new ImplementMe();
  }

}
