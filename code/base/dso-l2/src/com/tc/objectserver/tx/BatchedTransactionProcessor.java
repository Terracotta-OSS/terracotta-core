/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.net.protocol.tcm.ChannelID;

import java.util.Collection;
import java.util.List;

public interface BatchedTransactionProcessor {

  public void addTransactions(ChannelID channelID, List txns, Collection completedTxnIds);

  public void processTransactions(EventContext context, Sink applyChangesSink);

  public void shutDownClient(ChannelID channelID);

}
