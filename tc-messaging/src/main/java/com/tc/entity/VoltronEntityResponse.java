package com.tc.entity;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.tx.TransactionID;


/**
 * The generic super-interface for the entity response types since SEDA requires that each thread only process one type of message.
 * This means that the caller needs to down-cast to the specific sub-type, cased on getAckType.
 * In the future, it would be ideal to remove this in favor of a different SEDA implementation.
 */
public interface VoltronEntityResponse extends TCMessage {
  TransactionID getTransactionID();
  VoltronEntityMessage.Acks getAckType();
}
