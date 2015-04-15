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
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.net.ClientID;
import com.tc.object.tx.TransactionID;

import java.util.Set;

public class SyncWriteTransactionReceivedContext implements EventContext {
  private final long               batchID;
  private final ClientID           cid;
  private final Set<TransactionID> txIdSet;

  public SyncWriteTransactionReceivedContext(long batchID, ClientID cid, Set<TransactionID> set) {
    this.batchID = batchID;
    this.cid = cid;
    this.txIdSet = set;
  }

  public long getBatchID() {
    return batchID;
  }

  public ClientID getClientID() {
    return cid;
  }

  public Set<TransactionID> getSyncTransactions() {
    return txIdSet;
  }
}
