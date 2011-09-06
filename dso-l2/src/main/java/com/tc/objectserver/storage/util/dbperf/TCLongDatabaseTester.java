/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.util.dbperf;

import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCLongDatabase;

public class TCLongDatabaseTester extends AbstractTCDatabaseTester {
  private final TCLongDatabase longDatabase;

  public TCLongDatabaseTester(TCLongDatabase longDatabase) {
    this.longDatabase = longDatabase;
  }

  @Override
  protected void insertInternal(PersistenceTransaction tx) {
    long objectId = nextNewObjectId();
    longDatabase.insert(objectId, tx);
  }

  @Override
  protected void updateInternal(PersistenceTransaction tx) {
    throw new AssertionError("Can't update with TCLongDatabase.");
  }

  @Override
  protected void putInternal(PersistenceTransaction tx) {
    throw new AssertionError("Can't put with TCLongDatabase.");
  }

  @Override
  protected void deleteInternal(PersistenceTransaction tx) {
    long objectId = nextOldObjectId();
    longDatabase.delete(objectId, tx);
  }

  @Override
  protected void getInternal(PersistenceTransaction tx) {
    long objectId = nextExistentObjectId();
    longDatabase.contains(objectId, tx);
  }
}
