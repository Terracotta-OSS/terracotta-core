/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

import com.tc.object.ObjectID;
import com.tc.object.locks.LockID;

public class LocalCacheStoreStrongValue extends AbstractLocalCacheStoreValue {
  public LocalCacheStoreStrongValue() {
    //
  }

  public LocalCacheStoreStrongValue(LockID id, Object value, ObjectID mapID) {
    super(id, value, mapID);
  }

  @Override
  public boolean isStrongConsistentValue() {
    return true;
  }

  @Override
  public LockID getLockId() {
    return (LockID) id;
  }
}
