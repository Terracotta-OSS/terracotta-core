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
package com.tc.net.protocol;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.util.Assert;

import java.util.zip.Adler32;

/**
 * TODO: document me
 * 
 * @author teck
 */
public abstract class AbstractTCNetworkHeader implements TCNetworkHeader {
  protected static final int  LENGTH_NOT_AVAIL = -1;

  private static final byte[] EMTPY_BYTE_ARRAY = new byte[] {};
  private static final byte[] FOUR_ZERO_BYTES  = new byte[] { (byte) 0, (byte) 0, (byte) 0, (byte) 0 };

  protected final int         minLength;
  protected final int         maxLength;
  protected TCByteBuffer      data;

  abstract protected void setHeaderLength(short headerLength);

  /**
   * Override this method to allow protocol parsers to wait before asking <code>getHeaderByteLength()</code>. This is
   * really only useful if your header length data is not the first byte of your header
   * 
   * @return true if the length of this header is available
   */
  protected boolean isHeaderLengthAvail() {
    return true;
  }

  protected AbstractTCNetworkHeader(TCByteBuffer buffer, int min, int max) {
    this.minLength = min;
    this.maxLength = max;

    if (buffer == null) {
      this.data = TCByteBufferFactory.getInstance(max);
      this.data.limit(min);
    } else {
      this.data = buffer;
    }

//    Assert.eval(!this.data.isDirect());
    Assert.eval(this.data.capacity() >= this.maxLength);
    if (this.data.limit() % 4 != 0) { throw new AssertionError("buffer limit not a multiple of 4: " + this.data.limit()); }
  }

  protected AbstractTCNetworkHeader(int min, int max) {
    this(null, min, max);
  }

  @Override
  public TCByteBuffer getDataBuffer() {
    return data;
  }

  @Override
  abstract public void validate() throws TCProtocolException;

  private void setBytes(int pos, byte[] value) {
    setBytes(pos, value, 0, value.length);
  }

  private void setBytes(int pos, byte[] value, int offset, int length) {
    data.put(pos, value, offset, length);
  }

  protected byte getByte(int index) {
    return data.get(index);
  }

  protected byte[] getBytes(int offset, int len) {
    Assert.eval(len >= 0);

    if (0 == len) { return EMTPY_BYTE_ARRAY; }

    byte rv[] = new byte[len];
    data.get(offset, rv, 0, len);

    return rv;
  }

  protected void set4BitValue(int pos, boolean high, byte value) {
    byte other4 = (byte) (data.get(pos) & (high ? 0x0F : 0xF0));
    byte val = (byte) ((value << (high ? 4 : 0)) & (high ? 0xF0 : 0x0F));

    data.put(pos, (byte) (val | other4));
  }

  protected byte get4BitValue(int pos, boolean high) {
    byte bite = getByte(pos);

    if (high) {
      return (byte) ((bite >> 4) & 0x0F);
    } else {
      return (byte) (bite & 0x0F);
    }
  }

  protected long computeAdler32Checksum(int pos, boolean set) {
    Adler32 adler = new Adler32();

    // save off the existing checksum
    byte cksum[] = getBytes(pos, 4);

    // zero out the checksum bytes before doing the calculation
    setBytes(pos, FOUR_ZERO_BYTES);
    adler.update(data.array(), 0, getHeaderByteLength());

    long rv = adler.getValue();

    if (set) {
      data.putUint(pos, rv);
    } else {
      // restore the original checksum bytes
      setBytes(pos, cksum);
    }

    return rv;
  }

  protected void setLimit(int newLimit) {
    data.limit(newLimit);
  }

  /**
   * @param options zero-padded header option bytes. The byte array can be zero length and/or null to indicate that no
   *        options should be set in this header
   */
  public void setOptions(byte[] options) {
    if (options == null) {
      options = new byte[] {};
    }

    int optionsLen = options.length;

    Assert.eval((optionsLen % 4) == 0);
    Assert.eval(optionsLen <= (maxLength - minLength));

    if (optionsLen > 0) {
      setLimit(minLength + optionsLen);
      setBytes(minLength, options);
    } else {
      setLimit(minLength);
    }

    setHeaderLength((byte) ((minLength + optionsLen) / 4));
  }

  public byte[] getOptions() {
    return getBytes(minLength, getHeaderByteLength() - minLength);
  }

}
