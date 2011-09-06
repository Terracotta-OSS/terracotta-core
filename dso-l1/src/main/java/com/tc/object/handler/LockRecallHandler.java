/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.context.LocksToRecallContext;
import com.tc.object.locks.ClientLockManager;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ServerLockLevel;

import java.util.Set;

public class LockRecallHandler extends AbstractEventHandler implements LockRecaller {

  private ClientLockManager lockManager;

  @Override
  public void handleEvent(final EventContext context) {
    final LocksToRecallContext recallContext = (LocksToRecallContext) context;
    recallLocksInline(recallContext.getLocksToRecall());
  }

  public void recallLocksInline(final Set<LockID> locks) {
    for (final LockID lock : locks) {
      this.lockManager.recall(null, null, lock, ServerLockLevel.WRITE, -1, true);
    }
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    super.initialize(context);
    final ClientConfigurationContext ccc = (ClientConfigurationContext) context;
    this.lockManager = ccc.getLockManager();
  }

}
