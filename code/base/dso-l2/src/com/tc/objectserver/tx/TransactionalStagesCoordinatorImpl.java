/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.async.api.Sink;
import com.tc.async.api.StageManager;
import com.tc.objectserver.context.ApplyCompleteEventContext;
import com.tc.objectserver.context.ApplyTransactionContext;
import com.tc.objectserver.context.CommitTransactionContext;
import com.tc.objectserver.context.LookupEventContext;
import com.tc.objectserver.context.RecallObjectsContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;

import java.util.Collections;

public class TransactionalStagesCoordinatorImpl implements TransactionalStageCoordinator {

  private Sink               lookupSink;
  private Sink               applySink;
  private Sink               commitSink;
  private Sink               applyCompleteSink;
  private Sink               recallSink;

  private final StageManager stageManager;

  public TransactionalStagesCoordinatorImpl(StageManager stageManager) {
    this.stageManager = stageManager;
  }

  public void lookUpSinks() {
    this.lookupSink = stageManager.getStage(ServerConfigurationContext.TRANSACTION_LOOKUP_STAGE).getSink();
    this.recallSink = stageManager.getStage(ServerConfigurationContext.RECALL_OBJECTS_STAGE).getSink();
    this.applySink = stageManager.getStage(ServerConfigurationContext.APPLY_CHANGES_STAGE).getSink();
    this.applyCompleteSink = stageManager.getStage(ServerConfigurationContext.APPLY_COMPLETE_STAGE).getSink();
    this.commitSink = stageManager.getStage(ServerConfigurationContext.COMMIT_CHANGES_STAGE).getSink();
  }

  public void addToApplyStage(ApplyTransactionContext context) {
    applySink.add(context);
  }

  public void initiateLookup() {
    lookupSink.addLossy(new LookupEventContext());
  }

  public void initiateApplyComplete() {
    applyCompleteSink.addLossy(new ApplyCompleteEventContext());
  }

  public void initiateCommit() {
    commitSink.addLossy(new CommitTransactionContext());
  }

  public void initiateRecallAll() {
    recallSink.add(new RecallObjectsContext(Collections.EMPTY_LIST, true));
  }

}
