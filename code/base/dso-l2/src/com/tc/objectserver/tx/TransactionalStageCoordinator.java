/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.objectserver.context.ApplyTransactionContext;

public interface TransactionalStageCoordinator {

  public void addToApplyStage(ApplyTransactionContext context);

  public void initiateLookup();

  public void initiateApplyComplete();

  public void initiateCommit();

  public void initiateRecallAll();

}