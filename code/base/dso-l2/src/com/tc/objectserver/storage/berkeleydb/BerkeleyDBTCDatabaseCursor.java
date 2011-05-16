/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.berkeleydb;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.objectserver.storage.api.TCDatabaseCursor;
import com.tc.objectserver.storage.api.TCDatabaseEntry;

import java.util.NoSuchElementException;

public class BerkeleyDBTCDatabaseCursor implements TCDatabaseCursor<byte[], byte[]> {
  private final Cursor                      cursor;
  protected final boolean                   fetchValue;
  protected TCDatabaseEntry<byte[], byte[]> entry = null;

  public BerkeleyDBTCDatabaseCursor(Cursor cursor) {
    this(cursor, true);
  }

  public BerkeleyDBTCDatabaseCursor(Cursor cursor, boolean fetchValue) {
    this.cursor = cursor;
    this.fetchValue = fetchValue;
  }

  public void close() {
    cursor.close();
  }

  public void delete() {
    cursor.delete();
  }

  public boolean hasNext() {
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
    if (entry == null) {
      if (!hasNext()) { throw new NoSuchElementException("No Element left. Please do hasNext before calling next"); }
    }

    TCDatabaseEntry<byte[], byte[]> tempEntry = entry;
    entry = null;
    return tempEntry;
  }

  public Cursor getCursor() {
    return cursor;
  }
}
