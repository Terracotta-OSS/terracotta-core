/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.bytes.TCByteBuffer;

/**
 * @author teck
 */
public class NullNetworkMessage implements TCNetworkMessage {

  public NullNetworkMessage() {
    super();
  }

  @Override
  public TCNetworkHeader getHeader() {
    return new NullNetworkHeader();
  }

  @Override
  public TCNetworkMessage getMessagePayload() {
    return null;
  }

  @Override
  public TCByteBuffer[] getPayload() {
    return getEntireMessageData();
  }

  @Override
  public TCByteBuffer[] getEntireMessageData() {
    return new TCByteBuffer[] {};
  }

  @Override
  public boolean isSealed() {
    return true;
  }

  @Override
  public void seal() {
    return;
  }

  @Override
  public int getDataLength() {
    return 0;
  }

  @Override
  public int getHeaderLength() {
    return 0;
  }

  @Override
  public int getTotalLength() {
    return 0;
  }

  @Override
  public void wasSent() {
    return;
  }

  @Override
  public void setSentCallback(Runnable callback) {
    return;
  }

  @Override
  public Runnable getSentCallback() {
    return null;
  }

  @Override
  public void recycle() {
    return;
  }

}