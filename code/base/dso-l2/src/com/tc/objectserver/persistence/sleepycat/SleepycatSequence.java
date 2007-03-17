/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.logging.TCLogger;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.api.PersistentSequence;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor.SleepycatPersistorBase;
import com.tc.util.Conversion;
import com.tc.util.UUID;

class SleepycatSequence extends SleepycatPersistorBase implements PersistentSequence {
  private static final String                  UID_KEY = "UIDKEY-3475674589230";
  private final long                           startValue;
  private final DatabaseEntry                  key;
  private final Database                       sequenceDB;
  private final PersistenceTransactionProvider ptxp;
  private final String                         uid;

  SleepycatSequence(PersistenceTransactionProvider ptxp, TCLogger logger, long sequenceID, long startValue,
                    Database sequenceDB) {
    this.ptxp = ptxp;
    this.startValue = startValue;
    this.sequenceDB = sequenceDB;
    key = new DatabaseEntry();
    key.setData(Conversion.long2Bytes(sequenceID));
    this.uid = getOrCreateUID();
  }

  private String getOrCreateUID() {
    PersistenceTransaction tx = ptxp.newTransaction();
    String newuid;
    try {
      DatabaseEntry ukey = new DatabaseEntry();
      ukey.setData(Conversion.string2Bytes(UID_KEY));
      DatabaseEntry value = new DatabaseEntry();
      OperationStatus status = this.sequenceDB.get(pt2nt(tx), ukey, value, LockMode.DEFAULT);

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

  public synchronized long next() {
    return nextBatch(1);
  }

  public synchronized long nextBatch(int batchSize) {
    if (batchSize < 1) throw new AssertionError("Can't increment by a value less than 1.");
    PersistenceTransaction tx = ptxp.newTransaction();
    try {
      DatabaseEntry value = new DatabaseEntry();
      long currentValue = startValue;
      OperationStatus status = this.sequenceDB.get(pt2nt(tx), key, value, LockMode.DEFAULT);

      if (OperationStatus.SUCCESS.equals(status)) {
        currentValue = Conversion.bytes2Long(value.getData());
      } else if (!OperationStatus.NOTFOUND.equals(status)) {
        // Formatting
        throw new DBException("Unable to retrieve current value: " + status);
      }

      value.setData(Conversion.long2Bytes(currentValue + batchSize));
      status = this.sequenceDB.put(pt2nt(tx), key, value);
      if (!OperationStatus.SUCCESS.equals(status)) { throw new DBException("Unable to store current value: "
                                                                           + (currentValue + batchSize) + "): "
                                                                           + status); }
      tx.commit();
      return currentValue;
    } catch (Exception t) {
      abortOnError(tx);
      t.printStackTrace();
      throw (t instanceof DBException ? (DBException) t : new DBException(t));
    }
  }

  public String getUID() {
    return uid;
  }

  public void setNext(long next) {
    PersistenceTransaction tx = ptxp.newTransaction();
    try {
      DatabaseEntry value = new DatabaseEntry();
      long currentValue = startValue;
      OperationStatus status = this.sequenceDB.get(pt2nt(tx), key, value, LockMode.DEFAULT);

      if (OperationStatus.SUCCESS.equals(status)) {
        currentValue = Conversion.bytes2Long(value.getData());
      } else if (!OperationStatus.NOTFOUND.equals(status)) {
        // Formatting
        throw new DBException("Unable to retrieve current value: " + status);
      }

      if (currentValue > next) {
        abortOnError(tx);
        throw new AssertionError("Cant setNext sequence to a value less than current: current = " + currentValue
                                 + " next = " + next);
      }

      value.setData(Conversion.long2Bytes(next));
      status = this.sequenceDB.put(pt2nt(tx), key, value);
      if (!OperationStatus.SUCCESS.equals(status)) { throw new DBException("Unable to store next value: " + (next)
                                                                           + "): " + status); }
      tx.commit();
    } catch (Exception t) {
      abortOnError(tx);
      t.printStackTrace();
      throw (t instanceof DBException ? (DBException) t : new DBException(t));
    }
  }
}