/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFactory;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.collections.ToolkitBlockingQueue;
import org.terracotta.toolkit.collections.ToolkitList;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.collections.ToolkitSet;
import org.terracotta.toolkit.collections.ToolkitSortedSet;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.events.ToolkitNotifier;
import org.terracotta.toolkit.object.Destroyable;
import org.terracotta.toolkit.object.ToolkitLockedObject;
import org.terracotta.toolkit.object.ToolkitObject;
import org.terracotta.toolkit.store.ToolkitStore;

import com.tc.test.config.model.TestConfig;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

public class GetNameTest extends AbstractToolkitTestBase {

  public GetNameTest(TestConfig testConfig) {
    super(testConfig, GetNameTestClient.class);
  }

  public static class GetNameTestClient extends ClientBase {

    public GetNameTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      String tcUrl = getTerracottaUrl();
      debug("Starting new client with tcUrl: " + tcUrl);
      Toolkit tk = ToolkitFactory.createToolkit("toolkit:terracotta://" + getTerracottaUrl());

      List<ToolkitObject> toolkitObjects = new ArrayList<ToolkitObject>();

      String name = "someName";
      beforeDestroyTest(toolkitObjects, tk, name);

      afterDestroyTest(toolkitObjects, name);
    }

    private void afterDestroyTest(List<ToolkitObject> toolkitObjects, String name) {
      for (ToolkitObject obj : toolkitObjects) {
        if (obj instanceof Destroyable) {
          ((Destroyable) obj).destroy();
        }
      }

      for (ToolkitObject obj : toolkitObjects) {
        if (obj instanceof Destroyable) {
          Assert.assertTrue(((Destroyable) obj).isDestroyed());
        }

        if (obj instanceof ToolkitLockedObject) {
          assertToolkitLockedObject(name, (ToolkitLockedObject) obj);
        } else {
          assertToolkitObject(name, obj);
        }
      }
    }

    private void beforeDestroyTest(List<ToolkitObject> toolkitObjects, Toolkit tk, String name) {
      ToolkitReadWriteLock readWriteLock = tk.getReadWriteLock(name);
      toolkitObjects.add(readWriteLock);
      Assert.assertEquals(name, readWriteLock.getName());
      Assert.assertNull(readWriteLock.readLock().getName());
      Assert.assertNull(readWriteLock.writeLock().getName());

      ToolkitList list = tk.getList(name, null);
      assertToolkitLockedObject(name, list);
      toolkitObjects.add(list);

      ToolkitSet set = tk.getSet(name, null);
      assertToolkitLockedObject(name, set);
      toolkitObjects.add(set);

      ToolkitSortedSet<Integer> sortedSet = tk.getSortedSet(name, Integer.class);
      assertToolkitLockedObject(name, sortedSet);
      toolkitObjects.add(sortedSet);

      ToolkitBlockingQueue blockingQueue = tk.getBlockingQueue(name, null);
      assertToolkitLockedObject(name, blockingQueue);
      toolkitObjects.add(blockingQueue);

      ToolkitMap map = tk.getMap(name, null, null);
      assertToolkitLockedObject(name, map);
      toolkitObjects.add(map);

      ToolkitAtomicLong atomicLong = tk.getAtomicLong(name);
      assertToolkitObject(name, atomicLong);
      toolkitObjects.add(atomicLong);

      ToolkitBarrier barrier = tk.getBarrier(name, 1);
      assertToolkitObject(name, barrier);
      toolkitObjects.add(barrier);

      ToolkitCache cache = tk.getCache(name, null);
      assertToolkitObject(name, cache);
      toolkitObjects.add(cache);

      ToolkitStore store = tk.getStore(name, null);
      assertToolkitObject(name, store);
      toolkitObjects.add(store);

      ToolkitLock lock = tk.getLock(name);
      assertToolkitObject(name, lock);
      toolkitObjects.add(lock);

      ToolkitNotifier notifier = tk.getNotifier(name, null);
      assertToolkitObject(name, notifier);
      toolkitObjects.add(notifier);
    }

    private void assertToolkitObject(String name, ToolkitObject obj) {
      Assert.assertEquals(name, obj.getName());
    }

    private void assertToolkitLockedObject(String name, ToolkitLockedObject obj) {
      Assert.assertEquals(name, obj.getName());
      if (obj instanceof Destroyable && !((Destroyable) obj).isDestroyed()) {
        Assert.assertNotNull(obj.getReadWriteLock());
        Assert.assertNull(obj.getReadWriteLock().getName());
        Assert.assertNull(obj.getReadWriteLock().readLock().getName());
        Assert.assertNull(obj.getReadWriteLock().writeLock().getName());
      }
    }

  }
}
