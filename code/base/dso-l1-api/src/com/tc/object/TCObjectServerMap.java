/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.object.bytecode.TCServerMap;

import java.util.Set;

public interface TCObjectServerMap<L> extends TCObject {

  /**
   * Initializes the server map TCObject with TTI,TTL, max in memory capacity and max target count.
   * 
   * @param maxTTISeconds TTI
   * @param maxTTLSeconds TTL
   * @param targetMaxInMemoryCount targetMaxInMemoryCount
   * @param targetMaxTotalCount targetMaxTotalCount
   */
  public void initialize(int maxTTISeconds, int maxTTLSeconds, int targetMaxInMemoryCount, int targetMaxTotalCount);

  /**
   * Does a logic remove and removes from the local cache if present
   * 
   * @param map ServerTCMap
   * @param key Key Object
   * @param value Object in the mapping
   */
  public void doLogicalRemove(final TCServerMap map, final Object key);
  
  /**
   * Does a logic remove and mark as removed in the local cache if present. The cached item is incoherent
   * 
   * @param map ServerTCMap
   * @param key Key Object
   * @param value Object in the mapping
   */
  public void doLogicalRemoveIncoherent(final TCServerMap map, final Object key);

  /**
   * Does a logical put and updates the local cache
   * 
   * @param map ServerTCMap
   * @param lockID, lock under which this entry is added
   * @param key Key Object
   * @param value Object in the mapping
   */
  public void doLogicalPut(final TCServerMap map, final L lockID, final Object key, final Object value);

  /**
   * Does a logical put but doesn't add it to the local cache, old cache entries could be cleared
   * 
   * @param map ServerTCMap
   * @param lockID, lock under which this entry is added
   * @param key Key Object
   * @param value Object in the mapping
   */
  public void doLogicalPutButDontCache(final TCServerMap map, final Object key, final Object value);
  
  /**
   * Does a logical put and updates the local cache. The cached item is incoherent
   * 
   * @param map ServerTCMap
   * @param lockID, lock under which this entry is added
   * @param key Key Object
   * @param value Object in the mapping
   */
  public void doLogicalPutIncoherent(final TCServerMap map, final Object key, final Object value);

  /**
   * Returns the value for a particular Key in a ServerTCMap, gets it from the server and doesn't cache the value
   * locally
   * 
   * @param map ServerTCMap
   * @param key Key Object : Note currently only literal keys or shared keys are supported. Even if the key is portable,
   *        but not shared, it is not supported.
   * @return value Object in the mapping, null if no mapping present.
   */
  public Object getValueButDontCache(final TCServerMap map, final Object key);
  
  /**
   * Returns the value for a particular key in a TCServerMap. If already present in local cache, returns the value
   * otherwise fetches it from server and returns it, after caching it in local cache (if present). The cached item is incoherent.
   * 
   * @param map ServerTCMap
   * @param key Key Object : Note currently only literal keys or shared keys are supported. Even if the key is portable,
   *        but not shared, it is not supported.
   * @return value Object in the mapping, null if no mapping present.
   */
  public Object getValueIncoherent(final TCServerMap map, final Object key);

  
  /**
   * Returns a snapshot of keys for the giver ServerTCMap
   * 
   * @param map ServerTCMap
   * @return set Set return snapshot of keys
   */
  public Set keySet(final TCServerMap map);
  
  
  /**
   * Returns the value for a particular Key in a ServerTCMap.
   * 
   * @param map ServerTCMap
   * @param lockID, lock under which this key is looked up
   * @param key Key Object : Note currently only literal keys or shared keys are supported. Even if the key is portable,
   *        but not shared, it is not supported.
   * @return value Object in the mapping, null if no mapping present.
   */
  public Object getValue(final TCServerMap map, final L lockID, final Object key);

  /**
   * Returns the size of a ServerTCMap
   * 
   * @param map ServerTCMap
   * @return int for size of map.
   */
  public int getSize(final TCServerMap map);

  /**
   * Returns the size of the local cache
   * 
   * @return int for size for the local cache of map.
   */
  public int getLocalSize();

  /**
   * Clears local cache of all entries. It is not immediate as all associated locks needs to be recalled.
   * 
   * @param map ServerTCMap
   */
  public void clearLocalCache(final TCServerMap map);

  /**
   * Runs Target capacity eviction to evict Cached Entries from local cache
   */
  public void doCapacityEviction();
  
  /**
   * Clears local cache for the corresponding key
   */
  public void removeFromLocalCache(Object key);
  
  /**
   * Get set of keys present in the local cache.
   */
  public Set getLocalKeySet();
  
  /**
   * Get from local cache.
   */
  public Object getValueFromLocalCache(Object key);
}