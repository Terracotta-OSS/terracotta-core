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
import com.tc.object.locks.ThreadID;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.tx.TransactionCompleteListener;
import com.tc.operatorevent.TerracottaOperatorEvent.EventSubsystem;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;
import com.tc.platform.PlatformService;
import com.tc.platform.rejoin.RejoinLifecycleListener;
import com.tc.properties.TCProperties;
import com.tc.search.SearchQueryResults;
import com.tc.util.concurrent.ConcurrentHashMap;
import com.tc.util.runtime.ThreadIDManager;
import com.tc.util.runtime.ThreadIDManagerImpl;
import com.tc.util.runtime.ThreadIDMapImpl;
import com.tcclient.cluster.DsoNode;
import com.terracottatech.search.NVPair;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RejoinAwarePlatformService implements PlatformService, RejoinLifecycleListener {
  // private static final TCLogger LOGGER = TCLogging
  // .getLogger(RejoinAwarePlatformService.class);
  private final PlatformService delegate;
  private final ThreadIDManager threadIDManager = new ThreadIDManagerImpl(new ThreadIDMapImpl());
  private final ConcurrentHashMap<ThreadLockContext, AtomicInteger> current         = new ConcurrentHashMap<ThreadLockContext, AtomicInteger>();
  private final ConcurrentHashMap<ThreadLockContext, AtomicInteger> old             = new ConcurrentHashMap<ThreadLockContext, AtomicInteger>();

  private static class ThreadLockContext {
    private final Object   lockId;
    private final ThreadID threadId;

    @Override
    public String toString() {
      return "lockId=" + lockId + ", threadId=" + threadId;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((lockId == null) ? 0 : lockId.hashCode());
      result = prime * result + ((threadId == null) ? 0 : threadId.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (!(obj instanceof ThreadLockContext)) return false;
      ThreadLockContext other = (ThreadLockContext) obj;
      if (lockId == null) {
        if (other.lockId != null) return false;
      } else if (!lockId.equals(other.lockId)) return false;
      if (threadId == null) {
        if (other.threadId != null) return false;
      } else if (!threadId.equals(other.threadId)) return false;
      return true;
    }

    public ThreadLockContext(Object lockId, ThreadID threadId) {
      this.lockId = lockId;
      this.threadId = threadId;
    }
  }

  // private String getCallTrace() {
  // Throwable t = new Throwable();
  // StringWriter sw = new StringWriter();
  // PrintWriter pw = new PrintWriter(sw);
  // t.printStackTrace(pw);
  // return sw.toString();
  // }

  private void addContext(Object lockId) {
    ThreadLockContext context = createContext(lockId);
    AtomicInteger count = current.putIfAbsent(context, new AtomicInteger(1));
    if (count != null) {
      count.incrementAndGet();
    } else {
      old.remove(context);
    }
  }

  private void removeContext(Object lockId) {
    ThreadLockContext context = createContext(lockId);
    AtomicInteger count = current.get(context);
    if (count != null && count.decrementAndGet() < 0) { throw new IllegalStateException("current removed more times "
                                                                                        + context); }
  }

  private ThreadLockContext createContext(Object lockId) {
    ThreadID thredId = threadIDManager.getThreadID();
    ThreadLockContext context = new ThreadLockContext(lockId, thredId);
    return context;
  }

  private boolean isLockedBeforeRejoin(Object lockId) {
    ThreadLockContext context = createContext(lockId);
    AtomicInteger count = old.get(context);
    if (count != null) {
      int value = count.decrementAndGet();
      if (value >= 0) {
        if (value == 0) {
          old.remove(context);
        }
        return true;
      } else {
        throw new IllegalStateException("old removed more times " + context);
      }
    }
    return false;
  }

  @Override
  public void onRejoinStart() {
    old.putAll(current);
    current.clear();
  }

  @Override
  public void onRejoinComplete() {
    //
  }

  public RejoinAwarePlatformService(PlatformService delegate) {
    this.delegate = delegate;
    addRejoinLifecycleListener(this);
  }

  @Override
  public boolean isRejoinEnabled() {
    return delegate.isRejoinEnabled();
  }

  @Override
  public <T> T lookupRegisteredObjectByName(String name, Class<T> expectedType) {
    try {
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
    try {
      delegate.logicalInvoke(object, methodName, params);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public void waitForAllCurrentTransactionsToComplete() throws AbortedOperationException {
    try {
      delegate.waitForAllCurrentTransactionsToComplete();
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public boolean isHeldByCurrentThread(Object lockID, LockLevel level) throws AbortedOperationException {
    try {
      return delegate.isHeldByCurrentThread(lockID, level);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public void beginLock(Object lockID, LockLevel level) throws AbortedOperationException {
    try {
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
      delegate.beginLockInterruptibly(lockID, level);
      addContext(lockID);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public void commitLock(Object lockID, LockLevel level) throws AbortedOperationException {
    try {
      delegate.commitLock(lockID, level);
      removeContext(lockID);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    } catch (IllegalMonitorStateException e) {
      if (isLockedBeforeRejoin(lockID)) { throw new RejoinException(e); }
      throw e;
    }
  }

  @Override
  public boolean tryBeginLock(Object lockID, LockLevel level) throws AbortedOperationException {
    try {
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
    try {
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
    try {
      delegate.lockIDWait(lockID, timeout, timeUnit);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public void lockIDNotify(Object lockID) throws AbortedOperationException {
    try {
      delegate.lockIDNotify(lockID);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public void lockIDNotifyAll(Object lockID) throws AbortedOperationException {
    try {
      delegate.lockIDNotifyAll(lockID);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public TCProperties getTCProperties() {
    try {
      return delegate.getTCProperties();
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public Object lookupRoot(String name, GroupID gid) {
    try {
      return delegate.lookupRoot(name, gid);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public Object lookupOrCreateRoot(String name, Object object, GroupID gid) {
    try {
      return delegate.lookupOrCreateRoot(name, object, gid);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public TCObject lookupOrCreate(Object obj, GroupID gid) {
    try {
      return delegate.lookupOrCreate(obj, gid);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public Object lookupObject(ObjectID id) throws AbortedOperationException {
    try {
      return delegate.lookupObject(id);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public GroupID[] getGroupIDs() {
    try {
      return delegate.getGroupIDs();
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public TCLogger getLogger(String loggerName) {
    try {
      return delegate.getLogger(loggerName);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public void addTransactionCompleteListener(TransactionCompleteListener listener) {
    try {
      delegate.addTransactionCompleteListener(listener);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public MetaDataDescriptor createMetaDataDescriptor(String category) {
    try {
      return delegate.createMetaDataDescriptor(category);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public void fireOperatorEvent(EventType coreOperatorEventLevel, EventSubsystem coreEventSubsytem, String eventMessage) {
    try {
      delegate.fireOperatorEvent(coreOperatorEventLevel, coreEventSubsytem, eventMessage);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public DsoNode getCurrentNode() {
    try {
      return delegate.getCurrentNode();
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public DsoCluster getDsoCluster() {
    try {
      return delegate.getDsoCluster();
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public void registerBeforeShutdownHook(Runnable hook) {
    try {
      delegate.registerBeforeShutdownHook(hook);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public String getUUID() {
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
    try {
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
    try {
      return delegate.executeQuery(cachename, queryStack, attributeSet, groupByAttributes, sortAttributes, aggregators,
                                   maxResults, batchSize, waitForTxn);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public void preFetchObject(ObjectID id) throws AbortedOperationException {
    try {
      delegate.preFetchObject(id);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public void verifyCapability(String capability) {
    try {
      delegate.verifyCapability(capability);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public AbortableOperationManager getAbortableOperationManager() {
    try {
      return delegate.getAbortableOperationManager();
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

}
