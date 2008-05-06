/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.cache;

import java.util.Collection;

import com.tc.text.PrettyPrintable;


/**
 * @author steve
 */
public interface EvictionPolicy extends PrettyPrintable {
  /**
   * Add an object to the cache and return true if objects has to be be evicted from the cache. 
   * 
   * @param obj - object that should be cached.
   * @return true  if objects  should be removed from the cache
   */
  public boolean add(Cacheable obj);
  
  /**
   * returns a list of objects can be evicted from the cache. 
   * Note: It doesn't actually evict them, it is advisory at this point
   * 
   * @param maxCount - the maxCount of objects that should be returned.
   * @return Collection of objects that can be removed from the cache
   */
  public Collection getRemovalCandidates(int maxCount);

  /**
   * Retrieve a cached object from the cache.
   * 
   * @param ObjectID of the object to be retrieved
   * @return Cacheable - the requested object
   */
  public void remove(Cacheable obj);

  /**
   * moves it up the lru chain
   */
  public void markReferenced(Cacheable obj);
  
  /**
   * @return the cache Capacity when the cache becomes full
   */
  public int getCacheCapacity();

}