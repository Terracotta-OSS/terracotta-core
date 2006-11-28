/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.object.tx.TxnBatchID;

public interface BatchTransactionAcknowledgeMessage {

  public void initialize(TxnBatchID id);

  public TxnBatchID getBatchID();

  public void send();

}