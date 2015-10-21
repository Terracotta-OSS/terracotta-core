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

import org.terracotta.persistence.IPersistentStorage;
import org.terracotta.persistence.KeyValueStorage;

import com.tc.net.NodeID;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;


/**
 * Stores the information mapping the client-local TransactionIDs of in-flight transactions into a global order.
 * This is persisted because reconnect on restart needs to ensure that the transactions being replayed are done so in
 * the same order as their original order.
 */
public class TransactionOrderPersistor {
  private static final String CLIENT_LOCAL_LISTS = "client_local_lists";
  private static final String LIST_CONTAINER = "list_container";
  private static final String LIST_KEY = "list_container:key";

  private final KeyValueStorage<NodeID, List<ClientTransaction>> clientLocals;
  private final KeyValueStorage<String, List<ClientTransaction>> listContainer;

  // Unchecked and raw warnings because we are trying to use Class<List<?>>, which the compiler doesn't like but has no runtime meaning.
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public TransactionOrderPersistor(IPersistentStorage storageManager) {
    // In the future, we probably want a different storage approach since storing the information, this way, doesn't
    // work well with the type system (Lists inside KeyValueStorage) and will perform terribly.
    this.clientLocals = storageManager.getKeyValueStorage(CLIENT_LOCAL_LISTS, NodeID.class, (Class)List.class);
    this.listContainer = storageManager.getKeyValueStorage(LIST_CONTAINER, String.class, (Class)List.class);
    if (!this.listContainer.containsKey(LIST_KEY)) {
      this.listContainer.put(LIST_KEY, new Vector<>());
    } else {
      // TAB-6411 : Add additional checks to track down an intermittent bug.
      Assert.assertTrue(this.listContainer.get(LIST_KEY) instanceof Vector);
    }
  }

  /**
   * Called to handle the changes to persisted transactions, based on a new one.
   * This new transactionID will be enqueued as the most recent transaction for the given source but also globally.
   * Any transactions for this source which are older than oldestTransactionOnClient will be removed from persistence.
   */
  public synchronized void updateWithNewMessage(NodeID source, TransactionID transactionID, TransactionID oldestTransactionOnClient) {
    // We need to ensure that the arguments are sane.
    if ((null == oldestTransactionOnClient) || (null == transactionID)) {
      throw new IllegalArgumentException("Transactions cannot be null");
    }
    if (oldestTransactionOnClient.compareTo(transactionID) > 0) {
      throw new IllegalArgumentException("Oldest transaction cannot come after new transaction");
    }
    
    // Get the local list for this client.
    List<ClientTransaction> localList = clientLocals.get(source);
    if (null == localList) {
      localList = new Vector<>();
      clientLocals.put(source, localList);
    }
    
    // Create the new pair.
    ClientTransaction transaction = new ClientTransaction();
    transaction.client = source;
    transaction.id = transactionID;
    
    // Make sure that this transaction isn't already in this list.
    if (localList.contains(transaction)) {
      throw new IllegalArgumentException("Transaction already exists for this client");
    }
    
    List<ClientTransaction> globalList = this.listContainer.get(LIST_KEY);
    // TAB-6411 : Add additional checks to track down an intermittent bug.
    Assert.assertNotNull(globalList);
    Assert.assertTrue(globalList instanceof Vector);
    // Remove anything the client no longer cares about.
    while ((localList.size() > 0) && (-1 == localList.get(0).id.compareTo(oldestTransactionOnClient))) {
      ClientTransaction removed = localList.remove(0);
      globalList.remove(removed);
    }
    
    // Create this new pair and add it to the global list.
    localList.add(transaction);
    globalList.add(transaction);
    // Save this back.
    this.listContainer.put(LIST_KEY, globalList);
  }

  /**
   * Called when we no longer need to track transaction ordering information from source (presumably due to a disconnect).
   */
  public void removeTrackingForClient(NodeID source) {
    // Remove the local list for this client.
    clientLocals.remove(source);
    
    // Strip any references to this client from the global list.
    List<ClientTransaction> newGlobalList = new Vector<>();
    for (ClientTransaction transaction : this.listContainer.get(LIST_KEY)) {
      if (!source.equals(transaction.client)) {
        newGlobalList.add(transaction);
      }
    }
    // Save this back.
    this.listContainer.put(LIST_KEY, newGlobalList);
  }

  private static class ClientTransaction implements Serializable {
    private static final long serialVersionUID = 1L;
    public NodeID client;
    public TransactionID id;

    @Override
    public int hashCode() {
      return (7 * this.client.hashCode()) ^ this.id.hashCode();
    }
    @Override
    public boolean equals(Object obj) {
      boolean isEqual = (obj == this);
      if (!isEqual && (obj instanceof ClientTransaction)) {
        ClientTransaction other = (ClientTransaction) obj;
        isEqual = this.client.equals(other.client)
              & this.id.equals(other.id);
      }
      return isEqual;
    }
  }

  /**
   * Called to ask where a given client-local transaction exists in the global transaction list.
   * Returns the index or -1 if it isn't known.
   */
  public int getIndexToReplay(NodeID source, TransactionID transactionID) {
    int index = -1;
    
    List<ClientTransaction> globalList = this.listContainer.get(LIST_KEY);
    int seek = 0;
    for (ClientTransaction transaction : globalList) {
      if (source.equals(transaction.client) && transactionID.equals(transaction.id)) {
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
    this.clientLocals.clear();
    this.listContainer.clear();
    this.listContainer.put(LIST_KEY, new Vector<>());
  }
}
