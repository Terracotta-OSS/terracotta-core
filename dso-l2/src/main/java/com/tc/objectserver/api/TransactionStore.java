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
package com.tc.objectserver.api;

import com.tc.net.NodeID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;

import java.util.Collection;
import java.util.Set;

public interface TransactionStore {

  public void commitTransactionDescriptor(ServerTransactionID stxID);

  public GlobalTransactionDescriptor getTransactionDescriptor(ServerTransactionID serverTransactionID);

  public GlobalTransactionDescriptor getOrCreateTransactionDescriptor(ServerTransactionID serverTransactionID);

  public GlobalTransactionID getLeastGlobalTransactionID();

  /**
   * This method clears the server transaction ids less than the low water mark, for that particular node.
   * 
   * @return Collection of {@link GlobalTransactionDescriptor} for removed server transaction
   */
  public Collection<GlobalTransactionDescriptor> clearCommitedTransactionsBelowLowWaterMark(ServerTransactionID lowWaterMark);

  /**
   * Clear a single server transaction out of the store.
   *
   * @param serverTransactionID the transaction to clear
   * @return descriptor of the removed transaction
   */
  public GlobalTransactionDescriptor clearCommittedTransaction(ServerTransactionID serverTransactionID);

  /**
   * This is used by the passive to clear completed Transaction ids.
   */
  public void clearCommitedTransactionsBelowLowWaterMark(GlobalTransactionID lowGlobalTransactionIDWatermark);

  public void shutdownNode(NodeID nid);

  public void shutdownAllClientsExcept(Set cids);

  public void createGlobalTransactionDescIfNeeded(ServerTransactionID stxnID, GlobalTransactionID globalTransactionID);

}