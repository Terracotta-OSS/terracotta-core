/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.msg.CompletedTransactionLowWaterMarkMessage;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.context.ServerTransactionCompleteContext;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;

public class TransactionLowWaterMarkHandler extends AbstractEventHandler {

  private final ServerGlobalTransactionManager gtxm;

  public TransactionLowWaterMarkHandler(ServerGlobalTransactionManager gtxm) {
    this.gtxm = gtxm;
  }

  @Override
  public void handleEvent(EventContext context) {
    if (context instanceof CompletedTransactionLowWaterMarkMessage) {
      CompletedTransactionLowWaterMarkMessage mdg = (CompletedTransactionLowWaterMarkMessage) context;
      ServerTransactionID sid = new ServerTransactionID(mdg.getSourceNodeID(), mdg.getLowWaterMark());
      gtxm.clearCommitedTransactionsBelowLowWaterMark(sid);
    } else if (context instanceof ServerTransactionCompleteContext) {
      gtxm.clearCommittedTransaction(((ServerTransactionCompleteContext) context).getServerTransactionID());
    } else {
      throw new IllegalArgumentException("Unknown context " + context);
    }
  }

}
