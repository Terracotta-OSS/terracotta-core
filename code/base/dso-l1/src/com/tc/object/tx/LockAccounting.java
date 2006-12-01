/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.lockmanager.api.LockID;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class LockAccounting {

  private final Map tx2Locks = new HashMap();
  private final Map lock2Txs = new HashMap();

  public synchronized Object dump() {
    return "LockAccounting:\ntx2Locks=" + tx2Locks + "\nlock2Txs=" + lock2Txs + "/LockAccounting";
  }

  public String toString() {
    return "LockAccounting[tx2Locks=" + tx2Locks + ", lock2Txs=" + lock2Txs + "]";
  }

  public synchronized void add(TransactionID txID, Collection lockIDs) {
    getOrCreateSetFor(txID, tx2Locks).addAll(lockIDs);
    for (Iterator i = lockIDs.iterator(); i.hasNext();) {
      LockID lid = (LockID) i.next();
      getOrCreateSetFor(lid, lock2Txs).add(txID);
    }
  }

  public synchronized Collection getTransactionsFor(LockID lockID) {
    Collection rv = new HashSet();
    Collection toAdd = (Collection) lock2Txs.get(lockID);
    if (toAdd != null) {
      rv.addAll(toAdd);
    }
    return rv;
  }

  // This method returns a set of lockIds that has no more transactions to wait for
  public synchronized Set acknowledge(TransactionID txID) {
    Set completedLockIDs = null;
    Set lockIDs = getSetFor(txID, tx2Locks);
    if (lockIDs != null) {
      // this may be null if there are phantom acknowledgements caused by server restart.
      for (Iterator i = lockIDs.iterator(); i.hasNext();) {
        LockID lid = (LockID) i.next();
        Set txIDs = getOrCreateSetFor(lid, lock2Txs);
        if (!txIDs.remove(txID)) throw new AssertionError("No lock=>transaction found for " + lid + ", " + txID);
        if (txIDs.isEmpty()) {
          lock2Txs.remove(lid);
          if (completedLockIDs == null) {
            completedLockIDs = new HashSet();
          }
          completedLockIDs.add(lid);
        }
      }
    }
    tx2Locks.remove(txID);
    return (completedLockIDs == null ? Collections.EMPTY_SET :  completedLockIDs);
  }

  public synchronized boolean isEmpty() {
    return tx2Locks.isEmpty() && lock2Txs.isEmpty();
  }

  private static Set getSetFor(Object key, Map m) {
    return (Set) m.get(key);
  }

  private static Set getOrCreateSetFor(Object key, Map m) {
    Set rv = getSetFor(key, m);
    if (rv == null) {
      rv = new HashSet();
      m.put(key, rv);
    }
    return rv;
  }

}
