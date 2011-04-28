/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.berkeleydb;

import com.sleepycat.je.Database;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCDatabaseCursor;
import com.tc.objectserver.storage.api.TCDatabaseEntry;
import com.tc.objectserver.storage.api.TCTransactionStoreDatabase;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.util.Conversion;

public class BerkeleyDBTCTransactionStoreDatabase extends BerkeleyDBTCBytesBytesDatabase implements
    TCTransactionStoreDatabase {

  public BerkeleyDBTCTransactionStoreDatabase(Database db) {
    super(db);
  }

  public Status delete(long id, PersistenceTransaction tx) {
    byte[] key = (Conversion.long2Bytes(id));
    return delete(key, tx);
  }

  public Status insert(long id, byte[] value, PersistenceTransaction tx) {
    byte[] key = Conversion.long2Bytes(id);
    return put(key, value, tx);
  }

  @Override
  public TCDatabaseCursor openCursor(PersistenceTransaction tx) {
    TCDatabaseCursor byteByteCursor = super.openCursor(tx);
    return new BerkeleyDBLongBytesTCDatabaseCursor(byteByteCursor);
  }

  static class BerkeleyDBLongBytesTCDatabaseCursor implements TCDatabaseCursor<Long, byte[]> {
    private final TCDatabaseCursor<byte[], byte[]> byteByteCursor;

    public BerkeleyDBLongBytesTCDatabaseCursor(TCDatabaseCursor byteByteCursor) {
      this.byteByteCursor = byteByteCursor;
    }

    public void close() {
      this.byteByteCursor.close();
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
