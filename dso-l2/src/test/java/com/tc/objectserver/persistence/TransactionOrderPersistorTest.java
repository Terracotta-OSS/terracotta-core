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

import com.tc.net.ClientID;
import com.tc.object.tx.TransactionID;

import com.tc.test.TCTestCase;
import com.tc.net.core.ProductID;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TransactionOrderPersistorTest extends TCTestCase {
  private NullPlatformPersistentStorage persistentStorage;
  private TransactionOrderPersistor orderPersistor;
  private ClientID client1;
  private ClientID client2;

  @Override
  public void setUp() {
    this.persistentStorage = new NullPlatformPersistentStorage();
    this.orderPersistor = new TransactionOrderPersistor(this.persistentStorage, Collections.emptySet());
    this.client1 = new ClientID(1);
    this.client2 = new ClientID(2);
    this.orderPersistor.addTrackingForClient(client1, ProductID.PERMANENT);
    this.orderPersistor.addTrackingForClient(client2, ProductID.PERMANENT);
  }

  /**
   * Test that the trivial case of usage works.
   */
  public void testOneClientNoExpiry() {
    TransactionID oldest = new TransactionID(1L);
    
    for (int i = 1; i < 10; ++i) {
      TransactionID transaction = new TransactionID(i);
      this.orderPersistor.updateWithNewMessage(this.client1, transaction, oldest);
    }
    // We aren't expecting to check anything, just that we didn't fail to get this far.
  }

  /**
   * Test that multiple clients basically work.
   */
  public void testTwoClientsNoExpiry() {
    TransactionID oldest = new TransactionID(1L);
    
    for (int i = 1; i < 10; ++i) {
      TransactionID transaction = new TransactionID(i);
      this.orderPersistor.updateWithNewMessage(this.client1, transaction, oldest);
      this.orderPersistor.updateWithNewMessage(this.client2, transaction, oldest);
    }
    // We aren't expecting to check anything, just that we didn't fail to get this far.
  }

  /**
   * Test that multiple clients basically work.
   */
  public void testTwoClientsQuickExpiry() {
    TransactionID previous = new TransactionID(1L);
    
    for (int i = 1; i < 10; ++i) {
      TransactionID transaction = new TransactionID(i);
      this.orderPersistor.updateWithNewMessage(this.client1, transaction, previous);
      this.orderPersistor.updateWithNewMessage(this.client2, transaction, previous);
      previous = transaction;
    }
    // We aren't expecting to check anything, just that we didn't fail to get this far.
  }

  /**
   * Test that we throw an exception when "oldest" ID is null.
   */
  public void testOneClientFailsNull() {
    TransactionID oldest = null;
    
    for (int i = 1; i < 10; ++i) {
      TransactionID transaction = new TransactionID(i);
      boolean didFail = false;
      try {
        this.orderPersistor.updateWithNewMessage(this.client1, transaction, oldest);
      } catch (IllegalArgumentException e) {
        didFail = true;
      }
      // We were expecting to throw that exception.
      assertTrue(didFail);
    }
  }

  /**
   * Test that we throw an exception when "oldest" ID is greater than the new one.
   */
  public void testOneClientFailsTooOld() {
    TransactionID oldest = new TransactionID(100);
    
    for (int i = 1; i < 10; ++i) {
      TransactionID transaction = new TransactionID(i);
      boolean didFail = false;
      try {
        this.orderPersistor.updateWithNewMessage(this.client1, transaction, oldest);
      } catch (IllegalArgumentException e) {
        didFail = true;
      }
      // We were expecting to throw that exception.
      assertTrue(didFail);
    }
  }

  /**
   * Test that we throw an exception when one client provides the same transaction ID more than once.
   */
  public void testTwoClientsFailToReuse() {
    TransactionID previous = new TransactionID(1L);
    
    for (int i = 1; i < 10; ++i) {
      TransactionID transaction = new TransactionID(i);
      this.orderPersistor.updateWithNewMessage(this.client1, transaction, previous);
      /*
      boolean didFail = false;
      try {
        this.orderPersistor.updateWithNewMessage(this.client1, transaction, previous);
      } catch (IllegalArgumentException e) {
        didFail = true;
      }
      // We were expecting to throw that exception.
      assertTrue(didFail);
      */
      this.orderPersistor.updateWithNewMessage(this.client2, transaction, previous);
      previous = transaction;
    }
  }

  /**
   * Test that multiple clients work and one can disconnect part-way.
   */
  public void testTwoClientsNoExpiryOneDisconnect() {
    TransactionID oldest = new TransactionID(1L);
    
    // Populate some initial data.
    for (int i = 1; i < 10; ++i) {
      TransactionID transaction = new TransactionID(i);
      this.orderPersistor.updateWithNewMessage(this.client1, transaction, oldest);
      this.orderPersistor.updateWithNewMessage(this.client2, transaction, oldest);
    }
    // Disconnect client1.
    this.orderPersistor.removeTrackingForClient(this.client1);
    // Ensure that client2 can keep running.
    for (int i = 10; i < 20; ++i) {
      TransactionID transaction = new TransactionID(i);
      this.orderPersistor.updateWithNewMessage(this.client2, transaction, oldest);
    }
    // We aren't expecting to check anything, just that we didn't fail to get this far.
  }

  /**
   * Test that multiple clients work and queries of where they exist in the global order show them correctly interleaved.
   */
  public void testTwoClientsInterleavedGlobally() {
    TransactionID oldest = new TransactionID(1L);
    
    // Populate some initial data.
    for (int i = 1; i < 10; ++i) {
      TransactionID transaction = new TransactionID(i);
      this.orderPersistor.updateWithNewMessage(this.client1, transaction, oldest);
      this.orderPersistor.updateWithNewMessage(this.client2, transaction, oldest);
    }
    
    // Now, verify that these are interleaved, within the global order.
    for (int i = 1; i < 10; ++i) {
      TransactionID transaction = new TransactionID(i);
      int expectedIndex = (i - 1) * 2;
      assertEquals(expectedIndex, this.orderPersistor.getIndexToReplay(this.client1, transaction));
    }
    for (int i = 1; i < 10; ++i) {
      TransactionID transaction = new TransactionID(i);
      int expectedIndex = (i - 1) * 2 + 1;
      assertEquals(expectedIndex, this.orderPersistor.getIndexToReplay(this.client2, transaction));
    }
  }

  /**
   * Test that an unknown transaction reports its global order index as -1.
   */
  public void testUnknownTransactionNoOrder() {
    TransactionID oldest = new TransactionID(1L);
    
    // Populate some initial data.
    for (int i = 1; i < 10; ++i) {
      TransactionID transaction = new TransactionID(i);
      this.orderPersistor.updateWithNewMessage(this.client1, transaction, oldest);
    }
    TransactionID newTransaction = new TransactionID(10);
    assertEquals(-1, this.orderPersistor.getIndexToReplay(this.client1, newTransaction));
  }

  /**
   * Test that the persistor can be cleared and we can continue.
   */
  public void testClearAndContinue() {
    TransactionID oldest = new TransactionID(1L);
    
    // Populate some initial data.
    for (int i = 1; i < 10; ++i) {
      TransactionID transaction = new TransactionID(i);
      this.orderPersistor.updateWithNewMessage(this.client1, transaction, oldest);
    }
    
    // Verify that these are in persistor.
    for (int i = 1; i < 10; ++i) {
      TransactionID transaction = new TransactionID(i);
      int expectedIndex = i - 1;
      assertEquals(expectedIndex, this.orderPersistor.getIndexToReplay(this.client1, transaction));
    }
    
    // Clear the persistor and verify that they are no longer present.
    this.orderPersistor.clearAllRecords();
    for (int i = 1; i < 10; ++i) {
      TransactionID transaction = new TransactionID(i);
      assertEquals(-1, this.orderPersistor.getIndexToReplay(this.client1, transaction));
    }
    
    // Add a new transaction and verify that it is in the persistor.
    TransactionID newTransaction = new TransactionID(10);
    this.orderPersistor.updateWithNewMessage(this.client1, newTransaction, oldest);
    assertEquals(0, this.orderPersistor.getIndexToReplay(this.client1, newTransaction));
  }

  public void testSaveReloadEmpty() throws IOException {
    // Create the storage.
    NullPlatformPersistentStorage storage = new NullPlatformPersistentStorage();
    new TransactionOrderPersistor(storage, Collections.emptySet());
    
    // Now, try to reload it.
    new TransactionOrderPersistor(storage, Collections.emptySet());
  }

  public void testSaveReloadSimple() throws IOException {
    ClientID client1 = new ClientID(1);
    ClientID client2 = new ClientID(2);
    TransactionID oldest = new TransactionID(1L);

    // Create the storage.
    NullPlatformPersistentStorage storage = new NullPlatformPersistentStorage();
    TransactionOrderPersistor persistor = new TransactionOrderPersistor(storage, Collections.emptySet());
    for (int i = 1; i < 100; ++i) {
      TransactionID transaction = new TransactionID(i);
      persistor.updateWithNewMessage(client1, transaction, oldest);
      persistor.updateWithNewMessage(client2, transaction, oldest);
    }
    
    // Now, try to reload it.
    persistor = new TransactionOrderPersistor(storage, Collections.emptySet());
    for (int i = 100; i < 200; ++i) {
      TransactionID transaction = new TransactionID(i);
      persistor.updateWithNewMessage(client1, transaction, oldest);
      persistor.updateWithNewMessage(client2, transaction, oldest);
    }
  }
  
  public void testStoringAllPerClient() throws Exception {
    ProductID[] products = new ProductID[] {ProductID.PERMANENT, ProductID.STRIPE, ProductID.SERVER, ProductID.DIAGNOSTIC};
    ClientID[] clients = new ClientID[] {new ClientID(3), new ClientID(4), new ClientID(5), new ClientID(6)};
    String[] tags = new String[] {"permanentClients", "regularClients", null, null};
    for (int x=0;x<clients.length;x++) {
      ClientID cid = clients[x];
      ProductID product = products[x];
      String tag = tags[x];
      orderPersistor.addTrackingForClient(cid, product);
      orderPersistor.updateWithNewMessage(cid, TransactionID.NULL_ID, TransactionID.NULL_ID);
      //  make sure not stored
      Map<String, Object> map = new HashMap<String, Object>();
      orderPersistor.reportStateToMap(map);
      if (tag == null) {
    // if the tag is null, these clients should not be tracked.  
        assertFalse(((Map)map.get("regularClients")).containsKey(cid.toString()));
        assertFalse(((Map)map.get("permanentClients")).containsKey(cid.toString()));
      } else {
        assertTrue(((List)((Map)map.get(tag)).get(cid.toString())).isEmpty());
      }

      orderPersistor.updateWithNewMessage(cid, new TransactionID(1), new TransactionID(1));
      map.clear();
      orderPersistor.reportStateToMap(map);
      if (tag == null) {
    // if the tag is null, these clients should not be tracked.  
        assertFalse(((Map)map.get("regularClients")).containsKey(cid.toString()));
        assertFalse(((Map)map.get("permanentClients")).containsKey(cid.toString()));
      } else {
        assertTrue(((List)((Map)map.get(tag)).get(cid.toString())).size() == 1);
      }
      //  add more
      for (int i=2;i<=10;i++) {
        orderPersistor.updateWithNewMessage(cid, new TransactionID(i), new TransactionID(1));
      }

      map.clear();
      orderPersistor.reportStateToMap(map);
      if (tag == null) {
    // if the tag is null, these clients should not be tracked.  
        assertFalse(((Map)map.get("regularClients")).containsKey(cid.toString()));
        assertFalse(((Map)map.get("permanentClients")).containsKey(cid.toString()));
      } else {
        assertTrue(((List)((Map)map.get(tag)).get(cid.toString())).size() == 10);
      }
      // gc
      orderPersistor.updateWithNewMessage(cid, new TransactionID(11), new TransactionID(11));
      map.clear();
      orderPersistor.reportStateToMap(map);
      if (tag == null) {
    // if the tag is null, these clients should not be tracked.  
        assertFalse(((Map)map.get("regularClients")).containsKey(cid.toString()));
        assertFalse(((Map)map.get("permanentClients")).containsKey(cid.toString()));
      } else {
        assertTrue(((List)((Map)map.get(tag)).get(cid.toString())).size() == 1);
      }
    }
  }

  public void testSaveReloadMultipleThreads() throws IOException, InterruptedException {
    TransactionID oldest = new TransactionID(1L);

    // Create the storage.
    NullPlatformPersistentStorage storage = new NullPlatformPersistentStorage();
    TransactionOrderPersistor persistor = new TransactionOrderPersistor(storage, Collections.emptySet());
    ClientThread thread1 = new ClientThread(persistor, new ClientID(1), oldest, 1, 100);
    ClientThread thread2 = new ClientThread(persistor, new ClientID(2), oldest, 1, 100);
    
    thread1.start();
    thread2.start();
    thread1.join();
    thread2.join();
    
    // Now, try to reload it.
    oldest = new TransactionID(99);
    persistor = new TransactionOrderPersistor(storage, Collections.emptySet());
    thread1 = new ClientThread(persistor, new ClientID(1), oldest, 100, 200);
    thread2 = new ClientThread(persistor, new ClientID(2), oldest, 100, 200);
    
    thread1.start();
    thread2.start();
    thread1.join();
    thread2.join();
  }


  private static class ClientThread extends Thread {
    private TransactionOrderPersistor persistor;
    private ClientID client;
    private TransactionID oldest;
    private int start;
    private int end;
    
    public ClientThread(TransactionOrderPersistor persistor, ClientID client, TransactionID oldest, int start, int end) {
      this.persistor = persistor;
      this.client = client;
      this.oldest = oldest;
      this.start = start;
      this.end = end;
    }
    
    @Override
    public void run() {
      for (int i = this.start; i < this.end; ++i) {
        TransactionID transaction = new TransactionID(i);
        this.persistor.updateWithNewMessage(this.client, transaction, this.oldest);
      }
    }
  }
}
