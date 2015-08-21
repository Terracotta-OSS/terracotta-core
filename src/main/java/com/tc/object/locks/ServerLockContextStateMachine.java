/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.object.locks.ServerLockContext.State;
import com.tc.object.locks.ServerLockContext.Type;
import com.tc.util.Assert;

public class ServerLockContextStateMachine {
  // State diagram
  // (pending, try pending) ==> (greedy holder, holder)
  // (holder) ==> (waiter) ==> (pending)

  public boolean canSetState(State oldState, State newState) {
    State expectedState = null;

    if (oldState == null) { return true; }

    switch (newState.getType()) {
      case GREEDY_HOLDER:
        expectedState = moveToGreedy(oldState);
        break;
      case HOLDER:
        expectedState = moveToHolder(oldState);
        break;
      case PENDING:
        expectedState = moveToPending(oldState);
        break;
      case WAITER:
        expectedState = moveToWaiter(oldState);
        break;
      case TRY_PENDING:
        return true;
    }

    if (expectedState == newState) { return true; }

    return false;
  }

  private State moveToWaiter(State oldState) {
    Assert.assertNotNull(oldState);
    Assert.assertTrue(oldState.getType() == Type.HOLDER);
    Assert.assertTrue(oldState.getLockLevel() == ServerLockLevel.WRITE);
    switch (oldState.getLockLevel()) {
      case WRITE:
        return State.WAITER;
      default:
        // should never come here
        throw new IllegalStateException("Should never come here");
    }
  }

  private State moveToGreedy(State oldState) {
    Assert.assertNotNull(oldState);
    Assert.assertTrue(oldState.getType() == Type.PENDING || oldState.getType() == Type.TRY_PENDING
                      || oldState.getType() == Type.WAITER);
    switch (oldState.getLockLevel()) {
      case READ:
        return State.GREEDY_HOLDER_READ;
      case WRITE:
        return State.GREEDY_HOLDER_WRITE;
      default:
        // should never come here
        throw new IllegalStateException("Should never come here");
    }
  }

  private State moveToHolder(State oldState) {
    Assert.assertNotNull(oldState);
    Assert.assertTrue(oldState.getType() == Type.PENDING || oldState.getType() == Type.TRY_PENDING
                      || oldState.getType() == Type.WAITER);
    switch (oldState.getLockLevel()) {
      case READ:
        return State.HOLDER_READ;
      case WRITE:
        return State.HOLDER_WRITE;
      default:
        // should never come here
        throw new IllegalStateException("Should never come here");
    }
  }

  private State moveToPending(State oldState) {
    Assert.assertNotNull(oldState);
    Assert.assertTrue(oldState.getType() == Type.WAITER);
    switch (oldState.getLockLevel()) {
      case READ:
        return State.PENDING_READ;
      case WRITE:
        return State.PENDING_WRITE;
      default:
        // should never come here
        throw new IllegalStateException("Should never come here");
    }
  }
}
