/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.util.Assert;

import java.io.IOException;

public class StringLockID implements LockID {
  private static final long        serialVersionUID = 0x159578a476cef87dL;

  @Deprecated
  public final static StringLockID NULL_ID          = new StringLockID("null id");

  private String                   id;

  public StringLockID() {
    // to make TCSerializable happy
  }

  /**
   * New id
   * 
   * @param id ID value
   */
  public StringLockID(final String id) {
    Assert.eval(id != null);
    this.id = id;
  }

  /**
   * @return String value of id value
   */
  public String asString() {
    return this.id;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + this.id + ")";
  }

  @Override
  public int hashCode() {
    return this.id.hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof StringLockID) {
      final StringLockID lid = (StringLockID) obj;
      return this.id.equals(lid.id);
    }
    return false;
  }
  
  public int compareTo(Object o) {
    if (o instanceof StringLockID) {
      StringLockID other = (StringLockID)o;
      return id.compareTo(other.id);
    } else if (o instanceof LockID) {
      if (((LockID)o).getLockType() == LockIDType.DSO_LITERAL) {
        throw new ClassCastException("Can't compare LiteralLockID types.");
      }
      return toString().compareTo(o.toString());
    }
    
    throw new ClassCastException(o + " is not an instance of LockID");
  }

  public Object deserializeFrom(final TCByteBufferInput serialInput) throws IOException {
    this.id = serialInput.readString();
    return this;
  }

  public void serializeTo(final TCByteBufferOutput serialOutput) {
    serialOutput.writeString(this.id);
  }

  public LockIDType getLockType() {
    return LockIDType.STRING;
  }
}
