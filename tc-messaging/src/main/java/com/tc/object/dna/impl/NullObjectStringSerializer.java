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
package com.tc.object.dna.impl;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCDataInput;
import com.tc.io.TCDataOutput;

import java.io.IOException;

public class NullObjectStringSerializer implements ObjectStringSerializer {

  @Override
  public ObjectStringSerializer deserializeFrom(TCByteBufferInput serialInput) {
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
