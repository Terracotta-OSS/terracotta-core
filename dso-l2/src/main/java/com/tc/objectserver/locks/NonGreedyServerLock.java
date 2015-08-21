/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks;

import com.tc.object.locks.LockID;
import com.tc.object.locks.ServerLockContext;
import com.tc.object.locks.ServerLockLevel;

import java.util.List;

public final class NonGreedyServerLock extends AbstractServerLock {
  public NonGreedyServerLock(LockID lockID) {
    super(lockID);
  }

  @Override
  protected void processPendingRequests(LockHelper helper) {
    ServerLockContext request = getNextRequestIfCanAward(helper);
    if (request == null) { return; }

    ServerLockLevel lockLevel = request.getState().getLockLevel();
    switch (lockLevel) {
      case READ:
        add(request, helper);
        awardAllReads(helper, request);
        break;
      case WRITE:
        awardLock(helper, request);
        break;
      default:
        throw new AssertionError(lockLevel);
    }
  }

  private void awardAllReads(LockHelper helper, ServerLockContext request) {
    List<ServerLockContext> contexts = removeAllPendingReadRequests(helper);

    for (ServerLockContext context : contexts) {
      awardLock(helper, context);
    }
  }
}
