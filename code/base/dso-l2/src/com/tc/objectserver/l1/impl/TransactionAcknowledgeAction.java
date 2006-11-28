/*
 * Created on Sep 14, 2004
 */
package com.tc.objectserver.l1.impl;

import com.tc.object.tx.ServerTransactionID;

/**
 * @author steve
 */
public interface TransactionAcknowledgeAction {
  public void acknowledgeTransaction(ServerTransactionID stxID);
}