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
package com.tc.object.tx;

import com.tc.abortable.AbortableOperationManager;
import com.tc.abortable.AbortedOperationException;
import com.tc.exception.PlatformRejoinException;
import com.tc.exception.TCNotRunningException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ClearableCallback;
import com.tc.object.locks.LockID;
import com.tc.util.AbortedOperationUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class LockAccounting implements ClearableCallback {
  private static final TCLogger logger    = TCLogging.getLogger(LockAccounting.class);
  private static final long                              WAIT_FOR_TRANSACTIONS_INTERVAL = TimeUnit.SECONDS
                                                                                            .toMillis(10L);

  private final CopyOnWriteArrayList<TxnRemovedListener> listeners                      = new CopyOnWriteArrayList<TxnRemovedListener>();

  private final Map<TransactionIDWrapper, Set<LockID>>   tx2Locks                       = new HashMap<TransactionIDWrapper, Set<LockID>>();
  private final Map<LockID, Set<TransactionIDWrapper>>   lock2Txs                       = new HashMap<LockID, Set<TransactionIDWrapper>>();
  private final Map<TransactionID, TransactionIDWrapper> tid2wrap                       = new HashMap<TransactionID, TransactionIDWrapper>();
  private volatile boolean                               shutdown                       = false;
  private final AbortableOperationManager                abortableOperationManager;
  private final RemoteTransactionManagerImpl             remoteTxnMgrImpl;

  public LockAccounting(AbortableOperationManager abortableOperationManager,
                        RemoteTransactionManagerImpl remoteTxnMgrImpl) {
    this.abortableOperationManager = abortableOperationManager;
    this.remoteTxnMgrImpl = remoteTxnMgrImpl;
  }

  @Override
  public synchronized void cleanup() {
    for (TxnRemovedListener listener : listeners) {
      listener.release();
    }
    listeners.clear();
    tx2Locks.clear();
    lock2Txs.clear();
    tid2wrap.clear();
  }

  public synchronized boolean hasListeners() {
    return !listeners.isEmpty();
  }
  
  public synchronized boolean contains(TransactionID tid) {
    return tid2wrap.containsKey(tid);
  }

  public synchronized Object dump() {
    return toString();
  }

  public void shutdown() {
    this.shutdown = true;
  }

  @Override
  public synchronized String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Lock Accounting:\n");
    builder.append("[tx2Locks=" + tx2Locks + "\nlock2Txs=" + lock2Txs + "]");
    return builder.toString();
  }

  public synchronized void add(TransactionID txID, Collection lockIDs) {
    TransactionIDWrapper txIDWrapper = getOrCreateWrapperFor(txID);
    getOrCreateSetFor(txIDWrapper, tx2Locks).addAll(lockIDs);
    for (Iterator i = lockIDs.iterator(); i.hasNext();) {
      LockID lid = (LockID) i.next();
      getOrCreateSetFor(lid, lock2Txs).add(txIDWrapper);
    }
  }

  public synchronized Collection getTransactionsFor(LockID lockID) {
    Set rv = new HashSet();
    Set<TransactionIDWrapper> toAdd = lock2Txs.get(lockID);

    if (toAdd != null) {
      for (TransactionIDWrapper txIDWrapper : toAdd) {
        rv.add(txIDWrapper.getTransactionID());
      }
    }
    return rv;
  }

  public synchronized boolean areTransactionsReceivedForThisLockID(LockID lockID) {
    Set<TransactionIDWrapper> txnsForLockID = lock2Txs.get(lockID);

    if (txnsForLockID == null || txnsForLockID.isEmpty()) { return true; }

    for (TransactionIDWrapper txIDWrapper : txnsForLockID) {
      if (!txIDWrapper.isReceived()) { return false; }
    }

    return true;
  }

  public synchronized void transactionRecvdByServer(Set<TransactionID> txnsRecvd) {
    Set<TransactionIDWrapper> txnSet = tx2Locks.keySet();

    if (txnSet == null) { return; }

    for (TransactionIDWrapper txIDWrapper : txnSet) {
      if (txnsRecvd.contains(txIDWrapper.getTransactionID())) {
        txIDWrapper.received();
      }
    }
  }

  public synchronized Set<LockID> acknowledge(Collection<TransactionID> tids) {
    HashSet<LockID> agg = new HashSet<LockID>();
    for ( TransactionID txID : tids ) {
      agg.addAll(acknowledgeReally(txID));
    }
    return agg;
  }
  
  public synchronized Set<LockID> acknowledge(TransactionID txID) {
    return acknowledgeReally(txID);
  }
  // This method returns a set of lockIds that has no more transactions to wait for
  private Set<LockID> acknowledgeReally(TransactionID txID) {
    if ( !Thread.holdsLock(this) ) {
      throw new AssertionError();
    }
    Set<LockID> completedLockIDs = null;
    TransactionIDWrapper txIDWrapper = new TransactionIDWrapper(txID);
    Set<LockID> lockIDs = tx2Locks.get(txIDWrapper);
    if (lockIDs != null) {
      // this may be null if there are phantom acknowledgments caused by server restart.
      for (LockID lid : lockIDs ) {
        Set<TransactionIDWrapper> txIDs = getOrCreateSetFor(lid, lock2Txs);
        if (!txIDs.remove(txIDWrapper)) {
            throw new AssertionError("No lock=>transaction found for " + lid + ", " + txID);
        }
        if (txIDs.isEmpty()) {
          lock2Txs.remove(lid);
          if (completedLockIDs == null) {
            completedLockIDs = new HashSet<LockID>();
          }
          completedLockIDs.add(lid);
        }
      }
    } else {
      System.out.println("locks are null");
    }
    removeTxn(txIDWrapper);
    return (completedLockIDs == null ? Collections.<LockID>emptySet() : completedLockIDs);
  }

  private void removeTxn(TransactionIDWrapper txnIDWrapper) {
    tx2Locks.remove(txnIDWrapper);
    tid2wrap.remove(txnIDWrapper.getTransactionID());
    notifyTxnRemoved(txnIDWrapper);
    if ( logger.isDebugEnabled() ) {
      logger.debug(txnIDWrapper.getTransactionID() + " completed");
    }
  }

  private void notifyTxnRemoved(TransactionIDWrapper txnID) {
    for (TxnRemovedListener l : listeners) {
      l.txnRemoved(txnID);
    }
  }

  public synchronized boolean isEmpty() {
    return tx2Locks.isEmpty() && lock2Txs.isEmpty();
  }

  public void waitAllCurrentTxnCompleted() throws AbortedOperationException {
    TxnRemovedListener listener;
    CountDownLatch latch = null;
    synchronized (this) {
      latch = new CountDownLatch(tx2Locks.size());
      listener = new TxnRemovedListener(new HashSet(tx2Locks.keySet()), latch);
      listeners.add(listener);
    }

    boolean txnsCompleted = false;
    boolean interrupted = false;
    try {
      // DEV-6271: During rejoin, the client could be shut down. In that case we need to get
      // out of this wait and throw a TCNotRunningException for upper layers to handle
      do {
        try {
          if (shutdown) { throw new TCNotRunningException(); }
          if (remoteTxnMgrImpl.isRejoinInProgress()) { throw new PlatformRejoinException(); }
          txnsCompleted = latch.await(WAIT_FOR_TRANSACTIONS_INTERVAL, TimeUnit.MILLISECONDS);
          if ( logger.isDebugEnabled() ) {
            logger.debug("waiting for " + listener);
          }
        } catch (InterruptedException e) {
          AbortedOperationUtil.throwExceptionIfAborted(abortableOperationManager);
          interrupted = true;
        }
      } while (!txnsCompleted);
    } finally {
      synchronized (this) {
        listeners.remove(listener);
      }

      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }

  }

  private static <K,V> Set<V> getOrCreateSetFor(K key, Map<K, Set<V>> m) {
    Set<V> rv = m.get(key);
    if (rv == null) {
      rv = new HashSet<V>();
      m.put(key, rv);
    }
    return rv;
  }

  private TransactionIDWrapper getOrCreateWrapperFor(TransactionID txID) {
    TransactionIDWrapper rv = tid2wrap.get(txID);
    if (rv == null) {
      rv = new TransactionIDWrapper(txID);
      tid2wrap.put(txID, rv);
    }
    return rv;
  }

  private class TxnRemovedListener {
    private final Set<TransactionIDWrapper>         txnSet;
    private final CountDownLatch                     latch;
    // private boolean released = false;

    TxnRemovedListener(Set<TransactionIDWrapper> txnSet, CountDownLatch latch) {
      if ( !Thread.holdsLock(LockAccounting.this) ) {
          throw new AssertionError();
      }
      this.txnSet = txnSet;
      this.latch = latch;
    }

    void txnRemoved(TransactionIDWrapper txnID) {
//  locked several frames up
      if ( !Thread.holdsLock(LockAccounting.this) ) {
          throw new AssertionError();
      }
      if ( this.txnSet.remove(txnID) ) {
        this.latch.countDown();
      } else {
//  not interested in this transaction
      }
    }
    
    void release() {
//  locked several frames up
      if ( !Thread.holdsLock(LockAccounting.this) ) {
          throw new AssertionError();
      }
      txnSet.clear();
        while ( this.latch.getCount() > 0 ) {
            latch.countDown();
        }
      // released = true;
    }

    @Override
    public String toString() {
      return "TxnRemovedListener{" + "txnSet=" + txnSet + ", latch=" + latch + '}';
    }
  }

  private static class TransactionIDWrapper {
    private final TransactionID txID;
    private boolean             isReceived = false;

    public TransactionIDWrapper(TransactionID txID) {
      this.txID = txID;
    }

    public TransactionID getTransactionID() {
      return this.txID;
    }

    public void received() {
      isReceived = true;
    }

    public boolean isReceived() {
      return isReceived;
    }

    @Override
    public int hashCode() {
      return txID.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || getClass() != obj.getClass()) return false;
      TransactionIDWrapper other = (TransactionIDWrapper) obj;
      if (txID == null) {
        if (other.txID != null) return false;
      } else if (!txID.equals(other.txID)) return false;
      return true;
    }

    @Override
    public String toString() {
      return "TransactionIDWrapper [isReceived=" + isReceived + ", txID=" + txID + "]";
    }
  }

  // for testing purpose only
  int sizeOfTransactionMap() {
    return tx2Locks.size();
  }

  // for testing purpose only
  int sizeOfLockMap() {
    return lock2Txs.size();
  }

  // for testing purpose only
  int sizeOfIDWrapMap() {
    return tid2wrap.size();
  }

}
