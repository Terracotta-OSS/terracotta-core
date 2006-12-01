/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.api.TransactionPersistor;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor.SleepycatPersistorBase;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

class TransactionPersistorImpl extends SleepycatPersistorBase implements TransactionPersistor {

  private final Database                       db;
  private final CursorConfig                   cursorConfig;
  private final EntryBinding                   gtxBinding;
  private final EntryBinding                   stxIDBinding;
  private final PersistenceTransactionProvider ptp;

  public TransactionPersistorImpl(Database db, EntryBinding stxIDBinding, EntryBinding gtxBinding,
                                  PersistenceTransactionProvider ptp) {
    this.db = db;
    this.stxIDBinding = stxIDBinding;
    this.gtxBinding = gtxBinding;
    this.ptp = ptp;
    this.cursorConfig = new CursorConfig();
    this.cursorConfig.setReadCommitted(true);
  }

  public Collection loadAllGlobalTransactionDescriptors() {
    Cursor cursor = null;
    PersistenceTransaction tx = null;
    try {
      Collection rv = new HashSet();
      tx = ptp.newTransaction();
      cursor = this.db.openCursor(pt2nt(tx), cursorConfig);
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
        rv.add(gtxBinding.entryToObject(value));
      }
      cursor.close();
      tx.commit();
      return rv;
    } catch (DatabaseException e) {
      abortOnError(cursor, tx);
      throw new DBException(e);
    }
  }

  public void saveGlobalTransactionDescriptor(PersistenceTransaction tx, GlobalTransactionDescriptor gtx) {
    DatabaseEntry key = new DatabaseEntry();
    DatabaseEntry value = new DatabaseEntry();
    stxIDBinding.objectToEntry(gtx.getServerTransactionID(), key);
    gtxBinding.objectToEntry(gtx, value);
    try {
      this.db.put(pt2nt(tx), key, value);
    } catch (DatabaseException e) {
      throw new DBException(e);
    }
  }

  public void deleteAllByServerTransactionID(PersistenceTransaction tx, Collection toDelete) {
    DatabaseEntry key = new DatabaseEntry();
    for (Iterator i = toDelete.iterator(); i.hasNext();) {
      ServerTransactionID stxID = (ServerTransactionID) i.next();
      stxIDBinding.objectToEntry(stxID, key);
      try {
        db.delete(pt2nt(tx), key);
      } catch (DatabaseException e) {
        throw new DBException(e);
      }
    }
  }

}