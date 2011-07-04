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
import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.db.BatchedTransaction;
import com.tc.objectserver.persistence.db.TCCollectionsSerializer;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCDatabaseEntry;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.objectserver.storage.api.TCMapsDatabase;
import com.tc.objectserver.storage.api.TCMapsDatabaseCursor;
import com.tc.util.Conversion;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BerkeleyDBTCMapsDatabase extends BerkeleyDBTCBytesBytesDatabase implements TCMapsDatabase {

  private final BackingMapFactory factory = new BackingMapFactory() {
                                            public Map createBackingMapFor(final ObjectID mapID) {
                                              return new HashMap(0);
                                            }
                                          };

  public BerkeleyDBTCMapsDatabase(final Database db) {
    super(db);
  }

  public void loadMap(final PersistenceTransaction tx, final long id, final Map map,
                      final TCCollectionsSerializer serializer) throws TCDatabaseException {
    final byte idb[] = Conversion.long2Bytes(id);
    TCMapsDatabaseCursor c = null;
    try {
      c = openCursor(tx, id, true);
      while (c.hasNext()) {
        final TCDatabaseEntry<byte[], byte[]> entry = c.next();
        final Object mkey = serializer.deserialize(idb.length, entry.getKey());
        final Object mvalue = serializer.deserialize(entry.getValue());
        map.put(mkey, mvalue);
      }
    } catch (final Exception t) {
      throw new TCDatabaseException(t.getMessage());
    } finally {
      if (c != null) {
        c.close();
      }
    }
  }

  /**
   * These are the possible ways for isolation <br>
   * CursorConfig.DEFAULT : Default configuration used if null is passed to methods that create a cursor. <br>
   * CursorConfig.READ_COMMITTED : This ensures the stability of the current data item read by the cursor but permits
   * data read by this cursor to be modified or deleted prior to the commit of the transaction. <br>
   * CursorConfig.READ_UNCOMMITTED : A convenience instance to configure read operations performed by the cursor to
   * return modified but not yet committed data.<br>
   * <p>
   * During our testing we found that READ_UNCOMMITTED does not raise any problem and gives a performance enhancement
   * over READ_COMMITTED. Since we never read the map which has been marked for deletion by the DGC the deadlocks are
   * avoided
   */
  private TCMapsDatabaseCursor openCursor(final PersistenceTransaction tx, final long objectID, boolean fetchValue) {
    // XXX:: Since we read in one direction and since we have to read the first record of the next map to break out, we
    // need READ_COMMITTED to avoid deadlocks between commit thread and DGC thread.
    final Cursor cursor = this.db.openCursor(pt2nt(tx), CursorConfig.READ_UNCOMMITTED);
    return new BerkeleyMapsTCDatabaseCursor(cursor, objectID, fetchValue);
  }

  public int delete(final PersistenceTransaction tx, final long id, final Object key,
                    final TCCollectionsSerializer serializer) throws TCDatabaseException, IOException {
    final byte[] k = serializer.serialize(id, key);
    final int written = k.length;
    Status delStatus = super.delete(k, tx);
    final boolean status = (delStatus == Status.SUCCESS || delStatus == Status.NOT_FOUND);
    if (!status) { throw new TCDatabaseException("Unable to remove Map Entry for object id: " + id + ", status: "
                                                 + status + ", key: " + Arrays.toString(k)); }
    return written;
  }

  private int put(final PersistenceTransaction tx, final long id, final Object key, final Object value,
                  final TCCollectionsSerializer serializer) throws TCDatabaseException, IOException {
    final byte[] k = serializer.serialize(id, key);
    final byte[] v = serializer.serialize(value);
    final int written = v.length + k.length;
    final boolean status = super.put(k, v, tx) == Status.SUCCESS;
    if (!status) { throw new TCDatabaseException("Unable to update Map table : " + id + " status : " + status); }
    return written;
  }

  /**
   * BerkeleyDB has the most inefficient way to delete objects. Another way would be to delete all records explicitly.
   * <p>
   * This method Doesn't support more than Integer.MAX_VALUE in one collection.
   */
  public int deleteCollection(final long id, final PersistenceTransaction tx) {
    return deleteBatch(id, tx, Integer.MAX_VALUE);
  }

  /**
   * Deletes a collection by batch. The batch size is passed as param
   * 
   * @return number of entries deleted from Maps database for the passed id
   * @throws TCDatabaseException
   */
  public void deleteCollectionBatched(final long id, final BatchedTransaction batchedTransaction) {
    int deletedBatchCount = 0;
    do {
      deletedBatchCount = deleteBatch(id, batchedTransaction.getCurrentTransaction(), batchedTransaction.getBatchSize());
      batchedTransaction.optionalCommit(deletedBatchCount);
    } while (deletedBatchCount >= batchedTransaction.getBatchSize());
  }

  private int deleteBatch(final long id, final PersistenceTransaction tx, final int maxDeleteBatchSize) {
    int mapEntriesDeleted = 0;
    final TCMapsDatabaseCursor cursor = openCursor(tx, id, false);
    try {
      while (mapEntriesDeleted < maxDeleteBatchSize && cursor.hasNextKey()) {
        cursor.nextKey();
        cursor.delete();
        mapEntriesDeleted++;
      }
    } finally {
      cursor.close();
    }
    return mapEntriesDeleted;
  }

  private static boolean partialMatch(final byte[] idbytes, final byte[] key) {
    if (key.length < idbytes.length) { return false; }
    for (int i = 0; i < idbytes.length; i++) {
      if (idbytes[i] != key[i]) { return false; }
    }
    return true;
  }

  public long count(PersistenceTransaction tx) {
    return this.db.count();
  }

  public BackingMapFactory getBackingMapFactory(final TCCollectionsSerializer serializer) {
    return this.factory;
  }

  private static class BerkeleyMapsTCDatabaseCursor extends BerkeleyDBTCDatabaseCursor implements TCMapsDatabaseCursor {
    private boolean    isInit        = false;
    private boolean    noMoreMatches = false;
    private final long objectID;

    public BerkeleyMapsTCDatabaseCursor(final Cursor cursor, final long objectID, boolean fetchValue) {
      super(cursor, fetchValue);
      this.objectID = objectID;
    }

    public boolean hasNextKey() {
      return hasNext();
    }

    public TCDatabaseEntry<byte[], byte[]> nextKey() {
      return super.next();
    }

    @Override
    public boolean hasNext() {
      if (this.noMoreMatches) { return false; }
      if (this.entry != null) { return true; }

      if (!this.isInit) {
        this.isInit = true;
        if (!getSearchKeyRange()) { return false; }
      } else if (!super.hasNext()) { return false; }

      final byte idb[] = Conversion.long2Bytes(this.objectID);
      if (partialMatch(idb, this.entry.getKey())) {
        return true;
      } else {
        this.noMoreMatches = true;
        return false;
      }
    }

    private boolean getSearchKeyRange() {
      final DatabaseEntry entryKey = new DatabaseEntry();
      final DatabaseEntry entryValue = new DatabaseEntry();
      entryKey.setData(Conversion.long2Bytes(this.objectID));
      if (!fetchValue) {
        entryValue.setPartial(0, 0, true);
      }
      final OperationStatus status = this.getCursor().getSearchKeyRange(entryKey, entryValue, LockMode.DEFAULT);
      if (this.entry == null) {
        this.entry = new TCDatabaseEntry<byte[], byte[]>();
      }
      this.entry.setKey(entryKey.getData()).setValue(entryValue.getData());
      return status.equals(OperationStatus.SUCCESS);
    }
  }

  @Override
  public String toString() {
    return "BerkeleyDB-TCMapsDatabase";
  }

  public int update(PersistenceTransaction tx, long id, Object key, Object value, TCCollectionsSerializer serializer)
      throws IOException, TCDatabaseException {
    return put(tx, id, key, value, serializer);
  }

  public int insert(PersistenceTransaction tx, long id, Object key, Object value, TCCollectionsSerializer serializer)
      throws IOException, TCDatabaseException {
    return put(tx, id, key, value, serializer);
  }

}
