/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.junit.Assert;
import org.terracotta.express.tests.CallableWaiter;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.store.ToolkitStore;
import org.terracotta.toolkit.store.ToolkitStoreConfigBuilder;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields.Consistency;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

public class AbstractClusteredMapMultiThreadTestClient extends ClientBase {

  public static final int     THREAD_COUNT            = 10;
  public static final int     ELEMENTS_FOR_ONE_THREAD = 50;
  public static final int     NUM_ELEMENTS            = THREAD_COUNT * ELEMENTS_FOR_ONE_THREAD;
  public static final int     NODE_COUNT              = 2;
  public static final String  name                    = "ClusteredMapMultiThreadStrongTest";
  private static final String KEY                     = "key-";
  private static final String VALUE                   = "value-";
  private static final String REPLACE_VALUE           = "replace-";
  public static final String  CONSISTENCY             = "consistency";
  protected ToolkitBarrier    barrier;
  protected Thread[]          threads;
  private final Consistency   consistency;
  private Toolkit             toolkit;

  public AbstractClusteredMapMultiThreadTestClient(Consistency consistnecy, String[] args) {
    super(args);
    this.consistency = consistnecy;
  }

  public void initThreads(ToolkitStore map, AtomicReference<Throwable> error) {
    // override in sub-classes
  }

  protected void verifyBasic(ToolkitStore map) {
    Assert.assertEquals(name, map.getName());
    Assert.assertTrue(map.isEmpty());
    Assert.assertFalse(map.isDestroyed());
    Assert.assertEquals(0, map.size());
  }

  @Override
  protected void test(Toolkit myToolkit) throws Throwable {
    this.toolkit = myToolkit;
    this.barrier = myToolkit.getBarrier(name, THREAD_COUNT * NODE_COUNT);
    ToolkitStore map = getMap(name);
    verifyBasic(map);
    AtomicReference<Throwable> error = new AtomicReference<Throwable>();
    initThreads(map, error);

    // start executing all threads
    for (int i = 0; i < THREAD_COUNT; ++i) {
      threads[i].start();
    }

    // wait for all threads to finish
    for (int i = 0; i < THREAD_COUNT; ++i) {
      threads[i].join();
    }
    if (error.get() != null) { throw error.get(); }
  }

  private ToolkitStore<String, String> getMap(String mapName) {
    Configuration config = new ToolkitStoreConfigBuilder().consistency(consistency).build();
    return toolkit.getCache(mapName, config, null);
  }

  protected Boolean checkInts(int expected, int actual) {
    debug("Expected: " + expected + ", Actual: " + actual);
    return expected == actual;
  }

  protected void assertWithCallable(Callable<Boolean> callable) throws Exception {
    CallableWaiter.waitOnCallable(callable);
  }

  protected String getKey(int i) {
    return KEY + i;
  }

  protected String getValue(int i) {
    return VALUE + i;
  }

  protected String getReplacedValue(int i) {
    return REPLACE_VALUE + i;
  }
}
