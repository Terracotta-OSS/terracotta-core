/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.async.api.EventContext;
import com.tc.object.msg.CommitTransactionMessageImpl;

import java.io.IOException;
import java.util.HashSet;

public final class CommitTransactionMessageToTransactionBatchReader implements TransactionBatchReaderFactory {

  public TransactionBatchReader newTransactionBatchReader(EventContext ctxt) throws IOException {
    CommitTransactionMessageImpl ctm = (CommitTransactionMessageImpl) ctxt;
    return new TransactionBatchReaderImpl(ctm.getBatchData(), ctm.getChannelID(), ctm
        .addAcknowledgedTransactionIDsTo(new HashSet()), ctm.getSerializer());
  }
}