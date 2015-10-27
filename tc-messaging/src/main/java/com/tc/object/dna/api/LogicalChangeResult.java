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
package com.tc.object.dna.api;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;

import java.io.IOException;

public class LogicalChangeResult implements TCSerializable<LogicalChangeResult> {
  private Object result;

  public static LogicalChangeResult SUCCESS = new LogicalChangeResult(true);
  public static LogicalChangeResult FAILURE = new LogicalChangeResult(false);

  public LogicalChangeResult() {
    // To make TCSerialization happy
  }

  public LogicalChangeResult(Object result) {
    this.result = result;
  }

  @Override
  public void serializeTo(TCByteBufferOutput serialOutput) {
    if (result instanceof Boolean) {
      serialOutput.writeByte(0);
      serialOutput.writeBoolean((Boolean) result);
    } else if (result instanceof byte[]) {
      serialOutput.writeByte(1);
      final byte[] bytes = (byte[]) result;
      serialOutput.writeInt(bytes.length);
      serialOutput.write(bytes);
    } else {
      // it's a null...
      serialOutput.writeByte(2);
    }
  }

  @Override
  public LogicalChangeResult deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    byte resultType = serialInput.readByte();
    if (resultType == 0) {
      result = serialInput.readBoolean();
    } else if (resultType == 1) {
      byte[] bytes = new byte[serialInput.readInt()];
      serialInput.read(bytes);
      result = bytes;
    }
    return this;
  }

  public boolean isSuccess() {
    return result instanceof Boolean && (Boolean) result;
  }

  public Object getResult() {
    return result;
  }

  @Override
  public String toString() {
    return "LogicalChangeResult[" + result + "]";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final LogicalChangeResult that = (LogicalChangeResult) o;

    if (!result.equals(that.result)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return result.hashCode();
  }
}
