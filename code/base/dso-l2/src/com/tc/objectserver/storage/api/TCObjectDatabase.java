/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;

public interface TCObjectDatabase {
  /**
   * Inserts a <long, byte[]> key-value pair to the db.
   */
  public Status insert(long id, byte[] b, PersistenceTransaction tx);

  /**
   * Updates a <long, byte[]> key-value pair to the db.
   */
  public Status update(long id, byte[] b, PersistenceTransaction tx);

  /**
   * Gets the value <byte[]> mapped to the key id
   */
  public byte[] get(long id, PersistenceTransaction tx);

  /**
   * Deletes a <long, byte[]> key-value pair to the db.
   */
  public Status delete(long id, PersistenceTransaction tx);

  public Status upsert(long long1, byte[] value, PersistenceTransaction tx);
}
