/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.tx.ServerTransactionID;

import java.util.Collection;
import java.util.List;

public class TestTransactionalObjectManagerImpl implements TransactionalObjectManager {

  public void addTransactions(ChannelID channelID, List txns, Collection completedTxnIds) {
    // Nop
  }

  public boolean applyTransactionComplete(ServerTransactionID stxnID) {
    // Nop
    return false;
  }

  public void lookupObjectsForTransactions() {
    // Nop
  }

  public void commitTransactionsComplete(Collection txns) {
    // Nop
  }

}
