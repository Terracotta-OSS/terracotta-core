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

  @Override
  public void addTransactions(Collection<ServerTransaction> txns) {
    // Nop
  }

  @Override
  public boolean applyTransactionComplete(ApplyTransactionInfo applyTxnInfo) {
    // Nop
    return false;
  }

  @Override
  public void lookupObjectsForTransactions() {
    // Nop
  }

  @Override
  public void commitTransactionsComplete(CommitTransactionContext ctc) {
    // Nop
  }

  @Override
  public void processApplyComplete() {
    // Nop
  }

  @Override
  public void recallAllCheckedoutObject() {
    // Nop
  }

  @Override
  public void recallCheckedoutObject(RecallObjectsContext roc) {
    // Nop
  }
}
