/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

public interface ServerTransactionManager {

  /**
   * called when a Node (Client or Server) leaves.
   */
  public void shutdownNode(NodeID nodeID);

  /**
   * called with a Node is connected;
   */
  public void nodeConnected(NodeID nodeID);

  /**
   * Add "waiter/requestID" is waiting for clientID "waitee" to respond to my message send
   * 
   * @param waiter - ChannelID of the sender of the message that is waiting for a response
   * @param requestID - The id of the request sent by the channel ID that is waiting for a response
   * @param waitee - the channelID that waiter is waiting for a response from
   */
  public void addWaitingForAcknowledgement(NodeID waiter, TransactionID requestID, NodeID waitee);

  /**
   * Is the waiter done waiting or does it need to continue waiting?
   * 
   * @param waiter - ChannelID of the sender of the message that is waiting for a response
   * @param requestID - The id of the request sent by the channel ID that is waiting for a response
   * @return
   */
  public boolean isWaiting(NodeID waiter, TransactionID requestID);

  /**
   * received an acknowledgment from the client that the changes in the given transaction have been applied. This could
   * potentially trigger an acknowledgment to the originating client.
   * 
   * @param waiter - NodeID of the sender of the message that is waiting for a response
   * @param requesterID - The id of the request sent by the channel ID that is waiting for a response
   * @param gtxID - The GlobalTransactionID associated with the transaction.
   * @param waitee - the channelID that waiter is waiting for a response from
   */
  public void acknowledgement(NodeID waiter, TransactionID requestID, NodeID waitee);

  /**
   * Apply the changes in the given transaction to the given set of checked out objects.
   * 
   * @param instanceMonitor
   */
  public void apply(ServerTransaction txn, Map objects, ApplyTransactionInfo includeIDs,
                    ObjectInstanceMonitor instanceMonitor);

  /**
   * Commits all the changes in objects and releases the objects This could potentially trigger an acknowledgment to the
   * originating client.
   */
  public void commit(PersistenceTransactionProvider ptxp, Collection<ManagedObject> objects,
                     Map<String, ObjectID> newRoots, Collection<ServerTransactionID> appliedServerTransactionIDs,
                     SortedSet<ObjectID> deletedObjects);

  /**
   * The broadcast stage is completed. This could potentially trigger an acknowledgment to the originating client.
   */
  public void broadcasted(NodeID waiter, TransactionID requestID);

  public void processingMetaDataCompleted(NodeID sourceID, TransactionID txnID);

  /**
   * Notifies the transaction managed that the given transaction is being skipped
   */
  public void skipApplyAndCommit(ServerTransaction txn);

  public void addTransactionListener(ServerTransactionListener listener);

  public void removeTransactionListener(ServerTransactionListener listener);

  public void callBackOnTxnsInSystemCompletion(TxnsInSystemCompletionListener l);

  public void callBackOnResentTxnsInSystemCompletion(TxnsInSystemCompletionListener l);

  public void incomingTransactions(NodeID nodeID, Set txnIDs, Collection txns, boolean relayed);

  public void transactionsRelayed(NodeID node, Set serverTxnIDs);

  public void objectsSynched(NodeID node, ServerTransactionID tid);

  public void setResentTransactionIDs(NodeID source, Collection transactionIDs);

  public void start(Set cids);

  public void goToActiveMode();

  public int getTotalPendingTransactionsCount();

  public long getTotalNumOfActiveTransactions();

}
