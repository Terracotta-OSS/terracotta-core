/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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
import com.tc.objectserver.persistence.api.PersistentMapStore;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor.SleepycatPersistorBase;
import com.tc.util.Conversion;

public class SleepycatMapStore extends SleepycatPersistorBase implements PersistentMapStore {

  private final PersistenceTransactionProvider persistenceTransactionProvider;
  private final TCLogger                       logger;
  private final Database                       database;

  public SleepycatMapStore(PersistenceTransactionProvider persistenceTransactionProvider, TCLogger logger,
                           Database clusterStateStoreDatabase) {
    this.persistenceTransactionProvider = persistenceTransactionProvider;
    this.logger = logger;
    this.database = clusterStateStoreDatabase;
  }

  public String get(String key) {
    if(key == null) { throw new NullPointerException(); }
    
    PersistenceTransaction tx = persistenceTransactionProvider.newTransaction();
    try {
      DatabaseEntry dkey = new DatabaseEntry();
      dkey.setData(Conversion.string2Bytes(key));
      DatabaseEntry dvalue = new DatabaseEntry();
      OperationStatus status = this.database.get(pt2nt(tx), dkey, dvalue, LockMode.DEFAULT);
      tx.commit();

      if (OperationStatus.SUCCESS.equals(status)) {
        return Conversion.bytes2String(dvalue.getData());
      } else if (OperationStatus.NOTFOUND.equals(status)) {
        return null;
      } else {
        throw new DBException("Unable to retrieve value for key " + key + " in SleepycatMapStore : " + status);
      }
    } catch (Exception t) {
      abortOnError(tx);
      logger.error("Exception on get ", t);
      throw (t instanceof DBException ? (DBException) t : new DBException(t));
    }
  }

  public void put(String key, String value) {
    if(key == null || value == null) { throw new NullPointerException(); }
    
    PersistenceTransaction tx = persistenceTransactionProvider.newTransaction();
    try {
      DatabaseEntry dkey = new DatabaseEntry();
      dkey.setData(Conversion.string2Bytes(key));
      DatabaseEntry dvalue = new DatabaseEntry();
      dvalue.setData(Conversion.string2Bytes(value));
      OperationStatus status = this.database.put(pt2nt(tx), dkey, dvalue);

      if (!OperationStatus.SUCCESS.equals(status)) { throw new DBException("Unable to store value: " + value
                                                                           + " for key: " + key + "): " + status); }
      tx.commit();
    } catch (Exception t) {
      abortOnError(tx);
      logger.error("Exception on put ", t);
      throw (t instanceof DBException ? (DBException) t : new DBException(t));
    }
  }

  public boolean remove(String key) {
    if(key == null) { throw new NullPointerException(); }
    
    PersistenceTransaction tx = persistenceTransactionProvider.newTransaction();
    try {
      DatabaseEntry dkey = new DatabaseEntry();
      dkey.setData(Conversion.string2Bytes(key));
      OperationStatus status = this.database.delete(pt2nt(tx), dkey);
      tx.commit();

      if (OperationStatus.NOTFOUND.equals(status)) {
        return false;
      } else if (!OperationStatus.SUCCESS.equals(status)) { throw new DBException("Unable to remove value for key "
                                                                                  + key + " in SleepycatMapStore : "
                                                                                  + status); }
      return true;
    } catch (Exception t) {
      abortOnError(tx);
      logger.error("Exception on remove ", t);
      throw (t instanceof DBException ? (DBException) t : new DBException(t));
    }
  }

}
