/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.exception.ImplementMe;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.ChannelIDProvider;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.session.SessionID;
import com.tc.object.tx.ClientTransactionManagerImpl.ThreadTransactionLoggingStack;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.Counter;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  public Counter getLoggingCounter() {
    return loggingCounter;
  }

  public void clearBegins() {
    begins.clear();
  }

  public List getBegins() {
    List rv = new ArrayList();
    rv.addAll(begins);
    return rv;
  }

  public boolean begin(String lock, int lockType, String lockObjectType, String contextInfo) {
    // System.err.println(this + ".begin(" + lock + ")");

    begins.add(new Begin(lock, lockType));
    return true;
  }

  public void clearCommitCount() {
    this.commitCount = 0;
  }

  public int getCommitCount() {
    return this.commitCount;
  }

  public void createObject(TCObject source) {
    throw new ImplementMe();
  }

  public void createRoot(String name, ObjectID id) {
    throw new ImplementMe();
  }

  public void fieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index) {
    throw new ImplementMe();
  }

  public void logicalInvoke(TCObject source, int name, String methodName, Object[] parameters) {
    throw new ImplementMe();
  }

  public static class Begin {
    public String lockName;
    public int    lockType;

    Begin(String name, int type) {
      this.lockName = name;
      this.lockType = type;
    }
  }

  public void wait(TimerSpec call, Object object) {
    throw new ImplementMe();
  }

  public void notify(String lockName, boolean all, Object object) throws UnlockedSharedObjectException {
    throw new ImplementMe();
  }

  public void receivedBatchAcknowledgement(TxnBatchID batchID) {
    throw new ImplementMe();
  }

  public void apply(TxnType txType, List lockIDs, Collection objectChanges, Set lookupObjectIDs, Map newRoots) {
    throw new ImplementMe();
  }

  public void checkWriteAccess(Object context) {
    //
  }

  public void receivedAcknowledgement(SessionID sessionID, TransactionID requestID) {
    throw new ImplementMe();
  }

  public void addReference(TCObject tco) {
    throw new ImplementMe();
  }

  public ChannelIDProvider getChannelIDProvider() {
    return null;
  }

  public boolean isLocked(String lockName, int lockLevel) {
    throw new ImplementMe();
  }

  public void commit(String lockName) throws UnlockedSharedObjectException {
    if (logger.isDebugEnabled()) {
      logger.debug("commit(lockName)");
    }
    this.commitCount++;
  }

  public void wait(String lockName, TimerSpec call, Object object) throws UnlockedSharedObjectException {
    throw new ImplementMe();
  }

  // public void lock(String lockName, int lockLevel) {
  // throw new ImplementMe();
  // }
  //
  public void unlock(String lockName) {
    throw new ImplementMe();
  }

  public int queueLength(String lockName) {
    throw new ImplementMe();
  }

  public int waitLength(String lockName) {
    throw new ImplementMe();
  }

  public void disableTransactionLogging() {
    ThreadTransactionLoggingStack txnStack = (ThreadTransactionLoggingStack) txnLogging.get();
    if (txnStack == null) {
      txnStack = new ThreadTransactionLoggingStack();
      txnLogging.set(txnStack);
    }
    txnStack.increment();
    loggingCounter.decrement();
  }

  public void enableTransactionLogging() {
    ThreadTransactionLoggingStack txnStack = (ThreadTransactionLoggingStack) txnLogging.get();
    Assert.assertNotNull(txnStack);
    final int size = txnStack.decrement();

    if (size < 0) { throw new AssertionError("size=" + size); }
    loggingCounter.increment();
  }

  public boolean isTransactionLoggingDisabled() {
    Object txnStack = txnLogging.get();
    return (txnStack != null) && (((ThreadTransactionLoggingStack) txnStack).get() > 0);
  }

  public boolean isHeldByCurrentThread(String lockName, int lockLevel) {
    throw new ImplementMe();
  }

  public boolean tryBegin(String lock, TimerSpec timeout, int lockLevel, String lockObjectType) {
    throw new ImplementMe();
  }

  public void literalValueChanged(TCObject source, Object newValue, Object oldValue) {
    throw new ImplementMe();
  }

  public void arrayChanged(TCObject src, int startPos, Object array, int length) {
    throw new ImplementMe();
  }

  public void addDmiDescriptor(DmiDescriptor d) {
    throw new ImplementMe();
  }

  public int localHeldCount(String lockName, int lockLevel) {
    throw new ImplementMe();
  }

  public boolean isLockOnTopStack(String lockName) {
    return false;
  }

  public String dump() {
    throw new ImplementMe();
  }

  public void dump(Writer writer) {
    throw new ImplementMe();

  }

  public void dumpToLogger() {
    throw new ImplementMe();

  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    throw new ImplementMe();
  }
}
