/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;

/**
 * A database which can persist <bytes, bytes> key-value pair to the database.
 */
public interface TCBytesToBytesDatabase {
  /**
   * Puts a <bytes, bytes> key-value pair to the database.
   */
  public Status put(byte[] key, byte[] val, PersistenceTransaction tx);

  /**
   * Gets a <bytes, bytes> key-value pair to the database.
   */
  public byte[] get(byte[] key, PersistenceTransaction tx);

  /**
   * Inserts a <bytes, bytes> key-value pair to the database.
   */
  public Status insert(byte[] key, byte[] value, PersistenceTransaction tx);

  /**
   * Updates a <bytes, bytes> key-value pair in the database.
   */
  public Status update(byte[] key, byte[] value, PersistenceTransaction tx);

  /**
   * Deletes a <bytes, bytes> key-value pair from the database
   */
  public Status delete(byte[] key, PersistenceTransaction tx);

  /**
   * Opens a cursor which almost behaves the same way as a Java iterator
   */
  public TCDatabaseCursor<byte[], byte[]> openCursor(PersistenceTransaction tx);

  /**
   * Open a cursor which allows deletes.
   */
  public TCDatabaseCursor<byte[], byte[]> openCursorUpdatable(PersistenceTransaction tx);

  /**
   * Doesn't allow a put if already present.
   */
  public Status putNoOverwrite(PersistenceTransaction tx, byte[] key, byte[] value);
}
