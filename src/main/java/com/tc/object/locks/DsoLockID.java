/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.ObjectID;

import java.io.IOException;

/**
 * LockID implementation representing a lock on a non-literal DSO clustered object.
 */
public class DsoLockID implements LockID {
  private static final long serialVersionUID = 0x123456789abcdefL;

  private long              objectId;

  public DsoLockID() {
    // for tc serialization
  }

  public DsoLockID(ObjectID objectId) {
    this.objectId = objectId.toLong();
  }

  public String asString() {
    return null;
  }

  @Override
  public LockIDType getLockType() {
    return LockIDType.DSO;
  }

  @Override
  public DsoLockID deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    objectId = serialInput.readLong();
    return this;
  }

  @Override
  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeLong(objectId);
  }

  @Override
  public int hashCode() {
    return ((int) objectId) ^ ((int) (objectId >>> 32));
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof DsoLockID) {
      return objectId == ((DsoLockID) o).objectId;
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return "DsoLockID(" + new ObjectID(objectId) + ")";
  }

  public ObjectID getObjectID() {
    return new ObjectID(objectId);
  }

  @Override
  public int compareTo(LockID other) {
    if (other instanceof DsoLockID) {
      DsoLockID dsoLockID = (DsoLockID) other;
      if (this.objectId < dsoLockID.objectId) {
        return -1;
      } else if (this.objectId > dsoLockID.objectId) {
        return 1;
      } else {
        return 0;
      }
    }

    if (other.getLockType() == LockIDType.DSO_LITERAL) { throw new ClassCastException("Can't compare LiteralLockID types."); }
    return toString().compareTo(other.toString());
  }
}
