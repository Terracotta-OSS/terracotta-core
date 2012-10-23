/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.abortable.AbortedOperationException;
import com.tc.cluster.DsoCluster;
import com.tc.logging.TCLogger;
import com.tc.net.GroupID;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.locks.LockLevel;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.tx.TransactionCompleteListener;
import com.tc.operatorevent.TerracottaOperatorEvent.EventSubsystem;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;
import com.tc.properties.TCProperties;
import com.tc.search.SearchQueryResults;
import com.tcclient.cluster.DsoNode;
import com.terracottatech.search.NVPair;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class PlatformServiceImpl implements PlatformService {
  @Override
  public <T> T lookupRegisteredObjectByName(String name, Class<T> expectedType) {
    return ManagerUtil.lookupRegisteredObjectByName(name, expectedType);
  }

  @Override
  public <T> T registerObjectByNameIfAbsent(String name, T object) {
    return ManagerUtil.registerObjectByNameIfAbsent(name, object);
  }

  @Override
  public void logicalInvoke(final Object object, final String methodName, final Object[] params) {
    ManagerUtil.logicalInvoke(object, methodName, params);
  }

  @Override
  public void waitForAllCurrentTransactionsToComplete() {
    ManagerUtil.waitForAllCurrentTransactionsToComplete();
  }

  @Override
  public boolean isHeldByCurrentThread(Object lockID, LockLevel level) {
    return ManagerUtil.isHeldByCurrentThread(lockID, level);
  }

  @Override
  public void beginLock(final Object lockID, final LockLevel level) throws AbortedOperationException {
    ManagerUtil.beginLock(lockID, level);
  }

  @Override
  public void beginLockInterruptibly(Object obj, LockLevel level) throws InterruptedException,
      AbortedOperationException {
    ManagerUtil.beginLockInterruptibly(obj, level);
  }

  @Override
  public void commitLock(final Object lockID, final LockLevel level) throws AbortedOperationException {
    ManagerUtil.commitLock(lockID, level);
  }

  @Override
  public boolean tryBeginLock(final Object lockID, final LockLevel level) throws AbortedOperationException {
    return ManagerUtil.tryBeginLock(lockID, level);
  }

  @Override
  public boolean tryBeginLock(final Object lockID, final LockLevel level, final long timeout, TimeUnit timeUnit)
      throws InterruptedException, AbortedOperationException {
    return ManagerUtil.tryBeginLock(lockID, level, timeout, timeUnit);
  }

  @Override
  public void lockIDWait(Object lockID, long timeout, TimeUnit timeUnit) throws InterruptedException,
      AbortedOperationException {
    ManagerUtil.lockIDWait(lockID, timeout, timeUnit);
  }

  @Override
  public void lockIDNotify(Object lockID) {
    ManagerUtil.lockIDNotify(lockID);
  }

  @Override
  public void lockIDNotifyAll(Object lockID) {
    ManagerUtil.lockIDNotifyAll(lockID);
  }

  @Override
  public TCProperties getTCProperties() {
    return ManagerUtil.getTCProperties();
  }

  @Override
  public Object lookupRoot(final String name, GroupID gid) {
    return ManagerUtil.lookupRoot(name, gid);
  }

  @Override
  public Object lookupOrCreateRoot(final String name, final Object object, GroupID gid) {
    return ManagerUtil.lookupOrCreateRoot(name, object, gid);
  }

  @Override
  public TCObject lookupOrCreate(final Object obj, GroupID gid) {
    return ManagerUtil.lookupOrCreate(obj, gid);
  }

  @Override
  public Object lookupObject(final ObjectID id) {
    return ManagerUtil.lookupObject(id);
  }

  @Override
  public GroupID[] getGroupIDs() {
    return ManagerUtil.getGroupIDs();
  }

  @Override
  public TCLogger getLogger(final String loggerName) {
    return ManagerUtil.getLogger(loggerName);
  }

  @Override
  public void addTransactionCompleteListener(TransactionCompleteListener listener) {
    ManagerUtil.addTransactionCompleteListener(listener);
  }

  @Override
  public MetaDataDescriptor createMetaDataDescriptor(String category) {
    return ManagerUtil.createMetaDataDescriptor(category);
  }

  @Override
  public void fireOperatorEvent(EventType coreOperatorEventLevel, EventSubsystem coreEventSubsytem,
                                       String eventMessage) {
    ManagerUtil.fireOperatorEvent(coreOperatorEventLevel, coreEventSubsytem, eventMessage);
  }

  @Override
  public DsoNode getCurrentNode() {
    return ManagerUtil.getManager().getDsoCluster().getCurrentNode();
  }

  @Override
  public DsoCluster getDsoCluster() {
    return ManagerUtil.getManager().getDsoCluster();
  }

  @Override
  public void registerBeforeShutdownHook(Runnable hook) {
    ManagerUtil.getManager().registerBeforeShutdownHook(hook);
  }

  @Override
  public String getUUID() {
    return ManagerUtil.getUUID();
  }

  @Override
  public SearchQueryResults executeQuery(String cachename, List queryStack, boolean includeKeys,
                                                boolean includeValues, Set<String> attributeSet,
                                                List<NVPair> sortAttributes, List<NVPair> aggregators, int maxResults,
                                                int batchSize, boolean waitForTxn) {
    return ManagerUtil.executeQuery(cachename, queryStack, includeKeys, includeValues, attributeSet, sortAttributes,
                                    aggregators, maxResults, batchSize, waitForTxn);
  }

  @Override
  public SearchQueryResults executeQuery(String cachename, List queryStack, Set<String> attributeSet,
                                                Set<String> groupByAttributes, List<NVPair> sortAttributes,
                                                List<NVPair> aggregators, int maxResults, int batchSize,
                                                boolean waitForTxn) {
    return ManagerUtil.executeQuery(cachename, queryStack, attributeSet, groupByAttributes, sortAttributes,
                                    aggregators, maxResults, batchSize, waitForTxn);
  }

  @Override
  public void preFetchObject(final ObjectID id) {
    ManagerUtil.preFetchObject(id);
  }

  @Override
  public void verifyCapability(String capability) {
    ManagerUtil.verifyCapability(capability);
  }
}
