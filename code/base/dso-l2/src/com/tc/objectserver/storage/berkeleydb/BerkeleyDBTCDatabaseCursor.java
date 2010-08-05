/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.berkeleydb;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.storage.api.TCDatabaseCursor;
import com.tc.objectserver.storage.api.TCDatabaseEntry;

public class BerkeleyDBTCDatabaseCursor implements TCDatabaseCursor<byte[], byte[]> {
  private final Cursor                      cursor;
  protected TCDatabaseEntry<byte[], byte[]> entry = null;

  public BerkeleyDBTCDatabaseCursor(Cursor cursor) {
    this.cursor = cursor;
  }

  public void close() {
    cursor.close();
  }

  public void delete() {
    cursor.delete();
  }

  public boolean hasNext() {
    return hasNext(true);
  }

  protected boolean hasNext(boolean fetchValue) {
    if (entry != null) { return true; }

    entry = new TCDatabaseEntry<byte[], byte[]>();
    DatabaseEntry entryKey = new DatabaseEntry();
    DatabaseEntry entryValue = new DatabaseEntry();

    if (!fetchValue) {
      entryValue.setPartial(0, 0, true);
    }

    OperationStatus status = cursor.getNext(entryKey, entryValue, LockMode.DEFAULT);
    entry.setKey(entryKey.getData()).setValue(entryValue.getData());
    return status.equals(OperationStatus.SUCCESS);
  }

  public TCDatabaseEntry<byte[], byte[]> next() {
    if (entry == null) { throw new DBException("next call should be called only after checking hasNext."); }

    TCDatabaseEntry<byte[], byte[]> tempEntry = entry;
    entry = null;
    return tempEntry;
  }

  public Cursor getCursor() {
    return cursor;
  }
}
