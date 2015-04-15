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

import com.google.common.collect.SetMultimap;
import com.tc.abortable.AbortedOperationException;
import com.tc.exception.ImplementMe;
import com.tc.object.ObjectID;
import com.tc.object.LogicalOperation;
import com.tc.object.TCClass;
import com.tc.object.TCObjectServerMap;
import com.tc.object.VersionedObject;
import com.tc.object.bytecode.TCServerMap;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.PinnedEntryFaultCallback;
import com.tc.server.ServerEventType;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MockTCObjectServerMap implements TCObjectServerMap<Object> {

  Map<Object,Object> map = new ConcurrentHashMap<Object, Object>();
  
  @Override
  public ObjectID getObjectID() {
    return ObjectID.NULL_ID;
  }

  @Override
  public boolean isShared() {
    if(true) {
    throw new ImplementMe();
    }
    return false;
  }

  @Override
  public Object getPeerObject() {
    if(true) {
      throw new ImplementMe();
      }
    return null;
  }

  @Override
  public Object getResolveLock() {
    if(true) {
      throw new ImplementMe();
      }
    return null;
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
  public void hydrate(DNA from, boolean force, WeakReference peer) {
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
  public void logicalInvoke(LogicalOperation method, Object[] params) {
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
   MockUtil.logInfo("Initalizing TCObjectServerMap");
    
  }

  @Override
  public void doLogicalRemove(TCServerMap serverMap, Object lockID, Object key) {
    throw new ImplementMe();
    
  }

  @Override
  public void doLogicalRemoveVersioned(TCServerMap serverMap, Object lockID, Object key, long version) {
    throw new ImplementMe();
    
  }

  @Override
  public void doLogicalRemoveUnlocked(TCServerMap serverMap, Object key) {
    throw new ImplementMe();
    
  }

  @Override
  public void doLogicalRemoveUnlockedVersioned(TCServerMap serverMap, Object key, long version) {
    throw new ImplementMe();
    
  }

  @Override
  public boolean doLogicalRemoveUnlocked(TCServerMap serverMap, Object key, Object value, MetaDataDescriptor mdd) {
    throw new ImplementMe();
  }

  @Override
  public boolean doLogicalPutIfAbsentUnlocked(TCServerMap serverMap, Object key, Object value, MetaDataDescriptor mdd) {
    throw new ImplementMe();
  }

  @Override
  public boolean doLogicalReplaceUnlocked(TCServerMap serverMap, Object key, Object current, Object newValue,
                                          MetaDataDescriptor mdd) {
    throw new ImplementMe();
  }

  @Override
  public void doLogicalPut(Object lockID, Object key, Object value) {
    map.put(key, value);
    MockUtil.logInfo("TCObjectServerMap:doLogicalPut > " + lockID+key+value );
    
  }

  @Override
  public void doLogicalPutVersioned(TCServerMap serverMap, Object lockID, Object key, Object value, long version) {
    throw new ImplementMe();
    
  }

  @Override
  public void doClear(TCServerMap serverMap) {
    throw new ImplementMe();
    
  }

  @Override
  public void doClearVersioned() {
    throw new ImplementMe();
  }

  @Override
  public void doLogicalPutUnlocked(TCServerMap serverMap, Object key, Object value) {
    throw new ImplementMe();
    
  }

  @Override
  public void doLogicalPutUnlockedVersioned(TCServerMap serverMap, Object key, Object value, long version) {
    throw new ImplementMe();
    
  }

  @Override
  public Object getValueUnlocked(TCServerMap serverMap, Object key) {
    throw new ImplementMe();
  }

  @Override
  public Map getAllValuesUnlocked(SetMultimap mapIdToKeysMap) {
    throw new ImplementMe();
  }

  @Override
  public Set keySet(TCServerMap serverMap) {
    throw new ImplementMe();
  }

  @Override
  public Object getValue(TCServerMap tcmap, Object lockID, Object key) {
    MockUtil.logInfo("TCObjectServerMap:getValue > " + lockID+key );
    return  map.get(key);
  }

  @Override
  public long getAllSize(TCServerMap[] maps) {
    return 0;
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
  public void clearLocalCache(TCServerMap serverMap) {
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
    // MockUtil.logInfo("SetUp Local Store");
    setupLocalCache(serverMapLocalStore, callback);
  }

  private void setupLocalCache(L1ServerMapLocalCacheStore serverMapLocalStore, PinnedEntryFaultCallback callback) {
  //  this.cache = globalLocalCacheManager.getOrCreateLocalCache(this.objectID, objectManager, manager,
    // localCacheEnabled, serverMapLocalStore, callback);
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
  public boolean doLogicalExpireUnlocked(TCServerMap serverMap, Object key, Object value) {
    throw new ImplementMe();
  }

  @Override
  public void addTxnInProgressKeys(Set addSet, Set removeSet) {
    throw new ImplementMe();
    
  }

  @Override
  public void doLogicalPutIfAbsentVersioned(Object key, Object value, long version) {
    throw new ImplementMe();
    
  }

  @Override
  public VersionedObject getVersionedValue(TCServerMap serverMap, Object key) {
    throw new ImplementMe();
  }

  @Override
  public void doRegisterListener(Set<ServerEventType> eventTypes, boolean skipRejoinChecks) {
    throw new ImplementMe();

  }

  @Override
  public void doUnregisterListener(Set<ServerEventType> eventTypes) {
    throw new ImplementMe();

  }

  @Override
  public String getExtendingClassName() {
    throw new ImplementMe();
  }

  @Override
  public String getClassName() {
    throw new ImplementMe();
  }

  @Override
  public Class<?> getPeerClass() {
    throw new ImplementMe();
  }

  @Override
  public boolean isIndexed() {
    throw new ImplementMe();
  }

  @Override
  public boolean isLogical() {
    throw new ImplementMe();
  }

  @Override
  public boolean isEnum() {
    throw new ImplementMe();
  }

  @Override
  public Map<Object, VersionedObject> getAllVersioned(final SetMultimap<ObjectID, Object> mapIdToKeysMap) throws AbortedOperationException {
    throw new UnsupportedOperationException("Implement me!");
  }
}