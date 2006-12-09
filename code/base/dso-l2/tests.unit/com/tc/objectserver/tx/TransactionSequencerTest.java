/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.core.api.TestDNA;
import com.tc.test.TCTestCase;
import com.tc.util.SequenceID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class TransactionSequencerTest extends TCTestCase {

  private int                  sqID;
  private int                  txnID;
  private int                  batchID;
  private ChannelID            channelID;
  private TransactionSequencer sequencer;
  private int                  start;

  public void setUp() throws Exception {
    txnID = 100;
    sqID = 100;
    batchID = 100;
    start = 1;
    channelID = new ChannelID(0);
    sequencer = new TransactionSequencer();
  }

  // Test 1
  // Nothing is pending - disjoint anyway
  public void testNoPendingDisjointTxn() throws Exception {
    List txns = createDisjointTxns(5);
    sequencer.addTransactions(txns);
    assertEquals(txns, getAllTxnsPossible());
    assertFalse(sequencer.isPending(txns));
  }

  // Test 2
  // Nothing is pending - not disjoint though
  public void testNoPendingJointTxn() throws Exception {
    List txns = createIntersectingLocksTxns(5);
    sequencer.addTransactions(txns);
    assertEquals(txns, getAllTxnsPossible());
    assertFalse(sequencer.isPending(txns));
  }

  // Test 3
  // txn goes pending - disjoint anyway
  public void testPendingDisJointTxn() throws Exception {
    List txns = createDisjointTxns(5);
    sequencer.addTransactions(txns);
    ServerTransaction t1 = sequencer.getNextTxnToProcess();
    assertNotNull(t1);
    // Make it pending
    sequencer.makePending(t1);
    assertTrue(sequencer.isPending(txns));
    txns.remove(t1);
    assertEquals(txns, getAllTxnsPossible());
    assertTrue(sequencer.isPending(Arrays.asList(new Object[] { t1 })));
    // Nomore txns
    assertNull(sequencer.getNextTxnToProcess());
    sequencer.processedPendingTxn(t1);
    assertFalse(sequencer.isPending(Arrays.asList(new Object[] { t1 })));
    // Nomore txns
    assertNull(sequencer.getNextTxnToProcess());
  }

  // Test 4
  // txn goes pending - intersecting set
  public void testPendingJointAtLocksTxn() throws Exception {
    List txns = createIntersectingLocksTxns(5);
    sequencer.addTransactions(txns);
    ServerTransaction t1 = sequencer.getNextTxnToProcess();
    assertNotNull(t1);
    // Make it pending
    sequencer.makePending(t1);
    assertTrue(sequencer.isPending(txns));
    txns.remove(t1);

    // Since locks are common no txn should be available
    assertNull(sequencer.getNextTxnToProcess());
    assertTrue(sequencer.isPending(Arrays.asList(new Object[] { t1 })));
    sequencer.processedPendingTxn(t1);
    assertFalse(sequencer.isPending(Arrays.asList(new Object[] { t1 })));
    // Rest of the txns
    assertEquals(txns, getAllTxnsPossible());
  }

  // Test 5
  // txn goes pending - intersecting set
  public void testPendingJointAtObjectsTxn() throws Exception {
    List txns = createIntersectingObjectsTxns(5);
    sequencer.addTransactions(txns);
    ServerTransaction t1 = sequencer.getNextTxnToProcess();
    assertNotNull(t1);
    // Make it pending
    sequencer.makePending(t1);
    assertTrue(sequencer.isPending(txns));
    txns.remove(t1);

    // Since locks are common no txn should be available
    assertNull(sequencer.getNextTxnToProcess());
    assertTrue(sequencer.isPending(Arrays.asList(new Object[] { t1 })));
    sequencer.processedPendingTxn(t1);
    assertFalse(sequencer.isPending(Arrays.asList(new Object[] { t1 })));
    // Rest of the txns
    assertEquals(txns, getAllTxnsPossible());
  }

  // Test 6
  // txn goes pending - intersecting set
  public void testPendingJointAtBothLocksAndObjectsTxn() throws Exception {
    List txns = createIntersectingLocksObjectsTxns(5);
    sequencer.addTransactions(txns);
    ServerTransaction t1 = sequencer.getNextTxnToProcess();
    assertNotNull(t1);
    // Make it pending
    sequencer.makePending(t1);
    assertTrue(sequencer.isPending(txns));
    txns.remove(t1);

    // Since locks are common no txn should be available
    assertNull(sequencer.getNextTxnToProcess());
    assertTrue(sequencer.isPending(Arrays.asList(new Object[] { t1 })));
    sequencer.processedPendingTxn(t1);
    assertFalse(sequencer.isPending(Arrays.asList(new Object[] { t1 })));
    // Rest of the txns
    assertEquals(txns, getAllTxnsPossible());
  }

  // Test 7
  // Test error conditions
  public void testErrorConditions() throws Exception {
    // Call makepending twice
    List txns = createDisjointTxns(5);
    sequencer.addTransactions(txns);
    ServerTransaction t1 = sequencer.getNextTxnToProcess();
    assertNotNull(t1);
    sequencer.makePending(t1);
    assertTrue(sequencer.isPending(txns));
    try {
      sequencer.makePending(t1);
      fail();
    } catch (Throwable t) {
      // expected
    }

    // Call make unpending for something that is not pendin
    ServerTransaction t2 = sequencer.getNextTxnToProcess();
    assertNotNull(t2);
    try {
      sequencer.processedPendingTxn(t2);
      fail();
    } catch (Throwable t) {
      // expected
    }
    sequencer.processedPendingTxn(t1);
  }

  private List getAllTxnsPossible() {
    List txns = new ArrayList();
    ServerTransaction txn;
    while ((txn = sequencer.getNextTxnToProcess()) != null) {
      txns.add(txn);
    }
    return txns;
  }

  private List createDisjointTxns(int count) {
    List txns = new ArrayList();
    batchID++;
    int j = 3;
    while (count-- > 0) {
      int e = start + j;
      txns.add(new ServerTransactionImpl(new TxnBatchID(batchID), new TransactionID(txnID++), new SequenceID(sqID++),
                                         createLocks(start, e), channelID, createDNAs(start, e),
                                         new ObjectStringSerializer(), Collections.EMPTY_MAP, TxnType.NORMAL,
                                         new LinkedList()));
      start = e + 1;
    }
    return txns;
  }

  private List createIntersectingLocksTxns(int count) {
    List txns = new ArrayList();
    batchID++;
    int j = 3;
    while (count-- > 0) {
      int e = start + j;
      txns.add(new ServerTransactionImpl(new TxnBatchID(batchID), new TransactionID(txnID++), new SequenceID(sqID++),
                                         createLocks(start, e + j), channelID, createDNAs(start, e),
                                         new ObjectStringSerializer(), Collections.EMPTY_MAP, TxnType.NORMAL,
                                         new LinkedList()));
      start = e + 1;
    }
    return txns;
  }

  private List createIntersectingObjectsTxns(int count) {
    List txns = new ArrayList();
    batchID++;
    int j = 3;
    while (count-- > 0) {
      int e = start + j;
      txns.add(new ServerTransactionImpl(new TxnBatchID(batchID), new TransactionID(txnID++), new SequenceID(sqID++),
                                         createLocks(start, e), channelID, createDNAs(start, e + j),
                                         new ObjectStringSerializer(), Collections.EMPTY_MAP, TxnType.NORMAL,
                                         new LinkedList()));
      start = e + 1;
    }
    return txns;
  }

  private List createIntersectingLocksObjectsTxns(int count) {
    List txns = new ArrayList();
    batchID++;
    int j = 3;
    while (count-- > 0) {
      int e = start + j;
      txns.add(new ServerTransactionImpl(new TxnBatchID(batchID), new TransactionID(txnID++), new SequenceID(sqID++),
                                         createLocks(start, e + j), channelID, createDNAs(start, e + j),
                                         new ObjectStringSerializer(), Collections.EMPTY_MAP, TxnType.NORMAL,
                                         new LinkedList()));
      start = e + 1;
    }
    return txns;
  }

  private List createDNAs(int s, int e) {
    List dnas = new ArrayList();
    for (int i = s; i <= e; i++) {
      dnas.add(new TestDNA(new ObjectID(i)));
    }
    return dnas;
  }

  private LockID[] createLocks(int s, int e) {
    LockID[] locks = new LockID[e - s + 1];
    for (int j = s; j <= e; j++) {
      locks[j - s] = new LockID("@" + j);
    }
    return locks;
  }
}
