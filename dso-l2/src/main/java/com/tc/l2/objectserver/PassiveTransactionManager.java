/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.lang.Recyclable;
import com.tc.net.NodeID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.objectserver.tx.ServerTransaction;

import java.util.Collection;
import java.util.Set;

public interface PassiveTransactionManager {

  public void addCommitedTransactions(NodeID nodeID, Set txnIDs, Collection txns, Recyclable message);

  public void addObjectSyncTransaction(ServerTransaction txn);

  public void clearTransactionsBelowLowWaterMark(GlobalTransactionID lowGlobalTransactionIDWatermark);
}