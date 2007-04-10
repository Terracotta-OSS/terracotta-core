/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.context.ApplyCompleteEventContext;
import com.tc.objectserver.context.ApplyTransactionContext;
import com.tc.objectserver.context.CommitTransactionContext;
import com.tc.objectserver.context.LookupEventContext;
import com.tc.objectserver.context.RecallObjectsContext;
import com.tc.objectserver.core.api.TestDNA;
import com.tc.objectserver.core.impl.TestManagedObject;
import com.tc.objectserver.gtx.TestGlobalTransactionManager;
import com.tc.objectserver.impl.TestObjectManager;
import com.tc.test.TCTestCase;
import com.tc.util.SequenceID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TransactionalObjectManagerTest extends TCTestCase {

  TestObjectManager                      objectManager;
  TransactionSequencer                   sequencer;
  TestTransactionalStageCoordinator      coordinator;
  private TransactionalObjectManagerImpl txObjectManager;
  private TestGlobalTransactionManager   gtxMgr;

  public void setUp() {
    objectManager = new TestObjectManager();
    sequencer = new TransactionSequencer();
    coordinator = new TestTransactionalStageCoordinator();
    gtxMgr = new TestGlobalTransactionManager();
    txObjectManager = new TransactionalObjectManagerImpl(objectManager, sequencer, gtxMgr, coordinator);
  }

  // This test is added to reproduce a failure. More test are needed for TransactionalObjectManager
  public void testTxnObjectManagerRecallAllOnGC() throws Exception {

    Map changes = new HashMap();

    changes.put(new ObjectID(1), new TestDNA(new ObjectID(1)));
    changes.put(new ObjectID(2), new TestDNA(new ObjectID(2)));

    ServerTransaction stxn1 = new ServerTransactionImpl(gtxMgr, new TxnBatchID(1), new TransactionID(1),
                                                        new SequenceID(1), new LockID[0], new ChannelID(2),
                                                        new ArrayList(changes.values()), new ObjectStringSerializer(),
                                                        Collections.EMPTY_MAP, TxnType.NORMAL, new LinkedList(),
                                                        DmiDescriptor.EMPTY_ARRAY);
    List txns = new ArrayList();
    txns.add(stxn1);

    txObjectManager.addTransactions(new ChannelID(2), txns, Collections.EMPTY_LIST);

    // Lookup context should have been fired
    LookupEventContext loc = (LookupEventContext) coordinator.lookupSink.queue.remove(0);
    assertNotNull(loc);
    assertTrue(coordinator.lookupSink.queue.isEmpty());

    txObjectManager.lookupObjectsForTransactions();

    // for txn1 - ObjectID 1, 2/home/ssubbiah/2.2.1/code/base/tcbuild check_prep dso-l2 unit
    Object args[] = (Object[]) objectManager.lookupObjectForCreateIfNecessaryContexts.take();
    assertNotNull(args);

    // Apply should not have been called as we dont have Obejct 1, 2
    ApplyTransactionContext aoc = (ApplyTransactionContext) coordinator.applySink.queue.remove(0);
    assertTrue(stxn1 == aoc.getTxn());
    assertNotNull(aoc);
    assertTrue(coordinator.applySink.queue.isEmpty());

    // Next txn
    changes.put(new ObjectID(3), new TestDNA(new ObjectID(3)));
    changes.put(new ObjectID(4), new TestDNA(new ObjectID(4)));

    ServerTransaction stxn2 = new ServerTransactionImpl(gtxMgr, new TxnBatchID(2), new TransactionID(2),
                                                        new SequenceID(1), new LockID[0], new ChannelID(2),
                                                        new ArrayList(changes.values()), new ObjectStringSerializer(),
                                                        Collections.EMPTY_MAP, TxnType.NORMAL, new LinkedList(),
                                                        DmiDescriptor.EMPTY_ARRAY);

    txns.clear();
    txns.add(stxn2);

    txObjectManager.addTransactions(new ChannelID(2), txns, Collections.EMPTY_LIST);

    // Lookup context should have been fired
    loc = (LookupEventContext) coordinator.lookupSink.queue.remove(0);
    assertNotNull(loc);
    assertTrue(coordinator.lookupSink.queue.isEmpty());

    objectManager.makePending = true;
    txObjectManager.lookupObjectsForTransactions();

    // for txn2, ObjectID 3, 4
    args = (Object[]) objectManager.lookupObjectForCreateIfNecessaryContexts.take();
    assertNotNull(args);

    // Apply and commit complete for the first transaction
    txObjectManager.applyTransactionComplete(stxn1.getServerTransactionID());
    ApplyCompleteEventContext acec = (ApplyCompleteEventContext) coordinator.applyCompleteSink.queue.remove(0);
    assertNotNull(acec);
    assertTrue(coordinator.applyCompleteSink.queue.isEmpty());

    txObjectManager.processApplyComplete();
    CommitTransactionContext ctc = (CommitTransactionContext) coordinator.commitSink.queue.remove(0);
    assertNotNull(ctc);
    assertTrue(coordinator.commitSink.queue.isEmpty());

    txObjectManager.commitTransactionsComplete(ctc);
    Collection applied = ctc.getAppliedServerTransactionIDs();
    assertTrue(applied.size() == 1);
    assertEquals(stxn1.getServerTransactionID(), applied.iterator().next());
    Collection objects = ctc.getObjects();
    assertTrue(objects.size() == 2);

    Set recd = new HashSet();
    TestManagedObject tmo;
    for (Iterator i = objects.iterator(); i.hasNext();) {
      tmo = (TestManagedObject) i.next();
      recd.add(tmo.getID());
    }

    Set expected = new HashSet();
    expected.add(new ObjectID(1));
    expected.add(new ObjectID(2));

    assertEquals(expected, recd);

    // Process the request for 3, 4
    objectManager.makePending = false;
    objectManager.processPending(args);

    // Lookup context should have been fired
    loc = (LookupEventContext) coordinator.lookupSink.queue.remove(0);
    assertNotNull(loc);
    assertTrue(coordinator.lookupSink.queue.isEmpty());

    // Dont give out 1,2
    objectManager.makePending = true;
    txObjectManager.lookupObjectsForTransactions();

    // for txn2, ObjectID 1, 2
    args = (Object[]) objectManager.lookupObjectForCreateIfNecessaryContexts.take();
    assertNotNull(args);

    // Apply should not have been called as we dont have Obejct 1, 2
    assertTrue(coordinator.applySink.queue.isEmpty());

    // now do a recall.
    txObjectManager.recallAllCheckedoutObject();
    RecallObjectsContext roc = (RecallObjectsContext) coordinator.recallSink.queue.remove(0);
    assertNotNull(roc);
    assertTrue(coordinator.recallSink.queue.isEmpty());

    // process recall
    txObjectManager.recallCheckedoutObject(roc);

    // check 2 and only 2 object are released
    Collection released = (Collection) objectManager.releaseAllQueue.take();
    assertNotNull(released);

    System.err.println("Released = " + released);

    assertEquals(2, released.size());

    HashSet ids_expected = new HashSet();
    ids_expected.add(new ObjectID(3));
    ids_expected.add(new ObjectID(4));

    HashSet ids_recd = new HashSet();

    for (Iterator i = released.iterator(); i.hasNext();) {
      tmo = (TestManagedObject) i.next();
      ids_recd.add(tmo.getID());
    }

    assertEquals(ids_expected, ids_recd);
  }

  // This test is added to reproduce a failure. More test are needed for TransactionalObjectManager
  // This test is only slightly different from the above on in that the final recall should happen for 3 objects
  public void testTxnObjectManagerRecallAllOnGCCase2() throws Exception {

    Map changes = new HashMap();

    changes.put(new ObjectID(1), new TestDNA(new ObjectID(1)));
    changes.put(new ObjectID(2), new TestDNA(new ObjectID(2)));

    ServerTransaction stxn1 = new ServerTransactionImpl(gtxMgr, new TxnBatchID(1), new TransactionID(1),
                                                        new SequenceID(1), new LockID[0], new ChannelID(2),
                                                        new ArrayList(changes.values()), new ObjectStringSerializer(),
                                                        Collections.EMPTY_MAP, TxnType.NORMAL, new LinkedList(),
                                                        DmiDescriptor.EMPTY_ARRAY);
    List txns = new ArrayList();
    txns.add(stxn1);

    txObjectManager.addTransactions(new ChannelID(2), txns, Collections.EMPTY_LIST);

    // Lookup context should have been fired
    LookupEventContext loc = (LookupEventContext) coordinator.lookupSink.queue.remove(0);
    assertNotNull(loc);
    assertTrue(coordinator.lookupSink.queue.isEmpty());

    txObjectManager.lookupObjectsForTransactions();

    // for txn1 - ObjectID 1, 2
    Object args[] = (Object[]) objectManager.lookupObjectForCreateIfNecessaryContexts.take();
    assertNotNull(args);

    // Apply should have been called as we have Obejct 1, 2
    ApplyTransactionContext aoc = (ApplyTransactionContext) coordinator.applySink.queue.remove(0);
    assertTrue(stxn1 == aoc.getTxn());
    assertNotNull(aoc);
    assertTrue(coordinator.applySink.queue.isEmpty());

    // Next txn
    changes.put(new ObjectID(3), new TestDNA(new ObjectID(3)));
    changes.put(new ObjectID(4), new TestDNA(new ObjectID(4)));

    ServerTransaction stxn2 = new ServerTransactionImpl(gtxMgr, new TxnBatchID(2), new TransactionID(2),
                                                        new SequenceID(1), new LockID[0], new ChannelID(2),
                                                        new ArrayList(changes.values()), new ObjectStringSerializer(),
                                                        Collections.EMPTY_MAP, TxnType.NORMAL, new LinkedList(),
                                                        DmiDescriptor.EMPTY_ARRAY);

    txns.clear();
    txns.add(stxn2);

    txObjectManager.addTransactions(new ChannelID(2), txns, Collections.EMPTY_LIST);

    // Lookup context should have been fired
    loc = (LookupEventContext) coordinator.lookupSink.queue.remove(0);
    assertNotNull(loc);
    assertTrue(coordinator.lookupSink.queue.isEmpty());

    objectManager.makePending = true;
    txObjectManager.lookupObjectsForTransactions();

    // for txn2, ObjectID 3, 4
    args = (Object[]) objectManager.lookupObjectForCreateIfNecessaryContexts.take();
    assertNotNull(args);

    // Apply and commit complete for the first transaction
    txObjectManager.applyTransactionComplete(stxn1.getServerTransactionID());
    ApplyCompleteEventContext acec = (ApplyCompleteEventContext) coordinator.applyCompleteSink.queue.remove(0);
    assertNotNull(acec);
    assertTrue(coordinator.applyCompleteSink.queue.isEmpty());

    txObjectManager.processApplyComplete();
    CommitTransactionContext ctc = (CommitTransactionContext) coordinator.commitSink.queue.remove(0);
    assertNotNull(ctc);
    assertTrue(coordinator.commitSink.queue.isEmpty());

    txObjectManager.commitTransactionsComplete(ctc);
    Collection applied = ctc.getAppliedServerTransactionIDs();
    assertTrue(applied.size() == 1);
    assertEquals(stxn1.getServerTransactionID(), applied.iterator().next());
    Collection objects = ctc.getObjects();
    assertTrue(objects.size() == 2);

    Set recd = new HashSet();
    TestManagedObject tmo;
    for (Iterator i = objects.iterator(); i.hasNext();) {
      tmo = (TestManagedObject) i.next();
      recd.add(tmo.getID());
    }

    Set expected = new HashSet();
    expected.add(new ObjectID(1));
    expected.add(new ObjectID(2));

    assertEquals(expected, recd);

    // Process the request for 3, 4
    objectManager.makePending = false;
    objectManager.processPending(args);

    // Lookup context should have been fired
    loc = (LookupEventContext) coordinator.lookupSink.queue.remove(0);
    assertNotNull(loc);
    assertTrue(coordinator.lookupSink.queue.isEmpty());

    // Dont give out 1,2
    objectManager.makePending = true;
    txObjectManager.lookupObjectsForTransactions();

    // for txn2, ObjectID 1, 2
    args = (Object[]) objectManager.lookupObjectForCreateIfNecessaryContexts.take();
    assertNotNull(args);

    // Apply should not have been called as we dont have Obejct 1, 2
    assertTrue(coordinator.applySink.queue.isEmpty());

    // --------------------------------This is where its differernt from case 1-------------------------
    // Process the request for 5 and 6
    changes.clear();
    changes.put(new ObjectID(5), new TestDNA(new ObjectID(5)));
    ServerTransaction stxn3 = new ServerTransactionImpl(gtxMgr, new TxnBatchID(3), new TransactionID(3),
                                                        new SequenceID(2), new LockID[0], new ChannelID(2),
                                                        new ArrayList(changes.values()), new ObjectStringSerializer(),
                                                        Collections.EMPTY_MAP, TxnType.NORMAL, new LinkedList(),
                                                        DmiDescriptor.EMPTY_ARRAY);

    txns.clear();
    txns.add(stxn3);
    txObjectManager.addTransactions(new ChannelID(2), txns, Collections.EMPTY_LIST);

    // Lookup context should have been fired
    loc = (LookupEventContext) coordinator.lookupSink.queue.remove(0);
    assertNotNull(loc);
    assertTrue(coordinator.lookupSink.queue.isEmpty());

    objectManager.makePending = false;
    txObjectManager.lookupObjectsForTransactions();

    // for txn3 - ObjectID 5
    args = (Object[]) objectManager.lookupObjectForCreateIfNecessaryContexts.take();
    assertNotNull(args);

    // Apply should have been called as we have Object 5
    aoc = (ApplyTransactionContext) coordinator.applySink.queue.remove(0);
    assertTrue(stxn3 == aoc.getTxn());
    assertNotNull(aoc);
    assertTrue(coordinator.applySink.queue.isEmpty());

    // Next Txn , Object 5, 6
    changes.put(new ObjectID(6), new TestDNA(new ObjectID(6)));
    ServerTransaction stxn4 = new ServerTransactionImpl(gtxMgr, new TxnBatchID(4), new TransactionID(4),
                                                        new SequenceID(3), new LockID[0], new ChannelID(2),
                                                        new ArrayList(changes.values()), new ObjectStringSerializer(),
                                                        Collections.EMPTY_MAP, TxnType.NORMAL, new LinkedList(),
                                                        DmiDescriptor.EMPTY_ARRAY);

    txns.clear();
    txns.add(stxn4);
    txObjectManager.addTransactions(new ChannelID(2), txns, Collections.EMPTY_LIST);

    // Lookup context should have been fired
    loc = (LookupEventContext) coordinator.lookupSink.queue.remove(0);
    assertNotNull(loc);
    assertTrue(coordinator.lookupSink.queue.isEmpty());

    objectManager.makePending = true;
    txObjectManager.lookupObjectsForTransactions();

    // for txn4, ObjectID 6
    args = (Object[]) objectManager.lookupObjectForCreateIfNecessaryContexts.take();
    assertNotNull(args);

    // Apply and commit complete for the 3'rd transaction
    txObjectManager.applyTransactionComplete(stxn3.getServerTransactionID());
    acec = (ApplyCompleteEventContext) coordinator.applyCompleteSink.queue.remove(0);
    assertNotNull(acec);
    assertTrue(coordinator.applyCompleteSink.queue.isEmpty());

    txObjectManager.processApplyComplete();
    ctc = (CommitTransactionContext) coordinator.commitSink.queue.remove(0);
    assertNotNull(ctc);
    assertTrue(coordinator.commitSink.queue.isEmpty());

    txObjectManager.commitTransactionsComplete(ctc);
    applied = ctc.getAppliedServerTransactionIDs();
    assertTrue(applied.size() == 1);
    assertEquals(stxn3.getServerTransactionID(), applied.iterator().next());
    objects = ctc.getObjects();
    assertTrue(objects.size() == 1);

    recd = new HashSet();
    for (Iterator i = objects.iterator(); i.hasNext();) {
      tmo = (TestManagedObject) i.next();
      recd.add(tmo.getID());
    }

    expected = new HashSet();
    expected.add(new ObjectID(5));

    assertEquals(expected, recd);

    // Process the request for 6, still 1 and 2 is not given out
    objectManager.makePending = false;
    objectManager.processPending(args);

    // Lookup context should have been fired
    loc = (LookupEventContext) coordinator.lookupSink.queue.remove(0);
    assertNotNull(loc);
    assertTrue(coordinator.lookupSink.queue.isEmpty());

    // Now before look up thread got a chance to execute, recall gets executed.
    objectManager.makePending = true; // Since we dont want to give out 5 when we do a recall commit
    // -------------------------------------------------------------------------------------------------

    // now do a recall.
    txObjectManager.recallAllCheckedoutObject();
    RecallObjectsContext roc = (RecallObjectsContext) coordinator.recallSink.queue.remove(0);
    assertNotNull(roc);
    assertTrue(coordinator.recallSink.queue.isEmpty());

    // process recall
    txObjectManager.recallCheckedoutObject(roc);

    // check that all 3 objects (3,4,6) are released -- different from case 1
    Collection released = (Collection) objectManager.releaseAllQueue.take();
    assertNotNull(released);

    System.err.println("Released = " + released);

    assertEquals(3, released.size());

    HashSet ids_expected = new HashSet();
    ids_expected.add(new ObjectID(3));
    ids_expected.add(new ObjectID(4));
    ids_expected.add(new ObjectID(6));

    HashSet ids_recd = new HashSet();

    for (Iterator i = released.iterator(); i.hasNext();) {
      tmo = (TestManagedObject) i.next();
      ids_recd.add(tmo.getID());
    }

    assertEquals(ids_expected, ids_recd);
  }

}
