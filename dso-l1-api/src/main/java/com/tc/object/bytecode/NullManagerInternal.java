/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.cluster.DsoCluster;
import com.tc.logging.TCLogger;
import com.tc.management.TunneledDomainUpdater;
import com.tc.object.ObjectID;
import com.tc.object.TCObjectExternal;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.NamedClassLoader;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockLevel;
import com.tc.object.locks.Notify;
import com.tc.object.locks.UnclusteredLockID;
import com.tc.object.logging.InstrumentationLogger;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.metadata.NVPair;
import com.tc.operatorevent.TerracottaOperatorEvent.EventSubsystem;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;
import com.tc.properties.TCProperties;
import com.tc.search.SearchQueryResults;
import com.tc.statistics.StatisticRetrievalAction;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.management.MBeanServer;

public class NullManagerInternal implements ManagerInternal {
  private static final Manager NULL_MANAGER = NullManager.getInstance();

  public void lock(LockID lock, LockLevel level) {
    NULL_MANAGER.lock(lock, level);
  }

  public boolean tryLock(LockID lock, LockLevel level) {
    return NULL_MANAGER.tryLock(lock, level);
  }

  public boolean tryLock(LockID lock, LockLevel level, long timeout) throws InterruptedException {
    return NULL_MANAGER.tryLock(lock, level, timeout);
  }

  public void lockInterruptibly(LockID lock, LockLevel level) throws InterruptedException {
    NULL_MANAGER.lockInterruptibly(lock, level);
  }

  public boolean isPhysicallyInstrumented(Class clazz) {
    return NULL_MANAGER.isPhysicallyInstrumented(clazz);
  }

  public void unlock(LockID lock, LockLevel level) {
    NULL_MANAGER.unlock(lock, level);
  }

  public void init() {
    NULL_MANAGER.init();
  }

  public void initForTests() {
    NULL_MANAGER.initForTests();
  }

  public void initForTests(CountDownLatch latch) {
    //
  }

  public Notify notify(LockID lock, Object waitObject) {
    return NULL_MANAGER.notify(lock, waitObject);
  }

  public void stop() {
    NULL_MANAGER.stop();
  }

  public Object lookupOrCreateRoot(String name, Object object) {
    return NULL_MANAGER.lookupOrCreateRoot(name, object);
  }

  public Notify notifyAll(LockID lock, Object waitObject) {
    return NULL_MANAGER.notifyAll(lock, waitObject);
  }

  public Object lookupOrCreateRootNoDepth(String name, Object obj) {
    return NULL_MANAGER.lookupOrCreateRootNoDepth(name, obj);
  }

  public void wait(LockID lock, Object waitObject) throws InterruptedException {
    NULL_MANAGER.wait(lock, waitObject);
  }

  public Object createOrReplaceRoot(String rootName, Object object) {
    return NULL_MANAGER.createOrReplaceRoot(rootName, object);
  }

  public Object lookupObject(ObjectID id) throws ClassNotFoundException {
    return NULL_MANAGER.lookupObject(id);
  }

  public void wait(LockID lock, Object waitObject, long timeout) throws InterruptedException {
    NULL_MANAGER.wait(lock, waitObject, timeout);
  }

  public void preFetchObject(ObjectID id) {
    NULL_MANAGER.preFetchObject(id);
  }

  public boolean isLocked(LockID lock, LockLevel level) {
    return NULL_MANAGER.isLocked(lock, level);
  }

  public Object lookupObject(ObjectID id, ObjectID parentContext) throws ClassNotFoundException {
    return NULL_MANAGER.lookupObject(id, parentContext);
  }

  public boolean isLockedByCurrentThread(LockID lock, LockLevel level) {
    return NULL_MANAGER.isLockedByCurrentThread(lock, level);
  }

  public TCObjectExternal lookupExistingOrNull(Object obj) {
    return NULL_MANAGER.lookupExistingOrNull(obj);
  }

  public TCObjectExternal lookupOrCreate(Object obj) {
    return NULL_MANAGER.lookupOrCreate(obj);
  }

  public boolean isLockedByCurrentThread(LockLevel level) {
    return NULL_MANAGER.isLockedByCurrentThread(level);
  }

  public void logicalInvoke(Object object, String methodName, Object[] params) {
    NULL_MANAGER.logicalInvoke(object, methodName, params);
  }

  public void logicalInvokeWithTransaction(Object object, Object lockObject, String methodName, Object[] params) {
    NULL_MANAGER.logicalInvokeWithTransaction(object, lockObject, methodName, params);
  }

  public int localHoldCount(LockID lock, LockLevel level) {
    return NULL_MANAGER.localHoldCount(lock, level);
  }

  public int globalHoldCount(LockID lock, LockLevel level) {
    return NULL_MANAGER.globalHoldCount(lock, level);
  }

  public boolean distributedMethodCall(Object receiver, String method, Object[] params, boolean runOnAllNodes) {
    return NULL_MANAGER.distributedMethodCall(receiver, method, params, runOnAllNodes);
  }

  public int globalPendingCount(LockID lock) {
    return NULL_MANAGER.globalPendingCount(lock);
  }

  public void distributedMethodCallCommit() {
    NULL_MANAGER.distributedMethodCallCommit();
  }

  public Object lookupRoot(String name) {
    return NULL_MANAGER.lookupRoot(name);
  }

  public int globalWaitingCount(LockID lock) {
    return NULL_MANAGER.globalWaitingCount(lock);
  }

  public void checkWriteAccess(Object context) {
    NULL_MANAGER.checkWriteAccess(context);
  }

  public void pinLock(LockID lock) {
    NULL_MANAGER.pinLock(lock);
  }

  public int calculateDsoHashCode(Object obj) {
    return NULL_MANAGER.calculateDsoHashCode(obj);
  }

  public void unpinLock(LockID lock) {
    NULL_MANAGER.unpinLock(lock);
  }

  public LockID generateLockIdentifier(String str) {
    return NULL_MANAGER.generateLockIdentifier(str);
  }

  public LockID generateLockIdentifier(Object obj) {
    return NULL_MANAGER.generateLockIdentifier(obj);
  }

  public LockID generateLockIdentifier(Object obj, String field) {
    return NULL_MANAGER.generateLockIdentifier(obj, field);
  }

  public boolean isLiteralInstance(Object obj) {
    return NULL_MANAGER.isLiteralInstance(obj);
  }

  public boolean isManaged(Object object) {
    return NULL_MANAGER.isManaged(object);
  }

  public boolean isLiteralAutolock(Object o) {
    return NULL_MANAGER.isLiteralAutolock(o);
  }

  public boolean isDsoMonitored(Object obj) {
    return NULL_MANAGER.isDsoMonitored(obj);
  }

  public boolean isDsoMonitorEntered(Object obj) {
    return NULL_MANAGER.isDsoMonitorEntered(obj);
  }

  public Object getChangeApplicator(Class clazz) {
    return NULL_MANAGER.getChangeApplicator(clazz);
  }

  public boolean isLogical(Object object) {
    return NULL_MANAGER.isLogical(object);
  }

  public boolean isRoot(Field field) {
    return NULL_MANAGER.isRoot(field);
  }

  public String getClientID() {
    return NULL_MANAGER.getClientID();
  }

  public String getUUID() {
    return NULL_MANAGER.getUUID();
  }

  public TCLogger getLogger(String loggerName) {
    return NULL_MANAGER.getLogger(loggerName);
  }

  public InstrumentationLogger getInstrumentationLogger() {
    return NULL_MANAGER.getInstrumentationLogger();
  }

  public TCProperties getTCProperties() {
    return NULL_MANAGER.getTCProperties();
  }

  public boolean isFieldPortableByOffset(Object pojo, long fieldOffset) {
    return NULL_MANAGER.isFieldPortableByOffset(pojo, fieldOffset);
  }

  public boolean overridesHashCode(Object obj) {
    return NULL_MANAGER.overridesHashCode(obj);
  }

  public void registerNamedLoader(NamedClassLoader loader, String webAppName) {
    NULL_MANAGER.registerNamedLoader(loader, webAppName);
  }

  public ClassProvider getClassProvider() {
    return NULL_MANAGER.getClassProvider();
  }

  public TunneledDomainUpdater getTunneledDomainUpdater() {
    return NULL_MANAGER.getTunneledDomainUpdater();
  }

  public DsoCluster getDsoCluster() {
    return NULL_MANAGER.getDsoCluster();
  }

  public MBeanServer getMBeanServer() {
    return NULL_MANAGER.getMBeanServer();
  }

  public StatisticRetrievalAction getStatisticRetrievalActionInstance(String name) {
    return NULL_MANAGER.getStatisticRetrievalActionInstance(name);
  }

  public void registerStatisticRetrievalAction(StatisticRetrievalAction sra) {
    NULL_MANAGER.registerStatisticRetrievalAction(sra);
  }

  public void monitorEnter(LockID lock, LockLevel level) {
    NULL_MANAGER.monitorEnter(lock, level);
  }

  public void monitorExit(LockID lock, LockLevel level) {
    NULL_MANAGER.monitorExit(lock, level);
  }

  public SessionConfiguration getSessionConfiguration(String appName) {
    return NULL_MANAGER.getSessionConfiguration(appName);
  }

  public void waitForAllCurrentTransactionsToComplete() {
    NULL_MANAGER.waitForAllCurrentTransactionsToComplete();
  }

  public void registerBeforeShutdownHook(Runnable beforeShutdownHook) {
    NULL_MANAGER.registerBeforeShutdownHook(beforeShutdownHook);
  }

  public LockID generateLockIdentifier(long lockId) {
    return UnclusteredLockID.UNCLUSTERED_LOCK_ID;
  }

  public MetaDataDescriptor createMetaDataDescriptor(String category) {
    throw new UnsupportedOperationException();
  }

  public SearchQueryResults executeQuery(String cachename, List queryStack, boolean includeKeys, boolean includeValues,
                                         Set<String> attributeSet, List<NVPair> sortAttributes,
                                         List<NVPair> aggregators, int maxResults, int batchSize) {
    throw new UnsupportedOperationException();
  }

  public NVPair createNVPair(String name, Object value) {
    throw new UnsupportedOperationException();
  }

  public void verifyCapability(String capability) {
    // do nothing
  }

  public void fireOperatorEvent(EventType eventLevel, EventSubsystem eventSubsystem, String eventMessage) {
    //
  }

  public void stopImmediate() {
    //
  }

}
