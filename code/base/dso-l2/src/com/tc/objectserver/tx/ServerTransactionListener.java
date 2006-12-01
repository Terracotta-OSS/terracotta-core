/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.tx.ServerTransactionID;

import java.util.Collection;

public interface ServerTransactionListener {
  
  public void addResentServerTransactionIDs(Collection sTxIDs);
  
  public void transactionApplied(ServerTransactionID stxID);
  
  public void transactionCompleted(ServerTransactionID stxID);
  
  public void clearAllTransactionsFor(ChannelID client);

}
