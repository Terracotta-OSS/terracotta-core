/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.context;

import com.tc.async.api.EventContext;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.msg.CommitTransactionMessage;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class IncomingTransactionContext implements EventContext {

  private final CommitTransactionMessage ctm;
  private final Collection               txns;
  private final Set                      serverTxnIDs;
  private final ChannelID                channelID;

  public IncomingTransactionContext(ChannelID channelID, CommitTransactionMessage ctm, Map txnsMap) {
    this.channelID = channelID;
    this.ctm = ctm;
    this.txns = txnsMap.values();
    this.serverTxnIDs = txnsMap.keySet();
  }

  public CommitTransactionMessage getCommitTransactionMessage() {
    return ctm;
  }

  public Set getServerTransactionIDs() {
    return serverTxnIDs;
  }

  public Collection getTxns() {
    return txns;
  }

  public ChannelID getChannelID() {
    return channelID;
  }

}
