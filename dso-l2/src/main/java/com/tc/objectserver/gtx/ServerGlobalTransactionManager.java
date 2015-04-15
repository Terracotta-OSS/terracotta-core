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
package com.tc.objectserver.gtx;

import com.tc.net.NodeID;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.dna.api.LogicalChangeResult;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.gtx.GlobalTransactionIDGenerator;
import com.tc.object.tx.ServerTransactionID;
import com.tc.text.PrettyPrintable;
import com.tc.util.sequence.Sequence;

import java.util.Map;
import java.util.Set;

public interface ServerGlobalTransactionManager extends GlobalTransactionIDGenerator, PrettyPrintable {

  /**
   * Changes state to APPLY_INITIATED and returns true if the specified transaction hasn't been initiated apply. If not
   * returns false.
   */
  public boolean initiateApply(ServerTransactionID stxID);

  /**
   * Commits the state of the transaciton.
   */
  public void commit(ServerTransactionID stxID);

  /**
   * Notifies the transaction manager that the ServerTransactionIDs in the given collection are no longer active (i.e.,
   * it will never be referenced again). The transaction manager is free to release resources dedicated those
   * transactions.
   */
  public void clearCommitedTransactionsBelowLowWaterMark(ServerTransactionID sid);

  /**
   * Clear a given server transaction from the system.
   *
   * @param serverTransactionID the transaction to clear
   */
  public void clearCommittedTransaction(ServerTransactionID serverTransactionID);

  /**
   * This is used in the PASSIVE to clear completed transaction ids when low water mark is published from the ACTIVE.
   */
  public void clearCommitedTransactionsBelowLowWaterMark(GlobalTransactionID lowGlobalTransactionIDWatermark);

  public void shutdownNode(NodeID nodeID);

  public void shutdownAllClientsExcept(Set cids);

  public GlobalTransactionID getGlobalTransactionID(ServerTransactionID serverTransactionID);

  public void createGlobalTransactionDescIfNeeded(ServerTransactionID stxnID, GlobalTransactionID globalTransactionID);

  public GlobalTransactionIDSequenceProvider getGlobalTransactionIDSequenceProvider();

  public Sequence getGlobalTransactionIDSequence();

  public void registerCallbackOnLowWaterMarkReached(Runnable callback);

  public void recordApplyResults(ServerTransactionID stxnID, Map<LogicalChangeID, LogicalChangeResult> results);

  public Map<LogicalChangeID, LogicalChangeResult> getApplyResults(ServerTransactionID stxnID);
}
