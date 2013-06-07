/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.object.serialization;

import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.internal.cache.TimestampedValue;
import org.terracotta.toolkit.store.ToolkitConfigFields;

import com.tc.object.LocalCacheAddCallBack;
import com.tc.object.TCObject;
import com.tc.object.TCObjectSelf;
import com.tc.object.TCObjectSelfImpl;
import com.tc.object.TCObjectServerMap;
import com.tc.object.bytecode.Manageable;
import com.tc.object.servermap.ExpirableMapEntry;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.util.FindbugsSuppressWarnings;
import com.terracotta.toolkit.concurrent.locks.ToolkitLockingApi;
import com.terracotta.toolkit.rejoin.PlatformServiceProvider;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Storage wrapper for serialized cache entries.
 * <p>
 * This wrapper handles the de-serialization of the serialized cache entries, and also the local caching (in a transient
 * reference) of the de-serialized value.
 */
public class SerializedMapValue<T> extends TCObjectSelfImpl implements Externalizable, Manageable,
    LocalCacheAddCallBack, ExpirableMapEntry {

  private static final ToolkitLock   UPDATE_LAST_ACCESSED_TIME_CONCURRENT_LOCK = ToolkitLockingApi
                                                                                   .createConcurrentTransactionLock("servermap-update-last-accessed-time-concurrent-lock",
                                                                                                                    PlatformServiceProvider
                                                                                                                        .getPlatformService());

  private static final int           NEVER_EXPIRE                              = Integer.MAX_VALUE;
  /**
   * <pre>
   * ********************************************************************************************
   * IF YOU'RE CHANGING ANYTHING ABOUT THE FIELDS IN THIS CLASS (name, type, add or remove, etc)
   * YOU MUST UPDATE BOTH THE APPLICATOR AND SERVER STATE CLASSES ACCORDINGLY!
   * ********************************************************************************************
   * </pre>
   */
  private volatile byte[]            value;
  private volatile int               createTime;
  private volatile int               lastAccessedTime;

  private transient T                cached;
  private transient volatile boolean alreadyInCache                            = false;

  public SerializedMapValue() {
    // to make serialization happy
  }

  public SerializedMapValue(SerializedMapValueParameters<T> params) {
    this.value = params.getSerialized();
    this.createTime = params.getCreateTime();
    this.lastAccessedTime = params.getLastAccessedTime();
    if (value == null) { throw new AssertionError("Byte array value cannot be null"); }
  }

  @Override
  public void __tc_managed(TCObject t) {
    if (t != this) { throw new AssertionError(); }
  }

  @Override
  public TCObject __tc_managed() {
    if (oid == null) { return null; }
    return this;
  }

  @Override
  public boolean __tc_isManaged() {
    return oid != null;
  }

  /**
   * Return a new copy of this entry, newly deserialized from the serialized state.
   * 
   * @param strategy deserialization strategy
   * @return a newly deserialized entry
   * @throws IOException if de-serialization fails
   * @throws ClassNotFoundException if a necessary class definition is missing
   */
  public synchronized T getDeserializedValueCopy(final SerializationStrategy strategy, boolean compression,
                                                 boolean local) throws IOException, ClassNotFoundException {
    byte[] valueLocal = getValue();
    if (valueLocal == null) {
      if (cached == null) { throw new AssertionError("Cached value cannot be null when byte array is null"); }
      // TODO: fix not to case Serializable
      valueLocal = strategy.serialize(cached, compression);
    }
    T deserializedValue = (T) strategy.deserialize(valueLocal, compression, local);
    if (deserializedValue instanceof TimestampedValue) {
      ((TimestampedValue) deserializedValue).updateTimestamps(createTime, lastAccessedTime);
    }
    return deserializedValue;
  }

  /**
   * Return a copy of this entry, potentially reusing a cached copy.
   * 
   * @param strategy deserialization strategy
   * @param l1ServerMapLocalCacheStore
   * @return a newly deserialized entry
   * @throws IOException if de-serialization fails
   * @throws ClassNotFoundException if a necessary class definition is missing
   */
  public synchronized T getDeserializedValue(final SerializationStrategy strategy, boolean compression,
                                             L1ServerMapLocalCacheStore l1ServerMapLocalCacheStore, Object key,
                                             boolean local) throws IOException, ClassNotFoundException {
    T actualObject = this.cached;
    try {
      if (actualObject != null && value == null) { return actualObject; }

      if (actualObject == null) {
        byte[] bytes = getValue();
        if (bytes == null) { throw new AssertionError(
                                                      "bytes array is null for serializedEntry and not already cached - oid: "
                                                          + getObjectID()); }
        actualObject = (T) strategy.deserialize(bytes, compression, local);
        this.cached = actualObject;
      }

      if (this.alreadyInCache) {
        doNullByteArray();
        l1ServerMapLocalCacheStore.recalculateSize(key);
      }
      return actualObject;
    } finally {
      if (actualObject instanceof TimestampedValue) {
        ((TimestampedValue) actualObject).updateTimestamps(createTime, lastAccessedTime);
      }
    }
  }

  public int getCreateTime() {
    return this.createTime;
  }

  private synchronized byte[] getValue() {
    return this.value;
  }

  @FindbugsSuppressWarnings("DMI_INVOKING_TOSTRING_ON_ARRAY")
  @Override
  public String toString() {
    return "SerializedEntry [oid: " + getObjectID() + ", cached=" + cached + ", value=" + value + ", createTime="
           + createTime + ", lastAccessedTime=" + lastAccessedTime + ", alreadyInCache=" + alreadyInCache + "]";
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.serialize(out);
    out.writeInt(createTime);
    out.writeInt(lastAccessedTime);
    out.writeInt(value.length);
    out.write(value);
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException {
    super.deserialize(in);
    internalSetCreateTime(in.readInt());
    internalSetLastAccessedTime(in.readInt());
    int localLen = in.readInt();
    byte[] localValue = new byte[localLen];
    in.readFully(localValue);
    internalSetValue(localValue);
    this.alreadyInCache = true;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == null) return false;
    if (obj == this) { return true; }
    TCObjectSelf thisSelf = this;
    TCObjectSelf otherSelf = (TCObjectSelf) obj;

    if (thisSelf.getObjectID().equals(otherSelf.getObjectID())) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return getObjectID().hashCode();
  }

  /**
   * Discard the local copy of this entry's serialized state.
   */
  @Override
  public synchronized void addedToLocalCache() {
    this.alreadyInCache = true;
  }

  // called from within synchronized.
  private void doNullByteArray() {
    if (cached == null) {
      // no one has deserialized yet nor the put happened from this node, don't null byte[] yet
      return;
    }
    value = null;
  }

  public synchronized byte[] internalGetValue() {
    return value;
  }

  public int internalGetCreateTime() {
    return createTime;
  }

  public int internalGetLastAccessedTime() {
    return lastAccessedTime;
  }

  public void internalSetValue(byte[] valueParam) {
    this.value = valueParam;
  }

  public void internalSetCreateTime(int createTimeParam) {
    this.createTime = createTimeParam;
  }

  public void internalSetLastAccessedTime(Integer lastAccessedTimeParam) {
    this.lastAccessedTime = lastAccessedTimeParam;
  }

  public boolean isExpired(int atTime, int maxTTISeconds, int maxTTLSeconds) {
    return atTime >= calculateExpiresAt(maxTTISeconds, maxTTLSeconds);
  }

  protected int calculateExpiresAt(final int maxTTI, final int maxTTL) {
    if (maxTTI == ToolkitConfigFields.NO_MAX_TTI_SECONDS && maxTTL == ToolkitConfigFields.NO_MAX_TTL_SECONDS) { return NEVER_EXPIRE; }

    int expiresAtTTL;
    if (maxTTL == ToolkitConfigFields.NO_MAX_TTL_SECONDS || maxTTL < 0) {
      expiresAtTTL = NEVER_EXPIRE;
    } else {
      expiresAtTTL = getCreateTime() + maxTTL;
    }
    if (expiresAtTTL < 0) {
      expiresAtTTL = NEVER_EXPIRE;
    }

    int expiresAtTTI;
    if (maxTTI == ToolkitConfigFields.NO_MAX_TTI_SECONDS || maxTTI < 0) {
      expiresAtTTI = NEVER_EXPIRE;
    } else {
      expiresAtTTI = lastAccessedTime + maxTTI;
    }
    if (expiresAtTTI < 0) {
      expiresAtTTI = NEVER_EXPIRE;
    }

    // expires at time which comes earliest between TTI and TTL
    return Math.min(expiresAtTTI, expiresAtTTL);
  }

  public void updateLastAccessedTime(Object key, TCObjectServerMap tcObjectServerMap, int usedAtTime) {

    UPDATE_LAST_ACCESSED_TIME_CONCURRENT_LOCK.lock();
    try {
      synchronized (getResolveLock()) {
        this.lastAccessedTime = usedAtTime;
        tcObjectServerMap.doLogicalSetLastAccessedTime(key, getObjectID(), usedAtTime);
      }
    } finally {
      UPDATE_LAST_ACCESSED_TIME_CONCURRENT_LOCK.unlock();
    }
  }

  @Override
  public long getLastAccessedTime() {
    return lastAccessedTime;
  }

  @Override
  public void setLastAccessedTime(final long lastAccessedTime) {
    this.lastAccessedTime = (int) lastAccessedTime;
  }

  @Override
  public long getCreationTime() {
    return createTime;
  }

  @Override
  public void setCreationTime(final long creationTime) {
    this.createTime = (int) creationTime;
  }

  @Override
  public long getTimeToIdle() {
    return 0;
  }

  @Override
  public void setTimeToIdle(final long timeToIdle) {
    // Do nothing.
  }

  @Override
  public long getTimeToLive() {
    return 0;
  }

  @Override
  public void setTimeToLive(final long timeToLive) {
    // Do nothing.
  }
}
