/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.net;

import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ServerMapRequestType;
import com.tc.stats.StatsConfig;
import com.tc.stats.counter.BoundedCounterConfig;
import com.tc.stats.counter.Counter;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.stats.counter.sampled.SampledCumulativeCounterConfig;

public interface ChannelStats {

  public static final String        OBJECT_REQUEST_RATE           = "objectRequestRate";
  public static final String        OBJECT_FLUSH_RATE             = "objectFlushRate";
  public static final String        TXN_RATE                      = "transactionRate";
  public static final String        PENDING_TRANSACTIONS          = "pendingTransactions";
  public static final String        SERVER_MAP_GET_VALUE_REQUESTS = "serverMapGetValueRequests";
  public static final String        SERVER_MAP_GET_SIZE_REQUESTS  = "serverMapGetSizeRequests";

  public static final StatsConfig[] STATS_CONFIG                  = new StatsConfig[] {
      new StatsConfig(OBJECT_REQUEST_RATE, new SampledCounterConfig(1, 300, true, 0L)),
      new StatsConfig(OBJECT_FLUSH_RATE, new SampledCounterConfig(1, 300, true, 0L)),
      new StatsConfig(TXN_RATE, new SampledCounterConfig(1, 300, true, 0L)),
      new StatsConfig(PENDING_TRANSACTIONS, new BoundedCounterConfig(0L, 0L, Long.MAX_VALUE)),
      new StatsConfig(SERVER_MAP_GET_SIZE_REQUESTS, new SampledCumulativeCounterConfig(1, 300, true, 0L)),
      new StatsConfig(SERVER_MAP_GET_VALUE_REQUESTS, new SampledCumulativeCounterConfig(1, 300, true, 0L)) };

  public Counter getCounter(MessageChannel channel, String name);

  public void notifyTransaction(NodeID nodeID, int numTxns);

  public void notifyObjectRemove(MessageChannel channel, int numObjectsRemoved);

  public void notifyObjectRequest(MessageChannel channel, int numObjectsRequested);

  public void notifyTransactionBroadcastedTo(NodeID nodeID);

  public void notifyTransactionAckedFrom(NodeID nodeID);
  
  public void notifyServerMapRequest(ServerMapRequestType type, MessageChannel channel, int numRequests);
}
