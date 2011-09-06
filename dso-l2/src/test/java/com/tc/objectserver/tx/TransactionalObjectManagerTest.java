/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.MetaDataReader;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.locks.LockID;
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
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
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
  ServerTransactionSequencer             sequencer;
  TestTransactionalStageCoordinator      coordinator;
  private TransactionalObjectManagerImpl txObjectManager;
  private TestGlobalTransactionManager   gtxMgr;

  @Override
  public void setUp() {
    this.objectManager = new TestObjectManager();
    this.sequencer = new ServerTransactionSequencerImpl();
    this.coordinator = new TestTransactionalStageCoordinator();
    this.gtxMgr = new TestGlobalTransactionManager();
    this.txObjectManager = new TransactionalObjectManagerImpl(this.objectManager, this.sequencer, this.gtxMgr,
                                                              this.coordinator);
  }

  // This test is added to reproduce a failure. More test are needed for TransactionalObjectManager
  public void testTxnObjectManagerRecallAllOnGC() throws Exception {

    Map changes = new HashMap();

    changes.put(new ObjectID(1), new TestDNA(new ObjectID(1)));
    changes.put(new ObjectID(2), new TestDNA(new ObjectID(2)));

    ServerTransaction stxn1 = new ServerTransactionImpl(new TxnBatchID(1), new TransactionID(1), new SequenceID(1),
                                                        new LockID[0], new ClientID(2),
                                                        new ArrayList(changes.values()),
                                                        new ObjectStringSerializerImpl(), Collections.EMPTY_MAP,
                                                        TxnType.NORMAL, new LinkedList(), DmiDescriptor.EMPTY_ARRAY,
                                                        new MetaDataReader[0], 1, new long[0]);
    List txns = new ArrayList();
    txns.add(stxn1);

    this.txObjectManager.addTransactions(txns);

    // Lookup context should have been fired
    LookupEventContext loc = (LookupEventContext) this.coordinator.lookupSink.queue.take();
    assertNotNull(loc);
    assertTrue(this.coordinator.lookupSink.queue.isEmpty());

    this.txObjectManager.lookupObjectsForTransactions();

    // for txn1 - ObjectID 1, 2
    Object args[] = (Object[]) this.objectManager.lookupObjectForCreateIfNecessaryContexts.take();
    assertNotNull(args);

    // Apply should have been called as we have Object 1, 2
    ApplyTransactionContext aoc = (ApplyTransactionContext) this.coordinator.applySink.queue.take();
    assertTrue(stxn1 == aoc.getTxn());
    assertNotNull(aoc);
    assertTrue(this.coordinator.applySink.queue.isEmpty());

    // Next txn
    changes.put(new ObjectID(3), new TestDNA(new ObjectID(3)));
    changes.put(new ObjectID(4), new TestDNA(new ObjectID(4)));

    ServerTransaction stxn2 = new ServerTransactionImpl(new TxnBatchID(2), new TransactionID(2), new SequenceID(1),
                                                        new LockID[0], new ClientID(2),
                                                        new ArrayList(changes.values()),
                                                        new ObjectStringSerializerImpl(), Collections.EMPTY_MAP,
                                                        TxnType.NORMAL, new LinkedList(), DmiDescriptor.EMPTY_ARRAY,
                                                        new MetaDataReader[0], 1, new long[0]);

    txns.clear();
    txns.add(stxn2);

    this.txObjectManager.addTransactions(txns);

    // Lookup context should have been fired
    loc = (LookupEventContext) this.coordinator.lookupSink.queue.take();
    assertNotNull(loc);
    assertTrue(this.coordinator.lookupSink.queue.isEmpty());

    this.objectManager.makePending = true;
    this.txObjectManager.lookupObjectsForTransactions();

    // for txn2, ObjectID 3, 4
    args = (Object[]) this.objectManager.lookupObjectForCreateIfNecessaryContexts.take();
    assertNotNull(args);

    // Apply and commit complete for the first transaction
    this.txObjectManager.applyTransactionComplete(new ApplyTransactionInfo(stxn1.isActiveTxn(), stxn1
        .getServerTransactionID()));
    ApplyCompleteEventContext acec = (ApplyCompleteEventContext) this.coordinator.applyCompleteSink.queue.take();
    assertNotNull(acec);
    assertTrue(this.coordinator.applyCompleteSink.queue.isEmpty());

    this.txObjectManager.processApplyComplete();
    CommitTransactionContext ctc = (CommitTransactionContext) this.coordinator.commitSink.queue.take();
    assertNotNull(ctc);
    assertTrue(this.coordinator.commitSink.queue.isEmpty());

    this.txObjectManager.commitTransactionsComplete(ctc);
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
    this.objectManager.makePending = false;
    this.objectManager.processPending(args);

    // Lookup context should have been fired
    loc = (LookupEventContext) this.coordinator.lookupSink.queue.take();
    assertNotNull(loc);
    assertTrue(this.coordinator.lookupSink.queue.isEmpty());

    // Dont give out 1,2
    this.objectManager.makePending = true;
    this.txObjectManager.lookupObjectsForTransactions();

    // for txn2, ObjectID 1, 2
    args = (Object[]) this.objectManager.lookupObjectForCreateIfNecessaryContexts.take();
    assertNotNull(args);

    // Apply should not have been called as we dont have Obejct 1, 2
    assertTrue(this.coordinator.applySink.queue.isEmpty());

    // now do a recall.
    this.txObjectManager.recallAllCheckedoutObject();
    RecallObjectsContext roc = (RecallObjectsContext) this.coordinator.recallSink.queue.take();
    assertNotNull(roc);
    assertTrue(this.coordinator.recallSink.queue.isEmpty());

    // process recall
    this.txObjectManager.recallCheckedoutObject(roc);

    // check 2 and only 2 object are released
    Collection released = (Collection) this.objectManager.releaseAllQueue.take();
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

    ServerTransaction stxn1 = new ServerTransactionImpl(new TxnBatchID(1), new TransactionID(1), new SequenceID(1),
                                                        new LockID[0], new ClientID(2),
                                                        new ArrayList(changes.values()),
                                                        new ObjectStringSerializerImpl(), Collections.EMPTY_MAP,
                                                        TxnType.NORMAL, new LinkedList(), DmiDescriptor.EMPTY_ARRAY,
                                                        new MetaDataReader[0], 1, new long[0]);

    List txns = new ArrayList();
    txns.add(stxn1);

    this.txObjectManager.addTransactions(txns);

    // Lookup context should have been fired
    LookupEventContext loc = (LookupEventContext) this.coordinator.lookupSink.queue.take();
    assertNotNull(loc);
    assertTrue(this.coordinator.lookupSink.queue.isEmpty());

    this.txObjectManager.lookupObjectsForTransactions();

    // for txn1 - ObjectID 1, 2
    Object args[] = (Object[]) this.objectManager.lookupObjectForCreateIfNecessaryContexts.take();
    assertNotNull(args);

    // Apply should have been called as we have Obejct 1, 2
    ApplyTransactionContext aoc = (ApplyTransactionContext) this.coordinator.applySink.queue.take();
    assertTrue(stxn1 == aoc.getTxn());
    assertNotNull(aoc);
    assertTrue(this.coordinator.applySink.queue.isEmpty());

    // Next txn
    changes.put(new ObjectID(3), new TestDNA(new ObjectID(3)));
    changes.put(new ObjectID(4), new TestDNA(new ObjectID(4)));

    ServerTransaction stxn2 = new ServerTransactionImpl(new TxnBatchID(2), new TransactionID(2), new SequenceID(1),
                                                        new LockID[0], new ClientID(2),
                                                        new ArrayList(changes.values()),
                                                        new ObjectStringSerializerImpl(), Collections.EMPTY_MAP,
                                                        TxnType.NORMAL, new LinkedList(), DmiDescriptor.EMPTY_ARRAY,
                                                        new MetaDataReader[0], 1, new long[0]);

    txns.clear();
    txns.add(stxn2);

    this.txObjectManager.addTransactions(txns);

    // Lookup context should have been fired
    loc = (LookupEventContext) this.coordinator.lookupSink.queue.take();
    assertNotNull(loc);
    assertTrue(this.coordinator.lookupSink.queue.isEmpty());

    this.objectManager.makePending = true;
    this.txObjectManager.lookupObjectsForTransactions();

    // for txn2, ObjectID 3, 4
    args = (Object[]) this.objectManager.lookupObjectForCreateIfNecessaryContexts.take();
    assertNotNull(args);

    // Apply and commit complete for the first transaction
    this.txObjectManager.applyTransactionComplete(new ApplyTransactionInfo(stxn1.isActiveTxn(), stxn1
        .getServerTransactionID()));
    ApplyCompleteEventContext acec = (ApplyCompleteEventContext) this.coordinator.applyCompleteSink.queue.take();
    assertNotNull(acec);
    assertTrue(this.coordinator.applyCompleteSink.queue.isEmpty());

    this.txObjectManager.processApplyComplete();
    CommitTransactionContext ctc = (CommitTransactionContext) this.coordinator.commitSink.queue.take();
    assertNotNull(ctc);
    assertTrue(this.coordinator.commitSink.queue.isEmpty());

    this.txObjectManager.commitTransactionsComplete(ctc);
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
    this.objectManager.makePending = false;
    this.objectManager.processPending(args);

    // Lookup context should have been fired
    loc = (LookupEventContext) this.coordinator.lookupSink.queue.take();
    assertNotNull(loc);
    assertTrue(this.coordinator.lookupSink.queue.isEmpty());

    // Dont give out 1,2
    this.objectManager.makePending = true;
    this.txObjectManager.lookupObjectsForTransactions();

    // for txn2, ObjectID 1, 2
    args = (Object[]) this.objectManager.lookupObjectForCreateIfNecessaryContexts.take();
    assertNotNull(args);

    // Apply should not have been called as we dont have Obejct 1, 2
    assertTrue(this.coordinator.applySink.queue.isEmpty());

    // --------------------------------This is where its differernt from case 1-------------------------
    // Process the request for 5 and 6
    changes.clear();
    changes.put(new ObjectID(5), new TestDNA(new ObjectID(5)));
    ServerTransaction stxn3 = new ServerTransactionImpl(new TxnBatchID(3), new TransactionID(3), new SequenceID(2),
                                                        new LockID[0], new ClientID(2),
                                                        new ArrayList(changes.values()),
                                                        new ObjectStringSerializerImpl(), Collections.EMPTY_MAP,
                                                        TxnType.NORMAL, new LinkedList(), DmiDescriptor.EMPTY_ARRAY,
                                                        new MetaDataReader[0], 1, new long[0]);

    txns.clear();
    txns.add(stxn3);
    this.txObjectManager.addTransactions(txns);

    // Lookup context should have been fired
    loc = (LookupEventContext) this.coordinator.lookupSink.queue.take();
    assertNotNull(loc);
    assertTrue(this.coordinator.lookupSink.queue.isEmpty());

    this.objectManager.makePending = false;
    this.txObjectManager.lookupObjectsForTransactions();

    // for txn3 - ObjectID 5
    args = (Object[]) this.objectManager.lookupObjectForCreateIfNecessaryContexts.take();
    assertNotNull(args);

    // Apply should have been called as we have Object 5
    aoc = (ApplyTransactionContext) this.coordinator.applySink.queue.take();
    assertTrue(stxn3 == aoc.getTxn());
    assertNotNull(aoc);
    assertTrue(this.coordinator.applySink.queue.isEmpty());

    // Next Txn , Object 5, 6
    changes.put(new ObjectID(6), new TestDNA(new ObjectID(6)));
    ServerTransaction stxn4 = new ServerTransactionImpl(new TxnBatchID(4), new TransactionID(4), new SequenceID(3),
                                                        new LockID[0], new ClientID(2),
                                                        new ArrayList(changes.values()),
                                                        new ObjectStringSerializerImpl(), Collections.EMPTY_MAP,
                                                        TxnType.NORMAL, new LinkedList(), DmiDescriptor.EMPTY_ARRAY,
                                                        new MetaDataReader[0], 1, new long[0]);

    txns.clear();
    txns.add(stxn4);
    this.txObjectManager.addTransactions(txns);

    // Lookup context should have been fired
    loc = (LookupEventContext) this.coordinator.lookupSink.queue.take();
    assertNotNull(loc);
    assertTrue(this.coordinator.lookupSink.queue.isEmpty());

    this.objectManager.makePending = true;
    this.txObjectManager.lookupObjectsForTransactions();

    // for txn4, ObjectID 6
    args = (Object[]) this.objectManager.lookupObjectForCreateIfNecessaryContexts.take();
    assertNotNull(args);

    // Apply and commit complete for the 3'rd transaction
    this.txObjectManager.applyTransactionComplete(new ApplyTransactionInfo(stxn3.isActiveTxn(), stxn3
        .getServerTransactionID()));
    acec = (ApplyCompleteEventContext) this.coordinator.applyCompleteSink.queue.take();
    assertNotNull(acec);
    assertTrue(this.coordinator.applyCompleteSink.queue.isEmpty());

    this.txObjectManager.processApplyComplete();
    ctc = (CommitTransactionContext) this.coordinator.commitSink.queue.take();
    assertNotNull(ctc);
    assertTrue(this.coordinator.commitSink.queue.isEmpty());

    this.txObjectManager.commitTransactionsComplete(ctc);
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
    this.objectManager.makePending = false;
    this.objectManager.processPending(args);

    // Lookup context should have been fired
    loc = (LookupEventContext) this.coordinator.lookupSink.queue.take();
    assertNotNull(loc);
    assertTrue(this.coordinator.lookupSink.queue.isEmpty());

    // Now before look up thread got a chance to execute, recall gets executed.
    this.objectManager.makePending = true; // Since we dont want to give out 5 when we do a recall commit
    // -------------------------------------------------------------------------------------------------

    // now do a recall.
    this.txObjectManager.recallAllCheckedoutObject();
    RecallObjectsContext roc = (RecallObjectsContext) this.coordinator.recallSink.queue.take();
    assertNotNull(roc);
    assertTrue(this.coordinator.recallSink.queue.isEmpty());

    // process recall
    this.txObjectManager.recallCheckedoutObject(roc);

    // check that all 3 objects (3,4,6) are released -- different from case 1
    Collection released = (Collection) this.objectManager.releaseAllQueue.take();
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
