/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.lockmanager.impl;

import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.ServerThreadID;
import com.tc.objectserver.lockmanager.api.DeadlockChain;

/**
 * A portion of a deadlock chain
 */
class DeadlockChainImpl implements DeadlockChain {

  private final ServerThreadID waiter;
  private DeadlockChain        next;
  private final LockID         waitingOn;

  public DeadlockChainImpl(ServerThreadID threadID, LockID waitingOn) {
    this.waiter = threadID;
    this.waitingOn = waitingOn;
  }

  public ServerThreadID getWaiter() {
    return this.waiter;
  }

  public DeadlockChain getNextLink() {
    return this.next;
  }

  void setNextLink(DeadlockChain nextLink) {
    this.next = nextLink;
  }

  public LockID getWaitingOn() {
    return this.waitingOn;
  }

}
