/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.tx.ServerTransactionID;

import java.util.Set;

public interface ServerTransactionListener {
  
  public void incomingTransactions(ChannelID cid, Set serverTxnIDs);
  
  public void transactionApplied(ServerTransactionID stxID);
  
  public void transactionCompleted(ServerTransactionID stxID);
  
}
