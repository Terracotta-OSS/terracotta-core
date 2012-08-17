/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.test.util.WaitUtil;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields.Consistency;

import java.util.concurrent.Callable;

public abstract class ClusteredMapMaxElementsOnDiskTestClient extends ClientBase {

  public ClusteredMapMaxElementsOnDiskTestClient(String[] args) {
    super(args);
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    testMaxElementsOnDisk(toolkit, getConsistency());
    System.out.println("Testing with Strong Cache");
    testMaxElementsOnDisk(toolkit, Consistency.STRONG);

  }
  
  public abstract Consistency getConsistency();


  private void testMaxElementsOnDisk(Toolkit toolkit, Consistency consistency) throws Throwable {

    ToolkitCacheConfigBuilder builder = new ToolkitCacheConfigBuilder();
    builder.consistency(consistency);
    builder.concurrency(1);
    builder.maxCountLocalHeap(10);
    builder.maxTotalCount(0);
    final ToolkitCache cache = toolkit.getCache("cache11", builder.build(), null);
    final int size = 5000;
    populateCache(cache, size);
    WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {

      @Override
      public Boolean call() throws Exception {
        int actual = cache.size();
        System.out.println("Waiting till size: " + size + ", actual: " + actual);
        return size == actual;
      }

    });
    builder.maxCountLocalHeap(10);
    builder.maxTotalCount(100);
    final ToolkitCache cache2 = toolkit.getCache("cache22", builder.build(), null);
    populateCache(cache2, size);
    WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
      private int i = 100000;
      @Override
      public Boolean call() throws Exception {
        cache2.put(i++, i);
        System.out.println("cache22 size" + cache2.size());
        return cache2.size() < 1000;
      }
    });
    cache.destroy();
    cache2.destroy();

  }

  private void populateCache(ToolkitCache cache, int size) {

    for (int i = 0; i < size; i++) {
      cache.put(i, String.valueOf(i));
    }
    cache.size();
  }
}