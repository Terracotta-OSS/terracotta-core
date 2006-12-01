/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.lockmanager.impl;

import com.tc.objectserver.lockmanager.api.DeadlockResults;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

class DeadlockDetector {
  private static final ServerThreadContext[] EMPTY_TXN_ARRAY = new ServerThreadContext[] {};

  private final DeadlockResults          listener;
  private final Set                      inDeadlock      = new HashSet();

  DeadlockDetector(DeadlockResults listener) {
    this.listener = listener;
  }

  void detect(Iterator openTransactions) {
    while (openTransactions.hasNext()) {
      ServerThreadContext txn = (ServerThreadContext) openTransactions.next();
      txn.setCycle(null); // null this out just in case it was corrupted by some exception in a previous scan

      if (!inDeadlock.contains(txn)) {
        visit(txn);
      }
    }
  }

  private void visit(final ServerThreadContext me) {
    if (!me.isWaiting()) return;

    final Lock waitingOn = me.getWaitingOn();
    final Collection holders = waitingOn.getHoldersCollection();

    for (final Iterator iter = holders.iterator(); iter.hasNext();) {
      Holder holder = (Holder) iter.next();
      ServerThreadContext them = holder.getThreadContext();

      if (!them.equals(me)) {
        if (them.isWaiting()) {
          me.setCycle(them);
          if (them.getCycle() != null) {
            handleDeadlock(me, them);
          } else {
            visit(them);
          }
          me.setCycle(null);
        }
      }
    }
  }

  private void handleDeadlock(ServerThreadContext me, ServerThreadContext them) {
    List txnList = getTransactionsInCycle(me, them);
    inDeadlock.addAll(txnList);

    DeadlockChainImpl head = null;
    DeadlockChainImpl link = null;

    ServerThreadContext[] txnArray = (ServerThreadContext[]) txnList.toArray(EMPTY_TXN_ARRAY);
    for (int i = 0, n = txnArray.length; i < n; i++) {
      ServerThreadContext txn = txnArray[i];
      DeadlockChainImpl tmp = new DeadlockChainImpl(txn.getId(), txn.getWaitingOn().getLockID());

      if (head == null) {
        head = tmp;
      }

      if (link != null) {
        link.setNextLink(tmp);
      }

      link = tmp;
    }

    link.setNextLink(head);

    listener.foundDeadlock(head);
  }

  private static List getTransactionsInCycle(ServerThreadContext me, ServerThreadContext them) {
    List txns = new ArrayList();
    txns.add(me);

    do {
      txns.add(them);
      them = them.getCycle();
    } while (!them.equals(me));

    return txns;
  }
}
