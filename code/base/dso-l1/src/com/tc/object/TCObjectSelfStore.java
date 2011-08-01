/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;

import java.util.Set;

public interface TCObjectSelfStore {
  void initializeTCObjectSelfStore(TCObjectSelfCallback callback);

  Object getById(ObjectID oid);

  void addTCObjectSelf(L1ServerMapLocalCacheStore store, AbstractLocalCacheStoreValue localStoreValue, Object tcoself);

  int size();

  void addAllObjectIDs(Set oids);

  boolean contains(ObjectID objectID);

  Object getByIdFromStore(ObjectID value, L1ServerMapLocalCacheStore store);

  void addTCObjectSelf(TCObjectSelf obj);

  void removeTCObjectSelf(TCObjectSelf value);
}
