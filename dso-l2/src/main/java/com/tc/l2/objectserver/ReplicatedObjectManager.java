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
package com.tc.l2.objectserver;

import com.tc.l2.msg.GCResultMessage;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupException;
import com.tc.objectserver.tx.TransactionBatchContext;

public interface ReplicatedObjectManager {

  /**
   * This method is used to sync up all ObjectIDs from the remote ObjectManagers. It is synchronous and after when it
   * returns nobody is allowed to join the cluster with exisiting objects.
   */
  public void sync();

  public void relayTransactions(final TransactionBatchContext transactionBatchContext);

  public void query(NodeID nodeID) throws GroupException;
  
  public void clear(NodeID nodeID);

  public void handleGCResult(GCResultMessage message);

}