/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;
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
  
  //This test is added to repeoduce a failure. More test are needed for TransactionalObjectManager
  public void testTxnObjectManagerRecallAllOnGC() throws Exception {

    Map changes = new HashMap();

    changes.put( new ObjectID(1),new TestDNA(new ObjectID(1)));
    changes.put( new ObjectID(2),new TestDNA(new ObjectID(2)));

    ServerTransaction stxn1 = new ServerTransactionImpl(new TxnBatchID(1), new TransactionID(1),
                                                                    new SequenceID(1), new LockID[0], new ChannelID(2),
                                                                    new ArrayList(changes.values()), new ObjectStringSerializer(),
                                                                    Collections.EMPTY_MAP, TxnType.NORMAL,
                                                                    new LinkedList());
    List txns = new ArrayList();
    txns.add(stxn1);
    
    
    txObjectManager.addTransactions(new ChannelID(2), txns, Collections.EMPTY_LIST);
    
    //Lookup context should have been fired
    LookupEventContext loc = (LookupEventContext) coordinator.lookupSink.queue.remove(0);
    assertNotNull(loc);
    assertTrue(coordinator.lookupSink.queue.isEmpty());
    
    txObjectManager.lookupObjectsForTransactions();
    
    // for txn1 - ObjectID 1, 2/home/ssubbiah/2.2.1/code/base/tcbuild check_prep dso-l2 unit
    Object args[] = (Object []) objectManager.lookupObjectForCreateIfNecessaryContexts.take();
    assertNotNull(args);
    
    //Apply should not have been called as we dont have Obejct 1, 2
    ApplyTransactionContext aoc = (ApplyTransactionContext) coordinator.applySink.queue.remove(0);
    assertTrue(stxn1 == aoc.getTxn());
    assertNotNull(aoc);
    assertTrue(coordinator.applySink.queue.isEmpty());
    
    // Next txn
    changes.put( new ObjectID(3),new TestDNA(new ObjectID(3)));
    changes.put( new ObjectID(4),new TestDNA(new ObjectID(4)));

    ServerTransaction stxn2 = new ServerTransactionImpl(new TxnBatchID(2), new TransactionID(2),
                                                                    new SequenceID(1), new LockID[0], new ChannelID(2),
                                                                    new ArrayList(changes.values()), new ObjectStringSerializer(),
                                                                    Collections.EMPTY_MAP, TxnType.NORMAL,
                                                                    new LinkedList());
    
    txns.clear();
    txns.add(stxn2);
    
    txObjectManager.addTransactions(new ChannelID(2), txns, Collections.EMPTY_LIST);
    
    //Lookup context should have been fired
    loc = (LookupEventContext) coordinator.lookupSink.queue.remove(0);
    assertNotNull(loc);
    assertTrue(coordinator.lookupSink.queue.isEmpty());
    
    objectManager.makePending = true;
    txObjectManager.lookupObjectsForTransactions();
    
    // for txn2, ObjectID 3, 4
    args = (Object []) objectManager.lookupObjectForCreateIfNecessaryContexts.take();
    assertNotNull(args);
   
    //Apply and commit complete for the first transaction
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
    assertTrue(applied.size()==1);
    assertEquals(stxn1.getServerTransactionID(), applied.iterator().next());
    Collection objects = ctc.getObjects();
    assertTrue(objects.size()==2);
    
    Set recd = new HashSet();
    Iterator j = objects.iterator();
    TestManagedObject tmo = (TestManagedObject) j.next();
    recd.add(tmo.getID());
    tmo = (TestManagedObject) j.next();
    recd.add(tmo.getID());
    assertFalse(j.hasNext());
    
    Set expected = new HashSet();
    expected.add(new ObjectID(1));
    expected.add(new ObjectID(2));
    
    assertEquals(expected, recd);
    
    
    // Process the request for 3, 4
    objectManager.makePending = false;
    objectManager.processPending(args);

    //Lookup context should have been fired
    loc = (LookupEventContext) coordinator.lookupSink.queue.remove(0);
    assertNotNull(loc);
    assertTrue(coordinator.lookupSink.queue.isEmpty());
    
    // Dont give out 1,2
    objectManager.makePending = true;
    txObjectManager.lookupObjectsForTransactions();
    
    // for txn2, ObjectID 1, 2
    args = (Object []) objectManager.lookupObjectForCreateIfNecessaryContexts.take();
    assertNotNull(args);
    
    //Apply should not have been called as we dont have Obejct 1, 2
    assertTrue(coordinator.applySink.queue.isEmpty());
    
    // now do a recall.
    txObjectManager.recallAllCheckedoutObject();
    RecallObjectsContext roc = (RecallObjectsContext) coordinator.recallSink.queue.remove(0);
    assertNotNull(roc);
    assertTrue(coordinator.recallSink.queue.isEmpty());
    
    //process recall
    txObjectManager.recallCheckedoutObject(roc);
    
    // check 2 and one 2 object are released
    Collection released = (Collection) objectManager.releaseAllQueue.take(); 
    assertNotNull(released);
    
    System.err.println("Released = " + released);
    
    assertEquals(2, released.size());
    
    HashSet ids_expected = new HashSet();
    ids_expected.add(new ObjectID(3));
    ids_expected.add(new ObjectID(4));
    
    HashSet ids_recd = new HashSet();
    
    Iterator i = released.iterator();
    tmo = (TestManagedObject) i.next();
    ids_recd.add(tmo.getID());
    tmo = (TestManagedObject) i.next();
    ids_recd.add(tmo.getID());
    assertFalse(i.hasNext());
    
    assertEquals(ids_expected, ids_recd);
  }

}
