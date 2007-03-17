/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.l2.msg.RelayedCommitTransactionMessage;
import com.tc.object.msg.CommitTransactionMessage;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;

public final class CommitTransactionMessageToTransactionBatchReader implements TransactionBatchReaderFactory {

  // Used by active server
  public TransactionBatchReader newTransactionBatchReader(CommitTransactionMessage ctm) throws IOException {
    return new TransactionBatchReaderImpl(ctm.getBatchData(), ctm.getChannelID(), ctm
        .addAcknowledgedTransactionIDsTo(new HashSet()), ctm.getSerializer(), false);
  }

  // Used by passive server
  public TransactionBatchReader newTransactionBatchReader(RelayedCommitTransactionMessage ctm) throws IOException {
    return new TransactionBatchReaderImpl(ctm.getBatchData(), ctm.getChannelID(), Collections.EMPTY_LIST, ctm
        .getSerializer(), true);
  }

}