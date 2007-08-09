/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.cache;

import com.tc.object.ObjectID;

import gnu.trove.TLinkable;

/**
 * @author steve Interface for objects that can be stored in the cache
 */
public interface Cacheable extends TLinkable {
  public ObjectID getObjectID();

  public void markAccessed();

  public void clearAccessed();

  public boolean recentlyAccessed();
  
  /*
   * @param factor : factor by which accessCount to be reduced. 
   * 
   * @return accessCount after divided by factor
   * 
   * throws ArithmeticException if factor=0
   */
  public int accessCount(int factor);
  
  // This method checks to see if the element is in a state where it can be evicted.
  public boolean canEvict();
  
}