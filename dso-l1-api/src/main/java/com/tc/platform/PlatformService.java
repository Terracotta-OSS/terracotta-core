/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.platform;

import com.tc.abortable.AbortableOperationManager;
import com.tc.abortable.AbortedOperationException;
import com.tc.cluster.DsoCluster;
import com.tc.logging.TCLogger;
import com.tc.management.TCManagementEvent;
import com.tc.net.GroupID;
import com.tc.object.LogicalOperation;
import com.tc.object.ObjectID;
import com.tc.object.ServerEventDestination;
import com.tc.object.TCObject;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockLevel;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.tx.TransactionCompleteListener;
import com.tc.operatorevent.TerracottaOperatorEvent.EventLevel;
import com.tc.operatorevent.TerracottaOperatorEvent.EventSubsystem;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;
import com.tc.platform.rejoin.RejoinLifecycleListener;
import com.tc.properties.TCProperties;
import com.tc.search.SearchQueryResults;
import com.tc.search.SearchRequestID;
import com.tc.server.ServerEventType;
import com.tc.util.concurrent.TaskRunner;
import com.tcclient.cluster.DsoNode;
import com.terracottatech.search.NVPair;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public interface PlatformService {

  <T> T lookupRegisteredObjectByName(String name, Class<T> expectedType);

  <T> T registerObjectByNameIfAbsent(String name, T object);

  void logicalInvoke(final Object object, final LogicalOperation method, final Object[] params);

  void waitForAllCurrentTransactionsToComplete() throws AbortedOperationException;

  boolean isHeldByCurrentThread(Object lockID, LockLevel level) throws AbortedOperationException;

  void beginLock(final Object lockID, final LockLevel level) throws AbortedOperationException;

  void beginLockInterruptibly(Object obj, LockLevel level) throws InterruptedException, AbortedOperationException;

  void commitLock(final Object lockID, final LockLevel level) throws AbortedOperationException;

  boolean tryBeginLock(Object lockID, LockLevel level) throws AbortedOperationException;

  public boolean tryBeginLock(final Object lockID, final LockLevel level, final long timeout, TimeUnit timeUnit)
      throws InterruptedException, AbortedOperationException;

  void lockIDWait(Object lockID, long timeout, TimeUnit timeUnit) throws InterruptedException,
      AbortedOperationException;

  void lockIDNotify(Object lockID) throws AbortedOperationException;

  void lockIDNotifyAll(Object lockID) throws AbortedOperationException;

  TCProperties getTCProperties();

  Object lookupRoot(final String name, GroupID gid);

  Object lookupOrCreateRoot(final String name, final Object object, GroupID gid);

  TCObject lookupOrCreate(final Object obj, GroupID gid);

  Object lookupObject(final ObjectID id) throws AbortedOperationException;

  GroupID[] getGroupIDs();

  TCLogger getLogger(final String loggerName);

  void addTransactionCompleteListener(TransactionCompleteListener listener);

  MetaDataDescriptor createMetaDataDescriptor(String category);

  void fireOperatorEvent(EventLevel coreOperatorEventLevel, EventSubsystem coreEventSubsytem, EventType eventType,
                         String eventMessage);

  DsoNode getCurrentNode();

  DsoCluster getDsoCluster();

  void registerBeforeShutdownHook(Runnable hook);

  void unregisterBeforeShutdownHook(Runnable hook);

  String getUUID();

  SearchQueryResults executeQuery(String cachename, List queryStack, boolean includeKeys, boolean includeValues,
                                  Set<String> attributeSet, List<NVPair> sortAttributes, List<NVPair> aggregators,
                                  int maxResults, int batchSize, int resultPageSize, boolean waitForTxn, SearchRequestID queryId)
      throws AbortedOperationException;

  SearchQueryResults executeQuery(String cachename, List queryStack, Set<String> attributeSet,
                                  Set<String> groupByAttributes, List<NVPair> sortAttributes, List<NVPair> aggregators,
                                  int maxResults, int batchSize, boolean waitForTxn, SearchRequestID queryId) throws AbortedOperationException;

  void preFetchObject(final ObjectID id) throws AbortedOperationException;

  void verifyCapability(String capability);

  AbortableOperationManager getAbortableOperationManager();

  void addRejoinLifecycleListener(RejoinLifecycleListener listener);

  void removeRejoinLifecycleListener(RejoinLifecycleListener listener);

  boolean isRejoinEnabled();

  void throttlePutIfNecessary(ObjectID object) throws AbortedOperationException;

  boolean isLockedBeforeRejoin();

  void beginAtomicTransaction(LockID lockID, LockLevel level) throws AbortedOperationException;

  void commitAtomicTransaction(LockID lockID, LockLevel level) throws AbortedOperationException;

  boolean isExplicitlyLocked();

  void registerServerEventListener(ServerEventDestination destination, Set<ServerEventType> listenTo);

  void registerServerEventListener(ServerEventDestination destination, ServerEventType... listenTo);

  void unregisterServerEventListener(ServerEventDestination destination, final Set<ServerEventType> listenTo);

  void unregisterServerEventListener(ServerEventDestination destination, ServerEventType... listenTo);

  int getRejoinCount();

  boolean isRejoinInProgress();

  TaskRunner getTaskRunner();

  boolean isExplicitlyLocked(Object lockID, LockLevel level);

  boolean isLockedBeforeRejoin(Object lockID, LockLevel level);

  long getClientId();

  Object registerManagementService(Object service, ExecutorService executorService);

  void unregisterManagementService(Object serviceID);

  void sendEvent(TCManagementEvent event);
}
