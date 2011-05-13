/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.util.dbperf;

import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.util.Conversion;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractTCDatabaseTester {
  private static final int VALUE_SIZE       = 128;
  private final AtomicLong objectsDeleted   = new AtomicLong();
  private final AtomicLong objectsCreated   = new AtomicLong();
  protected final Random   random           = new Random();
  private final AtomicLong insertsThisCycle = new AtomicLong();
  private final AtomicLong putsThisCycle    = new AtomicLong();
  private final AtomicLong updatesThisCycle = new AtomicLong();
  private final AtomicLong getsThisCycle    = new AtomicLong();
  private final AtomicLong deletesThisCycle = new AtomicLong();
  private long             totalInserts     = 0;
  private long             totalPuts        = 0;
  private long             totalUpdates     = 0;
  private long             totalGets        = 0;
  private long             totalDeletes     = 0;

  public void insert(PersistenceTransaction tx) throws TCDatabaseException, IOException {
    insertsThisCycle.incrementAndGet();
    insertInternal(tx);
  }

  public void update(PersistenceTransaction tx) throws TCDatabaseException, IOException {
    updatesThisCycle.incrementAndGet();
    updateInternal(tx);
  }

  public void delete(PersistenceTransaction tx) throws TCDatabaseException, IOException {
    deletesThisCycle.incrementAndGet();
    deleteInternal(tx);
  }

  public void get(PersistenceTransaction tx) throws TCDatabaseException, IOException {
    getsThisCycle.incrementAndGet();
    getInternal(tx);
  }

  protected abstract void insertInternal(PersistenceTransaction tx) throws TCDatabaseException, IOException;

  protected abstract void updateInternal(PersistenceTransaction tx) throws TCDatabaseException, IOException;

  protected abstract void putInternal(PersistenceTransaction tx) throws TCDatabaseException, IOException;

  protected abstract void deleteInternal(PersistenceTransaction tx) throws TCDatabaseException, IOException;

  protected abstract void getInternal(PersistenceTransaction tx) throws TCDatabaseException, IOException;

  protected long mixLong(long key) {
    // 64-bit mix function found from http://www.concentric.net/~ttwang/tech/inthash.htm
    key = (~key) + (key << 21); // key = (key << 21) - key - 1;
    key = key ^ (key >>> 24);
    key = (key + (key << 3)) + (key << 8); // key * 265
    key = key ^ (key >>> 14);
    key = (key + (key << 2)) + (key << 4); // key * 21
    key = key ^ (key >>> 28);
    key = key + (key << 31);
    return key;
  }

  public long getObjectsCreated() {
    return objectsCreated.get();
  }

  public long getObjectsDeleted() {
    return objectsDeleted.get();
  }

  public long getNumberOfObjects() {
    return getObjectsCreated() - getObjectsDeleted();
  }

  protected long nextExistentObjectId() {
    long created = objectsCreated.get();
    long deleted = objectsDeleted.get();
    return mixLong(deleted + Math.abs(random.nextLong()) % (created - deleted));
  }

  protected long nextNewObjectId() {
    return mixLong(objectsCreated.incrementAndGet() - 1);
  }

  protected long nextOldObjectId() {
    return mixLong(objectsDeleted.incrementAndGet() - 1);
  }

  protected byte[] newValue() {
    return new byte[VALUE_SIZE];
  }

  protected Object keyWithLong(long l) {
    return Conversion.long2Bytes(l);
  }

  public void printCycleReport(String prefix, int timePeriod) {
    long inserts = insertsThisCycle.getAndSet(0);
    if (inserts != 0) {
      totalInserts += inserts;
      System.out.println(prefix + " inserts/s " + ((double) inserts) / timePeriod);
    }
    long updates = updatesThisCycle.getAndSet(0);
    if (updates != 0) {
      totalUpdates += updates;
      System.out.println(prefix + " updates/s " + ((double) updates) / timePeriod);
    }
    long puts = putsThisCycle.getAndSet(0);
    if (puts != 0) {
      totalPuts += puts;
      System.out.println(prefix + " puts/s " + ((double) puts) / timePeriod);
    }
    long deletes = deletesThisCycle.getAndSet(0);
    if (deletes != 0) {
      totalDeletes += deletes;
      System.out.println(prefix + " deletes/s " + ((double) deletes) / timePeriod);
    }
    long gets = getsThisCycle.getAndSet(0);
    if (gets != 0) {
      totalGets += gets;
      System.out.println(prefix + " gets/s " + ((double) gets) / timePeriod);
    }
  }

  public void printTotalReport(String prefix, int timePeriod) {
    System.out.println(prefix + " inserts/s " + ((double) totalInserts) / timePeriod);
    System.out.println(prefix + " updates/s " + ((double) totalUpdates) / timePeriod);
    System.out.println(prefix + " puts/s " + ((double) totalPuts) / timePeriod);
    System.out.println(prefix + " deletes/s " + ((double) totalDeletes) / timePeriod);
    System.out.println(prefix + " gets/s " + ((double) totalGets) / timePeriod);
  }
}
