/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.msg.CommitTransactionMessage;
import com.tc.objectserver.tx.TransactionBatchManager;

public class ProcessTransactionHandler extends AbstractEventHandler {

  private final TransactionBatchManager transactionBatchManager;

  public ProcessTransactionHandler(TransactionBatchManager transactionBatchManager) {
    this.transactionBatchManager = transactionBatchManager;
  }

  public void handleEvent(EventContext context) {
    final CommitTransactionMessage ctm = (CommitTransactionMessage) context;
    transactionBatchManager.addTransactionBatch(ctm);
  }

}
