/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
  
  public int accessCount();
  
  // This method checks to see if the element is in a state where it can be evicted.
  public boolean canEvict();
  
}