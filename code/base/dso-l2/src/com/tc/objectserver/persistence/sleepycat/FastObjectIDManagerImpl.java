/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.api.PersistentCollectionsUtil;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor.SleepycatPersistorBase;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Conversion;
import com.tc.util.ObjectIDSet;
import com.tc.util.OidLongArray;
import com.tc.util.SyncObjectIdSet;
import com.tc.util.sequence.MutableSequence;

import java.util.SortedSet;

public final class FastObjectIDManagerImpl extends SleepycatPersistorBase implements ObjectIDManager {
  private static final TCLogger                logger                     = TCLogging
                                                                              .getTestingLogger(FastObjectIDManagerImpl.class);
  private final static int                     SEQUENCE_BATCH_SIZE        = 50000;
  private final static int                     MINIMUM_WAIT_TIME          = 1000;

  private final byte                           NOT_PERSISTABLE_COLLECTION = 0x00;
  private final byte                           PERSISTABLE_COLLECTION     = 0x01;
  private final byte                           EVICTABLE_OBJECT           = 0x02;

  private final byte                           ADD_OBJECT_ID              = 0x00;
  private final byte                           DEL_OBJECT_ID              = 0x01;
  // property
  private final int                            checkpointMaxLimit;
  private final int                            checkpointMaxSleep;
  private final int                            longsPerDiskEntry;
  private final int                            longsPerStateEntry;
  private final boolean                        isMeasurePerf;
  private final Object                         checkpointLock             = new Object();
  private final Object                         objectIDUpdateLock         = new Object();

  private final Database                       objectOidStoreDB;
  private final Database                       mapsOidStoreDB;
  private final Database                       evictableOidStoreDB;
  private final Database                       oidStoreLogDB;
  private final PersistenceTransactionProvider ptp;
  private final CheckpointRunner               checkpointThread;
  private final MutableSequence                sequence;
  private long                                 nextSequence;
  private long                                 endSequence;

  public FastObjectIDManagerImpl(final DBEnvironment env, final PersistenceTransactionProvider ptp,
                                 final MutableSequence sequence) throws TCDatabaseException {
    this.objectOidStoreDB = env.getObjectOidStoreDatabase();
    this.mapsOidStoreDB = env.getMapsOidStoreDatabase();
    this.evictableOidStoreDB = env.getEvictableOidStoreDatabase();
    this.oidStoreLogDB = env.getOidStoreLogDatabase();
    this.ptp = ptp;

    this.checkpointMaxLimit = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_LOADOBJECTID_CHECKPOINT_MAXLIMIT);
    this.checkpointMaxSleep = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_LOADOBJECTID_CHECKPOINT_MAXSLEEP);
    this.longsPerDiskEntry = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_LOADOBJECTID_LONGS_PERDISKENTRY);
    this.longsPerStateEntry = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_LOADOBJECTID_MAPDB_LONGS_PERDISKENTRY);
    this.isMeasurePerf = TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.L2_OBJECTMANAGER_LOADOBJECTID_MEASURE_PERF, false);

    this.sequence = sequence;
    this.nextSequence = this.sequence.nextBatch(SEQUENCE_BATCH_SIZE);
    this.endSequence = this.nextSequence + SEQUENCE_BATCH_SIZE;

    // process left over from previous run
    processPreviousRunToCompressedStorage();

    // start checkpoint thread
    this.checkpointThread = new CheckpointRunner(this.checkpointMaxSleep);
    this.checkpointThread.setDaemon(true);
    this.checkpointThread.start();
  }

  /*
   * A thread to read in ObjectIDs from compressed DB at server restart
   */
  public Runnable getObjectIDReader(final SyncObjectIdSet objectIDSet) {
    return new OidObjectIdReader(this.objectOidStoreDB, objectIDSet);
  }

  /*
   * A thread to read in MapType ObjectIDs from compressed DB at server restart
   */
  public Runnable getMapsObjectIDReader(final SyncObjectIdSet objectIDSet) {
    return new OidObjectIdReader(this.mapsOidStoreDB, objectIDSet);
  }

  /*
   * A thread to read in Evictable ObjectIDs from compressed DB at server restart
   */
  public Runnable getEvictableObjectIDReader(final SyncObjectIdSet objectIDSet) {
    return new OidObjectIdReader(this.evictableOidStoreDB, objectIDSet);
  }

  public void stopCheckpointRunner() {
    this.checkpointThread.quit();
  }

  private synchronized long nextSeqID() {
    if (this.nextSequence == this.endSequence) {
      this.nextSequence = this.sequence.nextBatch(SEQUENCE_BATCH_SIZE);
      this.endSequence = this.nextSequence + SEQUENCE_BATCH_SIZE;
    }
    return (this.nextSequence++);
  }

  /*
   * Log key to make log records ordered in time sequence
   */
  private byte[] makeLogKey(final boolean isAdd) {
    final byte[] rv = new byte[OidLongArray.BYTES_PER_LONG + 1];
    Conversion.writeLong(nextSeqID(), rv, 0);
    rv[OidLongArray.BYTES_PER_LONG] = isAdd ? this.ADD_OBJECT_ID : this.DEL_OBJECT_ID;
    return rv;
  }

  /*
   * Log ObjectID+MapType in db value
   */
  private byte[] makeLogValue(final ManagedObject mo) {
    final byte[] rv = new byte[OidLongArray.BYTES_PER_LONG + 1];
    Conversion.writeLong(mo.getID().toLong(), rv, 0);
    rv[OidLongArray.BYTES_PER_LONG] = getIDFromStateObjectType(mo.getManagedObjectState().getType());
    return rv;
  }

  private byte getIDFromStateObjectType(final byte type) {
    byte id = PersistentCollectionsUtil.isPersistableCollectionType(type) ? this.PERSISTABLE_COLLECTION
        : this.NOT_PERSISTABLE_COLLECTION;
    if (PersistentCollectionsUtil.isEvictableMapType(type)) {
      id |= this.EVICTABLE_OBJECT;
    }
    return id;
  }

  private byte getIDFromExtantSets(final boolean isPersistableCollection, final boolean isEvictableObject) {
    byte id = isPersistableCollection ? this.PERSISTABLE_COLLECTION : this.NOT_PERSISTABLE_COLLECTION;
    if (isEvictableObject) {
      id |= this.EVICTABLE_OBJECT;
    }
    return id;
  }

  private boolean isAddOper(final byte[] logKey) {
    return (logKey[OidLongArray.BYTES_PER_LONG] == this.ADD_OBJECT_ID);
  }

  /*
   * Log the change of an ObjectID, added or deleted. Later, flush to BitsArray OidDB by checkpoint thread.
   */
  private OperationStatus logObjectID(final PersistenceTransaction tx, final byte[] oids, final boolean isAdd)
      throws TCDatabaseException {
    final DatabaseEntry key = new DatabaseEntry();
    key.setData(makeLogKey(isAdd));
    final DatabaseEntry value = new DatabaseEntry();
    value.setData(oids);
    OperationStatus rtn;
    try {
      rtn = this.oidStoreLogDB.putNoOverwrite(pt2nt(tx), key, value);
    } catch (final Exception t) {
      throw new TCDatabaseException(t.getMessage());
    }
    return (rtn);
  }

  private OperationStatus logAddObjectID(final PersistenceTransaction tx, final ManagedObject mo)
      throws TCDatabaseException {
    final OperationStatus status = logObjectID(tx, makeLogValue(mo), true);
    return (status);
  }

  /*
   * Flush out oidLogDB to bitsArray on disk.
   */
  boolean flushToCompressedStorage(final StoppedFlag stoppedFlag, final int maxProcessLimit) {
    boolean isAllFlushed = true;
    synchronized (this.objectIDUpdateLock) {
      if (stoppedFlag.isStopped()) { return isAllFlushed; }
      final OidBitsArrayMapDiskStoreImpl oidStoreMap = new OidBitsArrayMapDiskStoreImpl(this.longsPerDiskEntry,
                                                                                        this.objectOidStoreDB);
      final OidBitsArrayMapDiskStoreImpl mapOidStoreMap = new OidBitsArrayMapDiskStoreImpl(this.longsPerStateEntry,
                                                                                           this.mapsOidStoreDB);
      final OidBitsArrayMapDiskStoreImpl evictableOidStoreMap = new OidBitsArrayMapDiskStoreImpl(
                                                                                                 this.longsPerStateEntry,
                                                                                                 this.evictableOidStoreDB);

      PersistenceTransaction tx = null;
      try {
        tx = this.ptp.newTransaction();
        Cursor cursor = this.oidStoreLogDB.openCursor(pt2nt(tx), CursorConfig.READ_COMMITTED);
        final DatabaseEntry key = new DatabaseEntry();
        final DatabaseEntry value = new DatabaseEntry();
        int changes = 0;
        try {
          while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {

            if (stoppedFlag.isStopped()) {
              cursor.close();
              cursor = null;
              abortOnError(tx);
              return isAllFlushed;
            }

            final boolean isAddOper = isAddOper(key.getData());
            final byte[] oids = value.getData();
            int offset = 0;
            while (offset < oids.length) {
              final ObjectID objectID = new ObjectID(Conversion.bytes2Long(oids, offset));
              final byte idByte = oids[offset + OidLongArray.BYTES_PER_LONG];
              if (isAddOper) {
                oidStoreMap.getAndSet(objectID);
                if ((idByte & this.PERSISTABLE_COLLECTION) == this.PERSISTABLE_COLLECTION) {
                  mapOidStoreMap.getAndSet(objectID);
                }
                if ((idByte & this.EVICTABLE_OBJECT) == this.EVICTABLE_OBJECT) {
                  evictableOidStoreMap.getAndSet(objectID);
                }
              } else {
                oidStoreMap.getAndClr(objectID);
                if ((idByte & this.PERSISTABLE_COLLECTION) == this.PERSISTABLE_COLLECTION) {
                  mapOidStoreMap.getAndClr(objectID);
                }
                if ((idByte & this.EVICTABLE_OBJECT) == this.EVICTABLE_OBJECT) {
                  evictableOidStoreMap.getAndClr(objectID);
                }
              }

              offset += OidLongArray.BYTES_PER_LONG + 1;
              ++changes;
            }
            cursor.delete();

            if (maxProcessLimit > 0 && changes >= maxProcessLimit) {
              cursor.close();
              cursor = null;
              isAllFlushed = false;
              break;
            }
          }
        } catch (final Exception e) {
          throw new TCDatabaseException(e.getMessage());
        } finally {
          if (cursor != null) {
            cursor.close();
          }
          cursor = null;
        }
        oidStoreMap.flushToDisk(pt2nt(tx));
        mapOidStoreMap.flushToDisk(pt2nt(tx));
        evictableOidStoreMap.flushToDisk(pt2nt(tx));

        tx.commit();
        logger.debug("Checkpoint updated " + changes + " objectIDs");
      } catch (final TCDatabaseException e) {
        logger.error("Error ojectID checkpoint: " + e);
        abortOnError(tx);
      }
    }
    return isAllFlushed;
  }

  private void processPreviousRunToCompressedStorage() {
    flushToCompressedStorage(new StoppedFlag(), Integer.MAX_VALUE);
  }

  private boolean checkpointToCompressedStorage(final StoppedFlag stoppedFlag, final int maxProcessLimit) {
    return flushToCompressedStorage(stoppedFlag, maxProcessLimit);
  }

  static class StoppedFlag {
    private volatile boolean isStopped = false;

    public boolean isStopped() {
      return this.isStopped;
    }

    public void setStopped(final boolean stopped) {
      this.isStopped = stopped;
    }
  }

  /*
   * Periodically flush oid from oidLogDB to oidDB
   */
  private class CheckpointRunner extends Thread {
    private final int         maxSleep;
    private final StoppedFlag stoppedFlag = new StoppedFlag();

    public CheckpointRunner(final int maxSleep) {
      super("ObjectID-CompressedStorage Checkpoint");
      this.maxSleep = maxSleep;
    }

    public void quit() {
      this.stoppedFlag.setStopped(true);
    }

    @Override
    public void run() {
      int currentwait = this.maxSleep;
      int maxProcessLimit = FastObjectIDManagerImpl.this.checkpointMaxLimit;
      while (!this.stoppedFlag.isStopped()) {
        // Sleep for enough changes or specified time-period
        synchronized (FastObjectIDManagerImpl.this.checkpointLock) {
          try {
            FastObjectIDManagerImpl.this.checkpointLock.wait(currentwait);
          } catch (final InterruptedException e) {
            //
          }
        }

        if (this.stoppedFlag.isStopped()) {
          break;
        }
        final boolean isAllFlushed = checkpointToCompressedStorage(this.stoppedFlag, maxProcessLimit);

        if (isAllFlushed) {
          // All flushed, wait longer for next time
          currentwait += currentwait;
          if (currentwait > this.maxSleep) {
            currentwait = this.maxSleep;
            maxProcessLimit = FastObjectIDManagerImpl.this.checkpointMaxLimit;
          }
        } else {
          // reduce wait time to catch up
          currentwait = currentwait / 2;
          // at least wait 1 second
          if (currentwait < MINIMUM_WAIT_TIME) {
            currentwait = MINIMUM_WAIT_TIME;
            maxProcessLimit = -1; // unlimited
          }
        }
      }
    }
  }

  /*
   * fast way to load object-Ids at server restart by reading them from bits array
   */
  private class OidObjectIdReader implements Runnable {
    private long                  startTime;
    private int                   counter = 0;
    private final Database        oidDB;
    private final SyncObjectIdSet syncObjectIDSet;

    public OidObjectIdReader(final Database oidDB, final SyncObjectIdSet syncObjectIDSet) {
      this.oidDB = oidDB;
      this.syncObjectIDSet = syncObjectIDSet;
    }

    public void run() {
      if (FastObjectIDManagerImpl.this.isMeasurePerf) {
        this.startTime = System.currentTimeMillis();
      }

      final ObjectIDSet tmp = new ObjectIDSet();
      final PersistenceTransaction tx = FastObjectIDManagerImpl.this.ptp.newTransaction();
      Cursor cursor = null;
      try {
        final CursorConfig oidDBCursorConfig = new CursorConfig();
        oidDBCursorConfig.setReadCommitted(true);
        cursor = this.oidDB.openCursor(pt2nt(tx), oidDBCursorConfig);
        final DatabaseEntry key = new DatabaseEntry();
        final DatabaseEntry value = new DatabaseEntry();
        while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
          final OidLongArray bitsArray = new OidLongArray(key.getData(), value.getData());
          makeObjectIDFromBitsArray(bitsArray, tmp);
        }
        cursor.close();
        cursor = null;

        safeCommit(tx);
        if (FastObjectIDManagerImpl.this.isMeasurePerf) {
          logger.info("MeasurePerf: done");
        }
      } catch (final Throwable t) {
        logger.error("Error Reading Object IDs", t);
      } finally {
        safeClose(cursor);
        this.syncObjectIDSet.stopPopulating(tmp);
      }
    }

    private void makeObjectIDFromBitsArray(final OidLongArray entry, final ObjectIDSet tmp) {
      long oid = entry.getKey();
      final long[] ary = entry.getArray();
      for (int j = 0; j < ary.length; ++j) {
        long bit = 1L;
        final long bits = ary[j];
        for (int i = 0; i < OidLongArray.BITS_PER_LONG; ++i) {
          if ((bits & bit) != 0) {
            tmp.add(new ObjectID(oid));
            if (FastObjectIDManagerImpl.this.isMeasurePerf && ((++this.counter % 1000) == 0)) {
              final long elapse_time = System.currentTimeMillis() - this.startTime;
              final long avg_time = elapse_time / (this.counter / 1000);
              logger.info("MeasurePerf: reading " + this.counter + " OIDs took " + elapse_time + "ms avg(1000 objs):"
                          + avg_time + " ms");
            }
          }
          bit <<= 1;
          ++oid;
        }
      }
    }

    protected void safeCommit(final PersistenceTransaction tx) {
      if (tx == null) { return; }
      try {
        tx.commit();
      } catch (final Throwable t) {
        logger.error("Error Committing Transaction", t);
      }
    }

    protected void safeClose(final Cursor c) {
      if (c == null) { return; }

      try {
        c.close();
      } catch (final Throwable e) {
        logger.error("Error closing cursor", e);
      }
    }
  }

  public OperationStatus put(final PersistenceTransaction tx, final ManagedObject mo) throws TCDatabaseException {
    return (logAddObjectID(tx, mo));
  }

  public OperationStatus deleteAll(final PersistenceTransaction tx, final SortedSet<ObjectID> oidsToDelete,
                                   final SyncObjectIdSet extantMapTypeOidSet,
                                   final SyncObjectIdSet extantEvictableOidSet) throws TCDatabaseException {
    OperationStatus status = OperationStatus.SUCCESS;
    final int size = oidsToDelete.size();
    if (size == 0) { return (status); }

    final byte[] oids = new byte[size * (OidLongArray.BYTES_PER_LONG + 1)];
    int offset = 0;
    for (final ObjectID objectID : oidsToDelete) {
      Conversion.writeLong(objectID.toLong(), oids, offset);
      oids[offset + OidLongArray.BYTES_PER_LONG] = getIDFromExtantSets(extantMapTypeOidSet.contains(objectID),
                                                                       extantEvictableOidSet.contains(objectID));
      offset += OidLongArray.BYTES_PER_LONG + 1;
    }
    try {
      status = logObjectID(tx, oids, false);
    } catch (final Exception de) {
      throw new TCDatabaseException(de.getMessage());
    }
    return (status);
  }

  public OperationStatus putAll(final PersistenceTransaction tx, final SortedSet<ManagedObject> newManagedObjects)
      throws TCDatabaseException {
    OperationStatus status = OperationStatus.SUCCESS;
    final int size = newManagedObjects.size();
    if (size == 0) { return (status); }

    final byte[] oids = new byte[size * (OidLongArray.BYTES_PER_LONG + 1)];
    int offset = 0;
    for (final ManagedObject mo : newManagedObjects) {
      Conversion.writeLong(mo.getID().toLong(), oids, offset);
      oids[offset + OidLongArray.BYTES_PER_LONG] = getIDFromStateObjectType(mo.getManagedObjectState().getType());
      offset += OidLongArray.BYTES_PER_LONG + 1;
    }
    try {
      status = logObjectID(tx, oids, true);
    } catch (final Exception de) {
      throw new TCDatabaseException(de.getMessage());
    }
    return (status);
  }

}
