/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

public class TransactionState {
  private static final int APPLY_STARTED           = 0x01;
  private static final int APPLY_COMMITTED         = 0x02;
  private static final int BROADCAST_COMPLETED     = 0x04;
  private static final int TXN_RELAYED             = 0x08;

  private static final int TXN_PROCESSING_COMPLETE = (APPLY_COMMITTED | APPLY_STARTED | BROADCAST_COMPLETED | TXN_RELAYED);

  private int              state                   = 0x00;

  public void applyAndCommitSkipped() {
    state |= APPLY_STARTED | APPLY_COMMITTED;
  }

  public void applyStarted() {
    state |= APPLY_STARTED;
  }

  public boolean isComplete() {
    return (state == TXN_PROCESSING_COMPLETE);
  }

  public void broadcastCompleted() {
    state |= BROADCAST_COMPLETED;
  }

  public void applyCommitted() {
    state |= APPLY_COMMITTED;
  }

  public String toString() {
    return "TransactionState = [ " + ((state & APPLY_STARTED) == APPLY_STARTED ? " APPLY_STARTED : " : " : ")
           + ((state & APPLY_COMMITTED) == APPLY_COMMITTED ? " APPLY_COMMITED : " : " : ")
           + ((state & TXN_RELAYED) == TXN_RELAYED ? " TXN_RELAYED : " : " : ")
           + ((state & BROADCAST_COMPLETED) == BROADCAST_COMPLETED ? " BROADCAST_COMPLETE } " : " ]");
  }

  public void relayTransactionComplete() {
    state |= TXN_RELAYED;
  }

}