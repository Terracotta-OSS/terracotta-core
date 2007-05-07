/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionalObjectManager;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ReplicatedTransactionManagerImpl implements ReplicatedTransactionManager {

  private final ServerTransactionManager   transactionManager;
  private final TransactionalObjectManager txnObjectManager;

  public ReplicatedTransactionManagerImpl(ServerTransactionManager transactionManager,
                                          TransactionalObjectManager txnObjectManager) {
    this.transactionManager = transactionManager;
    this.txnObjectManager = txnObjectManager;
  }

  public void addCommitTransactionMessage(ChannelID channelID, Set txnIDs, Collection txns, Collection completedTxnIDs) {
    transactionManager.incomingTransactions(channelID, txnIDs, txns, false);
    txnObjectManager.addTransactions(txns, completedTxnIDs);
  }

  public void addObjectSyncTransaction(ServerTransaction txn) {
    Map txns = new LinkedHashMap(1);
    txns.put(txn.getServerTransactionID(), txn);
    transactionManager.incomingTransactions(ChannelID.L2_SERVER_ID, txns.keySet(), txns.values(), false);
    txnObjectManager.addTransactions(txns.values(), Collections.EMPTY_LIST);
  }
}
