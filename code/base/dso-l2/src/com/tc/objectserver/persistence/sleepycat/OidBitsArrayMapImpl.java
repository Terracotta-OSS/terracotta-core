/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.util.Conversion;
import com.tc.util.OidLongArray;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class OidBitsArrayMapImpl implements OidBitsArrayMap {
  private static final TCLogger             logger = TCLogging.getTestingLogger(OidBitsArrayMapImpl.class);

  private final Database                    oidDB;
  private final TreeMap<Long, OidLongArray> map;
  private final int                         bitsLength;
  private final int                         longsPerDiskUnit;
  private final int                         auxKey;

  /*
   * Compressed bits array for ObjectIDs, backed up by a database. If null database, then only in-memory representation.
   */
  public OidBitsArrayMapImpl(int longsPerDiskUnit, Database oidDB) {
    this(longsPerDiskUnit, oidDB, 0);
  }

  /*
   * auxKey: (main key + auxKey) to store different data entry to same db.
   */
  public OidBitsArrayMapImpl(int longsPerDiskUnit, Database oidDB, int auxKey) {
    this.oidDB = oidDB;
    this.longsPerDiskUnit = longsPerDiskUnit;
    this.bitsLength = longsPerDiskUnit * OidLongArray.BITS_PER_LONG;
    map = new TreeMap();
    this.auxKey = auxKey;
  }

  public void clear() {
    map.clear();
  }

  public int getAuxKey() {
    return auxKey;
  }

  private Long oidIndex(long oid) {
    return new Long(oid / bitsLength * bitsLength);
  }

  public Long oidIndex(ObjectID id) {
    long oid = id.toLong();
    return new Long(oid / bitsLength * bitsLength);
  }

  OidLongArray getBitsArray(long oid) {
    Long mapIndex = oidIndex(oid);
    synchronized (map) {
      return map.get(mapIndex);
    }
  }

  private OidLongArray getOrLoadBitsArray(long oid) {
    Long mapIndex = oidIndex(oid);
    OidLongArray longAry;
    synchronized (map) {
      longAry = map.get(mapIndex);
      if (longAry == null) {
        try {
          longAry = readDiskEntry(null, oid);
        } catch (DatabaseException e) {
          logger.error("Reading object ID " + oid + ":" + e);
        }
        if (longAry == null) longAry = new OidLongArray(longsPerDiskUnit, mapIndex.longValue());
        map.put(mapIndex, longAry);
      }
    }
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
  public void updateToDisk(Transaction tx) throws DatabaseException {
    synchronized (map) {
      Iterator<Map.Entry<Long, OidLongArray>> i = map.entrySet().iterator();
      while (i.hasNext()) {
        Map.Entry<Long, OidLongArray> entry = i.next();
        OidLongArray ary = entry.getValue();
        writeDiskEntry(tx, ary);
        if (ary.isZero()) i.remove();
      }
    }
  }

  private OidLongArray getAndModify(long oid, boolean doSet) {
    OidLongArray longAry = getOrLoadBitsArray(oid);
    int oidInArray = (int) (oid % bitsLength);
    synchronized (longAry) {
      if (doSet) {
        longAry.setBit(oidInArray);
      } else {
        longAry.clrBit(oidInArray);
      }
    }
    return (longAry);
  }

  public OidLongArray getAndSet(ObjectID id) {
    return (getAndModify(id.toLong(), true));
  }

  public OidLongArray getAndClr(ObjectID id) {
    return (getAndModify(id.toLong(), false));
  }

  public boolean contains(ObjectID id) {
    long oid = id.toLong();
    Long mapIndex = oidIndex(oid);
    synchronized (map) {
      if (map.containsKey(mapIndex)) {
        OidLongArray longAry = map.get(mapIndex);
        return (longAry.isSet((int) oid % bitsLength));
      }
    }
    return (false);
  }

  // for testing
  void loadAllFromDisk() {
    synchronized (map) {
      clear();
      Cursor cursor = null;
      try {
        cursor = oidDB.openCursor(null, CursorConfig.READ_COMMITTED);
        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry value = new DatabaseEntry();
        while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
          // load its only records indicated by auxKey
          long index = Conversion.bytes2Long(key.getData());
          if (index == (oidIndex(index) + auxKey)) {
            index -= auxKey;
            OidLongArray bitsArray = new OidLongArray(index, value.getData());
            map.put(new Long(index), bitsArray);
          }
        }
        cursor.close();
        cursor = null;
      } catch (DatabaseException e) {
        throw new RuntimeException(e);
      } finally {
        try {
          if (cursor != null) cursor.close();
        } catch (DatabaseException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  // for testing
  void saveAllToDisk() {
    synchronized (map) {
      // use another set to avoid ConcurrentModificationException
      Set dupKeySet = new HashSet();
      dupKeySet.addAll(map.keySet());
      Iterator i = dupKeySet.iterator();
      while (i.hasNext()) {
        OidLongArray bitsArray = map.get(i.next());
        try {
          writeDiskEntry(null, bitsArray);
        } catch (DatabaseException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  // for testing
  Set getMapKeySet() {
    return map.keySet();
  }
  
  // for testing
  public String toString() {
    StringBuffer buf = new StringBuffer();
    for(Map.Entry<Long, OidLongArray> entry : map.entrySet()) {
      Long base = entry.getKey();
      OidLongArray ary = entry.getValue();
      for(int i = 0; i < ary.totalBits(); ++i) {
        if (ary.isSet(i)) {
          buf.append(" " + (base+i));
        }
      }
    }
    return buf.toString();
  }
}
