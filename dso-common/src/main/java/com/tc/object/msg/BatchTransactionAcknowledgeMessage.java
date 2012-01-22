/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.tx.TxnBatchID;

public interface BatchTransactionAcknowledgeMessage extends TCMessage {

  public void initialize(TxnBatchID id);

  public TxnBatchID getBatchID();

}
