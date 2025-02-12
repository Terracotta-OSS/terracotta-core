/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.object.net;

import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.stats.counter.Counter;

public interface ChannelStats {

  public static final String READ_RATE                     = "readRate";
  public static final String WRITE_RATE                    = "writeRate";
  public static final String TXN_RATE                      = "transactionRate";
  public static final String PENDING_TRANSACTIONS          = "pendingTransactions";
  public static final String SERVER_MAP_GET_VALUE_REQUESTS = "serverMapGetValueRequests";
  public static final String SERVER_MAP_GET_SIZE_REQUESTS  = "serverMapGetSizeRequests";

  public Counter getCounter(MessageChannel channel, String name);

  public void notifyTransaction(NodeID nodeID, int numTxns);

  void notifyReadOperations(MessageChannel channel, int numObjectsRequested);

  public void notifyTransactionBroadcastedTo(NodeID nodeID);

  public void notifyTransactionAckedFrom(NodeID nodeID);

}
