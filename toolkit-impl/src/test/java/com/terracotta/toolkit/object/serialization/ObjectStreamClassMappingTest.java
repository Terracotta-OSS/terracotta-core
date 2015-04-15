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
package com.terracotta.toolkit.object.serialization;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.tc.abortable.AbortableOperationManager;
import com.tc.cluster.DsoCluster;
import com.tc.exception.ImplementMe;
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
import com.tc.platform.PlatformService;
import com.tc.platform.rejoin.RejoinLifecycleListener;
import com.tc.properties.TCProperties;
import com.tc.search.SearchQueryResults;
import com.tc.search.SearchRequestID;
import com.tc.server.ServerEventType;
import com.tc.util.concurrent.TaskRunner;
import com.tcclient.cluster.DsoNode;
import com.terracottatech.search.NVPair;

import java.io.IOException;
import java.io.ObjectStreamClass;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

public class ObjectStreamClassMappingTest {

  private ObjectStreamClassMapping serializer;
  private SerializerMap            localSerializerMap;

  @Before
  public void init() {
    localSerializerMap = new LocalSerializerMap();
    serializer = new ObjectStreamClassMapping(new ThreadCheckerPlatformService(), localSerializerMap);
  }

  @Test
  public void testSerializer() throws IOException, ClassNotFoundException {
    HashMap<Integer, ObjectStreamClass> mappings = new HashMap<Integer, ObjectStreamClass>();

    for (Class cl : classSet()) {
      ObjectStreamClass osc = ObjectStreamClass.lookup(cl);
      mappings.put(serializer.getMappingFor(osc), osc);
    }

    for (Entry<Integer, ObjectStreamClass> entry : mappings.entrySet()) {
      Assert.assertEquals(entry.getValue().toString(), serializer.getObjectStreamClassFor(entry.getKey()).toString());
    }
  }

  @Test
  public void testMultipleLookupsReturnSameID() throws Exception {
    for (Class aClass : classSet()) {
      ObjectStreamClass osc = ObjectStreamClass.lookup(aClass);
      assertEquals(serializer.getMappingFor(osc), serializer.getMappingFor(osc));
    }
  }

  private static Set<Class> classSet() {
    Set<Class> classSet = Sets.newHashSet();
    classSet.add(Integer.class);
    classSet.add(Long.class);
    classSet.add(Character.class);
    classSet.add(Float.class);
    classSet.add(Double.class);
    classSet.add(Byte.class);
    classSet.add(Boolean.class);
    classSet.add(Short.class);
    classSet.add(String.class);
    classSet.add(Long.class);
    classSet.add(Enum.class);
    return classSet;
  }

  // Asserts that the write lock is taken from the Serializer thread
  private static class ThreadCheckerPlatformService implements PlatformService {

    private void checkThreadAccess(LockLevel level) {
      if (level == LockLevel.WRITE || level == LockLevel.SYNCHRONOUS_WRITE) {
        if (!Thread.currentThread().getName().equals(ObjectStreamClassMapping.SERIALIZER_ADD_MAPPING_THREAD)) { throw new AssertionError(
                                                                                                                                         "Serializer should not take a write lock in any thread other than "
                                                                                                                                             + ObjectStreamClassMapping.SERIALIZER_ADD_MAPPING_THREAD); }
      }
    }

    @Override
    public boolean isExplicitlyLocked() {
      throw new ImplementMe();
    }

    @Override
    public void beginAtomicTransaction(LockID lock, LockLevel level) {
      throw new ImplementMe();
    }

    @Override
    public void commitAtomicTransaction(LockID lock, LockLevel level) {
      throw new ImplementMe();
    }

    @Override
    public <T> T lookupRegisteredObjectByName(String name, Class<T> expectedType) {
      return null;
    }

    @Override
    public <T> T registerObjectByNameIfAbsent(String name, T object) {
      return null;
    }

    @Override
    public void logicalInvoke(Object object, LogicalOperation method, Object[] params) {
      throw new ImplementMe();
    }

    @Override
    public void waitForAllCurrentTransactionsToComplete() {
      throw new ImplementMe();
    }

    @Override
    public boolean isHeldByCurrentThread(Object lockID, LockLevel level) {
      return false;
    }

    @Override
    public void beginLock(Object lockID, LockLevel level) {
      checkThreadAccess(level);
    }

    @Override
    public void beginLockInterruptibly(Object obj, LockLevel level) {
      checkThreadAccess(level);

    }

    @Override
    public void commitLock(Object lockID, LockLevel level) {
      checkThreadAccess(level);

    }

    @Override
    public boolean tryBeginLock(Object lockID, LockLevel level) {
      checkThreadAccess(level);
      return false;
    }

    @Override
    public boolean tryBeginLock(Object lockID, LockLevel level, long timeout, TimeUnit timeUnit) {
      checkThreadAccess(level);
      return false;
    }

    @Override
    public void lockIDWait(Object lockID, long timeout, TimeUnit timeUnit) {
      checkThreadAccess(LockLevel.WRITE);
    }

    @Override
    public void lockIDNotify(Object lockID) {
      checkThreadAccess(LockLevel.WRITE);

    }

    @Override
    public void lockIDNotifyAll(Object lockID) {
      checkThreadAccess(LockLevel.WRITE);

    }

    @Override
    public TCProperties getTCProperties() {
      return null;
    }

    @Override
    public Object lookupRoot(String name, GroupID gid) {
      return null;
    }

    @Override
    public Object lookupOrCreateRoot(String name, Object object, GroupID gid) {
      return null;
    }

    @Override
    public TCObject lookupOrCreate(Object obj, GroupID gid) {
      return null;
    }

    @Override
    public Object lookupObject(ObjectID id) {
      return null;
    }

    @Override
    public GroupID[] getGroupIDs() {
      return null;
    }

    @Override
    public TCLogger getLogger(String loggerName) {
      return null;
    }

    @Override
    public void addTransactionCompleteListener(TransactionCompleteListener listener) {
      //
    }

    @Override
    public MetaDataDescriptor createMetaDataDescriptor(String category) {
      return null;
    }

    @Override
    public void fireOperatorEvent(EventLevel coreOperatorEventLevel, EventSubsystem coreEventSubsytem,
                                  EventType eventType,
                                  String eventMessage) {
      //
    }

    @Override
    public DsoNode getCurrentNode() {
      return null;
    }

    @Override
    public DsoCluster getDsoCluster() {
      return null;
    }

    @Override
    public void registerBeforeShutdownHook(Runnable hook) {
      //
    }

    @Override
    public String getUUID() {
      return null;
    }

    @Override
    public SearchQueryResults executeQuery(String cachename, List queryStack, boolean includeKeys,
                                           boolean includeValues, Set<String> attributeSet,
                                           List<NVPair> sortAttributes, List<NVPair> aggregators, int maxResults,
                                           int batchSize, int pageSize, boolean waitForTxn, SearchRequestID queryId) {
      return null;
    }

    @Override
    public SearchQueryResults executeQuery(String cachename, List queryStack, Set<String> attributeSet,
                                           Set<String> groupByAttributes, List<NVPair> sortAttributes,
                                           List<NVPair> aggregators, int maxResults, int batchSize, boolean waitForTxn, SearchRequestID queryId) {
      return null;
    }

    @Override
    public void preFetchObject(ObjectID id) {
      //

    }

    @Override
    public void verifyCapability(String capability) {
      //
    }

    @Override
    public AbortableOperationManager getAbortableOperationManager() {
      return null;
    }

    @Override
    public void addRejoinLifecycleListener(RejoinLifecycleListener listener) {
      //
    }

    @Override
    public void removeRejoinLifecycleListener(RejoinLifecycleListener listener) {
      //
    }

    @Override
    public void throttlePutIfNecessary(ObjectID object) {
      //
    }

    @Override
    public boolean isLockedBeforeRejoin() {
      return false;
    }

    @Override
    public void registerServerEventListener(final ServerEventDestination destination, final Set<ServerEventType> listenTo) {
      //
    }

    @Override
    public void registerServerEventListener(final ServerEventDestination destination, final ServerEventType... listenTo) {
      //
    }

    @Override
    public void unregisterServerEventListener(final ServerEventDestination destination, final Set<ServerEventType> listenTo) {
      //
    }

    @Override
    public void unregisterServerEventListener(final ServerEventDestination destination, final ServerEventType... listenTo) {
      //
    }

    @Override
    public int getRejoinCount() {
      return 0;
    }

    @Override
    public boolean isRejoinInProgress() {
      return false;
    }

    @Override
    public void unregisterBeforeShutdownHook(Runnable hook) {
      //
    }

    @Override
    public TaskRunner getTaskRunner() {
      return null;
    }

    @Override
    public boolean isExplicitlyLocked(Object lockID, LockLevel level) {
      throw new ImplementMe();
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

}
