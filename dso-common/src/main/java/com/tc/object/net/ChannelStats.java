/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.net;

import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ServerMapRequestType;
import com.tc.stats.counter.Counter;

public interface ChannelStats {

  public static final String OBJECT_REQUEST_RATE           = "objectRequestRate";
  public static final String OBJECT_FLUSH_RATE             = "objectFlushRate";
  public static final String TXN_RATE                      = "transactionRate";
  public static final String PENDING_TRANSACTIONS          = "pendingTransactions";
  public static final String SERVER_MAP_GET_VALUE_REQUESTS = "serverMapGetValueRequests";
  public static final String SERVER_MAP_GET_SIZE_REQUESTS  = "serverMapGetSizeRequests";

  public Counter getCounter(MessageChannel channel, String name);

  public void notifyTransaction(NodeID nodeID, int numTxns);

  public void notifyObjectRemove(MessageChannel channel, int numObjectsRemoved);

  public void notifyObjectRequest(MessageChannel channel, int numObjectsRequested);

  public void notifyTransactionBroadcastedTo(NodeID nodeID);

  public void notifyTransactionAckedFrom(NodeID nodeID);

  public void notifyServerMapRequest(ServerMapRequestType type, MessageChannel channel, int numRequests);
}
