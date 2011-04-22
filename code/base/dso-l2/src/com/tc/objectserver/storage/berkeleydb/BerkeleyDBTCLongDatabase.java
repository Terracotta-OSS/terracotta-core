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
import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.objectserver.storage.api.TCLongDatabase;
import com.tc.util.Conversion;

import java.util.HashSet;
import java.util.Set;

public class BerkeleyDBTCLongDatabase extends BerkeleyDBTCBytesBytesDatabase implements TCLongDatabase {
  private static final byte[] VALUE = Conversion.long2Bytes(0);
  private final CursorConfig  cursorConfig;

  public BerkeleyDBTCLongDatabase(Database db) {
    super(db);
    this.cursorConfig = new CursorConfig();
    this.cursorConfig.setReadCommitted(true);
  }

  public boolean contains(long key, PersistenceTransaction tx) {
    byte[] keyInBytes = Conversion.long2Bytes(key);
    if (get(keyInBytes, tx) != null) { return true; }
    return false;
  }

  public Status insert(long key, PersistenceTransaction tx) {
    byte[] keyInBytes = Conversion.long2Bytes(key);
    return put(keyInBytes, VALUE, tx);
  }

  public Status delete(long key, PersistenceTransaction tx) {
    byte[] keyInBytes = Conversion.long2Bytes(key);
    return delete(keyInBytes, tx);
  }

  public Set<Long> getAllKeys(PersistenceTransaction tx) {
    Set<Long> set = new HashSet<Long>();
    DatabaseEntry key = new DatabaseEntry();
    Cursor cursor;
    try {
      cursor = db.openCursor(pt2nt(tx), cursorConfig);
      while (OperationStatus.SUCCESS.equals(cursor.getNext(key, new DatabaseEntry(), LockMode.DEFAULT))) {
        set.add(Conversion.bytes2Long(key.getData()));
      }
      cursor.close();
      tx.commit();
    } catch (Exception e) {
      throw new DBException(e);
    }
    return set;
  }
}
