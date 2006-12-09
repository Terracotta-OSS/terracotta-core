/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.lockmanager.api.LockID;
import com.tc.util.Assert;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TransactionSequencer {

  private static final TCLogger logger      = TCLogging.getLogger(TransactionSequencer.class);
  private final Map             pendingTxns = new LinkedHashMap();
  private final LinkedList      txnQ        = new LinkedList();

  private int                   txnsCount;

  public synchronized void addTransactions(List txns) {
    if (false) log_incoming(txns);
    txnQ.addAll(txns);
    txnsCount+= txns.size();
  }

  private void log_incoming(List txns) {
    for (Iterator i = txns.iterator(); i.hasNext();) {
      ServerTransaction txn = (ServerTransaction) i.next();
      logger.info("Incoming : " + txn);
    }
  }

  public synchronized ServerTransaction getNextTxnToProcess() {
    while (!txnQ.isEmpty()) {
      ServerTransaction txn = (ServerTransaction) txnQ.removeFirst();
      if (toProcessOrMakePending(txn)) {
        if (false) log_outgoing(txn);
        txnsCount--;
        return txn;
      }
    }
    if(false) log_no_txns_to_process();
    return null;
  }

  private void log_no_txns_to_process() {
    if (txnsCount != 0) {
      int psize = pendingTxns.size();
      logger.info("No More Txns that can be processed : txnCount = " + txnsCount + " of which pending txns = " + psize);
    }
  }

  private void log_outgoing(ServerTransaction txn) {
    logger.info("Outgoing : " + txn);
  }

  private boolean toProcessOrMakePending(ServerTransaction txn) {
    LockID[] locks = txn.getLockIDs();
    Collection oids = txn.getObjectIDs();
    for (Iterator i = pendingTxns.values().iterator(); i.hasNext();) {
      PendingAccount pe = (PendingAccount) i.next();
      if (pe.containsAnyLock(locks) || pe.containsAnyOids(oids)) {
        pe.addPending(txn);
        return false;
      }
    }
    return true;
  }

  public synchronized void makePending(ServerTransaction txn) {
    Object old = pendingTxns.put(txn.getServerTransactionID(), new PendingAccount(txn));
    txnsCount ++;
    Assert.assertNull(old);
    if (false) logger.info("Make Pending : " + txn);
  }

  public synchronized void processedPendingTxn(ServerTransaction txn) {
    PendingAccount pa = (PendingAccount) pendingTxns.remove(txn.getServerTransactionID());
    if (pa == null) { throw new AssertionError("processedPendingTxn() called without calling makePending()"); }
    txnsCount--;
    LinkedList txns = pa.getPendingTxnList();
    while (!txns.isEmpty()) {
      // Order need to be maintained
      txnQ.addFirst(txns.removeLast());
    }
    if (false) logger.info("Processed Pending : " + txn);
  }

  /*
   * Used for testing
   */
  boolean isPending(List txns) {
    for (Iterator i = txns.iterator(); i.hasNext();) {
      ServerTransaction st = (ServerTransaction) i.next();
      if (pendingTxns.containsKey(st.getServerTransactionID())) return true;
    }
    return false;
  }

  private static final class PendingAccount {

    private final ServerTransaction pendingTxn;
    private final Set               locks = new HashSet();
    private final LinkedList        txns  = new LinkedList();
    private final Set               oids  = new HashSet();

    public PendingAccount(ServerTransaction txn) {
      this.pendingTxn = txn;
      addLocks(txn);
      addObjectIds(txn);
    }

    public LinkedList getPendingTxnList() {
      return txns;
    }

    public void addPending(ServerTransaction txn) {
      addLocks(txn);
      addObjectIds(txn);
      txns.add(txn);
    }

    private void addObjectIds(ServerTransaction txn) {
      oids.addAll(txn.getObjectIDs());
    }

    public boolean containsAnyLock(LockID[] lockIDs) {
      for (int i = 0; i < lockIDs.length; i++) {
        if (locks.contains(lockIDs[i])) return true;
      }
      return false;
    }

    public boolean containsAnyOids(Collection oidsList) {
      for (Iterator i = oidsList.iterator(); i.hasNext();) {
        if (oids.contains(i.next())) return true;
      }
      return false;
    }

    private void addLocks(ServerTransaction txn) {
      LockID[] lockIDs = txn.getLockIDs();
      for (int i = 0; i < lockIDs.length; i++) {
        locks.add(lockIDs[i]);
      }
    }

    public String toString() {
      return "PendingAccount(" + pendingTxn + ") :  Locks = " + locks + " : txns : " + txns;
    }
  }
}
