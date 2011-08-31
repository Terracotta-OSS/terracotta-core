/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.protocol.AbstractTCNetworkHeader;
import com.tc.net.protocol.TCProtocolException;
import com.tc.util.Assert;

/**
 * TODO: document me
 * 
 * @author teck
 */
public class TCMessageHeaderImpl extends AbstractTCNetworkHeader implements TCMessageHeader {
  // TODO: This class (and other network headers) should be auto-generated from some form of definition file

  protected TCMessageHeaderImpl(TCByteBuffer hdrData) {
    super(hdrData, MIN_LENGTH, MAX_LENGTH);
  }

  protected TCMessageHeaderImpl(TCMessageType type) {
    super(MIN_LENGTH, MAX_LENGTH);

    setMessageType(type.getType());
    setVersion(VERSION_1);
    setHeaderLength((short) (MIN_LENGTH / 4));
  }

  public int getHeaderByteLength() {
    return getHeaderLength() * 4;
  }

  public short getVersion() {
    return data.getUbyte(0);
  }

  public int getHeaderLength() {
    return data.getUbyte(1);
  }

  public int getMessageType() {
    return data.getUshort(2);
  }

  public int getMessageTypeVersion() {
    return data.getUshort(4);
  }

  void setVersion(short version) {
    data.putUbyte(0, version);
  }

  protected void setHeaderLength(short length) {
    Assert.eval(length <= MAX_LENGTH);
    data.putUbyte(1, length);
  }

  public void setMessageType(int type) {
    data.putUshort(2, type);
  }

  public void setMessageTypeVersion(int version) {
    data.putUshort(4, version);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();

    TCMessageType type = TCMessageType.getInstance(getMessageType());

    buf.append("msgType: ");
    if (type != null) {
      buf.append(type.toString());
    } else {
      buf.append("UNKNOWN").append('(').append(getMessageType()).append(')');
    }

    buf.append(", msgVer=").append(getMessageTypeVersion());
    buf.append('\n');

    return buf.toString();
  }

  public void validate() throws TCProtocolException {
    final short version = getVersion();
    final short expect = VERSION_1;
    if (version != expect) { throw new TCProtocolException("Version " + version + " does not match expected version "
                                                           + expect); }

    // XXX: validate other fields
  }

  protected boolean isHeaderLengthAvail() {
    return data.position() > 1;
  }

}