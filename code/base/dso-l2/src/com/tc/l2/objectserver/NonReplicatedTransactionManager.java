/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.l2.context.StateChangedEvent;
import com.tc.lang.Recyclable;
import com.tc.net.NodeID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.objectserver.tx.ServerTransaction;

import java.util.Collection;
import java.util.Set;

public class NonReplicatedTransactionManager implements ReplicatedTransactionManager {

  public void addCommitedTransactions(NodeID nodeID, Set txnIDs, Collection txns, Recyclable message) {
    throw new AssertionError("Shouldn't be called");
  }

  public void addObjectSyncTransaction(ServerTransaction txn) {
    throw new AssertionError("Shouldn't be called");
  }

  public void goActive() {
    throw new AssertionError("Shouldn't be called");
  }

  public void publishResetRequest(NodeID nodeID) {
    throw new AssertionError("Shouldn't be called");
  }

  public void l2StateChanged(StateChangedEvent sce) {
    throw new AssertionError("Shouldn't be called");
  }

  public void init(Set knownObjectIDs) {
    throw new AssertionError("Shouldn't be called");
  }

  public void clearTransactionsBelowLowWaterMark(GlobalTransactionID lowGlobalTransactionIDWatermark) {
    throw new AssertionError("Shouldn't be called");
  }


}
