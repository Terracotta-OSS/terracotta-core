/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.cache.VersionedValue;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;
import org.terracotta.toolkit.internal.store.ConfigFieldsInternal.LOCK_STRATEGY;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractor;

import com.tc.abortable.AbortedOperationException;
import com.tc.exception.ImplementMe;
import com.tc.object.ObjectID;
import com.tc.object.TCClass;
import com.tc.object.TCObject;
import com.tc.object.TCObjectServerMap;
import com.tc.object.VersionedObject;
import com.tc.object.bytecode.TCServerMap;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.PinnedEntryFaultCallback;
import com.tc.platform.PlatformService;
import com.terracotta.toolkit.collections.map.AggregateServerMap;
import com.terracotta.toolkit.collections.map.InternalToolkitMap;
import com.terracotta.toolkit.collections.map.ServerMap.GetType;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFactory;
import com.terracotta.toolkit.object.DestroyApplicator;
import com.terracotta.toolkit.object.ToolkitObjectStripe;
import com.terracotta.toolkit.search.SearchFactory;
import com.terracotta.toolkit.type.DistributedClusteredObjectLookup;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class MockAggregateServerMap extends AggregateServerMap {
  private final InternalToolkitMap   anyServerMap         = new FakeServerMap();
  private final TCObjectServerMap    anyTcObjectServerMap = new FakeTCObjectServerMap();
  private final Map                  sharedMap            = new ConcurrentHashMap();

  @Mock
  private L1ServerMapLocalCacheStore localCacheStore;

  public MockAggregateServerMap(ToolkitObjectType type, SearchFactory searchBuilderFactory,
                                DistributedClusteredObjectLookup lookup, String name,
                                ToolkitObjectStripe[] stripeObjects, Configuration config, Callable schemaCreator,
                                ServerMapLocalStoreFactory serverMapLocalStoreFactory, PlatformService platformService) {
    super(type, searchBuilderFactory, lookup, name, stripeObjects, config, schemaCreator, serverMapLocalStoreFactory,
          platformService);
    MockitoAnnotations.initMocks(this);

  }

  @Override
  protected L1ServerMapLocalCacheStore createLocalCacheStore() {
    return localCacheStore;
  }

  @Override
  protected InternalToolkitMap getAnyServerMap() {
    return anyServerMap;
  }

  @Override
  protected TCObjectServerMap getAnyTCObjectServerMap() {
    return anyTcObjectServerMap;
  }

  @Override
  protected InternalToolkitMap getServerMapForKey(Object key) {
    return anyServerMap;
  }

  // --------------Fake class------------------------
  private class FakeServerMap implements InternalToolkitMap {

    @Override
    public Object put(Object key, Object value) {
      return sharedMap.put(key, value);
    }

    @Override
    public void clear() {
    }

    @Override
    public void clearLocalCache() {
      sharedMap.clear();
    }

    @Override
    public Object put(Object key, Object value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
      return sharedMap.put(key, value);
    }

    @Override
    public Object unlockedGet(Object key, boolean quiet) {
      return sharedMap.get(key);
    }

    @Override
    public Object putIfAbsent(Object key, Object value) {
      throw new ImplementMe();
    }

    @Override
    public boolean remove(Object key, Object value) {
      throw new ImplementMe();
    }

    @Override
    public Object replace(Object key, Object value) {
      throw new ImplementMe();
    }

    @Override
    public boolean replace(Object key, Object oldValue, Object newValue) {
      throw new ImplementMe();
    }

    @Override
    public boolean containsKey(Object key) {
      throw new ImplementMe();
    }

    @Override
    public boolean containsValue(Object value) {
      throw new ImplementMe();
    }

    @Override
    public Set entrySet() {
      throw new ImplementMe();
    }

    @Override
    public Object get(Object key) {
      throw new ImplementMe();
    }

    @Override
    public boolean isEmpty() {
      throw new ImplementMe();
    }

    @Override
    public Set keySet() {
      throw new ImplementMe();
    }

    @Override
    public void putAll(Map m) {
      throw new ImplementMe();

    }

    @Override
    public Object remove(Object key) {
      throw new ImplementMe();
    }

    @Override
    public int size() {
      throw new ImplementMe();
    }

    @Override
    public Collection values() {
      throw new ImplementMe();
    }

    @Override
    public void __tc_managed(TCObject t) {
      throw new ImplementMe();

    }

    @Override
    public TCObject __tc_managed() {
      throw new ImplementMe();
    }

    @Override
    public boolean __tc_isManaged() {
      throw new ImplementMe();
    }

    @Override
    public void cleanupOnDestroy() {
      throw new ImplementMe();

    }

    @Override
    public void destroy() {
      throw new ImplementMe();

    }

    @Override
    public boolean isDestroyed() {
      throw new ImplementMe();
    }

    @Override
    public void applyDestroy() {
      throw new ImplementMe();

    }

    @Override
    public void setApplyDestroyCallback(DestroyApplicator applyDestroyCallback) {
      throw new ImplementMe();

    }

    @Override
    public Object get(Object key, ObjectID valueOid) {
      throw new ImplementMe();
    }

    @Override
    public String getName() {
      throw new ImplementMe();
    }

    @Override
    public ToolkitLockTypeInternal getLockType() {
      throw new ImplementMe();
    }

    @Override
    public boolean isEventual() {
      throw new ImplementMe();
    }

    @Override
    public boolean invalidateOnChange() {
      throw new ImplementMe();
    }

    @Override
    public boolean isLocalCacheEnabled() {
      throw new ImplementMe();
    }

    @Override
    public int getMaxTTISeconds() {
      throw new ImplementMe();
    }

    @Override
    public int getMaxTTLSeconds() {
      throw new ImplementMe();
    }

    @Override
    public int getMaxCountInCluster() {
      throw new ImplementMe();
    }

    @Override
    public void putVersioned(Object key, Object value, long version, int createTimeInSecs, int customMaxTTISeconds,
                             int customMaxTTLSeconds) {
      throw new ImplementMe();

    }

    @Override
    public void putNoReturn(Object key, Object value, int createTimeInSecs, int customMaxTTISeconds,
                            int customMaxTTLSeconds) {
      throw new ImplementMe();

    }

    @Override
    public Object putIfAbsent(Object key, Object value, int createTimeInSecs, int customMaxTTISeconds,
                              int customMaxTTLSeconds) {
      throw new ImplementMe();
    }

    @Override
    public void putIfAbsentVersioned(Object key, Object value, long version, int createTimeInSecs,
                                     int customMaxTTISeconds, int customMaxTTLSeconds) {
      throw new ImplementMe();

    }

    @Override
    public Object get(Object key, boolean quiet) {
      throw new ImplementMe();
    }

    @Override
    public VersionedValue getVersionedValue(Object key) {
      throw new ImplementMe();
    }

    @Override
    public void setConfigField(String name, Object value) {
      throw new ImplementMe();

    }

    @Override
    public void initializeLocalCache(L1ServerMapLocalCacheStore localCacheStore, PinnedEntryFaultCallback callback,
                                     boolean localCacheEnabled) {
      throw new ImplementMe();

    }

    @Override
    public void removeNoReturn(Object key) {
      throw new ImplementMe();

    }

    @Override
    public void removeNoReturnVersioned(Object key, long version) {
      throw new ImplementMe();

    }

    @Override
    public Object unsafeLocalGet(Object key) {
      throw new ImplementMe();
    }

    @Override
    public int localSize() {
      throw new ImplementMe();
    }

    @Override
    public Set localKeySet() {
      throw new ImplementMe();
    }

    @Override
    public boolean containsLocalKey(Object key) {
      throw new ImplementMe();
    }

    @Override
    public Object checkAndGetNonExpiredValue(Object key, Object value, GetType getType, boolean quiet) {
      throw new ImplementMe();
    }

    @Override
    public void cleanLocalState() {
      throw new ImplementMe();

    }

    @Override
    public long localOnHeapSizeInBytes() {
      throw new ImplementMe();
    }

    @Override
    public long localOffHeapSizeInBytes() {
      throw new ImplementMe();
    }

    @Override
    public int localOnHeapSize() {
      throw new ImplementMe();
    }

    @Override
    public int localOffHeapSize() {
      throw new ImplementMe();
    }

    @Override
    public boolean containsKeyLocalOnHeap(Object key) {
      throw new ImplementMe();
    }

    @Override
    public boolean containsKeyLocalOffHeap(Object key) {
      throw new ImplementMe();
    }

    @Override
    public void unlockedPutNoReturn(Object key, Object value, int createTimeInSecs, int customMaxTTISeconds,
                                    int customMaxTTLSeconds) {
      throw new ImplementMe();

    }

    @Override
    public void unlockedPutNoReturnVersioned(Object key, Object value, long version, int createTimeInSecs,
                                             int customMaxTTISeconds, int customMaxTTLSeconds) {
      throw new ImplementMe();

    }

    @Override
    public void unlockedRemoveNoReturn(Object key) {
      throw new ImplementMe();

    }

    @Override
    public void unlockedRemoveNoReturnVersioned(Object key, long version) {
      throw new ImplementMe();

    }

    @Override
    public void unlockedClear() {
      throw new ImplementMe();

    }

    @Override
    public boolean isCompressionEnabled() {
      throw new ImplementMe();
    }

    @Override
    public boolean isCopyOnReadEnabled() {
      throw new ImplementMe();
    }

    @Override
    public void disposeLocally() {
      throw new ImplementMe();

    }

    @Override
    public ToolkitReadWriteLock createLockForKey(Object key) {
      throw new ImplementMe();
    }

    @Override
    public void registerAttributeExtractor(ToolkitAttributeExtractor extractor) {
      throw new ImplementMe();

    }

    @Override
    public boolean isEvictionEnabled() {
      throw new ImplementMe();
    }

    @Override
    public void setConfigFieldInternal(String fieldChanged, Object changedValue) {
      throw new ImplementMe();

    }

    @Override
    public void setLockStrategy(LOCK_STRATEGY strategy) {
      throw new ImplementMe();

    }

    @Override
    public void addTxnInProgressKeys(Set txnInProgressForAdd, Set removeSet) {
      throw new ImplementMe();

    }

    @Override
    public Set keySet(Set filterSet) {
      throw new ImplementMe();
    }

    @Override
    public Collection values(Set filterSet) {
      throw new ImplementMe();
    }

    @Override
    public Set entrySet(Set filterSet) {
      throw new ImplementMe();
    }

    @Override
    public void clearVersioned() {
      throw new ImplementMe();

    }
  }
  
  // --------------Fake class------------------------

  private class FakeTCObjectServerMap implements TCObjectServerMap {

    @Override
    public long getAllSize(TCServerMap[] maps) throws AbortedOperationException {
      return sharedMap.size();
    }

    @Override
    public ObjectID getObjectID() {
      throw new ImplementMe();
    }

    @Override
    public boolean isShared() {
      throw new ImplementMe();
    }

    @Override
    public Object getPeerObject() {
      throw new ImplementMe();
    }

    @Override
    public TCClass getTCClass() {
      throw new ImplementMe();
    }

    @Override
    public Object getResolveLock() {
      throw new ImplementMe();
    }

    @Override
    public void objectFieldChanged(String classname, String fieldname, Object newValue, int index) {
      throw new ImplementMe();

    }

    @Override
    public void booleanFieldChanged(String classname, String fieldname, boolean newValue, int index) {
      throw new ImplementMe();

    }

    @Override
    public void byteFieldChanged(String classname, String fieldname, byte newValue, int index) {
      throw new ImplementMe();

    }

    @Override
    public void charFieldChanged(String classname, String fieldname, char newValue, int index) {
      throw new ImplementMe();

    }

    @Override
    public void doubleFieldChanged(String classname, String fieldname, double newValue, int index) {
      throw new ImplementMe();

    }

    @Override
    public void floatFieldChanged(String classname, String fieldname, float newValue, int index) {
      throw new ImplementMe();

    }

    @Override
    public void intFieldChanged(String classname, String fieldname, int newValue, int index) {
      throw new ImplementMe();

    }

    @Override
    public void longFieldChanged(String classname, String fieldname, long newValue, int index) {
      throw new ImplementMe();

    }

    @Override
    public void shortFieldChanged(String classname, String fieldname, short newValue, int index) {
      throw new ImplementMe();

    }

    @Override
    public void objectArrayChanged(int startPos, Object[] array, int length) {
      throw new ImplementMe();

    }

    @Override
    public void primitiveArrayChanged(int startPos, Object array, int length) {
      throw new ImplementMe();

    }

    @Override
    public void literalValueChanged(Object newValue, Object oldValue) {
      throw new ImplementMe();

    }

    @Override
    public void setLiteralValue(Object newValue) {
      throw new ImplementMe();

    }

    @Override
    public void hydrate(DNA from, boolean force, WeakReference peer) throws ClassNotFoundException {
      throw new ImplementMe();

    }

    @Override
    public void resolveReference(String fieldName) {
      throw new ImplementMe();

    }

    @Override
    public void unresolveReference(String fieldName) {
      throw new ImplementMe();

    }

    @Override
    public void resolveArrayReference(int index) {
      throw new ImplementMe();

    }

    @Override
    public void resolveAllReferences() {
      throw new ImplementMe();

    }

    @Override
    public ObjectID setReference(String fieldName, ObjectID id) {
      throw new ImplementMe();
    }

    @Override
    public void setArrayReference(int index, ObjectID id) {
      throw new ImplementMe();

    }

    @Override
    public void clearReference(String fieldName) {
      throw new ImplementMe();

    }

    @Override
    public void setValue(String fieldName, Object obj) {
      throw new ImplementMe();

    }

    @Override
    public long getVersion() {
      throw new ImplementMe();
    }

    @Override
    public void setVersion(long version) {
      throw new ImplementMe();

    }

    @Override
    public boolean isNew() {
      throw new ImplementMe();
    }

    @Override
    public void logicalInvoke(int method, String methodSignature, Object[] params) {
      throw new ImplementMe();

    }

    @Override
    public void disableAutoLocking() {
      throw new ImplementMe();

    }

    @Override
    public boolean autoLockingDisabled() {
      throw new ImplementMe();
    }

    @Override
    public void setNotNew() {
      throw new ImplementMe();

    }

    @Override
    public void dehydrate(DNAWriter writer) {
      throw new ImplementMe();

    }

    @Override
    public String getFieldNameByOffset(long fieldOffset) {
      throw new ImplementMe();
    }

    @Override
    public void clearAccessed() {
      throw new ImplementMe();

    }

    @Override
    public void objectFieldChangedByOffset(String classname, long fieldOffset, Object newValue, int index) {
      throw new ImplementMe();

    }

    @Override
    public boolean recentlyAccessed() {
      throw new ImplementMe();
    }

    @Override
    public void markAccessed() {
      throw new ImplementMe();

    }

    @Override
    public int accessCount(int factor) {
      throw new ImplementMe();
    }

    @Override
    public void initialize(int maxTTISeconds, int maxTTLSeconds, int targetMaxTotalCount, boolean invalidateOnChange,
                           boolean localCacheEnabled) {
      throw new ImplementMe();

    }

    @Override
    public void doLogicalRemove(TCServerMap map, Object lockID, Object key) {
      throw new ImplementMe();

    }

    @Override
    public void doLogicalRemoveVersioned(TCServerMap map, Object lockID, Object key, long version) {
      throw new ImplementMe();

    }

    @Override
    public void doLogicalRemoveUnlocked(TCServerMap map, Object key) {
      throw new ImplementMe();

    }

    @Override
    public void doLogicalRemoveUnlockedVersioned(TCServerMap map, Object key, long version) {
      throw new ImplementMe();

    }

    @Override
    public boolean doLogicalRemoveUnlocked(TCServerMap map, Object key, Object value) {
      throw new ImplementMe();
    }

    @Override
    public Object doLogicalPutIfAbsentUnlocked(TCServerMap map, Object key, Object value) {
      throw new ImplementMe();
    }

    @Override
    public boolean doLogicalReplaceUnlocked(TCServerMap map, Object key, Object current, Object newValue) {
      throw new ImplementMe();
    }

    @Override
    public boolean doLogicalReplaceUnlocked(TCServerMap map, Object key, Object newValue) {
      throw new ImplementMe();
    }

    @Override
    public void doLogicalPut(Object lockID, Object key, Object value) {
      throw new ImplementMe();

    }

    @Override
    public void doLogicalPutVersioned(TCServerMap map, Object lockID, Object key, Object value, long version) {
      throw new ImplementMe();

    }

    @Override
    public void doLogicalPutIfAbsentVersioned(Object key, Object value, long version) {
      throw new ImplementMe();

    }

    @Override
    public void doClear(TCServerMap map) {
      throw new ImplementMe();
    }

    @Override
    public void doLogicalPutUnlocked(TCServerMap map, Object key, Object value) {
      throw new ImplementMe();

    }

    @Override
    public void doLogicalPutUnlockedVersioned(TCServerMap map, Object key, Object value, long version) {
      throw new ImplementMe();

    }

    @Override
    public Object getValueUnlocked(TCServerMap map, Object key) throws AbortedOperationException {
      throw new ImplementMe();
    }

    @Override
    public VersionedObject getVersionedValue(TCServerMap map, Object key) throws AbortedOperationException {
      throw new ImplementMe();
    }

    @Override
    public Map getAllValuesUnlocked(Map mapIdToKeysMap) throws AbortedOperationException {
      throw new ImplementMe();
    }

    @Override
    public Set keySet(TCServerMap map) throws AbortedOperationException {
      throw new ImplementMe();
    }

    @Override
    public Object getValue(TCServerMap map, Object lockID, Object key) throws AbortedOperationException {
      throw new ImplementMe();
    }

    @Override
    public int getLocalSize() {
      throw new ImplementMe();
    }

    @Override
    public long getLocalOnHeapSizeInBytes() {
      throw new ImplementMe();
    }

    @Override
    public long getLocalOffHeapSizeInBytes() {
      throw new ImplementMe();
    }

    @Override
    public int getLocalOnHeapSize() {
      throw new ImplementMe();
    }

    @Override
    public int getLocalOffHeapSize() {
      throw new ImplementMe();
    }

    @Override
    public void clearLocalCache(TCServerMap map) {
      throw new ImplementMe();

    }

    @Override
    public void cleanLocalState() {
      throw new ImplementMe();

    }

    @Override
    public void clearAllLocalCacheInline() {
      throw new ImplementMe();

    }

    @Override
    public void evictedInServer(Object key) {
      throw new ImplementMe();

    }

    @Override
    public Set getLocalKeySet() {
      throw new ImplementMe();
    }

    @Override
    public boolean containsLocalKey(Object key) {
      throw new ImplementMe();
    }

    @Override
    public boolean containsKeyLocalOnHeap(Object key) {
      throw new ImplementMe();
    }

    @Override
    public boolean containsKeyLocalOffHeap(Object key) {
      throw new ImplementMe();
    }

    @Override
    public Object getValueFromLocalCache(Object key) {
      throw new ImplementMe();
    }

    @Override
    public void removeValueFromLocalCache(Object key) {
      throw new ImplementMe();

    }

    @Override
    public void addMetaData(MetaDataDescriptor mdd) {
      throw new ImplementMe();

    }

    @Override
    public void setupLocalStore(L1ServerMapLocalCacheStore serverMapLocalStore, PinnedEntryFaultCallback callback) {
      throw new ImplementMe();

    }

    @Override
    public void destroyLocalStore() {
      throw new ImplementMe();

    }

    @Override
    public void setLocalCacheEnabled(boolean enabled) {
      throw new ImplementMe();

    }

    @Override
    public void recalculateLocalCacheSize(Object key) {
      throw new ImplementMe();

    }

    @Override
    public void doLogicalSetLastAccessedTime(Object key, Object value, long lastAccessedTime) {
      throw new ImplementMe();

    }

    @Override
    public void doLogicalExpire(Object lockID, Object key, Object value) {
      throw new ImplementMe();

    }

    @Override
    public boolean doLogicalExpireUnlocked(TCServerMap map, Object key, Object value) {
      throw new ImplementMe();
    }

    @Override
    public void addTxnInProgressKeys(Set addSet, Set removeSet) {
      throw new ImplementMe();

    }

    @Override
    public void doClearVersioned() {
      throw new ImplementMe();

    }

  }

}