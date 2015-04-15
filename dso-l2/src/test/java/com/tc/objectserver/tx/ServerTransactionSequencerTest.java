/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.tx;

import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.MetaDataReader;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.locks.LockID;
import com.tc.object.locks.StringLockID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.context.TransactionLookupContext;
import com.tc.objectserver.core.api.TestDNA;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.SequenceID;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class ServerTransactionSequencerTest extends TCTestCase {

  private int                            sqID;
  private int                            txnID;
  private int                            batchID;
  private ClientID                       clientID;
  private ServerTransactionSequencerImpl sequencer;
  private int                            start;

  @Override
  public void setUp() throws Exception {
    this.txnID = 100;
    this.sqID = 100;
    this.batchID = 100;
    this.start = 1;
    this.clientID = new ClientID(0);
    this.sequencer = new ServerTransactionSequencerImpl();
  }

  // Test 1
  // Nothing is pending - disjoint anyway
  public void testNoPendingDisjointTxn() throws Exception {
    List<ServerTransaction> txns = createDisjointTxns(5);
    this.sequencer.addTransactionLookupContexts(createTxnLookupContexts(txns));
    assertEquals(txns, getAllTxnsPossible());
    assertFalse(this.sequencer.isPending(txns));
  }

  private Collection<TransactionLookupContext> createTxnLookupContexts(List<ServerTransaction> txns) {
    List<TransactionLookupContext> contexts = new ArrayList<TransactionLookupContext>();
    for (final Object txn1 : txns) {
      ServerTransaction txn = (ServerTransaction)txn1;
      contexts.add(new TransactionLookupContext(txn, true));
    }
    return contexts;
  }

  // Test 2
  // Nothing is pending - not disjoint though
  public void testNoPendingJointTxn() throws Exception {
    List<ServerTransaction> txns = createIntersectingLocksTxns(5);
    this.sequencer.addTransactionLookupContexts(createTxnLookupContexts(txns));
    assertEquals(txns, getAllTxnsPossible());
    assertFalse(this.sequencer.isPending(txns));
  }

  // Test 3
  // txn goes pending - disjoint anyway
  public void testPendingDisJointTxn() throws Exception {
    List<ServerTransaction> txns = createDisjointTxns(5);
    this.sequencer.addTransactionLookupContexts(createTxnLookupContexts(txns));
    ServerTransaction t1 = this.sequencer.getNextTxnLookupContextToProcess().getTransaction();
    assertNotNull(t1);
    // Make it pending
    this.sequencer.makePending(t1);
    assertTrue(this.sequencer.isPending(txns));
    txns.remove(t1);
    assertEquals(txns, getAllTxnsPossible());
    assertTrue(this.sequencer.isPending(Arrays.asList(t1)));
    // No more txns
    assertNull(this.sequencer.getNextTxnLookupContextToProcess());
    this.sequencer.makeUnpending(t1);
    assertFalse(this.sequencer.isPending(Arrays.asList(t1)));
    // No more txns
    assertNull(this.sequencer.getNextTxnLookupContextToProcess());
  }

  // Test 4 - Removed it is not valid anymore

  // Test 5
  // txn goes pending - intersecting set
  public void testPendingJointAtObjectsTxn() throws Exception {
    List<ServerTransaction> txns = createIntersectingObjectsTxns(5);
    this.sequencer.addTransactionLookupContexts(createTxnLookupContexts(txns));
    ServerTransaction t1 = this.sequencer.getNextTxnLookupContextToProcess().getTransaction();
    assertNotNull(t1);
    // Make it pending
    this.sequencer.makePending(t1);
    assertTrue(this.sequencer.isPending(txns));
    txns.remove(t1);

    // Since locks are common no txn should be available
    assertNull(this.sequencer.getNextTxnLookupContextToProcess());
    assertTrue(this.sequencer.isPending(Arrays.asList(t1)));
    this.sequencer.makeUnpending(t1);
    assertFalse(this.sequencer.isPending(Arrays.asList(t1)));
    // Rest of the txns
    assertEquals(txns, getAllTxnsPossible());
  }

  // Test 6
  // txn goes pending - intersecting set - Note : Locks are not considered anymore.
  public void testPendingJointAtBothLocksAndObjectsTxn() throws Exception {
    List<ServerTransaction> txns = createIntersectingLocksObjectsTxns(5);
    this.sequencer.addTransactionLookupContexts(createTxnLookupContexts(txns));
    ServerTransaction t1 = this.sequencer.getNextTxnLookupContextToProcess().getTransaction();
    assertNotNull(t1);
    // Make it pending
    this.sequencer.makePending(t1);
    assertTrue(this.sequencer.isPending(txns));
    txns.remove(t1);

    // Since locks are common no txn should be available
    assertNull(this.sequencer.getNextTxnLookupContextToProcess());
    assertTrue(this.sequencer.isPending(Arrays.asList(t1)));
    this.sequencer.makeUnpending(t1);
    assertFalse(this.sequencer.isPending(Arrays.asList(t1)));
    // Rest of the txns
    assertEquals(txns, getAllTxnsPossible());
  }

  // Test 7
  // Test error conditions
  public void testErrorConditions() throws Exception {
    // Call makepending twice
    List<ServerTransaction> txns = createDisjointTxns(5);
    this.sequencer.addTransactionLookupContexts(createTxnLookupContexts(txns));
    ServerTransaction t1 = this.sequencer.getNextTxnLookupContextToProcess().getTransaction();
    assertNotNull(t1);
    this.sequencer.makePending(t1);
    assertTrue(this.sequencer.isPending(txns));
    try {
      this.sequencer.makePending(t1);
      fail();
    } catch (Throwable t) {
      // expected
    }

    // Call make unpending for something that is not pending
    ServerTransaction t2 = this.sequencer.getNextTxnLookupContextToProcess().getTransaction();
    assertNotNull(t2);
    try {
      this.sequencer.makeUnpending(t2);
      fail();
    } catch (Throwable t) {
      // expected
    }
    this.sequencer.makeUnpending(t1);
  }

  public void testOrderingByOID() {
    List<ServerTransaction> txns = new ArrayList<ServerTransaction>();

    int lock = 0;

    ServerTransaction txn1 = new ServerTransactionImpl(new TxnBatchID(this.batchID), new TransactionID(1),
                                                       new SequenceID(this.sqID++), createLocks(lock, lock++),
                                                       this.clientID, createDNAs(1, 1),
                                                       new ObjectStringSerializerImpl(), Collections.EMPTY_MAP,
                                                       TxnType.NORMAL, new LinkedList(),
                                                       new MetaDataReader[0], 1, new long[0]);

    ServerTransaction txn2 = new ServerTransactionImpl(new TxnBatchID(this.batchID), new TransactionID(2),
                                                       new SequenceID(this.sqID++), createLocks(lock, lock++),
                                                       this.clientID, createDNAs(2, 2),
                                                       new ObjectStringSerializerImpl(), Collections.EMPTY_MAP,
                                                       TxnType.NORMAL, new LinkedList(),
                                                       new MetaDataReader[0], 1, new long[0]);

    ServerTransaction txn3 = new ServerTransactionImpl(new TxnBatchID(this.batchID), new TransactionID(3),
                                                       new SequenceID(this.sqID++), createLocks(lock, lock++),
                                                       this.clientID, createDNAs(2, 3),
                                                       new ObjectStringSerializerImpl(), Collections.EMPTY_MAP,
                                                       TxnType.NORMAL, new LinkedList(),
                                                       new MetaDataReader[0], 1, new long[0]);

    ServerTransaction txn4 = new ServerTransactionImpl(new TxnBatchID(this.batchID), new TransactionID(4),
                                                       new SequenceID(this.sqID++), createLocks(lock, lock),
                                                       this.clientID, createDNAs(1, 2),
                                                       new ObjectStringSerializerImpl(), Collections.EMPTY_MAP,
                                                       TxnType.NORMAL, new LinkedList(),
                                                       new MetaDataReader[0], 1, new long[0]);

    txns.add(txn1);
    txns.add(txn2);
    txns.add(txn3);
    txns.add(txn4);
    this.sequencer.addTransactionLookupContexts(createTxnLookupContexts(txns));

    this.sequencer.makePending(this.sequencer.getNextTxnLookupContextToProcess().getTransaction());
    this.sequencer.makePending(this.sequencer.getNextTxnLookupContextToProcess().getTransaction());

    Object o;
    o = this.sequencer.getNextTxnLookupContextToProcess();
    Assert.assertNull(o);
    o = this.sequencer.getNextTxnLookupContextToProcess();
    Assert.assertNull(o);

    this.sequencer.makeUnpending(txn2);
    this.sequencer.makeUnpending(txn1);

    ServerTransaction shouldBe3 = this.sequencer.getNextTxnLookupContextToProcess().getTransaction();
    ServerTransaction shouldBe4 = this.sequencer.getNextTxnLookupContextToProcess().getTransaction();

    Assert.assertEquals(txn3, shouldBe3);
    Assert.assertEquals(txn4, shouldBe4);
  }

  public void testRandom() {
    for (int i = 0; i < 100; i++) {
      System.err.println("Running testRandom : " + i);
      doRandom();
      this.sequencer = new ServerTransactionSequencerImpl();
    }
  }

  public void testRandomFailedSeed1() {
    long seed = -7748167846395034562L;
    System.err.println("Testing failed seed : " + seed);
    doRandom(seed);
  }

  public void testRandomFailedSeed2() {
    long seed = -149113776740941224L;
    System.err.println("Testing failed seed : " + seed);
    doRandom(seed);
  }

  // XXX: multi-threaded version of this?
  private void doRandom() {
    final long seed = new SecureRandom().nextLong();
    System.err.println("seed is " + seed);
    doRandom(seed);
  }

  private void doRandom(long seed) {
    Random rnd = new Random(seed);

    int lock = 0;
    final int numObjects = 25;
    long versionsIn[] = new long[numObjects];
    long versionsRecv[] = new long[numObjects];

    Set<ServerTransaction> pending = new HashSet<ServerTransaction>();

    for (int loop = 0; loop < 5000; loop++) {
      List<ServerTransaction> txns = new ArrayList<ServerTransaction>();
      for (int i = 0, n = rnd.nextInt(3) + 1; i < n; i++) {
        txns.add(createRandomTxn(rnd.nextInt(3) + 1, versionsIn, rnd, lock++));
      }

      this.sequencer.addTransactionLookupContexts(createTxnLookupContexts(txns));

      TransactionLookupContext next;
      while ((next = this.sequencer.getNextTxnLookupContextToProcess()) != null) {
        if (rnd.nextInt(3) == 0) {
          ServerTransaction txn = next.getTransaction();
          this.sequencer.makePending(txn);
          pending.add(next.getTransaction());
          continue;
        }

        processTransaction(next.getTransaction(), versionsRecv);

        if (pending.size() > 0 && rnd.nextInt(4) == 0) {
          for (int i = 0, n = rnd.nextInt(pending.size()); i < n; i++) {
            Iterator iter = pending.iterator();
            ServerTransaction pendingTxn = (ServerTransaction) iter.next();
            iter.remove();
            processTransaction(pendingTxn, versionsRecv);
            this.sequencer.makeUnpending(pendingTxn);
          }
        }
      }

    }
  }

  private void processTransaction(ServerTransaction next, long[] versionsRecv) {
    for (final Object o : next.getChanges()) {
      TestDNA dna = (TestDNA)o;
      int oid = (int)dna.getObjectID().toLong();
      long ver = dna.version;
      long expect = versionsRecv[oid] + 1;
      if (expect != ver) {
        //
        throw new AssertionError(oid + " : Expected change to increment to version " + expect
                                 + ", but change was to version " + ver);
      }
      versionsRecv[oid] = ver;
    }
  }

  private ServerTransaction createRandomTxn(int numObjects, long[] versions, Random rnd, int lockID) {
    Map<Integer, TestDNA> dnas = new HashMap<Integer, TestDNA>();
    while (numObjects > 0) {
      int i = rnd.nextInt(versions.length);
      if (!dnas.containsKey(i)) {
        TestDNA dna = new TestDNA(new ObjectID(i));
        dna.version = ++versions[i];
        dnas.put(i, dna);
        numObjects--;
      }
    }

    return new ServerTransactionImpl(new TxnBatchID(this.batchID), new TransactionID(this.txnID++),
                                     new SequenceID(this.sqID++), createLocks(lockID, lockID), this.clientID,
                                     new ArrayList<TestDNA>(dnas.values()), new ObjectStringSerializerImpl(),
                                     Collections.EMPTY_MAP, TxnType.NORMAL, new LinkedList(),
                                     new MetaDataReader[0], 1, new long[0]);
  }

  private List getAllTxnsPossible() {
    List<ServerTransaction> txns = new ArrayList<ServerTransaction>();
    TransactionLookupContext txnLC;
    while ((txnLC = this.sequencer.getNextTxnLookupContextToProcess()) != null) {
      txns.add(txnLC.getTransaction());
    }
    return txns;
  }

  private List<ServerTransaction> createDisjointTxns(int count) {
    List<ServerTransaction> txns = new ArrayList<ServerTransaction>();
    this.batchID++;
    int j = 3;
    while (count-- > 0) {
      int e = this.start + j;
      txns.add(new ServerTransactionImpl(new TxnBatchID(this.batchID), new TransactionID(this.txnID++),
                                         new SequenceID(this.sqID++), createLocks(this.start, e), this.clientID,
                                         createDNAs(this.start, e), new ObjectStringSerializerImpl(),
                                         Collections.EMPTY_MAP, TxnType.NORMAL, new LinkedList(),
                                         new MetaDataReader[0], 1, new long[0]));
      this.start = e + 1;
    }
    return txns;
  }

  private List<ServerTransaction> createIntersectingLocksTxns(int count) {
    List<ServerTransaction> txns = new ArrayList<ServerTransaction>();
    this.batchID++;
    int j = 3;
    while (count-- > 0) {
      int e = this.start + j;
      txns.add(new ServerTransactionImpl(new TxnBatchID(this.batchID), new TransactionID(this.txnID++),
                                         new SequenceID(this.sqID++), createLocks(this.start, e + j), this.clientID,
                                         createDNAs(this.start, e), new ObjectStringSerializerImpl(),
                                         Collections.EMPTY_MAP, TxnType.NORMAL, new LinkedList(),
                                         new MetaDataReader[0], 1, new long[0]));
      this.start = e + 1;
    }
    return txns;
  }

  private List<ServerTransaction> createIntersectingObjectsTxns(int count) {
    List<ServerTransaction> txns = new ArrayList<ServerTransaction>();
    this.batchID++;
    int j = 3;
    while (count-- > 0) {
      int e = this.start + j;
      txns.add(new ServerTransactionImpl(new TxnBatchID(this.batchID), new TransactionID(this.txnID++),
                                         new SequenceID(this.sqID++), createLocks(this.start, e), this.clientID,
                                         createDNAs(this.start, e + j), new ObjectStringSerializerImpl(),
                                         Collections.EMPTY_MAP, TxnType.NORMAL, new LinkedList(),
                                         new MetaDataReader[0], 1, new long[0]));
      this.start = e + 1;
    }
    return txns;
  }

  private List<ServerTransaction> createIntersectingLocksObjectsTxns(int count) {
    List<ServerTransaction> txns = new ArrayList<ServerTransaction>();
    this.batchID++;
    int j = 3;
    while (count-- > 0) {
      int e = this.start + j;
      txns.add(new ServerTransactionImpl(new TxnBatchID(this.batchID), new TransactionID(this.txnID++),
                                         new SequenceID(this.sqID++), createLocks(this.start, e + j), this.clientID,
                                         createDNAs(this.start, e + j), new ObjectStringSerializerImpl(),
                                         Collections.EMPTY_MAP, TxnType.NORMAL, new LinkedList(),
                                         new MetaDataReader[0], 1, new long[0]));
      this.start = e + 1;
    }
    return txns;
  }

  private List<TestDNA> createDNAs(int s, int e) {
    List<TestDNA> dnas = new ArrayList<TestDNA>();
    for (int i = s; i <= e; i++) {
      dnas.add(new TestDNA(new ObjectID(i)));
    }
    return dnas;
  }

  private LockID[] createLocks(int s, int e) {
    LockID[] locks = new LockID[e - s + 1];
    for (int j = s; j <= e; j++) {
      locks[j - s] = new StringLockID("@" + j);
    }
    return locks;
  }

}
