/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import org.mockito.Matchers;
import org.mockito.Mockito;

import com.tc.abortable.AbortableOperationManager;
import com.tc.abortable.AbortableOperationManagerImpl;
import com.tc.abortable.AbortedOperationException;
import com.tc.net.protocol.tcm.TestChannelIDProvider;
import com.tc.object.ClientIDProviderImpl;
import com.tc.object.LogicalOperation;
import com.tc.object.TCObject;
import com.tc.object.TestClientObjectManager;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockLevel;
import com.tc.object.locks.MockClientLockManager;
import com.tc.object.locks.Notify;
import com.tc.object.locks.StringLockID;
import com.tc.stats.counter.sampled.SampledCounter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;
import junit.framework.TestCase;

public class AbortedOpClientTransactionManagerTest extends TestCase {
  TestClientTransactionFactory clientTxnFactory;
  TestRemoteTransactionManager rmtTxnMgr;
  TestClientObjectManager      objMgr;
  ClientTransactionManagerImpl clientTxnMgr;
  AtomicReference<Throwable>   error = new AtomicReference<Throwable>(null);
  AbortableOperationManager    abortableOperationManager;

  @Override
  public void setUp() throws Exception {
    clientTxnFactory = new TestClientTransactionFactory();
    rmtTxnMgr = new TestRemoteTransactionManager();
    objMgr = new TestClientObjectManager();
    abortableOperationManager = new AbortableOperationManagerImpl();

    clientTxnMgr = new ClientTransactionManagerImpl(new ClientIDProviderImpl(new TestChannelIDProvider()), objMgr,
                                                    clientTxnFactory, new MockClientLockManager(), rmtTxnMgr,
                                                    SampledCounter.NULL_SAMPLED_COUNTER, null,
                                                    abortableOperationManager);
  }

  @Override
  public void tearDown() throws Exception {
    if (error.get() != null) { throw new RuntimeException(error.get()); }
  }

  public void test() throws Exception {
    clientTxnMgr.begin(new StringLockID("test1"), LockLevel.WRITE, false);
    abortableOperationManager.begin();

    try {
      clientTxnMgr.begin(new StringLockID("test2"), LockLevel.WRITE, false);

      // change1
      clientTxnFactory.clientTransactions.get(0).logicalInvoke(null, LogicalOperation.ADD, null, null);
      Assert.assertEquals(1, clientTxnFactory.clientTransactions.size());

      abortableOperationManager.abort(Thread.currentThread());

      boolean expected = false;
      try {
        clientTxnMgr.commit(new StringLockID("test2"), LockLevel.WRITE, false, null);
      } catch (AbortedOperationException e) {
        expected = true;
      }

      Assert.assertTrue(expected);
    } finally {
      abortableOperationManager.finish();
    }
    Assert.assertEquals(2, clientTxnFactory.clientTransactions.size());

    // change 2
    clientTxnFactory.clientTransactions.get(1).addNotify(null);

    Mockito.verify(clientTxnFactory.clientTransactions.get(0), Mockito.times(1))
        .logicalInvoke((TCObject) Matchers.any(), Matchers.any(LogicalOperation.class), (Object[]) Matchers.any(),
                       (LogicalChangeID) Matchers.any());
    Mockito.verify(clientTxnFactory.clientTransactions.get(1), Mockito.never())
        .logicalInvoke((TCObject) Matchers.any(), Matchers.any(LogicalOperation.class), (Object[]) Matchers.any(),
                       (LogicalChangeID) Matchers.any());
    
    Mockito.verify(clientTxnFactory.clientTransactions.get(0), Mockito.never())
        .addNotify((Notify) Matchers
                                                                                               .anyObject());
    Mockito.verify(clientTxnFactory.clientTransactions.get(1), Mockito.times(1)).addNotify((Notify) Matchers
                                                                                               .anyObject());
    // change2
    clientTxnMgr.commit(new StringLockID("test1"), LockLevel.WRITE, false, null);

    Assert.assertEquals(clientTxnFactory.clientTransactions.get(1), rmtTxnMgr.getTransaction());
  }

  private static class TestClientTransactionFactory implements ClientTransactionFactory {
    private final List<ClientTransaction> clientTransactions = new ArrayList<ClientTransaction>();

    @Override
    public ClientTransaction newNullInstance(LockID id, TxnType type) {
      return null;
    }

    @Override
    public ClientTransaction newInstance(int session) {
      ClientTransaction clientTransaction = Mockito.mock(ClientTransaction.class);
      Mockito.when(clientTransaction.hasChangesOrNotifies()).thenReturn(true);

      clientTransactions.add(clientTransaction);
      return clientTransaction;
    }
  }

}
