/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.bytes.TCByteBuffer;
import com.tc.exception.ImplementMe;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.TCNetworkHeader;
import com.tc.net.protocol.TCNetworkMessage;

public class TestWireProtocolMessage implements WireProtocolMessage {

  public TCConnection connection;
  public TCNetworkHeader header;
  
  @Override
  public short getMessageProtocol() {
    throw new ImplementMe();
  }

  @Override
  public WireProtocolHeader getWireProtocolHeader() {
    throw new ImplementMe();
  }

  @Override
  public TCNetworkHeader getHeader() {
    return header;
  }

  @Override
  public TCNetworkMessage getMessagePayload() {
    throw new ImplementMe();
  }

  @Override
  public TCByteBuffer[] getPayload() {
    throw new ImplementMe();
  }

  @Override
  public TCByteBuffer[] getEntireMessageData() {
    throw new ImplementMe();
  }

  @Override
  public boolean isSealed() {
    throw new ImplementMe();
  }

  @Override
  public void seal() {
    throw new ImplementMe();

  }

  @Override
  public int getDataLength() {
    throw new ImplementMe();
  }

  @Override
  public int getHeaderLength() {
    throw new ImplementMe();
  }

  @Override
  public int getTotalLength() {
    throw new ImplementMe();
  }

  @Override
  public void wasSent() {
    throw new ImplementMe();

  }

  @Override
  public void setSentCallback(Runnable callback) {
    throw new ImplementMe();

  }

  @Override
  public Runnable getSentCallback() {
    throw new ImplementMe();
  }

  @Override
  public TCConnection getSource() {
    return connection;
  }

  @Override
  public void recycle() {
    return;
  }

}
