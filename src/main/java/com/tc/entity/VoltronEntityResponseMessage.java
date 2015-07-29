package com.tc.entity;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.tx.TransactionID;


public interface VoltronEntityResponseMessage extends TCMessage {
  // Read-only interface for the receiver.
  TransactionID getTransactionID();
  byte[] getSuccessValue();
  Exception getFailureException();
  
  // Writable interface for the sender.
  public void setSuccess(TransactionID transactionID, byte[] response);
  public void setFailure(TransactionID transactionID, Exception exception);
}
