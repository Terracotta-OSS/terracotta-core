/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.platform;

import com.google.common.base.Preconditions;
import com.tc.abortable.AbortableOperationManager;
import com.tc.abortable.AbortedOperationException;
import com.tc.cluster.DsoCluster;
import com.tc.exception.TCClassNotFoundException;
import com.tc.logging.TCLogger;
import com.tc.net.GroupID;
import com.tc.object.ObjectID;
import com.tc.object.ServerEventDestination;
import com.tc.object.TCObject;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockLevel;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.tx.TransactionCompleteListener;
import com.tc.operatorevent.TerracottaOperatorEvent.EventSubsystem;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;
import com.tc.platform.rejoin.RejoinLifecycleListener;
import com.tc.platform.rejoin.RejoinManager;
import com.tc.properties.TCProperties;
import com.tc.search.SearchQueryResults;
import com.tc.server.ServerEventType;
import com.tc.util.VicariousThreadLocal;
import com.tcclient.cluster.DsoNode;
import com.terracottatech.search.NVPair;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class PlatformServiceImpl implements PlatformService {
  private final Manager                           manager;
  private volatile RejoinLifecycleEventController rejoinEventsController;
  private final boolean                           rejoinEnabled;
  private static final int                               BASE_COUNT  = 1;
  private static final ThreadLocal<Map<Object, Integer>> lockIdToCount = new VicariousThreadLocal<Map<Object, Integer>>() {
                                                                         @Override
                                                                         protected Map<Object, Integer> initialValue() {
                                                                           return new HashMap<Object, Integer>();
                                                                         }
                                                                       };

  public PlatformServiceImpl(Manager manager, boolean rejoinEnabled) {
    this.manager = manager;
    this.rejoinEnabled = rejoinEnabled;
  }

  private void addContext(Object lockId) {
    Map<Object, Integer> threadLocal = lockIdToCount.get();
    Integer count = threadLocal.get(lockId);
    Integer lockCount = count != null ? new Integer(count.intValue() + BASE_COUNT) : new Integer(BASE_COUNT);
    threadLocal.put(lockId, lockCount);
  }

  private void removeContext(Object lockId) {
    Map<Object, Integer> threadLocal = lockIdToCount.get();
    Integer count = threadLocal.get(lockId);
    if(count != null) {
      if (count.intValue() == BASE_COUNT) {
        threadLocal.remove(lockId);
      } else {
        threadLocal.put(lockId, new Integer(count.intValue() - BASE_COUNT));
      }
    }
  }

  @Override
  public boolean isExplicitlyLocked() {
    return !lockIdToCount.get().isEmpty();
  }

  @Override
  public void beginAtomicTransaction(LockID lock, LockLevel level) throws AbortedOperationException {
    manager.beginAtomicTransaction(lock, level);
  }

  @Override
  public void commitAtomicTransaction(LockID lock, LockLevel level) throws AbortedOperationException {
    manager.commitAtomicTransaction(lock, level);
  }

  @Override
  public boolean isRejoinEnabled() {
    return rejoinEnabled;
  }

  public void init(RejoinManager rejoinManager, ClientHandshakeManager clientHandshakeManager) {
    rejoinEventsController = new RejoinLifecycleEventController(rejoinManager, clientHandshakeManager);
  }

  @Override
  public void addRejoinLifecycleListener(RejoinLifecycleListener listener) {
    rejoinEventsController.addUpperLayerListener(listener);
  }

  @Override
  public void removeRejoinLifecycleListener(RejoinLifecycleListener listener) {
    rejoinEventsController.removeUpperLayerListener(listener);
  }

  @Override
  public <T> T lookupRegisteredObjectByName(String name, Class<T> expectedType) {
    return manager.lookupRegisteredObjectByName(name, expectedType);
  }

  @Override
  public <T> T registerObjectByNameIfAbsent(String name, T object) {
    return manager.registerObjectByNameIfAbsent(name, object);
  }

  @Override
  public void logicalInvoke(final Object object, final String methodName, final Object[] params) {
    manager.logicalInvoke(object, methodName, params);
  }

  @Override
  public void waitForAllCurrentTransactionsToComplete() throws AbortedOperationException {
    manager.waitForAllCurrentTransactionsToComplete();
  }

  @Override
  public boolean isHeldByCurrentThread(Object lockID, LockLevel level) throws AbortedOperationException {
    LockID lock = manager.generateLockIdentifier(lockID);
    return manager.isLockedByCurrentThread(lock, level);
  }

  @Override
  public void beginLock(final Object lockID, final LockLevel level) throws AbortedOperationException {
    LockID lock = manager.generateLockIdentifier(lockID);
    manager.lock(lock, level);
    addContext(lockID);
  }

  @Override
  public void beginLockInterruptibly(Object lockID, LockLevel level) throws InterruptedException,
      AbortedOperationException {
    LockID lock = manager.generateLockIdentifier(lockID);
    manager.lockInterruptibly(lock, level);
    addContext(lockID);
  }

  @Override
  public boolean tryBeginLock(final Object lockID, final LockLevel level) throws AbortedOperationException {
    LockID lock = manager.generateLockIdentifier(lockID);
    boolean granted = manager.tryLock(lock, level);
    if (granted) {
      addContext(lockID);
    }
    return granted;
  }

  @Override
  public boolean tryBeginLock(final Object lockID, final LockLevel level, final long timeout, TimeUnit timeUnit)
      throws InterruptedException, AbortedOperationException {
    LockID lock = manager.generateLockIdentifier(lockID);
    boolean granted = manager.tryLock(lock, level, timeUnit.toMillis(timeout));
    if (granted) {
      addContext(lockID);
    }
    return granted;
  }

  @Override
  public void commitLock(final Object lockID, final LockLevel level) throws AbortedOperationException {
    LockID lock = manager.generateLockIdentifier(lockID);
    try {
      manager.unlock(lock, level);
    } finally {
      removeContext(lockID);
    }
  }

  @Override
  public void lockIDWait(Object lockID, long timeout, TimeUnit timeUnit) throws InterruptedException,
      AbortedOperationException {
    LockID lock = manager.generateLockIdentifier(lockID);
    manager.lockIDWait(lock, timeUnit.toMillis(timeout));
  }

  @Override
  public void lockIDNotify(Object lockID) throws AbortedOperationException {
    LockID lock = manager.generateLockIdentifier(lockID);
    manager.lockIDNotify(lock);
  }

  @Override
  public void lockIDNotifyAll(Object lockID) throws AbortedOperationException {
    LockID lock = manager.generateLockIdentifier(lockID);
    manager.lockIDNotifyAll(lock);
  }

  @Override
  public TCProperties getTCProperties() {
    return manager.getTCProperties();
  }

  @Override
  public Object lookupRoot(final String name, GroupID gid) {
    return manager.lookupRoot(name, gid);
  }

  @Override
  public Object lookupOrCreateRoot(final String name, final Object object, GroupID gid) {
    return manager.lookupOrCreateRoot(name, object, gid);
  }

  @Override
  public TCObject lookupOrCreate(final Object obj, GroupID gid) {
    return manager.lookupOrCreate(obj, gid);
  }

  @Override
  public Object lookupObject(final ObjectID id) throws AbortedOperationException {
    try {
      return manager.lookupObject(id);
    } catch (ClassNotFoundException e) {
      throw new TCClassNotFoundException(e);
    }
  }

  @Override
  public GroupID[] getGroupIDs() {
    return manager.getGroupIDs();
  }

  @Override
  public TCLogger getLogger(final String loggerName) {
    return manager.getLogger(loggerName);
  }

  @Override
  public void addTransactionCompleteListener(TransactionCompleteListener listener) {
    manager.addTransactionCompleteListener(listener);
  }

  @Override
  public MetaDataDescriptor createMetaDataDescriptor(String category) {
    return manager.createMetaDataDescriptor(category);
  }

  @Override
  public void fireOperatorEvent(EventType coreOperatorEventLevel, EventSubsystem coreEventSubsytem, String eventMessage) {
    manager.fireOperatorEvent(coreOperatorEventLevel, coreEventSubsytem, eventMessage);
  }

  @Override
  public DsoNode getCurrentNode() {
    return manager.getDsoCluster().getCurrentNode();
  }

  @Override
  public DsoCluster getDsoCluster() {
    return manager.getDsoCluster();
  }

  @Override
  public void registerBeforeShutdownHook(Runnable hook) {
    manager.registerBeforeShutdownHook(hook);
  }

  @Override
  public String getUUID() {
    return manager.getUUID();
  }

  @Override
  public SearchQueryResults executeQuery(String cachename, List queryStack, boolean includeKeys, boolean includeValues,
                                         Set<String> attributeSet, List<NVPair> sortAttributes,
                                         List<NVPair> aggregators, int maxResults, int batchSize, boolean waitForTxn)
      throws AbortedOperationException {
    return manager.executeQuery(cachename, queryStack, includeKeys, includeValues, attributeSet, sortAttributes,
        aggregators, maxResults, batchSize, waitForTxn);
  }

  @Override
  public SearchQueryResults executeQuery(String cachename, List queryStack, Set<String> attributeSet,
                                         Set<String> groupByAttributes, List<NVPair> sortAttributes,
                                         List<NVPair> aggregators, int maxResults, int batchSize, boolean waitForTxn)
      throws AbortedOperationException {
    return manager.executeQuery(cachename, queryStack, attributeSet, groupByAttributes, sortAttributes, aggregators,
                                maxResults, batchSize, waitForTxn);
  }

  @Override
  public void preFetchObject(final ObjectID id) throws AbortedOperationException {
    manager.preFetchObject(id);
  }

  @Override
  public void verifyCapability(String capability) {
    manager.verifyCapability(capability);
  }

  @Override
  public AbortableOperationManager getAbortableOperationManager() {
    return manager.getAbortableOperationManager();
  }

  @Override
  public void throttlePutIfNecessary(final ObjectID object) throws AbortedOperationException {
    ManagerUtil.throttlePutIfNecessary(object);
  }

  @Override
  public boolean isLockedBeforeRejoin() {
    return false;
  }

  @Override
  public void registerServerEventListener(final ServerEventDestination destination, final Set<ServerEventType> listenTo) {
    Preconditions.checkArgument(listenTo != null && !listenTo.isEmpty());
    manager.registerServerEventListener(destination, listenTo);
  }

  @Override
  public void registerServerEventListener(final ServerEventDestination destination, final ServerEventType... listenTo) {
    Preconditions.checkNotNull(listenTo);
    registerServerEventListener(destination, EnumSet.copyOf(Arrays.asList(listenTo)));
  }

  @Override
  public void unregisterServerEventListener(final ServerEventDestination destination) {
    manager.unregisterServerEventListener(destination);
  }

  @Override
  public int getRejoinCount() {
    return manager.getRejoinCount();
  }

  public boolean isRejoinInProgress() {
    return manager.isRejoinInProgress();
  }
}
