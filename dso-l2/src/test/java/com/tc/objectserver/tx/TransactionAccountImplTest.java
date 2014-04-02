/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.tx;

import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mscott
 */
public class TransactionAccountImplTest {
  
  private TransactionAccountImpl impl;
  private ClientID               source;
  private ClientID               destination;
  private ClientID               sync;
  
  public TransactionAccountImplTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp() {
    source = new ClientID(1);
    destination = new ClientID(2);
    sync = new ClientID(3);
    impl = new TransactionAccountImpl(source);
  }
  
  @After
  public void tearDown() {
  }

  /**
   * Test of getNodeID method, of class TransactionAccountImpl.
   */
  @Test
  public void testGetNodeID() {
    System.out.println("getNodeID");
    assertEquals(source, impl.getNodeID());
  }

  /**
   */
  @Test
  public void testTransactionProcessing() {
    System.out.println("transactionProcessing");
    Set<ServerTransactionID> txnIDs = new HashSet<ServerTransactionID>();
    txnIDs.add(new ServerTransactionID(source, new TransactionID(1)));
    txnIDs.add(new ServerTransactionID(source, new TransactionID(2)));
    txnIDs.add(new ServerTransactionID(source, new TransactionID(3)));
    impl.incomingTransactions(txnIDs);
/* test with added waitee  */
    impl.addWaitee(destination, new TransactionID(1));
    assertTrue(impl.hasWaitees(new TransactionID(1)));
    impl.applyCommitted(new TransactionID(1));
    impl.processMetaDataCompleted(new TransactionID(1));
    impl.relayTransactionComplete(new TransactionID(1));
    impl.broadcastCompleted(new TransactionID(1));
    
    Set<ServerTransactionID> check = new HashSet<ServerTransactionID>();
    impl.addAllPendingServerTransactionIDsTo(check);
    assertEquals(check.size(), txnIDs.size());
    for ( ServerTransactionID sid : check ) {
      assertTrue(txnIDs.contains(sid));
    }
    assertTrue(impl.removeWaitee(destination, new TransactionID(1)));
    check.clear();
    impl.addAllPendingServerTransactionIDsTo(check);
    assertFalse(check.contains(new ServerTransactionID(source, new TransactionID(1))));
/* test without waitee  */
    impl.applyCommitted(new TransactionID(2));
    impl.processMetaDataCompleted(new TransactionID(2));
    impl.relayTransactionComplete(new TransactionID(2));
    impl.broadcastCompleted(new TransactionID(2));
    check.clear();
    impl.addAllPendingServerTransactionIDsTo(check);
    assertEquals(1, check.size());
    assertFalse(check.contains(new ServerTransactionID(source, new TransactionID(2))));
/*  test an object sync */
    impl.addObjectsSyncedTo(sync, new TransactionID(2));
    check.clear();
    impl.addAllPendingServerTransactionIDsTo(check);
    assertEquals(2, check.size());
    assertTrue(check.contains(new ServerTransactionID(source, new TransactionID(2))));
    impl.removeWaitee(sync, new TransactionID(2));
    check.clear();
    impl.addAllPendingServerTransactionIDsTo(check);
    assertEquals(1, check.size());
    assertFalse(check.contains(new ServerTransactionID(source, new TransactionID(2))));
  }
}
