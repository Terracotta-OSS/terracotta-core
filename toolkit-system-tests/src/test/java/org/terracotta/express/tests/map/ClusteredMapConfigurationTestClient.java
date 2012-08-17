/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.store.ToolkitStore;
import org.terracotta.toolkit.store.ToolkitStoreConfigBuilder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import junit.framework.Assert;

public class ClusteredMapConfigurationTestClient extends ClientBase {

  public ClusteredMapConfigurationTestClient(String[] args) {
    super(args);
  }

  public static void main(String[] args) {
    new ClusteredMapConfigurationTestClient(args).run();
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    ToolkitStoreConfigBuilder clusteredMapConfig = new ToolkitStoreConfigBuilder();
    clusteredMapConfig.maxCountLocalHeap(1000);
    final ToolkitStore<String, String> maxEntriesLocalHeapMap = toolkit.getStore("maxEntriesLocalHeap",
                                                                                 clusteredMapConfig.build(), null);
    Object maxEntriesLocalHeapConfig = getLocalCacheConfiguration(maxEntriesLocalHeapMap);
    Assert.assertEquals(2001, longAccessor(maxEntriesLocalHeapConfig, "getMaxEntriesLocalHeap"));
    Assert.assertFalse(booleanAccessor(maxEntriesLocalHeapConfig, "isOverflowToDisk"));
    Assert.assertFalse(booleanAccessor(maxEntriesLocalHeapConfig, "isOverflowToOffHeap"));

    ToolkitStoreConfigBuilder clusteredMapConfig2 = new ToolkitStoreConfigBuilder();
    clusteredMapConfig2.maxBytesLocalHeap(1024);
    final ToolkitStore<String, String> maxBytesLocalHeapMap = toolkit.getStore("maxBytesLocalHeap",
                                                                               clusteredMapConfig2.build(), null);
    Object maxBytesLocalHeapConfig = getLocalCacheConfiguration(maxBytesLocalHeapMap);
    Assert.assertEquals(1024, longAccessor(maxBytesLocalHeapConfig, "getMaxBytesLocalHeap"));
    Assert.assertFalse(booleanAccessor(maxBytesLocalHeapConfig, "isOverflowToDisk"));
    Assert.assertFalse(booleanAccessor(maxBytesLocalHeapConfig, "isOverflowToOffHeap"));
  }

  private long longAccessor(Object o, String field) throws Exception {
    Method m = o.getClass().getDeclaredMethod(field);
    return (Long) m.invoke(o);
  }

  private boolean booleanAccessor(Object o, String field) throws Exception {
    Method m = o.getClass().getDeclaredMethod(field);
    return (Boolean) m.invoke(o);
  }

  private Object getLocalCacheConfiguration(final ToolkitStore map) throws Exception {
    Object clusteredMapImpl = getPrivateField(map, "aggregateServerMap");
    System.out.println("Methods on clusteredMapImpl: ");
    Method createLocalCacheStore = null;
    for (Method m : clusteredMapImpl.getClass().getDeclaredMethods()) {
      System.out.println("  " + m.getName() + " : " + m.getReturnType().getName() + ", numParams: "
                         + m.getParameterTypes().length);
      if (m.getName().equals("createLocalCacheStore")) {
        createLocalCacheStore = m;
        break;
      }
    }
    Assert.assertNotNull("Didn't find ClusteredMapImpl.createLocalCacheStore() method", createLocalCacheStore);
    createLocalCacheStore.setAccessible(true);
    Object localCacheStore = createLocalCacheStore.invoke(clusteredMapImpl, new Object[0]);
    Object toolkitStore = getPrivateField(localCacheStore, "toolkitStore");
    Object localStoreCache = getPrivateField(toolkitStore, "localStoreCache");

    Method m = localStoreCache.getClass().getDeclaredMethod("getCacheConfiguration");
    return m.invoke(localStoreCache);
  }

  private Object getPrivateField(Object o, String fieldName) throws Exception {
    Field f = o.getClass().getDeclaredField(fieldName);
    f.setAccessible(true);
    return f.get(o);
  }
}
