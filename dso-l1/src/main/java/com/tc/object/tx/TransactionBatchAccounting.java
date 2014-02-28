/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

public class TransactionBatchAccounting {

  private static final TCLogger LOGGER = TCLogging.getLogger(TransactionBatchAccounting.class);
  private final NavigableMap<TransactionID, BatchDescriptor> batchesByTransaction = new TreeMap<TransactionID, BatchDescriptor>();
  private boolean                                         stopped              = false;
  private TransactionID                                   highWaterMark        = TransactionID.NULL_ID;

  public Object dump() {
    return this.toString();
  }

  @Override
  public synchronized String toString() {
    return "TransactionBatchAccounting[batchesByTransaction=" + batchesByTransaction + "]";
  }

  public synchronized void addBatch(TxnBatchID batchID, List<TransactionID> transactionIDs) {
    if (transactionIDs.isEmpty()) return;
    Collections.sort(transactionIDs);
    BatchDescriptor desc = new BatchDescriptor(batchID, transactionIDs);
    TransactionID key = transactionIDs.get(0);
    batchesByTransaction.put(key, desc);
    Map.Entry<TransactionID,BatchDescriptor> lower = batchesByTransaction.lowerEntry(key);
    if ( lower != null && lower.getValue().getId().equals(batchID) ) {
      batchesByTransaction.remove(lower.getKey());
    }
    if (highWaterMark.toLong() < transactionIDs.get(transactionIDs.size()-1).toLong()) {
      highWaterMark = transactionIDs.get(transactionIDs.size()-1);
    }
  }

  public synchronized Collection<TransactionID> getTransactionIdsFor(TxnBatchID batchID) {
    Iterator<BatchDescriptor> iter = batchesByTransaction.values().iterator();
    while (iter.hasNext()) {
      BatchDescriptor bd = iter.next();
      if (bd.getId().equals(batchID)) { 
        return new HashSet<TransactionID>(bd.getTransactions()); 
      }
    }
    return Collections.<TransactionID>emptySet();
  }

  public synchronized TxnBatchID getBatchByTransactionID(TransactionID txID) {
    Map.Entry<TransactionID, BatchDescriptor> desc = batchesByTransaction.floorEntry(txID);
    while ( desc != null && !desc.getValue().contains(txID) ) {
      desc = batchesByTransaction.lowerEntry(desc.getKey());
  }
    return desc == null ? TxnBatchID.NULL_BATCH_ID : desc.getValue().getId();
  }

  public synchronized Map<TxnBatchID, Collection<TransactionID>> getBatchesForTransactions(List<TransactionID> list) {
    Map<TxnBatchID, Collection<TransactionID>> map = new HashMap<TxnBatchID, Collection<TransactionID>>();
    if ( stopped ) {
      return map;
    }
    Collections.sort(list);
    BatchDescriptor desc = null;
    Collection<TransactionID> tids = null;
    int lookupCount = 0;
    int batchCount = 0;
    for ( TransactionID tid : list ) {
      if ( desc == null || !desc.contains(tid) ) {
        lookupCount += 1;
        Map.Entry<TransactionID, BatchDescriptor> pointer = batchesByTransaction.floorEntry(tid);
        if ( pointer == null  ) {
          throw new AssertionError("batch not found for transaction " + tid);
        }
        while ( !pointer.getValue().contains(tid) ) {
          pointer = batchesByTransaction.lowerEntry(pointer.getKey());
          lookupCount++;
          if ( pointer == null ) {
            throw new AssertionError("batch not found for transaction " + tid);
          }
        }
        desc = pointer.getValue();
        tids = map.get(desc.getId());
        if ( tids == null ) {
          tids = new HashSet<TransactionID>();
          map.put(desc.getId(), tids);
        }
      } else {
        if ( desc == null || tids == null ) {
          throw new AssertionError("batch not found for transaction " + tid);
        }
      }
      tids.add(tid);
    }
    if ( LOGGER.isDebugEnabled() ) {
      LOGGER.debug("lookup count:" + lookupCount + " batch count:" + batchCount + " txn count:"  + list.size());
    }
    return map;
  }

  /**
   * Adds all incomplete transaction batch ids to the given collection in the order they were added. An incomplete
   * transaction batch is a batch for which not all its constituent transactions have been ACKed.
   * 
   * @param c The collection to add all incomplete batch ids to
   * @return The input collection
   */
  public synchronized List<TxnBatchID> addIncompleteBatchIDsTo(List<TxnBatchID> c) {
    for (BatchDescriptor desc : batchesByTransaction.values()) {
      c.add(desc.batchID);
    }
    Collections.sort(c);
    if (c.size() != new HashSet(c).size()) {
      throw new AssertionError("duplicate batch id");
    }
    return c;
  }

  public synchronized TxnBatchID getMinIncompleteBatchID() {
    if (stopped || batchesByTransaction.isEmpty() ) {
      return TxnBatchID.NULL_BATCH_ID;
  }
    List<BatchDescriptor> toSort = new ArrayList<BatchDescriptor>(batchesByTransaction.values());
    Collections.sort(toSort);
    return toSort.get(0).getId();
  }

  public synchronized boolean acknowledge(TxnBatchID batch, Collection<TransactionID> tids) {
    if (stopped) return false;
    if ( tids.isEmpty() ) {
      return false;
    }
    
    Map.Entry<TransactionID, BatchDescriptor> pointer = batchesByTransaction.floorEntry(tids.iterator().next());
    while ( !pointer.getValue().getId().equals(batch) ) {
      pointer = batchesByTransaction.lowerEntry(pointer.getKey());
    }
        
    if ( pointer.getValue().acknowledge(tids) ) {
      batchesByTransaction.remove(pointer.getKey());
      return true;
    } else {
      return false;
    }
  }

  public synchronized TransactionID getLowWaterMark() {
    if (batchesByTransaction.isEmpty()) {
      if (highWaterMark == TransactionID.NULL_ID) {
        return TransactionID.NULL_ID;
      } else {
        // Low water mark should be set to the next valid lowwatermark, so that transactions are cleared correctly in
        // the server
        return highWaterMark.next();
      }
    } else {
      List<TransactionID> list = new ArrayList(batchesByTransaction.firstEntry().getValue().getTransactions());
      Collections.sort(list);
      return list.get(0);
    }
  }

  public synchronized void clear() {
    batchesByTransaction.clear();
  }

  private static final class BatchDescriptor implements Comparable<BatchDescriptor>{
    private final TxnBatchID batchID;
    private final Set<TransactionID>        transactionIDs = new HashSet<TransactionID>();

    public BatchDescriptor(TxnBatchID batchID, Collection<TransactionID> txIDs) {
      this.batchID = batchID;
      transactionIDs.addAll(txIDs);
    }

    @Override
    public String toString() {
      return "BatchDescriptor[" + batchID + ", transactionIDs=" + transactionIDs + "]";
    }

    public int acknowledge(TransactionID txID) {
      transactionIDs.remove(txID);
      return transactionIDs.size();
    }

    public boolean acknowledge(Collection<TransactionID> tids) {
      for ( TransactionID txID : tids ) {
        transactionIDs.remove(txID);
      }
      return transactionIDs.isEmpty();
    }    

    public Collection<TransactionID> getTransactions() {
      return transactionIDs;
    }

    public TxnBatchID getId() {
      return batchID;
    }
    
    public boolean contains(TransactionID tid) {
      return transactionIDs.contains(tid);
  }

    public int size() {
      return transactionIDs.size();
    }

    @Override
    public int compareTo(BatchDescriptor o) {
      return getId().compareTo(o.getId());
    }
    
    
  }

  /**
   * This is used for testing.
   * @param list
   * @return 
   */
  public synchronized List<TransactionID> addIncompleteTransactionIDsTo(LinkedList<TransactionID> list) {
    for ( Map.Entry<TransactionID, BatchDescriptor> pointer : batchesByTransaction.entrySet() ) {
      list.addAll(pointer.getValue().getTransactions());
    }
    return list;
  }

  /**
   * Ignores all further modifier calls. This is used for testing to stop shutdown hooks from hanging.
   */
  public synchronized void stop() {
    this.stopped = true;
  }

}
