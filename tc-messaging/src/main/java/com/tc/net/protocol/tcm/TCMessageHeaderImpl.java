/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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

  @Override
  public int getHeaderByteLength() {
    return getHeaderLength() * 4;
  }

  @Override
  public short getVersion() {
    return data.getUbyte(0);
  }

  @Override
  public int getHeaderLength() {
    return data.getUbyte(1);
  }

  @Override
  public int getMessageType() {
    return data.getUshort(2);
  }

  @Override
  public int getMessageTypeVersion() {
    return data.getUshort(4);
  }

  void setVersion(short version) {
    data.putUbyte(0, version);
  }

  @Override
  protected void setHeaderLength(short length) {
    Assert.eval(length <= MAX_LENGTH);
    data.putUbyte(1, length);
  }

  @Override
  public void setMessageType(int type) {
    data.putUshort(2, type);
  }

  @Override
  public void setMessageTypeVersion(int version) {
    data.putUshort(4, version);
  }

  @Override
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

  @Override
  public void validate() throws TCProtocolException {
    final short version = getVersion();
    final short expect = VERSION_1;
    if (version != expect) { throw new TCProtocolException("Version " + version + " does not match expected version "
                                                           + expect); }

    // XXX: validate other fields
  }

  @Override
  protected boolean isHeaderLengthAvail() {
    return data.position() > 1;
  }

}
