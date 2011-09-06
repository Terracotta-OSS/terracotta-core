/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.object.locks.LockID.LockIDType;

import java.io.IOException;

public class LockIDSerializer implements TCSerializable {
  private LockID lockID;

  public LockIDSerializer() {
    // 
  }

  public LockIDSerializer(LockID lockID) {
    this.lockID = lockID;
  }

  public LockID getLockID() {
    return lockID;
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    byte type = serialInput.readByte();
    this.lockID = getImpl(type);
    this.lockID.deserializeFrom(serialInput);
    return this;
  }

  private LockID getImpl(byte type) {
    try {
      switch (LockIDType.values()[type]) {
        case LONG:
          return new LongLockID();
        case STRING:
          return new StringLockID();
        case DSO:
          return new DsoLockID();
        case DSO_LITERAL:
          return new DsoLiteralLockID();
        case DSO_VOLATILE:
          return new DsoVolatileLockID();
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      // stupid javac can't cope with the assertion throw being here...
    }
    throw new AssertionError("Unknown type : " + type);
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeByte((byte) lockID.getLockType().ordinal());
    lockID.serializeTo(serialOutput);
  }
}
