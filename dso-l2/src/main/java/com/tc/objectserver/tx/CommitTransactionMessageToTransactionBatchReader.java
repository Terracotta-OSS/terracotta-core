/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.l2.msg.RelayedCommitTransactionMessage;
import com.tc.object.msg.CommitTransactionMessage;
import com.tc.objectserver.core.api.DSOGlobalServerStats;

import java.io.IOException;

public final class CommitTransactionMessageToTransactionBatchReader implements TransactionBatchReaderFactory {

  private final ServerTransactionFactory activeTxnFactory  = new ActiveServerTransactionFactory();
  private final ServerTransactionFactory passiveTxnFactory = new PassiveServerTransactionFactory();
  private final DSOGlobalServerStats     globalSeverStats;

  public CommitTransactionMessageToTransactionBatchReader(final DSOGlobalServerStats globalSeverStats) {
    this.globalSeverStats = globalSeverStats;
  }

  // Used by active server
  public TransactionBatchReader newTransactionBatchReader(final CommitTransactionMessage ctm) throws IOException {
    return new TransactionBatchReaderImpl(ctm.getBatchData(), ctm.getSourceNodeID(), ctm.getSerializer(),
                                          this.activeTxnFactory, this.globalSeverStats);
  }

  // Used by passive server
  public TransactionBatchReader newTransactionBatchReader(final RelayedCommitTransactionMessage ctm) throws IOException {
    return new TransactionBatchReaderImpl(ctm.getBatchData(), ctm.getClientID(), ctm.getSerializer(),
                                          this.passiveTxnFactory, this.globalSeverStats);
  }
}