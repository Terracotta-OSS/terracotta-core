/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
