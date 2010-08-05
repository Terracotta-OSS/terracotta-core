/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;

import java.util.Set;

public interface TCLongDatabase {
  /**
   * Puts a long to the database
   */
  public Status put(long key, PersistenceTransaction tx);

  /**
   * Gets all the long keys persisted to the database.
   * The transaction is already committed in this method.
   */
  public Set<Long> getAllKeys(PersistenceTransaction tx);

  /**
   * Checks if the long key is present.
   */
  public boolean contains(long key, PersistenceTransaction tx);

  /**
   * Deletes the long key from the db
   */
  public Status delete(long key, PersistenceTransaction tx);
}
