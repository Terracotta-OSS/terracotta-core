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
import com.tc.objectserver.storage.api.TCBytesToBytesDatabase;
import com.tc.objectserver.storage.api.TCDatabaseCursor;
import com.tc.objectserver.storage.api.TCDatabaseEntry;
import com.tc.util.Conversion;

import java.util.Collection;
import java.util.HashSet;
import java.util.SortedSet;

class TransactionPersistorImpl extends DBPersistorBase implements TransactionPersistor {

  private final TCBytesToBytesDatabase         db;
  private final PersistenceTransactionProvider ptp;

  public TransactionPersistorImpl(TCBytesToBytesDatabase db, PersistenceTransactionProvider ptp) {
    this.db = db;
    this.ptp = ptp;
  }

  public Collection loadAllGlobalTransactionDescriptors() {
    TCDatabaseCursor<byte[], byte[]> cursor = null;
    PersistenceTransaction tx = null;
    try {
      Collection rv = new HashSet();
      tx = ptp.newTransaction();
      cursor = this.db.openCursor(tx);
      while (cursor.hasNext()) {
        TCDatabaseEntry<byte[], byte[]> entry = cursor.next();
        rv.add(new GlobalTransactionDescriptor(bytes2ServerTxnID(entry.getKey()), bytes2GlobalTxnID(entry.getValue())));
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
    byte[] key = serverTxnID2Bytes(gtx.getServerTransactionID());
    byte[] value = globalTxnID2Bytes(gtx.getGlobalTransactionID());
    try {
      this.db.insert(key, value, tx);
    } catch (Exception e) {
      throw new DBException(e);
    }
  }

  private GlobalTransactionID bytes2GlobalTxnID(byte[] data) {
    return new GlobalTransactionID(Conversion.bytes2Long(data));
  }

  private byte[] globalTxnID2Bytes(GlobalTransactionID globalTransactionID) {
    return Conversion.long2Bytes(globalTransactionID.toLong());
  }

  private byte[] serverTxnID2Bytes(ServerTransactionID serverTransactionID) {
    return serverTransactionID.getBytes();
  }

  private ServerTransactionID bytes2ServerTxnID(byte[] data) {
    return ServerTransactionID.createFrom(data);
  }

  public void deleteAllGlobalTransactionDescriptors(PersistenceTransaction tx,
                                                    SortedSet<ServerTransactionID> serverTxnIDs) {
    for (Object element : serverTxnIDs) {
      ServerTransactionID stxID = (ServerTransactionID) element;
      try {
        db.delete(serverTxnID2Bytes(stxID), tx);
      } catch (Exception e) {
        throw new DBException(e);
      }
    }
  }
}