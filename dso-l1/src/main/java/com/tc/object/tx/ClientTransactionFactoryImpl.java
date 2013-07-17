/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.locks.LockID;

/**
 * Creates ClientTransactions
 */
public class ClientTransactionFactoryImpl implements ClientTransactionFactory {

  public ClientTransactionFactoryImpl() {
    super();
  }

  @Override
  public ClientTransaction newInstance(int session) {
    return new ClientTransactionImpl(session);
  }

  @Override
  public ClientTransaction newNullInstance(final LockID id, final TxnType type) {
    ClientTransaction tc = new NullClientTransaction();
    tc.setTransactionContext(new TransactionContextImpl(id, type, type));
    return tc;
  }

}
