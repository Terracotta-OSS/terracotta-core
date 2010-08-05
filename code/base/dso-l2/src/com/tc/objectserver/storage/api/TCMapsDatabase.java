/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;

public interface TCMapsDatabase {
  /**
   * Puts an <byte[], byte[]> key-value pair to the db. The id here is the object id of the map.
   */
  public Status put(long id, byte[] key, byte[] value, PersistenceTransaction tx);

  /**
   * Deletes a key from the map whose object id is passed in as the parmeter
   */
  public Status delete(long id, byte[] key, PersistenceTransaction tx);

  /**
   * Returns no of bytes written
   */
  public int deleteCollection(long id, PersistenceTransaction tx) throws TCDatabaseException;

  public TCMapsDatabaseCursor openCursor(PersistenceTransaction tx, long objectID);

  public TCMapsDatabaseCursor openCursorUpdatable(PersistenceTransaction tx, long objectID);

  /**
   * Used in tests
   */
  public long count();
}
