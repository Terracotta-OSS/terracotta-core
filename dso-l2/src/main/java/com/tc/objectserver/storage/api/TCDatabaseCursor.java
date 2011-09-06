/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

/**
 * A point to note is that hasNext must be called before next is called.
 */
public interface TCDatabaseCursor<K, V> {
  
  /**
   * Returns true if a nextElement is presenrt.
   */
  public boolean hasNext();
  
  /**
   * Returns the next element
   */
  public TCDatabaseEntry<K, V> next();

  /**
   * Deletes the element at the cursor
   */
  public void delete();

  /**
   * Closes the cursor.
   */
  public void close();
}
