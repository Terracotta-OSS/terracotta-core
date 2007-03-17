/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.object.msg.CommitTransactionMessage;

public class RelayedCommitTransactionMessageFactory {

  public static RelayedCommitTransactionMessage createRelayedCommitTransactionMessage(CommitTransactionMessage commitMsg) {
    RelayedCommitTransactionMessage msg = new RelayedCommitTransactionMessage(commitMsg.getChannelID(), commitMsg
        .getBatchData(), commitMsg.getSerializer());
    return msg;
  }

}
