/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

import com.tc.object.ObjectID;
import com.tc.object.TCObjectSelf;

public class LocalCacheStoreEventualValue extends AbstractLocalCacheStoreValue {
  public LocalCacheStoreEventualValue() {
    //
  }

  public LocalCacheStoreEventualValue(ObjectID id, Object value) {
    super(id, value);
  }

  @Override
  public boolean isEventualConsistentValue() {
    return true;
  }

  @Override
  public ObjectID getValueObjectId() {
    return (ObjectID) id;
  }

  @Override
  public String toString() {
    return "LocalCacheStoreEventualValue [id=" + id + ", value="
           + (value instanceof TCObjectSelf ? ((TCObjectSelf) value).getObjectID() : value) + "]";
  }

}
