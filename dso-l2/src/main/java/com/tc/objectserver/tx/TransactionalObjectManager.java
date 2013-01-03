/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.objectserver.managedobject.ApplyTransactionInfo;

import java.util.Collection;

public interface TransactionalObjectManager {

  public void addTransactions(Collection<ServerTransaction> txns);

  public void lookupObjectsForTransactions();

  public void applyTransactionComplete(ApplyTransactionInfo applyTxnInfo);

}
