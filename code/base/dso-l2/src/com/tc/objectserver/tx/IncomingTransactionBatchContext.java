/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.NodeID;
import com.tc.object.msg.CommitTransactionMessage;

public class IncomingTransactionBatchContext implements TransactionBatchContext {

  private final CommitTransactionMessage ctm;
  private final TransactionBatchReader   reader;

  public IncomingTransactionBatchContext(CommitTransactionMessage ctm, TransactionBatchReader reader) {
    this.ctm = ctm;
    this.reader = reader;
  }

  public CommitTransactionMessage getCommitTransactionMessage() {
    return ctm;
  }

  public TransactionBatchReader getTransactionReader() {
    return reader;
  }

  public NodeID getSourceNodeID() {
    return ctm.getSourceNodeID();
  }

  public long[] getHighWatermark() {
    return reader.getHighWatermark();
  }

}
