/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.async.impl.MockSink;
import com.tc.async.impl.OrderedSink;
import com.tc.lang.Recyclable;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.groups.MessageID;
import com.tc.net.groups.SingleNodeGroupManager;
import com.tc.object.ObjectID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.MetaDataReader;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.gtx.GlobalTransactionIDAlreadySetException;
import com.tc.object.locks.LockID;
import com.tc.object.locks.StringLockID;
import com.tc.object.msg.NullMessageRecycler;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.core.api.TestDNA;
import com.tc.objectserver.gtx.TestGlobalTransactionManager;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionImpl;
import com.tc.objectserver.tx.TestServerTransactionManager;
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
  TestGlobalTransactionManager     gtxm;
  TestL2ObjectSyncAckManager       objectSyncAckManager;
  ClientID                         clientID;

  @Override
  public void setUp() throws Exception {
    this.clientID = new ClientID(1);
    this.grpMgr = new SingleNodeGroupManager();
    this.txnMgr = new TestServerTransactionManager();
    this.gtxm = new TestGlobalTransactionManager();
    this.objectSyncAckManager = new TestL2ObjectSyncAckManager();
    this.rtm = new ReplicatedTransactionManagerImpl(this.grpMgr,
                                                    new OrderedSink(TCLogging
                                                        .getLogger(ReplicatedTransactionManagerTest.class),
                                                                    new MockSink()), this.txnMgr, this.gtxm,
                                                    new NullMessageRecycler(), objectSyncAckManager);
  }

  /**
   * Some basic tests, a zillion other scenarios could be tested if only I had time :(
   */
  public void testPassiveUninitialized() throws Exception {

    Set knownIds = new HashSet();
    knownIds.add(new ObjectID(1));
    knownIds.add(new ObjectID(2));

    // two objects are already present
    this.rtm.init(knownIds);

    NullRecyclableMessage message = new NullRecyclableMessage();

    LinkedHashMap txns = createTxns(1, 1, 2, false);
    this.rtm.addCommitedTransactions(this.clientID, txns.keySet(), txns.values(), message);

    // Since both are know oids, transactions should pass thru
    assertAndClear(txns.values());

    // create a txn containing a new Object (OID 3)
    txns = createTxns(1, 3, 1, true);
    this.rtm.addCommitedTransactions(this.clientID, txns.keySet(), txns.values(), message);

    // Should go thru too
    assertAndClear(txns.values());

    // Now create a txn with all three objects
    txns = createTxns(1, 1, 3, false);
    this.rtm.addCommitedTransactions(this.clientID, txns.keySet(), txns.values(), message);

    // Since all are known oids, transactions should pass thru
    assertAndClear(txns.values());

    // Now create a txn with all unknown ObjectIDs (4,5,6)
    txns = createTxns(1, 4, 3, false);
    this.rtm.addCommitedTransactions(this.clientID, txns.keySet(), txns.values(), message);

    // None should be sent thru
    assertTrue(this.txnMgr.incomingTxns.isEmpty());

    // Create more txns with all unknown ObjectIDs (7,8,9)
    LinkedHashMap txns1 = createTxns(1, 7, 1, false);
    this.rtm.addCommitedTransactions(this.clientID, txns1.keySet(), txns1.values(), message);
    LinkedHashMap txns2 = createTxns(1, 8, 2, false);
    this.rtm.addCommitedTransactions(this.clientID, txns2.keySet(), txns2.values(), message);

    // None should be sent thru
    assertTrue(this.txnMgr.incomingTxns.isEmpty());

    // Now create Object Sync Txn for 4,5,6
    LinkedHashMap syncTxns = createTxns(1, 4, 3, true);
    objectSyncAckManager.stxnIDs.addAll(syncTxns.keySet());
    this.rtm.addObjectSyncTransaction((ServerTransaction) syncTxns.values().iterator().next());
    assertTrue(objectSyncAckManager.stxnIDs.containsAll(syncTxns.keySet()));
    objectSyncAckManager.reset();

    // One Compound Transaction containing the object DNA and the delta DNA should be sent to the
    // transactionalObjectManager
    assertTrue(this.txnMgr.incomingTxns.size() == 1);
    ServerTransaction gotTxn = (ServerTransaction) this.txnMgr.incomingTxns.remove(0);
    assertContainsAllAndRemove((ServerTransaction) syncTxns.values().iterator().next(), gotTxn);
    assertContainsAllVersionizedAndRemove((ServerTransaction) txns.values().iterator().next(), gotTxn);
    assertTrue(gotTxn.getChanges().isEmpty());

    // Now send transaction complete for txn1, with new Objects (10), this should clear pending changes for 7
    txns = createTxns(1, 10, 1, true);
    this.rtm.addCommitedTransactions(this.clientID, txns.keySet(), txns.values(), message);
    this.rtm.clearTransactionsBelowLowWaterMark(getNextLowWaterMark(txns1.values()));
    assertAndClear(txns.values());

    // Now create Object Sync txn for 7,8,9
    syncTxns = createTxns(1, 7, 3, true);
    objectSyncAckManager.stxnIDs.addAll(syncTxns.keySet());
    this.rtm.addObjectSyncTransaction((ServerTransaction) syncTxns.values().iterator().next());
    assertTrue(objectSyncAckManager.stxnIDs.containsAll(syncTxns.keySet()));
    objectSyncAckManager.reset();

    // One Compound Transaction containing the object DNA for 7 and object DNA and the delta DNA for 8,9 should be sent
    // to the transactionalObjectManager
    assertTrue(this.txnMgr.incomingTxns.size() == 1);
    gotTxn = (ServerTransaction) this.txnMgr.incomingTxns.remove(0);
    List changes = gotTxn.getChanges();
    assertEquals(5, changes.size());
    DNA dna = (DNA) changes.get(0);
    assertEquals(new ObjectID(7), dna.getObjectID());
    assertFalse(dna.isDelta()); // New object
    dna = (DNA) changes.get(1);
    assertEquals(new ObjectID(8), dna.getObjectID());
    assertFalse(dna.isDelta()); // New object
    dna = (DNA) changes.get(2);
    assertEquals(new ObjectID(8), dna.getObjectID());
    assertTrue(dna.isDelta()); // Change to that object
    dna = (DNA) changes.get(3);
    assertEquals(new ObjectID(9), dna.getObjectID());
    assertFalse(dna.isDelta()); // New object
    dna = (DNA) changes.get(4);
    assertEquals(new ObjectID(9), dna.getObjectID());
    assertTrue(dna.isDelta()); // Change to that object

    // Redo the object sync for objects 7,8,9. Since we already have the objects, the change should be entirely pruned.
    syncTxns = createTxns(1, 7, 3, true);
    objectSyncAckManager.stxnIDs.addAll(syncTxns.keySet());
    this.rtm.addObjectSyncTransaction((ServerTransaction) syncTxns.values().iterator().next());
    assertFalse(objectSyncAckManager.stxnIDs.containsAll(syncTxns.keySet()));
  }

  private GlobalTransactionID getNextLowWaterMark(Collection txns) {
    GlobalTransactionID lwm = GlobalTransactionID.NULL_ID;
    for (Iterator i = txns.iterator(); i.hasNext();) {
      ServerTransaction txn = (ServerTransaction) i.next();
      if (lwm.toLong() < txn.getGlobalTransactionID().toLong()) {
        lwm = txn.getGlobalTransactionID();
      }
    }
    return lwm.next();
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
    assertEquals(new ArrayList(txns), this.txnMgr.incomingTxns);
    this.txnMgr.incomingTxns.clear();
  }

  long bid = 0;
  long sid = 0;
  long tid = 0;

  private LinkedHashMap createTxns(int txnCount, int oidStart, int objectCount, boolean newObjects) {
    LinkedHashMap map = new LinkedHashMap();

    TxnBatchID batchID = new TxnBatchID(this.bid++);
    LockID[] lockIDs = new LockID[] { new StringLockID("1") };
    ObjectStringSerializer serializer = null;
    Map newRoots = Collections.unmodifiableMap(new HashMap());
    TxnType txnType = TxnType.NORMAL;
    List notifies = new LinkedList();

    for (int i = 0; i < txnCount; i++) {
      List dnas = new LinkedList();
      SequenceID sequenceID = new SequenceID(this.sid++);
      TransactionID txID = new TransactionID(this.tid++);
      for (int j = oidStart; j < oidStart + objectCount; j++) {
        dnas.add(new TestDNA(new ObjectID(j), !newObjects));
      }
      ServerTransaction tx = new ServerTransactionImpl(batchID, txID, sequenceID, lockIDs, this.clientID, dnas,
                                                       serializer, newRoots, txnType, notifies,
                                                       DmiDescriptor.EMPTY_ARRAY, new MetaDataReader[0], 1, new long[0]);
      map.put(tx.getServerTransactionID(), tx);
      try {
        tx.setGlobalTransactionID(this.gtxm.getOrCreateGlobalTransactionID(tx.getServerTransactionID()));
      } catch (GlobalTransactionIDAlreadySetException e) {
        throw new AssertionError(e);
      }
    }
    return map;
  }

  private static class NullRecyclableMessage implements Recyclable {

    public void recycle() {
      return;
    }

  }

  private static class TestL2ObjectSyncAckManager implements L2ObjectSyncAckManager {
    Set<ServerTransactionID> stxnIDs = new HashSet<ServerTransactionID>();

    public void reset() {
      stxnIDs.clear();
    }

    public void addObjectSyncMessageToAck(ServerTransactionID stxnID, MessageID requestID) {
      stxnIDs.add(stxnID);
    }

    public void objectSyncComplete() {
      //
    }

    public void ackObjectSyncTxn(ServerTransactionID stxnID) {
      stxnIDs.remove(stxnID);
    }
  }
}
