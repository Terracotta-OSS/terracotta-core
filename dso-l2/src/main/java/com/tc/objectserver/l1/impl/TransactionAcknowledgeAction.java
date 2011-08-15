/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.l1.impl;

import com.tc.object.tx.ServerTransactionID;

/**
 * @author steve
 */
public interface TransactionAcknowledgeAction {
  public void acknowledgeTransaction(ServerTransactionID stxID);
}