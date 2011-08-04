/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

import com.tc.invalidation.Invalidations;
import com.tc.object.ObjectID;
import com.tc.object.locks.LockID;

import java.util.Set;

public interface ServerMapLocalCache {

  /**
   * Set up local cache store for use
   */
  void setupLocalStore(L1ServerMapLocalCacheStore serverMapLocalStore);

  L1ServerMapLocalCacheStore getInternalStore();

  /**
   * Removes all entries associated with this objectId
   */
  void removeEntriesForObjectId(ObjectID objectId);

  /**
   * Remove all the entries associated with this lockId
   */
  void removeEntriesForLockId(LockID lockId);

  /**
   * Removes a key from the Local cache
   */
  void removeFromLocalCache(Object key);

  /**
   * Called when the key has been evicted from the local store
   */
  void evictedFromStore(Object id, Object key, AbstractLocalCacheStoreValue value);

  /**
   * Unpin entry for this object key. Becomes eligible for eviction if not before
   */
  void unpinEntry(Object key);

  /**
   * Used in handshake to send a list of ObjectIDs to the server for validation
   */
  void addAllObjectIDsToValidate(Invalidations invalidations);

  // ///////////////////////////////
  // TCObjectServerMap methods
  // ///////////////////////////////

  /**
   * Enable/disable local caching
   */
  void setLocalCacheEnabled(boolean enable);

  // /**
  // * Cache strong consistent values
  // */
  // void addStrongValueToCache(LockID lockId, Object key, Object value, MapOperationType operationType);
  //
  // /**
  // * Cache eventual consistent values
  // */
  // void addEventualValueToCache(ObjectID valueObjectId, Object key, Object value, MapOperationType operationType);
  //
  // /**
  // * Cache incoherent/bulk-load values
  // */
  // void addIncoherentValueToCache(Object key, Object value, MapOperationType operationType);

  void addToCache(Object key, AbstractLocalCacheStoreValue value, MapOperationType operationType);

  /**
   * Get a coherent value from the local cache. If an incoherent value is present, then return null.
   */
  AbstractLocalCacheStoreValue getCoherentLocalValue(Object key);

  /**
   * Get the value corresponding to the key if present
   */
  AbstractLocalCacheStoreValue getLocalValue(Object key);

  /**
   * Returns the size of the local cache
   */
  int size();

  /**
   * Clear all elements from the local cache
   */
  void clear();

  void clearInline();

  /**
   * Attempt to remove 'count' entries from the local cache. May be called under memory pressure
   */
  int evictCachedEntries(int count);

  /**
   * Returns the keys present in the local cache (does not include meta items stored)
   */
  Set getKeySet();

  /**
   * This is called when an element has been expired.
   */
  void evictExpired(Object key, AbstractLocalCacheStoreValue value);
}
