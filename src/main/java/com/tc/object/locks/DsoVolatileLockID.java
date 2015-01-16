/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.ObjectID;

import java.io.IOException;

/**
 * LockID implementation representing a lock on a volatile Object field.
 * <p>
 * Locking on these instances allows us to provide volatile read/write semantics across the cluster.
 */
public class DsoVolatileLockID implements LockID {
  private static final long serialVersionUID = 0xc62da86bc278450eL;

  private long              objectId;
  private String            fieldName;

  public DsoVolatileLockID() {
    //
  }

  public DsoVolatileLockID(ObjectID oid, String fieldName) {
    this.objectId = oid.toLong();
    this.fieldName = fieldName;
  }

  @Deprecated
  public String asString() {
    return null;
  }

  @Override
  public LockIDType getLockType() {
    return LockIDType.DSO_VOLATILE;
  }

  @Override
  public DsoVolatileLockID deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    objectId = serialInput.readLong();
    fieldName = serialInput.readString();
    return this;
  }

  @Override
  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeLong(objectId);
    serialOutput.writeString(fieldName);
  }

  public ObjectID getObjectID() {
    return new ObjectID(objectId);
  }

  @Override
  public int hashCode() {
    return (5 * (((int) objectId) ^ ((int) (objectId >>> 32)))) ^ (7 * fieldName.hashCode());
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof DsoVolatileLockID) {
      return (objectId == ((DsoVolatileLockID) o).objectId) && fieldName.equals(((DsoVolatileLockID) o).fieldName);
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return "DsoVolatileLockID(" + new ObjectID(objectId) + "." + fieldName + ")";
  }

  @Override
  public int compareTo(LockID o) {
    if (o instanceof DsoVolatileLockID) {
      DsoVolatileLockID other = (DsoVolatileLockID) o;
      if ((objectId == other.objectId) && fieldName.equals(other.fieldName)) {
        return 0;
      } else {
        return (objectId + "." + fieldName).compareTo(other.objectId + "." + other.fieldName);
      }
    }

    if (o.getLockType() == LockIDType.DSO_LITERAL) { throw new ClassCastException("Can't compare LiteralLockID types."); }
    return toString().compareTo(o.toString());
  }
}
