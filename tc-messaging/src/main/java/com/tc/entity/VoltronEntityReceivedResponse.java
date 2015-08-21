package com.tc.entity;

import com.tc.object.tx.TransactionID;


public interface VoltronEntityReceivedResponse extends VoltronEntityResponse {
  void setTransactionID(TransactionID id);
}
