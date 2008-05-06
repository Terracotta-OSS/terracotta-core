/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.msg.CommitTransactionMessage;
import com.tc.objectserver.tx.ServerTransaction;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class RelayedCommitTransactionMessageFactory {

  public static RelayedCommitTransactionMessage createRelayedCommitTransactionMessage(
                                                                                      CommitTransactionMessage commitMsg,
                                                                                      Collection txns, long seqID,
                                                                                      GlobalTransactionID lowWaterMark) {
    RelayedCommitTransactionMessage msg = new RelayedCommitTransactionMessage(commitMsg.getClientID(), commitMsg
        .getBatchData(), commitMsg.getSerializer(), getGlobalTransactionIDMapping(txns), seqID, lowWaterMark);
    return msg;
  }

  private static Map getGlobalTransactionIDMapping(Collection txns) {
    Map sid2gid = new HashMap(txns.size());
    for (Iterator i = txns.iterator(); i.hasNext();) {
      ServerTransaction txn = (ServerTransaction) i.next();
      sid2gid.put(txn.getServerTransactionID(), txn.getGlobalTransactionID());
    }
    return sid2gid;
  }

}
