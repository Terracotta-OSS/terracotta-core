/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.object;

import com.google.common.collect.SetMultimap;
import com.tc.abortable.AbortedOperationException;
import com.tc.object.bytecode.TCServerMap;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.PinnedEntryFaultCallback;
import com.tc.server.ServerEventType;

import java.util.Map;
import java.util.Set;

public interface TCObjectServerMap<L> extends TCObject {

  /**
   * Initializes the server map TCObject with TTI,TTL, max in memory capacity and max target count.
   * 
   * @param maxTTISeconds TTI
   * @param maxTTLSeconds TTL
   * @param targetMaxTotalCount targetMaxTotalCount
   * @param invalidateOnChange invalidateOnChange
   */
  void initialize(int maxTTISeconds, int maxTTLSeconds, int targetMaxTotalCount, boolean invalidateOnChange,
                  boolean localCacheEnabled);

  /**
   * Does a logic remove and removes from the local cache if present
   * 
   * @param map ServerTCMap
   * @param key Key Object
   * @param lockID LockID of lock protecting this key
   */
  void doLogicalRemove(final TCServerMap map, final L lockID, final Object key);

  void doLogicalRemoveVersioned(TCServerMap map, L lockID, Object key, long version);

  /**
   * Does a logic remove and mark as removed in the local cache if present. The cached item is not associated to a lock.
   * 
   * @param map ServerTCMap
   * @param key Key Object
   */
  void doLogicalRemoveUnlocked(final TCServerMap map, final Object key);

  void doLogicalRemoveUnlockedVersioned(TCServerMap map, Object key, long version);

  /**
   * Does a logic remove and mark as removed in the local cache if present only if there exist a mapping of key to
   * value. The cached item is not associated to a lock.
   * 
   * @param map ServerTCMap
   * @param key Key Object
   * @param value Object in the mapping
   * @return true if operation changed the clustered state
   * @throws AbortedOperationException
   */
  boolean doLogicalRemoveUnlocked(final TCServerMap map, final Object key, final Object value, MetaDataDescriptor mdd)
      throws AbortedOperationException;

  /**
   * Does a logic putIfAbsent. The cached item is not associated to a lock. The check about the presence of an existing
   * mapping is not done here and is expected to be done outside elsewhere.
   * 
   * @param map ServerTCMap
   * @param key Key Object
   * @param value Object in the mapping
   * @param mdd
   * @return true if operation changed the clustered state
   * @throws AbortedOperationException
   */
  boolean doLogicalPutIfAbsentUnlocked(final TCServerMap map, final Object key, final Object value,
                                      MetaDataDescriptor mdd)
      throws AbortedOperationException;

  /**
   * Does a logic replace. The cached item is not associated to a lock.
   * 
   * @param map ServerTCMap
   * @param key Key Object
   * @return true if operation changed the clustered state
   * @throws AbortedOperationException
   */
  boolean doLogicalReplaceUnlocked(final TCServerMap map, final Object key, final Object current,
                                   final Object newValue, MetaDataDescriptor mdd)
      throws AbortedOperationException;

  /**
   * Does a logic replace. The cached item is not associated to a lock.
   * 
   * @param map ServerTCMap
   * @param key Key Object
   * @return true if operation changed the clustered state
   * @throws AbortedOperationException
   */
  // boolean doLogicalReplaceUnlocked(final TCServerMap map, final Object key, final Object newValue)
  // throws AbortedOperationException;

  /**
   * Does a logical put and updates the local cache
   * 
   * @param key Key Object
   * @param value Object in the mapping
   */
  void doLogicalPut(final L lockID, final Object key, final Object value);

  /**
   * Does a logical put with version and updates the local cache
   * 
   * @param map ServerTCMap
   * @param lockID, lock under which this entry is added
   * @param key Key Object
   * @param value Object in the mapping
   * @param version
   */
  void doLogicalPutVersioned(TCServerMap map, L lockID, Object key, Object value, long version);

  /**
   * Does a logical put if absent or the version present is older
   * 
   * @param key Key Object
   * @param value Object in the mapping
   * @param version
   */
  void doLogicalPutIfAbsentVersioned(Object key, Object value, long version);

  /**
   * Clear this map
   * 
   * @param map ServerTCMap
   */
  void doClear(final TCServerMap map);

  /**
   * Does a logical put and updates the local cache without using a lock. The cached Item is not associated to a lock.
   * 
   * @param map ServerTCMap
   * @param key Key Object
   * @param value Object in the mapping
   */
  void doLogicalPutUnlocked(final TCServerMap map, final Object key, final Object value);

  /**
   * Does a logical put with version and updates the local cache without using a lock. The cached Item is not associated
   * to a lock.
   * 
   * @param map ServerTCMap
   * @param key Key Object
   * @param value Object in the mapping
   * @param version
   */
  void doLogicalPutUnlockedVersioned(TCServerMap map, Object key, Object value, long version);

  /**
   * Returns the value for a particular key in a TCServerMap. If already present in local cache, returns the value
   * otherwise fetches it from server and returns it, after caching it in local cache (if present). The cached item is
   * is not associated to a lock.
   * 
   * @param map ServerTCMap
   * @param key Key Object : Note currently only literal keys or shared keys are supported. Even if the key is portable,
   *        but not shared, it is not supported.
   * @return value Object in the mapping, null if no mapping present.
   * @throws AbortedOperationException
   */
  Object getValueUnlocked(final TCServerMap map, final Object key) throws AbortedOperationException;

  /**
   * Returns the VersionedObject for a particular key in a TCServerMap. It always fetches it from the server. LocalCache
   * is not read.
   * 
   * @param map ServerTCMap
   * @param key Key Object : Note currently only literal keys or shared keys are supported. Even if the key is portable,
   *        but not shared, it is not supported.
   * @return value VersionedObject in the mapping, null if no mapping present.
   * @throws AbortedOperationException
   */
  VersionedObject getVersionedValue(final TCServerMap map, final Object key) throws AbortedOperationException;

  Map<Object, Object> getAllValuesUnlocked(final SetMultimap<ObjectID, Object> mapIdToKeysMap)
      throws AbortedOperationException;

  Map<Object, VersionedObject> getAllVersioned(final SetMultimap<ObjectID, Object> mapIdToKeysMap) throws AbortedOperationException;

  /**
   * Returns a snapshot of keys for the giver TCServerMap
   * 
   * @param map TCServerMap
   * @return set Set return snapshot of keys
   */
  Set keySet(final TCServerMap map) throws AbortedOperationException;

  /**
   * Returns the value for a particular Key in a ServerTCMap.
   * 
   * @param map ServerTCMap
   * @param lockID, lock under which this key is looked up
   * @param key Key Object : Note currently only literal keys or shared keys are supported. Even if the key is portable,
   *        but not shared, it is not supported.
   * @return value Object in the mapping, null if no mapping present.
   * @throws AbortedOperationException
   */
  Object getValue(final TCServerMap map, final L lockID, final Object key) throws AbortedOperationException;

  /**
   * Returns total size of an array of ServerTCMap.
   * <p>
   * The list of TCServerMaps passed in need not contain this TCServerMap, this is only a pass thru method that calls
   * getAllSize on the RemoteServerMapManager and is provided as a convenient way of batching the size calls at the
   * higher level
   * 
   * @param maps ServerTCMap[]
   * @return long for size of map.
   */
  long getAllSize(final TCServerMap[] maps) throws AbortedOperationException;

  /**
   * Returns the size of the local cache
   * 
   * @return int for size for the local cache of map.
   */
  int getLocalSize();

  /**
   * Get the local cache's on heap size in bytes
   */
  long getLocalOnHeapSizeInBytes();

  /**
   * Get the local cache's off heap size in bytes
   */
  long getLocalOffHeapSizeInBytes();

  /**
   * Get the number of items on the local heap
   */
  int getLocalOnHeapSize();

  /**
   * Get the number of items in local offheap
   */
  int getLocalOffHeapSize();

  /**
   * Clears local cache of all entries. It is not immediate as all associated locks needs to be recalled.
   * 
   * @param map ServerTCMap
   */
  void clearLocalCache(final TCServerMap map);

  void cleanLocalState();

  void clearAllLocalCacheInline();

  /**
   * Clears local cache for the corresponding key
   */
  void evictedInServer(Object key);

  /**
   * Get set of keys present in the local cache.
   */
  Set getLocalKeySet();

  /**
   * Is key local.
   */
  boolean containsLocalKey(Object key);

  /**
   * Check if the requested key is on the local heap
   */
  boolean containsKeyLocalOnHeap(Object key);

  /**
   * Check if the requested key is on the local offheap
   */
  boolean containsKeyLocalOffHeap(Object key);

  /**
   * Get from local cache.
   */
  Object getValueFromLocalCache(Object key);

  /**
   * Remove the given key from local cache.
   * 
   * @param key key to remove.
   */
  void removeValueFromLocalCache(Object key);

  /**
   * Add meta data to this server map
   */
  void addMetaData(MetaDataDescriptor mdd);

  /**
   * Setup the local store for use. This method is called whenever the map is created or faulted in the L1 first time.
   * 
   * @param callback
   */
  void setupLocalStore(L1ServerMapLocalCacheStore serverMapLocalStore, PinnedEntryFaultCallback callback);

  /**
   * Destroy the local store.
   */
  void destroyLocalStore();

  void setLocalCacheEnabled(boolean enabled);

  void recalculateLocalCacheSize(Object key);

  void doLogicalSetLastAccessedTime(Object key, Object value, long lastAccessedTime);

  void doLogicalExpire(L lockID, Object key, Object value);

  boolean doLogicalExpireUnlocked(TCServerMap map, Object key, Object value);

  /**
   * Adds the Keys for which pending additions are in progress to addSet and the Keys for which pending removes are in
   * progress to removeSet.
   */
  void addTxnInProgressKeys(Set addSet, Set removeSet);

  void doClearVersioned();

  void doRegisterListener(Set<ServerEventType> eventTypes, boolean skipRejoinChecks);

  void doUnregisterListener(Set<ServerEventType> eventTypes);

}
