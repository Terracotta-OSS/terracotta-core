/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCDataInput;
import com.tc.io.TCDataOutput;

import java.io.IOException;

public class NullObjectStringSerializer implements ObjectStringSerializer {

  @Override
  public Object deserializeFrom(TCByteBufferInput serialInput) {
    return this;
  }

  @Override
  public void serializeTo(TCByteBufferOutput serialOutput) {
    //
  }

  @Override
  public String readFieldName(TCDataInput in) throws IOException {
    return in.readString();
  }

  @Override
  public String readString(TCDataInput in) throws IOException {
    return in.readString();
  }

  @Override
  public void writeFieldName(TCDataOutput out, String fieldName) {
    out.writeString(fieldName);
  }

  @Override
  public void writeString(TCDataOutput out, String string) {
    out.writeString(string);
  }

  @Override
  public void writeStringBytes(TCDataOutput out, byte[] bytes) {
    out.writeInt(bytes.length);
    out.write(bytes);
  }

  @Override
  public byte[] readStringBytes(TCDataInput input) throws IOException {
    int len = input.readInt();
    byte[] bytes = new byte[len];
    input.readFully(bytes);
    return bytes;
  }

  @Override
  public int getApproximateBytesWritten() {
    return 0;
  }
}
