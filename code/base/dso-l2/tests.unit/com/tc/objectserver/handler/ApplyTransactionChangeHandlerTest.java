/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.impl.MockSink;
import com.tc.async.impl.MockStage;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.Notify;
import com.tc.object.lockmanager.api.ThreadID;
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
import com.tc.objectserver.lockmanager.api.NotifiedWaiters;
import com.tc.objectserver.lockmanager.api.TestLockManager;
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

  public void setUp() throws Exception {
    instanceMonitor = new ObjectInstanceMonitorImpl();
    transactionManager = new TestServerTransactionManager();
    lockManager = new TestLockManager();
    gtxm = new TestGlobalTransactionManager();
    handler = new ApplyTransactionChangeHandler(instanceMonitor, gtxm);

    MockStage stageBo = new MockStage("Bo");
    MockStage stageCo = new MockStage("Co");
    broadcastSink = stageBo.sink;
    TestServerConfigurationContext context = new TestServerConfigurationContext();
    context.transactionManager = transactionManager;
    context.txnObjectManager = new NullTransactionalObjectManager();
    context.addStage(ServerConfigurationContext.BROADCAST_CHANGES_STAGE, stageBo);
    context.addStage(ServerConfigurationContext.COMMIT_CHANGES_STAGE, stageCo);
    context.lockManager = lockManager;

    handler.initialize(context);
  }

  public void testLockManagerNotifyIsCalled() throws Exception {
    TxnBatchID batchID = new TxnBatchID(1);
    TransactionID txID = new TransactionID(1);
    LockID[] lockIDs = new LockID[] { new LockID("1") };
    ChannelID channelID = new ChannelID(1);
    List dnas = Collections.unmodifiableList(new LinkedList());
    ObjectStringSerializer serializer = null;
    Map newRoots = Collections.unmodifiableMap(new HashMap());
    TxnType txnType = TxnType.NORMAL;
    List notifies = new LinkedList();

    for (int i = 0; i < 10; i++) {
      notifies.add(new Notify(new LockID("" + i), new ThreadID(i), i % 2 == 0));
    }
    SequenceID sequenceID = new SequenceID(1);
    ServerTransaction tx = new ServerTransactionImpl(gtxm, batchID, txID, sequenceID, lockIDs, channelID, dnas,
                                                     serializer, newRoots, txnType, notifies, DmiDescriptor.EMPTY_ARRAY);
    // call handleEvent with the global transaction reporting that it doesn't need an apply...
    assertTrue(lockManager.notifyCalls.isEmpty());
    assertTrue(broadcastSink.queue.isEmpty());
    handler.handleEvent(getApplyTxnContext(tx));
    // even if the transaction has already been applied, the notifies must be applied, since they aren't persistent.
    assertEquals(notifies.size(), lockManager.notifyCalls.size());
    lockManager.notifyCalls.clear();
    assertNotNull(broadcastSink.queue.remove(0));

    // call handleEvent with the global transaction reporting that it DOES need an apply...
    handler.handleEvent(getApplyTxnContext(tx));

    assertEquals(notifies.size(), lockManager.notifyCalls.size());
    NotifiedWaiters notifiedWaiters = null;
    for (Iterator i = notifies.iterator(); i.hasNext();) {
      Notify notify = (Notify) i.next();
      Object[] args = (Object[]) lockManager.notifyCalls.remove(0);
      assertEquals(notify.getLockID(), args[0]);
      assertEquals(channelID, args[1]);
      assertEquals(notify.getThreadID(), args[2]);
      assertEquals(new Boolean(notify.getIsAll()), args[3]);
      if (notifiedWaiters == null) {
        notifiedWaiters = (NotifiedWaiters) args[4];
      }
      assertNotNull(notifiedWaiters);
      assertSame(notifiedWaiters, args[4]);
    }

    // next, check to see that the handler puts the newly pending waiters into the broadcast context.
    BroadcastChangeContext bctxt = (BroadcastChangeContext) broadcastSink.queue.remove(0);
    assertNotNull(bctxt);
    assertEquals(notifiedWaiters, bctxt.getNewlyPendingWaiters());
  }

  private ApplyTransactionContext getApplyTxnContext(ServerTransaction txt) {
    ApplyTransactionContext atc = new ApplyTransactionContext(txt, new HashMap());
    return atc;
  }

}
