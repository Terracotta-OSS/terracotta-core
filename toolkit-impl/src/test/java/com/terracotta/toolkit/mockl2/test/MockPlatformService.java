/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.toolkit.mockl2.test;

import com.tc.abortable.AbortableOperationManager;
import com.tc.cluster.DsoCluster;
import com.tc.exception.ImplementMe;
import com.tc.logging.TCLogger;
import com.tc.management.TCManagementEvent;
import com.tc.net.GroupID;
import com.tc.object.LogicalOperation;
import com.tc.object.NullTCObject;
import com.tc.object.ObjectID;
import com.tc.object.ServerEventDestination;
import com.tc.object.TCObject;
import com.tc.object.bytecode.Manageable;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockLevel;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.tx.TransactionCompleteListener;
import com.tc.operatorevent.TerracottaOperatorEvent.EventLevel;
import com.tc.operatorevent.TerracottaOperatorEvent.EventSubsystem;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;
import com.tc.platform.PlatformService;
import com.tc.platform.rejoin.RejoinLifecycleListener;
import com.tc.properties.NullTCProperties;
import com.tc.properties.TCProperties;
import com.tc.search.SearchQueryResults;
import com.tc.search.SearchRequestID;
import com.tc.server.ServerEventType;
import com.tc.util.concurrent.ScheduledNamedTaskRunner;
import com.tc.util.concurrent.TaskRunner;
import com.tcclient.cluster.DsoNode;
import com.terracotta.toolkit.TerracottaToolkit;
import com.terracotta.toolkit.object.TCToolkitObject;
import com.terracotta.toolkit.object.ToolkitObjectStripeImpl;
import com.terracotta.toolkit.object.serialization.SerializationStrategy;
import com.terracotta.toolkit.object.serialization.SerializationStrategyImpl;
import com.terracotta.toolkit.object.serialization.SerializedMapValue;
import com.terracotta.toolkit.object.serialization.SerializerMapImpl;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;
import com.terracotta.toolkit.roots.impl.ToolkitTypeRootImpl;
import com.terracottatech.search.NVPair;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class MockPlatformService implements PlatformService {
  
  private final ConcurrentLinkedQueue<MockPlatformListener> listeners = new ConcurrentLinkedQueue<MockPlatformListener>();
  private final ScheduledNamedTaskRunner                    taskRunnner = new ScheduledNamedTaskRunner(4);
  
  public void addPlatformListener(MockPlatformListener listener) {
      listeners.add(listener);
  }

  @Override
  public void waitForAllCurrentTransactionsToComplete() {
    MockUtil.logInfo("Platform Service : wait for All Current TransactionsToComplete" );
  }

  @Override
  public void verifyCapability(String capability) {
    throw new ImplementMe();
  }

  @Override
  public void unregisterServerEventListener(ServerEventDestination destination, final Set<ServerEventType> listenTo) {
    throw new ImplementMe();
  }

  @Override
  public void unregisterServerEventListener(final ServerEventDestination destination, final ServerEventType... listenTo) {
    throw new ImplementMe();
  }

  @Override
  public boolean tryBeginLock(Object lockID, LockLevel level, long timeout, TimeUnit timeUnit) {
    return false;
  }

  @Override
  public boolean tryBeginLock(Object lockID, LockLevel level) {
    return false;
  }

  @Override
  public void throttlePutIfNecessary(ObjectID object) {
    MockUtil.logInfo("Platform Service : throttlePutIfNecessary : ObjectID : " + object);

  }

  @Override
  public void removeRejoinLifecycleListener(RejoinLifecycleListener listener) {
    throw new ImplementMe();

  }

  @Override
  public void registerServerEventListener(ServerEventDestination destination, ServerEventType... listenTo) {
    throw new ImplementMe();

  }

  @Override
  public void registerServerEventListener(ServerEventDestination destination, Set<ServerEventType> listenTo) {
    throw new ImplementMe();

  }

  @Override
  public <T> T registerObjectByNameIfAbsent(String name, T object) {
    return null;
  }

  @Override
  public void registerBeforeShutdownHook(Runnable hook) {
   MockUtil.logInfo("Register Before Shutdownhook " +hook);

  }

  @Override
  public void preFetchObject(ObjectID id) {
    throw new ImplementMe();
  }

  @Override
  public Object lookupRoot(String name, GroupID gid) {
    if(name.equalsIgnoreCase(ToolkitTypeConstants.SERIALIZER_MAP_ROOT_NAME)) {
      SerializerMapImpl serializerMapImpl = new SerializerMapImpl(this);
      serializerMapImpl.__tc_managed(NullTCObject.INSTANCE);
      return serializerMapImpl;
    }
    ToolkitTypeRootImpl t = new ToolkitTypeRootImpl(this);
    t.__tc_managed(NullTCObject.INSTANCE);
    return t;
  }

  @Override
  public <T> T lookupRegisteredObjectByName(String name, Class<T> expectedType) {
    if(expectedType.equals(SerializationStrategy.class) && name.equalsIgnoreCase(TerracottaToolkit.TOOLKIT_SERIALIZER_REGISTRATION_NAME)) {
      SerializerMapImpl serializerMapImpl = new SerializerMapImpl(this);
      serializerMapImpl.__tc_managed(NullTCObject.INSTANCE);
      return (T) new SerializationStrategyImpl(this, serializerMapImpl, getClass().getClassLoader());
    }
    return null;
  }

  @Override
  public Object lookupOrCreateRoot(String name, Object object, GroupID gid) {
      TCObject tcObject = new MockTCObject(ObjectID.NULL_ID, object);
      if(Manageable.class.isAssignableFrom(object.getClass())) {
        Manageable manageable = (Manageable) object;
        manageable.__tc_managed(tcObject);
      }
      return tcObject;
  }

  @Override
  public TCObject lookupOrCreate(Object obj, GroupID gid) {
    TCObject tcObject = new MockTCObject(ObjectID.NULL_ID, obj);
    if(SerializedMapValue.class.isAssignableFrom(obj.getClass())) {
      SerializedMapValue manageable = (SerializedMapValue) obj;
      manageable.initializeTCObject(ObjectID.NULL_ID, new MockTCClass(), false);
      manageable.__tc_managed(manageable);
    }else if(Manageable.class.isAssignableFrom(obj.getClass())) {
      Manageable manageable = (Manageable) obj;
      manageable.__tc_managed(tcObject);
    }
    if(ToolkitObjectStripeImpl.class.isAssignableFrom(obj.getClass())) {
      ToolkitObjectStripeImpl impl = (ToolkitObjectStripeImpl<TCToolkitObject>) obj;
      for(Object manObj : impl) {
        if(Manageable.class.isAssignableFrom(manObj.getClass())) {
          Manageable manageable = (Manageable) manObj;
          TCObject tcObj = new MockTCObjectServerMap();
          manageable.__tc_managed(tcObj);
        }
      }
      
      
    }
    return tcObject;
  }

  @Override
  public Object lookupObject(ObjectID id) {
    return null;
  }

  @Override
  public void logicalInvoke(Object object, LogicalOperation method, Object[] params) {
    MockUtil.logInfo("Platform Service : logicalInvoke : " + printVar(object, method, params));
    for(MockPlatformListener listener : listeners) {
      if(listener != null) {
        listener.logicalInvoke(object, method, params);
      }
    }
  }

  @Override
  public void lockIDWait(Object lockID, long timeout, TimeUnit timeUnit) {
    throw new ImplementMe();

  }

  @Override
  public void lockIDNotifyAll(Object lockID) {
    throw new ImplementMe();

  }

  @Override
  public void lockIDNotify(Object lockID) {
    throw new ImplementMe();

  }

  @Override
  public boolean isLockedBeforeRejoin() {
    return false;
  }

  @Override
  public boolean isHeldByCurrentThread(Object lockID, LockLevel level) {
    return false;
  }

  @Override
  public boolean isExplicitlyLocked() {
    return false;
  }

  @Override
  public String getUUID() {
    return null;
  }

  @Override
  public TCProperties getTCProperties() {
   return NullTCProperties.INSTANCE;
  }

  @Override
  public int getRejoinCount() {
    return 0;
  }

  @Override
  public TCLogger getLogger(String loggerName) {
    return null;
  }

  @Override
  public GroupID[] getGroupIDs() {
    return new GroupID[]{new GroupID(1)};
  }

  @Override
  public DsoCluster getDsoCluster() {
    return new MockDsoCluster();
  }

  @Override
  public DsoNode getCurrentNode() {
    return null;
  }

  @Override
  public AbortableOperationManager getAbortableOperationManager() {
    return null;
  }

  @Override
  public void fireOperatorEvent(EventLevel coreOperatorEventLevel, EventSubsystem coreEventSubsytem,
                                EventType eventType,
                                String eventMessage) {
    throw new ImplementMe();

  }

  @Override
  public SearchQueryResults executeQuery(String cachename, List queryStack, Set<String> attributeSet,
                                         Set<String> groupByAttributes, List<NVPair> sortAttributes,
                                         List<NVPair> aggregators, int maxResults, int batchSize, boolean waitForTxn,
                                         SearchRequestID queryId) {
    return null;
  }

  @Override
  public SearchQueryResults executeQuery(String cachename, List queryStack, boolean includeKeys,
                                         boolean includeValues, Set<String> attributeSet,
                                         List<NVPair> sortAttributes, List<NVPair> aggregators, int maxResults,
 int batchSize, int pageSize,
                                         boolean waitForTxn, SearchRequestID queryId) {
    return null;
  }

  @Override
  public MetaDataDescriptor createMetaDataDescriptor(String category) {
    return null;
  }

  @Override
  public void commitLock(Object lockID, LockLevel level) {
    MockUtil.logInfo("commit lock lock id "+  lockID +" - "+ level);
  }

  @Override
  public void commitAtomicTransaction(LockID lockID, LockLevel level) {
    throw new ImplementMe();

  }

  @Override
  public void beginLockInterruptibly(Object obj, LockLevel level) {
    throw new ImplementMe();

  }

  @Override
  public void beginLock(Object lockID, LockLevel level) {
    MockUtil.logInfo("begin lock lock id " + lockID + " - " + level);
  }

  @Override
  public void beginAtomicTransaction(LockID lockID, LockLevel level) {
    throw new ImplementMe();

  }

  @Override
  public void addTransactionCompleteListener(TransactionCompleteListener listener) {
    throw new ImplementMe();

  }

  @Override
  public void addRejoinLifecycleListener(RejoinLifecycleListener listener) {
      MockUtil.logInfo("addRejoinLifeCycleListener " + listener);
  }

  @Override
  public void unregisterBeforeShutdownHook(Runnable hook) {
    throw new ImplementMe();

  }

  @Override
  public boolean isRejoinInProgress() {
    return false;
  }
  
  public static String printVar(Object ... objs) {
    StringBuilder builder = new StringBuilder();
    for(Object obj : objs) {
      builder.append(obj.toString()).append(" : ");
    }
//    MockUtil.logInfo(builder);
    return builder.toString();
  }

  @Override
  public TaskRunner getTaskRunner() {
    return taskRunnner;
  }

  @Override
  public boolean isExplicitlyLocked(Object lockID, LockLevel level) {
    return false;
  }

  @Override
  public boolean isLockedBeforeRejoin(Object lockID, LockLevel level) {
    return false;
  }

  @Override
  public long getClientId() {
    return -1;
  }

  @Override
  public Object registerManagementService(Object service, ExecutorService executorService) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void unregisterManagementService(Object serviceID) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void sendEvent(TCManagementEvent event) {
    throw new UnsupportedOperationException();
  }

  @Override
  public LockID generateLockIdentifier(Object obj) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getLockAwardIDFor(LockID lock) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isLockAwardValid(LockID lock, long awardID) {
    throw new UnsupportedOperationException();
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
  public TCObject lookupExistingOrNull(Object obj) {
    throw new UnsupportedOperationException();
  }
}
