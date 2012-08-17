/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.store.ToolkitStore;

import com.tc.test.config.model.TestConfig;

import java.util.Map;
import java.util.Random;

public class ConcurrentModificationTest extends AbstractToolkitTestBase {

  public ConcurrentModificationTest(TestConfig testConfig) {
    super(testConfig, ConcurrentModificationTestApp.class, ConcurrentModificationTestApp.class,
          ConcurrentModificationTestApp.class);
    testConfig.getClientConfig().setMaxHeap(256);
  }

  public static class ConcurrentModificationTestApp extends ClientBase {

    public ConcurrentModificationTestApp(String[] args) {
      super(args);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      ToolkitMap<String, String> toolkitmap = toolkit.getMap("testMap", null, null);
      ToolkitStore<String, String> store = toolkit.getStore("testStore", null);
      Map<String, String>[] maps = new Map[] { toolkitmap, store };

      long end = System.currentTimeMillis() + (120 * 1000L);
      Random rnd = new Random();
      final int index = getBarrierForAllClients().await();
      int count = 0;

      while (System.currentTimeMillis() < end) {
        for (Map<String, String> map : maps) {
          if (index == 0) {
            map.put(String.valueOf(count++), "");
            map.remove(String.valueOf(rnd.nextInt(count)));
            if (count % 100 == 0) {
              System.out.println("COUNT = " + count + "Size of Map" + map.size());
            }
          } else {
            for (Map.Entry<String, String> entry : map.entrySet()) {
              if (entry == null) { throw new AssertionError("null entry"); }
              if (entry.getKey() == null) { throw new AssertionError("null entry key"); }
              if (entry.getValue() == null) { throw new AssertionError("null entry value"); }
            }

            for (String key : map.keySet()) {
              if (key == null) { throw new AssertionError("null key"); }
            }

            for (String value : map.values()) {
              if (value == null) { throw new AssertionError("null value"); }
            }
          }
        }
      }
    }
  }
}
