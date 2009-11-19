/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.cluster.DsoCluster;
import com.tc.logging.NullTCLogger;
import com.tc.logging.TCLogger;
import com.tc.management.beans.sessions.SessionMonitor;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.event.DmiManager;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.NamedClassLoader;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockLevel;
import com.tc.object.locks.Notify;
import com.tc.object.locks.UnclusteredLockID;
import com.tc.object.logging.InstrumentationLogger;
import com.tc.object.logging.NullInstrumentationLogger;
import com.tc.properties.TCProperties;
import com.tc.statistics.StatisticRetrievalAction;

import java.lang.reflect.Field;

import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Null implementation of the manager.
 */
public final class NullManager implements Manager {

  public static final String                 CLASS                 = "com/tc/object/bytecode/NullManager";
  public static final String                 TYPE                  = "L" + CLASS + ";";

  private static final Manager               INSTANCE              = new NullManager();

  private static final InstrumentationLogger instrumentationLogger = new NullInstrumentationLogger();

  /**
   * Get instance of the null manager
   * 
   * @return NullManager
   */
  public static Manager getInstance() {
    return INSTANCE;
  }

  private NullManager() {
    //
  }

  public final void init() {
    //
  }

  public final void initForTests() {
    //
  }

  public final void stop() {
    //
  }

  public final Object lookupOrCreateRoot(final String name, final Object object) {
    throw new UnsupportedOperationException();
  }

  public final Object lookupOrCreateRootNoDepth(final String name, final Object obj) {
    throw new UnsupportedOperationException();
  }

  public final Object createOrReplaceRoot(final String name, final Object object) {
    throw new UnsupportedOperationException();
  }

  public final TCObject lookupExistingOrNull(final Object obj) {
    return null;
  }

  public final void logicalInvoke(final Object object, final String methodName, final Object[] params) {
    //
  }

  public final boolean distributedMethodCall(final Object receiver, final String method, final Object[] params,
                                             final boolean runOnAllNodes) {
    return true;
  }

  public final void distributedMethodCallCommit() {
    //
  }

  public final void checkWriteAccess(final Object context) {
    //
  }

  public final boolean isManaged(final Object object) {
    return false;
  }

  public final boolean isLiteralInstance(final Object object) {
    return false;
  }

  public boolean isLiteralAutolock(Object o) {
    return false;
  }

  public final int calculateDsoHashCode(final Object object) {
    return 0;
  }

  public final boolean isLogical(final Object object) {
    throw new UnsupportedOperationException();
  }

  public final boolean isRoot(final Field field) {
    return false;
  }

  public final Object lookupRoot(final String name) {
    throw new UnsupportedOperationException();
  }

  public final boolean isLockHeldByCurrentThread(final String lockId, final int lockLevel) {
    return false;
  }

  public final void logicalInvokeWithTransaction(final Object object, final Object lockObject, final String methodName,
                                                 final Object[] params) {
    throw new UnsupportedOperationException();
  }

  public final boolean isPhysicallyInstrumented(final Class clazz) {
    return false;
  }

  public final String getClientID() {
    // XXX: even though this should *probably* throw UnsupportedOperationException, because some innocent tests use
    // ManagerUtil (e.g. ConfigPropertiesTest), it was decided to return "" from this method.
    return "";
  }

  public final TCLogger getLogger(final String loggerName) {
    return new NullTCLogger();
  }

  public final SessionMonitor getHttpSessionMonitor() {
    throw new UnsupportedOperationException();
  }

  public final TCObject lookupOrCreate(final Object obj) {
    throw new UnsupportedOperationException();
  }

  public final Object lookupObject(final ObjectID id) {
    throw new UnsupportedOperationException();
  }

  public Object lookupObject(final ObjectID id, final ObjectID parentContext) {
    throw new UnsupportedOperationException();
  }

  public final TCProperties getTCProperties() {
    throw new UnsupportedOperationException();
  }

  public final DmiManager getDmiManager() {
    throw new UnsupportedOperationException();
  }

  public final int localHeldCount(final Object obj, final int lockLevel) {
    throw new UnsupportedOperationException();
  }

  public boolean isDsoMonitored(final Object obj) {
    return false;
  }

  public boolean isDsoMonitorEntered(final Object obj) {
    return false;
  }

  public boolean isFieldPortableByOffset(final Object pojo, final long fieldOffset) {
    throw new UnsupportedOperationException();
  }

  public InstrumentationLogger getInstrumentationLogger() {
    return instrumentationLogger;
  }

  public boolean overridesHashCode(final Object obj) {
    throw new UnsupportedOperationException();
  }

  public void registerNamedLoader(final NamedClassLoader loader, final String webAppName) {
    throw new UnsupportedOperationException();
  }

  public ClassProvider getClassProvider() {
    throw new UnsupportedOperationException();
  }

  public DsoCluster getDsoCluster() {
    throw new UnsupportedOperationException();
  }

  public MBeanServer getMBeanServer() {
    return null;
  }

  public void preFetchObject(final ObjectID id) {
    return;
  }

  public StatisticRetrievalAction getStatisticRetrievalActionInstance(final String name) {
    return null;
  }

  public Object getChangeApplicator(final Class clazz) {
    return null;
  }

  public LockID generateLockIdentifier(String str) {
    return UnclusteredLockID.UNCLUSTERED_LOCK_ID;
  }

  public LockID generateLockIdentifier(Object obj) {
    return UnclusteredLockID.UNCLUSTERED_LOCK_ID;
  }

  public LockID generateLockIdentifier(Object obj, String field) {
    return UnclusteredLockID.UNCLUSTERED_LOCK_ID;
  }

  public int globalHoldCount(LockID lock, LockLevel level) {
    throw new UnsupportedOperationException();
  }

  public int globalPendingCount(LockID lock) {
    throw new UnsupportedOperationException();
  }

  public int globalWaitingCount(LockID lock) {
    throw new UnsupportedOperationException();
  }

  public boolean isLocked(LockID lock, LockLevel level) {
    throw new UnsupportedOperationException();
  }

  public boolean isLockedByCurrentThread(LockID lock, LockLevel level) {
    throw new UnsupportedOperationException();
  }

  public int localHoldCount(LockID lock, LockLevel level) {
    throw new UnsupportedOperationException();
  }

  public void lock(LockID lock, LockLevel level) {
    //
  }

  public void lockInterruptibly(LockID lock, LockLevel level) {
    //
  }

  public Notify notify(LockID lock, Object waitObject) {
    if (waitObject != null) {
      waitObject.notify();
    }
    return null;
  }

  public Notify notifyAll(LockID lock, Object waitObject) {
    if (waitObject != null) {
      waitObject.notifyAll();
    }
    return null;
  }

  public boolean tryLock(LockID lock, LockLevel level) {
    throw new UnsupportedOperationException();
  }

  public boolean tryLock(LockID lock, LockLevel level, long timeout) {
    throw new UnsupportedOperationException();
  }

  public void unlock(LockID lock, LockLevel level) {
    //
  }

  public void wait(LockID lock, Object waitObject) throws InterruptedException {
    if (waitObject != null) {
      waitObject.wait();
    }
  }

  public void wait(LockID lock, Object waitObject, long timeout) throws InterruptedException {
    if (waitObject != null) {
      waitObject.wait(timeout);
    }
  }

  public void pinLock(LockID lock) {
    throw new UnsupportedOperationException();
  }

  public void unpinLock(LockID lock) {
    throw new UnsupportedOperationException();
  }

  public boolean isLockedByCurrentThread(LockLevel level) {
    throw new UnsupportedOperationException();
  }

  public void monitorEnter(LockID lock, LockLevel level) {
    //
  }

  public void monitorExit(LockID lock, LockLevel level) {
    //
  }

  public void registerMBean(Object bean, ObjectName name) {
    /**/
  }

  public String getUUID() {
    return null;
  }
}
