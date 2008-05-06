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
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor.SleepycatPersistorBase;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.Conversion;
import com.tc.util.ObjectIDSet2;
import com.tc.util.OidLongArray;
import com.tc.util.SyncObjectIdSet;
import com.tc.util.sequence.MutableSequence;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

public final class FastObjectIDManagerImpl extends SleepycatPersistorBase implements ObjectIDManager {
  private static final TCLogger                logger                   = TCLogging
                                                                            .getTestingLogger(FastObjectIDManagerImpl.class);
  private final static int                     SEQUENCE_BATCH_SIZE      = 50000;
  // property
  public final static String                   LOAD_OBJECTID_PROPERTIES = "l2.objectmanager.loadObjectID";
  private final static String                  LONGS_PER_DISK_ENTRY     = "longsPerDiskEntry";
  public final static String                   MEASURE_PERF             = "measure.performance";                             // hidden
  private final static String                  CHCKPOINT_CHANGES        = "checkpoint.changes";
  private final static String                  CHCKPOINT_TIMEPERIOD     = "checkpoint.timeperiod";
  private final static String                  CHCKPOINT_MAXLIMIT       = "checkpoint.maxlimit";
  private final int                            checkpointChanges;
  private final int                            checkpointMaxLimit;
  private final int                            checkpointPeriod;
  private final int                            longsPerDiskEntry;
  private final boolean                        isMeasurePerf;

  private final Database                       oidDB;
  private final Database                       oidLogDB;
  private final PersistenceTransactionProvider ptp;
  private final CursorConfig                   oidDBCursorConfig;
  private final int                            bitsPerLong              = OidLongArray.BitsPerLong;
  private final AtomicInteger                  changesCount             = new AtomicInteger(0);
  private final CheckpointRunner               checkpointThread;
  private final Object                         checkpointSyncObj        = new Object();
  private final Object                         objectIDUpdateSyncObj    = new Object();
  private final MutableSequence                sequence;
  private long                                 nextSequence;
  private long                                 endSequence;

  public FastObjectIDManagerImpl(Database oidDB, Database oidLogDB, PersistenceTransactionProvider ptp,
                                 CursorConfig oidDBCursorConfig, MutableSequence sequence) {
    this.oidDB = oidDB;
    this.oidLogDB = oidLogDB;
    this.ptp = ptp;
    this.oidDBCursorConfig = oidDBCursorConfig;

    TCProperties loadObjProp = TCPropertiesImpl.getProperties().getPropertiesFor(LOAD_OBJECTID_PROPERTIES);
    checkpointChanges = loadObjProp.getInt(CHCKPOINT_CHANGES);
    checkpointMaxLimit = loadObjProp.getInt(CHCKPOINT_MAXLIMIT);
    checkpointPeriod = loadObjProp.getInt(CHCKPOINT_TIMEPERIOD);
    longsPerDiskEntry = loadObjProp.getInt(LONGS_PER_DISK_ENTRY);
    isMeasurePerf = loadObjProp.getBoolean(MEASURE_PERF, false);

    this.sequence = sequence;
    nextSequence = this.sequence.nextBatch(SEQUENCE_BATCH_SIZE);
    endSequence = nextSequence + SEQUENCE_BATCH_SIZE;

    // start checkpoint thread
    checkpointThread = new CheckpointRunner(checkpointPeriod);
    checkpointThread.setDaemon(true);
    checkpointThread.start();
  }

  /*
   * A thread to read in ObjectIDs from compressed DB at server restart
   */
  public Runnable getObjectIDReader(SyncObjectIdSet rv) {
    return new OidObjectIdReader(rv);
  }

  public void stopCheckpointRunner() {
    checkpointThread.quit();
  }

  /*
   * changesCount: the amount of changes to trigger checkpoint.
   */
  private void incChangesCount(int n) {
    if (changesCount.addAndGet(n) > checkpointChanges) {
      synchronized (checkpointSyncObj) {
        checkpointSyncObj.notifyAll();
      }
      logger.debug("Checkpoint waked up by " + changesCount.get() + " changes");
      resetChangesCount();
    }
  }

  private void incChangesCount() {
    incChangesCount(1);
  }

  private void resetChangesCount() {
    changesCount.set(0);
  }

  private synchronized long nextSeqID() {
    if (nextSequence == endSequence) {
      nextSequence = this.sequence.nextBatch(SEQUENCE_BATCH_SIZE);
      endSequence = nextSequence + SEQUENCE_BATCH_SIZE;
    }
    return(nextSequence++);
  }

  /*
   * Log key to make log records ordered in time sequenece
   */
  private byte[] makeLogKey(boolean isAdd) {
    byte[] rv = new byte[OidLongArray.BytesPerLong  + 1];
    Conversion.writeLong(nextSeqID(), rv, 0);
    rv[OidLongArray.BytesPerLong] = (byte) (isAdd ? 0 : 1);
    return rv;
  }

  private boolean isAddOper(byte[] logKey) {
    return (logKey[OidLongArray.BytesPerLong] == 0);
  }

  /*
   * Log the change of an ObjectID, added or deleted. Later, flush to BitsArray OidDB by checkpoint thread.
   */
  private OperationStatus logObjectID(PersistenceTransaction tx, byte[] oids, boolean isAdd) throws DatabaseException {
    DatabaseEntry key = new DatabaseEntry();
    key.setData(makeLogKey(isAdd));
    DatabaseEntry value = new DatabaseEntry();
    value.setData(oids);
    OperationStatus rtn = this.oidLogDB.putNoOverwrite(pt2nt(tx), key, value);
    return (rtn);
  }

  private OperationStatus logAddObjectID(PersistenceTransaction tx, ObjectID objectID) throws DatabaseException {
    OperationStatus status = logObjectID(tx, Conversion.long2Bytes(objectID.toLong()), true);
    if (OperationStatus.SUCCESS.equals(status)) incChangesCount();
    return (status);
  }

  /*
   * Flush out oidLogDB to bitsArray on disk.
   */
  private void oidFlushLogToBitsArray(StoppedFlag stoppedFlag, boolean isNoLimit) {
    synchronized (objectIDUpdateSyncObj) {
      if (stoppedFlag.isStopped()) return;
      OidBitsArrayMap oidBitsArrayMap = new OidBitsArrayMap(longsPerDiskEntry, this.oidDB);
      SortedSet<Long> sortedOnDiskIndexSet = new TreeSet<Long>();
      PersistenceTransaction tx = null;
      try {
        tx = ptp.newTransaction();
        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry value = new DatabaseEntry();
        int changes = 0;
        Cursor cursor = oidLogDB.openCursor(pt2nt(tx), CursorConfig.READ_COMMITTED);
        try {
          while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {

            if (stoppedFlag.isStopped()) {
              cursor.close();
              cursor = null;
              abortOnError(tx);
              return;
            }

            boolean isAddOper = isAddOper(key.getData());
            byte[] oids = value.getData();
            int offset = 0;
            while (offset < oids.length) {
              long oidValue = Conversion.bytes2Long(oids, offset);
              ObjectID objectID = new ObjectID(oidValue);
              if (isAddOper) {
                oidBitsArrayMap.getAndSet(objectID);
              } else {
                oidBitsArrayMap.getAndClr(objectID);
              }
              sortedOnDiskIndexSet.add(new Long(oidBitsArrayMap.oidIndex(oidValue)));
              offset += OidLongArray.BytesPerLong;
              ++changes;
            }
            cursor.delete();

            if (!isNoLimit && (changes >= checkpointMaxLimit)) {
              cursor.close();
              cursor = null;
              break;
            }
          }
        } catch (DatabaseException e) {
          throw e;
        } finally {
          if (cursor != null) cursor.close();
          cursor = null;
        }

        resetChangesCount();

        for (Long onDiskIndex : sortedOnDiskIndexSet) {
          if (stoppedFlag.isStopped()) {
            abortOnError(tx);
            return;
          }
          OidLongArray bits = oidBitsArrayMap.getBitsArray(onDiskIndex);
          key.setData(bits.keyToBytes());
          if (!bits.isZero()) {
            value.setData(bits.arrayToBytes());
            if (!OperationStatus.SUCCESS.equals(this.oidDB.put(pt2nt(tx), key, value))) {
              //
              throw new DatabaseException("Failed to update oidDB at " + onDiskIndex);
            }
          } else {
            OperationStatus status = this.oidDB.delete(pt2nt(tx), key);
            // OperationStatus.NOTFOUND happened if added and then deleted in the same batch
            if (!OperationStatus.SUCCESS.equals(status) && !OperationStatus.NOTFOUND.equals(status)) {
              //
              throw new DatabaseException("Failed to delete oidDB at " + onDiskIndex);
            }
          }
        }

        tx.commit();
        logger.debug("Checkpoint updated " + changes + " objectIDs");
      } catch (DatabaseException e) {
        logger.error("Error ojectID checkpoint: " + e);
        abortOnError(tx);
      } finally {
        oidBitsArrayMap.clear();
      }
    }
  }

  private void processPreviousRunOidLog() {
    oidFlushLogToBitsArray(new StoppedFlag(), true);
  }

  private void processCheckpoint(StoppedFlag stoppedFlag) {
    oidFlushLogToBitsArray(stoppedFlag, false);
  }

  private static class StoppedFlag {
    private volatile boolean isStopped = false;

    public boolean isStopped() {
      return isStopped;
    }

    public void setStopped(boolean stopped) {
      this.isStopped = stopped;
    }
  }

  /*
   * Periodically flush oid from oidLogDB to oidDB
   */
  private class CheckpointRunner extends Thread {
    private final int         timeperiod;
    private final StoppedFlag stoppedFlag = new StoppedFlag();

    public CheckpointRunner(int timeperiod) {
      super("ObjectID-Checkpoint");
      this.timeperiod = timeperiod;
    }

    public void quit() {
      stoppedFlag.setStopped(true);
    }

    public void run() {
      while (!stoppedFlag.isStopped()) {
        // Wait for enough changes or specified time-period
        synchronized (checkpointSyncObj) {
          try {
            checkpointSyncObj.wait(timeperiod);
          } catch (InterruptedException e) {
            //
          }
        }
        if (stoppedFlag.isStopped()) break;
        processCheckpoint(stoppedFlag);
      }
    }
  }

  /*
   * fast way to load object-Ids at server restart by reading them from bits array
   */
  private class OidObjectIdReader implements Runnable {
    private long                    startTime;
    private int                     counter     = 0;
    private final StoppedFlag       stoppedFlag = new StoppedFlag();
    protected final SyncObjectIdSet set;

    public OidObjectIdReader(SyncObjectIdSet set) {
      this.set = set;
    }

    public void stop() {
      stoppedFlag.setStopped(true);
    }

    public void run() {
      if (isMeasurePerf) startTime = System.currentTimeMillis();

      // process left over from previous run
      processPreviousRunOidLog();

      ObjectIDSet2 tmp = new ObjectIDSet2();
      PersistenceTransaction tx = null;
      Cursor cursor = null;
      try {
        cursor = oidDB.openCursor(pt2nt(tx), oidDBCursorConfig);
        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry value = new DatabaseEntry();
        while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
          OidLongArray bitsArray = new OidLongArray(key.getData(), value.getData());
          makeObjectIDFromBitsArray(bitsArray, tmp);
        }

        if (isMeasurePerf) {
          logger.info("MeasurePerf: done");
        }
      } catch (Throwable t) {
        logger.error("Error Reading Object IDs", t);
      } finally {
        safeClose(cursor);
        safeCommit(tx);
        set.stopPopulating(tmp);
        tmp = null;
      }
    }

    private void makeObjectIDFromBitsArray(OidLongArray entry, ObjectIDSet2 tmp) {
      long oid = entry.getKey();
      long[] ary = entry.getArray();
      for (int j = 0; j < ary.length; ++j) {
        long bit = 1L;
        long bits = ary[j];
        for (int i = 0; i < bitsPerLong; ++i) {
          if ((bits & bit) != 0) {
            tmp.add(new ObjectID(oid));

            if (isMeasurePerf && ((++counter % 1000) == 0)) {
              long elapse_time = System.currentTimeMillis() - startTime;
              long avg_time = elapse_time / (counter / 1000);
              logger.info("MeasurePerf: reading " + counter + " OIDs took " + elapse_time + "ms avg(1000 objs):"
                          + avg_time + " ms");
            }

          }
          bit <<= 1;
          ++oid;
        }
      }
    }

    protected void safeCommit(PersistenceTransaction tx) {
      if (tx == null) return;
      try {
        tx.commit();
      } catch (Throwable t) {
        logger.error("Error Committing Transaction", t);
      }
    }

    protected void safeClose(Cursor c) {
      if (c == null) return;

      try {
        c.close();
      } catch (Throwable e) {
        logger.error("Error closing cursor", e);
      }
    }
  }

  public OperationStatus put(PersistenceTransaction tx, ObjectID objectID) throws DatabaseException {

    return (logAddObjectID(tx, objectID));
  }

  public void prePutAll(Set<ObjectID> oidSet, ObjectID objectID) {
    oidSet.add(objectID);
  }

  private OperationStatus doAll(PersistenceTransaction tx, Set<ObjectID> oidSet, boolean isAdd)
      throws TCDatabaseException {
    OperationStatus status = OperationStatus.SUCCESS;
    int size = oidSet.size();
    if (size == 0) return (status);

    byte[] oids = new byte[size * OidLongArray.BytesPerLong];
    int offset = 0;
    for (ObjectID objectID : oidSet) {
      Conversion.writeLong(objectID.toLong(), oids, offset);
      offset += OidLongArray.BytesPerLong;
    }
    try {
      status = logObjectID(tx, oids, isAdd);
    } catch (DatabaseException de) {
      throw new TCDatabaseException(de);
    }
    incChangesCount(size);
    return (status);
  }

  public OperationStatus putAll(PersistenceTransaction tx, Set<ObjectID> oidSet) throws TCDatabaseException {
    return (doAll(tx, oidSet, true));
  }

  public OperationStatus deleteAll(PersistenceTransaction tx, Set<ObjectID> oidSet) throws TCDatabaseException {
    return (doAll(tx, oidSet, false));
  }

  public static class OidBitsArrayMap {
    private final Database oidDB;
    private final HashMap  map;
    private final int      bitsLength;
    private final int      longsPerDiskUnit;

    OidBitsArrayMap(int longsPerDiskUnit, Database oidDB) {
      this.oidDB = oidDB;
      this.longsPerDiskUnit = longsPerDiskUnit;
      this.bitsLength = longsPerDiskUnit * OidLongArray.BitsPerLong;
      map = new HashMap();
    }

    public void clear() {
      map.clear();
    }

    private Long oidIndex(long oid) {
      return new Long(oid / bitsLength * bitsLength);
    }

    public OidLongArray getBitsArray(long oid) {
      Long mapIndex = oidIndex(oid);
      OidLongArray longAry = null;
      synchronized (map) {
        if (map.containsKey(mapIndex)) {
          longAry = (OidLongArray) map.get(mapIndex);
        }
      }
      return longAry;
    }

    private OidLongArray getOrLoadBitsArray(long oid) {
      Long mapIndex = oidIndex(oid);
      OidLongArray longAry;
      synchronized (map) {
        if (map.containsKey(mapIndex)) {
          longAry = (OidLongArray) map.get(mapIndex);
        } else {
          longAry = readDiskEntry(oid);
          if (longAry == null) longAry = new OidLongArray(longsPerDiskUnit, mapIndex.longValue());
          map.put(mapIndex, longAry);
        }
      }
      return longAry;
    }

    private OidLongArray readDiskEntry(long oid) {
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      key.setData(Conversion.long2Bytes(oidIndex(oid)));
      try {
        OperationStatus status = oidDB.get(null, key, value, LockMode.DEFAULT);
        if (OperationStatus.SUCCESS.equals(status)) { return new OidLongArray(key.getData(), value.getData()); }
      } catch (DatabaseException e) {
        logger.error("Reading object ID " + oid + ":" + e);
      }
      return null;
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

    // for testing
    void loadBitsArrayFromDisk() {
      clear();
      Cursor cursor = null;
      try {
        cursor = oidDB.openCursor(null, CursorConfig.READ_COMMITTED);
        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry value = new DatabaseEntry();
        while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
          OidLongArray bitsArray = new OidLongArray(key.getData(), value.getData());
          map.put(new Long(bitsArray.getKey()), bitsArray);
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

    // for testing
    boolean contains(ObjectID id) {
      long oid = id.toLong();
      Long mapIndex = oidIndex(oid);
      synchronized (map) {
        if (map.containsKey(mapIndex)) {
          OidLongArray longAry = (OidLongArray) map.get(mapIndex);
          return (longAry.isSet((int) oid % bitsLength));
        }
      }
      return (false);
    }
  }

  /*
   * for testing purpose. Load bitsArray from disk
   */
  OidBitsArrayMap loadBitsArrayFromDisk() {
    OidBitsArrayMap oidMap = new OidBitsArrayMap(longsPerDiskEntry, this.oidDB);
    oidMap.loadBitsArrayFromDisk();
    return (oidMap);
  }

  /*
   * for testing purpose only. Return all IDs with ObjectID
   */
  Collection bitsArrayMapToObjectID() {
    OidBitsArrayMap oidMap = loadBitsArrayFromDisk();
    HashSet objectIDs = new HashSet();
    for (Iterator i = oidMap.map.keySet().iterator(); i.hasNext();) {
      long oid = ((Long) i.next()).longValue();
      OidLongArray bits = oidMap.getBitsArray(oid);
      for (int offset = 0; offset < bits.totalBits(); ++offset) {
        if (bits.isSet(offset)) {
          Assert.assertTrue("Same object ID represented by different bits in memory", objectIDs
              .add(new ObjectID(oid + offset)));
        }
      }
    }
    return (objectIDs);
  }

  /*
   * for testing purpose only.
   */
  void runCheckpoint() {
    processPreviousRunOidLog();
  }

}
