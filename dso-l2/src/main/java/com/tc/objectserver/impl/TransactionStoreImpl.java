/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.api.TransactionStore;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.objectserver.gtx.ServerTransactionIDBookKeeper;
import com.tc.objectserver.persistence.TransactionPersistor;
import com.tc.util.sequence.Sequence;

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
  private final TransactionPersistor persistor;
  private final Sequence                      globalIDSequence;

  public TransactionStoreImpl(TransactionPersistor persistor, Sequence globalIDSequence) {
    this.persistor = persistor;
    this.globalIDSequence = globalIDSequence;
    // We don't want to hit the DB (globalIDsequence) until all the stages are started.
    for (Object element : this.persistor.loadAllGlobalTransactionDescriptors()) {
      GlobalTransactionDescriptor gtx = (GlobalTransactionDescriptor) element;
      basicAdd(gtx);
      gtx.commitComplete();
    }
  }

  @Override
  public void commitTransactionDescriptor(ServerTransactionID stxID) {
    GlobalTransactionDescriptor gtx = this.sids.get(stxID);
    if (!stxID.isServerGeneratedTransaction()) {
      // XXX:: Since server Generated Transactions don't get completed ACKs, we don't persist these
      this.persistor.saveGlobalTransactionDescriptor(gtx);
    }
    gtx.commitComplete();
  }

  @Override
  public GlobalTransactionDescriptor getTransactionDescriptor(ServerTransactionID serverTransactionID) {
    return this.sids.get(serverTransactionID);
  }

  @Override
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

  @Override
  public GlobalTransactionID getLeastGlobalTransactionID() {
    synchronized (this.ids) {
      return (GlobalTransactionID) ((this.ids.isEmpty()) ? GlobalTransactionID.NULL_ID : this.ids.firstKey());
    }
  }

  /**
   * This method clears the server transaction ids less than the low water mark, for that particular node.
   */
  @Override
  public Collection<GlobalTransactionDescriptor> clearCommitedTransactionsBelowLowWaterMark(ServerTransactionID stxIDs) {
    Collection<GlobalTransactionDescriptor> removedGDs = this.sids.clearCommitedSidsBelowLowWaterMark(stxIDs);
    removeGlobalTransactionDescs(removedGDs);
    return removedGDs;
  }

  @Override
  public GlobalTransactionDescriptor clearCommittedTransaction(final ServerTransactionID serverTransactionID) {
    GlobalTransactionDescriptor descriptor = sids.remove(serverTransactionID);
    if (descriptor != null) {
      ids.remove(descriptor.getGlobalTransactionID());
    }
    return descriptor;
  }

  @Override
  public void shutdownNode(NodeID nid) {
    Collection gidDescs = this.sids.removeAll(nid);
    logger.info("shutdownClient() : Removing txns from DB : " + gidDescs.size());
    removeGlobalTransactionDescs(gidDescs);
  }

  /**
   * Global Transaction descriptors should have been deleted from sids data structure, before this call.
   */
  private void removeGlobalTransactionDescs(Collection gidDescs) {
    SortedSet<GlobalTransactionID> toRemove = new TreeSet<GlobalTransactionID>();
    for (Iterator i = gidDescs.iterator(); i.hasNext();) {
      GlobalTransactionDescriptor gd = (GlobalTransactionDescriptor) i.next();
      this.ids.remove(gd.getGlobalTransactionID());
      toRemove.add(gd.getGlobalTransactionID());
    }
    if (!gidDescs.isEmpty()) {
      this.persistor.deleteAllGlobalTransactionDescriptors(toRemove);
    }
  }

  @Override
  public void shutdownAllClientsExcept(Set cids) {
    Collection gidDescs = this.sids.removeAllExcept(cids);
    logger.info("shutdownAllClientsExcept() : Removing txns from DB : " + gidDescs.size());
    removeGlobalTransactionDescs(gidDescs);
  }

  // Used in Passive server
  @Override
  public void createGlobalTransactionDescIfNeeded(ServerTransactionID stxnID, GlobalTransactionID globalTransactionID) {
    GlobalTransactionDescriptor rv = new GlobalTransactionDescriptor(stxnID, globalTransactionID);
    basicAdd(rv, true);
  }

  // Used in Passive server
  @Override
  public void clearCommitedTransactionsBelowLowWaterMark(GlobalTransactionID lowWaterMark) {
    SortedSet<GlobalTransactionID> toRemove = new TreeSet<GlobalTransactionID>();
    synchronized (this.ids) {
      Map lowerThanLWM = this.ids.headMap(lowWaterMark);
      for (Iterator i = lowerThanLWM.values().iterator(); i.hasNext();) {
        GlobalTransactionDescriptor gd = (GlobalTransactionDescriptor) i.next();
        if (gd.complete()) {
          i.remove();
          ServerTransactionID sid = gd.getServerTransactionID();
          this.sids.remove(sid);
          toRemove.add(gd.getGlobalTransactionID());
        }
      }
    }
    if (!toRemove.isEmpty()) {
      this.persistor.deleteAllGlobalTransactionDescriptors(toRemove);
    }
  }
}