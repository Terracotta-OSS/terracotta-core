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
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import org.terracotta.persistence.IPlatformPersistence;


public class PassthroughTransactionOrderManager {
  private final IPlatformPersistence platformPersistence;

  private List<ClientTransaction> clientTransactionList;
  private long receivedTransactionCount;
  
  // This map is only available while handling re-sends.
  private Map<ClientTransaction, PassthroughMessageContainer> collectedResends;

  public PassthroughTransactionOrderManager(IPlatformPersistence platformPersistence, boolean shouldLoadStorage, Set<Long> savedClientConnections) {
    this.platformPersistence = platformPersistence;
    buildClientTransactionList(savedClientConnections, shouldLoadStorage);
  }

  public void updateTracking(long connectionID, long transactionID, long oldestIDOnConnection) {
    receivedTransactionCount++;

    IPlatformPersistence.SequenceTuple sequenceTuple = new IPlatformPersistence.SequenceTuple();
    sequenceTuple.globalSequenceID = receivedTransactionCount;
    sequenceTuple.localSequenceID = transactionID;

    this.platformPersistence.fastStoreSequence(connectionID, sequenceTuple, oldestIDOnConnection);
  }

  public void startHandlingResends() {
    Assert.assertTrue(null == this.collectedResends);
    this.collectedResends = new HashMap<ClientTransaction, PassthroughMessageContainer>();
  }

  public void handleResend(long connectionID, long transactionID, PassthroughMessageContainer container) {
    Assert.assertTrue(null != this.collectedResends);
    ClientTransaction fakeTransaction = new ClientTransaction();
    fakeTransaction.connectionID = connectionID;
    fakeTransaction.transactionID = transactionID;
    this.collectedResends.put(fakeTransaction, container);
  }

  public List<PassthroughMessageContainer> stopHandlingResends() {
    Assert.assertTrue(null != this.collectedResends);
    Assert.assertTrue(null != this.clientTransactionList);
    List<PassthroughMessageContainer> orderToExecute = new Vector<PassthroughMessageContainer>();
    
    // First, walk the transaction order list, extracting any matches from the collectedResends.
    for (ClientTransaction transaction : clientTransactionList) {
      PassthroughMessageContainer container = this.collectedResends.remove(transaction);
      if (null != container) {
        orderToExecute.add(container);
      }
    }
    
    // Then, walk the remaining resends, adding them to the list in an arbitrary order.
    for (PassthroughMessageContainer container : this.collectedResends.values()) {
      orderToExecute.add(container);
    }
    
    this.collectedResends = null;
    this.clientTransactionList = null;
    return orderToExecute;
  }

  private void buildClientTransactionList(Set<Long> savedClientConnections, boolean shouldLoadStorage) {
    if(shouldLoadStorage) {
      TreeMap<Long, ClientTransaction> sortMap = new TreeMap<Long, ClientTransaction>();
      for (long clientID : savedClientConnections) {
        List<IPlatformPersistence.SequenceTuple> transactions = null;
        try {
          transactions = this.platformPersistence.loadSequence(clientID);
        } catch (IOException e) {
          Assert.unexpected(e);
        }
        if (transactions != null) {
          for (IPlatformPersistence.SequenceTuple tuple : transactions) {
            ClientTransaction transaction = new ClientTransaction();
            transaction.connectionID = clientID;
            transaction.transactionID = tuple.localSequenceID;
            sortMap.put(tuple.globalSequenceID, transaction);
          }
        }
      }
      clientTransactionList = Collections.unmodifiableList(new ArrayList<ClientTransaction>(sortMap.values()));
      receivedTransactionCount = sortMap.size() != 0 ? sortMap.lastKey() : new Long(0L);
    } else {
      clientTransactionList = Collections.emptyList();
      receivedTransactionCount = 0L;
    }
  }

  private static class ClientTransaction implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public long connectionID;
    public long transactionID;
    
    @Override
    public boolean equals(Object obj) {
      boolean doesMatch = (this == obj);
      if (!doesMatch && (null != obj) && (getClass() == obj.getClass())) {
        ClientTransaction other = (ClientTransaction) obj;
        doesMatch = (connectionID == other.connectionID)
            && (transactionID == other.transactionID);
      }
      return doesMatch;
    }
    @Override
    public int hashCode() {
      return ((int)connectionID << 16) | (int)transactionID;
    }
  }
}