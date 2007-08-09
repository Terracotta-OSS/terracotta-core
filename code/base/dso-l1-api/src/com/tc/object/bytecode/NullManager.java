/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.cluster.ClusterEventListener;
import com.tc.logging.TCLogger;
import com.tc.management.beans.sessions.SessionMonitorMBean;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.event.DmiManager;
import com.tc.properties.TCProperties;

import java.lang.reflect.Field;

public final class NullManager implements Manager {

  public static final String   CLASS    = "com/tc/object/bytecode/NullManager";
  public static final String   TYPE     = "L" + CLASS + ";";

  private static final Manager INSTANCE = new NullManager();

  public static Manager getInstance() {
    return INSTANCE;
  }

  private NullManager() {
    //
  }

  public final void init() {
    //
  }

  public final void stop() {
    //
  }

  public final Object lookupOrCreateRoot(String name, Object object) {
    throw new UnsupportedOperationException();
  }

  public final Object lookupOrCreateRootNoDepth(String name, Object obj) {
    throw new UnsupportedOperationException();
  }

  public final Object createOrReplaceRoot(String name, Object object) {
    throw new UnsupportedOperationException();
  }

  public final void beginLock(String lockID, int type) {
    //
  }

  public final TCObject lookupExistingOrNull(Object obj) {
    return null;
  }

  public final void objectNotify(Object obj) {
    obj.notify();
  }

  public final void objectNotifyAll(Object obj) {
    obj.notifyAll();
  }

  public final void objectWait0(Object obj) throws InterruptedException {
    obj.wait();
  }

  public final void objectWait1(Object obj, long millis) throws InterruptedException {
    obj.wait(millis);
  }

  public final void objectWait2(Object obj, long millis, int nanos) throws InterruptedException {
    obj.wait(millis, nanos);
  }

  public final void monitorEnter(Object obj, int type) {
    //
  }

  public final void monitorExit(Object obj) {
    //
  }

  public final void logicalInvoke(Object object, String methodName, Object[] params) {
    //
  }

  public final boolean distributedMethodCall(Object receiver, String method, Object[] params, boolean runOnAllNodes) {
    return true;
  }

  public final void distributedMethodCallCommit() {
    //
  }

  public final void checkWriteAccess(Object context) {
    //
  }

  public final boolean isManaged(Object object) {
    return false;
  }

  public final boolean isLogical(Object object) {
    throw new UnsupportedOperationException();
  }

  public final boolean isRoot(Field field) {
    return false;
  }

  public final Object deepCopy(Object source) {
    throw new UnsupportedOperationException();
  }

  public final Object lookupRoot(String name) {
    throw new UnsupportedOperationException();
  }

  public final void optimisticBegin() {
    throw new UnsupportedOperationException();
  }

  public final void optimisticCommit() {
    throw new UnsupportedOperationException();
  }

  public final void optimisticRollback() {
    throw new UnsupportedOperationException();
  }

  public final void beginVolatile(TCObject tcObject, String fieldName, int type) {
    // do nothing
  }

  public final void commitLock(String lockName) {
    // do nothing
  }

  public final boolean isLocked(Object obj, int lockLevel) {
    return false;
  }

  public final int queueLength(Object obj) {
    return 0;
  }

  public final void commitVolatile(TCObject tcObject, String fieldName) {
    //
  }

  public final int waitLength(Object obj) {
    return 0;
  }

  public final boolean isHeldByCurrentThread(Object obj, int lockLevel) {
    return false;
  }

  public final void logicalInvokeWithTransaction(Object object, Object lockObject, String methodName, Object[] params) {
    throw new UnsupportedOperationException();
  }

  public final boolean tryMonitorEnter(Object obj, long timeoutInNanos, int type) {
    throw new UnsupportedOperationException();
  }

  public final boolean tryBeginLock(String lockID, int type) {
    throw new UnsupportedOperationException();
  }

  public final TCObject shareObjectIfNecessary(Object pojo) {
    throw new UnsupportedOperationException();
  }

  public final boolean isCreationInProgress() {
    return false;
  }

  public final boolean isPhysicallyInstrumented(Class clazz) {
    return false;
  }

  public final String getClientID() {
    // XXX: even though this should *probably* throw UnsupportedOperationException, because some innocent tests use
    // ManagerUtil (e.g. ConfigPropertiesTest), it was decided to return "" from this method.
    return "";
  }

  public final TCLogger getLogger(String loggerName) {
    throw new UnsupportedOperationException();
  }

  public final SessionMonitorMBean getSessionMonitorMBean() {
    throw new UnsupportedOperationException();
  }

  public final TCObject lookupOrCreate(Object obj) {
    throw new UnsupportedOperationException();
  }

  public final Object lookupObject(ObjectID id) {
    throw new UnsupportedOperationException();
  }

  public final TCProperties getTCProperites() {
    throw new UnsupportedOperationException();
  }

  public final void addClusterEventListener(ClusterEventListener cel) {
    throw new UnsupportedOperationException();
  }

  public final DmiManager getDmiManager() {
    throw new UnsupportedOperationException();
  }

  public final int localHeldCount(Object obj, int lockLevel) {
    throw new UnsupportedOperationException();
  }

}
