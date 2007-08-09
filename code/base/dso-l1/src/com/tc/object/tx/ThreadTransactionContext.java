/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.lockmanager.api.LockID;
import com.tc.util.Assert;
import com.tc.util.Stack;

import java.util.Iterator;

/**
 * We have two concepts. Transactions which carry changes/creates etc... And the locks That are associated with those
 * transactions. Transactions need to be created and then continued until the next transaction exit (in other words not
 * just until the next begin) if we are to maintain proper semantics/ordering. The Locks on the otherhand work more like
 * a stack. When a block is entered the locks get pushed onto the stack and associated with the current transaction and
 * when they are exited they are removed from the current transaction. This class maintains both the current transaction
 * and the stack of contexts that are associated with the thread.
 */
public class ThreadTransactionContext {
  private ClientTransaction currentTransaction;
  private Stack             transactionStack = new Stack();

  public void setCurrentTransaction(ClientTransaction tx) {
    Assert.eval(tx != null);
    this.currentTransaction = tx;
  }

  public ClientTransaction getCurrentTransaction() {
    return currentTransaction;
  }

  public ClientTransaction popCurrentTransaction() {
    transactionStack.pop();
    ClientTransaction ctx = currentTransaction;
    currentTransaction = null;
    return ctx;
  }
  
  public ITransactionContext peekContext(LockID id) {
    if (transactionStack.isEmpty()) return null;
    int len = transactionStack.size();
    ITransactionContext tc = null;
    int i=len-1;
    for (; i>=0; i--) {
      tc = (ITransactionContext) transactionStack.get(i);
      if (tc.getLockID().equals(id)) {
        return tc;
      }
    }
    return null;
  }
  
  public ClientTransaction popCurrentTransaction(LockID id) {
    int len = transactionStack.size();
    boolean found = false;
    TransactionContext tc = null;
    int i=len-1;
    for (; i>=0; i--) {
      tc = (TransactionContext) transactionStack.get(i);
      if (tc.getLockID().equals(id)) {
        found = true;
        break;
      }
    }
    if (found) {
      for (int j=i+1; j<len; j++) {
        tc = (TransactionContext)transactionStack.get(j);
        tc.removeLock(id);
      }
      transactionStack.remove(i);
    }
    
    ClientTransaction ctx = currentTransaction;
    currentTransaction = null;
    return ctx;
  }

  public void pushContext(LockID id, TxnType txType) {
    transactionStack.push(new TransactionContext(id, txType, getAllLockIDs(id)));
  }

  private LockID[] getAllLockIDs(LockID id) {
    LockID[] lids = new LockID[transactionStack.size() + 1];
    lids[0] = id;
    for (int i = 1; i < lids.length; i++) {
      ITransactionContext tc = (ITransactionContext) transactionStack.get(i - 1);
      lids[i] = tc.getLockID();
    }
    return lids;
  }
  
  public void removeLock(LockID id) {
    for (Iterator i=transactionStack.iterator(); i.hasNext(); ) {
      ITransactionContext tc = (ITransactionContext) i.next();
      if (id.equals(tc.getLockID())) {
        i.remove();
      } else {
        tc.removeLock(id);
      }
    }
  }

  public ITransactionContext peekContext() {
    if (transactionStack.isEmpty()) return null;
    return (ITransactionContext) transactionStack.peek();
  }
}
