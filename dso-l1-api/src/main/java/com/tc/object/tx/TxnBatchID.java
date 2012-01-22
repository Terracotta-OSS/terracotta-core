/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.util.AbstractIdentifier;

/**
 * Transaction batch identifier
 */
public class TxnBatchID extends AbstractIdentifier {

  public static final TxnBatchID NULL_BATCH_ID = new TxnBatchID();

  private TxnBatchID() {
    super();
  }
  
  public TxnBatchID(long batchID) {
    super(batchID);
  }

  public String getIdentifierType() {
    return "TxnBatchID";
  }

}
