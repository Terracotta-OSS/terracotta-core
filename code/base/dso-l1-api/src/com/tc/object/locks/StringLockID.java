/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.util.Assert;

import java.io.IOException;

public class StringLockID implements LockID {
  private static final long serialVersionUID = 0x159578a476cef87dL;
  
  @Deprecated
  public final static StringLockID NULL_ID = new StringLockID("null id");

  private String                   id;

  public StringLockID() {
    // to make TCSerializable happy
  }

  /**
   * New id
   * 
   * @param id ID value
   */
  public StringLockID(String id) {
    Assert.eval(id != null);
    this.id = id;
  }
  
  /**
   * @return String value of id value
   */
  public String asString() {
    return id;
  }

  public String toString() {
    return getClass().getSimpleName() + "(" + id + ")";
  }

  public int hashCode() {
    return id.hashCode();
  }

  public boolean equals(Object obj) {
    if (obj instanceof StringLockID) {
      StringLockID lid = (StringLockID) obj;
      return this.id.equals(lid.id);
    }
    return false;
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    this.id = serialInput.readString();
    return this;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeString(id);
  }

  public LockIDType getLockType() {
    return LockIDType.STRING;
  }
}
