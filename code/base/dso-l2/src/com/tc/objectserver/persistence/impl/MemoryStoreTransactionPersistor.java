/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import com.tc.memorydatastore.client.MemoryDataStoreClient;
import com.tc.memorydatastore.message.TCByteArrayKeyValuePair;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.TransactionPersistor;
import com.tc.util.Conversion;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

class MemoryStoreTransactionPersistor implements TransactionPersistor {

  private final MemoryDataStoreClient db;

  public MemoryStoreTransactionPersistor(MemoryDataStoreClient db) {
    this.db = db;
  }

  public Collection loadAllGlobalTransactionDescriptors() {
    Collection rv = new HashSet();
    Collection txns = db.getAll();

    for (Iterator i = txns.iterator(); i.hasNext();) {
      TCByteArrayKeyValuePair pair = (TCByteArrayKeyValuePair) i.next();
      rv.add(new GlobalTransactionDescriptor(bytes2ServerTxnID(pair.getKey()), bytes2GlobalTxnID(pair.getValue())));
    }
    return rv;
  }

  public void saveGlobalTransactionDescriptor(PersistenceTransaction tx, GlobalTransactionDescriptor gtx) {
    this.db.put(serverTxnID2Bytes(gtx.getServerTransactionID()), globalTxnID2Bytes(gtx.getGlobalTransactionID()));
  }

  private GlobalTransactionID bytes2GlobalTxnID(byte[] data) {
    return new GlobalTransactionID(Conversion.bytes2Long(data));
  }

  private byte[] globalTxnID2Bytes(GlobalTransactionID globalTransactionID) {
    return Conversion.long2Bytes(globalTransactionID.toLong());
  }

  private byte[] serverTxnID2Bytes(ServerTransactionID serverTransactionID) {
    byte b[] = new byte[16];
    Conversion.writeLong(serverTransactionID.getChannelID().toLong(), b, 0);
    Conversion.writeLong(serverTransactionID.getClientTransactionID().toLong(), b, 8);
    return b;
  }

  private ServerTransactionID bytes2ServerTxnID(byte[] data) {
    return new ServerTransactionID(new ChannelID(Conversion.bytes2Long(data, 0)), new TransactionID(Conversion
        .bytes2Long(data, 8)));
  }

  public void deleteAllByServerTransactionID(PersistenceTransaction tx, Collection toDelete) {
    for (Iterator i = toDelete.iterator(); i.hasNext();) {
      ServerTransactionID stxID = (ServerTransactionID) i.next();
      db.remove(serverTxnID2Bytes(stxID));
    }
  }
}