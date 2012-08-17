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

import java.util.concurrent.Callable;

import junit.framework.Assert;

public class ClusteredMapEventualLocalCacheDisabledTest extends AbstractToolkitTestBase {

  public ClusteredMapEventualLocalCacheDisabledTest(TestConfig testConfig) {

    super(testConfig, ClusteredMapEventualLocalCacheDisabledTestClient.class,
          ClusteredMapEventualLocalCacheDisabledTestClient.class);
  }

  public static class ClusteredMapEventualLocalCacheDisabledTestClient extends ClientBase {

    private static final int    ITERATION    = 20;
    private static final int    COUNT        = 100;
    private static final String KEY_PREFIX   = "key";
    private static final String VALUE_PREFIX = "val";

    public ClusteredMapEventualLocalCacheDisabledTestClient(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new ClusteredMapEventualLocalCacheDisabledTestClient(args).run();
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      ToolkitBarrier barrier = toolkit.getBarrier("test", 2);
      int index = barrier.await();

      ToolkitStore map = createMap(toolkit);

      barrier.await();

      for (int i = 0; i < ITERATION; i++) {
        doPut(index, map, i);
        barrier.await();
        verify(index, map, i);
        barrier.await();
      }
    }

    private void doPut(int index, ToolkitStore map, int iteration) {
      if (index == 0) {
        for (int i = 0; i < COUNT; i++) {
          map.put(createKey(i, iteration), createValue(i, iteration));
        }
      }
    }

    private void verify(final int index, final ToolkitStore map, final int iteration) throws Throwable {
      if (index == 0) {
        // monotonic reads
        for (int i = 0; i < COUNT; i++) {
          String value = (String) map.get(createKey(i, iteration));
          Assert.assertEquals("PUT Client failed: ", createValue(i, iteration), value);
        }
      } else {
        // eventually the correct value should be read
        for (int i = 0; i < COUNT; i++) {
          final int number = i;
          WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
              String key = createKey(number, iteration);
              String actualValue = (String) map.get(key);
              boolean result = createValue(number, iteration).equals(actualValue);
              if (!result) {
                System.err.println("Value got= " + result + " for key=" + key);
              }
              return result;
            }
          });
        }
      }
    }

    private ToolkitStore createMap(Toolkit toolkit) {
      Configuration config = new ToolkitStoreConfigBuilder().consistency(Consistency.EVENTUAL).localCacheEnabled(false)
          .build();
      ToolkitStore map = toolkit.getStore("test", config, null);
      return map;
    }

    private String createKey(int number, int iteration) {
      return KEY_PREFIX + number;
    }

    private String createValue(int number, int iteration) {
      return VALUE_PREFIX + iteration + "|" + number;
    }
  }

}
