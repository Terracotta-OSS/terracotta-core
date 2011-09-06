/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;

import java.util.Map;

public interface TCIntToBytesDatabase {
  /**
   * Puts a <int, byte[]> key-value pair to the db
   */
  public Status put(int id, byte[] b, PersistenceTransaction tx);

  /**
   * Inserts a <int, byte[]> key-value pair into the db.
   */
  public Status insert(int id, byte[] value, PersistenceTransaction tx);

  /**
   * Updates a <int, byte[]> key-value pair in the db.
   */
  public Status update(int id, byte[] value, PersistenceTransaction tx);

  /**
   * Gets a byte[] from the key id
   */
  public byte[] get(int id, PersistenceTransaction tx);

  /**
   * Gets all <int, byte[]> key value pairs. The PersistentTransaction is committed within this method.
   */
  public Map<Integer, byte[]> getAll(PersistenceTransaction tx);
}
