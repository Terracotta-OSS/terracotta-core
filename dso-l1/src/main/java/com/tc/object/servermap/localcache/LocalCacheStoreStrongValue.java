/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

import com.tc.object.ObjectID;
import com.tc.object.TCObjectSelf;
import com.tc.object.locks.LockID;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class LocalCacheStoreStrongValue extends AbstractLocalCacheStoreValue {
  private volatile ObjectID valueObjectID;
  private volatile long     lockAwardID;

  public long getLockAwardID() {
    return lockAwardID;
  }

  public LocalCacheStoreStrongValue() {
    //
  }

  public LocalCacheStoreStrongValue(LockID id, Object value, ObjectID valueObjectID, long lockAwardIDParam) {
    super(id, value);
    this.valueObjectID = valueObjectID;
    this.lockAwardID = lockAwardIDParam;
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
    out.writeLong(lockAwardID);
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    this.valueObjectID = new ObjectID(in.readLong());
    this.lockAwardID = in.readLong();
  }

  @Override
  public String toString() {
    return "LocalCacheStoreStrongValue [id=" + id + ", value="
           + (value instanceof TCObjectSelf ? ((TCObjectSelf) value).getObjectID() : value) + " lockAwardID="
           + lockAwardID + "]";
  }
}
