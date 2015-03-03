package com.tc.entity;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.tx.TransactionID;

/**
 * @author twu
 */
public interface RequestResponseMessage extends TCMessage {

  void setResponse(TransactionID id, Exception e);
  
  void setResponse(TransactionID id, byte[] value);

  void setResponse(TransactionID id);
  
  TransactionID getTransactionID();
  
  byte[] getResponseValue();
  
  Exception getException();
}
