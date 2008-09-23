/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Sequence;
import com.sleepycat.je.SequenceConfig;
import com.sleepycat.je.SequenceStats;
import com.sleepycat.je.StatsConfig;
import com.tc.logging.TCLogger;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor.SleepycatPersistorBase;
import com.tc.util.Conversion;
import com.tc.util.UUID;
import com.tc.util.sequence.MutableSequence;

class SleepycatSequence extends SleepycatPersistorBase implements MutableSequence {
  private static final String UID_KEY = "UIDKEY-3475674589230";
  private final String        uid;
  private final Database      sequenceDB;
  private final Sequence      sequence;

  SleepycatSequence(PersistenceTransactionProvider ptxp, TCLogger logger, String sequenceID, int startValue,
                    Database sequenceDB) {
    DatabaseEntry key = new DatabaseEntry();
    key.setData(Conversion.string2Bytes(sequenceID));
    this.sequenceDB = sequenceDB;
    this.uid = getOrCreateUID(sequenceID, ptxp);

    if (startValue < 0) throw new IllegalArgumentException("start value cannot be < 0");
    this.sequence = openSequence(startValue, key);
  }

  private Sequence openSequence(int startVal, DatabaseEntry key) {
    Sequence seq = null;
    SequenceConfig config = new SequenceConfig();
    config.setAllowCreate(true);
    try {
      seq = sequenceDB.openSequence(null, key, config);
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
      OperationStatus status = this.sequenceDB.get(null, ukey, value, LockMode.DEFAULT);

      if (OperationStatus.SUCCESS.equals(status)) {
        newuid = Conversion.bytes2String(value.getData());
      } else if (OperationStatus.NOTFOUND.equals(status)) {
        newuid = createUID();
        value.setData(Conversion.string2Bytes(newuid));
        status = this.sequenceDB.put(pt2nt(tx), ukey, value);
        if (!OperationStatus.SUCCESS.equals(status)) { throw new DBException(
                                                                             "Unable to store UID for SleepycatSequence: "
                                                                                 + newuid + "): " + status); }
      } else {
        throw new DBException("Unable to retrieve UID for SleepycatSequence: " + status);
      }
      tx.commit();
      return newuid;
    } catch (Exception t) {
      abortOnError(tx);
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

  public synchronized long nextBatch(int batchSize) {
    return nextBatchForSequence(batchSize, sequence);
  }

  private synchronized long nextBatchForSequence(int batchSize, Sequence seq) {
    if (batchSize < 1) throw new AssertionError("Can't increment by a value less than 1.");
    try {
      long sequenceNo = seq.get(null, batchSize);
      return sequenceNo;
    } catch (Exception t) {
      t.printStackTrace();
      throw (t instanceof DBException ? (DBException) t : new DBException(t));
    }
  }

  public String getUID() {
    return uid;
  }

  public void setNext(long next) {
    setNextForSequnce(next, sequence);
  }

  private void setNextForSequnce(long next, Sequence seq) {
    long currentValue = currentValueOfSequence(seq);
    if (currentValue > next) { throw new AssertionError(
                                                        "Cant setNext sequence to a value less than current: current = "
                                                            + currentValue + " next = " + next); }

    int batchSize = (int) (next - currentValue);
    nextBatchForSequence(batchSize, seq);
  }
}