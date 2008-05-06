/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcclient.cache;

/**
 * Callback interface for when a data item is expired and removed from the store.
 */
public interface Expirable {
  
  /**
   * The item handled by the key has been expired and removed
   * @param key The item key
   */
  public void expire(Object key);
}
