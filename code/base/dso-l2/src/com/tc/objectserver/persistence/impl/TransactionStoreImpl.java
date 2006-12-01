/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
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
import java.util.SortedSet;
import java.util.TreeSet;

public class TransactionStoreImpl implements TransactionStore {

  private final Map                  serverTransactionIDMap = Collections.synchronizedMap(new HashMap());
  private final Map                  globalTransactionIDMap = Collections.synchronizedMap(new HashMap());

  private final SortedSet            ids                    = Collections
                                                                .synchronizedSortedSet(new TreeSet(
                                                                                                   GlobalTransactionID.COMPARATOR));
  private final TransactionPersistor persistor;
  private final Sequence             globalIDSequence;

  public TransactionStoreImpl(TransactionPersistor persistor, Sequence globalIDSequence) {
    this.persistor = persistor;
    this.globalIDSequence = globalIDSequence;
    // Ok, this is relying on the fact that GlobalTransactionIDs are never given out negative,
    // except for NULL_ID which is -1
    // We don't want to hit the DB (globalIDsequence) until all the stages are started.
    int negativeID = -2;
    for (Iterator i = this.persistor.loadAllGlobalTransactionDescriptors().iterator(); i.hasNext();) {
      GlobalTransactionDescriptor gtx = (GlobalTransactionDescriptor) i.next();
      ServerTransactionID stxID = gtx.getServerTransactionID();
      basicAdd(stxID, gtx);
      // this will be reassigned later when the clients resend the transaction
      addGlobalTransactionID(stxID, new GlobalTransactionID(negativeID--)); 
    }
  }

  private GlobalTransactionID addGlobalTransactionID(ServerTransactionID serverTransactionID) {
    return addGlobalTransactionID(serverTransactionID, new GlobalTransactionID(globalIDSequence.next()));
  }

  private GlobalTransactionID addGlobalTransactionID(ServerTransactionID serverTransactionID, GlobalTransactionID gid) {
    Object previousID = globalTransactionIDMap.put(serverTransactionID, gid);
    if (previousID != null) {
      // GlobalTransactionID's will be remapped on server restarts
      ids.remove(previousID);
    }
    ids.add(gid);
    return gid;
  }

  public void commitTransactionDescriptor(PersistenceTransaction transaction, GlobalTransactionDescriptor gtx) {
    persistor.saveGlobalTransactionDescriptor(transaction, gtx);
  }

  public GlobalTransactionDescriptor getTransactionDescriptor(ServerTransactionID serverTransactionID) {
    return (GlobalTransactionDescriptor) this.serverTransactionIDMap.get(serverTransactionID);
  }

  public GlobalTransactionDescriptor createTransactionDescriptor(ServerTransactionID serverTransactionID) {
    GlobalTransactionDescriptor rv = new GlobalTransactionDescriptor(serverTransactionID);
    basicAdd(serverTransactionID, rv);
    return rv;
  }

  private void basicAdd(ServerTransactionID serverTransactionID, GlobalTransactionDescriptor gtx) {
    this.serverTransactionIDMap.put(serverTransactionID, gtx);
  }

  public GlobalTransactionID getLeastGlobalTransactionID() {
    synchronized (ids) {
      return (GlobalTransactionID) ((ids.isEmpty()) ? GlobalTransactionID.NULL_ID : ids.first());
    }
  }

  public void removeAllByServerTransactionID(PersistenceTransaction tx, Collection stxIDs) {
    Collection toDelete = new HashSet();
    synchronized (ids) {
      for (Iterator i = stxIDs.iterator(); i.hasNext();) {
        ServerTransactionID stxID = (ServerTransactionID) i.next();
        GlobalTransactionDescriptor desc = (GlobalTransactionDescriptor) this.serverTransactionIDMap.remove(stxID);
        GlobalTransactionID gid = (GlobalTransactionID) this.globalTransactionIDMap.remove(stxID);
        if (gid != null) {
          ids.remove(gid);
        }
        if (desc != null) {
          toDelete.add(stxID);
        }
      }
    }
    persistor.deleteAllByServerTransactionID(tx, toDelete);
  }

  public GlobalTransactionID createGlobalTransactionID(ServerTransactionID stxnID) {
    return addGlobalTransactionID(stxnID);
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
}