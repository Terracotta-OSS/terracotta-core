/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

/**
 * A point to note is that hasNextKey must be called before nextKey is called.
 */
public interface TCMapsDatabaseCursor extends TCDatabaseCursor<byte[], byte[]> {
  /**
   * This method checks if the next key is available. This doesn't fetch the value from the db.
   */
  public boolean hasNextKey();

  /**
   * This method next key is available. This doesn't fetch the value from the db.
   */
  public TCDatabaseEntry<byte[], byte[]> nextKey();
}
