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
package com.tc.objectserver.tx;

import com.tc.net.NodeID;

public interface TransactionFilter {

  public void addTransactionBatch(TransactionBatchContext transactionBatchContext);

  /**
   * The Filter returns true if the node can be disconnected immediately and the rest of the managers can be notified of
   * disconnect immediately. If not the filter calls back at a later time when it deems good.
   */
  public boolean shutdownNode(NodeID nodeID);

  public void notifyServerHighWaterMark(NodeID nodeID, long serverHighWaterMark);

}
