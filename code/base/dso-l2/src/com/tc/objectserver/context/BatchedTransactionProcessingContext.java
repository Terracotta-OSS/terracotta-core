/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class BatchedTransactionProcessingContext implements EventContext {

  private final Set        objectIDs    = new HashSet();
  private final Map        objects      = new HashMap();
  private final Map        newRoots     = new HashMap();
  private final Collection serverTxns   = new HashSet();
  private final Collection transactions = new ArrayList();

  private boolean          closed       = false;
  private Set              completedTxnIDs;

  public BatchedTransactionProcessingContext() {
    super();
  }

  public Iterator iterator() {
    if (!closed) { throw new AssertionError("BatchedTransactionProcessingContext is not closed yet !"); }
    return transactions.iterator();
  }

  public boolean isEmpty() {
    return transactions.isEmpty();
  }
  
  public int getTransactionsCount() {
    return transactions.size();
  }

  public void close(Set completedTxnIds) {
    this.closed = true;
    this.completedTxnIDs = completedTxnIds;
  }

  public boolean isClosed() {
    return closed;
  }

  public void addTransaction(ServerTransaction txn) {
    if (closed) { throw new AssertionError("BatchedTransactionContext is already closed !"); }
    transactions.add(txn);
    newRoots.putAll(txn.getNewRoots());
  }

  public void addLookedUpObjects(Collection ids, Map objectsMap) {
    if (closed) { throw new AssertionError("BatchedTransactionContext is already closed !"); }
    objectIDs.addAll(ids);
    objects.putAll(objectsMap);
  }

  public Set getObjectIDs() {
    return objectIDs;
  }

  public Collection getObjects() {
    return objects.values();
  }

  public Map getObjectsMap() {
    return objects;
  }

  public Map getNewRoots() {
    return newRoots;
  }

  public Collection getAppliedServerTransactionIDs() {
    return serverTxns;
  }

  public Collection getTxns() {
    return transactions;
  }

  public String toString() {
    return "BatchedTransactionProcessingContext : { Txn count = " + transactions.size() + " ObjectIDs = " + objectIDs
           + " } ";
  }

  public void addAppliedServerTransactionIDsTo(ServerTransactionID stxID) {
    if (!serverTxns.add(stxID)) throw new AssertionError("Attempt to add GlobalTransactionID more than once: " + stxID);
  }

  public Set getCompletedTransactionIDs() {
    Assert.assertTrue(closed);
    return completedTxnIDs;
  }

}
