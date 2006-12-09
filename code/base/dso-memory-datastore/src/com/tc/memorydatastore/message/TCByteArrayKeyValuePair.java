/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.memorydatastore.message;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;

import java.io.IOException;

public class TCByteArrayKeyValuePair implements TCSerializable {
  private byte[] key;
  private byte[] value;
  
  public TCByteArrayKeyValuePair() {
    super();
  }

  public TCByteArrayKeyValuePair(byte[] key, byte[] value) {
    this.key = key;
    this.value = value;
  }

  public byte[] getKey() {
    return key;
  }

  public byte[] getValue() {
    return value;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeInt(key.length);
    serialOutput.write(key);
    serialOutput.writeInt(value.length);
    serialOutput.write(value);
  }

  public Object deserializeFrom(TCByteBufferInputStream serialInput) throws IOException {
    int length = serialInput.readInt();
    this.key = new byte[length];
    serialInput.read(this.key);

    length = serialInput.readInt();
    this.value = new byte[length];
    serialInput.read(this.value);
    
    return this;
  }
}