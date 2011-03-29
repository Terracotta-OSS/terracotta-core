/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.impl.MockSink;
import com.tc.async.impl.MockStage;
import com.tc.l2.ha.L2HADisabledCooridinator;
import com.tc.net.ClientID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.MetaDataReader;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.locks.LockID;
import com.tc.object.locks.Notify;
import com.tc.object.locks.NotifyImpl;
import com.tc.object.locks.StringLockID;
import com.tc.object.locks.TestLockManager;
import com.tc.object.locks.ThreadID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.context.ApplyTransactionContext;
import com.tc.objectserver.context.BroadcastChangeContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.TestServerConfigurationContext;
import com.tc.objectserver.gtx.TestGlobalTransactionManager;
import com.tc.objectserver.impl.ObjectInstanceMonitorImpl;
import com.tc.objectserver.locks.NotifiedWaiters;
import com.tc.objectserver.tx.NullTransactionalObjectManager;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionImpl;
import com.tc.objectserver.tx.TestServerTransactionManager;
import com.tc.util.SequenceID;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

public class ApplyTransactionChangeHandlerTest extends TestCase {

  private ApplyTransactionChangeHandler handler;
  private ObjectInstanceMonitor         instanceMonitor;
  private TestServerTransactionManager  transactionManager;
  private TestGlobalTransactionManager  gtxm;
  private TestLockManager               lockManager;
  private MockSink                      broadcastSink;

  @Override
  public void setUp() throws Exception {
    this.instanceMonitor = new ObjectInstanceMonitorImpl();
    this.transactionManager = new TestServerTransactionManager();
    this.lockManager = new TestLockManager();
    this.gtxm = new TestGlobalTransactionManager();
    this.handler = new ApplyTransactionChangeHandler(this.instanceMonitor, this.gtxm);

    MockStage stageBo = new MockStage("Bo");
    MockStage stageCo = new MockStage("Co");
    MockStage stageCpEv = new MockStage("CpEv");
    this.broadcastSink = stageBo.sink;
    TestServerConfigurationContext context = new TestServerConfigurationContext();
    context.transactionManager = this.transactionManager;
    context.txnObjectManager = new NullTransactionalObjectManager();
    context.l2Coordinator = new L2HADisabledCooridinator();
    context.addStage(ServerConfigurationContext.BROADCAST_CHANGES_STAGE, stageBo);
    context.addStage(ServerConfigurationContext.COMMIT_CHANGES_STAGE, stageCo);
    context.addStage(ServerConfigurationContext.SERVER_MAP_CAPACITY_EVICTION_STAGE, stageCpEv);
    context.lockManager = this.lockManager;

    this.handler.initializeContext(context);
  }

  public void testLockManagerNotifyIsCalled() throws Exception {
    TxnBatchID batchID = new TxnBatchID(1);
    TransactionID txID = new TransactionID(1);
    LockID[] lockIDs = new LockID[] { new StringLockID("1") };
    ClientID cid = new ClientID(1);
    List dnas = Collections.unmodifiableList(new LinkedList());
    ObjectStringSerializer serializer = null;
    Map newRoots = Collections.unmodifiableMap(new HashMap());
    TxnType txnType = TxnType.NORMAL;
    List notifies = new LinkedList();

    for (int i = 0; i < 10; i++) {
      notifies.add(new NotifyImpl(new StringLockID("" + i), new ThreadID(i), i % 2 == 0));
    }
    SequenceID sequenceID = new SequenceID(1);
    ServerTransaction tx = new ServerTransactionImpl(batchID, txID, sequenceID, lockIDs, cid, dnas, serializer,
                                                     newRoots, txnType, notifies, DmiDescriptor.EMPTY_ARRAY,
                                                     new MetaDataReader[0], 1, new long[0]);
    // call handleEvent with the global transaction reporting that it doesn't need an apply...
    assertTrue(this.lockManager.notifyCalls.isEmpty());
    assertTrue(this.broadcastSink.queue.isEmpty());
    this.handler.handleEvent(getApplyTxnContext(tx));
    // even if the transaction has already been applied, the notifies must be applied, since they aren't persistent.
    assertEquals(notifies.size(), this.lockManager.notifyCalls.size());
    this.lockManager.notifyCalls.clear();
    assertNotNull(this.broadcastSink.queue.take());

    // call handleEvent with the global transaction reporting that it DOES need an apply...
    this.handler.handleEvent(getApplyTxnContext(tx));

    assertEquals(notifies.size(), this.lockManager.notifyCalls.size());
    NotifiedWaiters notifiedWaiters = null;
    for (Iterator i = notifies.iterator(); i.hasNext();) {
      Notify notify = (Notify) i.next();
      Object[] args = (Object[]) this.lockManager.notifyCalls.remove(0);
      assertEquals(notify.getLockID(), args[0]);
      assertEquals(cid, args[1]);
      assertEquals(notify.getThreadID(), args[2]);
      assertEquals(Boolean.valueOf(notify.getIsAll()), args[3]);
      if (notifiedWaiters == null) {
        notifiedWaiters = (NotifiedWaiters) args[4];
      }
      assertNotNull(notifiedWaiters);
      assertSame(notifiedWaiters, args[4]);
    }

    // next, check to see that the handler puts the newly pending waiters into the broadcast context.
    BroadcastChangeContext bctxt = (BroadcastChangeContext) this.broadcastSink.queue.take();
    assertNotNull(bctxt);
    assertEquals(notifiedWaiters, bctxt.getNewlyPendingWaiters());
  }

  private ApplyTransactionContext getApplyTxnContext(ServerTransaction txt) {
    ApplyTransactionContext atc = new ApplyTransactionContext(txt, new HashMap());
    return atc;
  }

}
