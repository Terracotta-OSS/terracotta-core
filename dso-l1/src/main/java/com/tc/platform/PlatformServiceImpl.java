/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.platform;

import com.google.common.base.Preconditions;
import com.tc.abortable.AbortableOperationManager;
import com.tc.abortable.AbortedOperationException;
import com.tc.cluster.DsoCluster;
import com.tc.exception.TCClassNotFoundException;
import com.tc.license.LicenseManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.TCManagementEvent;
import com.tc.net.GroupID;
import com.tc.object.ClientObjectManager;
import com.tc.object.ClientShutdownManager;
import com.tc.object.DistributedObjectClient;
import com.tc.object.LogicalOperation;
import com.tc.object.ObjectID;
import com.tc.object.RemoteSearchRequestManager;
import com.tc.object.ServerEventDestination;
import com.tc.object.ServerEventListenerManager;
import com.tc.object.TCObject;
import com.tc.object.bytecode.Manageable;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.locks.ClientLockManager;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockIdFactory;
import com.tc.object.locks.LockLevel;
import com.tc.object.locks.UnclusteredLockID;
import com.tc.object.management.ServiceID;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.metadata.MetaDataDescriptorImpl;
import com.tc.object.tx.ClientTransaction;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.object.tx.OnCommitCallable;
import com.tc.object.tx.TransactionCompleteListener;
import com.tc.object.tx.UnlockedSharedObjectException;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEvent.EventLevel;
import com.tc.operatorevent.TerracottaOperatorEvent.EventSubsystem;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;
import com.tc.operatorevent.TerracottaOperatorEventImpl;
import com.tc.operatorevent.TerracottaOperatorEventLogging;
import com.tc.platform.rejoin.RejoinLifecycleListener;
import com.tc.platform.rejoin.RejoinManagerInternal;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.search.SearchQueryResults;
import com.tc.search.SearchRequestID;
import com.tc.server.ServerEventType;
import com.tc.util.UUID;
import com.tc.util.Util;
import com.tc.util.VicariousThreadLocal;
import com.tc.util.concurrent.TaskRunner;
import com.tcclient.cluster.DsoClusterInternal;
import com.tcclient.cluster.DsoNode;
import com.terracottatech.search.NVPair;
import com.terracottatech.search.SearchBuilder.Search;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class PlatformServiceImpl implements PlatformService {

  private static final int                                 BASE_COUNT        = 1;
  private static final ThreadLocal<Map<LockInfo, Integer>> lockIdToCount     = new VicariousThreadLocal<Map<LockInfo, Integer>>() {
                                                                               @Override
                                                                               protected Map<LockInfo, Integer> initialValue() {
                                                                                 return new HashMap<LockInfo, Integer>();
                                                                               }
                                                                             };

  private final ClientObjectManager                        objectManager;
  private final ClientShutdownManager                      shutdownManager;
  private final ClientTransactionManager                   txManager;
  private final ClientLockManager                          lockManager;
  private final RemoteSearchRequestManager                 searchRequestManager;
  private final DistributedObjectClient                    client;
  private final LockIdFactory                              lockIdFactory;
  private final DsoClusterInternal                         dsoCluster;
  private final AbortableOperationManager                  abortableOperationManager;
  private final UUID                                       uuid;
  private final ConcurrentHashMap<String, Object>          registeredObjects = new ConcurrentHashMap<String, Object>();
  private final ServerEventListenerManager                 serverEventListenerManager;
  private final RejoinManagerInternal                      rejoinManager;
  private final TaskRunner                                 taskRunner;
  private final RejoinLifecycleEventController             rejoinEventsController;
  private final TCLogger                                   logger            = TCLogging
                                                                                 .getLogger(PlatformService.class);

  public PlatformServiceImpl(ClientObjectManager clientObjectManager,
                             ClientTransactionManager clientTransactionManager,
                             ClientShutdownManager clientShutdownManager, ClientLockManager clientLockManager,
                             RemoteSearchRequestManager remoteSearchRequestManager,
                             DistributedObjectClient distributedObjectClient, LockIdFactory lockIdFactory,
                             DsoClusterInternal dsoCluster, AbortableOperationManager abortableOperationManager,
                             UUID uuid, ServerEventListenerManager serverEventListenerManager,
                             RejoinManagerInternal rejoinManager, TaskRunner taskRunner,
                             ClientHandshakeManager clientHandshakeManager) {
    this.objectManager = clientObjectManager;
    this.txManager = clientTransactionManager;
    this.shutdownManager = clientShutdownManager;
    this.lockManager = clientLockManager;
    this.searchRequestManager = remoteSearchRequestManager;
    this.client = distributedObjectClient;
    this.lockIdFactory = lockIdFactory;
    this.dsoCluster = dsoCluster;
    this.abortableOperationManager = abortableOperationManager;
    this.uuid = uuid;
    this.serverEventListenerManager = serverEventListenerManager;
    this.rejoinManager = rejoinManager;
    this.taskRunner = taskRunner;
    this.rejoinEventsController = new RejoinLifecycleEventController(rejoinManager, clientHandshakeManager);
  }

  private void addContext(LockInfo lockInfo) {
    Map<LockInfo, Integer> threadLocal = lockIdToCount.get();
    Integer count = threadLocal.get(lockInfo);
    Integer lockCount = count != null ? new Integer(count.intValue() + BASE_COUNT) : new Integer(BASE_COUNT);
    threadLocal.put(lockInfo, lockCount);
  }

  private void removeContext(LockInfo lockInfo) {
    Map<LockInfo, Integer> threadLocal = lockIdToCount.get();
    Integer count = threadLocal.get(lockInfo);
    if (count != null) {
      if (count.intValue() == BASE_COUNT) {
        threadLocal.remove(lockInfo);
      } else {
        threadLocal.put(lockInfo, new Integer(count.intValue() - BASE_COUNT));
      }
    }
  }

  @Override
  public boolean isExplicitlyLocked() {
    return !lockIdToCount.get().isEmpty();
  }

  @Override
  public void beginAtomicTransaction(LockID lock, LockLevel level) throws AbortedOperationException {
    this.lockManager.lock(lock, level);
    txManagerBeginUnlockOnException(lock, level, true);
  }

  @Override
  public void commitAtomicTransaction(LockID lock, LockLevel level) throws AbortedOperationException {
    try {
      this.txManager.commit(lock, level, true, null);
    } finally {
      lockManager.unlock(lock, level);
    }
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
    return expectedType.cast(registeredObjects.get(name));
  }

  @Override
  public <T> T registerObjectByNameIfAbsent(String name, T object) {
    Object old = registeredObjects.putIfAbsent(name, object);
    if (old != null) {
      return (T) old;
    } else {
      return object;
    }
  }

  @Override
  public void logicalInvoke(final Object object, final LogicalOperation method, final Object[] params) {
    final Manageable m = (Manageable) object;
    if (m.__tc_managed() != null) {
      final TCObject tco = lookupExistingOrNull(object);

      try {
        if (tco != null) {
          if (LogicalOperation.ADD_ALL.equals(method)) {
            logicalAddAllInvoke((Collection) params[0], tco);
          } else if (LogicalOperation.ADD_ALL_AT.equals(method)) {
            logicalAddAllAtInvoke(((Integer) params[0]), (Collection) params[1], tco);
          } else {
            tco.logicalInvoke(method, params);
          }
        }
      } catch (final Throwable t) {
        Util.printLogAndRethrowError(t, logger);
      }
    }
  }

  @Override
  public void waitForAllCurrentTransactionsToComplete() throws AbortedOperationException {
    this.txManager.waitForAllCurrentTransactionsToComplete();
  }

  @Override
  public boolean isHeldByCurrentThread(Object lockID, LockLevel level) throws AbortedOperationException {
    LockID lock = generateLockIdentifier(lockID);
    return this.lockManager.isLockedByCurrentThread(lock, level);
  }

  @Override
  public void beginLock(final Object lockID, final LockLevel level) throws AbortedOperationException {
    LockID lock = generateLockIdentifier(lockID);
    if (clusteredLockingEnabled(lock)) {
      this.lockManager.lock(lock, level);
      txManagerBeginUnlockOnException(lock, level, false);
    }
    addContext(new LockInfo(lockID, level));
  }

  @Override
  public void beginLockInterruptibly(Object lockID, LockLevel level) throws InterruptedException,
      AbortedOperationException {
    LockID lock = generateLockIdentifier(lockID);
    if (clusteredLockingEnabled(lock)) {
      this.lockManager.lockInterruptibly(lock, level);
      txManagerBeginUnlockOnException(lock, level, false);
    }
    addContext(new LockInfo(lockID, level));
  }

  @Override
  public boolean tryBeginLock(final Object lockID, final LockLevel level) throws AbortedOperationException {
    LockID lock = generateLockIdentifier(lockID);

    final boolean granted;
    if (clusteredLockingEnabled(lock)) {
      if (this.lockManager.tryLock(lock, level)) {
        txManagerBeginUnlockOnException(lock, level, false);
        granted = true;
      } else {
        granted = false;
      }
    } else {
      granted = true;
    }

    if (granted) {
      addContext(new LockInfo(lockID, level));
    }

    return granted;
  }

  @Override
  public boolean tryBeginLock(final Object lockID, final LockLevel level, final long timeout, TimeUnit timeUnit)
      throws InterruptedException, AbortedOperationException {
    LockID lock = generateLockIdentifier(lockID);

    final boolean granted;

    if (clusteredLockingEnabled(lock)) {
      if (this.lockManager.tryLock(lock, level, timeUnit.toMillis(timeout))) {
        txManagerBeginUnlockOnException(lock, level, false);
        granted = true;
      } else {
        granted = false;
      }
    } else {
      granted = true;
    }

    if (granted) {
      addContext(new LockInfo(lockID, level));
    }

    return granted;
  }

  @Override
  public void commitLock(final Object lockID, final LockLevel level) throws AbortedOperationException {
    LockID lock = generateLockIdentifier(lockID);
    try {
      unlock(lock, level);
    } finally {
      removeContext(new LockInfo(lockID, level));
    }
  }

  @Override
  public void lockIDWait(Object lockID, long timeout, TimeUnit timeUnit) throws InterruptedException,
      AbortedOperationException {
    LockID lock = generateLockIdentifier(lockID);
    if (isCurrentTransactionAtomic()) {
      //
      throw new UnsupportedOperationException("Wait is not supported under an atomic transaction");
    }
    try {
      this.txManager.commit(lock, LockLevel.WRITE, false, null);
    } catch (final UnlockedSharedObjectException e) {
      throw new IllegalMonitorStateException();
    }
    try {
      this.lockManager.wait(lock, null, timeUnit.toMillis(timeout));
    } finally {
      // XXX this is questionable
      this.txManager.begin(lock, LockLevel.WRITE, false);
    }
  }

  @Override
  public void lockIDNotify(Object lockID) throws AbortedOperationException {
    LockID lock = generateLockIdentifier(lockID);
    this.txManager.notify(this.lockManager.notify(lock, null));
  }

  @Override
  public void lockIDNotifyAll(Object lockID) throws AbortedOperationException {
    LockID lock = generateLockIdentifier(lockID);
    this.txManager.notify(this.lockManager.notifyAll(lock, null));
  }

  @Override
  public TCProperties getTCProperties() {
    return TCPropertiesImpl.getProperties();
  }

  @Override
  public Object lookupRoot(final String name, GroupID gid) {
    try {
      return this.objectManager.lookupRoot(name, gid);
    } catch (final Throwable t) {
      Util.printLogAndRethrowError(t, logger);
      throw new AssertionError();
    }
  }

  @Override
  public Object lookupOrCreateRoot(final String name, final Object object, GroupID gid) {
    try {
      return this.objectManager.lookupOrCreateRoot(name, object, gid);
    } catch (final Throwable t) {
      Util.printLogAndRethrowError(t, logger);
      throw new AssertionError();
    }
  }

  @Override
  public TCObject lookupOrCreate(final Object obj, GroupID gid) {
    if (obj instanceof Manageable) {
      TCObject tco = ((Manageable) obj).__tc_managed();
      if (tco != null) { return tco; }
    }

    return this.objectManager.lookupOrCreate(obj, gid);
  }

  @Override
  public Object lookupObject(final ObjectID id) throws AbortedOperationException {
    try {
      return this.objectManager.lookupObject(id);
    } catch (ClassNotFoundException e) {
      throw new TCClassNotFoundException(e);
    }
  }

  @Override
  public GroupID[] getGroupIDs() {
    return this.client.getGroupIDs();
  }

  @Override
  public TCLogger getLogger(final String loggerName) {
    return TCLogging.getLogger(loggerName);
  }

  @Override
  public void addTransactionCompleteListener(TransactionCompleteListener listener) {
    txManager.getCurrentTransaction().addTransactionCompleteListener(listener);
  }

  @Override
  public MetaDataDescriptor createMetaDataDescriptor(String category) {
    return new MetaDataDescriptorImpl(category);
  }

  @Override
  public void fireOperatorEvent(EventLevel coreOperatorEventLevel, EventSubsystem coreEventSubsytem,
                                EventType eventType, String eventMessage) {
    TerracottaOperatorEvent opEvent = new TerracottaOperatorEventImpl(coreOperatorEventLevel, coreEventSubsytem,
                                                                      eventType, eventMessage, "");
    TerracottaOperatorEventLogging.getEventLogger().fireOperatorEvent(opEvent);
  }

  @Override
  public DsoNode getCurrentNode() {
    return dsoCluster.getCurrentNode();
  }

  @Override
  public DsoCluster getDsoCluster() {
    return this.dsoCluster;
  }

  @Override
  public void registerBeforeShutdownHook(Runnable hook) {
    this.shutdownManager.registerBeforeShutdownHook(hook);
  }

  @Override
  public void unregisterBeforeShutdownHook(Runnable hook) {
    this.shutdownManager.unregisterBeforeShutdownHook(hook);
  }

  @Override
  public String getUUID() {
    return this.uuid.toString();
  }

  @Override
  public SearchQueryResults executeQuery(String cachename, List queryStack, boolean includeKeys, boolean includeValues,
                                         Set<String> attributeSet, List<NVPair> sortAttributes,
                                         List<NVPair> aggregators, int maxResults, int batchSize, int resultPageSize,
                                         boolean waitForTxn, SearchRequestID queryId) throws AbortedOperationException {
    // Paginated queries are already transactional wrt local changes
    if (resultPageSize == Search.BATCH_SIZE_UNLIMITED && shouldWaitForTxn(waitForTxn)) {
      waitForAllCurrentTransactionsToComplete();
    }
    return searchRequestManager.query(cachename, queryStack, includeKeys, includeValues, attributeSet, sortAttributes,
                                      aggregators, maxResults, batchSize, queryId, resultPageSize);
  }

  @Override
  public SearchQueryResults executeQuery(String cachename, List queryStack, Set<String> attributeSet,
                                         Set<String> groupByAttributes, List<NVPair> sortAttributes,
                                         List<NVPair> aggregators, int maxResults, int batchSize, boolean waitForTxn,
                                         SearchRequestID queryId) throws AbortedOperationException {
    if (shouldWaitForTxn(waitForTxn)) {
      waitForAllCurrentTransactionsToComplete();
    }
    return searchRequestManager.query(cachename, queryStack, attributeSet, groupByAttributes, sortAttributes,
                                      aggregators, maxResults, batchSize, queryId);
  }

  @Override
  public void preFetchObject(final ObjectID id) throws AbortedOperationException {
    this.objectManager.preFetchObject(id);
  }

  @Override
  public void verifyCapability(String capability) {
    LicenseManager.verifyCapability(capability);
  }

  @Override
  public AbortableOperationManager getAbortableOperationManager() {
    return abortableOperationManager;
  }

  @Override
  public void throttlePutIfNecessary(final ObjectID object) throws AbortedOperationException {
    client.getRemoteResourceManager().throttleIfMutationIfNecessary(object);
  }

  @Override
  public boolean isLockedBeforeRejoin() {
    return false;
  }

  @Override
  public void registerServerEventListener(final ServerEventDestination destination, final Set<ServerEventType> listenTo) {
    Preconditions.checkNotNull(destination);
    Preconditions.checkArgument(listenTo != null && !listenTo.isEmpty());
    serverEventListenerManager.registerListener(destination, listenTo);
  }

  @Override
  public void registerServerEventListener(final ServerEventDestination destination, final ServerEventType... listenTo) {
    Preconditions.checkNotNull(destination);
    Preconditions.checkArgument(listenTo != null && listenTo.length > 0);
    registerServerEventListener(destination, EnumSet.copyOf(Arrays.asList(listenTo)));
  }

  @Override
  public void unregisterServerEventListener(final ServerEventDestination destination,
                                            final Set<ServerEventType> listenTo) {
    Preconditions.checkNotNull(destination);
    Preconditions.checkArgument(listenTo != null && !listenTo.isEmpty());
    serverEventListenerManager.unregisterListener(destination, listenTo);
  }

  @Override
  public void unregisterServerEventListener(final ServerEventDestination destination, final ServerEventType... listenTo) {
    Preconditions.checkNotNull(destination);
    Preconditions.checkArgument(listenTo != null && listenTo.length > 0);
    unregisterServerEventListener(destination, EnumSet.copyOf(Arrays.asList(listenTo)));
  }

  @Override
  public int getRejoinCount() {
    return rejoinManager.getRejoinCount();
  }

  @Override
  public boolean isRejoinInProgress() {
    return rejoinManager.isRejoinInProgress();
  }

  @Override
  public TaskRunner getTaskRunner() {
    return taskRunner;
  }

  @Override
  public boolean isExplicitlyLocked(Object lockID, LockLevel level) {
    return lockIdToCount.get().containsKey(new LockInfo(lockID, level));
  }

  @Override
  public boolean isLockedBeforeRejoin(Object lockID, LockLevel level) {
    return false;
  }

  @Override
  public long getClientId() {
    return this.client.getChannel().getClientIDProvider().getClientID().toLong();
  }

  @Override
  public Object registerManagementService(Object service, ExecutorService executorService) {
    ServiceID serviceID = ServiceID.newServiceID(service);
    client.getManagementServicesManager().registerService(serviceID, service, executorService);
    return serviceID;
  }

  @Override
  public void unregisterManagementService(Object serviceID) {
    if (!(serviceID instanceof ServiceID)) {
      //
      throw new IllegalArgumentException("serviceID object must be of class " + ServiceID.class.getName());
    }
    client.getManagementServicesManager().unregisterService((ServiceID) serviceID);
  }

  @Override
  public void sendEvent(TCManagementEvent event) {
    client.getManagementServicesManager().sendEvent(event);
  }

  @Override
  public long getLockAwardIDFor(LockID lock) {
    return lockManager.getAwardIDFor(lock);
  }

  @Override
  public boolean isLockAwardValid(LockID lock, long awardID) {
    return lockManager.isLockAwardValid(lock, awardID);
  }

  @Override
  public void pinLock(final LockID lock, long awardID) {
    this.lockManager.pinLock(lock, awardID);
  }

  @Override
  public void unpinLock(final LockID lock, long awardID) {
    this.lockManager.unpinLock(lock, awardID);
  }

  @Override
  public LockID generateLockIdentifier(final Object obj) {
    return this.lockIdFactory.generateLockIdentifier(obj);
  }

  private boolean clusteredLockingEnabled(final LockID lock) {
    return !((lock instanceof UnclusteredLockID) || this.txManager.isTransactionLoggingDisabled() || this.objectManager
        .isCreationInProgress());
  }

  private void unlock(final LockID lock, final LockLevel level) throws AbortedOperationException {
    if (clusteredLockingEnabled(lock)) {
      // LockManager Unlock callback will be called on commit of current transaction by txnManager.
      this.txManager.commit(lock, level, false, getUnlockCallback(lock, level));
    }
  }

  private OnCommitCallable getUnlockCallback(final LockID lock, final LockLevel level) {
    return new OnCommitCallable() {
      @Override
      public void call() throws AbortedOperationException {
        lockManager.unlock(lock, level);
      }
    };
  }

  private boolean isCurrentTransactionAtomic() {
    ClientTransaction transaction = txManager.getCurrentTransaction();
    return transaction != null && txManager.getCurrentTransaction().isAtomic();
  }

  private boolean shouldWaitForTxn(boolean userChoice) {
    // XXX: cache this value?
    return TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.SEARCH_QUERY_WAIT_FOR_TXNS, userChoice);
  }

  private void logicalAddAllInvoke(final Collection<?> collection, final TCObject tcobj) {
    for (Object obj : collection) {
      tcobj.logicalInvoke(LogicalOperation.ADD, new Object[] { obj });
    }
  }

  private void logicalAddAllAtInvoke(int index, final Collection<?> collection, final TCObject tcobj) {
    for (Object obj : collection) {
      tcobj.logicalInvoke(LogicalOperation.ADD_AT, new Object[] { index++, obj });
    }
  }

  private void txManagerBeginUnlockOnException(LockID lock, LockLevel level, boolean atomic)
      throws AbortedOperationException {
    try {
      this.txManager.begin(lock, level, atomic);
    } catch (Throwable t) {
      this.lockManager.unlock(lock, level);
      if (t instanceof RuntimeException) { throw (RuntimeException) t; }
      if (t instanceof Error) { throw (Error) t; }
      throw new RuntimeException(t);
    }
  }

  @Override
  public TCObject lookupExistingOrNull(final Object pojo) {
    if (pojo == null) { return null; }

    if (pojo instanceof Manageable) { return ((Manageable) pojo).__tc_managed(); }

    try {
      return this.objectManager.lookupExistingOrNull(pojo);
    } catch (final Throwable t) {
      Util.printLogAndRethrowError(t, logger);

      // shouldn't get here
      throw new AssertionError();
    }
  }

  private static class LockInfo {
    private final Object    lockId;
    private final LockLevel lockLevel;

    public LockInfo(Object lockId, LockLevel lockLevel) {
      this.lockId = lockId;
      this.lockLevel = lockLevel;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((lockId == null) ? 0 : lockId.hashCode());
      result = prime * result + ((lockLevel == null) ? 0 : lockLevel.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      LockInfo other = (LockInfo) obj;
      if (lockId == null) {
        if (other.lockId != null) return false;
      } else if (!lockId.equals(other.lockId)) return false;
      if (lockLevel != other.lockLevel) return false;
      return true;
    }
  }
}
