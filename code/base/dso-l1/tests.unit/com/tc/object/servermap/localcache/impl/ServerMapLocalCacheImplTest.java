/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import org.mockito.Mockito;

import com.tc.async.api.AddPredicate;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.exception.ImplementMe;
import com.tc.invalidation.Invalidations;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LocksRecallService;
import com.tc.object.locks.LongLockID;
import com.tc.object.locks.Notify;
import com.tc.object.metadata.MetaDataDescriptorInternal;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStoreListener;
import com.tc.object.servermap.localcache.LocalCacheStoreEventualValue;
import com.tc.object.servermap.localcache.LocalCacheStoreIncoherentValue;
import com.tc.object.servermap.localcache.LocalCacheStoreStrongValue;
import com.tc.object.servermap.localcache.MapOperationType;
import com.tc.object.tx.ClientTransaction;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.object.tx.TransactionCompleteListener;
import com.tc.object.tx.TransactionContext;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnType;
import com.tc.stats.Stats;
import com.tc.util.ObjectIDSet;
import com.tc.util.SequenceID;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;
import junit.framework.TestCase;

public class ServerMapLocalCacheImplTest extends TestCase {
  private volatile ServerMapLocalCacheImpl  cache;
  private final ObjectID                    mapID       = new ObjectID(50000);
  private int                               maxInMemory = 1000;
  private L1ServerMapLocalCacheStore        cacheIDStore;
  private TestLocksRecallHelper             locksRecallHelper;
  private L1ServerMapLocalCacheManagerImpl  globalLocalCacheManager;
  private MySink                            sink;
  private L1ServerMapLocalCacheStoreHashMap localCacheStore;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    setLocalCache(null, null, maxInMemory);
  }

  public void setLocalCache(CountDownLatch latch1, CountDownLatch latch2, int maxElementsInMemory) {
    setLocalCache(latch1, latch2, maxElementsInMemory, new TestLocksRecallHelper());
  }

  public void setLocalCache(CountDownLatch latch1, CountDownLatch latch2, int maxElementsInMemory,
                            TestLocksRecallHelper testLocksRecallHelper) {
    locksRecallHelper = testLocksRecallHelper;
    maxInMemory = maxElementsInMemory;
    sink = new MySink();
    globalLocalCacheManager = new L1ServerMapLocalCacheManagerImpl(locksRecallHelper, sink, Mockito.mock(Sink.class));
    sink.setGlobalLocalCacheManager(globalLocalCacheManager);
    locksRecallHelper.setGlobalLocalCacheManager(globalLocalCacheManager);
    final ClientTransaction clientTransaction = new MyClientTransaction(latch1, latch2);
    ClientObjectManager com = Mockito.mock(ClientObjectManager.class);
    ClientTransactionManager ctm = Mockito.mock(ClientTransactionManager.class);
    Mockito.when(com.getTransactionManager()).thenReturn(ctm);
    Mockito.when(ctm.getCurrentTransaction()).thenReturn(clientTransaction);
    cache = (ServerMapLocalCacheImpl) globalLocalCacheManager.getOrCreateLocalCache(mapID, com, null, true, null);
    localCacheStore = new L1ServerMapLocalCacheStoreHashMap(maxElementsInMemory);
    cache.setupLocalStore(localCacheStore);
    cacheIDStore = cache.getL1ServerMapLocalCacheStore();
  }

  public void testGetMapID() throws Exception {
    Assert.assertEquals(mapID, cache.getMapID());
  }

  private void assertValueType(AbstractLocalCacheStoreValue value, LocalCacheValueType type) {
    for (LocalCacheValueType t : LocalCacheValueType.values()) {
      if (t == type) {
        Assert.assertTrue(t.isValueOfType(value));
      } else {
        Assert.assertFalse(t.isValueOfType(value));
      }
    }
  }

  private void assertEventualValue(Object expectedValue, ObjectID expectedObjectId, AbstractLocalCacheStoreValue value) {
    assertValueType(value, LocalCacheValueType.EVENTUAL);
    Assert.assertTrue(value instanceof LocalCacheStoreEventualValue);
    try {
      value.asEventualValue();
    } catch (Throwable t) {
      Assert.fail("Should be able to retrieve value as Eventual value: " + t);
    }
    try {
      value.asStrongValue();
      fail("Should have failed");
    } catch (ClassCastException ignored) {
      // expected
    }
    try {
      value.asIncoherentValue();
      fail("Should have failed");
    } catch (ClassCastException ignored) {
      // expected
    }

    Assert.assertEquals(expectedValue, value.getValueObject(globalLocalCacheManager, this.localCacheStore));
    Assert.assertEquals(expectedObjectId, value.getId());
    Assert.assertEquals(expectedObjectId, value.asEventualValue().getObjectId());
  }

  private void assertIncoherentValue(Object expectedValue, AbstractLocalCacheStoreValue value) {
    assertValueType(value, LocalCacheValueType.INCOHERENT);
    Assert.assertTrue(value instanceof LocalCacheStoreIncoherentValue);
    try {
      value.asIncoherentValue();
    } catch (Throwable t) {
      Assert.fail("Should be able to retrieve value as Incoherent value: " + t);
    }
    try {
      value.asStrongValue();
      fail("Should have failed");
    } catch (ClassCastException ignored) {
      // expected
    }
    try {
      value.asEventualValue();
      fail("Should have failed");
    } catch (ClassCastException ignored) {
      // expected
    }
    Assert.assertEquals(expectedValue, value.getValueObject(globalLocalCacheManager, this.localCacheStore));
    Assert.assertEquals(null, value.getId());
  }

  private void assertStrongValue(Object expectedValue, LockID expectedLockId, AbstractLocalCacheStoreValue value) {
    assertValueType(value, LocalCacheValueType.STRONG);
    Assert.assertTrue(value instanceof LocalCacheStoreStrongValue);
    try {
      value.asStrongValue();
    } catch (Throwable t) {
      Assert.fail("Should be able to retrieve value as Strong value: " + t);
    }
    try {
      value.asIncoherentValue();
      fail("Should have failed");
    } catch (ClassCastException ignored) {
      // expected
    }
    try {
      value.asEventualValue();
      fail("Should have failed");
    } catch (ClassCastException ignored) {
      // expected
    }
    Assert.assertEquals(expectedValue, value.getValueObject(globalLocalCacheManager, this.localCacheStore));
    Assert.assertEquals(expectedLockId, value.getId());
    Assert.assertEquals(expectedLockId, value.asStrongValue().getLockId());
  }

  public void testAddEventualValueToCache() throws Exception {

    for (int i = 0; i < 50; i++) {
      addEventualValueToCache(cache, new ObjectID(i), "key" + i, "value" + i, MapOperationType.PUT);
    }

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
    }

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
    }

    for (int i = 0; i < 50; i++) {
      List list = (List) cacheIDStore.get(new ObjectID(i));
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("key" + i, list.get(0));
    }

    Assert.assertEquals(50, cache.size());
  }

  public void testAddEventualValueRemove1() throws Exception {
    for (int i = 0; i < 50; i++) {
      addEventualValueToCache(cache, new ObjectID(i), "key" + i, "value" + i, MapOperationType.PUT);
    }

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
    }

    Assert.assertEquals(50, cache.size());

    // REMOVE
    for (int i = 0; i < 25; i++) {
      addEventualValueToCache(cache, ObjectID.NULL_ID, "key" + i, null, MapOperationType.REMOVE);
    }

    for (int i = 0; i < 25; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      Assert.assertNull(value);
      Assert.assertNull(cacheIDStore.get(new ObjectID(i)));
    }

    for (int i = 25; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
    }

    for (int i = 25; i < 50; i++) {
      List list = (List) cacheIDStore.get(new ObjectID(i));
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("key" + i, list.get(0));
    }

    Assert.assertEquals(25, cacheIDStore.size());
  }

  public void testAddEventualValueRemove2() throws Exception {
    CountDownLatch latch1 = new CountDownLatch(1);
    CountDownLatch latch2 = new CountDownLatch(1);
    setLocalCache(latch1, latch2, this.maxInMemory);

    // GET - add to the local cache
    addEventualValueToCache(cache, new ObjectID(1), "key1", "value1", MapOperationType.GET);
    AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key1");
    assertEventualValue("value1", new ObjectID(1), value);
    List list = (List) cacheIDStore.get(new ObjectID(1));
    Assert.assertEquals(1, list.size());
    Assert.assertEquals("key1", list.get(0));

    // REMOVE
    addEventualValueToCache(cache, ObjectID.NULL_ID, "key1", null, MapOperationType.REMOVE);

    value = cache.getCoherentLocalValue("key1");
    Assert.assertEquals(null, value.getValueObject(globalLocalCacheManager, this.localCacheStore));
    Assert.assertEquals(ObjectID.NULL_ID, value.asEventualValue().getId());
    Assert.assertNull(cacheIDStore.get(new ObjectID(1)));

    latch1.countDown();
    latch2.await();
    Assert.assertEquals(0, cache.size());

    value = cache.getCoherentLocalValue("key1");
    Assert.assertNull(value);
    Assert.assertNull(cacheIDStore.get(new ObjectID(1)));
  }

  public void testAddIncoherentValueToCache() throws Exception {
    for (int i = 0; i < 50; i++) {
      addIncoherentValueToCache(cache, "key" + i, "value" + i, MapOperationType.PUT);
    }

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getLocalValue("key" + i);
      assertIncoherentValue("value" + i, value);
    }
    Assert.assertEquals(50, cache.size());

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      Assert.assertNull(value);
    }
    Assert.assertEquals(0, cache.size());
  }

  private void addToCache(ServerMapLocalCacheImpl localCache, String key, AbstractLocalCacheStoreValue storeValue,
                          String value, MapOperationType op) {
    localCache.addToCache(key, storeValue, op);
    globalLocalCacheManager.addTCObjectSelf(localCacheStore, storeValue, value);
  }

  private void addEventualValueToCache(ServerMapLocalCacheImpl localCache, ObjectID objectID, String key, String value,
                                       MapOperationType op) {
    LocalCacheStoreEventualValue storeValue = new LocalCacheStoreEventualValue(objectID, value, mapID);
    addToCache(localCache, key, storeValue, value, op);
  }

  private void addStrongValueToCache(ServerMapLocalCacheImpl localCache, LongLockID longLockID, String key,
                                     String value, MapOperationType op) {
    LocalCacheStoreStrongValue storeValue = new LocalCacheStoreStrongValue(longLockID, value, mapID);
    addToCache(localCache, key, storeValue, value, op);
  }

  private void addIncoherentValueToCache(ServerMapLocalCacheImpl localCache, String key, String value,
                                         MapOperationType op) {
    LocalCacheStoreIncoherentValue storeValue = new LocalCacheStoreIncoherentValue(value, mapID);
    addToCache(localCache, key, storeValue, value, op);
  }

  public void testFlush() throws Exception {
    for (int i = 0; i < 50; i++) {
      addEventualValueToCache(cache, new ObjectID(i), "key" + i, "value" + i, MapOperationType.PUT);
    }

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
    }

    // Flush
    for (int i = 0; i < 25; i++) {
      cache.removeEntriesForObjectId(new ObjectID(i));
    }

    for (int i = 0; i < 25; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      Assert.assertNull(value);
      Assert.assertNull(cacheIDStore.get(new ObjectID(i)));
    }

    for (int i = 25; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
    }

    for (int i = 25; i < 50; i++) {
      List list = (List) cacheIDStore.get(new ObjectID(i));
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("key" + i, list.get(0));
    }

  }

  public void testRecalledLocks() throws Exception {
    for (int i = 0; i < 50; i++) {
      addStrongValueToCache(cache, new LongLockID(i), "key" + i, "value" + i, MapOperationType.PUT);
    }

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertStrongValue("value" + i, new LongLockID(i), value);
      List list = (List) cacheIDStore.get(new LongLockID(i));
      Assert.assertNotNull(list);
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("key" + i, list.get(0));
    }

    Set<LockID> evictLocks = new HashSet<LockID>();
    for (int i = 0; i < 25; i++) {
      LockID lockID = new LongLockID(i);
      evictLocks.add(lockID);
      globalLocalCacheManager.removeEntriesForLockId(lockID);
    }

    for (int i = 0; i < 25; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      Assert.assertNull(value);
      Assert.assertNull(cacheIDStore.get(new LongLockID(i)));
    }

    for (int i = 25; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertStrongValue("value" + i, new LongLockID(i), value);
    }

    for (int i = 25; i < 50; i++) {
      List list = (List) cacheIDStore.get(new LongLockID(i));
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("key" + i, list.get(0));
    }
  }

  public void testRemovedEntries() throws Exception {
    Test2LocksRecallHelper test2LocksRecallHelper = new Test2LocksRecallHelper();
    setLocalCache(null, null, maxInMemory, test2LocksRecallHelper);

    for (int i = 0; i < 50; i++) {
      addStrongValueToCache(cache, new LongLockID(i), "key" + i, "value" + i, MapOperationType.PUT);
    }

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertStrongValue("value" + i, new LongLockID(i), value);
      List list = (List) cacheIDStore.get(new LongLockID(i));
      Assert.assertNotNull(list);
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("key" + i, list.get(0));
    }

    Set<LockID> evictLocks = new HashSet<LockID>();
    for (int i = 0; i < 25; i++) {
      LockID lockID = new LongLockID(i);
      evictLocks.add(lockID);
      cache.removeFromLocalCache("key" + i);
    }

    Assert.assertEquals(evictLocks, test2LocksRecallHelper.lockIdsToEvict);

    for (int i = 0; i < 25; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      Assert.assertNull(value);
      Assert.assertNull(cacheIDStore.get(new LongLockID(i)));
    }

    for (int i = 25; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertStrongValue("value" + i, new LongLockID(i), value);
    }

    for (int i = 25; i < 50; i++) {
      List list = (List) cacheIDStore.get(new LongLockID(i));
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("key" + i, list.get(0));
    }
  }

  public void testPinEntry() throws Exception {
    // CountDownLatch latch1 = new CountDownLatch(1);
    // CountDownLatch latch2 = new CountDownLatch(1);
    // setLocalCache(latch1, latch2, 10);
    //
    // for (int i = 0; i < 50; i++) {
    // addStrongValueToCache(cache, new LongLockID(i), "key" + i, "value" + i, MapOperationType.PUT);
    // }
    //
    // Assert.assertEquals(50, cache.size());
    //
    // ThreadUtil.reallySleep(10 * 1000);
    //
    // Assert.assertEquals(50, cache.size());
    //
    // latch1.countDown();
    // latch2.await();
  }

  public void testUnpinEntry() throws Exception {
    // int noOfElements = 50;
    //
    // CountDownLatch latch1 = new CountDownLatch(1);
    // CountDownLatch latch2 = new CountDownLatch(1);
    // setLocalCache(latch1, latch2, 10);
    //
    // for (int i = 0; i < noOfElements; i++) {
    // addStrongValueToCache(cache, new LongLockID(i), "key" + i, "value" + i, MapOperationType.PUT);
    // }
    //
    // Assert.assertEquals(noOfElements, cache.size());
    //
    // ThreadUtil.reallySleep(10 * 1000);
    //
    // Assert.assertEquals(50, cache.size());
    //
    // latch1.countDown();
    // latch2.await();
    //
    // ThreadUtil.reallySleep(10 * 1000);
    // addStrongValueToCache(cache, new LongLockID(50), "key" + noOfElements, "value" + noOfElements,
    // MapOperationType.PUT);
    //
    // ThreadUtil.reallySleep(10 * 1000);
    // Assert.assertTrue(cache.size() < 10);
  }

  public void testEvictFromLocalCache() throws Exception {
    for (int i = 0; i < 50; i++) {
      addEventualValueToCache(cache, new ObjectID(i), "key" + i, "value" + i, MapOperationType.PUT);
    }

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
    }

    for (int i = 0; i < 25; i++) {
      cache.removeFromLocalCache("key" + i);
    }

    for (int i = 0; i < 25; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      Assert.assertNull(value);
      Assert.assertNull(cacheIDStore.get(new ObjectID(i)));
    }

    for (int i = 25; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
    }

    for (int i = 25; i < 50; i++) {
      List list = (List) cacheIDStore.get(new ObjectID(i));
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("key" + i, list.get(0));
    }
  }

  public void testAddAllObjectIDsToValidate() throws Exception {
    for (int i = 0; i < 50; i++) {
      addStrongValueToCache(cache, new LongLockID(i), "key" + i, "value" + i, MapOperationType.PUT);
    }

    Invalidations invalidations = new Invalidations();
    cache.addAllObjectIDsToValidate(invalidations);

    Assert.assertEquals(0, invalidations.size());

    for (int i = 50; i < 100; i++) {
      addEventualValueToCache(cache, new ObjectID(i), "key" + i, "value" + i, MapOperationType.PUT);
    }
    cache.addAllObjectIDsToValidate(invalidations);

    Assert.assertEquals(50, invalidations.size());

    ObjectIDSet set = invalidations.getObjectIDSetForMapId(this.mapID);
    Assert.assertEquals(50, set.size());
    for (int i = 50; i < 100; i++) {
      Assert.assertTrue(set.contains(new ObjectID(i)));
    }
  }

  public void testSize() throws Exception {
    for (int i = 0; i < 50; i++) {
      addEventualValueToCache(cache, new ObjectID(i), "key" + i, "value" + i, MapOperationType.PUT);
    }

    Assert.assertEquals(50, cache.size());
  }

  public void testClearAllLocalCache() throws Exception {
    Assert.assertEquals(0, cache.size());
    int n = 1;
    for (int i = 0; i < n; i++) {
      addEventualValueToCache(cache, new ObjectID(i), "key" + i, "value" + i, MapOperationType.PUT);
    }

    cache.clear();

    for (int i = 0; i < n; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      Assert.assertNull(value);
      Object object = cacheIDStore.get(new ObjectID(i));
      Assert.assertNull("Not null - " + object, object);
    }
  }

  public void testRemoveFromLocalCache() throws Exception {
    for (int i = 0; i < 50; i++) {
      addEventualValueToCache(cache, new ObjectID(i), "key" + i, "value" + i, MapOperationType.PUT);
    }

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
    }

    for (int i = 0; i < 25; i++) {
      cache.removeFromLocalCache("key" + i);
    }

    for (int i = 0; i < 25; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      Assert.assertNull(value);
      Assert.assertNull(cacheIDStore.get(new ObjectID(i)));
    }

    for (int i = 25; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
    }

    for (int i = 25; i < 50; i++) {
      List list = (List) cacheIDStore.get(new ObjectID(i));
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("key" + i, list.get(0));
    }
  }

  public void testEvictCachedEntries() throws Exception {
    for (int i = 0; i < 50; i++) {
      addEventualValueToCache(cache, new ObjectID(i), "key" + i, "value" + i, MapOperationType.PUT);
    }
    cache.evictCachedEntries(25);
    System.err.println("Current size in testEvictCachedEntries " + cache.size());
    System.err.println(localCacheStore.toString());
    System.err.println(cacheIDStore.toString());
    int evicted = 0;
    int notEvicted = 0;
    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      if (value != null) {
        assertEventualValue("value" + i, new ObjectID(i), value);
        List list = (List) cacheIDStore.get(new ObjectID(i));
        Assert.assertEquals(1, list.size());
        Assert.assertEquals("key" + i, list.get(0));
        notEvicted++;
      } else {
        Object object = cacheIDStore.get(new ObjectID(i));
        Assert.assertNull("Not null - " + object, object);
        evicted++;
      }
    }

    Assert.assertEquals(25, evicted);
    Assert.assertEquals(25, notEvicted);
  }

  public void testGetKeySet() throws Exception {
    int count = 50;
    for (int i = 0; i < count; i++) {
      int eventualId = count + i;
      addStrongValueToCache(cache, new LongLockID(i), "key" + i, "value" + i, MapOperationType.PUT);
      addEventualValueToCache(cache, new ObjectID(eventualId), "key" + eventualId, "value" + eventualId,
                              MapOperationType.PUT);
    }

    for (int i = 0; i < count; i++) {
      int eventualId = count + i;
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertStrongValue("value" + i, new LongLockID(i), value);
      List list = (List) cacheIDStore.get(new LongLockID(i));
      Assert.assertNotNull(list);
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("key" + i, list.get(0));

      value = cache.getCoherentLocalValue("key" + eventualId);
      assertEventualValue("value" + eventualId, new ObjectID(eventualId), value);
      list = (List) cacheIDStore.get(new ObjectID(eventualId));
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("key" + eventualId, list.get(0));
    }

    Assert.assertEquals(2 * count, cache.size());
    Set keySet = cache.getKeySet();
    Iterator iterator = keySet.iterator();
    Assert.assertEquals(2 * count, keySet.size());
    Set keysFromKeySet = new HashSet();
    while (iterator.hasNext()) {
      Object key = iterator.next();
      keysFromKeySet.add(key);
      try {
        iterator.remove();
        fail("iterator.remove() should throw unsupported operation exception");
      } catch (UnsupportedOperationException e) {
        // expected
      }
    }

    try {
      iterator.next();
      fail("Calling next after iteration should throw NoSuchElementException");
    } catch (NoSuchElementException e) {
      System.out.println("Caught expected exception: " + e);
    }

    Assert.assertEquals(2 * count, keysFromKeySet.size());
    Set expectedKeys = new HashSet();
    for (int i = 0; i < count; i++) {
      int eventualId = count + i;
      expectedKeys.add("key" + i);
      expectedKeys.add("key" + eventualId);

      Assert.assertTrue(keysFromKeySet.contains("key" + i));
      Assert.assertTrue(keysFromKeySet.contains("key" + eventualId));

      Assert.assertTrue(keySet.contains("key" + i));
      Assert.assertTrue(keySet.contains("key" + eventualId));
    }

    keySet = cache.getKeySet();
    try {
      keySet.add(new Object());
      fail("Adding to keyset should fail");
    } catch (UnsupportedOperationException e) {
      // expected
      System.out.println("Caught expected exception: " + e);
    }
    try {
      keySet.remove(new Object());
      fail("Remove from keyset should fail");
    } catch (UnsupportedOperationException e) {
      // expected
      System.out.println("Caught expected exception: " + e);
    }
    iterator = keySet.iterator();
    // calling hasNext multiple times
    for (int i = 0; i < 20 * count; i++) {
      Assert.assertTrue(iterator.hasNext());
    }
    // iterating without hasNext()
    keysFromKeySet = new HashSet();
    for (int i = 0; i < 2 * count; i++) {
      keysFromKeySet.add(iterator.next());
    }
    Assert.assertEquals(expectedKeys, keysFromKeySet);

    try {
      iterator.next();
      fail("Calling next after iteration should throw NoSuchElementException");
    } catch (NoSuchElementException e) {
      System.out.println("Caught expected exception: " + e);
    }

    final int half = count / 2;
    for (int i = 0; i < half; i++) {
      int eventualId = count + i;
      LockID lockID = new LongLockID(i);
      ObjectID objectId = new ObjectID(eventualId);
      globalLocalCacheManager.removeEntriesForLockId(lockID);
      globalLocalCacheManager.removeEntriesForObjectId(mapID, Collections.singleton(objectId));
    }

    for (int i = 0; i < half; i++) {
      int eventualId = count + i;

      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      Assert.assertNull(value);
      Assert.assertNull(cacheIDStore.get(new LongLockID(i)));

      value = cache.getCoherentLocalValue("key" + eventualId);
      Assert.assertNull(value);
      Assert.assertNull(cacheIDStore.get(new ObjectID(eventualId)));
    }

    Assert.assertEquals(count, cache.size());
    keySet = cache.getKeySet();
    iterator = keySet.iterator();
    Assert.assertEquals(count, keySet.size());
    keysFromKeySet = new HashSet();
    while (iterator.hasNext()) {
      Object key = iterator.next();
      keysFromKeySet.add(key);
    }
    try {
      iterator.next();
      fail("Calling next after iteration should throw NoSuchElementException");
    } catch (NoSuchElementException e) {
      System.out.println("Caught expected exception: " + e);
    }
    Assert.assertEquals(count, keysFromKeySet.size());
    for (int i = half; i < count; i++) {
      int eventualId = count + i;
      Assert.assertTrue(keysFromKeySet.contains("key" + i));
      Assert.assertTrue(keysFromKeySet.contains("key" + eventualId));

      Assert.assertTrue(keySet.contains("key" + i));
      Assert.assertTrue(keySet.contains("key" + eventualId));
    }

    for (int i = half; i < count; i++) {
      int eventualId = count + i;
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertStrongValue("value" + i, new LongLockID(i), value);
      List list = (List) cacheIDStore.get(new LongLockID(i));
      Assert.assertNotNull(list);
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("key" + i, list.get(0));

      value = cache.getCoherentLocalValue("key" + eventualId);
      assertEventualValue("value" + eventualId, new ObjectID(eventualId), value);
      list = (List) cacheIDStore.get(new ObjectID(eventualId));
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("key" + eventualId, list.get(0));
    }
  }

  public void testGlobalLocalCacheManagerShutdown() {
    int count = 50;
    for (int i = 0; i < count; i++) {
      int eventualId = count + i;
      addStrongValueToCache(cache, new LongLockID(i), "key" + i, "value" + i, MapOperationType.PUT);
      addEventualValueToCache(cache, new ObjectID(eventualId), "key" + eventualId, "value" + eventualId,
                              MapOperationType.PUT);
    }

    for (int i = 0; i < count; i++) {
      int eventualId = count + i;
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertStrongValue("value" + i, new LongLockID(i), value);
      List list = (List) cacheIDStore.get(new LongLockID(i));
      Assert.assertNotNull(list);
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("key" + i, list.get(0));

      value = cache.getCoherentLocalValue("key" + eventualId);
      assertEventualValue("value" + eventualId, new ObjectID(eventualId), value);
      list = (List) cacheIDStore.get(new ObjectID(eventualId));
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("key" + eventualId, list.get(0));
    }

    Assert.assertEquals(2 * count, cache.size());

    globalLocalCacheManager.shutdown();
    for (int i = 0; i < count; i++) {
      int eventualId = count + i;

      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      Assert.assertNull(value);
      Assert.assertNull(cacheIDStore.get(new LongLockID(i)));

      value = cache.getCoherentLocalValue("key" + eventualId);
      Assert.assertNull(value);
      Assert.assertNull(cacheIDStore.get(new ObjectID(eventualId)));
    }

    Assert.assertEquals(0, cache.size());
    Set keySet = cache.getKeySet();
    Assert.assertFalse(keySet.iterator().hasNext());
  }

  public void testCapacityEviction() {
    int count = 45;
    int maxElementsInMemory = 40;
    setLocalCache(null, null, maxElementsInMemory);

    for (int i = 0; i < count; i++) {
      addStrongValueToCache(cache, new LongLockID(i), "key" + i, "value" + i, MapOperationType.GET);
    }
    int cacheSize = cache.size();
    System.err.println("Current size in testCapacityEviction " + cacheSize);

    L1ServerMapLocalCacheStoreHashMap store = (L1ServerMapLocalCacheStoreHashMap) cache.getL1ServerMapLocalCacheStore();
    List<L1ServerMapLocalCacheStoreListener> listeners = store.getListeners();
    for (L1ServerMapLocalCacheStoreListener l : listeners) {
      l.notifySizeChanged(store);
    }

    sink.waitUntitContextsAddedEqualsAndCompletedEquals(1, 1);

    cacheSize = cache.size();
    System.err.println("Current size in testCapacityEviction " + cacheSize);
    Assert.assertTrue(cacheSize < 40);
  }

  public class MyClientTransaction implements ClientTransaction {
    private final CountDownLatch latch1;
    private final CountDownLatch latch2;

    public MyClientTransaction(CountDownLatch latch1, CountDownLatch latch2) {
      this.latch1 = latch1;
      this.latch2 = latch2;
    }

    public void addDmiDescriptor(DmiDescriptor dd) {
      throw new ImplementMe();
    }

    public void addMetaDataDescriptor(TCObject tco, MetaDataDescriptorInternal md) {
      throw new ImplementMe();
    }

    public void addNotify(Notify notify) {
      throw new ImplementMe();
    }

    public void addTransactionCompleteListener(TransactionCompleteListener l) {
      if (latch1 == null) {
        callDefault(l);
      } else {
        callLatched(l);
      }
    }

    private void callDefault(TransactionCompleteListener l) {
      ThreadUtil.reallySleep(1);
      l.transactionComplete(null);
    }

    public void callLatched(final TransactionCompleteListener l) {
      Runnable runnable = new Runnable() {
        public void run() {
          try {
            latch1.await();
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }

          l.transactionComplete(null);

          latch2.countDown();
        }
      };

      Thread t = new Thread(runnable, "invoke txn complete");
      t.start();
    }

    public void arrayChanged(TCObject source, int startPos, Object array, int length) {
      throw new ImplementMe();

    }

    public void createObject(TCObject source) {
      throw new ImplementMe();

    }

    public void createRoot(String name, ObjectID rootID) {
      throw new ImplementMe();

    }

    public void fieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index) {
      throw new ImplementMe();

    }

    public List getAllLockIDs() {
      throw new ImplementMe();
    }

    public Map getChangeBuffers() {
      throw new ImplementMe();
    }

    public List getDmiDescriptors() {
      throw new ImplementMe();
    }

    public TxnType getEffectiveType() {
      throw new ImplementMe();
    }

    public LockID getLockID() {
      throw new ImplementMe();
    }

    public TxnType getLockType() {
      throw new ImplementMe();
    }

    public Map getNewRoots() {
      throw new ImplementMe();
    }

    public List getNotifies() {
      throw new ImplementMe();
    }

    public int getNotifiesCount() {
      throw new ImplementMe();
    }

    public Collection getReferencesOfObjectsInTxn() {
      throw new ImplementMe();
    }

    public SequenceID getSequenceID() {
      throw new ImplementMe();
    }

    public List getTransactionCompleteListeners() {
      throw new ImplementMe();
    }

    public TransactionID getTransactionID() {
      throw new ImplementMe();
    }

    public boolean hasChanges() {
      throw new ImplementMe();
    }

    public boolean hasChangesOrNotifies() {
      throw new ImplementMe();
    }

    public boolean isConcurrent() {
      throw new ImplementMe();
    }

    public boolean isNull() {
      throw new ImplementMe();
    }

    public void literalValueChanged(TCObject source, Object newValue, Object oldValue) {
      throw new ImplementMe();

    }

    public void logicalInvoke(TCObject source, int method, Object[] parameters, String methodName) {
      throw new ImplementMe();

    }

    public void setAlreadyCommitted() {
      throw new ImplementMe();

    }

    public void setSequenceID(SequenceID sequenceID) {
      throw new ImplementMe();

    }

    public void setTransactionContext(TransactionContext transactionContext) {
      throw new ImplementMe();

    }

    public void setTransactionID(TransactionID tid) {
      throw new ImplementMe();

    }

  }

  private static class TestLocksRecallHelper implements LocksRecallService {
    private volatile Set<LockID>                  lockIds;
    private volatile L1ServerMapLocalCacheManager globalLocalCacheManager;

    public void setGlobalLocalCacheManager(L1ServerMapLocalCacheManager globalLocalCacheManagerParam) {
      this.globalLocalCacheManager = globalLocalCacheManagerParam;
    }

    public void recallLocks(Set<LockID> locks) {
      this.lockIds = locks;
      for (LockID id : lockIds) {
        globalLocalCacheManager.removeEntriesForLockId(id);
      }
    }

    public void recallLocksInline(Set<LockID> locks) {
      this.lockIds = locks;
      for (LockID id : lockIds) {
        globalLocalCacheManager.removeEntriesForLockId(id);
      }
    }

  }

  private static class Test2LocksRecallHelper extends TestLocksRecallHelper {
    private volatile Set<LockID> lockIdsToEvict = new HashSet<LockID>();

    @Override
    public void recallLocks(Set<LockID> locks) {
      for (LockID id : locks) {
        lockIdsToEvict.add(id);
      }
    }

    @Override
    public void recallLocksInline(Set<LockID> locks) {
      for (LockID id : locks) {
        lockIdsToEvict.add(id);
      }
    }

  }

  public static enum LocalCacheValueType {
    STRONG() {

      @Override
      public boolean isValueOfType(AbstractLocalCacheStoreValue value) {
        return value.isStrongConsistentValue();
      }

    },
    EVENTUAL() {

      @Override
      public boolean isValueOfType(AbstractLocalCacheStoreValue value) {
        return value.isEventualConsistentValue();
      }
    },
    INCOHERENT() {

      @Override
      public boolean isValueOfType(AbstractLocalCacheStoreValue value) {
        return value.isIncoherentValue();
      }
    };

    public abstract boolean isValueOfType(AbstractLocalCacheStoreValue value);
  }

  private static class MySink implements Sink {
    private final LinkedBlockingQueue<EventContext> q                 = new LinkedBlockingQueue<EventContext>();
    private final AtomicInteger                     contextsAdded     = new AtomicInteger(0);
    private final AtomicInteger                     contextsCompleted = new AtomicInteger(0);
    private final Thread                            t1;
    final L1ServerMapCapacityEvictionHandler        handler;

    public MySink() {
      handler = new L1ServerMapCapacityEvictionHandler();
      Runnable runnable = new Runnable() {
        public void run() {
          while (true) {
            EventContext context;
            try {
              context = q.take();
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
            handler.handleEvent(context);
            contextsCompleted.incrementAndGet();
          }
        }
      };

      t1 = new Thread(runnable, "MySink thread");
    }

    public void setGlobalLocalCacheManager(L1ServerMapLocalCacheManager gloLocalCacheManager) {
      handler.initialize(gloLocalCacheManager);
      t1.start();
    }

    public void add(EventContext context) {
      try {
        q.put(context);
        contextsAdded.incrementAndGet();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    public void waitUntitContextsAddedEqualsAndCompletedEquals(int expectedContextsAdded, int expectedContextsCompleted) {
      waitUntilAtomicIntegerReaches("expectedContextsAdded", contextsAdded, expectedContextsAdded);
      waitUntilAtomicIntegerReaches("expectedContextsCompleted", contextsCompleted, expectedContextsCompleted);
    }

    private void waitUntilAtomicIntegerReaches(String integerString, AtomicInteger integer, int expected) {
      while (integer.get() < expected) {
        System.err.println("Sleep for 1 seconds for " + integerString + " Current values = " + integer.get()
                           + " expected = " + expected);
        ThreadUtil.reallySleep(1 * 1000);
      }
    }

    public boolean addLossy(EventContext context) {
      throw new ImplementMe();
    }

    public void addMany(Collection contexts) {
      throw new ImplementMe();
    }

    public void clear() {
      throw new ImplementMe();
    }

    public AddPredicate getPredicate() {
      throw new ImplementMe();
    }

    public void setAddPredicate(AddPredicate predicate) {
      throw new ImplementMe();
    }

    public int size() {
      throw new ImplementMe();
    }

    public void enableStatsCollection(boolean enable) {
      throw new ImplementMe();
    }

    public Stats getStats(long frequency) {
      throw new ImplementMe();
    }

    public Stats getStatsAndReset(long frequency) {
      throw new ImplementMe();
    }

    public boolean isStatsCollectionEnabled() {
      throw new ImplementMe();
    }

    public void resetStats() {
      throw new ImplementMe();
    }

  }

}
