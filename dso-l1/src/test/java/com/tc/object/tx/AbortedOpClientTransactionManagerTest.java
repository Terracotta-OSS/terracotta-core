/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.tc.abortable.AbortableOperationManager;
import com.tc.abortable.AbortableOperationManagerImpl;
import com.tc.abortable.AbortedOperationException;
import com.tc.exception.ImplementMe;
import com.tc.net.protocol.tcm.TestChannelIDProvider;
import com.tc.object.ClientIDProviderImpl;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.TestClientObjectManager;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockLevel;
import com.tc.object.locks.MockClientLockManager;
import com.tc.object.locks.Notify;
import com.tc.object.locks.StringLockID;
import com.tc.object.logging.NullRuntimeLogger;
import com.tc.object.metadata.MetaDataDescriptorInternal;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.util.SequenceID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import junit.framework.TestCase;

public class AbortedOpClientTransactionManagerTest extends TestCase {
  TestClientTransactionFactory clientTxnFactory;
  TestRemoteTransactionManager rmtTxnMgr;
  TestClientObjectManager      objMgr;
  ClientTransactionManagerImpl clientTxnMgr;
  SynchronizedRef              error = new SynchronizedRef(null);
  AbortableOperationManager    abortableOperationManager;

  @Override
  public void setUp() throws Exception {
    clientTxnFactory = new TestClientTransactionFactory();
    rmtTxnMgr = new TestRemoteTransactionManager();
    objMgr = new TestClientObjectManager();
    abortableOperationManager = new AbortableOperationManagerImpl();

    clientTxnMgr = new ClientTransactionManagerImpl(new ClientIDProviderImpl(new TestChannelIDProvider()), objMgr,
                                                    clientTxnFactory, new MockClientLockManager(), rmtTxnMgr,
                                                    new NullRuntimeLogger(), SampledCounter.NULL_SAMPLED_COUNTER, null,
                                                    abortableOperationManager);
  }

  @Override
  public void tearDown() throws Exception {
    if (error.get() != null) { throw new RuntimeException((Throwable) error.get()); }
  }

  public void test() throws Exception {
    clientTxnMgr.begin(new StringLockID("test1"), LockLevel.WRITE);

    abortableOperationManager.begin();

    try {
      clientTxnMgr.begin(new StringLockID("test2"), LockLevel.WRITE);

      // change1
      clientTxnFactory.clientTransactions.get(0).logicalInvoke(null, -1, null, null);
      Assert.assertEquals(1, clientTxnFactory.clientTransactions.size());

      abortableOperationManager.abort(Thread.currentThread());

      boolean expected = false;
      try {
      clientTxnMgr.commit(new StringLockID("test2"), LockLevel.WRITE);
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


    Assert.assertTrue(clientTxnFactory.clientTransactions.get(0).logicalInvoked);
    Assert.assertFalse(clientTxnFactory.clientTransactions.get(0).notified);
    Assert.assertFalse(clientTxnFactory.clientTransactions.get(1).logicalInvoked);
    Assert.assertTrue(clientTxnFactory.clientTransactions.get(1).notified);

    // change2
    clientTxnMgr.commit(new StringLockID("test1"), LockLevel.WRITE);

    Assert.assertEquals(clientTxnFactory.clientTransactions.get(1), rmtTxnMgr.getTransaction());
  }

  private static class TestClientTransactionFactory implements ClientTransactionFactory {
    private final List<TestClientTransaction> clientTransactions = new ArrayList<TestClientTransaction>();

    @Override
    public ClientTransaction newNullInstance(LockID id, TxnType type) {
      return null;
    }

    @Override
    public ClientTransaction newInstance() {
      TestClientTransaction clientTransaction = new TestClientTransaction();
      clientTransactions.add(clientTransaction);
      return clientTransaction;
    }
  }

  private static class TestClientTransaction implements ClientTransaction {
    private boolean logicalInvoked = false;
    private boolean notified       = false;

    @Override
    public void setTransactionContext(TransactionContext transactionContext) {
      //
    }

    @Override
    public Map getChangeBuffers() {
      return null;
    }

    @Override
    public Map getNewRoots() {
      return null;
    }

    @Override
    public LockID getLockID() {
      throw new ImplementMe();
    }

    @Override
    public List getAllLockIDs() {
      return null;
    }

    @Override
    public void setTransactionID(TransactionID tid) {
      throw new ImplementMe();
    }

    @Override
    public TransactionID getTransactionID() {
      return null;
    }

    @Override
    public void createObject(TCObject source) {
      throw new ImplementMe();
    }

    @Override
    public void createRoot(String name, ObjectID rootID) {
      throw new ImplementMe();
    }

    @Override
    public void fieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index) {
      throw new ImplementMe();
    }

    @Override
    public void literalValueChanged(TCObject source, Object newValue, Object oldValue) {
      throw new ImplementMe();
    }

    @Override
    public void arrayChanged(TCObject source, int startPos, Object array, int length) {
      throw new ImplementMe();
    }

    @Override
    public void logicalInvoke(TCObject source, int method, Object[] parameters, String methodName) {
      logicalInvoked = true;
    }

    @Override
    public boolean hasChangesOrNotifies() {
      return notified || logicalInvoked;
    }

    @Override
    public boolean isNull() {
      throw new ImplementMe();
    }

    @Override
    public TxnType getLockType() {
      throw new ImplementMe();
    }

    @Override
    public TxnType getEffectiveType() {
      throw new ImplementMe();
    }

    @Override
    public void addNotify(Notify notify) {
      notified = true;
    }

    @Override
    public void setSequenceID(SequenceID sequenceID) {
      throw new ImplementMe();

    }

    @Override
    public SequenceID getSequenceID() {
      throw new ImplementMe();
    }

    @Override
    public boolean isConcurrent() {
      throw new ImplementMe();
    }

    @Override
    public void setAlreadyCommitted() {
      //
    }

    @Override
    public boolean hasChanges() {
      return logicalInvoked;
    }

    @Override
    public int getNotifiesCount() {
      throw new ImplementMe();
    }

    @Override
    public Collection getReferencesOfObjectsInTxn() {
      throw new ImplementMe();
    }

    @Override
    public void addDmiDescriptor(DmiDescriptor dd) {
      throw new ImplementMe();
    }

    @Override
    public void addMetaDataDescriptor(TCObject tco, MetaDataDescriptorInternal md) {
      throw new ImplementMe();
    }

    @Override
    public List getDmiDescriptors() {
      throw new ImplementMe();
    }

    @Override
    public List getNotifies() {
      throw new ImplementMe();
    }

    @Override
    public void addTransactionCompleteListener(TransactionCompleteListener l) {
      throw new ImplementMe();
    }

    @Override
    public List getTransactionCompleteListeners() {
      throw new ImplementMe();
    }

  }
}
