/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.cache;

import com.tc.object.ObjectID;

import gnu.trove.TLinkable;

/**
 * Interface for objects that can be stored in the cache
 */
public interface Cacheable extends TLinkable {

  /**
   * Get object identifier
   * 
   * @return Identifier
   */
  public ObjectID getObjectID();

  /**
   * Mark object as accessed
   */
  public void markAccessed();

  /**
   * Clear access count
   */
  public void clearAccessed();

  /**
   * Determine whether object was accessed since a clear occurred
   * 
   * @return True if accessed since clear
   */
  public boolean recentlyAccessed();

  /**
   * Reduce access count by factor
   * 
   * @param factor Factor by which accessCount to be reduced.
   * @return New accessCount after being divided by factor
   * @throws ArithmeticException if factor=0
   */
  public int accessCount(int factor);

  /**
   * This method checks to see if the element is in a state where it can be evicted.
   * 
   * @return true if can evict now
   */
  public boolean canEvict();

  /**
   * This method checks to see if the element is to be managed by the cache or not
   */
  public boolean isCacheManaged();

}
