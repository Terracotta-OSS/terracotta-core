/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.util;

import org.terracotta.toolkit.rejoin.RejoinException;

import com.google.common.collect.SetMultimap;
import com.tc.abortable.AbortedOperationException;
import com.tc.object.ObjectID;
import com.tc.object.TCClass;
import com.tc.object.TCObjectServerMap;
import com.tc.object.VersionedObject;
import com.tc.object.bytecode.TCServerMap;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.PinnedEntryFaultCallback;
import com.tc.platform.PlatformService;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Set;

public class ExplicitLockingTCObjectServerMapImpl<L> implements TCObjectServerMap<L> {

  private final TCObjectServerMap delegate;
  private final PlatformService   service;

  public ExplicitLockingTCObjectServerMapImpl(TCObjectServerMap delegate, PlatformService service) {
    this.delegate = delegate;
    this.service = service;
  }

  private void assertLockAndRejoinState() {
    if (service.isLockedBeforeRejoin()) { throw new RejoinException(
                                                                    "Lock state not valid: lock was taken before rejoin"); }
    if (service.isRejoinInProgress()) { throw new RejoinException(
                                                                  "Operation can't be performed, as rejoin is in progress"); }
  }

  @Override
  public ObjectID getObjectID() {
    return delegate.getObjectID();
  }

  @Override
  public boolean isShared() {
    return delegate.isShared();
  }

  @Override
  public Object getPeerObject() {
    return delegate.getPeerObject();
  }

  @Override
  public TCClass getTCClass() {
    return delegate.getTCClass();
  }

  @Override
  public Object getResolveLock() {
    return delegate.getResolveLock();
  }

  @Override
  public void objectFieldChanged(String classname, String fieldname, Object newValue, int index) {
    delegate.objectFieldChanged(classname, fieldname, newValue, index);
  }

  @Override
  public void booleanFieldChanged(String classname, String fieldname, boolean newValue, int index) {
    delegate.booleanFieldChanged(classname, fieldname, newValue, index);
  }

  @Override
  public void byteFieldChanged(String classname, String fieldname, byte newValue, int index) {
    delegate.byteFieldChanged(classname, fieldname, newValue, index);
  }

  @Override
  public void charFieldChanged(String classname, String fieldname, char newValue, int index) {
    delegate.charFieldChanged(classname, fieldname, newValue, index);
  }

  @Override
  public void doubleFieldChanged(String classname, String fieldname, double newValue, int index) {
    delegate.doubleFieldChanged(classname, fieldname, newValue, index);
  }

  @Override
  public void floatFieldChanged(String classname, String fieldname, float newValue, int index) {
    delegate.floatFieldChanged(classname, fieldname, newValue, index);
  }

  @Override
  public void intFieldChanged(String classname, String fieldname, int newValue, int index) {
    delegate.intFieldChanged(classname, fieldname, newValue, index);
  }

  @Override
  public void longFieldChanged(String classname, String fieldname, long newValue, int index) {
    delegate.longFieldChanged(classname, fieldname, newValue, index);
  }

  @Override
  public void shortFieldChanged(String classname, String fieldname, short newValue, int index) {
    delegate.shortFieldChanged(classname, fieldname, newValue, index);
  }

  @Override
  public void objectArrayChanged(int startPos, Object[] array, int length) {
    delegate.objectArrayChanged(startPos, array, length);
  }

  @Override
  public void primitiveArrayChanged(int startPos, Object array, int length) {
    delegate.primitiveArrayChanged(startPos, array, length);
  }

  @Override
  public void literalValueChanged(Object newValue, Object oldValue) {
    delegate.literalValueChanged(newValue, oldValue);
  }

  @Override
  public void setLiteralValue(Object newValue) {
    delegate.setLiteralValue(newValue);
  }

  @Override
  public void hydrate(DNA from, boolean force, WeakReference peer) throws ClassNotFoundException {
    delegate.hydrate(from, force, peer);
  }

  @Override
  public void resolveReference(String fieldName) {
    delegate.resolveReference(fieldName);
  }

  @Override
  public void unresolveReference(String fieldName) {
    delegate.unresolveReference(fieldName);
  }

  @Override
  public void resolveArrayReference(int index) {
    delegate.resolveArrayReference(index);
  }

  @Override
  public void resolveAllReferences() {
    delegate.resolveAllReferences();
  }

  @Override
  public ObjectID setReference(String fieldName, ObjectID id) {
    return delegate.setReference(fieldName, id);
  }

  @Override
  public void setArrayReference(int index, ObjectID id) {
    delegate.setArrayReference(index, id);
  }

  @Override
  public void clearReference(String fieldName) {
    delegate.clearReference(fieldName);
  }

  @Override
  public void setValue(String fieldName, Object obj) {
    delegate.setValue(fieldName, obj);
  }

  @Override
  public long getVersion() {
    return delegate.getVersion();
  }

  @Override
  public void setVersion(long version) {
    delegate.setVersion(version);
  }

  @Override
  public boolean isNew() {
    return delegate.isNew();
  }

  @Override
  public void logicalInvoke(int method, String methodSignature, Object[] params) {
    assertLockAndRejoinState();
    delegate.logicalInvoke(method, methodSignature, params);
  }

  @Override
  public void disableAutoLocking() {
    delegate.disableAutoLocking();
  }

  @Override
  public boolean autoLockingDisabled() {
    return delegate.autoLockingDisabled();
  }

  @Override
  public void setNotNew() {
    delegate.setNotNew();
  }

  @Override
  public void dehydrate(DNAWriter writer) {
    delegate.dehydrate(writer);
  }

  @Override
  public String getFieldNameByOffset(long fieldOffset) {
    return delegate.getFieldNameByOffset(fieldOffset);
  }

  @Override
  public void clearAccessed() {
    delegate.clearAccessed();
  }

  @Override
  public void objectFieldChangedByOffset(String classname, long fieldOffset, Object newValue, int index) {
    delegate.objectFieldChangedByOffset(classname, fieldOffset, newValue, index);
  }

  @Override
  public boolean recentlyAccessed() {
    return delegate.recentlyAccessed();
  }

  @Override
  public void markAccessed() {
    delegate.markAccessed();
  }

  @Override
  public int accessCount(int factor) {
    return delegate.accessCount(factor);
  }

  @Override
  public void initialize(int maxTTISeconds, int maxTTLSeconds, int targetMaxTotalCount, boolean invalidateOnChange,
                         boolean localCacheEnabled) {
    delegate.initialize(maxTTISeconds, maxTTLSeconds, targetMaxTotalCount, invalidateOnChange, localCacheEnabled);
  }

  @Override
  public void doLogicalRemove(TCServerMap map, Object lockID, Object key) {
    assertLockAndRejoinState();
    delegate.doLogicalRemove(map, lockID, key);
  }

  @Override
  public void doLogicalRemoveVersioned(final TCServerMap map, final Object lockID, final Object key, final long version) {
    assertLockAndRejoinState();
    delegate.doLogicalRemoveVersioned(map, lockID, key, version);
  }

  @Override
  public void doLogicalRemoveUnlocked(TCServerMap map, Object key) {
    assertLockAndRejoinState();
    delegate.doLogicalRemoveUnlocked(map, key);
  }

  @Override
  public void doLogicalRemoveUnlockedVersioned(final TCServerMap map, final Object key, final long version) {
    assertLockAndRejoinState();
    delegate.doLogicalRemoveUnlockedVersioned(map, key, version);
  }

  @Override
  public boolean doLogicalRemoveUnlocked(TCServerMap map, Object key, Object value, MetaDataDescriptor mdd)
      throws AbortedOperationException {
    assertLockAndRejoinState();
    return delegate.doLogicalRemoveUnlocked(map, key, value, mdd);
  }

  @Override
  public boolean doLogicalPutIfAbsentUnlocked(TCServerMap map, Object key, Object value, MetaDataDescriptor mdd)
      throws AbortedOperationException {
    assertLockAndRejoinState();
    return delegate.doLogicalPutIfAbsentUnlocked(map, key, value, mdd);
  }

  @Override
  public boolean doLogicalReplaceUnlocked(TCServerMap map, Object key, Object current, Object newValue,
                                          MetaDataDescriptor mdd)
      throws AbortedOperationException {
    assertLockAndRejoinState();
    return delegate.doLogicalReplaceUnlocked(map, key, current, newValue, mdd);
  }

  @Override
  public void doLogicalPut(Object lockID, Object key, Object value) {
    assertLockAndRejoinState();
    delegate.doLogicalPut(lockID, key, value);
  }

  @Override
  public void doLogicalPutVersioned(final TCServerMap map, final Object lockID, final Object key, final Object value, final long version) {
    assertLockAndRejoinState();
    delegate.doLogicalPutVersioned(map, lockID, key, value, version);
  }

  @Override
  public void doLogicalPutIfAbsentVersioned(Object key, Object value, long version) {
    assertLockAndRejoinState();
    delegate.doLogicalPutIfAbsentVersioned(key, value, version);
  }

  @Override
  public void doClear(TCServerMap map) {
    assertLockAndRejoinState();
    delegate.doClear(map);
  }

  @Override
  public void doLogicalPutUnlocked(TCServerMap map, Object key, Object value) {
    assertLockAndRejoinState();
    delegate.doLogicalPutUnlocked(map, key, value);
  }

  @Override
  public void doLogicalPutUnlockedVersioned(final TCServerMap map, final Object key, final Object value, final long version) {
    assertLockAndRejoinState();
    delegate.doLogicalPutUnlockedVersioned(map, key, value, version);
  }

  @Override
  public Object getValueUnlocked(TCServerMap map, Object key) throws AbortedOperationException {
    assertLockAndRejoinState();
    return delegate.getValueUnlocked(map, key);
  }

  @Override
  public Map<Object, Object> getAllValuesUnlocked(final SetMultimap<ObjectID, Object> mapIdToKeysMap) throws AbortedOperationException {
    assertLockAndRejoinState();
    return delegate.getAllValuesUnlocked(mapIdToKeysMap);
  }

  @Override
  public Set keySet(TCServerMap map) throws AbortedOperationException {
    assertLockAndRejoinState();
    return delegate.keySet(map);
  }

  @Override
  public Object getValue(TCServerMap map, Object lockID, Object key) throws AbortedOperationException {
    assertLockAndRejoinState();
    return delegate.getValue(map, lockID, key);
  }

  @Override
  public long getAllSize(TCServerMap[] maps) throws AbortedOperationException {
    assertLockAndRejoinState();
    return delegate.getAllSize(maps);
  }

  @Override
  public int getLocalSize() {
    return delegate.getLocalSize();
  }

  @Override
  public long getLocalOnHeapSizeInBytes() {
    return delegate.getLocalOnHeapSizeInBytes();
  }

  @Override
  public long getLocalOffHeapSizeInBytes() {
    return delegate.getLocalOffHeapSizeInBytes();
  }

  @Override
  public int getLocalOnHeapSize() {
    return delegate.getLocalOnHeapSize();
  }

  @Override
  public int getLocalOffHeapSize() {
    return delegate.getLocalOffHeapSize();
  }

  @Override
  public void clearLocalCache(TCServerMap map) {
    delegate.clearLocalCache(map);
  }

  @Override
  public void cleanLocalState() {
    delegate.cleanLocalState();
  }

  @Override
  public void clearAllLocalCacheInline() {
    delegate.clearAllLocalCacheInline();
  }

  @Override
  public void evictedInServer(Object key) {
    delegate.evictedInServer(key);
  }

  @Override
  public Set getLocalKeySet() {
    return delegate.getLocalKeySet();
  }

  @Override
  public boolean containsLocalKey(Object key) {
    return delegate.containsLocalKey(key);
  }

  @Override
  public boolean containsKeyLocalOnHeap(Object key) {
    return delegate.containsKeyLocalOnHeap(key);
  }

  @Override
  public boolean containsKeyLocalOffHeap(Object key) {
    return delegate.containsKeyLocalOffHeap(key);
  }

  @Override
  public Object getValueFromLocalCache(Object key) {
    return delegate.getValueFromLocalCache(key);
  }

  @Override
  public void removeValueFromLocalCache(Object key) {
    delegate.removeValueFromLocalCache(key);
  }

  @Override
  public void addMetaData(MetaDataDescriptor mdd) {
    delegate.addMetaData(mdd);
  }

  @Override
  public void setupLocalStore(L1ServerMapLocalCacheStore serverMapLocalStore, PinnedEntryFaultCallback callback) {
    delegate.setupLocalStore(serverMapLocalStore, callback);
  }

  @Override
  public void destroyLocalStore() {
    delegate.destroyLocalStore();
  }

  @Override
  public void setLocalCacheEnabled(boolean enabled) {
    delegate.setLocalCacheEnabled(enabled);
  }

  @Override
  public void recalculateLocalCacheSize(Object key) {
    delegate.recalculateLocalCacheSize(key);
  }

  @Override
  public void doLogicalSetLastAccessedTime(Object key, Object value, long lastAccessedTime) {
    assertLockAndRejoinState();
    delegate.doLogicalSetLastAccessedTime(key, value, lastAccessedTime);
  }

  @Override
  public void doLogicalExpire(final Object lockID, final Object key, final Object value) {
    assertLockAndRejoinState();
    delegate.doLogicalExpire(lockID, key, value);
  }

  @Override
  public boolean doLogicalExpireUnlocked(final TCServerMap map, final Object key, final Object value) {
    assertLockAndRejoinState();
    return delegate.doLogicalExpireUnlocked(map, key, value);
  }

  @Override
  public void addTxnInProgressKeys(Set addSet, Set removeSet) {
    delegate.addTxnInProgressKeys(addSet, removeSet);
  }

  @Override
  public VersionedObject getVersionedValue(TCServerMap map, Object key) throws AbortedOperationException {
    assertLockAndRejoinState();
    return delegate.getVersionedValue(map, key);
  }

  @Override
  public Map<Object, VersionedObject> getAllVersioned(final SetMultimap<ObjectID, Object> mapIdToKeysMap) throws AbortedOperationException {
    assertLockAndRejoinState();
    return delegate.getAllVersioned(mapIdToKeysMap);
  }

  @Override
  public void doClearVersioned() {
    assertLockAndRejoinState();
    delegate.doClearVersioned();
  }

  @Override
  public void doRegisterListener(Set eventTypes, boolean skipRejoinChecks) {
    if (!skipRejoinChecks) {
      assertLockAndRejoinState();
    }
    delegate.doRegisterListener(eventTypes, skipRejoinChecks);
  }

  @Override
  public void doUnregisterListener(Set eventTypes) {
    assertLockAndRejoinState();
    delegate.doUnregisterListener(eventTypes);
  }

}
