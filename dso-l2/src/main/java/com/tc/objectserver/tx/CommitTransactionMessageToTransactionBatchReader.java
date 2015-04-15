/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.tx;

import com.tc.l2.msg.RelayedCommitTransactionMessage;
import com.tc.object.msg.CommitTransactionMessage;
import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.objectserver.tx.TransactionBatchReaderImpl.TransactionSizeCounterCallback;

import java.io.IOException;

public final class CommitTransactionMessageToTransactionBatchReader implements TransactionBatchReaderFactory {

  private final ServerTransactionFactory activeTxnFactory  = new ActiveServerTransactionFactory();
  private final ServerTransactionFactory passiveTxnFactory = new PassiveServerTransactionFactory();
  private final TransactionSizeCounterCallback transactionSizeCounterCallback;

  public CommitTransactionMessageToTransactionBatchReader(final DSOGlobalServerStats globalSeverStats) {
    this.transactionSizeCounterCallback = globalSeverStats == null ? null : newCallback(globalSeverStats);
  }

  private TransactionSizeCounterCallback newCallback(final DSOGlobalServerStats globalSeverStats) {
    return new TransactionSizeCounterCallback() {
      @Override
      public void increment(long numerator, long denominator) {
        globalSeverStats.getTransactionSizeCounter().increment(numerator, denominator);
      }
    };
  }

  // Used by active server
  @Override
  public TransactionBatchReader newTransactionBatchReader(final CommitTransactionMessage ctm) throws IOException {
    return new TransactionBatchReaderImpl(ctm.getBatchData(), ctm.getSourceNodeID(), ctm.getSerializer(),
                                          this.activeTxnFactory, this.transactionSizeCounterCallback);
  }

  // Used by passive server
  @Override
  public TransactionBatchReader newTransactionBatchReader(final RelayedCommitTransactionMessage ctm) throws IOException {
    return new TransactionBatchReaderImpl(ctm.getBatchData(), ctm.getClientID(), ctm.getSerializer(),
                                          this.passiveTxnFactory, this.transactionSizeCounterCallback);
  }
}