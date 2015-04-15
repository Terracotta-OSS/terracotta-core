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

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.PostInit;
import com.tc.net.NodeID;
import com.tc.objectserver.core.api.ServerConfigurationContext;

public class PassThruTransactionFilter implements TransactionFilter, PostInit {

  private TransactionBatchManager transactionBatchManager;

  @Override
  public void initializeContext(final ConfigurationContext context) {
    final ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.transactionBatchManager = scc.getTransactionBatchManager();
  }

  @Override
  public void addTransactionBatch(final TransactionBatchContext transactionBatchContext) {
    this.transactionBatchManager.processTransactions(transactionBatchContext);
  }

  @Override
  public boolean shutdownNode(final NodeID nodeID) {
    return true;
  }

  @Override
  public void notifyServerHighWaterMark(final NodeID nodeID, final long serverHighWaterMark) {
    // NOP
  }
}