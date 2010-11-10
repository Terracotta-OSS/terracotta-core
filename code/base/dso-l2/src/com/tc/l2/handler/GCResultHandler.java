/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.l2.msg.GCResultMessage;
import com.tc.l2.objectserver.ReplicatedObjectManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TxnsInSystemCompletionListener;

public class GCResultHandler extends AbstractEventHandler {

  private static final TCLogger    logger = TCLogging.getLogger(GCResultHandler.class);

  private ReplicatedObjectManager  rObjectManager;
  private ServerTransactionManager transactionManager;
  private Sink                     gcResultSink;

  public void handleEvent(EventContext context) {
    if (context instanceof GCResultMessage) {
      GCResultMessage msg = (GCResultMessage) context;
       logger.info("Scheduling to process GC results when all current transactions are completed : " + msg);
      transactionManager.callBackOnTxnsInSystemCompletion(new GCResultCallback(msg, gcResultSink));
    } else {
      GCResultCallback callback = (GCResultCallback) context;
      rObjectManager.handleGCResult(callback.getGCResult());
    }
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.transactionManager = oscc.getTransactionManager();
    this.rObjectManager = oscc.getL2Coordinator().getReplicatedObjectManager();
    this.gcResultSink = oscc.getStage(ServerConfigurationContext.GC_RESULT_PROCESSING_STAGE).getSink();
  }

  private static final class GCResultCallback implements TxnsInSystemCompletionListener, EventContext {

    private final Sink            sink;
    private final GCResultMessage msg;

    public GCResultCallback(GCResultMessage msg, Sink sink) {
      this.msg = msg;
      this.sink = sink;
    }

    public GCResultMessage getGCResult() {
      return msg;
    }

    public void onCompletion() {
      sink.add(this);
    }
  }
}
