/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.test.util.WaitUtil;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.store.ToolkitStore;
import org.terracotta.toolkit.store.ToolkitStoreConfigBuilder;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields.Consistency;

import com.tc.test.config.model.TestConfig;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import junit.framework.Assert;

public class ClusteredMapDiffSetTest extends AbstractToolkitTestBase {

  public ClusteredMapDiffSetTest(TestConfig testConfig) {

    super(testConfig, ClusteredMapDiffSetTestClient.class, ClusteredMapDiffSetTestClient.class);
  }

  public static class ClusteredMapDiffSetTestClient extends ClientBase {
    private static final int    COUNT      = 100;
    private static final String KEY_PREFIX = "key";

    public ClusteredMapDiffSetTestClient(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new ClusteredMapDiffSetTestClient(args).run();
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      ToolkitBarrier barrier = toolkit.getBarrier("test", 2);
      int index = barrier.await();

      ToolkitStore map = createMap(toolkit);
      barrier.await();

      doPut(index, map);
      barrier.await();
      verify(index, map);
    }

    private void doPut(int index, ToolkitStore map) {
      if (index == 0) {
        for (int i = 0; i < COUNT; i++) {
          map.put(createKey(i), createValue(i));
        }
      }
    }

    private void verify(final int index, final ToolkitStore map) throws Throwable {
      verifyBasic(index, map);
      verifyDiffSets(index, map);
    }

    private void verifyBasic(final int index, final ToolkitStore map) throws Exception {
      if (index == 0) {
        // monotonic reads
        for (int i = 0; i < COUNT; i++) {
          MyInt value = (MyInt) map.get(createKey(i));
          Assert.assertEquals("PUT Client failed: ", createValue(i), value);
        }
      } else {
        // eventually the correct value should be read
        for (int i = 0; i < COUNT; i++) {
          final int number = i;
          WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
              String key = createKey(number);
              MyInt actualValue = (MyInt) map.get(key);
              boolean result = createValue(number).equals(actualValue);
              if (!result) {
                System.err.println("Value got= " + result + " for key=" + key);
              }
              return result;
            }
          });
        }
      }
    }

    private void verifyDiffSets(int index, ToolkitStore map) {
      verifyKeySet(index, map);
      verifyEntrySet(index, map);
      verifyValues(index, map);
    }

    private void verifyValues(int index, ToolkitStore map) {
      HashSet set = new HashSet();
      for (int i = 0; i < COUNT; i++) {
        set.add(createValue(i));
      }

      Collection<MyInt> collection = map.values();
      Assert.assertEquals(set.size(), collection.size());
      Assert.assertEquals(COUNT, collection.size());
      set.removeAll(collection);
      Assert.assertEquals(0, set.size());

      Object[] array = collection.toArray();
      Assert.assertEquals(COUNT, array.length);
      set = new HashSet();
      for (int i = 0; i < COUNT; i++) {
        set.add(createValue(i));
      }
      set.removeAll(Arrays.asList(array));
      Assert.assertEquals(0, set.size());

      String[] l = new String[COUNT];
      try {
        collection.toArray(l);
        Assert.fail("Expected ArrayStoreException");
      } catch (ArrayStoreException e) {
        // Excepted
      }
    }

    private void verifyKeySet(int index, ToolkitStore map) {
      HashSet set = new HashSet();
      for (int i = 0; i < COUNT; i++) {
        set.add(createKey(i));
      }

      Collection<String> collection = map.keySet();
      Assert.assertEquals(set.size(), collection.size());
      Assert.assertEquals(COUNT, collection.size());
      set.removeAll(collection);
      Assert.assertEquals(0, set.size());

      Object[] array = collection.toArray();
      Assert.assertEquals(COUNT, array.length);
      set = new HashSet();
      for (int i = 0; i < COUNT; i++) {
        set.add(createKey(i));
      }
      set.removeAll(Arrays.asList(array));
      Assert.assertEquals(0, set.size());

      String[] l = new String[COUNT];
      try {
        collection.toArray(l);
      } catch (ArrayStoreException e) {
        Assert.fail("Not Expected ArrayStoreException");
      }
    }

    private void verifyEntrySet(int index, ToolkitStore map) {
      HashSet keySetExpected = new HashSet();
      HashSet valueSetExpected = new HashSet();
      for (int i = 0; i < COUNT; i++) {
        keySetExpected.add(createKey(i));
        valueSetExpected.add(createValue(i));
      }

      Collection<Entry> collection = map.entrySet();
      Assert.assertEquals(COUNT, collection.size());
      for (Entry entry : collection) {
        Assert.assertTrue(keySetExpected.contains(entry.getKey()));
        Assert.assertTrue(valueSetExpected.contains(entry.getValue()));
      }

      Object[] array = collection.toArray();
      Assert.assertEquals(COUNT, array.length);
      Assert.assertEquals(COUNT, collection.size());
      for (Entry entry : collection) {
        Assert.assertTrue(keySetExpected.contains(entry.getKey()));
        Assert.assertTrue(valueSetExpected.contains(entry.getValue()));
      }

      String[] l = new String[COUNT];
      try {
        collection.toArray(l);
        Assert.fail("Expected ArrayStoreException");
      } catch (ArrayStoreException e) {
        // Excepted
      }
    }

    private ToolkitStore createMap(Toolkit toolkit) {
      Configuration config = new ToolkitStoreConfigBuilder().consistency(Consistency.EVENTUAL).localCacheEnabled(false)
          .build();
      ToolkitStore map = toolkit.getStore("test", config, null);
      return map;
    }

    private String createKey(int number) {
      return KEY_PREFIX + number;
    }

    private MyInt createValue(int number) {
      return new MyInt(number);
    }
  }

  public static class MyInt implements Serializable {
    private final int i;

    public MyInt(int i) {
      this.i = i;
    }

    public int getI() {
      return i;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + i;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      MyInt other = (MyInt) obj;
      if (i != other.i) return false;
      return true;
    }

    @Override
    public String toString() {
      return "MyInt [i=" + i + "]";
    }
  }
}
