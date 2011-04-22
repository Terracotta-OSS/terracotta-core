/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.berkeleydb;

import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCLongToStringDatabase;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.util.Conversion;

import gnu.trove.TLongObjectHashMap;

public class BerkeleyDBTCLongToStringDatabase extends AbstractBerkeleyDatabase implements TCLongToStringDatabase {
  private final CursorConfig  cursorConfig = new CursorConfig();
  private final SerialBinding serialBinding;

  public BerkeleyDBTCLongToStringDatabase(ClassCatalog catalog, Database db) {
    super(db);
    this.cursorConfig.setReadCommitted(true);
    this.serialBinding = new SerialBinding(catalog, String.class);
  }

  public TLongObjectHashMap loadMappingsInto(TLongObjectHashMap target, PersistenceTransaction tx) {
    Cursor cursor = null;
    try {
      DatabaseEntry key = new DatabaseEntry(), value = new DatabaseEntry();
      cursor = db.openCursor(pt2nt(tx), cursorConfig);
      while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
        target.put(Conversion.bytes2Long(key.getData()), bytes2String(value));
      }
      cursor.close();
      tx.commit();
    } catch (Throwable t) {
      if (cursor != null) {
        cursor.close();
      }
      tx.abort();
      throw new DBException(t);
    }
    return target;
  }

  public Status insert(long index, String string, PersistenceTransaction tx) {
    DatabaseEntry entryKey = new DatabaseEntry();
    DatabaseEntry entryValue = new DatabaseEntry();
    entryKey.setData(Conversion.long2Bytes(index));
    string2Bytes(string, entryValue);
    return this.db.put(pt2nt(tx), entryKey, entryValue).equals(OperationStatus.SUCCESS) ? Status.SUCCESS
        : Status.NOT_SUCCESS;
  }

  private String bytes2String(DatabaseEntry entry) {
    return (String) serialBinding.entryToObject(entry);
  }

  private void string2Bytes(String string, DatabaseEntry entry) {
    serialBinding.objectToEntry(string, entry);
  }
}
