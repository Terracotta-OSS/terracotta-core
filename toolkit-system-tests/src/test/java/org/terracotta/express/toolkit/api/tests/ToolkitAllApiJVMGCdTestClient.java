/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.toolkit.api.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.collections.ToolkitBlockingQueue;
import org.terracotta.toolkit.collections.ToolkitList;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.collections.ToolkitSet;
import org.terracotta.toolkit.collections.ToolkitSortedSet;
import org.terracotta.toolkit.store.ToolkitStore;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

public class ToolkitAllApiJVMGCdTestClient extends ClientBase {
  WeakReference<ToolkitMap>           weakMap;
  WeakReference<ToolkitStore>         weakStore;
  WeakReference<ToolkitCache>         weakCache;
  WeakReference<ToolkitSet>           weakSet;
  WeakReference<ToolkitSortedSet>     weakSortedSet;
  WeakReference<ToolkitBlockingQueue> weakBlockingQueue;
  WeakReference<ToolkitList>          weakList;

  public ToolkitAllApiJVMGCdTestClient(String[] args) {
    super(args);
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    initAllWeakReferences(toolkit);
    long time = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
    while (allWeakReferencesNull() == false && System.currentTimeMillis() < time) {
      Thread.sleep(1000);
      System.err.println("Sleeping for 1 sec and waiting for map to be GC'd");
      for (int i = 0; i < 5; i++) {
        System.gc();
      }
    }
    Assert.assertTrue(allWeakReferencesNull());
  }

  private boolean allWeakReferencesNull() {
    if (weakStore.get() != null) return false;
    if (weakCache.get() != null) return false;
    if (weakMap.get() != null) return false;
    if (weakSet.get() != null) return false;
    if (weakSortedSet.get() != null) return false;
    if (weakBlockingQueue.get() != null) return false;
    if (weakList.get() != null) return false;
    return true;

  }

  private void initAllWeakReferences(Toolkit toolkit) {
    weakStore = new WeakReference<ToolkitStore>(toolkit.getStore("myStore", null));
    weakMap = new WeakReference<ToolkitMap>(toolkit.getMap("myMap", null, null));
    weakCache = new WeakReference<ToolkitCache>(toolkit.getCache("myCache", null));
    weakSet = new WeakReference<ToolkitSet>(toolkit.getSet("mySet", null));
    weakSortedSet = new WeakReference<ToolkitSortedSet>(toolkit.getSortedSet("mySortedSet", Integer.class));
    weakBlockingQueue = new WeakReference<ToolkitBlockingQueue>(toolkit.getBlockingQueue("myBlockingQueue", null));
    weakList = new WeakReference<ToolkitList>(toolkit.getList("myList", null));

  }
}
