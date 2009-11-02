/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;

import java.io.IOException;

public class LongLockID implements LockID {
  private static final long serialVersionUID = 0x2845dcae50983bcdL;

  private long id;

  public LongLockID() {
    // to make TCSerializable happy
    this(-1);
  }

  /**
   * New id
   * 
   * @param id ID value
   */
  public LongLockID(long id) {
    this.id = id;
  }

  public String toString() {
    return getClass().getSimpleName() + "(" + id + ")";
  }
  
  /**
   * @return String value of id value
   */
  public String asString() {
    return Long.toString(id);
  }

  public int hashCode() {
    return ((int) id) ^ ((int) (id >>> 32));
  }

  public boolean equals(Object obj) {
    if (obj instanceof LongLockID) {
      LongLockID lid = (LongLockID) obj;
      return this.id == lid.id;
    }
    return false;
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    this.id = serialInput.readLong();
    return this;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeLong(id);
  }

  public LockIDType getLockType() {
    return LockIDType.LONG;
  }
}
