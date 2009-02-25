/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.cluster.DsoCluster;
import com.tc.logging.TCLogger;
import com.tc.management.beans.sessions.SessionMonitor;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.event.DmiManager;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.NamedClassLoader;
import com.tc.object.logging.InstrumentationLogger;
import com.tc.object.logging.NullInstrumentationLogger;
import com.tc.properties.TCProperties;

import java.lang.reflect.Field;

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

  public final void beginLock(final String lockID, final int type, final String lockType) {
    //
  }

  public final TCObject lookupExistingOrNull(final Object obj) {
    return null;
  }

  public final void objectNotify(final Object obj) {
    obj.notify();
  }

  public final void objectNotifyAll(final Object obj) {
    obj.notifyAll();
  }

  public final void objectWait(final Object obj) throws InterruptedException {
    obj.wait();
  }

  public final void objectWait(final Object obj, final long millis) throws InterruptedException {
    obj.wait(millis);
  }

  public final void objectWait(final Object obj, final long millis, final int nanos) throws InterruptedException {
    obj.wait(millis, nanos);
  }

  public void monitorEnterInterruptibly(final Object obj, final int type) {
    //
  }

  public final void monitorExit(final Object obj) {
    //
  }

  public final void logicalInvoke(final Object object, final String methodName, final Object[] params) {
    //
  }

  public final boolean distributedMethodCall(final Object receiver, final String method, final Object[] params, final boolean runOnAllNodes) {
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

  public final boolean isLogical(final Object object) {
    throw new UnsupportedOperationException();
  }

  public final boolean isRoot(final Field field) {
    return false;
  }

  public final Object lookupRoot(final String name) {
    throw new UnsupportedOperationException();
  }

  public final void beginVolatile(final TCObject tcObject, final String fieldName, final int type) {
    // do nothing
  }

  public final void commitLock(final String lockName) {
    // do nothing
  }

  public final boolean isLocked(final Object obj, final int lockLevel) {
    return false;
  }

  public final int queueLength(final Object obj) {
    return 0;
  }

  public final void commitVolatile(final TCObject tcObject, final String fieldName) {
    //
  }

  public final int waitLength(final Object obj) {
    return 0;
  }

  public final boolean isHeldByCurrentThread(final Object obj, final int lockLevel) {
    return false;
  }

  public final void logicalInvokeWithTransaction(final Object object, final Object lockObject, final String methodName, final Object[] params) {
    throw new UnsupportedOperationException();
  }

  public final boolean tryMonitorEnter(final Object obj, final int type, final long timeoutInNanos) {
    throw new UnsupportedOperationException();
  }

  public final boolean tryBeginLock(final String lockID, final int type) {
    throw new UnsupportedOperationException();
  }

  public final boolean tryBeginLock(final String lockID, final int type, final long timeoutInNanos) {
    throw new UnsupportedOperationException();
  }

  public final TCObject shareObjectIfNecessary(final Object pojo) {
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
    throw new UnsupportedOperationException();
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

  public final TCProperties getTCProperites() {
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

  public void monitorEnter(final Object obj, final int type, final String contextInfo) {
    //
  }

  public InstrumentationLogger getInstrumentationLogger() {
    return instrumentationLogger;
  }

  public boolean overridesHashCode(final Object obj) {
    throw new UnsupportedOperationException();
  }

  public void beginLockWithoutTxn(final String lockID, final int type) {
    //
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
}
