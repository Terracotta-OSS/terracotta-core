/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.objectserver.gtx.TransactionCommittedError;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.TransactionPersistor;
import com.tc.objectserver.persistence.api.TransactionStore;
import com.tc.util.sequence.Sequence;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class TransactionStoreImpl implements TransactionStore {

  private final Map                  serverTransactionIDMap = Collections.synchronizedMap(new HashMap());
  private final SortedMap            ids                    = Collections
                                                                .synchronizedSortedMap(new TreeMap(
                                                                                                   GlobalTransactionID.COMPARATOR));
  private final TransactionPersistor persistor;
  private final Sequence             globalIDSequence;

  public TransactionStoreImpl(TransactionPersistor persistor, Sequence globalIDSequence) {
    this.persistor = persistor;
    this.globalIDSequence = globalIDSequence;
    // We don't want to hit the DB (globalIDsequence) until all the stages are started.
    for (Iterator i = this.persistor.loadAllGlobalTransactionDescriptors().iterator(); i.hasNext();) {
      GlobalTransactionDescriptor gtx = (GlobalTransactionDescriptor) i.next();
      basicAdd(gtx);
      gtx.commitComplete();
    }
  }

  public void commitTransactionDescriptor(PersistenceTransaction transaction, GlobalTransactionDescriptor gtx) {
    if (gtx.isCommitted()) { throw new TransactionCommittedError("Already committed : " + gtx); }
    persistor.saveGlobalTransactionDescriptor(transaction, gtx);
    gtx.commitComplete();
  }

  public GlobalTransactionDescriptor getTransactionDescriptor(ServerTransactionID serverTransactionID) {
    return (GlobalTransactionDescriptor) this.serverTransactionIDMap.get(serverTransactionID);
  }

  public GlobalTransactionDescriptor getOrCreateTransactionDescriptor(ServerTransactionID serverTransactionID) {
    synchronized (serverTransactionIDMap) {
      GlobalTransactionDescriptor rv = (GlobalTransactionDescriptor) serverTransactionIDMap.get(serverTransactionID);
      if (rv == null) {
        rv = new GlobalTransactionDescriptor(serverTransactionID, getNextGlobalTransactionID());
        basicAdd(rv);
      }
      return rv;
    }
  }

  private GlobalTransactionID getNextGlobalTransactionID() {
    return new GlobalTransactionID(this.globalIDSequence.next());
  }

  private void basicAdd(GlobalTransactionDescriptor gtx) {
    ServerTransactionID sid = gtx.getServerTransactionID();
    GlobalTransactionID gid = gtx.getGlobalTransactionID();
    Object prevDesc = this.serverTransactionIDMap.put(sid, gtx);
    ids.put(gid, gtx);
    if (prevDesc != null) { throw new AssertionError("Adding new mapping for old txn IDs : " + gtx + " Prev desc = "
                                                     + prevDesc); }
  }

  public GlobalTransactionID getLeastGlobalTransactionID() {
    synchronized (ids) {
      return (GlobalTransactionID) ((ids.isEmpty()) ? GlobalTransactionID.NULL_ID : ids.firstKey());
    }
  }

  public void removeAllByServerTransactionID(PersistenceTransaction tx, Collection stxIDs) {
    Collection toDelete = new HashSet();
    for (Iterator i = stxIDs.iterator(); i.hasNext();) {
      ServerTransactionID stxID = (ServerTransactionID) i.next();
      GlobalTransactionDescriptor desc = (GlobalTransactionDescriptor) this.serverTransactionIDMap.remove(stxID);
      if (desc != null) {
        ids.remove(desc.getGlobalTransactionID());
        toDelete.add(stxID);
      }
    }
    if (!toDelete.isEmpty()) {
      persistor.deleteAllByServerTransactionID(tx, toDelete);
    }
  }

  public GlobalTransactionID getGlobalTransactionID(ServerTransactionID stxnID) {
    GlobalTransactionDescriptor gdesc = (GlobalTransactionDescriptor) serverTransactionIDMap.get(stxnID);
    if (gdesc == null) {
      return GlobalTransactionID.NULL_ID;
    } else {
      return gdesc.getGlobalTransactionID();
    }
  }

  public void shutdownClient(PersistenceTransaction tx, ChannelID client) {
    Collection stxIDs = new HashSet();
    synchronized (serverTransactionIDMap) {
      for (Iterator iter = serverTransactionIDMap.keySet().iterator(); iter.hasNext();) {
        ServerTransactionID stxID = (ServerTransactionID) iter.next();
        if (stxID.getChannelID().equals(client)) {
          stxIDs.add(stxID);
        }
      }
    }
    removeAllByServerTransactionID(tx, stxIDs);
  }

  public void createGlobalTransactionDesc(ServerTransactionID stxnID, GlobalTransactionID globalTransactionID) {
    GlobalTransactionDescriptor rv = new GlobalTransactionDescriptor(stxnID, globalTransactionID);
    basicAdd(rv);
  }

}