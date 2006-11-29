/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.l1.impl;

import com.tc.object.tx.ServerTransactionID;

/**
 * @author steve
 */
public interface TransactionAcknowledgeAction {
  public void acknowledgeTransaction(ServerTransactionID stxID);
}