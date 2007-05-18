/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.tx.ServerTransactionListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * TODO::This adds some computational overhead. If found as a performance problem, this functionality could be provided
 * by the ServerTransactionManager itself
 */
public class UnappliedTransactionsInTheSystemMonitor implements ServerTransactionListener {

  public static interface Callback {
    public void doAction();
  }

  private static final TCLogger logger           = TCLogging.getLogger(UnappliedTransactionsInTheSystemMonitor.class);

  private final Set             unappliedTxns    = new HashSet();
  private final Map             pendingCallbacks = new HashMap();

  public synchronized void incomingTransactions(ChannelID cid, Set serverTxnIDs) {
    unappliedTxns.addAll(serverTxnIDs);
  }

  public void callBackWhenAllCurrentTxnsApplied(Callback callback) {
    boolean callbackNow = false;
    synchronized (this) {
      if (unappliedTxns.isEmpty()) {
        callbackNow = true;
      } else {
        Set copy = new HashSet(unappliedTxns);
        logger.info(callback + " : Pending Txn Applys to wait :" + copy);
        pendingCallbacks.put(callback, copy);
      }
    }
    if (callbackNow) {
      /**
       * XXX:: callbacks are called outside sync scope so that objectmanager.getAllObjectIDs() call which can take a
       * long time during startup in persisitent mode doesnt hold up incomming transactions.
       */
      callback.doAction();
    }
  }

  public void transactionApplied(ServerTransactionID stxID) {
    List callBacks = null;
    synchronized (this) {
      unappliedTxns.remove(stxID);
      if (pendingCallbacks.isEmpty()) return;
      for (Iterator i = pendingCallbacks.entrySet().iterator(); i.hasNext();) {
        Entry e = (Entry) i.next();
        Set pendingTxns = (Set) e.getValue();
        pendingTxns.remove(stxID);
        if (pendingTxns.isEmpty()) {
          Callback callback = (Callback) e.getKey();
          if (callBacks == null) {
            callBacks = new ArrayList(3);
          }
          callBacks.add(callback);
          i.remove();
        }
      }
    }
    if (callBacks != null) {
      /**
       * @see comments above.
       */
      for (Iterator i = callBacks.iterator(); i.hasNext();) {
        Callback callback = (Callback) i.next();
        callback.doAction();
      }
    }
  }

  public void transactionCompleted(ServerTransactionID stxID) {
    // NOP
  }

  public void addResentServerTransactionIDs(Collection stxIDs) {
    // NOP
  }

  public void clearAllTransactionsFor(ChannelID killedClient) {
    // NOP
  }
}
