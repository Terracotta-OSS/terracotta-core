/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.nonstop;

import org.terracotta.express.tests.base.NonStopClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.nonstop.NonStopConfigurationBuilder;
import org.terracotta.toolkit.nonstop.NonStopConfigurationFields.NonStopTimeoutBehavior;
import org.terracotta.toolkit.nonstop.NonStopException;

import java.util.Date;

import junit.framework.Assert;

public abstract class AbstractNonStopTestClient extends NonStopClientBase {
  protected static final int  CLIENT_COUNT            = 2;
  protected static final int  NUMBER_OF_ELEMENTS      = 10;
  protected static final int  MAX_ENTRIES_LOCAL_HEAP  = 0;
  protected static final long NON_STOP_TIMEOUT_MILLIS = 10000;

  public AbstractNonStopTestClient(String[] args) {
    super(args);
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    ToolkitBarrier barrier = toolkit.getBarrier("testBarrier", CLIENT_COUNT);
    int index = barrier.await();
    ToolkitCache<Integer, Integer> cache = null;
    cache = createCache(toolkit);

    if (index == 0) {
      for (int i = 0; i < NUMBER_OF_ELEMENTS; i++) {
        cache.put(i, i);
      }
      System.err.println("Cache size " + cache.size() + " at " + new Date());
    }

    barrier.await();

    if (index == 1) {
      addToLocalCache(cache);

      makeServerDie();

      for (int i = 0; i < NUMBER_OF_ELEMENTS; i++) {
        NonStopException exception = null;
        long time = System.currentTimeMillis();
        try {
          Integer intValue = cache.get(i);
          checkOnReturnValue(i, intValue);
        } catch (NonStopException e) {
          exception = e;
        }

        time = System.currentTimeMillis() - time;
        Assert.assertTrue((time > (NON_STOP_TIMEOUT_MILLIS - 500)) && (time < (NON_STOP_TIMEOUT_MILLIS + 2000)));
        System.err.println("Time consumed " + time);

        checkNonStopExceptionOnReads(exception);
      }

      restartCrashedServer();
    }
  }

  protected void addToLocalCache(ToolkitCache<Integer, Integer> cache) {
    for (int i = 0; i < NUMBER_OF_ELEMENTS; i++) {
      long time = System.currentTimeMillis();
      try {
        Assert.assertNotNull(cache.get(i));
      } catch (NonStopException e) {
        System.err.println("Time elapsed " + (System.currentTimeMillis() - time) + " , i=" + i);
        throw e;
      }
    }
  }

  private void checkOnReturnValue(Integer expected, Integer actual) {
    switch (getImmutableOpTimeoutBehavior()) {
      case EXCEPTION_ON_TIMEOUT:
        throw new AssertionError("Expected " + expected + " , actual " + actual + ". But no value should have come.");
      case LOCAL_READS:
        Assert.assertEquals(expected, actual);
        break;
      case NO_OP:
        Assert.assertNull(actual);
        break;
    }
  }

  private void checkNonStopExceptionOnReads(NonStopException exception) {
    switch (getImmutableOpTimeoutBehavior()) {
      case EXCEPTION_ON_TIMEOUT:
        Assert.assertNotNull(exception);
        break;
      case LOCAL_READS:
      case NO_OP:
        if (exception != null) {
          System.err.println(exception);
          throw new AssertionError("Unexpected NonStop Exception");
        }
        break;
    }
  }

  private void restartCrashedServer() throws Exception {
    getTestControlMbean().reastartLastCrashedServer(0);
  }

  private void makeServerDie() throws Exception {
    getTestControlMbean().crashActiveServer(0);
    Thread.sleep(10 * 1000);
  }

  private ToolkitCache createCache(Toolkit toolkit) {
    String cacheName = "test-cache";

    new NonStopConfigurationBuilder().timeoutMillis(NON_STOP_TIMEOUT_MILLIS)
        .nonStopTimeoutBehavior(getImmutableOpTimeoutBehavior(), getMutableOpTimeoutBehavior()).apply(toolkit);

    ToolkitCacheConfigBuilder builder = new ToolkitCacheConfigBuilder();
    builder.maxCountLocalHeap(MAX_ENTRIES_LOCAL_HEAP);

    addMoreConfigToBuilder(builder);

    return toolkit.getCache(cacheName, builder.build(), Integer.class);
  }

  protected void addMoreConfigToBuilder(ToolkitCacheConfigBuilder builder) {
    //
  }

  protected abstract NonStopTimeoutBehavior getImmutableOpTimeoutBehavior();

  protected abstract NonStopTimeoutBehavior getMutableOpTimeoutBehavior();
}
