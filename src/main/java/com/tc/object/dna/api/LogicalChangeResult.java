/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dna.api;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;

import java.io.IOException;

public class LogicalChangeResult implements TCSerializable {
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
  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
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
  public boolean equals(final Object o) {
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
