/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;

public interface TCStringToStringDatabase {
  /**
   * Gets the <String> value corresponding to the key
   */
  public Status get(TCDatabaseEntry<String, String> key, PersistenceTransaction tx);

  /**
   * Deletes the String key from the database.
   */
  public Status delete(String key, PersistenceTransaction tx);

  /**
   * Stored the <String, String> key value pair to the db
   */
  public Status put(String key, String value, PersistenceTransaction tx);

  /**
   * Insert a <String, String> key-value pair into the db.
   */
  public Status insert(String key, String value, PersistenceTransaction tx);

  /**
   * Update a <String, String> key-value pair in the db.
   */
  public Status update(String key, String value, PersistenceTransaction tx);
}
