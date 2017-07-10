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
import com.tc.object.tx.TransactionID;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.ProductID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;


/**
 * Stores the information mapping the client-local TransactionIDs of in-flight transactions into a global order.
 * This is persisted because reconnect on restart needs to ensure that the transactions being replayed are done so in
 * the same order as their original order.
 */
public class TransactionOrderPersistor {
  private final IPlatformPersistence storageManager;
  private Long receivedTransactionCount = new Long(0L);
    
  private List<ClientTransaction> globalList = null;
  private final Set<ClientID> permNodeIDs = new HashSet<>();
  private final Map<ClientID, List<ClientTransaction>> fastSequenceCache = new HashMap<>();
  
  public TransactionOrderPersistor(IPlatformPersistence storageManager, Set<ClientID> clients) {
    this.storageManager = storageManager;
    
    for (ClientID oneClient : clients) {
      this.permNodeIDs.add(oneClient);
    }
  }

  /**
   * Called to handle the changes to persisted transactions, based on a new one.
   * This new transactionID will be enqueued as the most recent transaction for the given source but also globally.
   * Any transactions for this source which are older than oldestTransactionOnClient will be removed from persistence.
   */
  public synchronized Future<Void> updateWithNewMessage(ClientID source, TransactionID transactionID, TransactionID oldestTransactionOnClient) {
    // We need to ensure that the arguments are sane.
    if ((null == oldestTransactionOnClient) || (null == transactionID)) {
      throw new IllegalArgumentException("Transactions cannot be null");
    }
    if (oldestTransactionOnClient.compareTo(transactionID) > 0) {
      throw new IllegalArgumentException("Oldest transaction cannot come after new transaction");
    }
    
    // This operation requires that the globalList be rebuilt.
    this.globalList = null;
    
    // Increment the number of received transactions.
    this.receivedTransactionCount += 1;
    
    // We now pass this straight into the underlying storage.
    if (this.permNodeIDs.contains(source)) {
      // Create the new pair.
      IPlatformPersistence.SequenceTuple transaction = new IPlatformPersistence.SequenceTuple();
      transaction.localSequenceID = transactionID.toLong();
      transaction.globalSequenceID = this.receivedTransactionCount;
    
      return this.storageManager.fastStoreSequence(source.toLong(), transaction, oldestTransactionOnClient.toLong());
    } else {
      ClientTransaction transaction = new ClientTransaction();
      transaction.localTransactionID = transactionID.toLong();
      transaction.globalTransactionID = this.receivedTransactionCount;
      return fastStoreSequence(source, new ClientTransaction(), 0);
    }
  }
  
  synchronized void addTrackingForClient(ClientID source, ProductID product) {
    // Make sure we have tracking for this client.
    if (product.isPermanent()) {
      this.permNodeIDs.add(source);
    } else {
      this.fastSequenceCache.put(source, new LinkedList<>());
    }
  }

  /**
   * Called when we no longer need to track transaction ordering information from source (presumably due to a disconnect).
   */
  synchronized void removeTrackingForClient(ClientID source) {
    long sourceID = source.toLong();
    try {
      if (this.permNodeIDs.remove(source)) {
        this.storageManager.deleteSequence(sourceID);
      } else {
        fastSequenceCache.remove(source);
      }
    } catch (IOException e) {
      Assert.fail(e.getLocalizedMessage());
    }
  }

  private Future<Void> fastStoreSequence(ClientID sequenceIndex, ClientTransaction newEntry, long oldestValidSequenceID) {
    List<ClientTransaction> sequence = fastSequenceCache.get(sequenceIndex);
    if (sequence != null) {
      if (!sequence.isEmpty()) {
  //  exploiting the knowledge that sequences are always updated in an increasing fashion, as soon as the first
  //  cleaning function fails, bail on the iteration
        Iterator<ClientTransaction> tuple = sequence.iterator();
        while (tuple.hasNext()) {
          if (tuple.next().localTransactionID < oldestValidSequenceID) {
            tuple.remove();
          } else {
            break;
          }
        }
      }
      sequence.add(newEntry);
    } else {
      // must be a client that will not reconnect
    }
    return CompletableFuture.completedFuture(null);
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

    @Override
    public String toString() {
      return "{clientID=" + clientID +
             ", localTransactionID=" + localTransactionID +
             ", globalTransactionID=" + globalTransactionID +
             '}';
    }
  }
  
  private List<ClientTransaction> buildGlobalListIfNecessary() {
    if (null == this.globalList) {
      TreeMap<Long, ClientTransaction> sortMap = new TreeMap<>();
      for (ClientID clientID : this.permNodeIDs) {
        List<IPlatformPersistence.SequenceTuple> transactions = null;
        try {
          transactions = this.storageManager.loadSequence(clientID.toLong());
        } catch (IOException e) {
          Assert.fail(e.getLocalizedMessage());
        }
        if (transactions != null) {
          for (IPlatformPersistence.SequenceTuple tuple : transactions) {
            ClientTransaction transaction = new ClientTransaction();
            transaction.clientID = clientID.toLong();
            transaction.localTransactionID = tuple.localSequenceID;
            transaction.globalTransactionID = tuple.globalSequenceID;
            sortMap.put(tuple.globalSequenceID, transaction);
          }
        }
      }
      for (List<ClientTransaction> all : this.fastSequenceCache.values()) {
        if (all != null) {
          for (ClientTransaction t : all) {
            sortMap.put(t.globalTransactionID, t);
          }
        }
      }
      globalList = Collections.unmodifiableList(new ArrayList<>(sortMap.values()));
      receivedTransactionCount = !sortMap.isEmpty() ? sortMap.lastKey() : 0L;
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
    List<ClientTransaction> list = buildGlobalListIfNecessary();
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
  public synchronized  void clearAllRecords() {
    this.globalList = null;
    for (ClientID nodeID : this.permNodeIDs) {
      try {
        this.storageManager.deleteSequence(nodeID.toLong());
      } catch (IOException e) {
        Assert.fail(e.getLocalizedMessage());
      }
    }
    this.fastSequenceCache.clear();
  }

  /**
   * @return The number of transactions which have been observed by the persistor (NOT the number persisted).
   */
  public long getReceivedTransactionCount() {
    return this.receivedTransactionCount;
  }
  
  public Map<String, Object> reportStateToMap(Map<String, Object> map) {
    map.put("className", this.getClass().getName());
    map.put("receivedTransactions", getReceivedTransactionCount());
    if(this.permNodeIDs != null && storageManager != null) {
      Map<String, Object> clientMap = new LinkedHashMap<>();
      map.put("permanentClients", clientMap);
      for (ClientID clientNodeID : permNodeIDs) {
        List<IPlatformPersistence.SequenceTuple> transactions = null;
        try {
          transactions = this.storageManager.loadSequence(clientNodeID.toLong());
        } catch (IOException e) {
          Assert.fail(e.getLocalizedMessage());
        }
        List<String> trans = new ArrayList<>();
        if (transactions != null) {
          for (IPlatformPersistence.SequenceTuple transaction : transactions) {
            trans.add("Global seq Id = " + transaction.globalSequenceID + ", local seq id = " + transaction.localSequenceID);
          }
          clientMap.put(clientNodeID.toString(), trans);
        }
      }
    }

    Map<String, Object> clientMap = new LinkedHashMap<>();
    map.put("regularClients", clientMap);
    for (Map.Entry<ClientID, List<ClientTransaction>> entry : fastSequenceCache.entrySet()) {
      List<String> trans = new ArrayList<>();
      if (entry.getValue() != null) {
        clientMap.put(entry.getKey().toString(), trans);
        for (ClientTransaction transaction : entry.getValue()) {
          trans.add("Global seq Id = " + transaction.globalTransactionID + ", local seq id = " + transaction.localTransactionID);
        }
      }
    }
    return map;
  }
}
