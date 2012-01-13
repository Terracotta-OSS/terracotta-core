/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.objectserver.context.CommitTransactionContext;
import com.tc.objectserver.context.RecallObjectsContext;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;

import java.util.Collection;

public interface TransactionalObjectManager {

  public void addTransactions(Collection<ServerTransaction> txns);

  public void lookupObjectsForTransactions();

  public boolean applyTransactionComplete(ApplyTransactionInfo applyTxnInfo);

  public void processApplyComplete();

  public void commitTransactionsComplete(CommitTransactionContext ctc);

  public void recallAllCheckedoutObject();

  public void recallCheckedoutObject(RecallObjectsContext roc);

}
