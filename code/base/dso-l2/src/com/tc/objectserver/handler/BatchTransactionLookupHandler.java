/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.tx.BatchedTransactionProcessor;

public class BatchTransactionLookupHandler extends AbstractEventHandler {

  private Sink                        applyChangesSink;
  private BatchedTransactionProcessor txnProcessor;

  public void handleEvent(EventContext context) {
    this.txnProcessor.processTransactions(context, applyChangesSink);
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    applyChangesSink = context.getStage(ServerConfigurationContext.APPLY_CHANGES_STAGE).getSink();
    txnProcessor = oscc.getBatchedTransactionProcessor();
  }

}
