/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.util.dbperf;

import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCLongToBytesDatabase;

public class TCLongToBytesDatabaseTester extends AbstractTCDatabaseTester {
  private final TCLongToBytesDatabase objectDB;

  public TCLongToBytesDatabaseTester(TCLongToBytesDatabase objectDB) {
    this.objectDB = objectDB;
  }

  @Override
  protected void insertInternal(PersistenceTransaction tx) {
    long objectId = nextNewObjectId();
    objectDB.insert(objectId, newValue(), tx);
  }

  @Override
  protected void updateInternal(PersistenceTransaction tx) {
    long objectId = nextExistentObjectId();
    objectDB.update(objectId, newValue(), tx);
  }

  @Override
  protected void putInternal(PersistenceTransaction tx) {
    long objectId = 0;
    if (random.nextBoolean() && getNumberOfObjects() > 100) {
      objectId = nextExistentObjectId();
    } else {
      objectId = nextNewObjectId();
    }
    objectDB.put(objectId, newValue(), tx);
  }

  @Override
  protected void deleteInternal(PersistenceTransaction tx) {
    long objectId = nextOldObjectId();
    objectDB.delete(objectId, tx);
  }

  @Override
  protected void getInternal(PersistenceTransaction tx) {
    long objectId = nextExistentObjectId();
    objectDB.get(objectId, tx);
  }
}
