/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dna.api;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;

import java.io.IOException;

public class LogicalChangeResult implements TCSerializable {
  private boolean success;

  public static LogicalChangeResult SUCCESS = new LogicalChangeResult(true);
  public static LogicalChangeResult FAILURE = new LogicalChangeResult(false);

  public LogicalChangeResult() {
    // To make TCSerialization happy
  }

  public LogicalChangeResult(boolean success) {
    this.success = success;
  }

  @Override
  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeBoolean(success);
  }

  @Override
  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    this.success = serialInput.readBoolean();
    return this;
  }

  public boolean isSuccess() {
    return success;
  }

  @Override
  public String toString() {
    return "LogicalChangeResult[" + success + "]";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    LogicalChangeResult other = (LogicalChangeResult) obj;
    if (success != other.success) return false;
    return true;
  }

}
