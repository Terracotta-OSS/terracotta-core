/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.inmemory;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.objectserver.gtx.ServerTransactionIDBookKeeper;
import com.tc.objectserver.persistence.api.TransactionPersistor;
import com.tc.objectserver.persistence.api.TransactionStore;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.util.sequence.Sequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class TransactionStoreImpl implements TransactionStore {

  private static final TCLogger               logger = TCLogging.getLogger(TransactionStoreImpl.class);

  private final ServerTransactionIDBookKeeper sids   = new ServerTransactionIDBookKeeper();
  private final SortedMap                     ids    = Collections.synchronizedSortedMap(new TreeMap());
  private final TransactionPersistor          persistor;
  private final Sequence                      globalIDSequence;

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

  public void commitAllTransactionDescriptor(PersistenceTransaction transaction, Collection stxIDs) {
    for (Iterator i = stxIDs.iterator(); i.hasNext();) {
      ServerTransactionID stxnID = (ServerTransactionID) i.next();
      GlobalTransactionDescriptor gtx = this.sids.get(stxnID);
      if (stxnID.isServerGeneratedTransaction()) {
        // XXX:: Since server Generated Transactions don't get completed ACKs, we don't persist these
        this.sids.remove(stxnID);
        this.ids.remove(gtx.getGlobalTransactionID());
      } else {
        this.persistor.saveGlobalTransactionDescriptor(transaction, gtx);
        gtx.commitComplete();
      }
    }
  }

  // used only in tests
  public void commitTransactionDescriptor(PersistenceTransaction transaction, ServerTransactionID stxID) {
    ArrayList stxIDs = new ArrayList(1);
    stxIDs.add(stxID);
    commitAllTransactionDescriptor(transaction, stxIDs);
  }

  public GlobalTransactionDescriptor getTransactionDescriptor(ServerTransactionID serverTransactionID) {
    return this.sids.get(serverTransactionID);
  }

  public GlobalTransactionDescriptor getOrCreateTransactionDescriptor(ServerTransactionID serverTransactionID) {
    GlobalTransactionDescriptor rv = this.sids.get(serverTransactionID);
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
    GlobalTransactionDescriptor prevDesc = this.sids.add(sid, gtx);
    if (prevDesc != null) {
      if (allowRemapping) {
        // This can happen in the 3'rd Passive when the active crashes and the 2'nd passive takes over. Some
        // transactions that arrived to the 3'rd passive might not have arrived at the 2'nd passive and when the 2'nd
        // becomes active, it might assign a new GID to the same transaction previsously known to the 3'rd with a
        // different GID. It needs to reconsile. It is ok to just remove it from ids since even if it was commited, when
        // completeTxns arrive, it will be removed from the DB. If the 3'rd passive crashes before that, when it come
        // back up the DB is wiped out.
        this.ids.remove(prevDesc.getGlobalTransactionID());
        gtx.saveStateFrom(prevDesc);
        logger.warn("Remapped new desc " + gtx + " for the same SID. old = " + prevDesc);
      } else {
        throw new AssertionError("Adding new mapping for old txn IDs : " + gtx + " Prev desc = " + prevDesc);
      }
    }
    if (!gid.isNull()) {
      this.ids.put(gid, gtx);
    }
  }

  public GlobalTransactionID getLeastGlobalTransactionID() {
    synchronized (this.ids) {
      return (GlobalTransactionID) ((this.ids.isEmpty()) ? GlobalTransactionID.NULL_ID : this.ids.firstKey());
    }
  }

  /**
   * This method clears the server transaction ids less than the low water mark, for that particular node.
   */
  public void clearCommitedTransactionsBelowLowWaterMark(PersistenceTransaction tx, ServerTransactionID stxIDs) {
    Collection gidDescs = this.sids.clearCommitedSidsBelowLowWaterMark(stxIDs);
    removeGlobalTransactionDescs(gidDescs, tx);
  }

  public void shutdownNode(PersistenceTransaction tx, NodeID nid) {
    Collection gidDescs = this.sids.removeAll(nid);
    logger.info("shutdownClient() : Removing txns from DB : " + gidDescs.size());
    removeGlobalTransactionDescs(gidDescs, tx);
  }

  /**
   * Global Transaction descriptors should have been deleted from sids data structure, before this call.
   */
  private void removeGlobalTransactionDescs(Collection gidDescs, PersistenceTransaction tx) {
    SortedSet<ServerTransactionID> toRemove = new TreeSet<ServerTransactionID>();
    for (Iterator i = gidDescs.iterator(); i.hasNext();) {
      GlobalTransactionDescriptor gd = (GlobalTransactionDescriptor) i.next();
      this.ids.remove(gd.getGlobalTransactionID());
      toRemove.add(gd.getServerTransactionID());
    }
    if (!gidDescs.isEmpty()) {
      this.persistor.deleteAllGlobalTransactionDescriptors(tx, toRemove);
    }
  }

  public void shutdownAllClientsExcept(PersistenceTransaction tx, Set cids) {
    Collection gidDescs = this.sids.removeAllExcept(cids);
    logger.info("shutdownAllClientsExcept() : Removing txns from DB : " + gidDescs.size());
    removeGlobalTransactionDescs(gidDescs, tx);
  }

  // Used in Passive server
  public void createGlobalTransactionDescIfNeeded(ServerTransactionID stxnID, GlobalTransactionID globalTransactionID) {
    GlobalTransactionDescriptor rv = new GlobalTransactionDescriptor(stxnID, globalTransactionID);
    basicAdd(rv, true);
  }

  // Used in Passive server
  public void clearCommitedTransactionsBelowLowWaterMark(PersistenceTransaction tx, GlobalTransactionID lowWaterMark) {
    SortedSet<ServerTransactionID> toRemove = new TreeSet<ServerTransactionID>();
    synchronized (this.ids) {
      Map lowerThanLWM = this.ids.headMap(lowWaterMark);
      for (Iterator i = lowerThanLWM.values().iterator(); i.hasNext();) {
        GlobalTransactionDescriptor gd = (GlobalTransactionDescriptor) i.next();
        if (gd.complete()) {
          i.remove();
          ServerTransactionID sid = gd.getServerTransactionID();
          this.sids.remove(sid);
          toRemove.add(sid);
        }
      }
    }
    if (!toRemove.isEmpty()) {
      this.persistor.deleteAllGlobalTransactionDescriptors(tx, toRemove);
    }
  }
}