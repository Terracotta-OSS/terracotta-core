/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.storage.berkeleydb;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Sequence;
import com.sleepycat.je.SequenceConfig;
import com.sleepycat.je.SequenceStats;
import com.sleepycat.je.StatsConfig;
import com.tc.logging.TCLogger;
import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.util.Conversion;
import com.tc.util.UUID;
import com.tc.util.sequence.MutableSequence;

class BerkeleyDBSequence extends AbstractBerkeleyDatabase implements MutableSequence {
  private static final String UID_KEY = "UIDKEY-3475674589230";
  private final String        uid;
  private final Sequence      sequence;

  BerkeleyDBSequence(PersistenceTransactionProvider ptxp, TCLogger logger, String sequenceID, int startValue,
                    Database sequenceDB) {
    super(sequenceDB);
    DatabaseEntry key = new DatabaseEntry();
    key.setData(Conversion.string2Bytes(sequenceID));
    this.uid = getOrCreateUID(sequenceID, ptxp);

    if (startValue < 0) throw new IllegalArgumentException("start value cannot be < 0");
    this.sequence = openSequence(startValue, key);
  }

  private Sequence openSequence(int startVal, DatabaseEntry key) {
    Sequence seq = null;
    SequenceConfig config = new SequenceConfig();
    config.setAllowCreate(true);
    config.setCacheSize(0);
    try {
      seq = db.openSequence(null, key, config);
      long currentVal = currentValueOfSequence(seq);
      if (currentVal < startVal) {
        setNextForSequnce(startVal, seq);
      }
    } catch (Exception t) {
      t.printStackTrace();
      throw (t instanceof DBException ? (DBException) t : new DBException(t));
    }
    return seq;
  }

  private String getOrCreateUID(String sequenceID, PersistenceTransactionProvider ptxp) {
    PersistenceTransaction tx = ptxp.newTransaction();

    String newuid;
    try {
      DatabaseEntry ukey = new DatabaseEntry();
      String temp = UID_KEY + sequenceID;
      ukey.setData(Conversion.string2Bytes(temp));
      DatabaseEntry value = new DatabaseEntry();
      OperationStatus status = this.db.get(null, ukey, value, LockMode.DEFAULT);

      if (OperationStatus.SUCCESS.equals(status)) {
        newuid = Conversion.bytes2String(value.getData());
      } else if (OperationStatus.NOTFOUND.equals(status)) {
        newuid = createUID();
        value.setData(Conversion.string2Bytes(newuid));
        status = this.db.put(pt2nt(tx), ukey, value);
        if (!OperationStatus.SUCCESS.equals(status)) { throw new DBException(
                                                                             "Unable to store UID for SleepycatSequence: "
                                                                                 + newuid + "): " + status); }
      } else {
        throw new DBException("Unable to retrieve UID for SleepycatSequence: " + status);
      }
      tx.commit();
      return newuid;
    } catch (Exception t) {
      tx.abort();
      t.printStackTrace();
      throw (t instanceof DBException ? (DBException) t : new DBException(t));
    }
  }

  private String createUID() {
    UUID uuid = UUID.getUUID();
    return uuid.toString();
  }

  public synchronized long current() {
    return currentValueOfSequence(sequence);
  }

  private synchronized long currentValueOfSequence(Sequence seq) {
    SequenceStats stats;
    try {
      stats = seq.getStats(StatsConfig.DEFAULT);
    } catch (Exception t) {
      t.printStackTrace();
      throw (t instanceof DBException ? (DBException) t : new DBException(t));
    }
    return stats.getCurrent();
  }

  public synchronized long next() {
    return nextBatch(1);
  }

  public synchronized long nextBatch(long batchSize) {
    return nextBatchForSequence(batchSize, sequence);
  }

  private synchronized long nextBatchForSequence(long batchSize, Sequence seq) {
    if (batchSize == 0) {
      return current();
    } else if (batchSize < 1) { throw new AssertionError("Can't increment by a value less than 1."); }
    try {
      if (batchSize > Integer.MAX_VALUE) {
        long sequenceNo = seq.get(null, Integer.MAX_VALUE);
        batchSize = batchSize - Integer.MAX_VALUE;
        while (batchSize > 0) {
          int size = batchSize > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) batchSize;
          seq.get(null, size);
          batchSize = batchSize - Integer.MAX_VALUE;
        }
        return sequenceNo;
      }
      long sequenceNo = seq.get(null, (int) batchSize);
      return sequenceNo;
    } catch (Exception t) {
      t.printStackTrace();
      throw (t instanceof DBException ? (DBException) t : new DBException(t));
    }
  }

  public String getUID() {
    return uid;
  }

  public synchronized void setNext(long next) {
    setNextForSequnce(next, sequence);
  }

  private void setNextForSequnce(long next, Sequence seq) {
    long currentValue = currentValueOfSequence(seq);
    if (currentValue > next) { throw new AssertionError(
                                                        "Cant setNext sequence to a value less than current: current = "
                                                            + currentValue + " next = " + next); }

    nextBatchForSequence(next - currentValue, seq);
  }
}