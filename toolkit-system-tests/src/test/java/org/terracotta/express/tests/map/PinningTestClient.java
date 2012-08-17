/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.cache.ToolkitCacheConfigFields.PinningStore;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields.Consistency;

import java.util.concurrent.BrokenBarrierException;

import junit.framework.Assert;

public class PinningTestClient extends ClientBase {
  public static final String  CONSISTENCY = "consistency";
  private final Consistency   consistency;
  private Toolkit             toolkit;
  private ToolkitBarrier      barrier;
  private static final String KEY         = "key-";
  private static final String VALUE       = "value-";

  public PinningTestClient(String[] args) {
    super(args);
    this.consistency = Consistency.valueOf(System.getProperty(CONSISTENCY));
  }

  @Override
  protected void test(Toolkit myToolkit) throws Throwable {
    this.toolkit = myToolkit;
    this.barrier = getBarrierForAllClients();
    testPinningLocal(PinningStore.LOCALHEAP);
    testPinningLocal(PinningStore.LOCALMEMORY);
    testPinningInCache();
    testPinnedKeys();
  }

  private void testPinningInCache() throws InterruptedException, BrokenBarrierException {
    int maxCountLocalHeap = 10;
    int maxTotalCount = maxCountLocalHeap;
    int numElements = maxCountLocalHeap * 2;
    Configuration config = new ToolkitCacheConfigBuilder().maxTotalCount(maxTotalCount)
        .maxCountLocalHeap(maxCountLocalHeap).maxBytesLocalOffheap(0).pinningStore(PinningStore.INCACHE)
        .consistency(consistency).build();
    ToolkitCache inCachePinnedMap = toolkit.getCache("inCachePinnedMap", config, null);
    int index = barrier.await();
    if (index == 0) {
      for (int i = 0; i < numElements; ++i) {
        inCachePinnedMap.put(getKey(i), getValue(i));
      }
    }
    barrier.await();
    for (int i = 0; i < numElements; ++i) {
      Assert.assertNotNull(inCachePinnedMap.get(getKey(i)));
    }
  }

  private void testPinningLocal(PinningStore store) throws InterruptedException, BrokenBarrierException {
    int maxCountLocalHeap = 10;
    int numElements = maxCountLocalHeap * 2;
    Configuration config = new ToolkitCacheConfigBuilder().maxCountLocalHeap(maxCountLocalHeap).maxBytesLocalOffheap(0)
        .pinningStore(store).consistency(consistency).build();
    ToolkitCacheInternal localPinnedMap = (ToolkitCacheInternal) toolkit.getCache("localPinnedMap-" + store.name(),
                                                                                  config, null);
    int index = barrier.await();
    if (index == 0) {
      for (int i = 0; i < numElements; ++i) {
        localPinnedMap.put(getKey(i), getValue(i));
      }
    }
    barrier.await();
    for (int i = 0; i < numElements; ++i) {
      Assert.assertNotNull(localPinnedMap.get(getKey(i)));
    }
    barrier.await();
    for (int i = 0; i < numElements; ++i) {
      Assert.assertTrue(localPinnedMap.containsLocalKey(getKey(i)));
    }
  }

  private void testPinnedKeys() throws InterruptedException, BrokenBarrierException {
    int maxCountLocalHeap = 1;
    int numElements = 20;
    Configuration config = new ToolkitCacheConfigBuilder().maxCountLocalHeap(maxCountLocalHeap).maxBytesLocalOffheap(0)
        .consistency(consistency).build();
    ToolkitCacheInternal testPinnedKeysMap = (ToolkitCacheInternal) toolkit.getCache("testPinnedKeysMap", config, null);
    int index = barrier.await();
    if (index == 0) {
      for (int i = 0; i < numElements; ++i) {
        testPinnedKeysMap.setPinned(getKey(i), true);
      }
    }
    barrier.await();
    Assert.assertTrue(testPinnedKeysMap.keySet().isEmpty());
    Assert.assertTrue(testPinnedKeysMap.localKeySet().isEmpty());
    Assert.assertTrue(testPinnedKeysMap.localSize() == 0);
    barrier.await();

    if (index == 0) {
      for (int i = 0; i < numElements; ++i) {
        testPinnedKeysMap.put(getKey(i), getValue(i));
      }
    }
    barrier.await();
    if (index == 0) {
      for (int i = 0; i < numElements; ++i) {
        Assert.assertTrue(testPinnedKeysMap.isPinned(getKey(i)));
        Assert.assertTrue(testPinnedKeysMap.containsLocalKey(getKey(i)));
      }
    } else {
      for (int i = 0; i < numElements; ++i) {
        Assert.assertFalse(testPinnedKeysMap.isPinned(getKey(i)));
        Assert.assertFalse(testPinnedKeysMap.containsLocalKey(getKey(i)));
      }
    }

    barrier.await();
    if (index == 1) {
      testPinnedKeysMap.clear();
    }

    barrier.await();
    if (index == 0) {
      for (int i = 0; i < numElements; ++i) {
        Assert.assertTrue(testPinnedKeysMap.isPinned(getKey(i)));
      }

      for (int i = 0; i < numElements / 2; ++i) {
        testPinnedKeysMap.setPinned(getKey(i), false);
      }

      for (int i = 0; i < numElements / 2; ++i) {
        Assert.assertFalse(testPinnedKeysMap.isPinned(getKey(i)));
      }

      testPinnedKeysMap.unpinAll();
      for (int i = 0; i < numElements; ++i) {
        Assert.assertFalse(testPinnedKeysMap.isPinned(getKey(i)));
      }
    }
    barrier.await();
  }

  protected String getKey(int i) {
    return KEY + i;
  }

  protected String getValue(int i) {
    return VALUE + i;
  }

}
