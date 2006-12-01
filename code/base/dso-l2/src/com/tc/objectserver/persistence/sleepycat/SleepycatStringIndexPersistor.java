/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.api.StringIndexPersistor;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor.SleepycatPersistorBase;
import com.tc.util.Conversion;

import gnu.trove.TLongObjectHashMap;

public final class SleepycatStringIndexPersistor extends SleepycatPersistorBase implements StringIndexPersistor {

  private final PersistenceTransactionProvider ptp;
  private final Database                       stringIndexDatabase;
  private final CursorConfig                   stringIndexCursorConfig;
  private final SerialBinding                  serialBinding;
  private final SynchronizedBoolean            initialized = new SynchronizedBoolean(false);

  public SleepycatStringIndexPersistor(PersistenceTransactionProvider ptp, Database stringIndexDatabase,
                                       CursorConfig stringIndexCursorConfig, ClassCatalog classCatalog) {
    this.ptp = ptp;
    this.stringIndexDatabase = stringIndexDatabase;
    this.stringIndexCursorConfig = stringIndexCursorConfig;
    this.serialBinding = new SerialBinding(classCatalog, String.class);
  }

  public TLongObjectHashMap loadMappingsInto(TLongObjectHashMap target) {
    if (initialized.set(true)) throw new AssertionError("Attempt to use more than once.");
    Cursor cursor = null;
    PersistenceTransaction tx = null;
    try {
      tx = ptp.newTransaction();
      DatabaseEntry key = new DatabaseEntry(), value = new DatabaseEntry();
      cursor = stringIndexDatabase.openCursor(pt2nt(tx), stringIndexCursorConfig);
      while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
        target.put(Conversion.bytes2Long(key.getData()), bytes2String(value));
      }
      cursor.close();
      tx.commit();
    } catch (Throwable t) {
      abortOnError(cursor, tx);
      throw new DBException(t);
    }
    return target;
  }

  public void saveMapping(long index, String string) {
    PersistenceTransaction tx = null;
    try {
      tx = ptp.newTransaction();
      DatabaseEntry key = new DatabaseEntry(), value = new DatabaseEntry();
      key.setData(Conversion.long2Bytes(index));
      string2Bytes(string, value);
      stringIndexDatabase.put(pt2nt(tx), key, value);
      tx.commit();
    } catch (Throwable t) {
      abortOnError(tx);
      throw new DBException(t);
    }
  }

  private String bytes2String(DatabaseEntry entry) {
    return (String) serialBinding.entryToObject(entry);
  }

  private void string2Bytes(String string, DatabaseEntry entry) {
    serialBinding.objectToEntry(string, entry);
  }

}