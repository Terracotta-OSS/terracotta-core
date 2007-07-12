/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.exception.ImplementMe;
import com.tc.exception.TCRuntimeException;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ObjectID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.net.ChannelStats;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.gtx.TestGlobalTransactionManager;
import com.tc.objectserver.impl.ObjectInstanceMonitorImpl;
import com.tc.objectserver.impl.TestObjectManager;
import com.tc.objectserver.l1.api.TestClientStateManager;
import com.tc.objectserver.l1.impl.TransactionAcknowledgeAction;
import com.tc.objectserver.lockmanager.api.TestLockManager;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.objectserver.persistence.impl.NullPersistenceTransactionProvider;
import com.tc.objectserver.persistence.impl.TestTransactionStore;
import com.tc.stats.counter.Counter;
import com.tc.stats.counter.CounterImpl;
import com.tc.util.SequenceID;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

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

import junit.framework.TestCase;

public class ServerTransactionManagerImplTest extends TestCase {

  private ServerTransactionManagerImpl       transactionManager;
  private TestTransactionAcknowledgeAction   action;
  private TestClientStateManager             clientStateManager;
  private TestLockManager                    lockManager;
  private TestObjectManager                  objectManager;
  private TestTransactionStore               transactionStore;
  private Counter                            transactionRateCounter;
  private TestChannelStats                   channelStats;
  private TestGlobalTransactionManager       gtxm;
  private ObjectInstanceMonitor              imo;
  private NullPersistenceTransactionProvider ptxp;

  protected void setUp() throws Exception {
    super.setUp();
    this.action = new TestTransactionAcknowledgeAction();
    this.clientStateManager = new TestClientStateManager();
    this.lockManager = new TestLockManager();
    this.objectManager = new TestObjectManager();
    this.transactionStore = new TestTransactionStore();
    this.transactionRateCounter = new CounterImpl();
    this.channelStats = new TestChannelStats();
    this.gtxm = new TestGlobalTransactionManager();
    this.imo = new ObjectInstanceMonitorImpl();
    newTransactionManager();
    this.ptxp = new NullPersistenceTransactionProvider();
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  private void newTransactionManager() {
    this.transactionManager = new ServerTransactionManagerImpl(this.gtxm, this.transactionStore, this.lockManager,
                                                               this.clientStateManager, this.objectManager,
                                                               new NullTransactionalObjectManager(), this.action,
                                                               this.transactionRateCounter, this.channelStats,
                                                               new ServerTransactionManagerConfig());
    this.transactionManager.goToActiveMode();
  }

  public void testRootCreatedEvent() {
    Map roots = new HashMap();
    roots.put("root", new ObjectID(1));

    // first test w/o any listeners attached
    this.transactionManager.commit(ptxp, Collections.EMPTY_SET, roots, Collections.EMPTY_LIST, Collections.EMPTY_SET);

    // add a listener
    Listener listener = new Listener();
    this.transactionManager.addRootListener(listener);
    roots.clear();
    roots.put("root2", new ObjectID(2));

    this.transactionManager.commit(ptxp, Collections.EMPTY_SET, roots, Collections.EMPTY_LIST, Collections.EMPTY_SET);
    assertEquals(1, listener.rootsCreated.size());
    Root root = (Root) listener.rootsCreated.remove(0);
    assertEquals("root2", root.name);
    assertEquals(new ObjectID(2), root.id);

    // add another listener
    Listener listener2 = new Listener();
    this.transactionManager.addRootListener(listener2);
    roots.clear();
    roots.put("root3", new ObjectID(3));

    this.transactionManager.commit(ptxp, Collections.EMPTY_SET, roots, Collections.EMPTY_LIST, Collections.EMPTY_SET);
    assertEquals(1, listener.rootsCreated.size());
    root = (Root) listener.rootsCreated.remove(0);
    assertEquals("root3", root.name);
    assertEquals(new ObjectID(3), root.id);
    root = (Root) listener2.rootsCreated.remove(0);
    assertEquals("root3", root.name);
    assertEquals(new ObjectID(3), root.id);

    // add a listener that throws an exception
    this.transactionManager.addRootListener(new ServerTransactionManagerEventListener() {
      public void rootCreated(String name, ObjectID id) {
        throw new RuntimeException("This exception is supposed to be here");
      }
    });
    this.transactionManager.commit(ptxp, Collections.EMPTY_SET, roots, Collections.EMPTY_LIST, Collections.EMPTY_SET);
  }

  public void testAddAndRemoveTransactionListeners() throws Exception {
    TestServerTransactionListener l1 = new TestServerTransactionListener();
    TestServerTransactionListener l2 = new TestServerTransactionListener();
    transactionManager.addTransactionListener(l1);
    transactionManager.addTransactionListener(l2);

    Set txns = new HashSet();
    ChannelID cid1 = new ChannelID(1);
    List dnas = Collections.unmodifiableList(new LinkedList());
    ObjectStringSerializer serializer = null;
    Map newRoots = Collections.unmodifiableMap(new HashMap());
    TxnType txnType = TxnType.NORMAL;

    HashSet tids = new HashSet();
    for (int i = 0; i < 10; i++) {
      TransactionID tid1 = new TransactionID(i);
      SequenceID sequenceID = new SequenceID(i);
      LockID[] lockIDs = new LockID[0];
      ServerTransaction tx = new ServerTransactionImpl(gtxm, new TxnBatchID(1), tid1, sequenceID, lockIDs, cid1, dnas,
                                                       serializer, newRoots, txnType, new LinkedList(),
                                                       DmiDescriptor.EMPTY_ARRAY);
      txns.add(tx);
      tids.add(tx.getServerTransactionID());
    }
    doStages(cid1, txns, false);

    // check for events
    Object o[] = (Object[]) l1.incomingContext.take();
    assertNotNull(o);
    o = (Object[]) l2.incomingContext.take();
    assertNotNull(o);

    for (int i = 0; i < 10; i++) {
      ServerTransactionID tid1 = (ServerTransactionID) l1.appliedContext.take();
      ServerTransactionID tid2 = (ServerTransactionID) l2.appliedContext.take();
      assertEquals(tid1, tid2);
      // System.err.println("tid1 = " + tid1 + " tid2 = " + tid2 + " tids = " + tids);
      assertTrue(tids.contains(tid1));
      tid1 = (ServerTransactionID) l1.completedContext.take();
      tid2 = (ServerTransactionID) l2.completedContext.take();
      assertEquals(tid1, tid2);
      assertTrue(tids.contains(tid1));
    }

    // No more events
    o = (Object[]) l1.incomingContext.poll(2000);
    assertNull(o);
    o = (Object[]) l2.incomingContext.poll(2000);
    assertNull(o);
    ServerTransactionID tid = (ServerTransactionID) l1.appliedContext.poll(2000);
    assertNull(tid);
    tid = (ServerTransactionID) l2.appliedContext.poll(2000);
    assertNull(tid);
    tid = (ServerTransactionID) l1.completedContext.poll(2000);
    assertNull(tid);
    tid = (ServerTransactionID) l2.completedContext.poll(2000);
    assertNull(tid);

    // unregister one
    transactionManager.removeTransactionListener(l2);

    // more txn
    tids.clear();
    txns.clear();
    for (int i = 10; i < 20; i++) {
      TransactionID tid1 = new TransactionID(i);
      SequenceID sequenceID = new SequenceID(i);
      LockID[] lockIDs = new LockID[0];
      ServerTransaction tx = new ServerTransactionImpl(gtxm, new TxnBatchID(2), tid1, sequenceID, lockIDs, cid1, dnas,
                                                       serializer, newRoots, txnType, new LinkedList(),
                                                       DmiDescriptor.EMPTY_ARRAY);
      txns.add(tx);
      tids.add(tx.getServerTransactionID());
    }
    doStages(cid1, txns, false);

    // Events to only l1
    o = (Object[]) l1.incomingContext.take();
    assertNotNull(o);
    o = (Object[]) l2.incomingContext.poll(2000);
    assertNull(o);

    for (int i = 0; i < 10; i++) {
      ServerTransactionID tid1 = (ServerTransactionID) l1.appliedContext.take();
      ServerTransactionID tid2 = (ServerTransactionID) l2.appliedContext.poll(1000);
      assertNotNull(tid1);
      assertNull(tid2);
      assertTrue(tids.contains(tid1));
      tid1 = (ServerTransactionID) l1.completedContext.take();
      tid2 = (ServerTransactionID) l2.completedContext.poll(1000);
      assertNotNull(tid1);
      assertNull(tid2);
      assertTrue(tids.contains(tid1));
    }
  }

  public void tests() throws Exception {
    ChannelID cid1 = new ChannelID(1);
    TransactionID tid1 = new TransactionID(1);
    TransactionID tid2 = new TransactionID(2);
    TransactionID tid3 = new TransactionID(3);
    TransactionID tid4 = new TransactionID(4);
    TransactionID tid5 = new TransactionID(5);
    TransactionID tid6 = new TransactionID(6);

    ChannelID cid2 = new ChannelID(2);
    ChannelID cid3 = new ChannelID(3);

    LockID[] lockIDs = new LockID[0];
    List dnas = Collections.unmodifiableList(new LinkedList());
    ObjectStringSerializer serializer = null;
    Map newRoots = Collections.unmodifiableMap(new HashMap());
    TxnType txnType = TxnType.NORMAL;
    SequenceID sequenceID = new SequenceID(1);
    ServerTransaction tx1 = new ServerTransactionImpl(gtxm, new TxnBatchID(1), tid1, sequenceID, lockIDs, cid1, dnas,
                                                      serializer, newRoots, txnType, new LinkedList(),
                                                      DmiDescriptor.EMPTY_ARRAY);

    // Test with one waiter
    Set txns = new HashSet();
    txns.add(tx1);
    Set txnIDs = new HashSet();
    txnIDs.add(new ServerTransactionID(cid1, tid1));
    transactionManager.incomingTransactions(cid1, txnIDs, txns, false, Collections.EMPTY_LIST);
    transactionManager.addWaitingForAcknowledgement(cid1, tid1, cid2);
    assertTrue(transactionManager.isWaiting(cid1, tid1));
    assertTrue(action.clientID == null && action.txID == null);
    transactionManager.acknowledgement(cid1, tid1, cid2);
    assertTrue(action.clientID == null && action.txID == null);
    doStages(cid1, txns);
    assertTrue(action.clientID == cid1 && action.txID == tid1);
    assertFalse(transactionManager.isWaiting(cid1, tid1));

    // Test with 2 waiters
    action.clear();
    gtxm.clear();
    txns.clear();
    txnIDs.clear();
    sequenceID = new SequenceID(2);
    ServerTransaction tx2 = new ServerTransactionImpl(gtxm, new TxnBatchID(2), tid2, sequenceID, lockIDs, cid1, dnas,
                                                      serializer, newRoots, txnType, new LinkedList(),
                                                      DmiDescriptor.EMPTY_ARRAY);
    txns.add(tx2);
    txnIDs.add(new ServerTransactionID(cid1, tid2));
    transactionManager.incomingTransactions(cid1, txnIDs, txns, false, Collections.EMPTY_LIST);

    transactionManager.addWaitingForAcknowledgement(cid1, tid2, cid2);
    transactionManager.addWaitingForAcknowledgement(cid1, tid2, cid3);
    assertTrue(action.clientID == null && action.txID == null);
    assertTrue(transactionManager.isWaiting(cid1, tid2));
    transactionManager.acknowledgement(cid1, tid2, cid2);
    assertTrue(action.clientID == null && action.txID == null);
    assertTrue(transactionManager.isWaiting(cid1, tid2));
    transactionManager.acknowledgement(cid1, tid2, cid3);
    assertTrue(action.clientID == null && action.txID == null);
    doStages(cid1, txns);
    assertTrue(action.clientID == cid1 && action.txID == tid2);
    assertFalse(transactionManager.isWaiting(cid1, tid2));

    // Test shutdown client with 2 waiters
    action.clear();
    gtxm.clear();
    txns.clear();
    txnIDs.clear();
    sequenceID = new SequenceID(3);
    ServerTransaction tx3 = new ServerTransactionImpl(gtxm, new TxnBatchID(3), tid3, sequenceID, lockIDs, cid1, dnas,
                                                      serializer, newRoots, txnType, new LinkedList(),
                                                      DmiDescriptor.EMPTY_ARRAY);
    txns.add(tx3);
    txnIDs.add(new ServerTransactionID(cid1, tid3));
    transactionManager.incomingTransactions(cid1, txnIDs, txns, false, Collections.EMPTY_LIST);
    transactionManager.addWaitingForAcknowledgement(cid1, tid3, cid2);
    transactionManager.addWaitingForAcknowledgement(cid1, tid3, cid3);
    assertTrue(action.clientID == null && action.txID == null);
    assertTrue(transactionManager.isWaiting(cid1, tid3));
    transactionManager.shutdownClient(cid3);
    assertTrue(this.clientStateManager.shutdownClientCalled);
    assertTrue(transactionManager.isWaiting(cid1, tid3));
    transactionManager.acknowledgement(cid1, tid3, cid2);
    doStages(cid1, txns);
    assertTrue(action.clientID == cid1 && action.txID == tid3);
    assertFalse(transactionManager.isWaiting(cid1, tid3));

    // Test shutdown client that no one is waiting for
    action.clear();
    gtxm.clear();
    txns.clear();
    txnIDs.clear();

    sequenceID = new SequenceID(4);
    ServerTransaction tx4 = new ServerTransactionImpl(gtxm, new TxnBatchID(4), tid4, sequenceID, lockIDs, cid1, dnas,
                                                      serializer, newRoots, txnType, new LinkedList(),
                                                      DmiDescriptor.EMPTY_ARRAY);
    txns.add(tx4);
    txnIDs.add(new ServerTransactionID(cid1, tid4));
    transactionManager.incomingTransactions(cid1, txnIDs, txns, false, Collections.EMPTY_LIST);
    transactionManager.addWaitingForAcknowledgement(cid1, tid4, cid2);
    transactionManager.addWaitingForAcknowledgement(cid1, tid4, cid3);
    transactionManager.shutdownClient(cid1);
    assertTrue(action.clientID == null && action.txID == null);
    assertFalse(transactionManager.isWaiting(cid1, tid4));

    // Test with 2 waiters on different tx's
    action.clear();
    gtxm.clear();
    txns.clear();
    txnIDs.clear();
    sequenceID = new SequenceID(5);
    ServerTransaction tx5 = new ServerTransactionImpl(gtxm, new TxnBatchID(5), tid5, sequenceID, lockIDs, cid1, dnas,
                                                      serializer, newRoots, txnType, new LinkedList(),
                                                      DmiDescriptor.EMPTY_ARRAY);
    sequenceID = new SequenceID(6);
    ServerTransaction tx6 = new ServerTransactionImpl(gtxm, new TxnBatchID(5), tid6, sequenceID, lockIDs, cid1, dnas,
                                                      serializer, newRoots, txnType, new LinkedList(),
                                                      DmiDescriptor.EMPTY_ARRAY);
    txns.add(tx5);
    txns.add(tx6);
    txnIDs.add(new ServerTransactionID(cid1, tid5));
    txnIDs.add(new ServerTransactionID(cid1, tid6));

    transactionManager.incomingTransactions(cid1, txnIDs, txns, false, Collections.EMPTY_LIST);
    transactionManager.addWaitingForAcknowledgement(cid1, tid5, cid2);
    transactionManager.addWaitingForAcknowledgement(cid1, tid6, cid2);

    assertTrue(action.clientID == null && action.txID == null);
    assertTrue(transactionManager.isWaiting(cid1, tid5));
    assertTrue(transactionManager.isWaiting(cid1, tid6));

    transactionManager.acknowledgement(cid1, tid5, cid2);
    assertFalse(transactionManager.isWaiting(cid1, tid5));
    assertTrue(transactionManager.isWaiting(cid1, tid6));
    doStages(cid1, txns);
    assertTrue(action.clientID == cid1 && action.txID == tid5);

  }

  private void doStages(ChannelID cid, Set txns) {
    doStages(cid, txns, true);
  }

  private void doStages(ChannelID cid, Set txns, boolean skipIncoming) {

    // process stage
    if (!skipIncoming) transactionManager.incomingTransactions(cid, getServerTransactionIDs(txns), txns, false,
                                                               Collections.EMPTY_LIST);

    for (Iterator iter = txns.iterator(); iter.hasNext();) {
      ServerTransaction tx = (ServerTransaction) iter.next();

      // apply stage
      transactionManager.apply(tx, Collections.EMPTY_MAP, new BackReferences(), imo);

      // commit stage
      Set committedIDs = new HashSet();
      committedIDs.add(tx.getServerTransactionID());
      this.transactionManager.commit(ptxp, Collections.EMPTY_SET, Collections.EMPTY_MAP, committedIDs,
                                     Collections.EMPTY_SET);

      // broadcast stage
      transactionManager.broadcasted(tx.getChannelID(), tx.getTransactionID());
    }
  }

  private Set getServerTransactionIDs(Set txns) {
    Set s = new HashSet(txns.size());
    for (Iterator iter = txns.iterator(); iter.hasNext();) {
      ServerTransaction st = (ServerTransaction) iter.next();
      s.add(st.getServerTransactionID());
    }
    return s;
  }

  private static final class TestChannelStats implements ChannelStats {

    public LinkedQueue notifyTransactionContexts = new LinkedQueue();

    public Counter getCounter(MessageChannel channel, String name) {
      throw new ImplementMe();
    }

    public void notifyTransaction(ChannelID channelID) {
      try {
        notifyTransactionContexts.put(channelID);
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
    }

  }

  private static class Root {
    final String   name;
    final ObjectID id;

    Root(String name, ObjectID id) {
      this.name = name;
      this.id = id;
    }
  }

  private static class Listener implements ServerTransactionManagerEventListener {
    final List rootsCreated = new ArrayList();

    public void rootCreated(String name, ObjectID id) {
      rootsCreated.add(new Root(name, id));
    }
  }

  private static class TestServerTransactionListener implements ServerTransactionListener {

    NoExceptionLinkedQueue incomingContext  = new NoExceptionLinkedQueue();
    NoExceptionLinkedQueue appliedContext   = new NoExceptionLinkedQueue();
    NoExceptionLinkedQueue completedContext = new NoExceptionLinkedQueue();

    public void incomingTransactions(ChannelID cid, Set serverTxnIDs) {
      incomingContext.put(new Object[] { cid, serverTxnIDs });
    }

    public void transactionApplied(ServerTransactionID stxID) {
      appliedContext.put(stxID);
    }

    public void transactionCompleted(ServerTransactionID stxID) {
      completedContext.put(stxID);
    }

    public void addResentServerTransactionIDs(Collection stxIDs) {
      throw new ImplementMe();
    }

    public void clearAllTransactionsFor(ChannelID killedClient) {
      throw new ImplementMe();
    }

    public void transactionManagerStarted(Set cids) {
      throw new ImplementMe();
    }

  }

  public class TestTransactionAcknowledgeAction implements TransactionAcknowledgeAction {
    public ChannelID     clientID;
    public TransactionID txID;

    public void acknowledgeTransaction(ServerTransactionID stxID) {
      this.txID = stxID.getClientTransactionID();
      this.clientID = stxID.getChannelID();
    }

    public void clear() {
      txID = null;
      clientID = null;
    }

  }
}
