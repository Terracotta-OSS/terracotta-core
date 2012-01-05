/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.objectserver.persistence.db.TCCollectionsSerializer;
import com.tc.objectserver.persistence.db.TCCollectionsSerializerImpl;

import java.util.concurrent.CyclicBarrier;

public class TCMapsClearDeadlockTest extends AbstractDatabaseTest {
  private TCMapsDatabase                       database;
  private final CyclicBarrier                  barrier                        = new CyclicBarrier(2);
  private static final TCCollectionsSerializer serializer                     = new TCCollectionsSerializerImpl();
  private static final long                    TESTMAP_CLEAR_DEADLOCK_TIMEOUT = 120000;
  private volatile boolean                     testMapClearDeadlockFailed     = false;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    this.database = getDbenv().getMapsDatabase();
  }

  public void testMapClearDeadlock() throws Exception {
    PersistenceTransaction tx = newTransaction();
    database.insert(tx, 1, "123", "abc", serializer);
    database.insert(tx, 1, "321", "abc", serializer);
    database.insert(tx, 2, "123", "abc", serializer);
    database.insert(tx, 2, "321", "abc", serializer);
    tx.commit();
    MapClearThread clearer1 = new MapClearThread(1);
    clearer1.start();
    MapClearThread clearer2 = new MapClearThread(2);
    clearer2.start();
    clearer1.join(TESTMAP_CLEAR_DEADLOCK_TIMEOUT);
    clearer2.join(TESTMAP_CLEAR_DEADLOCK_TIMEOUT);
    assertFalse(testMapClearDeadlockFailed);
  }

  private class MapClearThread extends Thread {
    private final long mapId;

    private MapClearThread(long mapId) {
      this.mapId = mapId;
    }

    @Override
    public void run() {
      try {
        PersistenceTransaction tx = newTransaction();
        barrier.await();
        database.deleteCollection(this.mapId, tx);
        barrier.await();
        tx.commit();
      } catch (Exception e) {
        e.printStackTrace();
        testMapClearDeadlockFailed = true;
      }
    }
  }
}
