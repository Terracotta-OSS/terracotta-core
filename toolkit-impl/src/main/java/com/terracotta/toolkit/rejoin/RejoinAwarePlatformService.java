/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.rejoin;

import org.terracotta.toolkit.rejoin.RejoinException;

import com.tc.abortable.AbortableOperationManager;
import com.tc.abortable.AbortedOperationException;
import com.tc.cluster.DsoCluster;
import com.tc.exception.PlatformRejoinException;
import com.tc.logging.TCLogger;
import com.tc.net.GroupID;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.locks.LockLevel;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.tx.TransactionCompleteListener;
import com.tc.operatorevent.TerracottaOperatorEvent.EventSubsystem;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;
import com.tc.platform.PlatformService;
import com.tc.platform.rejoin.RejoinLifecycleListener;
import com.tc.properties.TCProperties;
import com.tc.search.SearchQueryResults;
import com.tc.util.VicariousThreadLocal;
import com.tcclient.cluster.DsoNode;
import com.terracottatech.search.NVPair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RejoinAwarePlatformService implements PlatformService {
  // private static final TCLogger LOGGER = TCLogging
  // .getLogger(RejoinAwarePlatformService.class);
  private final PlatformService                                  delegate;
  private final RejoinStateListener                              rejoinState;
  private static final ThreadLocal<Map<Object, RejoinLockState>> lockIdToCount = new VicariousThreadLocal<Map<Object, RejoinLockState>>() {
                                                                                 @Override
                                                                                 protected HashMap<Object, RejoinLockState> initialValue() {
                                                                                   return new HashMap<Object, RejoinLockState>();
                                                                                 }
                                                                               };

  public RejoinAwarePlatformService(PlatformService delegate) {
    this.delegate = delegate;
    this.rejoinState = new RejoinStateListener();
    delegate.addRejoinLifecycleListener(rejoinState);
  }

  private void assertRejoinNotInProgress() {
    rejoinState.assertRejoinNotInProgress();
  }

  private void addContext(Object lockId) {
    Map<Object, RejoinLockState> localMap = lockIdToCount.get();
    RejoinLockState lockState = localMap.get(lockId);
    if (lockState != null) {
      lockState.incrementLockCount();
    } else {
      localMap.put(lockId, new RejoinLockState(rejoinState.getRejoinCount(), 1));
    }
  }

  private void removeContext(Object lockId) {
    Map<Object, RejoinLockState> localMap = lockIdToCount.get();
    RejoinLockState lockState = localMap.get(lockId);
    if (lockState != null) {
      lockState.decrementLockCount();
      if (lockState.getLockCount() == 0) {
        localMap.remove(lockId);
      }
    } else {
      throw new IllegalMonitorStateException("lock.unlock done more times than lock.lock");
    }
  }

  @Override
  public boolean isLockedBeforeRejoin() {
    // check whether lock was taken before rejoin
    Map<Object, RejoinLockState> localMap = lockIdToCount.get();
    if (localMap.size() > 0) {
      RejoinLockState lockState = localMap.entrySet().iterator().next().getValue();
      if (lockState.getRejoinCount() < rejoinState.getRejoinCount()) { return true; }
    }
    return false;

  }

  @Override
  public boolean isRejoinEnabled() {
    return delegate.isRejoinEnabled();
  }

  @Override
  public <T> T lookupRegisteredObjectByName(String name, Class<T> expectedType) {
    try {
      assertNotLockedBeforeRejoin();
      return delegate.lookupRegisteredObjectByName(name, expectedType);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public <T> T registerObjectByNameIfAbsent(String name, T object) {
    try {
      return delegate.registerObjectByNameIfAbsent(name, object);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public void logicalInvoke(Object object, String methodName, Object[] params) {
    assertRejoinNotInProgress();
    try {
      assertNotLockedBeforeRejoin();
      delegate.logicalInvoke(object, methodName, params);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  protected void assertNotLockedBeforeRejoin() {
    if (isLockedBeforeRejoin()) { throw new RejoinException(
                                                            "Lock is not usable anymore after rejoin has occured,Operation failed"); }
  }

  @Override
  public void waitForAllCurrentTransactionsToComplete() throws AbortedOperationException {
    assertRejoinNotInProgress();
    try {
      assertNotLockedBeforeRejoin();
      delegate.waitForAllCurrentTransactionsToComplete();
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public boolean isHeldByCurrentThread(Object lockID, LockLevel level) throws AbortedOperationException {
    assertRejoinNotInProgress();
    try {
      return delegate.isHeldByCurrentThread(lockID, level);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public void beginLock(Object lockID, LockLevel level) throws AbortedOperationException {
    assertRejoinNotInProgress();
    try {
      assertNotLockedBeforeRejoin();
      delegate.beginLock(lockID, level);
      addContext(lockID);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public void beginLockInterruptibly(Object lockID, LockLevel level) throws InterruptedException,
      AbortedOperationException {

    try {
      assertNotLockedBeforeRejoin();
      delegate.beginLockInterruptibly(lockID, level);
      addContext(lockID);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public void commitLock(Object lockID, LockLevel level) throws AbortedOperationException {
    // do not assert and throw rejoin ex when rejoin is in progress.
    try {
      assertNotLockedBeforeRejoin();
      delegate.commitLock(lockID, level);
    } catch (PlatformRejoinException e) {
      if (isLockedBeforeRejoin()) { throw new RejoinException(e); }
      throw new IllegalMonitorStateException();
    } catch (IllegalMonitorStateException e) {
      if (isLockedBeforeRejoin()) { throw new RejoinException(e); }
      throw e;
    } finally {
      removeContext(lockID);
    }
  }

  @Override
  public boolean tryBeginLock(Object lockID, LockLevel level) throws AbortedOperationException {
    assertRejoinNotInProgress();
    try {
      assertNotLockedBeforeRejoin();
      boolean granted = delegate.tryBeginLock(lockID, level);
      if (granted) {
        addContext(lockID);
      }
      return granted;
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public boolean tryBeginLock(Object lockID, LockLevel level, long timeout, TimeUnit timeUnit)
      throws InterruptedException, AbortedOperationException {
    assertRejoinNotInProgress();
    try {
      assertNotLockedBeforeRejoin();
      boolean granted = delegate.tryBeginLock(lockID, level, timeout, timeUnit);
      if (granted) {
        addContext(lockID);
      }
      return granted;
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public void lockIDWait(Object lockID, long timeout, TimeUnit timeUnit) throws InterruptedException,
      AbortedOperationException {
    assertRejoinNotInProgress();
    try {
      assertNotLockedBeforeRejoin();
      delegate.lockIDWait(lockID, timeout, timeUnit);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public void lockIDNotify(Object lockID) throws AbortedOperationException {
    assertRejoinNotInProgress();
    try {
      assertNotLockedBeforeRejoin();
      delegate.lockIDNotify(lockID);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public void lockIDNotifyAll(Object lockID) throws AbortedOperationException {
    assertRejoinNotInProgress();
    try {
      assertNotLockedBeforeRejoin();
      delegate.lockIDNotifyAll(lockID);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public TCProperties getTCProperties() {
    assertRejoinNotInProgress();
    try {
      return delegate.getTCProperties();
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public Object lookupRoot(String name, GroupID gid) {
    assertRejoinNotInProgress();
    try {
      assertNotLockedBeforeRejoin();
      return delegate.lookupRoot(name, gid);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public Object lookupOrCreateRoot(String name, Object object, GroupID gid) {
    assertRejoinNotInProgress();
    try {
      assertNotLockedBeforeRejoin();
      return delegate.lookupOrCreateRoot(name, object, gid);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public TCObject lookupOrCreate(Object obj, GroupID gid) {
    assertRejoinNotInProgress();
    try {
      assertNotLockedBeforeRejoin();
      return delegate.lookupOrCreate(obj, gid);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public Object lookupObject(ObjectID id) throws AbortedOperationException {
    assertRejoinNotInProgress();
    try {
      assertNotLockedBeforeRejoin();
      return delegate.lookupObject(id);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public GroupID[] getGroupIDs() {
    assertRejoinNotInProgress();
    try {
      assertNotLockedBeforeRejoin();
      return delegate.getGroupIDs();
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public TCLogger getLogger(String loggerName) {
    assertRejoinNotInProgress();
    try {
      return delegate.getLogger(loggerName);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public void addTransactionCompleteListener(TransactionCompleteListener listener) {
    assertRejoinNotInProgress();
    try {
      delegate.addTransactionCompleteListener(listener);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public MetaDataDescriptor createMetaDataDescriptor(String category) {
    assertRejoinNotInProgress();
    try {
      return delegate.createMetaDataDescriptor(category);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public void fireOperatorEvent(EventType coreOperatorEventLevel, EventSubsystem coreEventSubsytem, String eventMessage) {
    assertRejoinNotInProgress();
    try {
      delegate.fireOperatorEvent(coreOperatorEventLevel, coreEventSubsytem, eventMessage);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public DsoNode getCurrentNode() {
    assertRejoinNotInProgress();
    try {
      return delegate.getCurrentNode();
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public DsoCluster getDsoCluster() {
    assertRejoinNotInProgress();
    try {
      return delegate.getDsoCluster();
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public void registerBeforeShutdownHook(Runnable hook) {
    assertRejoinNotInProgress();
    try {
      delegate.registerBeforeShutdownHook(hook);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public String getUUID() {
    assertRejoinNotInProgress();
    try {
      return delegate.getUUID();
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public SearchQueryResults executeQuery(String cachename, List queryStack, boolean includeKeys, boolean includeValues,
                                         Set<String> attributeSet, List<NVPair> sortAttributes,
                                         List<NVPair> aggregators, int maxResults, int batchSize, boolean waitForTxn)
      throws AbortedOperationException {
    assertRejoinNotInProgress();
    try {
      assertNotLockedBeforeRejoin();
      return delegate.executeQuery(cachename, queryStack, includeKeys, includeValues, attributeSet, sortAttributes,
                                   aggregators, maxResults, batchSize, waitForTxn);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public SearchQueryResults executeQuery(String cachename, List queryStack, Set<String> attributeSet,
                                         Set<String> groupByAttributes, List<NVPair> sortAttributes,
                                         List<NVPair> aggregators, int maxResults, int batchSize, boolean waitForTxn)
      throws AbortedOperationException {
    assertRejoinNotInProgress();
    try {
      assertNotLockedBeforeRejoin();
      return delegate.executeQuery(cachename, queryStack, attributeSet, groupByAttributes, sortAttributes, aggregators,
                                   maxResults, batchSize, waitForTxn);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public void preFetchObject(ObjectID id) throws AbortedOperationException {
    assertRejoinNotInProgress();
    try {
      delegate.preFetchObject(id);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public void verifyCapability(String capability) {
    assertRejoinNotInProgress();
    try {
      delegate.verifyCapability(capability);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public AbortableOperationManager getAbortableOperationManager() {
    assertRejoinNotInProgress();
    try {
      return delegate.getAbortableOperationManager();
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public void throttlePutIfNecessary(final ObjectID object) throws AbortedOperationException {
    assertRejoinNotInProgress();
    try {
      assertNotLockedBeforeRejoin();
      delegate.throttlePutIfNecessary(object);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public void addRejoinLifecycleListener(RejoinLifecycleListener listener) {
    try {
      delegate.addRejoinLifecycleListener(listener);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public void removeRejoinLifecycleListener(RejoinLifecycleListener listener) {
    try {
      delegate.removeRejoinLifecycleListener(listener);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  private static class RejoinStateListener implements RejoinLifecycleListener {
    private final AtomicBoolean rejoinInProgress = new AtomicBoolean(false);
    private final AtomicInteger rejoinCount      = new AtomicInteger();

    @Override
    public void onRejoinStart() {
      rejoinInProgress.set(true);
      rejoinCount.incrementAndGet();
    }

    @Override
    public void onRejoinComplete() {
      rejoinInProgress.set(false);
    }

    public void assertRejoinNotInProgress() throws RejoinException {
      if (rejoinInProgress.get()) throw new RejoinException("Rejoin is in progress");
    }

    public int getRejoinCount() {
      return rejoinCount.get();
    }

  }

  private static class RejoinLockState {
    private final int rejoinCount;
    private int       lockCount;

    public RejoinLockState(int rejoinCount, int lockCount) {
      super();
      this.rejoinCount = rejoinCount;
      this.lockCount = lockCount;
    }

    public int getRejoinCount() {
      return rejoinCount;
    }

    public int getLockCount() {
      return lockCount;
    }

    public void incrementLockCount() {
      this.lockCount++;
    }

    public void decrementLockCount() {
      this.lockCount--;
    }
  }

}
