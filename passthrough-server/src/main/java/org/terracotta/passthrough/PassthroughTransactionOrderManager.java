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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.terracotta.persistence.IPersistentStorage;
import org.terracotta.persistence.KeyValueStorage;


public class PassthroughTransactionOrderManager {
  // We will index this at 0 to get the actual list - only map types provided.
  private KeyValueStorage<Long, List<ClientTransaction>> transactionOrderContainer;
  
  // This map is only available while handling re-sends.
  private Map<ClientTransaction, PassthroughMessageContainer> collectedResends;

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public PassthroughTransactionOrderManager(IPersistentStorage persistentStorage) {
    this.transactionOrderContainer = persistentStorage.getKeyValueStorage("transaction order", Long.class, (Class)List.class);
  }

  public void updateTracking(long connectionID, long transactionID, long oldestIDOnConnection) {
    List<ClientTransaction> oldList = this.transactionOrderContainer.get(0L);
    List<ClientTransaction> newList = new Vector<ClientTransaction>();
    
    if (null != oldList) {
      for (ClientTransaction transaction : oldList) {
        if (transaction.connectionID == connectionID) {
          if (transaction.transactionID >= oldestIDOnConnection) {
            newList.add(transaction);
          } else {
            // This is old, so drop it.
          }
        } else {
          newList.add(transaction);
        }
      }
    }
    ClientTransaction newTransaction = new ClientTransaction();
    newTransaction.connectionID = connectionID;
    newTransaction.transactionID = transactionID;
    newList.add(newTransaction);
    this.transactionOrderContainer.put(0L, newList);
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
    List<PassthroughMessageContainer> orderToExecute = new Vector<PassthroughMessageContainer>();
    
    // First, walk the transaction order list, extracting any matches from the collectedResends.
    List<ClientTransaction> orderList = this.transactionOrderContainer.get(0L);
    if (null != orderList) {
      for (ClientTransaction transaction : orderList) {
        PassthroughMessageContainer container = this.collectedResends.remove(transaction);
        if (null != container) {
          orderToExecute.add(container);
        }
      }
    }
    
    // Then, walk the remaining resends, adding them to the list in an arbitrary order.
    for (PassthroughMessageContainer container : this.collectedResends.values()) {
      orderToExecute.add(container);
    }
    
    this.collectedResends = null;
    return orderToExecute;
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