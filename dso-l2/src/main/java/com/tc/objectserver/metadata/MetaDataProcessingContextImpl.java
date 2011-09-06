/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.metadata;

import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.tx.ServerTransactionManager;

public class MetaDataProcessingContextImpl implements MetaDataProcessingContext {

  private static final int               NOT_SET   = -1;

  private final ServerTransactionID      txnID;
  private final ServerTransactionManager txnManager;
  private int                            processed = 0;
  private int                            expected  = NOT_SET;

  public MetaDataProcessingContextImpl(ServerTransactionID txnID, ServerTransactionManager txnManager) {
    this.txnID = txnID;
    this.txnManager = txnManager;
  }

  public synchronized void metaDataProcessed() {
    if (isCountSet()) {
      if (processed + 1 > expected) {
        //
        throw new AssertionError("Exceeded expected count (" + expected + ") for " + txnID);
      }
    }

    processed++;
    attemptFinish();
  }

  public synchronized void setExpectedCount(int count) {
    if (count < 0) throw new AssertionError("invalid count (" + count + ") for " + txnID);

    if (isCountSet()) { throw new AssertionError("expected already set to " + expected + " for " + txnID); }

    this.expected = count;
    attemptFinish();
  }

  private boolean isCountSet() {
    return expected != NOT_SET;
  }

  private void attemptFinish() {
    if (isCountSet() && expected == processed) {
      txnManager.processingMetaDataCompleted(txnID.getSourceID(), txnID.getClientTransactionID());
    }
  }
}
