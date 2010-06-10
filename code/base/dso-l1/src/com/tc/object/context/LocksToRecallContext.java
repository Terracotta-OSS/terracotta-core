/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.context;

import com.tc.async.api.EventContext;
import com.tc.object.locks.LockID;

import java.util.Set;

public class LocksToRecallContext implements EventContext {

  private final Set<LockID> toRecall;

  public LocksToRecallContext(final Set<LockID> toRecall) {
    this.toRecall = toRecall;
  }

  public Set<LockID> getLocksToRecall() {
    return this.toRecall;
  }

}
