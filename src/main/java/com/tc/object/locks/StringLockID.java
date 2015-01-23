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

  @Override
  public int compareTo(LockID o) {
    if (o instanceof StringLockID) {
      StringLockID other = (StringLockID) o;
      return id.compareTo(other.id);
    }
    
    return toString().compareTo(o.toString());
  }

  @Override
  public StringLockID deserializeFrom(final TCByteBufferInput serialInput) throws IOException {
    this.id = serialInput.readString();
    return this;
  }

  @Override
  public void serializeTo(final TCByteBufferOutput serialOutput) {
    serialOutput.writeString(this.id);
  }

  @Override
  public LockIDType getLockType() {
    return LockIDType.STRING;
  }
}