/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.tx;

public interface TransactionCompleteListener {

  public void transactionComplete(TransactionID txnID);
}
