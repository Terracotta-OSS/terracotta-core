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
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionID;
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
import com.tc.objectserver.persistence.impl.TestTransactionStore;
import com.tc.stats.counter.Counter;
import com.tc.stats.counter.CounterImpl;
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

import junit.framework.TestCase;

public class ServerTransactionManagerImplTest extends TestCase {

  private ServerTransactionManagerImpl     transactionManager;
  private TestTransactionAcknowledgeAction action;
  private TestClientStateManager           clientStateManager;
  private TestLockManager                  lockManager;
  private TestObjectManager                objectManager;
  private TestTransactionStore             transactionStore;
  private Counter                          transactionRateCounter;
  private TestChannelStats                 channelStats;
  private TestGlobalTransactionManager     gtxm;
  private int                              idsequence;
  private ObjectInstanceMonitor            imo;

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
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  private void newTransactionManager() {
    this.transactionManager = new ServerTransactionManagerImpl(this.gtxm, this.transactionStore, this.lockManager,
                                                               this.clientStateManager, this.objectManager,
                                                               this.action, this.transactionRateCounter,
                                                               this.channelStats);
  }

  public void testRootCreatedEvent() {
    Map roots = new HashMap();
    roots.put("root", new ObjectID(1));

    // first test w/o any listeners attached
    this.transactionManager.release(null, Collections.EMPTY_SET, roots);

    // add a listener
    Listener listener = new Listener();
    this.transactionManager.addRootListener(listener);
    roots.clear();
    roots.put("root2", new ObjectID(2));

    this.transactionManager.release(null, Collections.EMPTY_SET, roots);
    assertEquals(1, listener.rootsCreated.size());
    Root root = (Root) listener.rootsCreated.remove(0);
    assertEquals("root2", root.name);
    assertEquals(new ObjectID(2), root.id);

    // add another listener
    Listener listener2 = new Listener();
    this.transactionManager.addRootListener(listener2);
    roots.clear();
    roots.put("root3", new ObjectID(3));

    this.transactionManager.release(null, Collections.EMPTY_SET, roots);
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
    this.transactionManager.release(null, Collections.EMPTY_SET, roots);
  }

  public void tests() throws Exception {

    ChannelID cid1 = new ChannelID(1);
    TransactionID tid1 = new TransactionID(1);
    TransactionID tid2 = new TransactionID(2);
    ChannelID cid2 = new ChannelID(2);
    ChannelID cid3 = new ChannelID(3);

    LockID[] lockIDs = new LockID[0];
    List dnas = Collections.unmodifiableList(new LinkedList());
    ObjectStringSerializer serializer = null;
    Map newRoots = Collections.unmodifiableMap(new HashMap());
    TxnType txnType = TxnType.NORMAL;
    SequenceID sequenceID = new SequenceID(1);
    ServerTransaction tx1 = new ServerTransactionImpl(new TxnBatchID(1), tid1, sequenceID, lockIDs, cid1, dnas,
                                                      serializer, newRoots, txnType, new LinkedList());

    // Test with one waiter
    transactionManager.addWaitingForAcknowledgement(cid1, tid1, cid2);
    assertTrue(transactionManager.isWaiting(cid1, tid1));
    assertTrue(action.clientID == null && action.txID == null);
    transactionManager.acknowledgement(cid1, tid1, cid2);
    assertTrue(action.clientID == null && action.txID == null);
    Set txns = new HashSet();
    txns.add(tx1);
    doStages(txns);
    assertTrue(action.clientID == cid1 && action.txID == tid1);
    assertFalse(transactionManager.isWaiting(cid1, tid1));

    // Test with 2 waiters
    action.clear();
    gtxm.clear();
    transactionManager.addWaitingForAcknowledgement(cid1, tid1, cid2);
    transactionManager.addWaitingForAcknowledgement(cid1, tid1, cid3);
    assertTrue(action.clientID == null && action.txID == null);
    assertTrue(transactionManager.isWaiting(cid1, tid1));
    transactionManager.acknowledgement(cid1, tid1, cid2);
    assertTrue(action.clientID == null && action.txID == null);
    assertTrue(transactionManager.isWaiting(cid1, tid1));
    transactionManager.acknowledgement(cid1, tid1, cid3);
    assertTrue(action.clientID == null && action.txID == null);
    doStages(txns);
    assertTrue(action.clientID == cid1 && action.txID == tid1);
    assertFalse(transactionManager.isWaiting(cid1, tid1));

    // Test shutdown client with 2 waiters
    action.clear();
    gtxm.clear();
    transactionManager.addWaitingForAcknowledgement(cid1, tid1, cid2);
    transactionManager.addWaitingForAcknowledgement(cid1, tid1, cid3);
    assertTrue(action.clientID == null && action.txID == null);
    assertTrue(transactionManager.isWaiting(cid1, tid1));
    transactionManager.shutdownClient(cid3);
    assertTrue(this.clientStateManager.shutdownClientCalled);
    assertTrue(transactionManager.isWaiting(cid1, tid1));
    transactionManager.acknowledgement(cid1, tid1, cid2);
    doStages(txns);
    assertTrue(action.clientID == cid1 && action.txID == tid1);
    assertFalse(transactionManager.isWaiting(cid1, tid1));

    // Test shutdown client that no one is waiting for
    action.clear();
    gtxm.clear();
    transactionManager.addWaitingForAcknowledgement(cid1, tid1, cid2);
    transactionManager.addWaitingForAcknowledgement(cid1, tid1, cid3);
    transactionManager.shutdownClient(cid1);
    assertTrue(action.clientID == null && action.txID == null);
    assertFalse(transactionManager.isWaiting(cid1, tid1));

    // Test with 2 waiters on different tx's
    action.clear();
    gtxm.clear();
    transactionManager.addWaitingForAcknowledgement(cid1, tid1, cid2);
    transactionManager.addWaitingForAcknowledgement(cid1, tid2, cid2);

    assertTrue(action.clientID == null && action.txID == null);
    assertTrue(transactionManager.isWaiting(cid1, tid1));
    assertTrue(transactionManager.isWaiting(cid1, tid2));

    transactionManager.acknowledgement(cid1, tid1, cid2);
    assertFalse(transactionManager.isWaiting(cid1, tid1));
    assertTrue(transactionManager.isWaiting(cid1, tid2));
    doStages(txns);
    assertTrue(action.clientID == cid1 && action.txID == tid1);

    action.clear();
    gtxm.clear();
    transactionManager.acknowledgement(cid1, tid2, cid2);
    ServerTransaction tx2 = new ServerTransactionImpl(new TxnBatchID(2), tid2, sequenceID, lockIDs, cid1, dnas,
                                                      serializer, newRoots, txnType, new LinkedList());
    txns.clear();
    txns.add(tx2);
    doStages(txns);
    assertTrue(action.clientID == cid1 && action.txID == tid2);
    assertFalse(transactionManager.isWaiting(cid1, tid1));
    assertFalse(transactionManager.isWaiting(cid1, tid2));
  }

  private void doStages(Set txns) {

    for (Iterator iter = txns.iterator(); iter.hasNext();) {
      ServerTransaction tx = (ServerTransaction) iter.next();

      // apply stage
      transactionManager.apply(new GlobalTransactionID(idsequence++), tx, Collections.EMPTY_MAP, new BackReferences(),
                               imo);
      assertTrue(action.clientID == null && action.txID == null);
      // release
      transactionManager.release(null, Collections.EMPTY_SET, Collections.EMPTY_MAP);
      assertTrue(action.clientID == null && action.txID == null);
      // commit stage
      gtxm.commitAll(null, getServerTransactionIDs(txns));
      ArrayList committedIDs = new ArrayList();
      committedIDs.add(tx.getServerTransactionID());
      transactionManager.committed(committedIDs);
      assertTrue(action.clientID == null && action.txID == null);

      // broadcast stage
      transactionManager.broadcasted(tx.getChannelID(), tx.getTransactionID());
    }
  }

  private Collection getServerTransactionIDs(Set txns) {
    Set s = new HashSet();
    for (Iterator iter = txns.iterator(); iter.hasNext();) {
      s.add(((ServerTransaction) iter.next()).getServerTransactionID());
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
