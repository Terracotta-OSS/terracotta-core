/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

public class TransactionState {
  private static final int APPLY_STARTED       = 0x01;
  private static final int APPLY_COMMITTED     = 0x02;
  private static final int BROADCAST_COMPLETED = 0x04;

  private int              state               = 0x00;

  public void applySkipped() {
    applyStarted();
  }

  public void applyStarted() {
    state |= APPLY_STARTED;
  }

  public boolean isComplete() {
    return (state == (APPLY_COMMITTED | APPLY_STARTED | BROADCAST_COMPLETED));
  }

  public void broadcastCompleted() {
    state |= BROADCAST_COMPLETED;
  }

  public void applyCommitted() {
    state |= APPLY_COMMITTED;
  }

  // To keep the curly braces balanced {
  public String toString() {
    return "TransactionState = { " + ((state & APPLY_STARTED) == APPLY_STARTED ? " APPLY_STARTED : " : " : ")
           + ((state & APPLY_COMMITTED) == APPLY_COMMITTED ? " APPLY_COMMITED : " : " : ")
           + ((state & BROADCAST_COMPLETED) == BROADCAST_COMPLETED ? " BROADCAST_COMPLETE } " : " }");
  }

}