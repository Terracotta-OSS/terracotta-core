/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.builder.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.store.ToolkitConfigFields;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

public class ClusteredCacheDistributedTypeFactoryTest extends TestCase {
  private static final Random random = new Random(System.currentTimeMillis());

  public void testSingleConcurrencyMultipleStripes() {
    ToolkitCacheDistributedTypeFactory stubFactory = new ToolkitCacheDistributedTypeFactory(null, null);
    ToolkitCacheConfigBuilder builder = new ToolkitCacheConfigBuilder();

    builder.maxTotalCount(100).concurrency(1);
    Configuration[] configs = stubFactory.distributeConfigAmongStripes(builder.build(), 2);
    Assert.assertEquals(2, configs.length);
    Assert.assertEquals(1, configs[0].getInt(ToolkitConfigFields.CONCURRENCY_FIELD_NAME));
    Assert.assertEquals(100, configs[0].getInt(ToolkitConfigFields.MAX_TOTAL_COUNT_FIELD_NAME));
    Assert.assertEquals(0, configs[1].getInt(ToolkitConfigFields.CONCURRENCY_FIELD_NAME));
    Assert.assertEquals(0, configs[1].getInt(ToolkitConfigFields.MAX_TOTAL_COUNT_FIELD_NAME));
  }

  public void testMultiConcurrencyMultiStripe() {
    ToolkitCacheDistributedTypeFactory stubFactory = new ToolkitCacheDistributedTypeFactory(null, null);
    ToolkitCacheConfigBuilder builder = new ToolkitCacheConfigBuilder();

    builder.maxTotalCount(100).concurrency(4);
    Configuration[] configs = stubFactory.distributeConfigAmongStripes(builder.build(), 5);
    Assert.assertEquals(5, configs.length);
    for (int i = 0; i < 4; i++) {
      Assert.assertEquals(1, configs[i].getInt(ToolkitConfigFields.CONCURRENCY_FIELD_NAME));
      Assert.assertEquals(25, configs[i].getInt(ToolkitConfigFields.MAX_TOTAL_COUNT_FIELD_NAME));
    }
    Assert.assertEquals(0, configs[4].getInt(ToolkitConfigFields.CONCURRENCY_FIELD_NAME));
    Assert.assertEquals(0, configs[4].getInt(ToolkitConfigFields.MAX_TOTAL_COUNT_FIELD_NAME));
  }

  public void testRandomly() {
    final int testCases = 10000;
    int concurrencyLessTestCases = 0;
    for (int i = 1; i <= testCases; i++) {
      final int randomNumStripes = random.nextInt(1000) + 1; // min value 1
      final int randomConcurrency;
      final boolean concurrencyLessThanStripes = (random.nextInt(2) == 0);
      if (concurrencyLessThanStripes) {
        concurrencyLessTestCases++;
        randomConcurrency = random.nextInt(randomNumStripes) + 1;
      } else {
        randomConcurrency = random.nextInt(5000) + 1;
      }
      final int randomMaxTotalCount = random.nextInt(1000000);
      doRandomTest(randomNumStripes, randomConcurrency, randomMaxTotalCount);
      if (i % 1000 == 0) {
        System.out.print(".");
        System.out.flush();
      }
    }
    System.out.println("Done with random " + testCases + " test cases (" + concurrencyLessTestCases
                       + " cases where concurrency<=stripes)");
  }

  private void doRandomTest(int randomNumStripes, int randomConcurrency, int randomMaxTotalCount) {

    final String msg = "numStripes: " + randomNumStripes + ", concurrency: " + randomConcurrency + ", maxTotalCount: "
                       + randomMaxTotalCount;

    ToolkitCacheDistributedTypeFactory stubFactory = new ToolkitCacheDistributedTypeFactory(null, null);
    ToolkitCacheConfigBuilder builder = new ToolkitCacheConfigBuilder();

    builder.maxTotalCount(randomMaxTotalCount).concurrency(randomConcurrency);
    Configuration[] configs = stubFactory.distributeConfigAmongStripes(builder.build(), randomNumStripes);
    Assert.assertEquals(msg, randomNumStripes, configs.length);

    int overallActualConcurrency = 0;
    int overallActualMaxTotalCount = 0;
    Set<Integer> distinctConcurrencyValuesSet = new HashSet<Integer>();
    Set<Integer> distinctMaxTotalCountValuesSet = new HashSet<Integer>();
    int largestConcurrency = Integer.MIN_VALUE;
    int leastConcurrency = Integer.MAX_VALUE;
    int largestTotalCount = Integer.MIN_VALUE;
    int leastTotalCount = Integer.MAX_VALUE;
    List<Integer> concurrencies = new ArrayList<Integer>();
    List<Integer> totalCounts = new ArrayList<Integer>();
    for (Configuration config : configs) {
      int concurrency = config.getInt(ToolkitConfigFields.CONCURRENCY_FIELD_NAME);
      int maxTotalCount = config.getInt(ToolkitConfigFields.MAX_TOTAL_COUNT_FIELD_NAME);
      distinctConcurrencyValuesSet.add(concurrency);
      distinctMaxTotalCountValuesSet.add(maxTotalCount);

      overallActualConcurrency += concurrency;
      overallActualMaxTotalCount += maxTotalCount;

      concurrencies.add(concurrency);
      totalCounts.add(maxTotalCount);

      largestConcurrency = Math.max(largestConcurrency, concurrency);
      leastConcurrency = Math.min(leastConcurrency, concurrency);
      largestTotalCount = Math.max(largestTotalCount, maxTotalCount);
      if (maxTotalCount != 0) {
        leastTotalCount = Math.min(leastTotalCount, maxTotalCount);
      }
    }

    Assert.assertEquals(msg + " - divided configs' concurrency doesn't sum up to overall concurrency",
                        randomConcurrency, overallActualConcurrency);
    Assert.assertEquals(msg + " - divided configs' maxTotalCount doesn't sum up to overall maxTotalCount",
                        randomMaxTotalCount, overallActualMaxTotalCount);
    Assert.assertTrue(msg + " - All stripes should get only 2 distinct concurrency values",
                      distinctConcurrencyValuesSet.size() <= 2);
    Assert.assertTrue(msg + " - All stripes should get only 2 distinct maxTotalCount values",
                      distinctConcurrencyValuesSet.size() <= 2);
    Assert.assertTrue(msg + " - not fairly distributed concurrency: largest: " + largestConcurrency + ", least: "
                      + leastConcurrency + " - " + concurrencies, largestConcurrency - leastConcurrency <= 1);
    Assert.assertTrue(msg + " - not fairly distributed maxTotalCount: largest: " + largestTotalCount + ", least: "
                      + leastTotalCount + " - " + totalCounts, largestTotalCount - leastTotalCount <= 1);
  }

}
