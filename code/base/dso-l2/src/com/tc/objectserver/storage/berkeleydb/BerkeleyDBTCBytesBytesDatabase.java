/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.berkeleydb;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCBytesToBytesDatabase;
import com.tc.objectserver.storage.api.TCDatabaseCursor;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;

public class BerkeleyDBTCBytesBytesDatabase extends AbstractBerkeleyDatabase implements TCBytesToBytesDatabase {
  public BerkeleyDBTCBytesBytesDatabase(Database db) {
    super(db);
  }

  public Status delete(byte[] key, PersistenceTransaction tx) {
    DatabaseEntry entry = new DatabaseEntry();
    entry.setData(key);
    OperationStatus status = this.db.delete(pt2nt(tx), entry);
    if (status.equals(OperationStatus.SUCCESS)) {
      return TCDatabaseReturnConstants.Status.SUCCESS;
    } else if (status.equals(OperationStatus.NOTFOUND)) {
      return TCDatabaseReturnConstants.Status.NOT_FOUND;
    } else {
      return TCDatabaseReturnConstants.Status.NOT_SUCCESS;
    }
  }

  public byte[] get(byte[] key, PersistenceTransaction tx) {
    DatabaseEntry entry = new DatabaseEntry();
    entry.setData(key);
    DatabaseEntry value = new DatabaseEntry();
    OperationStatus status = db.get(pt2nt(tx), entry, value, LockMode.DEFAULT);
    if (OperationStatus.SUCCESS.equals(status)) { return value.getData(); }
    return null;
  }

  public Status put(byte[] key, byte[] val, PersistenceTransaction tx) {
    DatabaseEntry entryKey = new DatabaseEntry();
    entryKey.setData(key);
    DatabaseEntry entryValue = new DatabaseEntry();
    entryValue.setData(val);
    if (!OperationStatus.SUCCESS.equals(this.db.put(pt2nt(tx), entryKey, entryValue))) { return Status.NOT_SUCCESS; }
    return Status.SUCCESS;
  }

  public TCDatabaseCursor openCursor(PersistenceTransaction tx) {
    Cursor cursor = this.db.openCursor(pt2nt(tx), CursorConfig.READ_COMMITTED);
    return new BerkeleyDBTCDatabaseCursor(cursor);
  }

  public Status putNoOverwrite(PersistenceTransaction tx, byte[] key, byte[] value) {
    DatabaseEntry entryKey = new DatabaseEntry();
    entryKey.setData(key);
    DatabaseEntry entryValue = new DatabaseEntry();
    entryValue.setData(value);
    OperationStatus status = this.db.putNoOverwrite(pt2nt(tx), entryKey, entryValue);
    return status.equals(OperationStatus.SUCCESS) ? Status.SUCCESS : Status.NOT_SUCCESS;
  }

  public TCDatabaseCursor<byte[], byte[]> openCursorUpdatable(PersistenceTransaction tx) {
    return openCursor(tx);
  }
}
