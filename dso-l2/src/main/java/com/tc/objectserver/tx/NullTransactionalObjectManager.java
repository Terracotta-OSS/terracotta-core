/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.objectserver.managedobject.ApplyTransactionInfo;

import java.util.Collection;

public class NullTransactionalObjectManager implements TransactionalObjectManager {

  @Override
  public void addTransactions(Collection<ServerTransaction> txns) {
    // Nop
  }

  @Override
  public void applyTransactionComplete(ApplyTransactionInfo applyTxnInfo) {
    // Nop
  }

  @Override
  public void lookupObjectsForTransactions() {
    // Nop
  }
}
