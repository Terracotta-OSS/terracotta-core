/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.cache.ToolkitCacheConfigFields.PinningStore;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields.Consistency;

public class ClusteredMapDiffL2ConfigTestClient extends ClientBase {
  public ClusteredMapDiffL2ConfigTestClient(String[] args) {
    super(args);
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    // check tti, ttl, maxTotalCount, concurrency, consistency, pinning
    ToolkitBarrier barrier = toolkit.getBarrier("mybarrier", 2);
    int index = barrier.await();
    int count = 0;
    configCheck(index, getTTI(index, toolkit), count++, toolkit, barrier);
    configCheck(index, getTTL(index, toolkit), count++, toolkit, barrier);
    configCheck(index, getMaxTotalCount(index, toolkit), count++, toolkit, barrier);
    configCheck(index, getConcurrency(index, toolkit), count++, toolkit, barrier);
    configCheck(index, getConsistency(index, toolkit), count++, toolkit, barrier);
    configCheck(index, getPinning(index, toolkit), count++, toolkit, barrier);
  }

  private void configCheck(int index, Configuration configuration, int count, Toolkit toolkit, ToolkitBarrier barrier)
      throws Throwable {
    if (index == 0) {
      toolkit.getCache("testConfig" + count, configuration, null);
      barrier.await();
    } else {
      barrier.await();
      try {
        toolkit.getCache("testConfig" + count, configuration, null);
        throw new AssertionError();
      } catch (IllegalArgumentException e) {
        //
      }
    }
    barrier.await();
  }

  private Configuration getTTI(int index, Toolkit toolkit) {
    ToolkitCacheConfigBuilder builder = new ToolkitCacheConfigBuilder();
    int tti;
    if (index == 0) {
      tti = 10;
    } else {
      tti = 20;
    }
    builder.maxTTISeconds(tti);

    return builder.build();
  }

  private Configuration getTTL(int index, Toolkit toolkit) {
    ToolkitCacheConfigBuilder builder = new ToolkitCacheConfigBuilder();
    int ttl;
    if (index == 0) {
      ttl = 10;
    } else {
      ttl = 20;
    }
    builder.maxTTLSeconds(ttl);

    return builder.build();
  }

  private Configuration getConcurrency(int index, Toolkit toolkit) {
    ToolkitCacheConfigBuilder builder = new ToolkitCacheConfigBuilder();
    int ttl;
    if (index == 0) {
      ttl = 10;
    } else {
      ttl = 20;
    }
    builder.maxTTLSeconds(ttl);

    return builder.build();
  }

  private Configuration getMaxTotalCount(int index, Toolkit toolkit) {
    ToolkitCacheConfigBuilder builder = new ToolkitCacheConfigBuilder();
    int maxTotalCount;
    if (index == 0) {
      maxTotalCount = 400;
    } else {
      maxTotalCount = 500;
    }
    builder.maxTotalCount(maxTotalCount);

    return builder.build();
  }

  private Configuration getConsistency(int index, Toolkit toolkit) {
    ToolkitCacheConfigBuilder builder = new ToolkitCacheConfigBuilder();
    Consistency consistency;
    if (index == 0) {
      consistency = Consistency.EVENTUAL;
    } else {
      consistency = Consistency.STRONG;
    }
    builder.consistency(consistency);

    return builder.build();
  }

  private Configuration getPinning(int index, Toolkit toolkit) {
    ToolkitCacheConfigBuilder builder = new ToolkitCacheConfigBuilder();
    PinningStore pinning;
    if (index == 0) {
      pinning = PinningStore.INCACHE;
    } else {
      pinning = PinningStore.NONE;
    }
    builder.pinningStore(pinning);

    return builder.build();
  }
}
