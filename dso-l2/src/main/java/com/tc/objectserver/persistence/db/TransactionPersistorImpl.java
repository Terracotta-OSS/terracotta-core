/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.objectserver.persistence.api.TransactionPersistor;
import com.tc.objectserver.persistence.db.DBPersistorImpl.DBPersistorBase;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.storage.api.TCDatabaseCursor;
import com.tc.objectserver.storage.api.TCDatabaseEntry;
import com.tc.objectserver.storage.api.TCTransactionStoreDatabase;

import java.util.Collection;
import java.util.HashSet;
import java.util.SortedSet;

class TransactionPersistorImpl extends DBPersistorBase implements TransactionPersistor {

  private final TCTransactionStoreDatabase     db;
  private final PersistenceTransactionProvider ptp;

  public TransactionPersistorImpl(TCTransactionStoreDatabase db, PersistenceTransactionProvider ptp) {
    this.db = db;
    this.ptp = ptp;
  }

  public Collection loadAllGlobalTransactionDescriptors() {
    TCDatabaseCursor<Long, byte[]> cursor = null;
    PersistenceTransaction tx = null;
    try {
      Collection rv = new HashSet();
      tx = ptp.newTransaction();
      cursor = this.db.openCursor(tx);
      while (cursor.hasNext()) {
        TCDatabaseEntry<Long, byte[]> entry = cursor.next();
        rv.add(new GlobalTransactionDescriptor(bytes2ServerTxnID(entry.getValue()), new GlobalTransactionID(entry
            .getKey())));
      }
      cursor.close();
      tx.commit();
      return rv;
    } catch (Exception e) {
      abortOnError(cursor, tx);
      throw new DBException(e);
    }
  }

  public void saveGlobalTransactionDescriptor(PersistenceTransaction tx, GlobalTransactionDescriptor gtx) {
    byte[] value = serverTxnID2Bytes(gtx.getServerTransactionID());
    try {
      this.db.insert(gtx.getGlobalTransactionID().toLong(), value, tx);
    } catch (Exception e) {
      throw new DBException(e);
    }
  }

  private byte[] serverTxnID2Bytes(ServerTransactionID serverTransactionID) {
    return serverTransactionID.getBytes();
  }

  private ServerTransactionID bytes2ServerTxnID(byte[] data) {
    return ServerTransactionID.createFrom(data);
  }

  public void deleteAllGlobalTransactionDescriptors(PersistenceTransaction tx,
                                                    SortedSet<GlobalTransactionID> globalTransactionIDs) {
    for (GlobalTransactionID globalTransactionID : globalTransactionIDs) {
      try {
        db.delete(globalTransactionID.toLong(), tx);
      } catch (Exception e) {
        throw new DBException(e);
      }
    }
  }
}