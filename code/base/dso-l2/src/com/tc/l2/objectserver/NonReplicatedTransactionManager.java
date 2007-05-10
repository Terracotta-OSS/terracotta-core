/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.exception.ImplementMe;
import com.tc.net.groups.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.objectserver.tx.ServerTransaction;

import java.util.Collection;
import java.util.Set;

public class NonReplicatedTransactionManager implements ReplicatedTransactionManager {

  public void addCommitTransactionMessage(ChannelID channelID, Set txnIDs, Collection txns, Collection completedTxnIDs) {
    throw new ImplementMe();
  }

  public void addObjectSyncTransaction(ServerTransaction txn) {
    throw new ImplementMe();
  }

  public void goActive() {
    throw new ImplementMe();
  }

  public void publishResetRequest(NodeID nodeID) {
    throw new ImplementMe();
  }

}
