/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.Collection;
import java.util.List;

public class TestBatchedTransactionProcessor implements BatchedTransactionProcessor {

  public NoExceptionLinkedQueue shutdownClientCalls = new NoExceptionLinkedQueue();

  public void addTransactions(ChannelID channelID, List txns, Collection completedTxnIds) {
    // NOP
  }

  public void processTransactions(EventContext context, Sink applyChangesSink) {
    // NOP
  }

  public void shutDownClient(ChannelID channelID) {
    shutdownClientCalls.put(channelID);
  }

}
