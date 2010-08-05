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
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCDatabaseEntry;
import com.tc.objectserver.storage.api.TCMapsDatabase;
import com.tc.objectserver.storage.api.TCMapsDatabaseCursor;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.util.Conversion;

public class BerkeleyDBTCMapsDatabase extends BerkeleyDBTCBytesBytesDatabase implements TCMapsDatabase {

  public BerkeleyDBTCMapsDatabase(Database db) {
    super(db);
  }

  public TCMapsDatabaseCursor openCursor(PersistenceTransaction tx, long objectID) {
    Cursor cursor = this.db.openCursor(pt2nt(tx), CursorConfig.READ_UNCOMMITTED);
    return new BerkeleyMapsTCDatabaseCursor(cursor, objectID);
  }
  
  public TCMapsDatabaseCursor openCursorUpdatable(PersistenceTransaction tx, long objectID) {
    return openCursor(tx, objectID);
  }

  public Status delete(long id, byte[] key, PersistenceTransaction tx) {
    return super.delete(key, tx);
  }

  public Status put(long id, byte[] key, byte[] value, PersistenceTransaction tx) {
    return super.put(key, value, tx);
  }

  public int deleteCollection(long objectID, PersistenceTransaction tx) throws TCDatabaseException {
    int written = 0;
    Cursor c = db.openCursor(pt2nt(tx), CursorConfig.READ_UNCOMMITTED);
    byte idb[] = Conversion.long2Bytes(objectID);
    DatabaseEntry key = new DatabaseEntry();
    key.setData(idb);
    DatabaseEntry value = new DatabaseEntry();
    value.setPartial(0, 0, true);
    try {
      if (c.getSearchKeyRange(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
        do {
          if (partialMatch(idb, key.getData())) {
            written += key.getSize();
            c.delete();
          } else {
            break;
          }
        } while (c.getNext(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS);
      }
    } catch (Exception t) {
      throw new TCDatabaseException(t.getMessage());
    } finally {
      c.close();
    }
    return written;
  }

  private static boolean partialMatch(byte[] idbytes, byte[] key) {
    if (key.length < idbytes.length) return false;
    for (int i = 0; i < idbytes.length; i++) {
      if (idbytes[i] != key[i]) return false;
    }
    return true;
  }

  private static class BerkeleyMapsTCDatabaseCursor extends BerkeleyDBTCDatabaseCursor implements TCMapsDatabaseCursor {
    private boolean isInit        = false;
    private boolean noMoreMatches = false;
    private long    objectID;

    public BerkeleyMapsTCDatabaseCursor(Cursor cursor, long objectID) {
      super(cursor);
      this.objectID = objectID;
    }

    @Override
    public boolean hasNext() {
      return hasNext(true);
    }

    public boolean hasNextKey() {
      return hasNext(false);
    }

    public TCDatabaseEntry<byte[], byte[]> nextKey() {
      return super.next();
    }

    @Override
    protected boolean hasNext(boolean fetchValue) {
      if (noMoreMatches) { return false; }
      if (entry != null) { return true; }

      if (!isInit) {
        isInit = true;
        if (!getSearchKeyRange(fetchValue)) { return false; }
      } else if (!super.hasNext(fetchValue)) { return false; }

      byte idb[] = Conversion.long2Bytes(objectID);
      if (partialMatch(idb, entry.getKey())) {
        return true;
      } else {
        noMoreMatches = true;
        return false;
      }
    }

    private boolean getSearchKeyRange(boolean fetchValue) {
      DatabaseEntry entryKey = new DatabaseEntry();
      DatabaseEntry entryValue = new DatabaseEntry();
      entryKey.setData(Conversion.long2Bytes(objectID));
      if (!fetchValue) {
        entryValue.setPartial(0, 0, true);
      }
      OperationStatus status = this.getCursor().getSearchKeyRange(entryKey, entryValue, LockMode.DEFAULT);
      if (entry == null) {
        entry = new TCDatabaseEntry<byte[], byte[]>();
      }
      entry.setKey(entryKey.getData()).setValue(entryValue.getData());
      return status.equals(OperationStatus.SUCCESS);
    }
  }

  public long count() {
    return this.db.count();
  }
}
