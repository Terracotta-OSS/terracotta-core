/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.objectserver.persistence;

import org.terracotta.persistence.IPlatformPersistence;

import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


/**
 * Stores the information mapping the client-local TransactionIDs of in-flight transactions into a global order.
 * This is persisted because reconnect on restart needs to ensure that the transactions being replayed are done so in
 * the same order as their original order.
 */
public class TransactionOrderPersistor {
  private static final String RECEIVED_TRANSACTION_COUNT_FILE_NAME = "received_transaction_count.map";
  
  private final IPlatformPersistence storageManager;
  private Long receivedTransactionCount;
    
  private List<ClientTransaction> globalList = null;
  private final Set<Long> clientNodeIDs;
  
  public TransactionOrderPersistor(IPlatformPersistence storageManager, Set<ChannelID> clients) {
    this.storageManager = storageManager;
    
    this.clientNodeIDs = new HashSet<>();
    for (ChannelID oneClient : clients) {
      this.clientNodeIDs.add(oneClient.toLong());
    }
    
    Long receivedTransactionCount = null;
    try {
      receivedTransactionCount = (Long)this.storageManager.loadDataElement(RECEIVED_TRANSACTION_COUNT_FILE_NAME);
    } catch (IOException e) {
      // We don't expect this during startup so just throw it as runtime.
      throw new RuntimeException("Failure reading TransactionOrderPersistor count file", e);
    }
    this.receivedTransactionCount = (null != receivedTransactionCount) ? receivedTransactionCount : new Long(0L);
  }

  /**
   * Called to handle the changes to persisted transactions, based on a new one.
   * This new transactionID will be enqueued as the most recent transaction for the given source but also globally.
   * Any transactions for this source which are older than oldestTransactionOnClient will be removed from persistence.
   */
  public synchronized void updateWithNewMessage(ClientID source, TransactionID transactionID, TransactionID oldestTransactionOnClient) {
    // We need to ensure that the arguments are sane.
    if ((null == oldestTransactionOnClient) || (null == transactionID)) {
      throw new IllegalArgumentException("Transactions cannot be null");
    }
    if (oldestTransactionOnClient.compareTo(transactionID) > 0) {
      throw new IllegalArgumentException("Oldest transaction cannot come after new transaction");
    }
    
    // This operation requires that the globalList be rebuilt.
    this.globalList = null;
    
    // Make sure we have tracking for this client.
    this.clientNodeIDs.add(source.toLong());
    
    // Increment the number of received transactions.
    this.receivedTransactionCount += 1;
    storeToDisk(RECEIVED_TRANSACTION_COUNT_FILE_NAME, this.receivedTransactionCount);
    
    // Create the new pair.
    IPlatformPersistence.SequenceTuple transaction = new IPlatformPersistence.SequenceTuple();
    transaction.localSequenceID = transactionID.toLong();
    transaction.globalSequenceID = this.receivedTransactionCount;
    
    // We now pass this straight into the underlying storage.
    Future<Void> syncFuture = this.storageManager.fastStoreSequence(source.toLong(), transaction, oldestTransactionOnClient.toLong());
    
    // TODO:  We need to float this future blocking on the sync further out.
    try {
      syncFuture.get();
    } catch (InterruptedException e) {
      Assert.fail(e.getLocalizedMessage());
    } catch (ExecutionException e) {
      Assert.fail(e.getLocalizedMessage());
    }
  }

  /**
   * Called when we no longer need to track transaction ordering information from source (presumably due to a disconnect).
   */
  public synchronized void removeTrackingForClient(ClientID source) {
    long sourceID = source.toLong();
    try {
      this.storageManager.deleteSequence(sourceID);
    } catch (IOException e) {
      Assert.fail(e.getLocalizedMessage());
    }
    this.clientNodeIDs.remove(sourceID);
  }

  private static class ClientTransaction {
    public long clientID;
    public long localTransactionID;
    public long globalTransactionID;

    @Override
    public int hashCode() {
      return (int) ((7 * clientID)
          ^ (5 * localTransactionID)
          ^ globalTransactionID);
    }
    @Override
    public boolean equals(Object obj) {
      boolean isEqual = (obj == this);
      if (!isEqual && (obj instanceof ClientTransaction)) {
        ClientTransaction other = (ClientTransaction) obj;
        isEqual = (this.clientID == other.clientID)
            && (this.localTransactionID == other.localTransactionID)
            && (this.globalTransactionID == other.globalTransactionID);
      }
      return isEqual;
    }
  }
  
  private List<ClientTransaction> buildGlobalListIfNessessary() {
    if (null == this.globalList) {
      TreeMap<Long, ClientTransaction> sortMap = new TreeMap<>();
      for (long clientID : this.clientNodeIDs) {
        List<IPlatformPersistence.SequenceTuple> transactions = null;
        try {
          transactions = this.storageManager.loadSequence(clientID);
        } catch (IOException e) {
          Assert.fail(e.getLocalizedMessage());
        }
        for (IPlatformPersistence.SequenceTuple tuple : transactions) {
          ClientTransaction transaction = new ClientTransaction();
          transaction.clientID = clientID;
          transaction.localTransactionID = tuple.localSequenceID;
          transaction.globalTransactionID = tuple.globalSequenceID;
          sortMap.put(tuple.globalSequenceID, transaction);
        }
      }
      globalList = Collections.unmodifiableList(new ArrayList<>(sortMap.values()));
    }
    return globalList;
  }

  /**
   * Called to ask where a given client-local transaction exists in the global transaction list.
   * Returns the index or -1 if it isn't known.
   */
  public int getIndexToReplay(ClientID source, TransactionID transaction) {
    long sourceID = source.toLong();
    long transactionID = transaction.toLong();
    
    int index = -1;
    List<ClientTransaction> list = buildGlobalListIfNessessary();
    int seek = 0;
    for (ClientTransaction oneTransaction : list) {
      if ((oneTransaction.clientID == sourceID) && (oneTransaction.localTransactionID == transactionID)) {
        index = seek;
        break;
      }
      seek += 1;
    }
    return index;
  }

  /**
   * Clears all internal state.
   */
  public void clearAllRecords() {
    this.globalList = null;
    for (long nodeID : clientNodeIDs) {
      try {
        this.storageManager.deleteSequence(nodeID);
      } catch (IOException e) {
        Assert.fail(e.getLocalizedMessage());
      }
    }
    this.clientNodeIDs.clear();
  }

  /**
   * @return The number of transactions which have been observed by the persistor (NOT the number persisted).
   */
  public long getReceivedTransactionCount() {
    return this.receivedTransactionCount;
  }

  private void storeToDisk(String dataName, Serializable dataElement) {
    try {
      this.storageManager.storeDataElement(dataName, dataElement);
    } catch (IOException e) {
      // In general, we have no way of solving this problem so throw it.
      throw new RuntimeException("Failure storing TransactionOrderPersistor data", e);
    }
  }
}
