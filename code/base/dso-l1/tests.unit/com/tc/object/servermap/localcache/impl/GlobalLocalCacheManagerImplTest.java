/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import org.mockito.Mockito;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.AddPredicate;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.exception.ImplementMe;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.context.LocksToRecallContext;
import com.tc.object.handler.LockRecallHandler;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LocksRecallService;
import com.tc.object.locks.LocksRecallServiceImpl;
import com.tc.object.locks.LongLockID;
import com.tc.object.locks.MockClientLockManager;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStoreListener;
import com.tc.object.servermap.localcache.LocalCacheStoreStrongValue;
import com.tc.object.servermap.localcache.MapOperationType;
import com.tc.object.servermap.localcache.PutType;
import com.tc.object.servermap.localcache.ServerMapLocalCache;
import com.tc.stats.Stats;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;
import junit.framework.TestCase;

public class GlobalLocalCacheManagerImplTest extends TestCase {
  private L1ServerMapLocalCacheManagerImpl globalLocalCacheManagerImpl;
  private MockClientLockManager            clientLockManager;
  private MySink                           testSink;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    clientLockManager = new MockClientLockManager();

    ClientConfigurationContext configurationContext = Mockito.mock(ClientConfigurationContext.class);
    Mockito.when(configurationContext.getLockManager()).thenReturn(clientLockManager);

    LockRecallHandler lockRecallHandler = new LockRecallHandler();
    lockRecallHandler.initialize(configurationContext);

    testSink = new MySink(new MyHandler(lockRecallHandler, new L1ServerMapCapacityEvictionHandler()));
    Stage lockRecallStage = Mockito.mock(Stage.class);
    Mockito.when(lockRecallStage.getSink()).thenReturn(testSink);

    LocksRecallService locksRecallHelper = new LocksRecallServiceImpl(lockRecallHandler, lockRecallStage);
    this.globalLocalCacheManagerImpl = new L1ServerMapLocalCacheManagerImpl(locksRecallHelper, testSink,
                                                                            Mockito.mock(Sink.class));
  }

  public void testCapacityEviction() {
    int maxInMemory = 10;

    L1ServerMapLocalCacheStoreHashMap store = new L1ServerMapLocalCacheStoreHashMap(maxInMemory);
    this.globalLocalCacheManagerImpl.addStoreListener(store);

    ObjectID mapID = new ObjectID(100);
    this.globalLocalCacheManagerImpl.getOrCreateLocalCache(mapID, Mockito.mock(ClientObjectManager.class), null, true,
                                                           null);

    for (int i = 0; i < 15; i++) {
      // store.put("key" + i, new LocalCacheStoreStrongValue(new LongLockID(i), "value" + i, mapID), PutType.NORMAL);
      store.put("key" + i, new LocalCacheStoreStrongValue(new LongLockID(i), new ObjectID(i), mapID), PutType.NORMAL);
    }

    Assert.assertEquals(15, store.size());

    List<L1ServerMapLocalCacheStoreListener> listeners = store.getListeners();
    for (L1ServerMapLocalCacheStoreListener l : listeners) {
      l.notifySizeChanged(store);
    }

    testSink.waitUntilContextsAddedEqualsAndCompletedEquals(2, 2);

    System.err.println("Store size is " + store.size());
    Assert.assertTrue(store.size() < maxInMemory);

    int sizeBefore = store.size();

    System.err.println("Sleeping for 10 seconds -- ");
    ThreadUtil.reallySleep(10 * 1000);
    testSink.waitUntilContextsAddedEqualsAndCompletedEquals(2, 2);

    int sizeAfter = store.size();
    Assert.assertEquals(sizeBefore, sizeAfter);
  }

  public void testInitiateRecall() {
    LockID lockID = new LongLockID(500);
    Set<LockID> lockIDs = Collections.singleton(lockID);

    globalLocalCacheManagerImpl.recallLocks(lockIDs);
    testSink.waitUntilContextsAddedEqualsAndCompletedEquals(1, 1);

    Assert.assertEquals(1, clientLockManager.getRecallList().size());
    Assert.assertEquals(lockID, clientLockManager.getRecallList().get(0));
  }

  public void testInlineRecall() {
    LockID lockID = new LongLockID(500);
    Set<LockID> lockIDs = Collections.singleton(lockID);

    globalLocalCacheManagerImpl.recallLocksInline(lockIDs);

    Assert.assertEquals(1, clientLockManager.getRecallList().size());
    Assert.assertEquals(lockID, clientLockManager.getRecallList().get(0));
  }

  public void testRememberMapIDToLockID() {

    L1ServerMapLocalCacheStore store = new L1ServerMapLocalCacheStoreHashMap();
    this.globalLocalCacheManagerImpl.addStoreListener(store);

    ObjectID mapID = new ObjectID(100);
    LockID lockID = new LongLockID(100);
    ServerMapLocalCache localCache = this.globalLocalCacheManagerImpl.getOrCreateLocalCache(mapID, Mockito
        .mock(ClientObjectManager.class), null, true, null);
    localCache.setupLocalStore(store);

    // localCache.addStrongValueToCache(lockID, "key", "value", MapOperationType.GET);
    localCache.addToCache("key", new LocalCacheStoreStrongValue(lockID, new ObjectID(12345), mapID),
                          MapOperationType.GET);

    this.globalLocalCacheManagerImpl.removeEntriesForLockId(lockID);

    Assert.assertEquals(0, store.size());
    Assert.assertEquals(0, localCache.size());
  }

  private static class MySink implements Sink {
    private final LinkedBlockingQueue<EventContext> q                 = new LinkedBlockingQueue<EventContext>();
    private final AtomicInteger                     contextsAdded     = new AtomicInteger(0);
    private final AtomicInteger                     contextsCompleted = new AtomicInteger(0);

    public MySink(final AbstractEventHandler handler) {
      Runnable runnable = new Runnable() {
        public void run() {
          while (true) {
            EventContext context;
            try {
              context = q.take();
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
            try {
              handler.handleEvent(context);
            } catch (EventHandlerException e) {
              throw new RuntimeException(e);
            }
            contextsCompleted.incrementAndGet();
          }
        }
      };

      Thread t1 = new Thread(runnable, "MySink thread");
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

    public void waitUntilContextsAddedEqualsAndCompletedEquals(int expectedContextsAdded, int expectedContextsCompleted) {
      waitUntilAtomicIntegerReaches("expectedContextsAdded", contextsAdded, expectedContextsAdded);
      waitUntilAtomicIntegerReaches("expectedContextsCompleted", contextsCompleted, expectedContextsCompleted);

      Assert.assertEquals(expectedContextsAdded, contextsAdded.get());
      Assert.assertEquals(expectedContextsCompleted, contextsCompleted.get());
    }

    private void waitUntilAtomicIntegerReaches(String integerString, AtomicInteger integer, int expected) {
      while (integer.get() != expected) {
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

  private static class MyHandler extends AbstractEventHandler {

    private final LockRecallHandler                  lockRecallHandler;
    private final L1ServerMapCapacityEvictionHandler l1ServerMapCapacityEvictionHandler;

    public MyHandler(LockRecallHandler lockRecallHandler,
                     L1ServerMapCapacityEvictionHandler l1ServerMapCapacityEvictionHandler) {
      this.lockRecallHandler = lockRecallHandler;
      this.l1ServerMapCapacityEvictionHandler = l1ServerMapCapacityEvictionHandler;

    }

    @Override
    public void handleEvent(EventContext context) {
      if (context instanceof LocksToRecallContext) {
        lockRecallHandler.handleEvent(context);
      } else if (context instanceof L1ServerMapLocalStoreEvictionInfo) {
        l1ServerMapCapacityEvictionHandler.handleEvent(context);
      }
    }

  }

}
