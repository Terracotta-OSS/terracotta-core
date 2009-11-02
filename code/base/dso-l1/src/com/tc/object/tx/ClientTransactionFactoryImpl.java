/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.locks.LockID;
import com.tc.object.logging.RuntimeLogger;

/**
 * Creates ClientTransactions
 */
public class ClientTransactionFactoryImpl implements ClientTransactionFactory {
  private final RuntimeLogger runtimeLogger;

  public ClientTransactionFactoryImpl(RuntimeLogger runtimeLogger) {
    this.runtimeLogger = runtimeLogger;
  }

  public ClientTransaction newInstance() {
    return new ClientTransactionImpl(runtimeLogger);
  }

  public ClientTransaction newNullInstance(final LockID id, final TxnType type) {
    ClientTransaction tc = new NullClientTransaction();
    tc.setTransactionContext(new TransactionContextImpl(id, type, type));
    return tc;
  }

}