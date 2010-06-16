/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.object.bytecode.TCServerMap;

public interface TCObjectServerMap extends TCObject {

  /**
   * Does a logic remove and removes from the local cache if present
   * 
   * @param map ServerTCMap
   * @param key Key Object
   * @param value Object in the mapping
   */
  public void doLogicalRemove(final TCServerMap map, final Object key);

  /**
   * Does a logical put and updates the local cache
   * 
   * @param map ServerTCMap
   * @param lockID, lock under which this entry is added
   * @param key Key Object
   * @param value Object in the mapping
   */
  public void doLogicalPut(final TCServerMap map, final String lockID, final Object key, final Object value);

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
   * Returns the value for a particular Key in a ServerTCMap.
   * 
   * @param map ServerTCMap
   * @param lockID, lock under which this key is looked up
   * @param key Key Object : Note currently only literal keys or shared keys are supported. Even if the key is portable,
   *        but not shared, it is not supported.
   * @return value Object in the mapping, null if no mapping present.
   */
  public Object getValue(final TCServerMap map, final String lockID, final Object key);

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
}