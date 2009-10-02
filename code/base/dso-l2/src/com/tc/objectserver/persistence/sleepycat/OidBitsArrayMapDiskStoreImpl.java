/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Conversion;
import com.tc.util.OidBitsArrayMap;
import com.tc.util.OidBitsArrayMapImpl;
import com.tc.util.OidLongArray;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class OidBitsArrayMapDiskStoreImpl extends OidBitsArrayMapImpl implements OidBitsArrayMap {

  private static final TCLogger logger = TCLogging.getTestingLogger(FastObjectIDManagerImpl.class);

  private final Database        oidDB;
  private final int             auxKey;

  /*
   * Compressed bits array for ObjectIDs, backed up by a database. If null database, then only in-memory representation.
   */
  public OidBitsArrayMapDiskStoreImpl(int longsPerDiskUnit, Database oidDB) {
    this(longsPerDiskUnit, oidDB, 0);
  }

  /*
   * auxKey: (main key + auxKey) to store different data entry to same db.
   */
  public OidBitsArrayMapDiskStoreImpl(int longsPerDiskUnit, Database oidDB, int auxKey) {
    super(longsPerDiskUnit);
    this.oidDB = oidDB;
    this.auxKey = auxKey;
  }

  @Override
  protected OidLongArray loadArray(long oid, int lPerDiskUnit, long mapIndex) {
    OidLongArray longAry = null;
    try {
      if (oidDB != null) {
        longAry = readDiskEntry(null, oid);
      }
    } catch (DatabaseException e) {
      logger.error("Reading object ID " + oid + ":" + e);
    }
    if (longAry == null) longAry = super.loadArray(oid, lPerDiskUnit, mapIndex);
    return longAry;
  }

  OidLongArray readDiskEntry(Transaction txn, long oid) throws DatabaseException {
    DatabaseEntry key = new DatabaseEntry();
    DatabaseEntry value = new DatabaseEntry();
    long aryIndex = oidIndex(oid);
    key.setData(Conversion.long2Bytes(aryIndex + auxKey));
    OperationStatus status = oidDB.get(txn, key, value, LockMode.DEFAULT);
    if (OperationStatus.SUCCESS.equals(status)) { return new OidLongArray(aryIndex, value.getData()); }
    return null;
  }

  void writeDiskEntry(Transaction txn, OidLongArray bits) throws DatabaseException {
    DatabaseEntry key = new DatabaseEntry();
    DatabaseEntry value = new DatabaseEntry();
    key.setData(bits.keyToBytes(auxKey));

    if (!bits.isZero()) {
      value.setData(bits.arrayToBytes());
      if (!OperationStatus.SUCCESS.equals(this.oidDB.put(txn, key, value))) {
        //
        throw new DatabaseException("Failed to update oidDB at " + bits.getKey());
      }
    } else {
      OperationStatus status = this.oidDB.delete(txn, key);
      // OperationStatus.NOTFOUND happened if added and then deleted in the same batch
      if (!OperationStatus.SUCCESS.equals(status) && !OperationStatus.NOTFOUND.equals(status)) {
        //
        throw new DatabaseException("Failed to delete oidDB at " + bits.getKey());
      }
    }
  }

  /*
   * flush in-memory entry to disk
   */
  public void flushToDisk(Transaction tx) throws DatabaseException {
    Iterator<Map.Entry<Long, OidLongArray>> i = map.entrySet().iterator();
    while (i.hasNext()) {
      Map.Entry<Long, OidLongArray> entry = i.next();
      OidLongArray ary = entry.getValue();
      writeDiskEntry(tx, ary);
      if (ary.isZero()) i.remove();
    }
    map.clear();
  }

  // for testing
  TreeMap<Long, OidLongArray> getMap() {
    return map;
  }

  // for testing
  int getAuxKey() {
    return auxKey;
  }

}
