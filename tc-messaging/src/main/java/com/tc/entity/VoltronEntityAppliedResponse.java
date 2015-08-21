package com.tc.entity;

import com.tc.object.tx.TransactionID;


public interface VoltronEntityAppliedResponse extends VoltronEntityResponse {
  // Read-only interface for the receiver.
  byte[] getSuccessValue();
  Exception getFailureException();
  
  // Writable interface for the sender.
  public void setSuccess(TransactionID transactionID, byte[] response);
  public void setFailure(TransactionID transactionID, Exception exception);
}
