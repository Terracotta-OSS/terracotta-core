/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.abortable.AbortableOperationManager;
import com.tc.cluster.DsoCluster;
import com.tc.exception.ImplementMe;
import com.tc.logging.NullTCLogger;
import com.tc.logging.TCLogger;
import com.tc.management.TunneledDomainUpdater;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.object.ObjectID;
import com.tc.object.ServerEventDestination;
import com.tc.object.TCObject;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockLevel;
import com.tc.object.locks.Notify;
import com.tc.object.locks.UnclusteredLockID;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.tx.TransactionCompleteListener;
import com.tc.operatorevent.TerracottaOperatorEvent.EventSubsystem;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;
import com.tc.platform.PlatformService;
import com.tc.properties.NullTCProperties;
import com.tc.properties.TCProperties;
import com.tc.search.SearchQueryResults;
import com.tc.search.SearchRequestID;
import com.tc.server.ServerEventType;
import com.tc.util.concurrent.Runners;
import com.tc.util.concurrent.TaskRunner;
import com.terracottatech.search.NVPair;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;

/**
 * Null implementation of the manager.
 */
public class NullManager implements Manager {

  public static final String                 CLASS                 = "com/tc/object/bytecode/NullManager";
  public static final String                 TYPE                  = "L" + CLASS + ";";

  private static final Manager               INSTANCE              = new NullManager();

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

  @Override
  public void init() {
    //
  }

  @Override
  public void initForTests() {
    //
  }

  @Override
  public void stop() {
    //
  }

  @Override
  public Object lookupOrCreateRoot(String name, Object object) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object lookupOrCreateRootNoDepth(String name, Object obj) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object createOrReplaceRoot(String name, Object object) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TCObject lookupExistingOrNull(Object obj) {
    return null;
  }

  @Override
  public void logicalInvoke(Object object, String methodName, Object[] params) {
    //
  }

  @Override
  public void checkWriteAccess(Object context) {
    //
  }

  @Override
  public boolean isManaged(Object object) {
    return false;
  }

  @Override
  public boolean isLiteralInstance(Object object) {
    return false;
  }

  @Override
  public boolean isLiteralAutolock(Object o) {
    return false;
  }

  @Override
  public boolean isLogical(Object object) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isRoot(Field field) {
    return false;
  }

  @Override
  public Object lookupRoot(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void logicalInvokeWithTransaction(Object object, Object lockObject, String methodName, Object[] params) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ClientID getClientID() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TCLogger getLogger(String loggerName) {
    return new NullTCLogger();
  }

  @Override
  public TCObject lookupOrCreate(Object obj) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object lookupObject(ObjectID id) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object lookupObject(ObjectID id, ObjectID parentContext) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TCProperties getTCProperties() {
    return NullTCProperties.INSTANCE;
  }

  @Override
  public boolean isFieldPortableByOffset(Object pojo, long fieldOffset) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ClassProvider getClassProvider() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TunneledDomainUpdater getTunneledDomainUpdater() {
    throw new UnsupportedOperationException();
  }

  @Override
  public DsoCluster getDsoCluster() {
    return null;
  }

  @Override
  public MBeanServer getMBeanServer() {
    return null;
  }

  @Override
  public void preFetchObject(ObjectID id) {
    return;
  }

  @Override
  public Object getChangeApplicator(Class clazz) {
    return null;
  }

  @Override
  public LockID generateLockIdentifier(String str) {
    return UnclusteredLockID.UNCLUSTERED_LOCK_ID;
  }

  @Override
  public LockID generateLockIdentifier(Object obj) {
    return UnclusteredLockID.UNCLUSTERED_LOCK_ID;
  }

  @Override
  public LockID generateLockIdentifier(Object obj, String field) {
    return UnclusteredLockID.UNCLUSTERED_LOCK_ID;
  }

  @Override
  public LockID generateLockIdentifier(long lockId) {
    return UnclusteredLockID.UNCLUSTERED_LOCK_ID;
  }

  @Override
  public int globalHoldCount(LockID lock, LockLevel level) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int globalPendingCount(LockID lock) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int globalWaitingCount(LockID lock) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isLocked(LockID lock, LockLevel level) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isLockedByCurrentThread(LockID lock, LockLevel level) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int localHoldCount(LockID lock, LockLevel level) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void lock(LockID lock, LockLevel level) {
    //
  }

  @Override
  public void lockInterruptibly(LockID lock, LockLevel level) {
    //
  }

  @Override
  public Notify notify(LockID lock, Object waitObject) {
    if (waitObject != null) {
      waitObject.notify();
    }
    return null;
  }

  @Override
  public Notify notifyAll(LockID lock, Object waitObject) {
    if (waitObject != null) {
      waitObject.notifyAll();
    }
    return null;
  }

  @Override
  public boolean tryLock(LockID lock, LockLevel level) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean tryLock(LockID lock, LockLevel level, long timeout) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void unlock(LockID lock, LockLevel level) {
    //
  }

  @Override
  public void wait(LockID lock, Object waitObject) throws InterruptedException {
    if (waitObject != null) {
      waitObject.wait();
    }
  }

  @Override
  public void wait(LockID lock, Object waitObject, long timeout) throws InterruptedException {
    if (waitObject != null) {
      waitObject.wait(timeout);
    }
  }

  @Override
  public void pinLock(LockID lock, long awardID) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void unpinLock(LockID lock, long awardID) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isLockedByCurrentThread(LockLevel level) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getUUID() {
    return null;
  }

  @Override
  public void waitForAllCurrentTransactionsToComplete() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerBeforeShutdownHook(Runnable beforeShutdownHook) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void unregisterBeforeShutdownHook(Runnable beforeShutdownHook) {
    throw new UnsupportedOperationException();
  }

  @Override
  public MetaDataDescriptor createMetaDataDescriptor(String category) {
    throw new UnsupportedOperationException();
  }

  @Override
  public SearchQueryResults executeQuery(String cachename, List queryStack, boolean includeKeys, boolean includeValues,
                                         Set<String> attributeSet, List<NVPair> sortAttributes,
                                         List<NVPair> aggregators, int maxResults, int batchSize, int resultPageSize,
                                         boolean waitForTxn, SearchRequestID reqId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public SearchQueryResults executeQuery(String cachename, List queryStack, Set<String> attributeSet,
                                         Set<String> groupByAttribues, List<NVPair> sortAttributes,
                                         List<NVPair> aggregators, int maxResults, int batchSize, boolean waitForTxn,
                                         SearchRequestID reqId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public NVPair createNVPair(String name, Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void verifyCapability(String capability) {
    // do nothing
  }

  @Override
  public void fireOperatorEvent(EventType eventLevel, EventSubsystem eventSubsystem, String eventMessage) {
    //
  }

  @Override
  public void stopImmediate() {
    //
  }

  @Override
  public Object lookupOrCreateRoot(String name, Object object, GroupID gid) {
    throw new UnsupportedOperationException();
  }

  @Override
  public GroupID[] getGroupIDs() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object lookupRoot(String name, GroupID gid) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TCObject lookupOrCreate(Object obj, GroupID gid) {
    throw new ImplementMe();
  }

  @Override
  public void lockIDWait(LockID lock, long timeout) {
    throw new UnsupportedOperationException();

  }

  @Override
  public void lockIDNotifyAll(LockID lock) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void lockIDNotify(LockID lock) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T lookupRegisteredObjectByName(String name, Class<T> expectedType) {
    throw new ImplementMe();
  }


  @Override
  public void addTransactionCompleteListener(TransactionCompleteListener listener) {
    //
  }

  @Override
  public AbortableOperationManager getAbortableOperationManager() {
    return null;
  }

  @Override
  public PlatformService getPlatformService() {
    return null;
  }

  @Override
  public void throttlePutIfNecessary(final ObjectID object) {
    //
  }


  @Override
  public void beginAtomicTransaction(LockID lock, LockLevel level) {
    //
  }

  @Override
  public void commitAtomicTransaction(LockID lock, LockLevel level) {
    //
  }

  @Override
  public void registerServerEventListener(final ServerEventDestination destination, final Set<ServerEventType> listenTo) {
    //
  }

  @Override
  public void unregisterServerEventListener(final ServerEventDestination destination, final Set<ServerEventType> listenTo) {
    //
  }

  @Override
  public int getRejoinCount() {
    return 0;
  }

  @Override
  public <T> T registerObjectByNameIfAbsent(String name, T object) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isRejoinInProgress() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TaskRunner getTastRunner() {
    return Runners.newDefaultCachedScheduledTaskRunner();
  }

  @Override
  public long getLockAwardIDFor(LockID lock) {
    throw new ImplementMe();
  }

  @Override
  public boolean isLockAwardValid(LockID lock, long awardID) {
    throw new ImplementMe();
  }
}
