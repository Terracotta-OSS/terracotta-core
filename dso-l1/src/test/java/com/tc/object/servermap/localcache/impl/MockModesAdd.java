/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.object.ObjectID;
import com.tc.object.TCObjectSelfStore;
import com.tc.object.locks.LockID;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.LocalCacheStoreEventualValue;
import com.tc.object.servermap.localcache.LocalCacheStoreIncoherentValue;
import com.tc.object.servermap.localcache.LocalCacheStoreStrongValue;
import com.tc.object.servermap.localcache.MapOperationType;
import com.tc.object.servermap.localcache.ServerMapLocalCache;

import java.util.Random;

public class MockModesAdd {

  private static final int BYTE_ARRAY_LENGTH = 32;
  private static Random    random            = new Random();

  static {
    random.setSeed(System.currentTimeMillis());
  }

  // final mappings
  // key -> (id, mapOid, valueOid)
  // lockId -> List<key>
  // oid -> value
  public static void addStrongValueToCache(ServerMapLocalCache cache, TCObjectSelfStore store, String key,
                                           LockID lockID, MockSerializedEntry value, ObjectID mapID,
                                           MapOperationType operationType) {
    Object valueToAdd = value != null ? value.getObjectID() : value;
    AbstractLocalCacheStoreValue localStoreValue = new LocalCacheStoreStrongValue(lockID, valueToAdd, mapID);
    addToCache(cache, store, key, value, operationType, localStoreValue);
  }

  // final mappings
  // key -> (id, mapOid, value)
  // valueOid -> List<key>
  public static void addEventualValueToCache(ServerMapLocalCache cache, TCObjectSelfStore store, String key,
                                             MockSerializedEntry value, ObjectID mapID, MapOperationType operationType) {
    ObjectID valueID = value != null ? value.getObjectID() : ObjectID.NULL_ID;
    AbstractLocalCacheStoreValue localStoreValue = new LocalCacheStoreEventualValue(valueID, value, mapID);
    addToCache(cache, store, key, value, operationType, localStoreValue);
  }

  // final mappings
  // key -> (null, mapOid, valueOid)
  // oid -> value
  public static void addIncoherentValueToCache(ServerMapLocalCache cache, TCObjectSelfStore store, String key,
                                               MockSerializedEntry value, ObjectID mapID, MapOperationType operationType) {
    Object valueToAdd = value != null ? value.getObjectID() : value;
    AbstractLocalCacheStoreValue localStoreValue = new LocalCacheStoreIncoherentValue(valueToAdd, mapID);
    addToCache(cache, store, key, value, operationType, localStoreValue);
  }

  private static void addToCache(ServerMapLocalCache cache, TCObjectSelfStore store, String key,
                                 MockSerializedEntry value, MapOperationType operationType,
                                 AbstractLocalCacheStoreValue localStoreValue) {
    store.addTCObjectSelf(cache.getInternalStore(), localStoreValue, value, operationType.isMutateOperation());
    cache.addToCache(key, value, localStoreValue, operationType);
  }

  public static MockSerializedEntry createMockSerializedEntry(int oid) {
    return createMockSerializedEntry(oid, getRandomlyFilledByteArray());
  }

  public static MockSerializedEntry createMockSerializedEntry(int oid, byte[] array) {
    MockSerializedEntry entry = new MockSerializedEntry(new ObjectID(oid), array, null);
    return entry;
  }

  private static byte[] getRandomlyFilledByteArray() {
    byte[] array = new byte[BYTE_ARRAY_LENGTH];
    random.nextBytes(array);
    return array;
  }

}
