/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.platform;

import com.tc.abortable.AbortableOperationManager;
import com.tc.abortable.AbortedOperationException;
import com.tc.cluster.DsoCluster;
import com.tc.logging.TCLogger;
import com.tc.net.GroupID;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.bytecode.Manager;
import com.tc.object.locks.LockLevel;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.tx.TransactionCompleteListener;
import com.tc.operatorevent.TerracottaOperatorEvent.EventSubsystem;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;
import com.tc.platform.InterceptedProxy.Interceptor;
import com.tc.platform.rejoin.RejoinLifecycleListener;
import com.tc.platform.rejoin.RejoinManager;
import com.tc.properties.TCProperties;
import com.tc.search.SearchQueryResults;
import com.tcclient.cluster.DsoNode;
import com.terracottatech.search.NVPair;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RejoinAwarePlatformService implements PlatformService {

  private final Status                         status                          = new Status();
  private final RejoinLifecycleEventController rejoinLifecycleEventsController = new RejoinLifecycleEventController(
                                                                                                                    status);
  private final PlatformService                interceptedProxy;

  public RejoinAwarePlatformService(Manager manager, RejoinManager rejoinManager) {
    final Interceptor statusCheckInterceptor = new StatusCheckInterceptor(status);
    final PlatformService actualDelegate = new PlatformServiceImpl(manager, rejoinManager);
    this.interceptedProxy = InterceptedProxy.createInterceptedProxy(actualDelegate, PlatformService.class,
                                                                    statusCheckInterceptor);
    actualDelegate.addRejoinLifecycleListener(rejoinLifecycleEventsController);

  }

  @Override
  public void addRejoinLifecycleListener(RejoinLifecycleListener listener) {
    // this method is not intercepted
    rejoinLifecycleEventsController.addUpperLayerListener(listener);
  }

  @Override
  public void removeRejoinLifecycleListener(RejoinLifecycleListener listener) {
    // this method is not intercepted
    rejoinLifecycleEventsController.removeUpperLayerListener(listener);
  }

  // /// all methods below pass through the interceptor

  @Override
  public <T> T lookupRegisteredObjectByName(String name, Class<T> expectedType) {
    return interceptedProxy.lookupRegisteredObjectByName(name, expectedType);
  }

  @Override
  public <T> T registerObjectByNameIfAbsent(String name, T object) {
    return interceptedProxy.registerObjectByNameIfAbsent(name, object);
  }

  @Override
  public void logicalInvoke(Object object, String methodName, Object[] params) {
    interceptedProxy.logicalInvoke(object, methodName, params);
  }

  @Override
  public void waitForAllCurrentTransactionsToComplete() throws AbortedOperationException {
    interceptedProxy.waitForAllCurrentTransactionsToComplete();
  }

  @Override
  public boolean isHeldByCurrentThread(Object lockID, LockLevel level) {
    return interceptedProxy.isHeldByCurrentThread(lockID, level);
  }

  @Override
  public void beginLock(Object lockID, LockLevel level) throws AbortedOperationException {
    interceptedProxy.beginLock(lockID, level);
  }

  @Override
  public void beginLockInterruptibly(Object obj, LockLevel level) throws InterruptedException,
      AbortedOperationException {
    interceptedProxy.beginLockInterruptibly(obj, level);
  }

  @Override
  public void commitLock(Object lockID, LockLevel level) throws AbortedOperationException {
    interceptedProxy.commitLock(lockID, level);
  }

  @Override
  public boolean tryBeginLock(Object lockID, LockLevel level) throws AbortedOperationException {
    return interceptedProxy.tryBeginLock(lockID, level);
  }

  @Override
  public boolean tryBeginLock(Object lockID, LockLevel level, long timeout, TimeUnit timeUnit)
      throws InterruptedException, AbortedOperationException {
    return interceptedProxy.tryBeginLock(lockID, level, timeout, timeUnit);
  }

  @Override
  public void lockIDWait(Object lockID, long timeout, TimeUnit timeUnit) throws InterruptedException,
      AbortedOperationException {
    interceptedProxy.lockIDWait(lockID, timeout, timeUnit);
  }

  @Override
  public void lockIDNotify(Object lockID) {
    interceptedProxy.lockIDNotify(lockID);
  }

  @Override
  public void lockIDNotifyAll(Object lockID) {
    interceptedProxy.lockIDNotifyAll(lockID);
  }

  @Override
  public TCProperties getTCProperties() {
    return interceptedProxy.getTCProperties();
  }

  @Override
  public Object lookupRoot(String name, GroupID gid) {
    return interceptedProxy.lookupRoot(name, gid);
  }

  @Override
  public Object lookupOrCreateRoot(String name, Object object, GroupID gid) {
    return interceptedProxy.lookupOrCreateRoot(name, object, gid);
  }

  @Override
  public TCObject lookupOrCreate(Object obj, GroupID gid) {
    return interceptedProxy.lookupOrCreate(obj, gid);
  }

  @Override
  public Object lookupObject(ObjectID id) throws AbortedOperationException {
    return interceptedProxy.lookupObject(id);
  }

  @Override
  public GroupID[] getGroupIDs() {
    return interceptedProxy.getGroupIDs();
  }

  @Override
  public TCLogger getLogger(String loggerName) {
    return interceptedProxy.getLogger(loggerName);
  }

  @Override
  public void addTransactionCompleteListener(TransactionCompleteListener listener) {
    interceptedProxy.addTransactionCompleteListener(listener);
  }

  @Override
  public MetaDataDescriptor createMetaDataDescriptor(String category) {
    return interceptedProxy.createMetaDataDescriptor(category);
  }

  @Override
  public void fireOperatorEvent(EventType coreOperatorEventLevel, EventSubsystem coreEventSubsytem, String eventMessage) {
    interceptedProxy.fireOperatorEvent(coreOperatorEventLevel, coreEventSubsytem, eventMessage);
  }

  @Override
  public DsoNode getCurrentNode() {
    return interceptedProxy.getCurrentNode();
  }

  @Override
  public DsoCluster getDsoCluster() {
    return interceptedProxy.getDsoCluster();
  }

  @Override
  public void registerBeforeShutdownHook(Runnable hook) {
    interceptedProxy.registerBeforeShutdownHook(hook);
  }

  @Override
  public String getUUID() {
    return interceptedProxy.getUUID();
  }

  @Override
  public SearchQueryResults executeQuery(String cachename, List queryStack, boolean includeKeys, boolean includeValues,
                                         Set<String> attributeSet, List<NVPair> sortAttributes,
                                         List<NVPair> aggregators, int maxResults, int batchSize, boolean waitForTxn)
      throws AbortedOperationException {
    return interceptedProxy.executeQuery(cachename, queryStack, includeKeys, includeValues, attributeSet,
                                         sortAttributes, aggregators, maxResults, batchSize, waitForTxn);
  }

  @Override
  public SearchQueryResults executeQuery(String cachename, List queryStack, Set<String> attributeSet,
                                         Set<String> groupByAttributes, List<NVPair> sortAttributes,
                                         List<NVPair> aggregators, int maxResults, int batchSize, boolean waitForTxn)
      throws AbortedOperationException {
    return interceptedProxy.executeQuery(cachename, queryStack, attributeSet, groupByAttributes, sortAttributes,
                                         aggregators, maxResults, batchSize, waitForTxn);
  }

  @Override
  public void preFetchObject(ObjectID id) throws AbortedOperationException {
    interceptedProxy.preFetchObject(id);
  }

  @Override
  public void verifyCapability(String capability) {
    interceptedProxy.verifyCapability(capability);
  }

  @Override
  public AbortableOperationManager getAbortableOperationManager() {
    return interceptedProxy.getAbortableOperationManager();
  }

  private static class StatusCheckInterceptor implements Interceptor {
    private final Status status;

    public StatusCheckInterceptor(Status status) {
      this.status = status;
    }

    @Override
    public void intercept(Object proxy, Method method, Object[] args) throws Exception {
      // always check for status before proceeding
      status.waitIfRejoinInProgress();
    }
  }

  private static class Status {

    private final Object        monitor          = new Object();
    private final AtomicBoolean rejoinInProgress = new AtomicBoolean(false);

    public void markRejoinInProgress() {
      synchronized (monitor) {
        rejoinInProgress.set(true);
        monitor.notifyAll();
      }
    }

    public void markRejoinComplete() {
      synchronized (monitor) {
        rejoinInProgress.set(false);
        monitor.notifyAll();
      }
    }

    public void waitIfRejoinInProgress() throws AbortedOperationException {
      while (rejoinInProgress.get()) {
        synchronized (monitor) {
          try {
            monitor.wait();
          } catch (InterruptedException e) {
            // return with aborted exception if interrrupted during wait
            // todo: consult abortableManager here? or throw for every interrupt?
            throw new AbortedOperationException("Interrupted while waiting for rejoin to complete", e);
          }
        }
      }
    }

  }

  private static class RejoinLifecycleEventController implements RejoinLifecycleListener {

    private final Status                                       status;
    private final CopyOnWriteArraySet<RejoinLifecycleListener> upperLayerListeners = new CopyOnWriteArraySet<RejoinLifecycleListener>();

    public RejoinLifecycleEventController(Status status) {
      this.status = status;
    }

    private void addUpperLayerListener(RejoinLifecycleListener listener) {
      upperLayerListeners.add(listener);
    }

    private void removeUpperLayerListener(RejoinLifecycleListener listener) {
      upperLayerListeners.remove(listener);
    }

    @Override
    public void onRejoinStart() {
      status.markRejoinInProgress();
      for (RejoinLifecycleListener listener : upperLayerListeners) {
        listener.onRejoinStart();
      }
    }

    @Override
    public void onRejoinComplete() {
      status.markRejoinComplete();
      for (RejoinLifecycleListener listener : upperLayerListeners) {
        listener.onRejoinComplete();
      }
    }

  }
}
