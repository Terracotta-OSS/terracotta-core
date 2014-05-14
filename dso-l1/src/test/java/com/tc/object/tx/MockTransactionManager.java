/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.abortable.AbortedOperationException;
import com.tc.exception.ImplementMe;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.object.ClientIDProvider;
import com.tc.object.ObjectID;
import com.tc.object.LogicalOperation;
import com.tc.object.TCObject;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.dna.api.LogicalChangeResult;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockLevel;
import com.tc.object.locks.Notify;
import com.tc.object.metadata.MetaDataDescriptorInternal;
import com.tc.object.session.SessionID;
import com.tc.object.tx.ClientTransactionManagerImpl.ThreadTransactionLoggingStack;
import com.tc.util.Assert;
import com.tc.util.Counter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * MockTransactionManager for unit testing.
 */
public class MockTransactionManager implements ClientTransactionManager {

  private static final TCLogger logger         = TCLogging.getLogger(MockTransactionManager.class);

  private int                   commitCount;
  private final List            begins         = new ArrayList();
  // We need to remove initialValue() here because read auto locking now calls Manager.isDsoMonitored() which will
  // checks if isTransactionLogging is disabled. If it runs in the context of class loading, it will try to load
  // the class ThreadTransactionContext and thus throws a LinkageError.
  private final ThreadLocal     txnLogging     = new ThreadLocal();

  // TODO: This is a test member remove otherwise.
  private final Counter         loggingCounter = new Counter(0);

  @Override
  public void cleanup() {
    //
  }

  public Counter getLoggingCounter() {
    return this.loggingCounter;
  }

  public void clearBegins() {
    this.begins.clear();
  }

  public List getBegins() {
    List rv = new ArrayList();
    rv.addAll(this.begins);
    return rv;
  }

  @Override
  public void begin(LockID lock, LockLevel lockType, boolean atomic) {
    // System.err.println(this + ".begin(" + lock + ")");

    this.begins.add(new Begin(lock, lockType));
  }

  public void clearCommitCount() {
    this.commitCount = 0;
  }

  public int getCommitCount() {
    return this.commitCount;
  }

  @Override
  public void createObject(TCObject source) {
    throw new ImplementMe();
  }

  @Override
  public void createRoot(String name, ObjectID id) {
    throw new ImplementMe();
  }

  @Override
  public void fieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index) {
    throw new ImplementMe();
  }

  @Override
  public void logicalInvoke(TCObject source, LogicalOperation name, Object[] parameters) {
    throw new ImplementMe();
  }

  public static class Begin {
    public LockID    lock;
    public LockLevel level;

    Begin(LockID lock, LockLevel level) {
      this.lock = lock;
      this.level = level;
    }
  }

  @Override
  public void notify(Notify notify) throws UnlockedSharedObjectException {
    throw new ImplementMe();
  }

  @Override
  public void receivedBatchAcknowledgement(TxnBatchID batchID, NodeID nodeID) {
    throw new ImplementMe();
  }

  @Override
  public void apply(TxnType txType, List<LockID> lockIDs, Collection objectChanges, Map newRoots) {
    throw new ImplementMe();
  }

  @Override
  public void checkWriteAccess(Object context) {
    //
  }

  @Override
  public void receivedAcknowledgement(SessionID sessionID, TransactionID requestID, NodeID nodeID) {
    throw new ImplementMe();
  }

  @Override
  public void addReference(TCObject tco) {
    throw new ImplementMe();
  }

  @Override
  public ClientIDProvider getClientIDProvider() {
    return null;
  }

  public boolean isLocked(String lockName, int lockLevel) {
    throw new ImplementMe();
  }

  @Override
  public void commit(LockID lock, LockLevel level, boolean atomic, OnCommitCallable callable)
      throws UnlockedSharedObjectException,
      AbortedOperationException {
    if (logger.isDebugEnabled()) {
      logger.debug("commit(" + lock + ")");
    }
    callable.call();
    this.commitCount++;
  }

  public void wait(String lockName, TimerSpec call, Object object) throws UnlockedSharedObjectException {
    throw new ImplementMe();
  }

  public int queueLength(String lockName) {
    throw new ImplementMe();
  }

  public int waitLength(String lockName) {
    throw new ImplementMe();
  }

  @Override
  public void disableTransactionLogging() {
    ThreadTransactionLoggingStack txnStack = (ThreadTransactionLoggingStack) this.txnLogging.get();
    if (txnStack == null) {
      txnStack = new ThreadTransactionLoggingStack();
      this.txnLogging.set(txnStack);
    }
    txnStack.increment();
    this.loggingCounter.decrement();
  }

  @Override
  public void enableTransactionLogging() {
    ThreadTransactionLoggingStack txnStack = (ThreadTransactionLoggingStack) this.txnLogging.get();
    Assert.assertNotNull(txnStack);
    final int size = txnStack.decrement();

    if (size < 0) { throw new AssertionError("size=" + size); }
    this.loggingCounter.increment();
  }

  @Override
  public boolean isTransactionLoggingDisabled() {
    Object txnStack = this.txnLogging.get();
    return (txnStack != null) && (((ThreadTransactionLoggingStack) txnStack).get() > 0);
  }

  @Override
  public boolean isObjectCreationInProgress() {
    return false;
  }

  @Override
  public void literalValueChanged(TCObject source, Object newValue, Object oldValue) {
    throw new ImplementMe();
  }

  @Override
  public void arrayChanged(TCObject src, int startPos, Object array, int length) {
    throw new ImplementMe();
  }

  public int localHeldCount(String lockName, int lockLevel) {
    throw new ImplementMe();
  }

  @Override
  public boolean isLockOnTopStack(LockID lock) {
    return false;
  }

  @Override
  public ClientTransaction getCurrentTransaction() {
    throw new ImplementMe();
  }

  @Override
  public void waitForAllCurrentTransactionsToComplete() {
    throw new ImplementMe();
  }

  @Override
  public void addMetaDataDescriptor(TCObject tco, MetaDataDescriptorInternal md) {
    throw new ImplementMe();

  }

  @Override
  public void receivedLogicalChangeResult(Map<LogicalChangeID, LogicalChangeResult> results) {
    throw new ImplementMe();

  }

  @Override
  public boolean logicalInvokeWithResult(TCObject source, LogicalOperation method, Object[] parameters) {
    return false;
  }

}
