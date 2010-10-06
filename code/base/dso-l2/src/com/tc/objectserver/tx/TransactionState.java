/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

public class TransactionState {
  private static final int             APPLY_COMMITTED         = 0x01;
  private static final int             BROADCAST_COMPLETED     = 0x02;
  private static final int             TXN_RELAYED             = 0x04;

  private static final int             TXN_PROCESSING_COMPLETE = (APPLY_COMMITTED | BROADCAST_COMPLETED | TXN_RELAYED);

  public static final TransactionState COMPLETED_STATE         = new TransactionState(TXN_PROCESSING_COMPLETE);

  private int                          state;

  public TransactionState() {
    this(0x00);
  }

  private TransactionState(final int state) {
    this.state = state;
  }

  public void applyAndCommitSkipped() {
    this.state |= APPLY_COMMITTED;
  }

  public boolean isComplete() {
    return (this.state == TXN_PROCESSING_COMPLETE);
  }

  public void broadcastCompleted() {
    this.state |= BROADCAST_COMPLETED;
  }

  public void applyCommitted() {
    this.state |= APPLY_COMMITTED;
  }

  @Override
  public String toString() {
    return "TransactionState = [ " + ((this.state & APPLY_COMMITTED) == APPLY_COMMITTED ? " APPLY_COMMITED : " : " : ")
           + ((this.state & TXN_RELAYED) == TXN_RELAYED ? " TXN_RELAYED : " : " : ")
           + ((this.state & BROADCAST_COMPLETED) == BROADCAST_COMPLETED ? " BROADCAST_COMPLETE } " : " ]");
  }

  public void relayTransactionComplete() {
    this.state |= TXN_RELAYED;
  }

}