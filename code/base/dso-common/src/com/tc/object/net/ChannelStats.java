/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.net;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.stats.counter.Counter;

public interface ChannelStats {
  public static final String OBJECT_REQUEST_RATE = "objectRequestRate";
  public static final String OBJECT_FLUSH_RATE   = "objectFlushRate";
  public static final String TXN_RATE            = "transactionRate";

  public Counter getCounter(MessageChannel channel, String name);

  public void notifyTransaction(ChannelID channelID);
}
