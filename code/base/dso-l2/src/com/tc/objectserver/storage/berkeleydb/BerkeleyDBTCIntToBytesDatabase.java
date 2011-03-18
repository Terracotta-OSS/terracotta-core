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
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.objectserver.storage.api.TCIntToBytesDatabase;
import com.tc.util.Conversion;

import java.util.HashMap;
import java.util.Map;

public class BerkeleyDBTCIntToBytesDatabase extends BerkeleyDBTCBytesBytesDatabase implements TCIntToBytesDatabase {
  private final CursorConfig cursorConfig = new CursorConfig();

  public BerkeleyDBTCIntToBytesDatabase(Database db) {
    super(db);
    this.cursorConfig.setReadCommitted(true);
  }

  public byte[] get(int id, PersistenceTransaction tx) {
    byte[] key = Conversion.int2Bytes(id);
    return this.get(key, tx);
  }

  public Status put(int id, byte[] b, PersistenceTransaction tx) {
    byte[] key = Conversion.int2Bytes(id);
    return put(key, b, tx);
  }

  public Map<Integer, byte[]> getAll(PersistenceTransaction tx) {
    Map<Integer, byte[]> allClazzBytes = new HashMap<Integer, byte[]>();
    Cursor cursor = null;
    try {
      cursor = db.openCursor(pt2nt(tx), cursorConfig);
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
        allClazzBytes.put(Integer.valueOf(Conversion.bytes2Int(key.getData())), value.getData());
      }
      cursor.close();
      tx.commit();
    } catch (Exception e) {
      if (cursor != null) {
        cursor.close();
      }
      tx.abort();
    }
    return allClazzBytes;
  }

}