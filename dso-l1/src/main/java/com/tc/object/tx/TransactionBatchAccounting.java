/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import gnu.trove.TLinkable;
import gnu.trove.TLinkedList;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class TransactionBatchAccounting {

  private final SortedMap   batchesByTransaction = new TreeMap();
  private final TLinkedList batches              = new TLinkedList();
  private boolean           stopped              = false;
  private TransactionID     highWaterMark        = TransactionID.NULL_ID;

  public Object dump() {
    return this.toString();
  }

  public String toString() {
    return "TransactionBatchAccounting[batchesByTransaction=" + batchesByTransaction + "]";
  }

  public synchronized void addBatch(TxnBatchID batchID, Collection transactionIDs) {
    if (stopped || transactionIDs.size() == 0) return;
    BatchDescriptor desc = new BatchDescriptor(batchID, transactionIDs);
    batches.add(desc);
    for (Iterator i = transactionIDs.iterator(); i.hasNext();) {
      TransactionID txID = (TransactionID) i.next();
      Object removed = batchesByTransaction.put(txID, desc);
      if (removed != null) { throw new AssertionError("TransactionID is already accounted for: " + txID + "=>"
                                                      + removed); }
      if (highWaterMark.toLong() < txID.toLong()) {
        highWaterMark = txID;
      }
    }
  }

  public synchronized Collection getTransactionIdsFor(TxnBatchID batchID) {
    Iterator iter = batches.iterator();
    while (iter.hasNext()) {
      BatchDescriptor bd = (BatchDescriptor) iter.next();
      if (bd.getId().equals(batchID)) { return new HashSet(bd.getTransactions()); }
    }
    return Collections.EMPTY_SET;
  }

  public synchronized TxnBatchID getBatchByTransactionID(TransactionID txID) {
    BatchDescriptor desc = (BatchDescriptor) batchesByTransaction.get(txID);
    return desc == null ? TxnBatchID.NULL_BATCH_ID : desc.batchID;
  }

  /**
   * Adds all incomplete transaction batch ids to the given collection in the order they were added. An incomplete
   * transaction batch is a batch for which not all its constituent transactions have been ACKed.
   * 
   * @param c The collection to add all incomplete batch ids to
   * @return The input collection
   */
  public synchronized List addIncompleteBatchIDsTo(List c) {
    for (Iterator i = batches.iterator(); i.hasNext();) {
      BatchDescriptor desc = (BatchDescriptor) i.next();
      c.add(desc.batchID);
    }
    return c;
  }

  public synchronized TxnBatchID getMinIncompleteBatchID() {
    return batches.isEmpty() ? TxnBatchID.NULL_BATCH_ID : ((BatchDescriptor) batches.getFirst()).batchID;
  }

  public synchronized TxnBatchID acknowledge(TransactionID txID) {
    if (stopped) return TxnBatchID.NULL_BATCH_ID;
    final TxnBatchID completed;
    final BatchDescriptor desc = (BatchDescriptor) batchesByTransaction.remove(txID);
    if (desc == null) throw new AssertionError("Batch not found for " + txID);
    if (desc.acknowledge(txID) == 0) {
      // completedGlobalTransactionIDs.addAll(desc.globalTransactionIDs);
      batches.remove(desc);
      completed = desc.batchID;
    } else {
      completed = TxnBatchID.NULL_BATCH_ID;
    }
    if (batches.size() == 0 && batchesByTransaction.size() > 0) { throw new AssertionError(
                                                                                           "Batches list and batchesByTransaction map aren't zero at the same time"); }
    return completed;
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
      return (TransactionID) batchesByTransaction.firstKey();
    }
  }

  public synchronized void clear() {
    batches.clear();
    batchesByTransaction.clear();
  }

  private static final class BatchDescriptor implements TLinkable {
    private final TxnBatchID batchID;
    private final Set        transactionIDs = new HashSet();

    private TLinkable        next;
    private TLinkable        previous;

    public BatchDescriptor(TxnBatchID batchID, Collection txIDs) {
      this.batchID = batchID;
      transactionIDs.addAll(txIDs);
    }

    public String toString() {
      return "BatchDescriptor[" + batchID + ", transactionIDs=" + transactionIDs + "]";
    }

    public int acknowledge(TransactionID txID) {
      transactionIDs.remove(txID);
      return transactionIDs.size();
    }

    public TLinkable getNext() {
      return next;
    }

    public TLinkable getPrevious() {
      return previous;
    }

    public void setNext(TLinkable linkable) {
      next = linkable;
    }

    public void setPrevious(TLinkable linkable) {
      previous = linkable;
    }

    public Collection getTransactions() {
      return transactionIDs;
    }

    public TxnBatchID getId() {
      return batchID;
    }
  }

  /**
   * This is used for testing.
   */
  public synchronized List addIncompleteTransactionIDsTo(LinkedList list) {
    list.addAll(batchesByTransaction.keySet());
    return list;
  }

  /**
   * Ignores all further modifier calls. This is used for testing to stop shutdown hooks from hanging.
   */
  public synchronized void stop() {
    this.stopped = true;
  }

}
