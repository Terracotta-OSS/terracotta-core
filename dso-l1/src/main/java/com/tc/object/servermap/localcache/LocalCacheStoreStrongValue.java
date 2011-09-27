/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

import com.tc.object.ObjectID;
import com.tc.object.locks.LockID;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class LocalCacheStoreStrongValue extends AbstractLocalCacheStoreValue {
  private volatile ObjectID valueObjectID;

  public LocalCacheStoreStrongValue() {
    //
  }

  public LocalCacheStoreStrongValue(LockID id, Object value, ObjectID valueObjectID) {
    super(id, value);
    this.valueObjectID = valueObjectID;
  }

  @Override
  public boolean isStrongConsistentValue() {
    return true;
  }

  @Override
  public LockID getLockId() {
    return (LockID) id;
  }

  @Override
  public ObjectID getValueObjectId() {
    return valueObjectID;
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeLong(valueObjectID.toLong());
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    this.valueObjectID = new ObjectID(in.readLong());
  }
}
