/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;

public interface TCTransactionStoreDatabase {

  /**
   * Inserts a <long, byte[]> key-value pair to the db.
   */
  public Status insert(long id, byte[] b, PersistenceTransaction tx);

  /**
   * Open a <long, byte[]> cursor for interating over the database
   */
  public TCDatabaseCursor<Long, byte[]> openCursor(PersistenceTransaction tx);

  /**
   * Deletes a <long, byte[]> key-value pair to the db.
   */
  public Status delete(long id, PersistenceTransaction tx);
}
