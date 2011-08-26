/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.util.dbperf;

import com.tc.exception.ImplementMe;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCBytesToBytesDatabase;

public class TCBytesToBytesDatabaseTester extends AbstractTCDatabaseTester {
  private final TCBytesToBytesDatabase bytesToBytesDatabase;
  private final int                    keyLength;
  private final int                    valueLength;

  public TCBytesToBytesDatabaseTester(TCBytesToBytesDatabase bytesToBytesDatabase, int keyLength, int valueLength) {
    this.bytesToBytesDatabase = bytesToBytesDatabase;
    this.keyLength = keyLength;
    this.valueLength = valueLength;
  }

  @Override
  protected void insertInternal(PersistenceTransaction tx) {
    long objectId = nextNewObjectId();
    bytesToBytesDatabase.insert(keyWithLong(objectId), newValue(), tx);
  }

  @Override
  protected void updateInternal(PersistenceTransaction tx) {
    long objectId = nextExistentObjectId();
    bytesToBytesDatabase.update(keyWithLong(objectId), newValue(), tx);
  }

  @Override
  protected void putInternal(PersistenceTransaction tx) {
    throw new ImplementMe();
  }

  @Override
  protected void deleteInternal(PersistenceTransaction tx) {
    long objectId = nextOldObjectId();
    bytesToBytesDatabase.delete(keyWithLong(objectId), tx);
  }

  @Override
  protected void getInternal(PersistenceTransaction tx) {
    long objectId = nextExistentObjectId();
    bytesToBytesDatabase.get(keyWithLong(objectId), tx);
  }

  @Override
  protected byte[] keyWithLong(long l) {
    byte[] key = new byte[keyLength];
    key[0] = (byte) ((l & 0xFF00000000000000L) >> 56);
    key[1] = (byte) ((l & 0x00FF000000000000L) >> 48);
    key[2] = (byte) ((l & 0x0000FF0000000000L) >> 40);
    key[3] = (byte) ((l & 0x000000FF00000000L) >> 32);
    key[4] = (byte) ((l & 0x00000000FF000000L) >> 24);
    key[5] = (byte) ((l & 0x0000000000FF0000L) >> 16);
    key[6] = (byte) ((l & 0x000000000000FF00L) >> 8);
    key[7] = (byte) (l & 0x00000000000000FFL);
    return key;
  }

  @Override
  protected byte[] newValue() {
    return new byte[valueLength];
  }
}
