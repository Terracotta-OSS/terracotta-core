/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.ServerMapLocalCache;

import java.util.Set;

public interface TCObjectSelfStore {
  void initializeTCObjectSelfStore(TCObjectSelfCallback callback);

  Object getById(ObjectID oid);

  void addTCObjectSelf(L1ServerMapLocalCacheStore store, AbstractLocalCacheStoreValue localStoreValue, Object tcoself);

  int size();

  void addAllObjectIDs(Set oids);

  boolean contains(ObjectID objectID);

  Object getByIdFromStore(ObjectID value, L1ServerMapLocalCacheStore store);

  void addTCObjectSelfTemp(TCObjectSelf obj);

  void removeTCObjectSelfTemp(TCObjectSelf value, boolean notifyServer);

  void removeTCObjectSelf(ServerMapLocalCache serverMapLocalCache, AbstractLocalCacheStoreValue localStoreValue);

  void removeStore(L1ServerMapLocalCacheStore store);
}
