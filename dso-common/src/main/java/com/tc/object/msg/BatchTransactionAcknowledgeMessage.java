/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.tx.TxnBatchID;

public interface BatchTransactionAcknowledgeMessage extends TCMessage {

  public void initialize(TxnBatchID id);

  public TxnBatchID getBatchID();

}