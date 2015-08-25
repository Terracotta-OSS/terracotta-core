/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence;

import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.tx.TransactionID;

import com.tc.test.TCTestCase;

import java.io.IOException;


public class TransactionOrderPersistorTest extends TCTestCase {
  private static final String TEMP_FILE = "temp_file";
  private FlatFilePersistentStorage persistentStorage;
  private TransactionOrderPersistor orderPersistor;
  private NodeID client1;
  private NodeID client2;
  
  @Override
  public void setUp() {
    this.persistentStorage = new FlatFilePersistentStorage(TEMP_FILE);
    try {
      this.persistentStorage.create();
    } catch (IOException e) {
      fail(e);
    }
    this.orderPersistor = new TransactionOrderPersistor(this.persistentStorage);
    this.client1 = new ClientID(1);
    this.client2 = new ClientID(2);
  }

  /**
   * Test that the trivial case of usage works.
   */
  public void testOneClientNoExpiry() {
    TransactionID oldest = new TransactionID(0);
    
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
    TransactionID oldest = new TransactionID(0);
    
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
    TransactionID previous = new TransactionID(0);
    
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
    TransactionID previous = new TransactionID(0);
    
    for (int i = 1; i < 10; ++i) {
      TransactionID transaction = new TransactionID(i);
      this.orderPersistor.updateWithNewMessage(this.client1, transaction, previous);
      boolean didFail = false;
      try {
        this.orderPersistor.updateWithNewMessage(this.client1, transaction, previous);
      } catch (IllegalArgumentException e) {
        didFail = true;
      }
      // We were expecting to throw that exception.
      assertTrue(didFail);
      this.orderPersistor.updateWithNewMessage(this.client2, transaction, previous);
      previous = transaction;
    }
  }

  /**
   * Test that multiple clients work and one can disconnect part-way.
   */
  public void testTwoClientsNoExpiryOneDisconnect() {
    TransactionID oldest = new TransactionID(0);
    
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
    TransactionID oldest = new TransactionID(0);
    
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
    TransactionID oldest = new TransactionID(0);
    
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
    TransactionID oldest = new TransactionID(0);
    
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
}
