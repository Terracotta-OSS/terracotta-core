/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.l2.msg.RelayedCommitTransactionMessage;
import com.tc.object.msg.CommitTransactionMessage;

import java.io.IOException;

public interface TransactionBatchReaderFactory {
  public TransactionBatchReader newTransactionBatchReader(CommitTransactionMessage ctxt)
      throws IOException;

  public TransactionBatchReader newTransactionBatchReader(RelayedCommitTransactionMessage commitMessage)
      throws IOException;
}
