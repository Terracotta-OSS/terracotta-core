/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.TransactionPersistor;
import com.tc.objectserver.persistence.api.TransactionStore;
import com.tc.util.Assert;
import com.tc.util.sequence.Sequence;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class TransactionStoreImpl implements TransactionStore {

  private static final TCLogger      logger                 = TCLogging.getLogger(TransactionStoreImpl.class);

  private final Map                  serverTransactionIDMap = new HashMap();
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

  public synchronized void commitAllTransactionDescriptor(PersistenceTransaction persistenceTransaction,
                                                          Collection stxIDs) {
    for (Iterator i = stxIDs.iterator(); i.hasNext();) {
      ServerTransactionID stxnID = (ServerTransactionID) i.next();
      commitTransactionDescriptor(persistenceTransaction, stxnID);
    }
  }

  // TODO:: Move call to persistor outside synch block, but that might open up some raceconditions that need to be
  // handled
  public synchronized void commitTransactionDescriptor(PersistenceTransaction transaction, ServerTransactionID stxID) {
    GlobalTransactionDescriptor gtx = getTransactionDescriptor(stxID);
    Assert.assertNotNull(gtx);
    if (gtx.commitComplete()) {
      // reconsile when txn complete arrives before commit. Can happen in Passive server
      this.serverTransactionIDMap.remove(gtx.getServerTransactionID());
      ids.remove(gtx.getGlobalTransactionID());
    } else {
      persistor.saveGlobalTransactionDescriptor(transaction, gtx);
    }
  }

  public synchronized GlobalTransactionDescriptor getTransactionDescriptor(ServerTransactionID serverTransactionID) {
    return (GlobalTransactionDescriptor) this.serverTransactionIDMap.get(serverTransactionID);
  }

  public synchronized GlobalTransactionDescriptor getOrCreateTransactionDescriptor(
                                                                                   ServerTransactionID serverTransactionID) {
    GlobalTransactionDescriptor rv = (GlobalTransactionDescriptor) serverTransactionIDMap.get(serverTransactionID);
    if (rv == null) {
      rv = new GlobalTransactionDescriptor(serverTransactionID, getNextGlobalTransactionID());
      basicAdd(rv);
    }
    return rv;
  }

  private GlobalTransactionID getNextGlobalTransactionID() {
    return new GlobalTransactionID(this.globalIDSequence.next());
  }

  private void basicAdd(GlobalTransactionDescriptor gtx) {
    basicAdd(gtx, false);
  }

  private void basicAdd(GlobalTransactionDescriptor gtx, boolean allowRemapping) {
    ServerTransactionID sid = gtx.getServerTransactionID();
    GlobalTransactionID gid = gtx.getGlobalTransactionID();
    GlobalTransactionDescriptor prevDesc = (GlobalTransactionDescriptor) this.serverTransactionIDMap.put(sid, gtx);
    if (prevDesc != null) {
      if (allowRemapping) {
        // This can happen in the 3'rd Passive when the active crashes and the 2'nd passive takes over. Some
        // transactions that arrived to the 3'rd passive might not have arrived at the 2'nd passive and when the 2'nd
        // becomes active, it might assign a new GID to the same transaction previsously known to the 3'rd with a
        // different GID. It needs to reconsile. It is ok to just remove it from ids since even if it was commited, when
        // completeTxns arrive, it will be removed from the DB. If the 3'rd passive crashes before that, when it come
        // back up the DB is wiped out.
        ids.remove(prevDesc.getGlobalTransactionID());
        gtx.saveStateFrom(prevDesc);
        logger.warn("Remapped new desc " + gtx + " for the same SID. old = " + prevDesc);
      } else {
        throw new AssertionError("Adding new mapping for old txn IDs : " + gtx + " Prev desc = " + prevDesc);
      }
    }
    if (!gid.isNull()) {
      ids.put(gid, gtx);
    }
  }

  public GlobalTransactionID getLeastGlobalTransactionID() {
    synchronized (ids) {
      return (GlobalTransactionID) ((ids.isEmpty()) ? GlobalTransactionID.NULL_ID : ids.firstKey());
    }
  }

  // TODO:: Move call to persistor outside synch block, but that might open up some raceconditions that need to be
  // handled
  public synchronized void removeAllByServerTransactionID(PersistenceTransaction tx, Collection stxIDs) {
    Collection toDelete = new HashSet();
    for (Iterator i = stxIDs.iterator(); i.hasNext();) {
      ServerTransactionID stxID = (ServerTransactionID) i.next();
      GlobalTransactionDescriptor desc = (GlobalTransactionDescriptor) this.serverTransactionIDMap.remove(stxID);
      if (desc != null) {
        if (desc.complete()) {
          ids.remove(desc.getGlobalTransactionID());
          toDelete.add(stxID);
        } else {
          // reconsile, commit will remove this
          this.serverTransactionIDMap.put(stxID, desc);
        }
      }
    }
    if (!toDelete.isEmpty()) {
      persistor.deleteAllByServerTransactionID(tx, toDelete);
    }
  }

  public void shutdownClient(PersistenceTransaction tx, ChannelID client) {
    Collection stxIDs = new HashSet();
    synchronized (this) {
      for (Iterator iter = serverTransactionIDMap.keySet().iterator(); iter.hasNext();) {
        ServerTransactionID stxID = (ServerTransactionID) iter.next();
        if (stxID.getChannelID().equals(client)) {
          stxIDs.add(stxID);
        }
      }
    }
    logger.info("shutdownClient() : Removing txns from DB : " + stxIDs.size());
    removeAllByServerTransactionID(tx, stxIDs);
  }

  public void shutdownAllClientsExcept(PersistenceTransaction tx, Set cids) {
    Collection stxIDs = new HashSet();
    synchronized (this) {
      for (Iterator iter = serverTransactionIDMap.keySet().iterator(); iter.hasNext();) {
        ServerTransactionID stxID = (ServerTransactionID) iter.next();
        if (!cids.contains(stxID.getChannelID())) {
          stxIDs.add(stxID);
        }
      }
    }
    logger.info("shutdownAllClientsExcept() : Removing txns from DB : " + stxIDs.size());
    removeAllByServerTransactionID(tx, stxIDs);
  }

  // Used in Passive server
  public synchronized void createGlobalTransactionDescIfNeeded(ServerTransactionID stxnID,
                                                               GlobalTransactionID globalTransactionID) {
    GlobalTransactionDescriptor rv = new GlobalTransactionDescriptor(stxnID, globalTransactionID);
    basicAdd(rv, true);
  }

}