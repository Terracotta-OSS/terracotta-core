/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

import com.tc.object.ObjectID;
import com.tc.object.TCObjectSelfStore;

public class LocalCacheStoreEventualValue extends AbstractLocalCacheStoreValue {
  public LocalCacheStoreEventualValue() {
    //
  }

  public LocalCacheStoreEventualValue(ObjectID id, Object value, ObjectID mapID) {
    super(id, value, mapID);
  }

  @Override
  public boolean isEventualConsistentValue() {
    return true;
  }

  @Override
  public ObjectID getObjectId() {
    return (ObjectID) id;
  }

  public Object getValue() {
    return value;
  }

  @Override
  public Object getValueObject(TCObjectSelfStore tcObjectSelfStore, L1ServerMapLocalCacheStore store) {
    return value;
  }
}
