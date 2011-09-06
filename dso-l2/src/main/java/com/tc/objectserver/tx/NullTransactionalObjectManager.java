/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.objectserver.context.CommitTransactionContext;
import com.tc.objectserver.context.RecallObjectsContext;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;

import java.util.Collection;

public class NullTransactionalObjectManager implements TransactionalObjectManager {

  public void addTransactions(Collection txns) {
    // Nop
  }

  public boolean applyTransactionComplete(ApplyTransactionInfo applyTxnInfo) {
    // Nop
    return false;
  }

  public void lookupObjectsForTransactions() {
    // Nop
  }

  public void commitTransactionsComplete(CommitTransactionContext ctc) {
    // Nop
  }

  public void processApplyComplete() {
    // Nop
  }

  public void recallAllCheckedoutObject() {
    // Nop
  }

  public void recallCheckedoutObject(RecallObjectsContext roc) {
    // Nop
  }
}
