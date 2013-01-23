/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;


import com.tc.async.api.EventContext;
import com.tc.async.impl.MockSink;
import com.tc.objectserver.context.LookupEventContext;

public class TestTransactionalStageCoordinator implements TransactionalStageCoordinator {

  public MockSink lookupSink        = new MockSink();
  public MockSink applySink         = new MockSink();

  @Override
  public void addToApplyStage(EventContext context) {
    applySink.add(context);
  }

  @Override
  public void initiateLookup() {
    lookupSink.addLossy(new LookupEventContext());
  }

}