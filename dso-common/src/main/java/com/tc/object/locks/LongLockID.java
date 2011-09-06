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
  
  public int compareTo(Object o) {
    if (o instanceof LongLockID) {
      LongLockID other = (LongLockID)o;
      if (this.id < other.id) {
        return -1;
      } else if (this.id > other.id) {
        return 1;
      } else {
        return 0;
      }
    } else if (o instanceof LockID) {
      if (((LockID)o).getLockType() == LockIDType.DSO_LITERAL) {
        throw new ClassCastException("Can't compare LiteralLockID types.");
      }
      return toString().compareTo(o.toString());
    }
    
    throw new ClassCastException(o + " is not an instance of LockID");
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
