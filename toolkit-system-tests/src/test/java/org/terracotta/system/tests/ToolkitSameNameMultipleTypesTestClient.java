/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.system.tests;

import org.junit.Assert;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.collections.ToolkitBlockingQueue;
import org.terracotta.toolkit.collections.ToolkitList;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.collections.ToolkitSet;
import org.terracotta.toolkit.collections.ToolkitSortedSet;
import org.terracotta.toolkit.store.ToolkitStore;

public class ToolkitSameNameMultipleTypesTestClient extends ClientBase {
  private ToolkitList          list;
  private ToolkitMap           map;
  private ToolkitStore         store;
  private ToolkitCache         cache;
  private ToolkitSet           set;
  private ToolkitSortedSet<Integer> sortedSet;
  private ToolkitBlockingQueue blockingQueue;

  public ToolkitSameNameMultipleTypesTestClient(String[] args) {
    super(args);
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {

    String name = "DataStruc";

    chkList(name, toolkit);
    chkMap(name, toolkit);
    chkStore(name, toolkit);
    chkCache(name, toolkit);
    chkSet(name, toolkit);
    chkSortedSet(name, toolkit);
    chkBlockingQueue(name, toolkit);
    tearDown();
  }

  private void tearDown() {
    list.destroy();
    map.destroy();
    store.destroy();
    cache.destroy();
    set.destroy();
    sortedSet.destroy();
    blockingQueue.destroy();
  }

  private void chkBlockingQueue(String name, Toolkit toolkit) {
    blockingQueue = toolkit.getBlockingQueue(name, null);
    Assert.assertEquals(0, blockingQueue.size());
    doSomePutsInBlockingQueue(0, 100);

  }

  private void doSomePutsInBlockingQueue(int start, int count) {
    for (int index = start; index < start + count; index++) {
      blockingQueue.add(index);
    }

  }

  private void chkSortedSet(String name, Toolkit toolkit) {

    sortedSet = toolkit.getSortedSet(name, Integer.class);
    Assert.assertEquals(0, sortedSet.size());
    doSomePutsInSortedSet(0, 100);

  }

  private void doSomePutsInSortedSet(int start, int count) {
    for (int index = start; index < start + count; index++) {
      sortedSet.add(index);
    }

  }

  private void chkSet(String name, Toolkit toolkit) {

    set = toolkit.getSet(name, null);
    Assert.assertEquals(0, set.size());
    doSomePutsInSet(0, 100);

  }

  private void doSomePutsInSet(int start, int count) {
    for (int index = start; index < start + count; index++) {
      set.add(index);
    }

  }

  private void chkCache(String name, Toolkit toolkit) {

    cache = toolkit.getCache(name, null);
    Assert.assertEquals(0, cache.size());
    doSomePutsInCache(0, 100);

  }

  private void doSomePutsInCache(int start, int count) {
    for (int index = start; index < start + count; index++) {
      cache.put(index, index);
    }

  }

  private void chkStore(String name, Toolkit toolkit) {
    store = toolkit.getStore(name, null);
    Assert.assertEquals(0, store.size());
    doSomePutsInStore(0, 100);
  }

  private void doSomePutsInStore(int start, int count) {
    for (int index = start; index < start + count; index++) {
      store.put(index, index);
    }

  }

  private void chkMap(String name, Toolkit toolkit) {
    map = toolkit.getMap(name, null, null);
    Assert.assertEquals(0, map.size());
    doSomePutsInMap(0, 100);
  }

  private void doSomePutsInMap(int start, int count) {
    for (int index = start; index < start + count; index++) {
      map.put(index, index);
    }

  }

  private void chkList(String name, Toolkit toolkit) {

    list = toolkit.getList(name, null);
    Assert.assertEquals(0, list.size());
    doSomePutsInList(0, 100);

  }

  private void doSomePutsInList(int start, int count) {
    for (int index = start; index < start + count; index++) {
      list.add(index);
    }

  }

}
