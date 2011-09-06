/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.logging.TCLogger;
import com.tc.object.persistence.api.PersistentMapStore;
import com.tc.objectserver.persistence.db.DBPersistorImpl.DBPersistorBase;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.storage.api.TCDatabaseEntry;
import com.tc.objectserver.storage.api.TCStringToStringDatabase;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;

public class TCMapStore extends DBPersistorBase implements PersistentMapStore {

  private final PersistenceTransactionProvider persistenceTransactionProvider;
  private final TCLogger                       logger;
  private final TCStringToStringDatabase       database;

  public TCMapStore(PersistenceTransactionProvider persistenceTransactionProvider, TCLogger logger,
                           TCStringToStringDatabase clusterStateStoreDatabase) {
    this.persistenceTransactionProvider = persistenceTransactionProvider;
    this.logger = logger;
    this.database = clusterStateStoreDatabase;
  }

  public String get(String key) {
    if (key == null) { throw new NullPointerException(); }

    PersistenceTransaction tx = persistenceTransactionProvider.newTransaction();
    try {
      TCDatabaseEntry<String, String> entry = new TCDatabaseEntry<String, String>();
      entry.setKey(key);
      Status status = this.database.get(entry, tx);
      tx.commit();

      if (Status.SUCCESS.equals(status)) {
        return entry.getValue();
      } else if (Status.NOT_FOUND.equals(status)) {
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
    if (key == null || value == null) { throw new NullPointerException(); }

    PersistenceTransaction tx = persistenceTransactionProvider.newTransaction();
    try {
      Status status = this.database.put(key, value, tx);

      if (status != Status.SUCCESS) { throw new DBException("Unable to store value: " + value + " for key: " + key
                                                            + "): " + status); }
      tx.commit();
    } catch (Exception t) {
      abortOnError(tx);
      logger.error("Exception on put ", t);
      throw (t instanceof DBException ? (DBException) t : new DBException(t));
    }
  }

  public boolean remove(String key) {
    if (key == null) { throw new NullPointerException(); }

    PersistenceTransaction tx = persistenceTransactionProvider.newTransaction();
    try {
      Status status = this.database.delete(key, tx);
      tx.commit();

      if (Status.NOT_FOUND.equals(status)) {
        return false;
      } else if (!Status.SUCCESS.equals(status)) { throw new DBException("Unable to remove value for key " + key
                                                                         + " in SleepycatMapStore : " + status); }
      return true;
    } catch (Exception t) {
      abortOnError(tx);
      logger.error("Exception on remove ", t);
      throw (t instanceof DBException ? (DBException) t : new DBException(t));
    }
  }

}
