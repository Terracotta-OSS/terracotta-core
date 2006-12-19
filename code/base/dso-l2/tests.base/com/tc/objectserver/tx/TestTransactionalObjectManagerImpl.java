/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.async.api.Sink;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.context.CommitTransactionContext;

import java.util.Collection;
import java.util.List;

public class TestTransactionalObjectManagerImpl implements TransactionalObjectManager {

  public void addTransactions(ChannelID channelID, List txns, Collection completedTxnIds) {
    //Nop
  }

  public void addTransactionsToCommit(CommitTransactionContext ctc) {
    //Nop
  }

  public boolean applyTransactionComplete(ServerTransactionID stxnID) {
    //Nop
    return false;
  }

  public void lookupObjectsForTransactions(Sink applyChangesSink) {
    //Nop
  }

}
