/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.async.api.EventContext;

public interface TransactionalStageCoordinator {

  public void addToApplyStage(EventContext context);

  public void initiateLookup();

}