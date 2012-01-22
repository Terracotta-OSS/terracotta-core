/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.exception.ImplementMe;
import com.tc.net.protocol.NetworkLayer;

public class TestSynAckMessage extends TestTransportHandshakeMessage implements SynAckMessage {

  public String getErrorContext() {
    throw new ImplementMe();
  }

  public short getErrorType() {
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

  public short getStackLayerFlags() {
    return NetworkLayer.TYPE_TEST_MESSAGE;
  }

  public int getCallbackPort() {
    throw new ImplementMe();
  }

}
