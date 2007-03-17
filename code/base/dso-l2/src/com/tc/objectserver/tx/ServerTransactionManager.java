/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.objectserver.persistence.api.PersistenceTransaction;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface ServerTransactionManager {

  /**
   * Used to recover from client crashes this will acknowledge any waiter waiting for a downed client
   * 
   * @param waitee
   */
  public void shutdownClient(ChannelID deadClient);

  /**
   * Add "waiter/requestID" is waiting for clientID "waitee" to respond to my message send
   * 
   * @param waiter - ChannelID of the sender of the message that is waiting for a response
   * @param requestID - The id of the request sent by the channel ID that is waiting for a response
   * @param waitee - the channelID that waiter is waiting for a response from
   */
  public void addWaitingForAcknowledgement(ChannelID waiter, TransactionID requestID, ChannelID waitee);

  /**
   * Is the waiter done waiting or does it need to continue waiting?
   * 
   * @param waiter - ChannelID of the sender of the message that is waiting for a response
   * @param requestID - The id of the request sent by the channel ID that is waiting for a response
   * @return
   */
  public boolean isWaiting(ChannelID waiter, TransactionID requestID);

  /**
   * received an acknowledgement from the client that the changes in the given transaction have been applied.
   * This could potentially trigger an acknowledgement to the orginating client.
   * 
   * @param waiter - ChannelID of the sender of the message that is waiting for a response
   * @param requesterID - The id of the request sent by the channel ID that is waiting for a response
   * @param gtxID - The GlobalTransactionID associated with the transaction.
   * @param waitee - the channelID that waiter is waiting for a response from
   */
  public void acknowledgement(ChannelID waiter, TransactionID requestID, ChannelID waitee);

  /**
   * Apply the changes in the given transaction to the given set of checked out objects.
   * @param instanceMonitor 
   * 
   * @return The results of the transaction apply
   */
  public void apply(GlobalTransactionID gtxID, ServerTransaction txn, Map objects, BackReferences includeIDs,
                    ObjectInstanceMonitor instanceMonitor);
  
  /**
   * The Objects will be checked back to the object manager
   */
  public void release(PersistenceTransaction ptx, Collection objects, Map newRoots);
  
  /**
   * The set of transactions are commited.
   * This could potentially trigger an acknowledgement to the orginating client.
   */
  public void committed(Collection txnIds);
  
  /**
   * The broadcast stage is completed.
   * This could potentially trigger an acknowledgement to the orginating client.
   */
  public void broadcasted(ChannelID waiter, TransactionID requestID);

  public void dump();

  /**
   * Notifies the transaction managed that the given transaction is being skipped
   */
  public void skipApplyAndCommit(ServerTransaction txn);

  public void setResentTransactionIDs(ChannelID channelID, Collection transactionIDs);
  
  public void addTransactionListener(ServerTransactionListener listener);

  public void incomingTransactions(ChannelID channelID, Set serverTxnIDs, boolean relayed);

  public void transactionsRelayed(ChannelID channelID, Set serverTxnIDs);
}
