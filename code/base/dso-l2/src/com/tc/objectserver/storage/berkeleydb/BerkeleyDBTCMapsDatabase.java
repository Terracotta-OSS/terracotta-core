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
import com.tc.objectserver.persistence.db.TCCollectionsSerializer;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCDatabaseEntry;
import com.tc.objectserver.storage.api.TCMapsDatabase;
import com.tc.objectserver.storage.api.TCMapsDatabaseCursor;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
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
      c = openCursor(tx, id);
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
  private TCMapsDatabaseCursor openCursor(final PersistenceTransaction tx, final long objectID) {
    // XXX:: Since we read in one direction and since we have to read the first record of the next map to break out, we
    // need READ_COMMITTED to avoid deadlocks between commit thread and DGC thread.
    final Cursor cursor = this.db.openCursor(pt2nt(tx), CursorConfig.READ_UNCOMMITTED);
    return new BerkeleyMapsTCDatabaseCursor(cursor, objectID);
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

  public int put(final PersistenceTransaction tx, final long id, final Object key, final Object value,
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
  public void deleteCollection(final long id, final PersistenceTransaction tx) {
    deleteCollectionBatched(id, tx, Integer.MAX_VALUE);
  }

  /**
   * Deletes a collection but only up to a max delete batch size and returns the number of entries deleted.
   * 
   * @return number of entries in Maps database deleted, if less than DELETE_BATCH_SIZE, then there could be more
   *         entries for the same map ID.
   * @throws TCDatabaseException
   */
  public int deleteCollectionBatched(final long id, final PersistenceTransaction tx, final int maxDeleteBatchSize) {
    int mapEntriesDeleted = 0;
    final TCMapsDatabaseCursor cursor = openCursor(tx, id);
    try {
      while (mapEntriesDeleted <= maxDeleteBatchSize && cursor.hasNextKey()) {
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

    public BerkeleyMapsTCDatabaseCursor(final Cursor cursor, final long objectID) {
      super(cursor);
      this.objectID = objectID;
    }

    @Override
    public boolean hasNext() {
      return hasNext(true);
    }

    public boolean hasNextKey() {
      return hasNext(false);
    }

    public TCDatabaseEntry<byte[], byte[]> nextKey() {
      return super.next();
    }

    @Override
    protected boolean hasNext(final boolean fetchValue) {
      if (this.noMoreMatches) { return false; }
      if (this.entry != null) { return true; }

      if (!this.isInit) {
        this.isInit = true;
        if (!getSearchKeyRange(fetchValue)) { return false; }
      } else if (!super.hasNext(fetchValue)) { return false; }

      final byte idb[] = Conversion.long2Bytes(this.objectID);
      if (partialMatch(idb, this.entry.getKey())) {
        return true;
      } else {
        this.noMoreMatches = true;
        return false;
      }
    }

    private boolean getSearchKeyRange(final boolean fetchValue) {
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

}
