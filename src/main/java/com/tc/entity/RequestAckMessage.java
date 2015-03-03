package com.tc.entity;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.tx.TransactionID;

/**
 * @author twu
 */
public interface RequestAckMessage extends TCMessage {
  void setTransactionID(TransactionID id);
  
  TransactionID getTransactionID();
}
