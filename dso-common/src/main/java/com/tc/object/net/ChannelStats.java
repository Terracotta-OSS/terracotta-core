/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.object.net;

import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ServerMapRequestType;
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

  public void notifyServerMapRequest(ServerMapRequestType type, MessageChannel channel, int numRequests);
}
