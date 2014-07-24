/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.exceptions.verification.TooManyActualInvocations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.tc.async.api.AddPredicate;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.exception.ImplementMe;
import com.tc.invalidation.Invalidations;
import com.tc.net.GroupID;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.LogicalOperation;
import com.tc.object.TCObject;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerImpl;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LongLockID;
import com.tc.object.locks.Notify;
import com.tc.object.metadata.MetaDataDescriptorInternal;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.LocalCacheStoreEventualValue;
import com.tc.object.servermap.localcache.LocalCacheStoreStrongValue;
import com.tc.object.servermap.localcache.MapOperationType;
import com.tc.object.servermap.localcache.PinnedEntryFaultCallback;
import com.tc.object.servermap.localcache.ServerMapLocalCacheRemoveCallback;
import com.tc.object.tx.ClientTransaction;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.object.tx.OnCommitCallable;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;
import junit.framework.TestCase;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ServerMapLocalCacheImplTest extends TestCase {
  private volatile ServerMapLocalCacheImpl cache;
  private final ObjectID                   mapID                   = new ObjectID(50000);
  private int                              maxInMemory             = 1000;
  private L1ServerMapLocalCacheManagerImpl globalLocalCacheManager;
  private MySink                           sink;
  private L1ServerMapLocalCacheStore       localCacheStore;
  private MockTCObjectSelfCallback         mockTCObjectSelfCallback;
  private final Map<LockID, Long>          lockIDandAwardIDMapping = new HashMap();
  private Manager                          mng;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    setLocalCache(null, false, maxInMemory);
  }

  public void setLocalCache(CyclicBarrier barrier, boolean transactionAbort, int maxElementsInMemory) {
    maxInMemory = maxElementsInMemory;
    sink = new MySink();
    globalLocalCacheManager = new L1ServerMapLocalCacheManagerImpl(null, sink, new TxnCompleteSink(),
                                                                   Mockito.mock(Sink.class));
    mockTCObjectSelfCallback = new MockTCObjectSelfCallback();
    globalLocalCacheManager.initializeTCObjectSelfStore(mockTCObjectSelfCallback);
    sink.setGlobalLocalCacheManager(globalLocalCacheManager);
    final ClientTransaction clientTransaction = new MyClientTransaction(barrier, transactionAbort);
    ClientObjectManager com = Mockito.mock(ClientObjectManager.class);
    ClientTransactionManager ctm = Mockito.mock(ClientTransactionManager.class);
    Mockito.when(com.getTransactionManager()).thenReturn(ctm);
    Mockito.when(ctm.getCurrentTransaction()).thenReturn(clientTransaction);
    mng = Mockito.mock(ManagerImpl.class);
    Mockito.when(mng.isLockAwardValid(Matchers.any(LockID.class), Matchers.anyLong())).thenAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        return args[1].equals(lockIDandAwardIDMapping.get(args[0]));
      }
    });
    localCacheStore = new L1ServerMapLocalCacheStoreHashMap(maxElementsInMemory);
    cache = (ServerMapLocalCacheImpl) globalLocalCacheManager
        .getOrCreateLocalCache(mapID, com, mng, true, localCacheStore, Mockito.mock(PinnedEntryFaultCallback.class));
  }

  public void testDev6522() throws InterruptedException, BrokenBarrierException {
    CyclicBarrier barier = new CyclicBarrier(2);
    setLocalCache(barier, false, 100);

    MockModesAdd.addEventualValueToCache(cache, this.globalLocalCacheManager, "key1",
                                         createMockSerializedEntry("value" + 1, 1), mapID, MapOperationType.PUT);
    cache.evictedInServer("key1");
    Assert.assertNotNull(cache.getLocalValue("key1"));
    barier.await();
    barier.await();
  }

  public void testPinIfNecessory() throws Exception {
    CyclicBarrier barier = new CyclicBarrier(2);
    setLocalCache(barier, false, 10);
    long lockId = 100;
    long strongOid = 2 * lockId;
    String key = "key" + lockId;
    LockID lockOid = new LongLockID(lockId);
    MockSerializedEntry strongEntry = createMockSerializedEntry("valueStrong", strongOid);
    lockIDandAwardIDMapping.put(lockOid, 1L);
    MockModesAdd.addStrongValueToCacheWithAwardID(cache, this.globalLocalCacheManager, key, lockOid, strongEntry,
                                                  mapID, MapOperationType.PUT, 1L);
    Mockito.verify(mng, Mockito.times(1)).pinLock(lockOid, lockIDandAwardIDMapping.get(lockOid));
    barier.await();
    barier.await();
    Mockito.verify(mng, Mockito.times(2)).pinLock(lockOid, lockIDandAwardIDMapping.get(lockOid));
    Mockito.verify(mng, Mockito.times(1)).unpinLock(lockOid, lockIDandAwardIDMapping.get(lockOid));

    // should call pin again even with same lock id and award id (outgoing old value will unpin)
    MockModesAdd.addStrongValueToCacheWithAwardID(cache, this.globalLocalCacheManager, key, lockOid, strongEntry,
                                                  mapID, MapOperationType.PUT, 1L);
    Mockito.verify(mng, Mockito.times(3)).pinLock(lockOid, lockIDandAwardIDMapping.get(lockOid));
    barier.await();
    barier.await();
    Mockito.verify(mng, Mockito.times(4)).pinLock(lockOid, lockIDandAwardIDMapping.get(lockOid));
    Mockito.verify(mng, Mockito.times(3)).unpinLock(lockOid, lockIDandAwardIDMapping.get(lockOid));
    // should pin for different lock id and key
    lockId = 101;
    strongOid = 2 * lockId;
    key = "key" + lockId;
    lockOid = new LongLockID(lockId);
    strongEntry = createMockSerializedEntry("valueStrong", strongOid);
    lockIDandAwardIDMapping.put(lockOid, 1L);
    MockModesAdd.addStrongValueToCacheWithAwardID(cache, this.globalLocalCacheManager, key, lockOid, strongEntry,
                                                  mapID, MapOperationType.PUT, 1L);

    Mockito.verify(mng, Mockito.times(1)).pinLock(lockOid, lockIDandAwardIDMapping.get(lockOid));
    barier.await();
    barier.await();
    Mockito.verify(mng, Mockito.times(2)).pinLock(lockOid, lockIDandAwardIDMapping.get(lockOid));
    Mockito.verify(mng, Mockito.times(1)).unpinLock(lockOid, lockIDandAwardIDMapping.get(lockOid));

  }

  public void testPinIfNecessoryForTransactionAbort() throws Exception {
    CyclicBarrier barier = new CyclicBarrier(2);
    setLocalCache(barier, true, this.maxInMemory);

    long lockId = 100;
    long strongOid = 2 * lockId;
    String key = "key" + lockId;
    LockID lockOid = new LongLockID(lockId);
    MockSerializedEntry strongEntry = createMockSerializedEntry("valueStrong" + lockId, strongOid);
    lockIDandAwardIDMapping.put(lockOid, 1L);
    MockModesAdd.addStrongValueToCacheWithAwardID(cache, this.globalLocalCacheManager, key, lockOid, strongEntry,
                                                  mapID, MapOperationType.PUT, 1L);
    Mockito.verify(mng, Mockito.times(1)).pinLock(lockOid, lockIDandAwardIDMapping.get(lockOid));
    Mockito.verify(mng, Mockito.never()).unpinLock(lockOid, lockIDandAwardIDMapping.get(lockOid));
    barier.await();
    barier.await();
    Mockito.verify(mng, Mockito.times(1)).pinLock(lockOid, lockIDandAwardIDMapping.get(lockOid));
    Mockito.verify(mng, Mockito.times(1)).unpinLock(lockOid, lockIDandAwardIDMapping.get(lockOid));

  }

  public void testPinIfNecessoryForEviction() throws Exception {
    CyclicBarrier barier = new CyclicBarrier(2);
    setLocalCache(barier, false, 25);
    for (int i = 1; i <= 50; i++) {
      long lockId = 100 + i;
      long strongOid = 2 * lockId;
      String key = "key" + lockId;
      LockID lockOid = new LongLockID(lockId);
      MockSerializedEntry strongEntry = createMockSerializedEntry("valueStrong" + lockId, strongOid);
      lockIDandAwardIDMapping.put(lockOid, 1L);
      MockModesAdd.addStrongValueToCacheWithAwardID(cache, this.globalLocalCacheManager, key, lockOid, strongEntry,
                                                    mapID, MapOperationType.PUT, 1L);
      Mockito.verify(mng, Mockito.times(1)).pinLock(lockOid, lockIDandAwardIDMapping.get(lockOid));
      barier.await();
      barier.await();
      Mockito.verify(mng, Mockito.times(2)).pinLock(lockOid, lockIDandAwardIDMapping.get(lockOid));
    }

    int pinned = 0;
    int unpinned = 0;
    for (int j = 1; j <= 50; j++) {
      long lockId = 100 + j;
      LockID lockOid = new LongLockID(lockId);
      try {
        Mockito.verify(mng, Mockito.times(1)).unpinLock(lockOid, lockIDandAwardIDMapping.get(lockOid));
        pinned++;
      } catch (TooManyActualInvocations e) {
        // for evicted key unpin should be 2
        Mockito.verify(mng, Mockito.times(2)).unpinLock(lockOid, lockIDandAwardIDMapping.get(lockOid));
        unpinned++;
      }
    }
    System.out.println("unpinned=" + unpinned);
    System.out.println("pinned=" + pinned);
    Assert.assertTrue(unpinned >= 25);
    Assert.assertTrue(pinned <= 25);
  }

  public void testMixedPut() throws Exception {
    long lockId = 100;
    long strongOid = 2 * lockId;
    long eventualOid = 3 * lockId;
    String key = "key" + lockId;
    LongLockID lockOid = new LongLockID(lockId);
    MockSerializedEntry strongEntry = createMockSerializedEntry("valueStrong", strongOid);
    MockSerializedEntry eventualEntry = createMockSerializedEntry("valueEventual", eventualOid);
    // put strong then put eventual
    MockModesAdd.addStrongValueToCache(cache, this.globalLocalCacheManager, key, lockOid, strongEntry, mapID,
                                       MapOperationType.PUT);
    assertNotNull("oid->value does not exist", cache.getMappingUnlocked(strongEntry.getObjectID()));
    MockModesAdd.addEventualValueToCache(cache, this.globalLocalCacheManager, key, eventualEntry, mapID,
                                         MapOperationType.PUT);
    assertNull("oid->value still exist", cache.getMappingUnlocked(strongEntry.getObjectID()));
    assertNotNull("oid->list<key> does not exist", cache.getMappingUnlocked(eventualEntry.getObjectID()));

    lockId = 200;
    strongOid = 2 * lockId;
    eventualOid = 3 * lockId;
    key = "key" + lockId;
    lockOid = new LongLockID(lockId);
    strongEntry = createMockSerializedEntry("valueStrong", strongOid);
    eventualEntry = createMockSerializedEntry("valueEventual", eventualOid);
    // put eventual then put strong
    MockModesAdd.addEventualValueToCache(cache, this.globalLocalCacheManager, key, eventualEntry, mapID,
                                         MapOperationType.PUT);
    assertNotNull("oid->list<key> does not exist", cache.getInternalStore().get(eventualEntry.getObjectID()));
    MockModesAdd.addStrongValueToCache(cache, this.globalLocalCacheManager, key, lockOid, strongEntry, mapID,
                                       MapOperationType.PUT);
    assertNotNull("oid->value does not exist", cache.getInternalStore().get(strongEntry.getObjectID()));
    assertNull("oid->list<key> still exist", cache.getInternalStore().get(eventualEntry.getObjectID()));
  }

  public void testAddEventualValueToCache() throws Exception {
    for (int i = 0; i < 50; i++) {
      MockModesAdd.addEventualValueToCache(cache, this.globalLocalCacheManager, "key" + i,
                                           createMockSerializedEntry("value" + i, i), mapID, MapOperationType.PUT);
    }

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
    }

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
    }

    for (int i = 0; i < 50; i++) {
      String keyGot = (String) localCacheStore.get(new ObjectID(i));
      Assert.assertEquals("key" + i, keyGot);
    }

    Assert.assertEquals(50, cache.size());
  }

  public void testAddEventualValueRemove1() throws Exception {
    for (int i = 0; i < 50; i++) {
      MockModesAdd.addEventualValueToCache(cache, this.globalLocalCacheManager, "key" + i,
                                           createMockSerializedEntry("value" + i, i), mapID, MapOperationType.PUT);
    }

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
    }

    Assert.assertEquals(50, cache.size());

    // REMOVE
    for (int i = 0; i < 25; i++) {
      MockModesAdd.addEventualValueToCache(cache, this.globalLocalCacheManager, "key" + i, null, mapID,
                                           MapOperationType.REMOVE);
    }

    for (int i = 0; i < 25; i++) {
      AbstractLocalCacheStoreValue value = cache.getLocalValue("key" + i);
      Assert.assertNull(value);
      Assert.assertNull(localCacheStore.get(new ObjectID(i)));
    }

    for (int i = 25; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
    }

    for (int i = 25; i < 50; i++) {
      String keyGot = (String) localCacheStore.get(new ObjectID(i));
      Assert.assertEquals("key" + i, keyGot);
    }

    Assert.assertEquals(25, localCacheStore.size());
  }

  public void testAddEventualValueRemove2() throws Exception {
    CyclicBarrier barier = new CyclicBarrier(2);
    setLocalCache(barier, false, this.maxInMemory);

    // GET - add to the local cache
    MockModesAdd.addEventualValueToCache(cache, globalLocalCacheManager, "key1",
                                         createMockSerializedEntry("value1", 1), mapID, MapOperationType.GET);
    AbstractLocalCacheStoreValue value = cache.getLocalValue("key1");
    assertEventualValue("value1", new ObjectID(1), value);
    String keyGot = (String) localCacheStore.get(new ObjectID(1));
    Assert.assertEquals("key1", keyGot);

    // REMOVE
    MockModesAdd.addEventualValueToCache(cache, this.globalLocalCacheManager, "key1", null, mapID,
                                         MapOperationType.REMOVE);

    value = cache.getLocalValue("key1");
    Assert.assertEquals(null, value.getValueObject());
    Assert.assertEquals(ObjectID.NULL_ID, value.asEventualValue().getMetaId());
    Assert.assertNull(localCacheStore.get(new ObjectID(1)));

    barier.await();
    barier.await();
    Assert.assertEquals(0, cache.size());

    value = cache.getLocalValue("key1");
    Assert.assertNull(value);
    Assert.assertNull(localCacheStore.get(new ObjectID(1)));
  }

  public void testAddIncoherentValueToCache() throws Exception {
    for (int i = 0; i < 50; i++) {
      MockModesAdd.addIncoherentValueToCache(cache, globalLocalCacheManager, "key" + i,
                                             createMockSerializedEntry("value" + i, i), mapID, MapOperationType.PUT);
    }

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getLocalValue("key" + i);
      assertIncoherentValue("value" + i, new ObjectID(i), value);
    }
    Assert.assertEquals(50, cache.size());
  }

  public void testFlush() throws Exception {
    for (int i = 0; i < 50; i++) {
      MockModesAdd.addEventualValueToCache(cache, globalLocalCacheManager, "key" + i,
                                           createMockSerializedEntry("value" + i, i), mapID, MapOperationType.PUT);
    }

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
    }

    // Flush
    for (int i = 0; i < 25; i++) {
      cache.removeEntriesForObjectId(new ObjectID(i));
    }

    for (int i = 0; i < 25; i++) {
      AbstractLocalCacheStoreValue value = cache.getLocalValue("key" + i);
      Assert.assertNull(value);
      Assert.assertNull(localCacheStore.get(new ObjectID(i)));
    }

    for (int i = 25; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
      String keyGot = (String) localCacheStore.get(new ObjectID(i));
      Assert.assertEquals("key" + i, keyGot);
    }
  }

  public void testEvictFromLocalCache() throws Exception {
    for (int i = 0; i < 50; i++) {
      MockModesAdd.addEventualValueToCache(cache, globalLocalCacheManager, "key" + i,
                                           createMockSerializedEntry("value" + i, i), mapID, MapOperationType.PUT);
    }

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
    }

    for (int i = 0; i < 25; i++) {
      cache.removeFromLocalCache("key" + i);
    }

    for (int i = 0; i < 25; i++) {
      AbstractLocalCacheStoreValue value = cache.getLocalValue("key" + i);
      Assert.assertNull(value);
      Assert.assertNull(localCacheStore.get(new ObjectID(i)));
    }

    for (int i = 25; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
      String keyGot = (String) localCacheStore.get(new ObjectID(i));
      Assert.assertEquals("key" + i, keyGot);
    }
  }

  public void testAddAllObjectIDsToValidate() throws Exception {
    for (int i = 0; i < 50; i++) {
      MockModesAdd.addStrongValueToCache(cache, globalLocalCacheManager, "key" + i, new LongLockID(i),
                                         createMockSerializedEntry("value" + i, i), mapID, MapOperationType.PUT);
    }


    ObjectIDSet validations1 = globalLocalCacheManager.getObjectIDsToValidate(new GroupID(0));

    Assert.assertEquals(0, validations1.size());

    for (int i = 50; i < 100; i++) {
      MockModesAdd.addEventualValueToCache(cache, globalLocalCacheManager, "key" + i,
                                           createMockSerializedEntry("value" + i, i), mapID, MapOperationType.PUT);
    }
    ObjectIDSet validations2 = globalLocalCacheManager.getObjectIDsToValidate(new GroupID(0));

    Assert.assertEquals(50, validations2.size());

    for (int i = 50; i < 100; i++) {
      Assert.assertTrue(validations2.contains(new ObjectID(i)));
    }
  }

  public void testSize() throws Exception {
    for (int i = 0; i < 50; i++) {
      MockModesAdd.addEventualValueToCache(cache, globalLocalCacheManager, "key" + i,
                                           createMockSerializedEntry("value" + i, i), mapID, MapOperationType.PUT);
    }

    Assert.assertEquals(50, cache.size());
  }

  public void testClearAllLocalCache() throws Exception {
    Assert.assertEquals(0, cache.size());
    int n = 1;
    for (int i = 0; i < n; i++) {
      MockModesAdd.addEventualValueToCache(cache, globalLocalCacheManager, "key" + i,
                                           createMockSerializedEntry("value" + i, i), mapID, MapOperationType.PUT);
    }

    cache.clear();

    for (int i = 0; i < n; i++) {
      AbstractLocalCacheStoreValue value = cache.getLocalValue("key" + i);
      Assert.assertNull(value);
      Object object = localCacheStore.get(new ObjectID(i));
      Assert.assertNull("Not null - " + object, object);
    }
  }

  public void testRemoveFromLocalCache() throws Exception {
    for (int i = 0; i < 50; i++) {
      MockModesAdd.addEventualValueToCache(cache, globalLocalCacheManager, "key" + i,
                                           createMockSerializedEntry("value" + i, i), mapID, MapOperationType.PUT);
    }

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
    }

    for (int i = 0; i < 25; i++) {
      cache.removeFromLocalCache("key" + i);
    }

    for (int i = 0; i < 25; i++) {
      AbstractLocalCacheStoreValue value = cache.getLocalValue("key" + i);
      Assert.assertNull(value);
      Assert.assertNull(localCacheStore.get(new ObjectID(i)));
    }

    for (int i = 25; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
      String keyGot = (String) localCacheStore.get(new ObjectID(i));
      Assert.assertEquals("key" + i, keyGot);
    }
  }

  public void testEvictCachedEntries() throws Exception {
    setLocalCache(null, false, 25);

    for (int i = 0; i < 50; i++) {
      MockModesAdd.addEventualValueToCache(cache, globalLocalCacheManager, "key" + i,
                                           createMockSerializedEntry("value" + i, i), mapID, MapOperationType.GET);
    }
    System.err.println("Sleeping for 10 seconds, Current size in testEvictCachedEntries " + cache.size());

    ThreadUtil.reallySleep(10 * 1000);

    int evicted = 0;
    int notEvicted = 0;
    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getLocalValue("key" + i);
      if (value != null) {
        assertEventualValue("value" + i, new ObjectID(i), value);
        String keyGot = (String) localCacheStore.get(new ObjectID(i));
        Assert.assertEquals("key" + i, keyGot);
        notEvicted++;
      } else {
        Object object = localCacheStore.get(new ObjectID(i));
        Assert.assertNull("Not null - " + object, object);
        evicted++;
      }
    }

    Assert.assertTrue(evicted >= 25);
    Assert.assertTrue(notEvicted <= 25);
  }

  public void testGetKeySet() throws Exception {
    int count = 50;
    for (int i = 0; i < count; i++) {
      int eventualId = count + i;
      MockModesAdd.addStrongValueToCache(cache, globalLocalCacheManager, "key" + i, new LongLockID(i),
                                         createMockSerializedEntry("value" + i, i), mapID, MapOperationType.PUT);
      MockModesAdd.addEventualValueToCache(cache, globalLocalCacheManager, "key" + eventualId,
                                           createMockSerializedEntry("value" + eventualId, eventualId), mapID,
                                           MapOperationType.PUT);
    }

    for (int i = 0; i < count; i++) {
      int eventualId = count + i;
      AbstractLocalCacheStoreValue value = cache.getLocalValue("key" + i);
      assertStrongValue("value" + i, new LongLockID(i), new ObjectID(i), value);

      value = cache.getLocalValue("key" + eventualId);
      assertEventualValue("value" + eventualId, new ObjectID(eventualId), value);
      String keyGot = (String) localCacheStore.get(new ObjectID(eventualId));
      Assert.assertEquals("key" + eventualId, keyGot);
    }

    Assert.assertEquals(2 * count, cache.size());
    Set keySet = cache.getKeys();
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

    keySet = cache.getKeys();
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
  }

  public void testGlobalLocalCacheManagerShutdown() {
    int count = 50;
    for (int i = 0; i < count; i++) {
      int eventualId = count + i;
      MockModesAdd.addStrongValueToCache(cache, globalLocalCacheManager, "key" + i, new LongLockID(i),
                                         createMockSerializedEntry("value" + i, i), mapID, MapOperationType.PUT);
      MockModesAdd.addEventualValueToCache(cache, globalLocalCacheManager, "key" + eventualId,
                                           createMockSerializedEntry("value" + eventualId, eventualId), mapID,
                                           MapOperationType.PUT);
    }

    for (int i = 0; i < count; i++) {
      int eventualId = count + i;
      AbstractLocalCacheStoreValue value = cache.getLocalValue("key" + i);
      assertStrongValue("value" + i, new LongLockID(i), new ObjectID(i), value);

      value = cache.getLocalValue("key" + eventualId);
      assertEventualValue("value" + eventualId, new ObjectID(eventualId), value);
      String keyGot = (String) localCacheStore.get(new ObjectID(eventualId));
      Assert.assertEquals("key" + eventualId, keyGot);
    }

    Assert.assertEquals(2 * count, cache.size());

    globalLocalCacheManager.shutdown(false);
    for (int i = 0; i < count; i++) {
      int eventualId = count + i;

      AbstractLocalCacheStoreValue value = cache.getLocalValue("key" + i);
      Assert.assertNull(value);
      Assert.assertNull(localCacheStore.get(new LongLockID(i)));

      value = cache.getLocalValue("key" + eventualId);
      Assert.assertNull(value);
      Assert.assertNull(localCacheStore.get(new ObjectID(eventualId)));
    }

    Assert.assertEquals(0, cache.size());
    Set keySet = cache.getKeys();
    Assert.assertFalse(keySet.iterator().hasNext());
  }

  @Test
  public void testEventualValueReplacedWithStrong() {
    ClientTransaction txn = mock(ClientTransaction.class);
    ClientTransactionManager txnManager = mock(ClientTransactionManager.class);
    when(txnManager.getCurrentTransaction()).thenReturn(txn);
    ClientObjectManager objectManager = mock(ClientObjectManager.class);
    when(objectManager.getTransactionManager()).thenReturn(txnManager);
    LocalCacheStoreEventualValue eventual = mock(LocalCacheStoreEventualValue.class);
    when(eventual.isEventualConsistentValue()).thenReturn(true);
    when(eventual.getValueObjectId()).thenReturn(new ObjectID(41L));
    L1ServerMapLocalCacheStore localStore = mock(L1ServerMapLocalCacheStore.class);
    when(localStore.remove("foo")).thenReturn(eventual);
    
    Manager manager = mock(Manager.class);
    ServerMapLocalCacheImpl cache = new ServerMapLocalCacheImpl(objectManager, manager, null, true, 
            mock(ServerMapLocalCacheRemoveCallback.class), localStore);

    LocalCacheStoreStrongValue strong = mock(LocalCacheStoreStrongValue.class);
    when(strong.isStrongConsistentValue()).thenReturn(true);
    when(strong.getValueObjectId()).thenReturn(new ObjectID(42L));
    
    cache.addToCache("foo", strong, MapOperationType.PUT);
    
    verify(manager, times(1)).pinLock(any(LockID.class), anyLong());
  }
          
  public class MyClientTransaction implements ClientTransaction {
    private final CyclicBarrier barrier;
    private final boolean       transactionAbort;

    public MyClientTransaction(CyclicBarrier barrier, boolean transactionAbort) {
      this.barrier = barrier;
      this.transactionAbort = transactionAbort;
    }

    @Override
    public void addMetaDataDescriptor(TCObject tco, MetaDataDescriptorInternal md) {
      throw new ImplementMe();
    }

    @Override
    public void addNotify(Notify notify) {
      throw new ImplementMe();
    }

    @Override
    public void addTransactionCompleteListener(TransactionCompleteListener l) {
      if (barrier == null) {
        callDefault(l);
      } else {
        callBarriered(l);
      }
    }

    private void callDefault(TransactionCompleteListener l) {
      ThreadUtil.reallySleep(1);
      if (transactionAbort) l.transactionAborted(null);
      else l.transactionComplete(null);
    }

    public void callBarriered(final TransactionCompleteListener l) {
      Runnable runnable = new Runnable() {
        @Override
        public void run() {
          try {
            barrier.await();
            if (transactionAbort) l.transactionAborted(null);
            else l.transactionComplete(null);
            barrier.await();
          } catch (InterruptedException e) {
            e.printStackTrace();
          } catch (BrokenBarrierException e) {
            e.printStackTrace();
          }
        }
      };

      Thread t = new Thread(runnable, "invoke txn complete");
      t.start();
    }

    @Override
    public void arrayChanged(TCObject source, int startPos, Object array, int length) {
      throw new ImplementMe();

    }

    @Override
    public void createObject(TCObject source) {
      throw new ImplementMe();

    }

    @Override
    public void createRoot(String name, ObjectID rootID) {
      throw new ImplementMe();

    }

    @Override
    public void fieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index) {
      throw new ImplementMe();

    }

    @Override
    public List getAllLockIDs() {
      throw new ImplementMe();
    }

    @Override
    public Map getChangeBuffers() {
      throw new ImplementMe();
    }

    @Override
    public TxnType getEffectiveType() {
      throw new ImplementMe();
    }

    @Override
    public LockID getLockID() {
      throw new ImplementMe();
    }

    @Override
    public TxnType getLockType() {
      throw new ImplementMe();
    }

    @Override
    public Map getNewRoots() {
      throw new ImplementMe();
    }

    @Override
    public List getNotifies() {
      throw new ImplementMe();
    }

    @Override
    public int getNotifiesCount() {
      throw new ImplementMe();
    }

    @Override
    public Collection getReferencesOfObjectsInTxn() {
      throw new ImplementMe();
    }

    @Override
    public SequenceID getSequenceID() {
      throw new ImplementMe();
    }

    @Override
    public List getTransactionCompleteListeners() {
      throw new ImplementMe();
    }

    @Override
    public TransactionID getTransactionID() {
      throw new ImplementMe();
    }

    @Override
    public boolean hasChanges() {
      throw new ImplementMe();
    }

    @Override
    public boolean hasChangesOrNotifies() {
      throw new ImplementMe();
    }

    @Override
    public boolean isConcurrent() {
      throw new ImplementMe();
    }

    @Override
    public boolean isNull() {
      throw new ImplementMe();
    }

    @Override
    public void literalValueChanged(TCObject source, Object newValue, Object oldValue) {
      throw new ImplementMe();

    }

    @Override
    public void logicalInvoke(TCObject source, LogicalOperation method, Object[] parameters, LogicalChangeID id) {
      throw new ImplementMe();

    }

    @Override
    public void setAlreadyCommitted() {
      throw new ImplementMe();

    }

    @Override
    public void setSequenceID(SequenceID sequenceID) {
      throw new ImplementMe();

    }

    @Override
    public void setTransactionContext(TransactionContext transactionContext) {
      throw new ImplementMe();

    }

    @Override
    public void setTransactionID(TransactionID tid) {
      throw new ImplementMe();

    }

    @Override
    public boolean isAtomic() {
      return false;
    }

    @Override
    public void setAtomic(boolean atomic) {
      throw new ImplementMe();

    }

    @Override
    public void addOnCommitCallable(OnCommitCallable callable) {
      throw new ImplementMe();

    }

    @Override
    public List<OnCommitCallable> getOnCommitCallables() {
      return Collections.EMPTY_LIST;
    }

    @Override
    public int getSession() {
      return 0;
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
    };

    public abstract boolean isValueOfType(AbstractLocalCacheStoreValue value);
  }

  private static class TxnCompleteSink implements Sink {
    L1ServerMapTransactionCompletionHandler completionHandler = new L1ServerMapTransactionCompletionHandler();

    @Override
    public void add(EventContext context) {
      completionHandler.handleEvent(context);
    }

    @Override
    public boolean addLossy(EventContext context) {
      throw new ImplementMe();
    }

    @Override
    public void addMany(Collection contexts) {
      throw new ImplementMe();

    }

    @Override
    public void clear() {
      throw new ImplementMe();

    }

    @Override
    public AddPredicate getPredicate() {
      throw new ImplementMe();
    }

    @Override
    public void setAddPredicate(AddPredicate predicate) {
      throw new ImplementMe();

    }

    @Override
    public int size() {
      throw new ImplementMe();
    }

    @Override
    public void enableStatsCollection(boolean enable) {
      throw new ImplementMe();

    }

    @Override
    public Stats getStats(long frequency) {
      throw new ImplementMe();
    }

    @Override
    public Stats getStatsAndReset(long frequency) {
      throw new ImplementMe();
    }

    @Override
    public boolean isStatsCollectionEnabled() {
      throw new ImplementMe();
    }

    @Override
    public void resetStats() {
      throw new ImplementMe();

    }

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
        @Override
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

    @Override
    public void add(EventContext context) {
      try {
        q.put(context);
        contextsAdded.incrementAndGet();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public boolean addLossy(EventContext context) {
      throw new ImplementMe();
    }

    @Override
    public void addMany(Collection contexts) {
      throw new ImplementMe();
    }

    @Override
    public void clear() {
      throw new ImplementMe();
    }

    @Override
    public AddPredicate getPredicate() {
      throw new ImplementMe();
    }

    @Override
    public void setAddPredicate(AddPredicate predicate) {
      throw new ImplementMe();
    }

    @Override
    public int size() {
      throw new ImplementMe();
    }

    @Override
    public void enableStatsCollection(boolean enable) {
      throw new ImplementMe();
    }

    @Override
    public Stats getStats(long frequency) {
      throw new ImplementMe();
    }

    @Override
    public Stats getStatsAndReset(long frequency) {
      throw new ImplementMe();
    }

    @Override
    public boolean isStatsCollectionEnabled() {
      throw new ImplementMe();
    }

    @Override
    public void resetStats() {
      throw new ImplementMe();
    }

  }

  private void assertEventualValue(String expectedValue, ObjectID expectedObjectId, AbstractLocalCacheStoreValue value) {
    assertValueType(value, LocalCacheValueType.EVENTUAL);
    Assert.assertTrue(value instanceof LocalCacheStoreEventualValue);
    try {
      value.asEventualValue();
    } catch (Throwable t) {
      fail("Should be able to retrieve value as Eventual value: " + t);
    }
    try {
      value.asStrongValue();
      fail("Should have failed");
    } catch (ClassCastException ignored) {
      // expected
    }

    Assert.assertEquals(createMockSerializedEntry(expectedValue, expectedObjectId.toLong()), value.getValueObject());
    Assert.assertEquals(expectedObjectId, value.getMetaId());
    Assert.assertEquals(expectedObjectId, value.asEventualValue().getValueObjectId());
  }

  private void assertIncoherentValue(String expectedValue, ObjectID expectedObjectId, AbstractLocalCacheStoreValue value) {
    assertValueType(value, LocalCacheValueType.EVENTUAL);
    Assert.assertTrue(value instanceof LocalCacheStoreEventualValue);
    try {
      value.asEventualValue();
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
    } catch (ClassCastException ignored) {
      fail("Should not have failed");
    }
    Assert.assertTrue(createMockSerializedEntry(expectedValue, expectedObjectId.toLong())
        .equals(value.getValueObject()));
    Assert.assertEquals(expectedObjectId, value.getMetaId());
  }

  private void assertStrongValue(String expectedValue, LockID expectedLockId, ObjectID expectedObjectId,
                                 AbstractLocalCacheStoreValue value) {
    assertValueType(value, LocalCacheValueType.STRONG);
    Assert.assertTrue(value instanceof LocalCacheStoreStrongValue);
    try {
      value.asStrongValue();
    } catch (Throwable t) {
      Assert.fail("Should be able to retrieve value as Strong value: " + t);
    }
    try {
      value.asEventualValue();
      fail("Should have failed");
    } catch (ClassCastException ignored) {
      // expected
    }
    Assert.assertEquals(createMockSerializedEntry(expectedValue, expectedObjectId.toLong()), value.getValueObject());
    Assert.assertEquals(expectedLockId, value.getMetaId());
    Assert.assertEquals(expectedLockId, value.asStrongValue().getLockId());
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

  public MockSerializedEntry createMockSerializedEntry(String value, long id) {
    byte[] bytes = value.getBytes();
    return new MockSerializedEntry(new ObjectID(id), bytes, null);
  }

}
