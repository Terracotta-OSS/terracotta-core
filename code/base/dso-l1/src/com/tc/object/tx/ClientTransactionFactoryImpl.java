/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.lockmanager.api.LockID;
import com.tc.object.logging.RuntimeLogger;

/**
 * @author steve
 */
public class ClientTransactionFactoryImpl implements ClientTransactionFactory {
  private long                transactionID = 0;
  private final RuntimeLogger runtimeLogger;

  public ClientTransactionFactoryImpl(RuntimeLogger runtimeLogger) {
    this.runtimeLogger = runtimeLogger;
  }

  public ClientTransaction newInstance() {
    return new ClientTransactionImpl(nextTransactionID(), runtimeLogger);
  }

  public ClientTransaction newNullInstance(LockID id, TxnType type) {
    ClientTransaction tc = new NullClientTransaction(nextTransactionID());
    tc.setTransactionContext(new TransactionContextImpl(id, type));
    return tc;
  }

  private synchronized TransactionID nextTransactionID() {
    return new TransactionID(transactionID++);
  }
}