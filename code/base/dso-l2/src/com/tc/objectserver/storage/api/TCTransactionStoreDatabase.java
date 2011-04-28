/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;

public interface TCTransactionStoreDatabase {
  public Status insert(long id, byte[] value, PersistenceTransaction tx);

  public TCDatabaseCursor<Long, byte[]> openCursor(PersistenceTransaction tx);

  public Status delete(long id, PersistenceTransaction tx);
}
