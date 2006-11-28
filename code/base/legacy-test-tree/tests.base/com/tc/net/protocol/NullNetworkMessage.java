package com.tc.net.protocol;

import com.tc.bytes.TCByteBuffer;

/**
 * TODO: Document me
 * 
 * @author teck
 */
public class NullNetworkMessage implements TCNetworkMessage {

  public NullNetworkMessage() {
    super();
  }

  public TCNetworkHeader getHeader() {
    return new NullNetworkHeader();
  }

  public TCNetworkMessage getMessagePayload() {
    return null;
  }

  public TCByteBuffer[] getPayload() {
    return getEntireMessageData();
  }

  public TCByteBuffer[] getEntireMessageData() {
    return new TCByteBuffer[] {};
  }

  public boolean isSealed() {
    return true;
  }

  public void seal() {
    return;
  }

  public int getDataLength() {
    return 0;
  }

  public int getHeaderLength() {
    return 0;
  }

  public int getTotalLength() {
    return 0;
  }

  public void wasSent() {
    return;
  }

  public void setSentCallback(Runnable callback) {
    return;
  }

  public Runnable getSentCallback() {
    return null;
  }

  public void recycle() {
    return;
  }

}