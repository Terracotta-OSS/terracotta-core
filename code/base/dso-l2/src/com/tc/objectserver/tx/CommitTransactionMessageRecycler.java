/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.msg.MessageRecyclerImpl;
import com.tc.object.tx.ServerTransactionID;

import java.util.Collection;
import java.util.Set;

public class CommitTransactionMessageRecycler extends MessageRecyclerImpl implements ServerTransactionListener {

  public CommitTransactionMessageRecycler(ServerTransactionManager transactionManager) {
    transactionManager.addTransactionListener(this);
  }

  public void transactionCompleted(ServerTransactionID stxID) {
    recycle(stxID);
  }

  public void transactionApplied(ServerTransactionID stxID) {
    return;
  }

  public void incomingTransactions(ChannelID cid, Set serverTxnIDs) {
    return;
  }

  public void addResentServerTransactionIDs(Collection stxIDs) {
    return;
  }

  public void clearAllTransactionsFor(ChannelID killedClient) {
    return;
  }

}
