/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;
import com.tc.logging.TCLogger;
import com.tc.objectserver.persistence.api.ClassPersistor;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor.SleepycatPersistorBase;
import com.tc.util.Conversion;

import java.util.HashMap;
import java.util.Map;

class ClassPersistorImpl extends SleepycatPersistorBase implements ClassPersistor {
  private final Database                       classDB;
  private final PersistenceTransactionProvider ptxp;
  private final CursorConfig                   cursorConfig;

  ClassPersistorImpl(PersistenceTransactionProvider ptxp, TCLogger logger, Database classDB) {
    this.ptxp = ptxp;
    this.classDB = classDB;
    this.cursorConfig = new CursorConfig();
    this.cursorConfig.setReadCommitted(true);
  }

  public void storeClass(int clazzId, byte[] clazzBytes) {
    Transaction tx = null;
    try {
      tx = pt2nt(ptxp.newTransaction());
      DatabaseEntry key = new DatabaseEntry();
      key.setData(Conversion.int2Bytes(clazzId));
      DatabaseEntry value = new DatabaseEntry();
      value.setData(clazzBytes);
      OperationStatus status = this.classDB.put(tx, key, value);
      tx.commit();

      if (!OperationStatus.SUCCESS.equals(status)) {
        // Formatting
        throw new DBException("Unable to store class Bytes: " + clazzId);
      }
    } catch (DatabaseException t) {
      abortOnError(tx);
      t.printStackTrace();
      throw new DBException(t);
    }
  }

  public byte[] retrieveClass(int clazzId) {
    Transaction tx = null;
    try {
      tx = pt2nt(ptxp.newTransaction());
      DatabaseEntry key = new DatabaseEntry();
      key.setData(Conversion.int2Bytes(clazzId));
      DatabaseEntry value = new DatabaseEntry();
      OperationStatus status = this.classDB.get(tx, key, value, LockMode.DEFAULT);
      tx.commit();
      if (!OperationStatus.SUCCESS.equals(status)) {
        // Formatting
        throw new DBException("Unable to retrieve class Bytes: " + clazzId);
      }
      return value.getData();
    } catch (DatabaseException t) {
      abortOnError(tx);
      t.printStackTrace();
      throw new DBException(t);
    }
  }

  public Map retrieveAllClasses() {
    Map allClazzBytes = new HashMap();
    Cursor cursor = null;
    Transaction tx = null;
    try {
      tx = pt2nt(ptxp.newTransaction());
      cursor = classDB.openCursor(tx, cursorConfig);
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
        allClazzBytes.put(new Integer(Conversion.bytes2Int(key.getData())), value.getData());
      }
      cursor.close();
      tx.commit();
    } catch (DatabaseException e) {
      abortOnError(cursor, tx);
      e.printStackTrace();
      throw new DBException(e);
    }
    return allClazzBytes;
  }
}