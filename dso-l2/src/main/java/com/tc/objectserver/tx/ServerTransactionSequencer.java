/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.objectserver.context.TransactionLookupContext;

import java.util.Collection;

public interface ServerTransactionSequencer {

  public void addTransactionLookupContexts(Collection<TransactionLookupContext> txnLookupContexts);

  public TransactionLookupContext getNextTxnLookupContextToProcess();

  public void makePending(ServerTransaction txn);

  public void makeUnpending(ServerTransaction txn);

}