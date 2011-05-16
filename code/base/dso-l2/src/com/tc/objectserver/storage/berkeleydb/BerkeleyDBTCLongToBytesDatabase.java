/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.berkeleydb;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCDatabaseCursor;
import com.tc.objectserver.storage.api.TCDatabaseEntry;
import com.tc.objectserver.storage.api.TCLongToBytesDatabase;
import com.tc.objectserver.storage.api.TCTransactionStoreDatabase;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.util.Conversion;

public class BerkeleyDBTCLongToBytesDatabase extends BerkeleyDBTCBytesBytesDatabase implements TCLongToBytesDatabase,
    TCTransactionStoreDatabase {
  private static final TCLogger logger = TCLogging.getLogger(BerkeleyDBTCLongToBytesDatabase.class);

  private final SampledCounter  l2FaultFromDisk;

  public BerkeleyDBTCLongToBytesDatabase(Database db) {
    this(db, SampledCounter.NULL_SAMPLED_COUNTER);
  }

  public BerkeleyDBTCLongToBytesDatabase(Database db, SampledCounter l2FaultFromDisk) {
    super(db);
    this.l2FaultFromDisk = l2FaultFromDisk;
  }

  public Status delete(long id, PersistenceTransaction tx) {
    byte[] key = (Conversion.long2Bytes(id));
    return delete(key, tx);
  }

  public Status put(long id, byte[] val, PersistenceTransaction tx) {
    byte[] key = Conversion.long2Bytes(id);
    return put(key, val, tx);
  }

  public byte[] get(long id, PersistenceTransaction tx) {
    // not calling super.get as we need to check the Operation status for not found
    DatabaseEntry key = new DatabaseEntry();
    key.setData(Conversion.long2Bytes(id));
    DatabaseEntry value = new DatabaseEntry();
    OperationStatus status = this.db.get(pt2nt(tx), key, value, LockMode.DEFAULT);
    if (OperationStatus.SUCCESS.equals(status)) {
      l2FaultFromDisk.increment();
      return value.getData();
    } else if (OperationStatus.NOTFOUND.equals(status)) { return null; }

    throw new DBException("Error retrieving object id: " + id + "; status: " + status);
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

  public Status insert(long id, byte[] b, PersistenceTransaction tx) {
    return put(id, b, tx);
  }

  public Status update(long id, byte[] b, PersistenceTransaction tx) {
    return put(id, b, tx);
  }

  @Override
  public String toString() {
    return "BerkeleyDB-TCLongToBytesDatabase";
  }

  @Override
  public TCDatabaseCursor openCursor(PersistenceTransaction tx) {
    TCDatabaseCursor byteByteCursor = super.openCursor(tx);
    return new BerkeleyDBLongBytesTCDatabaseCursor(byteByteCursor);
  }

  static class BerkeleyDBLongBytesTCDatabaseCursor implements TCDatabaseCursor<Long, byte[]> {
    private final TCDatabaseCursor<byte[], byte[]> byteByteCursor;
    private volatile boolean                       isClosed = false;

    public BerkeleyDBLongBytesTCDatabaseCursor(TCDatabaseCursor byteByteCursor) {
      this.byteByteCursor = byteByteCursor;
    }

    public void close() {
      this.byteByteCursor.close();
      isClosed = true;
    }

    @Override
    protected void finalize() throws Throwable {
      if (!isClosed) {
        logger.info("Since the closed for the cursor was not called. So calling it explicity in finalize.");
        close();
      }
      super.finalize();
    }

    public void delete() {
      this.byteByteCursor.delete();
    }

    public boolean hasNext() {
      return this.byteByteCursor.hasNext();
    }

    public TCDatabaseEntry<Long, byte[]> next() {
      TCDatabaseEntry<byte[], byte[]> entry = this.byteByteCursor.next();
      return new TCDatabaseEntry<Long, byte[]>(Conversion.bytes2Long(entry.getKey()), entry.getValue());
    }
  }
}
