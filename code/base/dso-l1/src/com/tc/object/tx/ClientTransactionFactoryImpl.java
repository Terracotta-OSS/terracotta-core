/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import com.tc.net.protocol.tcm.ChannelIDProvider;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.logging.RuntimeLogger;

/**
 * @author steve
 */
public class ClientTransactionFactoryImpl implements ClientTransactionFactory {
  private long                transactionID = 0;
  private final RuntimeLogger runtimeLogger;
  private final ChannelIDProvider cidProvider;

  public ClientTransactionFactoryImpl(RuntimeLogger runtimeLogger, ChannelIDProvider cidProvider) {
    this.runtimeLogger = runtimeLogger;
    this.cidProvider = cidProvider;
  }

  public ClientTransaction newInstance() {
    return new ClientTransactionImpl(nextTransactionID(), runtimeLogger, cidProvider);
  }

  public ClientTransaction newNullInstance(LockID id, TxnType type) {
    ClientTransaction tc = new NullClientTransaction(nextTransactionID(), cidProvider);
    tc.setTransactionContext(new TransactionContextImpl(id, type, new LockID[] { id }));
    return tc;
  }

  private synchronized TransactionID nextTransactionID() {
    return new TransactionID(transactionID++);
  }
}