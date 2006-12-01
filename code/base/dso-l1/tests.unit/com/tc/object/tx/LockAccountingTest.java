/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.lockmanager.api.LockID;

import java.util.Collection;
import java.util.HashSet;

import junit.framework.TestCase;

public class LockAccountingTest extends TestCase {

  private LockAccounting la;
  private LockID lockID1;
  private LockID lockID2;
  private LockID lockID3;
  private LockID lockID4;
  private TransactionID txID1;
  private TransactionID txID2;
  private TransactionID txID3;
  private TransactionID txID4;
  private Collection lock1Txs;
  private Collection lock2Txs;
  private Collection lock3Txs;
  private Collection lock4Txs;
  
  public void setUp() {
    la = new LockAccounting();
  }
  
  public void tests() throws Exception {
    lockID1 = new LockID("lock1");
    lockID2 = new LockID("lock2");
    lockID3 = new LockID("lock3");
    lockID4 = new LockID("lock4");
    txID1 = new TransactionID(1);
    txID2 = new TransactionID(2);
    txID3 = new TransactionID(3);
    txID4 = new TransactionID(4);
    lock1Txs = new HashSet();
    lock2Txs = new HashSet();
    lock3Txs = new HashSet();
    lock4Txs = new HashSet();
    Collection tx1locks = new HashSet();
    Collection tx2locks = new HashSet();
    Collection tx3locks = new HashSet();
    Collection tx4locks = new HashSet();
    
    tx1locks.add(lockID1);
    lock1Txs.add(txID1);
    tx1locks.add(lockID2);
    lock2Txs.add(txID1);
    
    tx2locks.add(lockID1);
    lock1Txs.add(txID2);
    tx2locks.add(lockID2);
    lock2Txs.add(txID2);
    
    tx3locks.add(lockID3);
    lock3Txs.add(txID3);
    
    tx4locks.add(lockID4);
    lock4Txs.add(txID4);
    
    la.add(txID1, tx1locks);
    la.add(txID2, tx2locks);
    la.add(txID3, tx3locks);
    la.add(txID4, tx4locks);

    verifyGetTransactionsFor();
    
    la.acknowledge(txID1);
    assertTrue(lock1Txs.remove(txID1));
    assertTrue(lock2Txs.remove(txID1));
    assertFalse(lock3Txs.remove(txID1));
    assertFalse(lock4Txs.remove(txID1));
    verifyGetTransactionsFor();
    
    la.acknowledge(txID2);
    assertTrue(lock1Txs.remove(txID2));
    assertTrue(lock2Txs.remove(txID2));
    assertFalse(lock3Txs.remove(txID2));
    assertFalse(lock4Txs.remove(txID2));
    verifyGetTransactionsFor();
    
    la.acknowledge(txID3);
    assertFalse(lock1Txs.remove(txID3));
    assertFalse(lock2Txs.remove(txID3));
    assertTrue(lock3Txs.remove(txID3));
    assertFalse(lock4Txs.remove(txID3));
    verifyGetTransactionsFor();
    
    la.acknowledge(txID4);
    assertFalse(lock1Txs.remove(txID4));
    assertFalse(lock2Txs.remove(txID4));
    assertFalse(lock3Txs.remove(txID4));
    assertTrue(lock4Txs.remove(txID4));
    verifyGetTransactionsFor();
    
    assertTrue(lock1Txs.isEmpty());
    assertTrue(lock2Txs.isEmpty());
    assertTrue(lock3Txs.isEmpty());
    assertTrue(lock4Txs.isEmpty());
    assertTrue(la.isEmpty());
  }

  private void verifyGetTransactionsFor() {
    assertEquals(lock1Txs, la.getTransactionsFor(lockID1));
    assertEquals(lock2Txs, la.getTransactionsFor(lockID2));
    assertEquals(lock3Txs, la.getTransactionsFor(lockID3));
    assertEquals(lock4Txs, la.getTransactionsFor(lockID4));
  }
  
}
