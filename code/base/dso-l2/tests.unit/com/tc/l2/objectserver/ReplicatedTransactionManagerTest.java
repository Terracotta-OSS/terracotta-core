/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.async.impl.MockSink;
import com.tc.async.impl.OrderedSink;
import com.tc.exception.ImplementMe;
import com.tc.logging.TCLogging;
import com.tc.net.groups.SingleNodeGroupManager;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.context.CommitTransactionContext;
import com.tc.objectserver.context.RecallObjectsContext;
import com.tc.objectserver.core.api.TestDNA;
import com.tc.objectserver.gtx.TestGlobalTransactionManager;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionImpl;
import com.tc.objectserver.tx.TestServerTransactionManager;
import com.tc.objectserver.tx.TransactionalObjectManager;
import com.tc.util.SequenceID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

public class ReplicatedTransactionManagerTest extends TestCase {

  ReplicatedTransactionManagerImpl rtm;
  SingleNodeGroupManager           grpMgr;
  TestServerTransactionManager     txnMgr;
  TestTransactionalObjectManager   txnObjectMgr;
  TestGlobalTransactionManager     gtxm;
  ChannelID                        channelID;

  public void setUp() throws Exception {
    channelID = new ChannelID(1);
    grpMgr = new SingleNodeGroupManager();
    txnMgr = new TestServerTransactionManager();
    txnObjectMgr = new TestTransactionalObjectManager();
    gtxm = new TestGlobalTransactionManager();
    rtm = new ReplicatedTransactionManagerImpl(grpMgr, new OrderedSink(TCLogging
        .getLogger(ReplicatedTransactionManagerTest.class), new MockSink()), txnMgr, txnObjectMgr);
  }

  /**
   * Some basic tests, a zillion other scenarios could be tested if only I had time :(
   */
  public void testPassiveUninitialized() throws Exception {

    Set knownIds = new HashSet();
    knownIds.add(new ObjectID(1));
    knownIds.add(new ObjectID(2));

    // two objects are already present
    rtm.init(knownIds);

    LinkedHashMap txns = createTxns(1, 1, 2, false);
    rtm.addCommitedTransactions(channelID, txns.keySet(), txns.values(), Collections.EMPTY_LIST);

    // Since both are know oids, transactions should pass thru
    assertAndClear(txns.values());

    // create a txn containing a new Object (OID 3)
    txns = createTxns(1, 3, 1, true);
    rtm.addCommitedTransactions(channelID, txns.keySet(), txns.values(), Collections.EMPTY_LIST);

    // Should go thru too
    assertAndClear(txns.values());

    // Now create a txn with all three objects
    txns = createTxns(1, 1, 3, false);
    rtm.addCommitedTransactions(channelID, txns.keySet(), txns.values(), Collections.EMPTY_LIST);

    // Since all are known oids, transactions should pass thru
    assertAndClear(txns.values());

    // Now create a txn with all unknown ObjectIDs (4,5,6)
    txns = createTxns(1, 4, 3, false);
    rtm.addCommitedTransactions(channelID, txns.keySet(), txns.values(), Collections.EMPTY_LIST);

    // None should be sent thru
    assertTrue(txnObjectMgr.incomingTxns.isEmpty());

    // Create more txns with all unknown ObjectIDs (7,8,9)
    LinkedHashMap txns1 = createTxns(1, 7, 1, false);
    rtm.addCommitedTransactions(channelID, txns1.keySet(), txns1.values(), Collections.EMPTY_LIST);
    LinkedHashMap txns2 = createTxns(1, 8, 2, false);
    rtm.addCommitedTransactions(channelID, txns2.keySet(), txns2.values(), Collections.EMPTY_LIST);

    // None should be sent thru
    assertTrue(txnObjectMgr.incomingTxns.isEmpty());

    // Now create Object Sync Txn for 4,5,6
    LinkedHashMap syncTxns = createTxns(1, 4, 3, true);
    rtm.addObjectSyncTransaction((ServerTransaction) syncTxns.values().iterator().next());

    // One Compound Transaction containing the object DNA and the delta DNA should be sent to the
    // transactionalObjectManager
    assertTrue(txnObjectMgr.incomingTxns.size() == 1);
    ServerTransaction gotTxn = (ServerTransaction) txnObjectMgr.incomingTxns.remove(0);
    assertContainsAllAndRemove((ServerTransaction) syncTxns.values().iterator().next(), gotTxn);
    assertContainsAllVersionizedAndRemove((ServerTransaction) txns.values().iterator().next(), gotTxn);
    assertTrue(gotTxn.getChanges().isEmpty());

    // Now send transaction complete for txn2, with new Objects (10), this should clear pending changes for 8,9
    txns = createTxns(1, 10, 1, true);
    rtm.addCommitedTransactions(channelID, txns.keySet(), txns.values(), txns2.keySet());
    assertAndClear(txns.values(), txns2.keySet());

    // Now create Object Sync txn for 7,8,9
    syncTxns = createTxns(1, 7, 3, true);
    rtm.addObjectSyncTransaction((ServerTransaction) syncTxns.values().iterator().next());

    // One Compound Transaction containing the object DNA for 7 and the delta DNA should be sent to the
    // transactionalObjectManager
    assertTrue(txnObjectMgr.incomingTxns.size() == 1);
    gotTxn = (ServerTransaction) txnObjectMgr.incomingTxns.remove(0);
    List changes = gotTxn.getChanges();
    assertEquals(4, changes.size());
    DNA dna = (DNA) changes.get(0);
    assertEquals(new ObjectID(7), dna.getObjectID());
    assertFalse(dna.isDelta()); // New object
    dna = (DNA) changes.get(1);
    assertEquals(new ObjectID(7), dna.getObjectID());
    assertTrue(dna.isDelta()); // Change to that object
    dna = (DNA) changes.get(2);
    assertEquals(new ObjectID(8), dna.getObjectID());
    assertFalse(dna.isDelta()); // New object
    dna = (DNA) changes.get(3);
    assertEquals(new ObjectID(9), dna.getObjectID());
    assertFalse(dna.isDelta()); // New object

  }

  private void assertContainsAllVersionizedAndRemove(ServerTransaction expected, ServerTransaction got) {
    List c1 = expected.getChanges();
    List c2 = got.getChanges();
    assertEquals(c1.size(), c2.size());
    for (Iterator i = c2.iterator(); i.hasNext();) {
      DNA dna = (DNA) i.next();
      assertEquals(expected.getGlobalTransactionID().toLong(), dna.getVersion());
      boolean found = false;
      for (Iterator j = c1.iterator(); j.hasNext();) {
        DNA orgDNA = (DNA) j.next();
        // XXX:: This depends on the fact that we dont create a resetable cursor when creating a VersionizedDNAWrapper
        // in ReplicatedTransactionManagerImpl
        if (dna.getCursor() == orgDNA.getCursor()) {
          found = true;
          break;
        }
      }
      assertTrue(found);
      i.remove();
    }
  }

  private void assertContainsAllAndRemove(ServerTransaction expected, ServerTransaction got) {
    List c1 = expected.getChanges();
    List c2 = got.getChanges();
    for (Iterator i = c1.iterator(); i.hasNext();) {
      DNA dna = (DNA) i.next();
      assertTrue(c2.remove(dna));
    }
  }

  private void assertAndClear(Collection txns) {
    assertEquals(new ArrayList(txns), txnObjectMgr.incomingTxns);
    txnObjectMgr.incomingTxns.clear();
  }

  private void assertAndClear(Collection txns, Set completedTxnIDs) {
    assertAndClear(txns);
    assertEquals(completedTxnIDs, txnObjectMgr.completedTxns);
    txnObjectMgr.completedTxns.clear();
  }

  long bid = 0;
  long sid = 0;
  long tid = 0;

  private LinkedHashMap createTxns(int txnCount, int oidStart, int objectCount, boolean newObjects) {
    LinkedHashMap map = new LinkedHashMap();

    TxnBatchID batchID = new TxnBatchID(bid++);
    LockID[] lockIDs = new LockID[] { new LockID("1") };
    ObjectStringSerializer serializer = null;
    Map newRoots = Collections.unmodifiableMap(new HashMap());
    TxnType txnType = TxnType.NORMAL;
    List notifies = new LinkedList();

    for (int i = 0; i < txnCount; i++) {
      List dnas = new LinkedList();
      SequenceID sequenceID = new SequenceID(sid++);
      TransactionID txID = new TransactionID(tid++);
      for (int j = oidStart; j < oidStart + objectCount; j++) {
        dnas.add(new TestDNA(new ObjectID(j), !newObjects));
      }
      ServerTransaction tx = new ServerTransactionImpl(gtxm, batchID, txID, sequenceID, lockIDs, channelID, dnas,
                                                       serializer, newRoots, txnType, notifies,
                                                       DmiDescriptor.EMPTY_ARRAY);
      map.put(tx.getServerTransactionID(), tx);
    }
    return map;
  }

  public class TestTransactionalObjectManager implements TransactionalObjectManager {

    List incomingTxns  = new ArrayList();
    Set  completedTxns = new HashSet();

    public void addTransactions(Collection txns, Collection completedTxnIds) {
      incomingTxns.addAll(txns);
      completedTxns.addAll(completedTxnIds);
    }

    public boolean applyTransactionComplete(ServerTransactionID stxnID) {
      throw new ImplementMe();
    }

    public void commitTransactionsComplete(CommitTransactionContext ctc) {
      throw new ImplementMe();
    }

    public void lookupObjectsForTransactions() {
      throw new ImplementMe();
    }

    public void processApplyComplete() {
      throw new ImplementMe();
    }

    public void recallAllCheckedoutObject() {
      throw new ImplementMe();
    }

    public void recallCheckedoutObject(RecallObjectsContext roc) {
      throw new ImplementMe();
    }

  }

}
