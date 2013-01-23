/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.async.api.StageManager;
import com.tc.objectserver.context.LookupEventContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;


public class TransactionalStagesCoordinatorImpl implements TransactionalStageCoordinator {

  private Sink               lookupSink;
  private Sink               applySink;

  private final StageManager stageManager;

  public TransactionalStagesCoordinatorImpl(StageManager stageManager) {
    this.stageManager = stageManager;
  }

  public void lookUpSinks() {
    this.lookupSink = stageManager.getStage(ServerConfigurationContext.TRANSACTION_LOOKUP_STAGE).getSink();
    this.applySink = stageManager.getStage(ServerConfigurationContext.APPLY_CHANGES_STAGE).getSink();
  }

  @Override
  public void addToApplyStage(EventContext context) {
    applySink.add(context);
  }

  @Override
  public void initiateLookup() {
    lookupSink.addLossy(new LookupEventContext());
  }

}
