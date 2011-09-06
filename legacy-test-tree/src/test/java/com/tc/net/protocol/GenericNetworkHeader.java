/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol;

public class GenericNetworkHeader extends AbstractTCNetworkHeader {

  private static final int LENGTH = 12;

  public GenericNetworkHeader() {
    super(LENGTH, LENGTH);
  }

  public void setSequence(int sequence) {
    data.putInt(4, sequence);
  }

  public int getSequence() {
    return data.getInt(4);
  }

  public void setClientNum(int num) {
    data.putInt(8, num);
  }

  public int getClientNum() {
    return data.getInt(8);
  }

  public int getHeaderByteLength() {
    return LENGTH;
  }

  protected void setHeaderLength(short length) {
    if (length != LENGTH) { throw new IllegalArgumentException("Header length must be " + LENGTH); }

    return;
  }

  public int getMessageDataLength() {
    return data.getInt(0);
  }

  void setMessageDataLength(int length) {
    data.putInt(0, length);
  }

  public void validate() {
    return;
  }

}