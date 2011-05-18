/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.persistence.api.PersistentCollectionsUtil;
import com.tc.objectserver.persistence.db.DBPersistorImpl.DBPersistorBase;
import com.tc.objectserver.storage.api.DBEnvironment;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.storage.api.TCBytesToBytesDatabase;
import com.tc.objectserver.storage.api.TCDatabaseCursor;
import com.tc.objectserver.storage.api.TCDatabaseEntry;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Conversion;
import com.tc.util.ObjectIDSet;
import com.tc.util.OidLongArray;
import com.tc.util.SyncObjectIdSet;
import com.tc.util.sequence.MutableSequence;

import java.util.SortedSet;

/**
 * A good start to understand this is to go and look at the explanation in the intranet link<br>
 * http://intranet.terracotta.lan/xwiki/bin/view/Main/DgcOptDelete<br>
 * <br>
 * How it works?<br>
 * On creation of new objects or deletion of objects due to DGC, we keep writing to a oidStoreLogDB.<br>
 * Then a check pointer thread runs periodically which iterates through this oidStoreLogDB. This checkpoint threads
 * flushes data to objectOidStoreDB, mapsOidStoreDB and evictableOidStoreDB after compressing it properly.<br>
 * <br>
 * Format of data stored in oidStoreLogDB:<br>
 * Key: First 8 bytes are sequence-id, then 1 byte whether this is ADD/DELETE op. Hence total 9 bytes.<br>
 * Value: Set of (8 bytes ObjectID, then 1 byte for type of object). Thus 9 bytes * (no of objects).<br>
 * <br>
 * Format of data stored in objectOidStoreDB, mapsOidStoreDB and evictableOidStoreDB:<br>
 * Key: Base id<br>
 * Value: 64 length byte array.<br>
 * e.g. if u want to store ObjectID=513, then Base id = 512 & Value will have 2nd bit set.<br>
 */
public final class FastObjectIDManagerImpl extends DBPersistorBase implements ObjectIDManager {
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

  private final TCBytesToBytesDatabase         objectOidStoreDB;
  private final TCBytesToBytesDatabase         mapsOidStoreDB;
  private final TCBytesToBytesDatabase         oidStoreLogDB;
  private final TCBytesToBytesDatabase         evictableOidStoreDB;
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
  private boolean logObjectID(final PersistenceTransaction tx, final byte[] oids, final boolean isAdd)
      throws TCDatabaseException {
    boolean rtn;
    try {
      rtn = this.oidStoreLogDB.putNoOverwrite(tx, makeLogKey(isAdd), oids) == Status.SUCCESS;
    } catch (final Exception t) {
      throw new TCDatabaseException(t.getMessage());
    }
    return rtn;
  }

  private boolean logAddObjectID(final PersistenceTransaction tx, final ManagedObject mo) throws TCDatabaseException {
    return logObjectID(tx, makeLogValue(mo), true);
  }

  /*
   * Flush out oidLogDB to bitsArray on disk.
   */
  boolean flushToCompressedStorage(final StoppedFlag stoppedFlag, final int maxProcessLimit) {
    boolean isAllFlushed = true;
    synchronized (objectIDUpdateLock) {
      if (stoppedFlag.isStopped()) return isAllFlushed;
      final OidBitsArrayMapDiskStoreImpl oidStoreMap = new OidBitsArrayMapDiskStoreImpl(this.longsPerDiskEntry,
                                                                                        this.objectOidStoreDB, ptp);
      final OidBitsArrayMapDiskStoreImpl mapOidStoreMap = new OidBitsArrayMapDiskStoreImpl(this.longsPerStateEntry,
                                                                                           this.mapsOidStoreDB, ptp);

      final OidBitsArrayMapDiskStoreImpl evictableOidStoreMap = new OidBitsArrayMapDiskStoreImpl(
                                                                                                 this.longsPerStateEntry,
                                                                                                 this.evictableOidStoreDB,
                                                                                                 ptp);
      PersistenceTransaction tx = null;
      try {
        tx = ptp.newTransaction();
        TCDatabaseCursor<byte[], byte[]> cursor = oidStoreLogDB.openCursorUpdatable(tx);
        int changes = 0;
        try {
          while (cursor.hasNext()) {
            TCDatabaseEntry<byte[], byte[]> entry = cursor.next();

            if (stoppedFlag.isStopped()) {
              cursor.close();
              cursor = null;
              abortOnError(tx);
              return isAllFlushed;
            }

            final boolean isAddOper = isAddOper(entry.getKey());
            final byte[] oids = entry.getValue();

            int offset = 0;
            while (offset < oids.length) {
              final ObjectID objectID = new ObjectID(Conversion.bytes2Long(oids, offset));
              final byte idByte = oids[offset + OidLongArray.BYTES_PER_LONG];
              if (isAddOper) {
                oidStoreMap.getAndSet(objectID, tx);
                if ((idByte & this.PERSISTABLE_COLLECTION) == this.PERSISTABLE_COLLECTION) {
                  mapOidStoreMap.getAndSet(objectID, tx);
                }
                if ((idByte & this.EVICTABLE_OBJECT) == this.EVICTABLE_OBJECT) {
                  evictableOidStoreMap.getAndSet(objectID, tx);
                }
              } else {
                oidStoreMap.getAndClr(objectID, tx);
                if ((idByte & this.PERSISTABLE_COLLECTION) == this.PERSISTABLE_COLLECTION) {
                  mapOidStoreMap.getAndClr(objectID, tx);
                }
                if ((idByte & this.EVICTABLE_OBJECT) == this.EVICTABLE_OBJECT) {
                  evictableOidStoreMap.getAndClr(objectID, tx);
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
        oidStoreMap.flushToDisk(tx);
        mapOidStoreMap.flushToDisk(tx);
        evictableOidStoreMap.flushToDisk(tx);

        tx.commit();
        logger.debug("Checkpoint updated " + changes + " objectIDs");
      } catch (final TCDatabaseException e) {
        logger.error("Error ojectID checkpoint: ", e);
        abortOnError(tx);
        throw new TCRuntimeException(e);
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
    private long                         startTime;
    private int                          counter = 0;
    private final TCBytesToBytesDatabase oidDB;
    private final SyncObjectIdSet        syncObjectIDSet;

    public OidObjectIdReader(final TCBytesToBytesDatabase oidDB, final SyncObjectIdSet syncObjectIDSet) {
      this.oidDB = oidDB;
      this.syncObjectIDSet = syncObjectIDSet;
    }

    public void run() {
      if (FastObjectIDManagerImpl.this.isMeasurePerf) {
        this.startTime = System.currentTimeMillis();
      }

      final ObjectIDSet tmp = new ObjectIDSet();
      final PersistenceTransaction tx = FastObjectIDManagerImpl.this.ptp.newTransaction();
      TCDatabaseCursor<byte[], byte[]> cursor = null;
      try {
        cursor = oidDB.openCursor(tx);
        while (cursor.hasNext()) {
          TCDatabaseEntry<byte[], byte[]> entry = cursor.next();
          OidLongArray bitsArray = new OidLongArray(entry.getKey(), entry.getValue());
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
        throw new TCRuntimeException(t);
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

    protected void safeClose(TCDatabaseCursor c) {
      if (c == null) return;

      try {
        c.close();
      } catch (final Throwable e) {
        logger.error("Error closing cursor", e);
      }
    }
  }

  public boolean put(PersistenceTransaction tx, ManagedObject mo) throws TCDatabaseException {
    return (logAddObjectID(tx, mo));
  }

  public boolean deleteAll(final PersistenceTransaction tx, final SortedSet<ObjectID> oidsToDelete,
                           final SyncObjectIdSet extantMapTypeOidSet, final SyncObjectIdSet extantEvictableOidSet)
      throws TCDatabaseException {
    boolean status = true;
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

  public boolean putAll(final PersistenceTransaction tx, final SortedSet<ManagedObject> newManagedObjects)
      throws TCDatabaseException {
    boolean status = true;
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
