/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.logging.TCLogger;
import com.tc.objectserver.persistence.api.ClassPersistor;
import com.tc.objectserver.persistence.db.DBPersistorImpl.DBPersistorBase;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.storage.api.TCIntToBytesDatabase;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;

import java.util.Map;

class ClassPersistorImpl extends DBPersistorBase implements ClassPersistor {
  private final TCIntToBytesDatabase           classDB;
  private final PersistenceTransactionProvider ptxp;

  ClassPersistorImpl(PersistenceTransactionProvider ptxp, TCLogger logger, TCIntToBytesDatabase classDB) {
    this.ptxp = ptxp;
    this.classDB = classDB;
  }

  public void storeClass(int clazzId, byte[] clazzBytes) {
    PersistenceTransaction tx = ptxp.newTransaction();
    try {
      Status status = this.classDB.put(clazzId, clazzBytes, tx);

      if (status != Status.SUCCESS) {
        // Formatting
        throw new DBException("Unable to store class Bytes: " + clazzId);
      }
    } catch (Exception t) {
      t.printStackTrace();
      throw new DBException(t);
    } finally {
      tx.commit();
    }
  }

  public byte[] retrieveClass(int clazzId) {
    PersistenceTransaction tx = ptxp.newTransaction();
    try {
      byte[] val = this.classDB.get(clazzId, tx);
      tx.commit();
      if (val == null) {
        // Formatting
        throw new DBException("Unable to retrieve class Bytes: " + clazzId);
      }
      return val;
    } catch (Exception t) {
      t.printStackTrace();
      throw new DBException(t);
    } finally {
      tx.commit();
    }
  }

  public Map retrieveAllClasses() {
    PersistenceTransaction tx = ptxp.newTransaction();
    try {
      return this.classDB.getAll(tx);
    } finally {
      tx.commit();
    }
  }
}